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

import dorkbox.notify.LAFUtil.RANDOM
import dorkbox.notify.LAFUtil.SPACER
import dorkbox.notify.LAFUtil.accessor
import dorkbox.notify.LAFUtil.addPopupToMap
import dorkbox.notify.LAFUtil.animation
import dorkbox.notify.LAFUtil.frameStartHandler
import dorkbox.notify.LAFUtil.getAnchorX
import dorkbox.notify.LAFUtil.getAnchorY
import dorkbox.notify.LAFUtil.growDown
import dorkbox.notify.LAFUtil.mouseListener
import dorkbox.notify.LAFUtil.popups
import dorkbox.notify.LAFUtil.removePopupFromMap
import dorkbox.notify.LAFUtil.windowListener
import dorkbox.swingActiveRender.SwingActiveRender
import dorkbox.tweenEngine.Tween
import dorkbox.tweenEngine.TweenEquations
import dorkbox.util.ScreenUtil
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window

internal class LookAndFeel(
    private val parent: Window,
    private val notifyCanvas: NotifyCanvas,
    val notification: Notify,
    parentBounds: Rectangle,
    val isDesktopNotification: Boolean
) {
    @Volatile
    var anchorX: Int

    @Volatile
    var anchorY: Int
    val hideAfterDurationInSeconds: Float
    val position: Position

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    var idAndPosition: String
    var popupIndex = 0

    @Volatile
    var tween: Tween<*>? = null

    @Volatile
    var hideTween: Tween<*>? = null


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
