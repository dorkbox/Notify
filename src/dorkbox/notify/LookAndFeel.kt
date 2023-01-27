/*
 * Copyright 2015 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.notify

import dorkbox.swingActiveRender.ActionHandlerLong
import dorkbox.swingActiveRender.SwingActiveRender
import dorkbox.tweenEngine.Tween
import dorkbox.tweenEngine.TweenCallback.Events.COMPLETE
import dorkbox.tweenEngine.TweenEngine.Companion.create
import dorkbox.tweenEngine.TweenEquations
import dorkbox.util.ScreenUtil
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window
import java.awt.event.MouseAdapter
import java.util.*

internal class LookAndFeel(
    private val notify: INotify,
    private val parent: Window,
    private val notifyCanvas: NotifyCanvas,
    private val notification: Notify,
    parentBounds: Rectangle,
    private val isDesktopNotification: Boolean
) {
    companion object {
        private val popups: MutableMap<String?, PopupList> = HashMap()

        // access is only from a single thread ever, so unsafe is preferred.
        val animation = create().unsafe().build()

        val accessor = NotifyAccessor()

        // this is for updating the tween engine during active-rendering
        private val frameStartHandler = ActionHandlerLong { deltaInNanos -> animation.update(deltaInNanos) }

        const val SPACER = 10
        const val MARGIN = 20

        private val windowListener: java.awt.event.WindowAdapter = WindowAdapter()
        private val mouseListener: MouseAdapter = ClickAdapter()
        private val RANDOM = Random()
        private val MOVE_DURATION = Notify.MOVE_DURATION

        private fun getAnchorX(position: Pos, bounds: Rectangle, isDesktop: Boolean): Int {
            // we use the screen that the mouse is currently on.
            val startX = if (isDesktop) {
                bounds.getX().toInt()
            } else {
                0
            }

            val screenWidth = bounds.getWidth().toInt()
            return when (position) {
                Pos.TOP_LEFT, Pos.BOTTOM_LEFT -> MARGIN + startX
                Pos.CENTER -> startX + screenWidth / 2 - NotifyCanvas.WIDTH / 2 - MARGIN / 2
                Pos.TOP_RIGHT, Pos.BOTTOM_RIGHT -> startX + screenWidth - NotifyCanvas.WIDTH - MARGIN
            }
        }

        private fun getAnchorY(position: Pos, bounds: Rectangle, isDesktop: Boolean): Int {
            val startY = if (isDesktop) {
                bounds.getY().toInt()
            } else {
                0
            }

            val screenHeight = bounds.getHeight().toInt()
            return when (position) {
                Pos.TOP_LEFT, Pos.TOP_RIGHT -> startY + MARGIN
                Pos.CENTER -> startY + screenHeight / 2 - NotifyCanvas.HEIGHT / 2 - MARGIN / 2 - SPACER
                Pos.BOTTOM_LEFT, Pos.BOTTOM_RIGHT -> if (isDesktop) {
                    startY + screenHeight - NotifyCanvas.HEIGHT - MARGIN
                } else {
                             screenHeight - NotifyCanvas.HEIGHT - MARGIN - SPACER * 2
                }
            }
        }

        // only called on the swing EDT thread
        private fun addPopupToMap(sourceLook: LookAndFeel) {
            synchronized(popups) {
                val id = sourceLook.idAndPosition
                var looks = popups[id]
                if (looks == null) {
                    looks = PopupList()
                    popups[id] = looks
                }

                val index = looks.size()
                sourceLook.popupIndex = index

                // the popups are ALL the same size!
                // popups at TOP grow down, popups at BOTTOM grow up
                val anchorX = sourceLook.anchorX
                val anchorY = sourceLook.anchorY

                val targetY = if (index == 0) {
                    anchorY
                } else {
                    val growDown = growDown(sourceLook)
                    if (sourceLook.isDesktopNotification && index == 1) {
                        // have to adjust for offsets when the window-manager has a toolbar that consumes space and prevents overlap.
                        // this is only done when the 2nd popup is added to the list
                        looks.calculateOffset(growDown, anchorX, anchorY)
                    }
                    if (growDown) {
                        anchorY + index * (NotifyCanvas.HEIGHT + SPACER) + looks.offsetY
                    } else {
                        anchorY - index * (NotifyCanvas.HEIGHT + SPACER) + looks.offsetY
                    }
                }

                looks.add(sourceLook)
                sourceLook.setLocation(anchorX, targetY)

                if (sourceLook.hideAfterDurationInSeconds > 0 && sourceLook.hideTween == null) {
                    // begin a timeline to get rid of the popup (default is 5 seconds)
                    animation.to(sourceLook, NotifyAccessor.PROGRESS, accessor, sourceLook.hideAfterDurationInSeconds)
                        .target(NotifyCanvas.WIDTH.toFloat())
                        .ease(TweenEquations.Linear)
                        .addCallback(COMPLETE) { sourceLook.notify.close() }
                        .start()
                }
            }
        }

        // only called on the swing app or SwingActiveRender thread
        private fun removePopupFromMap(sourceLook: LookAndFeel): Boolean {
            val growDown = growDown(sourceLook)
            var popupsAreEmpty: Boolean

            synchronized(popups) {
                popupsAreEmpty = popups.isEmpty()
                val allLooks = popups[sourceLook.idAndPosition]

                // there are two loops because it is necessary to cancel + remove all tweens BEFORE adding new ones.
                var adjustPopupPosition = false
                val iterator = allLooks!!.iterator()
                while (iterator.hasNext()) {
                    val look = iterator.next()
                    if (look.tween != null) {
                        look.tween!!.cancel() // cancel does its thing on the next tick of animation cycle
                        look.tween = null
                    }

                    if (look === sourceLook) {
                        if (look.hideTween != null) {
                            look.hideTween!!.cancel()
                            look.hideTween = null
                        }
                        adjustPopupPosition = true
                        iterator.remove()
                    }

                    if (adjustPopupPosition) {
                        look.popupIndex--
                    }
                }

                // have to adjust for offsets when the window-manager has a toolbar that consumes space and prevents overlap.
                val offsetY = allLooks.offsetY
                for (index in 0 until allLooks.size()) {
                    val look = allLooks[index]

                    // the popups are ALL the same size!
                    // popups at TOP grow down, popups at BOTTOM grow up
                    val changedY = if (growDown) {
                        look.anchorY + (look.popupIndex * (NotifyCanvas.HEIGHT + SPACER) + offsetY)
                    } else {
                        look.anchorY - (look.popupIndex * (NotifyCanvas.HEIGHT + SPACER) + offsetY)
                    }

                    // now animate that popup to its new location
                    look.tween = animation
                        .to(look, NotifyAccessor.Y_POS, accessor, MOVE_DURATION)
                        .target(changedY.toFloat())
                        .ease(TweenEquations.Linear)
                        .addCallback(COMPLETE) {
                            // make sure to remove the tween once it's done, otherwise .kill can do weird things.
                            look.tween = null
                        }
                        .start()
                }
            }
            return popupsAreEmpty
        }

        private fun growDown(look: LookAndFeel): Boolean {
            return when (look.position) {
                Pos.TOP_LEFT, Pos.TOP_RIGHT, Pos.CENTER -> true
                else -> false
            }
        }
    }



    @Volatile
    private var anchorX: Int

    @Volatile
    private var anchorY: Int
    private val hideAfterDurationInSeconds: Float
    private val position: Pos

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    private var idAndPosition: String? = null
    private var popupIndex = 0

    @Volatile
    private var tween: Tween<*>? = null

    @Volatile
    private var hideTween: Tween<*>? = null
    private val onGeneralAreaClickAction = notification.onGeneralAreaClickAction // explicitly make a copy

    init {
        if (isDesktopNotification) {
            parent.addWindowListener(windowListener)
        }

        notifyCanvas.addMouseListener(mouseListener)
        hideAfterDurationInSeconds = notification.hideAfterDurationInMillis / 1000.0f
        position = notification.position

        idAndPosition = if (isDesktopNotification) {
            val point = Point(parentBounds.getX().toInt(), parentBounds.getY().toInt())
            ScreenUtil.getMonitorNumberAtLocation(point).toString() + ":" + position
        } else {
            parent.name + ":" + position
        }

        anchorX = getAnchorX(position, parentBounds, isDesktopNotification)
        anchorY = getAnchorY(position, parentBounds, isDesktopNotification)
    }

    fun onClick(x: Int, y: Int) {
        // Check - we were over the 'X' (and thus no notify), or was it in the general area?

        // reasonable position for detecting mouse over
        if (!notifyCanvas.isCloseButton(x, y)) {
            // only call the general click handler IF we click in the general area!
            onGeneralAreaClickAction.invoke(notification)
        }

        // we always close the notification popup
        notify.close()
    }

    // only called from an application
    fun reLayout(bounds: Rectangle) {
        // when the parent window moves, we stop all animation and snap the popup into place. This simplifies logic greatly
        anchorX = getAnchorX(position, bounds, isDesktopNotification)
        anchorY = getAnchorY(position, bounds, isDesktopNotification)

        val growDown = growDown(this)

        if (tween != null) {
            tween!!.cancel() // cancel does its thing on the next tick of animation cycle
            tween = null
        }


        var changedY: Int
        if (popupIndex == 0) {
            changedY = anchorY
        } else {
            synchronized(popups) {
                val id = idAndPosition
                val looks = popups[id]
                changedY = if (looks != null) {
                    if (growDown) {
                        anchorY + popupIndex * (NotifyCanvas.HEIGHT + SPACER)
                    } else {
                        anchorY - popupIndex * (NotifyCanvas.HEIGHT + SPACER)
                    }
                } else {
                    anchorY
                }
            }
        }

        setLocation(anchorX, changedY)
    }

    fun close() {
        if (hideTween != null) {
            hideTween!!.cancel()
            hideTween = null
        }

        if (tween != null) {
            tween!!.cancel()
            tween = null
        }

        if (isDesktopNotification) {
            parent.removeWindowListener(windowListener)
        }

        parent.removeMouseListener(mouseListener)
        updatePositionsPre(false)
        updatePositionsPost(false)
    }

    fun shake(durationInMillis: Int, amplitude: Int) {
        var i1 = RANDOM.nextInt((amplitude shl 2) + 1) - amplitude
        var i2 = RANDOM.nextInt((amplitude shl 2) + 1) - amplitude
        i1 = i1 shr 2
        i2 = i2 shr 2

        // make sure it always moves by some amount
        if (i1 < 0) {
            i1 -= amplitude shr 2
        } else {
            i1 += amplitude shr 2
        }
        if (i2 < 0) {
            i2 -= amplitude shr 2
        } else {
            i2 += amplitude shr 2
        }

        var count = durationInMillis / 50
        // make sure we always end the animation where we start
        if (count and 1 == 0) {
            count++
        }

        animation
            .to(this, NotifyAccessor.X_Y_POS, accessor, 0.05f)
            .targetRelative(i1.toFloat(), i2.toFloat())
            .repeatAutoReverse(count, 0f)
            .ease(TweenEquations.Linear)
            .start()
    }

    var y: Int
        get() = if (isDesktopNotification) {
            parent.y
        } else {
            notifyCanvas.y
        }
        set(y) {
            if (isDesktopNotification) {
                parent.setLocation(parent.x, y)
            } else {
                notifyCanvas.setLocation(notifyCanvas.x, y)
            }
        }

    val x: Int
        get() = if (isDesktopNotification) {
            parent.x
        } else {
            notifyCanvas.x
        }

    fun setLocation(x: Int, y: Int) {
        if (isDesktopNotification) {
            parent.setLocation(x, y)
        } else {
            notifyCanvas.setLocation(x, y)
        }
    }

    var progress: Int
        get() = notifyCanvas.progress
        set(progress) {
            notifyCanvas.progress = progress
        }

    /**
     * we have to remove the active renderer BEFORE we set the visibility status.
     */
    fun updatePositionsPre(visible: Boolean) {
        if (!visible) {
            val popupsAreEmpty = removePopupFromMap(this)
            SwingActiveRender.removeActiveRender(notifyCanvas)
            if (popupsAreEmpty) {
                // if there's nothing left, stop the timer.
                SwingActiveRender.removeActiveRenderFrameStart(frameStartHandler)
            }
        }
    }

    /**
     * when using active rendering, we have to add it AFTER we have set the visibility status
     */
    fun updatePositionsPost(visible: Boolean) {
        if (visible) {
            SwingActiveRender.addActiveRender(notifyCanvas)

            // start if we have previously stopped the timer
            if (!SwingActiveRender.containsActiveRenderFrameStart(frameStartHandler)) {
                animation.resetUpdateTime()
                SwingActiveRender.addActiveRenderFrameStart(frameStartHandler)
            }
            addPopupToMap(this)
        }
    }
}
