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

import dorkbox.swingActiveRender.SwingActiveRender
import dorkbox.tweenEngine.Tween
import java.awt.BasicStroke
import java.awt.Color
import java.awt.RenderingHints
import java.awt.Stroke
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JLabel

internal interface NotifyType<T> {
    companion object {
        private val stroke: Stroke = BasicStroke(2f)

        internal const val closeX = 282
        internal const val closeY = 2

        internal const val Y_1 = closeY + 5
        internal const val X_1 = closeX + 5
        internal const val Y_2 = closeY + 11
        internal const val X_2 = closeX + 11
    }

    val notification: Notify

    var shakeTween: Tween<T>?
    var moveTween: Tween<T>?
    var hideTween: Tween<T>?


    // this is used in combination with position, so that we can track which screen and what position a popup is in
    var idAndPosition: String
    var popupIndex: Int

    // the anchor position is where the FIRST popup would show
    var anchorX: Int
    var anchorY: Int

    fun getX(): Int
    fun getY(): Int

    // for the progress bar. we directly draw this onscreen
    // non-volatile because it's always accessed in the active render thread
    var progress: Int


    fun renderBackgroundInfo(title: String, textBody: String, theme: Theme, imageIcon: ImageIcon?): BufferedImage {
        val image = BufferedImage(Notify.WIDTH, Notify.HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()

        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY)
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE)
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)

        try {
            g2.color = theme.panel_BG
            g2.fillRect(0, 0, Notify.WIDTH, Notify.HEIGHT)

            // Draw the title text
            g2.color = theme.titleText_FG
            g2.font = theme.titleTextFont
            g2.drawString(title, 5, 20)


            var posX = 10
            val posY = -8
            var textLengthLimit = 108

            // ICON
            if (imageIcon != null) {
                textLengthLimit = 88
                posX = 60
                // Draw the image
                imageIcon.paintIcon(null, g2, 5, 30)
            }

            // Draw the main text
            var length = textBody.length
            val text = StringBuilder(length)

            // are we "html" already? just check for the starting tag and strip off END html tag
            if (length >= 13 && textBody.regionMatches(length - 7, "</html>", 0, 7, ignoreCase = true)) {
                text.append(textBody)
                text.delete(text.length - 7, text.length)
                length -= 7
            } else {
                text.append("<html>")
                text.append(textBody)
            }

            // make sure the text is the correct length
            if (length > textLengthLimit) {
                text.delete(6 + textLengthLimit, text.length)
                text.append("...")
            }
            text.append("</html>")


            val mainTextLabel = JLabel()
            mainTextLabel.foreground = theme.mainText_FG
            mainTextLabel.font = theme.mainTextFont
            mainTextLabel.text = text.toString()
            mainTextLabel.setBounds(0, 0, Notify.WIDTH - posX - 2, Notify.HEIGHT)

            g2.translate(posX, posY)
            mainTextLabel.paint(g2)
            g2.translate(-posX, -posY)
        } finally {
            g2.dispose()
        }

        return image
    }

    fun renderCloseButton(theme: Theme, enabled: Boolean): BufferedImage {
        // manually draw the close button

        val image = BufferedImage(Notify.WIDTH, Notify.HEIGHT, BufferedImage.TYPE_INT_ARGB)
        val g2 = image.createGraphics()

        g2.color = theme.panel_BG
        g2.stroke = stroke


        if (enabled) {
            g2.color = Color.RED
        } else {
            g2.color = theme.closeX_FG
        }

        // draw the X
        g2.drawLine(X_1, Y_1, X_2, Y_2)
        g2.drawLine(X_2, Y_1, X_1, Y_2)

        return image
    }



    /**
     * we have to remove the active renderer BEFORE we set the visibility status.
     */
    fun updatePositionsPre(component: java.awt.Component, notify: NotifyType<T>, visible: Boolean) {
        if (!visible) {
            LAFUtil.removePopupFromMap(notify)
            SwingActiveRender.remove(component)
        }
    }

    /**
     * when using active rendering, we have to add it AFTER we have set the visibility status
     */
    fun updatePositionsPost(component: java.awt.Component, notify: NotifyType<T>, visible: Boolean) {
        if (visible) {
            SwingActiveRender.add(component)
            LAFUtil.addPopupToMap(notify)
        }
    }

    fun doShake(count: Int, targetX: Float, targetY: Float)

    fun shake(durationInMillis: Int, amplitude: Int) {
        var i1 = LAFUtil.RANDOM.nextInt((amplitude shl 2) + 1) - amplitude
        var i2 = LAFUtil.RANDOM.nextInt((amplitude shl 2) + 1) - amplitude
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

        doShake(count, i1.toFloat(), i2.toFloat())
    }

    fun cancelShake() {
        if (shakeTween != null) {
            shakeTween!!.cancel()
            shakeTween = null
        }
    }

    fun close()
    fun setVisible(visible: Boolean)
    fun setLocationInternal(x: Int, y: Int)
    fun refresh()
    fun setupHide()
    fun cancelHide() {
        if (hideTween != null) {
            hideTween!!.cancel()
            hideTween = null
        }
    }

    fun setupMove(y: Float)
    fun cancelMove() {
        if (moveTween != null) {
            moveTween!!.cancel() // cancel does its thing on the next tick of animation cycle
            moveTween = null
        }
    }
}
