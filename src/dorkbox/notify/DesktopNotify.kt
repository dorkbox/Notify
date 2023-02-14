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

import dorkbox.notify.LAFUtil.tweenEngine
import dorkbox.notify.Notify.Companion.MOVE_DURATION
import dorkbox.tweenEngine.Tween
import dorkbox.tweenEngine.TweenEquations
import dorkbox.tweenEngine.TweenEvents
import dorkbox.util.ScreenUtil
import java.awt.Dimension
import java.awt.Graphics
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Rectangle
import java.awt.Toolkit
import java.awt.image.BufferedImage
import javax.swing.JWindow

// we can't use regular popup, because if we have no owner, it won't work!
// instead, we just create a JWindow and use it to hold our content
internal class DesktopNotify(override val notification: Notify) : JWindow(), NotifyType<DesktopNotify> {
    companion object {
        private val tweenAccessor = DesktopAccessor()

        private val windowCloseListener = DesktopWindowCloseAdapter()
        private val mouseListener = DesktopMouseAdapter()


        @Suppress("DuplicatedCode")
        private fun getAnchorX(position: Position, bounds: Rectangle): Int {
            // we use the screen that the mouse is currently on.
            val startX = bounds.getX().toInt()
            val screenWidth = bounds.getWidth().toInt()

            return when (position) {
                Position.TOP_LEFT, Position.BOTTOM_LEFT -> Notify.MARGIN + startX
                Position.CENTER -> startX + screenWidth / 2 - Notify.WIDTH / 2 - Notify.MARGIN / 2
                Position.TOP_RIGHT, Position.BOTTOM_RIGHT -> startX + screenWidth - Notify.WIDTH - Notify.MARGIN
            }
        }

        private fun getAnchorY(position: Position, bounds: Rectangle): Int {
            val startY = bounds.getY().toInt()
            val screenHeight = bounds.getHeight().toInt()

            return when (position) {
                Position.TOP_LEFT, Position.TOP_RIGHT -> startY + Notify.MARGIN
                Position.CENTER -> startY + screenHeight / 2 - Notify.HEIGHT / 2 - Notify.MARGIN / 2
                Position.BOTTOM_LEFT, Position.BOTTOM_RIGHT -> startY + screenHeight - Notify.HEIGHT - Notify.MARGIN
            }
        }
    }

    private lateinit var cachedImage: BufferedImage
    private lateinit var cachedClose: BufferedImage
    private lateinit var cachedCloseEnabled: BufferedImage

    // for the progress bar. we directly draw this onscreen
    // non-volatile because it's always accessed in the active render thread
    private var prevProgress = 0
    override var progress = 0
        set(value) {
            prevProgress = field
            field = value
        }

    // The button is "hittable" from the entire corner
    private val closeButton: Rectangle

    override var shakeTween: Tween<DesktopNotify>? = null
    override var moveTween: Tween<DesktopNotify>? = null
    override var hideTween: Tween<DesktopNotify>? = null

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    override var idAndPosition = ""
    override var popupIndex = 0

    override var anchorX = 0
    override var anchorY = 0

    // the ONLY reason "shake" works, is because we configure the target X/Y location for moving based on where we WANT the popup to go
    // and completely ignoring its current position
    var shakeX = 0
    var shakeY = 0


    // this is on the swing EDT
    init {
        addWindowListener(windowCloseListener)
        addMouseListener(mouseListener)
        addMouseMotionListener(mouseListener)

        isAlwaysOnTop = true

        preferredSize = Dimension(Notify.WIDTH, Notify.HEIGHT)
        maximumSize = Dimension(Notify.WIDTH, Notify.HEIGHT)
        minimumSize = Dimension(Notify.WIDTH, Notify.HEIGHT)
        size = Dimension(Notify.WIDTH, Notify.HEIGHT)

        setLocation(Short.MIN_VALUE.toInt(), Short.MIN_VALUE.toInt())

        closeButton = Rectangle(NotifyType.closeX, NotifyType.closeY,
                                18, 18)

        background = notification.theme.panel_BG

        // now we setup the rendering of the image
        refresh()
    }

    override fun refresh() {
        cachedImage = renderBackgroundInfo(notification.title, notification.text, notification.theme, notification.image)
        cachedClose = renderCloseButton(notification.theme, false)
        cachedCloseEnabled = renderCloseButton(notification.theme, true)

        val bounds = LAFUtil.getGraphics(notification.screen)
        val point = Point(bounds.getX().toInt(), bounds.getY().toInt())
        idAndPosition = ScreenUtil.getMonitorNumberAtLocation(point).toString() + ":" + notification.position

        val growDown = LAFUtil.growDown(this)

        anchorX = getAnchorX(notification.position, bounds)
        anchorY = getAnchorY(notification.position, bounds) + calculateOffset(growDown, point)
    }

