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
import dorkbox.tweenEngine.TweenCallback.Events.COMPLETE
import dorkbox.tweenEngine.TweenEngine.Companion.create
import dorkbox.tweenEngine.TweenEquations
import dorkbox.util.ScreenUtil
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.util.*

internal object LAFUtil{
    val popups: MutableMap<String, PopupList> = HashMap()

    // access is only from a single thread ever, so unsafe is preferred.
    val animation = create().unsafe().build()

    val accessor = NotifyAccessor()

    // this is for updating the tween engine during active-rendering
    val frameStartHandler = ActionHandlerLong { deltaInNanos -> animation.update(deltaInNanos) }

    const val SPACER = 10
    const val MARGIN = 20

    val windowListener: java.awt.event.WindowAdapter = WindowAdapter()
    val mouseListener: MouseAdapter = ClickAdapter()
    val RANDOM = Random()

    private val MOVE_DURATION = Notify.MOVE_DURATION

    fun getGraphics(screen: Int): Rectangle {
        val device = if (screen == Short.MIN_VALUE.toInt()) {
            // set screen position based on mouse
            val mouseLocation = MouseInfo.getPointerInfo().location
            ScreenUtil.getMonitorAtLocation(mouseLocation)
        } else {
            // set screen position based on specified screen
            var screenNumber = screen
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val screenDevices = ge.screenDevices

            if (screenNumber < 0) {
                screenNumber = 0
            } else if (screenNumber > screenDevices.size - 1) {
                screenNumber = screenDevices.size - 1
            }

            screenDevices[screenNumber]
        }

        return device.defaultConfiguration.bounds
    }

    fun getAnchorX(position: Position, bounds: Rectangle, isDesktop: Boolean): Int {
        // we use the screen that the mouse is currently on.
        val startX = if (isDesktop) {
            bounds.getX().toInt()
        } else {
            0
        }

        val screenWidth = bounds.getWidth().toInt()
        return when (position) {
            Position.TOP_LEFT, Position.BOTTOM_LEFT -> MARGIN + startX
            Position.CENTER -> startX + screenWidth / 2 - NotifyCanvas.WIDTH / 2 - MARGIN / 2
            Position.TOP_RIGHT, Position.BOTTOM_RIGHT -> startX + screenWidth - NotifyCanvas.WIDTH - MARGIN
        }
    }

    fun getAnchorY(position: Position, bounds: Rectangle, isDesktop: Boolean): Int {
        val startY = if (isDesktop) {
            bounds.getY().toInt()
        } else {
            0
        }

        val screenHeight = bounds.getHeight().toInt()
        return when (position) {
            Position.TOP_LEFT, Position.TOP_RIGHT -> startY + MARGIN
            Position.CENTER -> startY + screenHeight / 2 - NotifyCanvas.HEIGHT / 2 - MARGIN / 2 - SPACER
            Position.BOTTOM_LEFT, Position.BOTTOM_RIGHT -> if (isDesktop) {
                startY + screenHeight - NotifyCanvas.HEIGHT - MARGIN
            } else {
                         screenHeight - NotifyCanvas.HEIGHT - MARGIN - SPACER * 2
            }
        }
    }

    // only called on the swing EDT thread
    fun addPopupToMap(sourceLook: LookAndFeel) {
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
                    .addCallback(COMPLETE) { sourceLook.notification.onClose() }
                    .start()
            }
        }
    }

    // only called on the swing app or SwingActiveRender thread
    fun removePopupFromMap(sourceLook: LookAndFeel): Boolean {
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

    fun growDown(look: LookAndFeel): Boolean {
        return when (look.position) {
            Position.TOP_LEFT, Position.TOP_RIGHT, Position.CENTER -> true
            else -> false
        }
    }
}