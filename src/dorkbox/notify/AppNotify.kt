/*
 * Copyright 2023 dorkbox, llc
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
import dorkbox.tweenEngine.Tween
import dorkbox.tweenEngine.TweenEquations
import dorkbox.tweenEngine.TweenEvents
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.UIManager

// this is a child to a Jframe/window (instead of globally to the screen).
internal class AppNotify(override val notification: Notify): Canvas(), NotifyType<AppNotify> {

    companion object {
        private const val glassPanePrefix = "dorkbox.notify"

        // the state of the glass pane BEFORE we modified it.
        private val previousStates = mutableMapOf<JFrame, Pair<String, Boolean>>()

        private val tweenAccessor = AppAccessor()
        private val mouseListener: MouseAdapter = AppMouseAdapter()

        @Suppress("DuplicatedCode")
        private fun getAnchorX(position: Position, bounds: Rectangle): Int {
            // we use the screen that the mouse is currently on.
            val startX = 0
            val screenWidth = bounds.getWidth().toInt()

            return when (position) {
                // LEFT ALIGN
                Position.TOP_LEFT, Position.BOTTOM_LEFT -> Notify.MARGIN + startX
                // CENTER ALIGN
                Position.TOP, Position.CENTER, Position.BOTTOM -> startX + screenWidth / 2 - Notify.WIDTH / 2 - Notify.MARGIN / 2
                // RIGHT ALIGN
                Position.TOP_RIGHT, Position.BOTTOM_RIGHT -> startX + screenWidth - Notify.WIDTH - Notify.MARGIN
            }
        }

        private fun getAnchorY(position: Position, bounds: Rectangle): Int {
            val startY = 0
            val screenHeight = bounds.getHeight().toInt()

            return when (position) {
                // TOP ALIGN
                Position.TOP_LEFT, Position.TOP, Position.TOP_RIGHT -> startY + Notify.MARGIN
                // CENTER ALIGN
                Position.CENTER -> startY + screenHeight / 2 - Notify.HEIGHT / 2 - Notify.MARGIN / 2 - Notify.SPACER
                // BOTTOM ALIGN
                Position.BOTTOM_LEFT, Position.BOTTOM, Position.BOTTOM_RIGHT -> screenHeight - Notify.HEIGHT - Notify.MARGIN - Notify.SPACER * 2
            }
        }
    }


    private lateinit var cachedImage: BufferedImage
    private lateinit var cachedClose: BufferedImage
    private lateinit var cachedCloseEnabled: BufferedImage

    // for the progress bar. we directly draw this onscreen
    // non-volatile because it's always accessed in the active render thread
    override var progress = 0

    override var shakeTween: Tween<AppNotify>? = null
    override var moveTween: Tween<AppNotify>? = null
    override var hideTween: Tween<AppNotify>? = null

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    override var idAndPosition = ""
    override var popupIndex = 0

    override var anchorX = 0
    override var anchorY = 0

    // the ONLY reason "shake" works, is because we configure the target X/Y location for moving based on where we WANT the popup to go
    // and completely ignoring its current position
    var shakeX = 0
    var shakeY = 0


    @Volatile
    var mouseY = 0
    @Volatile
    var mouseX = 0



    @Volatile
    var lastMousePosition = Point(0,0)

    @Volatile
    var lastMouseX = 0

    @Volatile
    var lastMouseY = 0



    private val parent = notification.attachedFrame!!
    private var glassPane: JPanel

    // this is on the swing EDT
    init {
        addMouseListener(mouseListener)
        addMouseMotionListener(mouseListener)

        val actualSize = Dimension(Notify.WIDTH, Notify.HEIGHT)

        preferredSize = actualSize
        maximumSize = actualSize
        minimumSize = actualSize
        size = actualSize

        super.setVisible(false)
        isFocusable = true
        background = UIManager.getColor("Panel.background")


        idAndPosition = parent.name + ":" + notification.position

        anchorX = getAnchorX(notification.position, parent.bounds)
        anchorY = getAnchorY(notification.position, parent.bounds)

        val pane = parent.glassPane
        if (pane is JPanel) {
            glassPane = pane
            val name = glassPane.name
            if (name != glassPanePrefix) {
                // We just tweak the already existing glassPane, instead of replacing it with our own
                // NOTE: This could cause problems!

                // glassPane = new JPanel();
                glassPane.layout = null
                glassPane.name = glassPanePrefix

                previousStates[parent] = Pair(name, glassPane.isVisible)

                if (!glassPane.isVisible) {
                    glassPane.isVisible = true
                }

                // glassPane.setSize(appWindow.getSize());
                // glassPane.setOpaque(false);
                // appWindow.setGlassPane(glassPane);
            }

            // add our "notify canvas" drawing element to the parent JFrame (or other component)
            glassPane.add(this)
        } else {
            throw RuntimeException("Not able to add the notification to the window glassPane")
        }

        // now we setup the rendering of the image
        refresh()

        setLocation(anchorX, anchorY)
    }

    override fun refresh() {
        cachedImage = renderBackgroundInfo(notification.title, notification.text, notification.image)
        cachedClose = renderCloseButton(false)
        cachedCloseEnabled = renderCloseButton(true)

        idAndPosition = parent.name + ":" + notification.position

        anchorX = getAnchorX(notification.position, parent.bounds)
        anchorY = getAnchorY(notification.position, parent.bounds)
    }

    override fun paint(gIgnore: Graphics) {
        // Get the graphics context from the buffer strategy
        val bufferStrategy = bufferStrategy

        val g = bufferStrategy.drawGraphics

        g.clearRect(0, 0, width, height) // Clear the screen

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

        // Dispose the graphics context and show the buffer
        g.dispose()
        bufferStrategy.show()
    }



    private fun draw(g: Graphics) {
        g.drawImage(cachedImage, 0, 0, null)

        val pad = 2

        if (!notification.hideCloseButton) {
            // there is a SMALL problem, where if we move the mouse fast enough, we can "trick" the close button into thinking that the
            // mouse is still over it.

            // if the mouse has moved on the screen, but our mouseX/Y  __has not changed__ then we know that the mouse is outside of our
            // component and we should HIDE the close button.
            if (mouseX >= NotifyType.X_1-pad && mouseX <= Notify.WIDTH-pad &&
                mouseY >= 2 && mouseY <= NotifyType.Y_2+5) {

                // now do the extra check!
                val currentMouse = MouseInfo.getPointerInfo().location
                if (lastMouseX == mouseX && lastMouseY == mouseY &&
                    lastMousePosition != currentMouse) {

                    lastMouseX = mouseX
                    lastMouseY = mouseY
                    g.drawImage(cachedClose, 0, 0, null)
                } else {
                    g.drawImage(cachedCloseEnabled, 0, 0, null)
                }


            } else {
                g.drawImage(cachedClose, 0, 0, null)
            }
        }

        // the progress bar can change, so we always draw it every time
        if (progress > 0) {
            // draw the progress bar along the bottom
            g.color = UIManager.getColor("ProgressBar.foreground")
            g.fillRect(0, Notify.HEIGHT - 3, progress, 2) // "-3" to account for the border
        }
    }

    fun onClick(x: Int, y: Int) {
        // Check - we were over the 'X' (and thus no notify), or was it in the general area?
        val isClickOnCloseButton = !notification.hideCloseButton && x >= (Notify.WIDTH - 20) && y <= 20

        if (isClickOnCloseButton) {
            // we always close the notification popup
            notification.onClose()
        } else {
            // only call the general click handler IF we click in the general area!
            notification.onClickAction()
        }
    }

    override fun setupHide() {
        if (hideTween == null && notification.hideAfterDurationInMillis > 0) {
            // begin a timeline to get rid of the popup (default is 5 seconds)
            val tween = tweenEngine
                .to(this, AppAccessor.PROGRESS, tweenAccessor, notification.hideAfterDurationInMillis / 1000.0f)
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
                .to(this, AppAccessor.Y_POS, tweenAccessor, Notify.MOVE_DURATION)
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
            shakeTween!!.valueRelative(targetX, targetY)
                .repeatAutoReverse(count, 0f)
        } else {
            val tween = tweenEngine
                .to(this, AppAccessor.SHAKE, tweenAccessor, 0.05f)
                .valueRelative(targetX, targetY)
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
    }

    // this is called during parent initialization (before we are initialized), so we cannot access objects here properly!
    // NOTE: This is present here because DesktopNotify requires it (and consistency is important)
//    override fun setLocation(x: Int, y: Int) {
//        super.setLocation(x, y)
//    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)

        // this is because the order of operations are different based upon visibility.
        updatePositionsPre(this, this, visible)
        updatePositionsPost(this, this, visible)
    }

    // called on the Swing EDT.
    override fun close() {
        cancelMove()
        cancelHide()
        cancelShake()

        glassPane.remove(this)

        removeMouseMotionListener(mouseListener)
        removeMouseListener(mouseListener)

        updatePositionsPre(component = this, notify = this, visible = false)

        // revert the glass pane if there are no more notifications on it.
        var found = false
        val components = glassPane.components
        for (component in components) {
            if (component is AppNotify) {
                found = true
                break
            }
        }

        if (!found) {
            val (name, visibility) = previousStates.remove(parent)!!
            glassPane.name = name
            glassPane.isVisible = visibility
        }
    }
}
