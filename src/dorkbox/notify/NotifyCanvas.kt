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

import java.awt.BasicStroke
import java.awt.Canvas
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Stroke
import java.awt.image.BufferedImage
import javax.swing.ImageIcon
import javax.swing.JLabel

internal class NotifyCanvas(
    val parent: INotify,
    private val notification: Notify,
    private val imageIcon: ImageIcon?,
    private val theme: Theme
) : Canvas() {

    private val showCloseButton: Boolean
    private var cachedImage: BufferedImage

    // for the progress bar. we directly draw this onscreen
    // non-volatile because it's always accessed in the active render thread
    var progress = 0

    init {
        val preferredSize = Dimension(WIDTH, HEIGHT)
        setPreferredSize(preferredSize)
        maximumSize = preferredSize
        minimumSize = preferredSize
        setSize(WIDTH, HEIGHT)

        isFocusable = false
        background = theme.panel_BG
        showCloseButton = !notification.hideCloseButton

        // now we setup the rendering of the image
        cachedImage = renderBackgroundInfo(notification.title, notification.text, theme, imageIcon)
    }

    override fun paint(g: Graphics) {
        // we cache the text + image (to another image), and then always render the close + progressbar

        // use our cached image, so we don't have to re-render text/background/etc
        try {
            g.drawImage(cachedImage, 0, 0, null)
        } catch (ignored: Exception) {
            // have also seen (happened after screen/PC was "woken up", in Xubuntu 16.04):
            // java.lang.ClassCastException:sun.awt.image.BufImgSurfaceData cannot be cast to sun.java2d.xr.XRSurfaceData at sun.java2d.xr.XRPMBlitLoops.cacheToTmpSurface(XRPMBlitLoops.java:148)
            // at sun.java2d.xr.XrSwToPMBlit.Blit(XRPMBlitLoops.java:356)
            // at sun.java2d.SurfaceDataProxy.updateSurfaceData(SurfaceDataProxy.java:498)
            // at sun.java2d.SurfaceDataProxy.replaceData(SurfaceDataProxy.java:455)
            // at sun.java2d.SurfaceData.getSourceSurfaceData(SurfaceData.java:233)
            // at sun.java2d.pipe.DrawImage.renderImageCopy(DrawImage.java:566)
            // at sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:67)
            // at sun.java2d.pipe.DrawImage.copyImage(DrawImage.java:1014)
            // at sun.java2d.pipe.ValidatePipe.copyImage(ValidatePipe.java:186)
            // at sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3318)
            // at sun.java2d.SunGraphics2D.drawImage(SunGraphics2D.java:3296)
            // at dorkbox.notify.NotifyCanvas.paint(NotifyCanvas.java:92)

            // redo the image
            cachedImage = renderBackgroundInfo(notification.title, notification.text, theme, imageIcon)

            // try to draw again
            try {
                g.drawImage(cachedImage, 0, 0, null)
            } catch (ignored2: Exception) {
            }
        }

        // the progress bar and close button are the only things that can change, so we always draw them every time
        val g2 = g.create() as Graphics2D
        try {
            if (showCloseButton) {
                // manually draw the close button
                val g3 = g.create() as Graphics2D
                g3.color = theme.panel_BG
                g3.stroke = stroke

                val p = mousePosition

                // reasonable position for detecting mouse over
                if (p != null && p.getX() >= 280 && p.getY() <= 20) {
                    g3.color = Color.RED
                } else {
                    g3.color = theme.closeX_FG
                }

                // draw the X
                g3.drawLine(X_1, Y_1, X_2, Y_2)
                g3.drawLine(X_2, Y_1, X_1, Y_2)
            }

            // draw the progress bar along the bottom
            g2.color = theme.progress_FG
            g2.fillRect(0, PROGRESS_HEIGHT, progress, 2)
        } finally {
            g2.dispose()
        }
    }

    /**
     * @return TRUE if we were over the 'X' or FALSE if the click was in the general area (and not over the 'X').
     */
    fun isCloseButton(x: Int, y: Int): Boolean {
        return showCloseButton && x >= 280 && y <= 20
    }

    companion object {
        private val stroke: Stroke = BasicStroke(2f)

        private const val closeX = 282
        private const val closeY = 2

        private const val Y_1 = closeY + 5
        private const val X_1 = closeX + 5
        private const val Y_2 = closeY + 11
        private const val X_2 = closeX + 11

        const val WIDTH = 300
        const val HEIGHT = 87
        private const val PROGRESS_HEIGHT = HEIGHT - 2
        private fun renderBackgroundInfo(title: String, notificationText: String, theme: Theme, imageIcon: ImageIcon?): BufferedImage {

            val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
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
                g2.fillRect(0, 0, WIDTH, HEIGHT)

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
                var length = notificationText.length
                val text = StringBuilder(length)

                // are we "html" already? just check for the starting tag and strip off END html tag
                if (length >= 13 && notificationText.regionMatches(length - 7, "</html>", 0, 7, ignoreCase = true)) {
                    text.append(notificationText)
                    text.delete(text.length - 7, text.length)
                    length -= 7
                } else {
                    text.append("<html>")
                    text.append(notificationText)
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
                mainTextLabel.setBounds(0, 0, WIDTH - posX - 2, HEIGHT)

                g2.translate(posX, posY)
                mainTextLabel.paint(g2)
                g2.translate(-posX, -posY)
            } finally {
                g2.dispose()
            }
            return image
        }
    }
}