    /**
     * have to adjust for offsets when the window-manager has a toolbar that consumes space and prevents overlap.
     */
    private fun calculateOffset(showFromTop: Boolean, point: Point): Int {
        val gc = ScreenUtil.getMonitorAtLocation(point).defaultConfiguration
        val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc)

        if (showFromTop) {
            if (screenInsets.top > 0) {
                return screenInsets.top - Notify.MARGIN
            }
        } else {
            if (screenInsets.bottom > 0) {
                return screenInsets.bottom + Notify.MARGIN
            }
        }

        return 0
    }

    override fun paint(g: Graphics) {
        // we cache the text + image (to an image), the two states of the close "button" and then always render the progressbar
        try {
            draw(g)
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

            // redo the cache
            refresh()

            // try to draw again
            try {
                draw(g)
            } catch (ignored2: Exception) {
            }
        }

        // the progress bar can change (only getting bigger!), so we always draw it when it grows
        if (progress > 0 && prevProgress != progress) {
            // draw the progress bar along the bottom
            g.color = notification.theme.progress_FG
            g.fillRect(0, Notify.HEIGHT - 2, progress, 2)
        }
    }

    private fun draw(g: Graphics) {
        g.drawImage(cachedImage, 0, 0, null)

        if (!notification.hideCloseButton) {
            if (closeButton.contains(MouseInfo.getPointerInfo().location)) {
                g.drawImage(cachedCloseEnabled, 0, 0, null)
            } else {
                g.drawImage(cachedClose, 0, 0, null)
            }
        }
    }

    fun onClick() {
        // Check - we were over the 'X' (and thus no notify), or was it in the general area
        val isClickOnCloseButton = !notification.hideCloseButton && closeButton.contains(MouseInfo.getPointerInfo().location)

        if (isClickOnCloseButton) {
            // we always close the notification popup
            notification.onClose()
        } else {
            // only call the general click handler IF we click in the general area!
            notification.onClickAction()
        }
    }

    override fun setupHide() {
        if (hideTween != null) {
            hideTween!!.value(Notify.WIDTH.toFloat())
        } else if (notification.hideAfterDurationInMillis > 0) {
            // begin a timeline to get rid of the popup (default is 5 seconds)
            val tween = tweenEngine
                .to(this, DesktopAccessor.PROGRESS, tweenAccessor, notification.hideAfterDurationInMillis / 1000.0f)
                .value(Notify.WIDTH.toFloat())
                .ease(TweenEquations.Linear)
                .addCallback(TweenEvents.COMPLETE) {
                    notification.onClose()
                }

            hideTween = tween
            tween.start()
        }
    }

    override fun setupMove(y: Float) {
        if (moveTween != null) {
            moveTween!!.value(y)
        } else {
            val tween = tweenEngine
                .to(this, DesktopAccessor.Y_POS, tweenAccessor, MOVE_DURATION)
                .value(y)
                .ease(TweenEquations.Linear)
                .addCallback(TweenEvents.COMPLETE) {
                    // make sure to remove the tween once it's done, otherwise .cancel can do weird things.
                    moveTween = null
                }

            moveTween = tween
            tween.start()
        }
    }

    override fun doShake(count: Int, targetX: Float, targetY: Float) {
        if (shakeTween != null) {
            shakeTween!!.value(targetX, targetY)
                        .repeatAutoReverse(count, 0f)
        } else {
            val tween = tweenEngine
                .to(this, DesktopAccessor.SHAKE, tweenAccessor, 0.05f)
                .value(targetX, targetY)
                .repeatAutoReverse(count, 0f)
                .ease(TweenEquations.Linear)

            shakeTween = tween
            tween.start()
        }
    }

    fun setLocationShake(x: Int, y: Int) {
        val x1 = getX() - shakeX
        val y1 = getY() - shakeY
        shakeX = x
        shakeY = y
        setLocationInternal(x1 + x, y1 + y)
    }

    override fun setLocationInternal(x: Int, y: Int) {
        setLocation(x, y)

        // update the location where we detect the close button. The button is "hittable" from the entire corner
        closeButton.setLocation(x + NotifyType.X_1, y)
    }

    // this is called during parent initialization (before we are initialized), so we cannot access objects here properly!
//    override fun setLocation(x: Int, y: Int) {
//        super.setLocation(x, y)
//    }

    override fun setVisible(visible: Boolean) {
        // this is because the order of operations are different based upon visibility.
        updatePositionsPre(this, this, visible)

        super.setVisible(visible)

        // this is because the order of operations are different based upon visibility.
        updatePositionsPost(this, this, visible)

        if (visible) {
            toFront()
        }
    }

    // called on the Swing EDT
    override fun close() {
        cancelMove()
        cancelHide()
        cancelShake()

        removeWindowListener(windowCloseListener)
        removeMouseMotionListener(mouseListener)
        removeMouseListener(mouseListener)

        updatePositionsPre(component = this, notify = this, visible = false)

        super.setVisible(false)

        removeAll()
        dispose()
    }
}
