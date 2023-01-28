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
import dorkbox.notify.LAFUtil.accessor
import dorkbox.notify.LAFUtil.addPopupToMap
import dorkbox.notify.LAFUtil.animation
import dorkbox.notify.LAFUtil.frameStartHandler
import dorkbox.notify.LAFUtil.mouseListener
import dorkbox.notify.LAFUtil.removePopupFromMap
import dorkbox.swingActiveRender.SwingActiveRender
import dorkbox.tweenEngine.Tween
import dorkbox.tweenEngine.TweenEquations
import java.awt.Rectangle
import java.awt.Window

internal abstract class LookAndFeel(private val parent: Window, val notifyCanvas: NotifyCanvas, val notification: Notify) {

    @Volatile
    var anchorX = 0

    @Volatile
    var anchorY = 0

    val hideAfterDurationInSeconds: Float
    val position: Position

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    var idAndPosition = ""
    var popupIndex = 0

    @Volatile
    var tween: Tween<*>? = null

    @Volatile
    var hideTween: Tween<*>? = null

    abstract val isDesktop: Boolean


    init {
        notifyCanvas.addMouseListener(mouseListener)
        hideAfterDurationInSeconds = notification.hideAfterDurationInMillis / 1000.0f
        position = notification.position
    }

    // only called from an application
    open fun reLayout(bounds: Rectangle) {}

    open fun close() {
        if (hideTween != null) {
            hideTween!!.cancel()
            hideTween = null
        }

        if (tween != null) {
            tween!!.cancel()
            tween = null
        }

        parent.removeMouseListener(mouseListener)
        updatePositionsPre(false)
        updatePositionsPost(false, isDesktop)
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

    abstract var y: Int

    abstract val x: Int

    abstract fun setLocation(x: Int, y: Int)


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
            println("remove post")
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
    fun updatePositionsPost(visible: Boolean, isDesktop: Boolean) {
        if (visible) {
            println("add post")
            SwingActiveRender.addActiveRender(notifyCanvas)

            // start if we have previously stopped the timer
            if (!SwingActiveRender.containsActiveRenderFrameStart(frameStartHandler)) {
                animation.resetUpdateTime()
                SwingActiveRender.addActiveRenderFrameStart(frameStartHandler)
            }

            addPopupToMap(this, isDesktop)
        }
    }
}
