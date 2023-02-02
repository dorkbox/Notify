/*
 * Copyright 2017 dorkbox, llc
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

import dorkbox.tweenEngine.Tween
import dorkbox.tweenEngine.TweenEquations
import dorkbox.tweenEngine.TweenEvents
import dorkbox.util.SwingUtil
import java.awt.Canvas
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.MouseAdapter
import java.awt.image.BufferedImage
import javax.swing.JFrame
import javax.swing.JPanel

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
                Position.TOP_LEFT, Position.BOTTOM_LEFT -> Notify.MARGIN + startX
                Position.CENTER -> startX + screenWidth / 2 - Notify.WIDTH / 2 - Notify.MARGIN / 2
                Position.TOP_RIGHT, Position.BOTTOM_RIGHT -> startX + screenWidth - Notify.WIDTH - Notify.MARGIN
            }
        }

        private fun getAnchorY(position: Position, bounds: Rectangle): Int {
            val startY = 0
            val screenHeight = bounds.getHeight().toInt()

            return when (position) {
                Position.TOP_LEFT, Position.TOP_RIGHT -> startY + Notify.MARGIN
                Position.CENTER -> startY + screenHeight / 2 - Notify.HEIGHT / 2 - Notify.MARGIN / 2 - Notify.SPACER
                Position.BOTTOM_LEFT, Position.BOTTOM_RIGHT -> screenHeight - Notify.HEIGHT - Notify.MARGIN - Notify.SPACER * 2
            }
        }
    }


    private lateinit var cachedImage: BufferedImage
    private lateinit var cachedClose: BufferedImage
    private lateinit var cachedCloseEnabled: BufferedImage

    // for the progress bar. we directly draw this onscreen
    // non-volatile because it's always accessed in the active render thread
    override var progress = 0

    @Volatile
    var mouseOver = false

    override var shakeTween: Tween<AppNotify>? = null
    override var moveTween: Tween<AppNotify>? = null
    override var hideTween: Tween<AppNotify>? = null

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    override var idAndPosition = ""
    override var popupIndex = 0

    override var anchorX = 0
    override var anchorY = 0


    @Volatile
    var mouseY = 0
    @Volatile
    var mouseX = 0

    private val parent = notification.attachedFrame!!
    private var glassPane: JPanel

    // this makes sure that our notify canvas stay anchored to the parent window (if it's hidden/shown/moved/etc)
    private val parentListener = AppComponentListener(this)
    private val windowStateListener = AppWindowStateListener(this)


    // this is on the swing EDT
    init {
        val actualSize = Dimension(Notify.WIDTH, Notify.HEIGHT)

        preferredSize = actualSize
        maximumSize = actualSize
        minimumSize = actualSize
        size = actualSize

        isFocusable = true
        background = notification.theme.panel_BG


        addMouseListener(mouseListener)
        addMouseMotionListener(mouseListener)


        idAndPosition = parent.name + ":" + notification.position

        anchorX = getAnchorX(notification.position, parent.bounds)
        anchorY = getAnchorY(notification.position, parent.bounds)

        parent.addWindowStateListener(windowStateListener)
        parent.addComponentListener(parentListener)

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
    }

    override fun getX(): Int {
        return super.getX()
    }

    override fun getY(): Int {
        return super.getY()
    }


    // when the parent window moves, we stop all animation and snap the popup into place. This simplifies logic greatly
    fun reLayout() {
        val bounds = parent.bounds
        anchorX = getAnchorX(notification.position, bounds)
        anchorY = getAnchorY(notification.position, bounds)
//
//        val growDown = growDown(this)
//
//        if (tween != null) {
//            tween!!.cancel() // cancel does its thing on the next tick of animation cycle
//            tween = null
//        }
//
//
//        var changedY: Int
//        if (popupIndex == 0) {
//            changedY = anchorY
//        } else {
//            synchronized(popups) {
//                val id = idAndPosition
//                val looks = popups[id]
//                changedY = if (looks != null) {
//                    if (growDown) {
//                        anchorY + popupIndex * (NotifyCanvas.HEIGHT + SPACER)
//                    } else {
//                        anchorY - popupIndex * (NotifyCanvas.HEIGHT + SPACER)
//                    }
//                } else {
//                    anchorY
//                }
//            }
//        }
//
//        setLocation(anchorX, changedY)
    }

    override fun refresh() {
        cachedImage = renderBackgroundInfo(notification.title, notification.text, notification.theme, notification.image)
        cachedClose = renderCloseButton(notification.theme, false)
        cachedCloseEnabled = renderCloseButton(notification.theme, true)

        idAndPosition = parent.name + ":" + notification.position

        val growDown = LAFUtil.growDown(this)

        val offset = if (growDown) {
            Notify.MARGIN
        } else {
            -Notify.MARGIN
        }

        anchorX = getAnchorX(notification.position, parent.bounds)
        anchorY = getAnchorY(notification.position, parent.bounds)
    }

    override fun paint(g: Graphics) {
        // we cache the text + image (to an image), the two stats of the close "button" and then always render the close + progressbar

        // use our cached image, so we don't have to re-render text/background/etc
        try {
            g.drawImage(cachedImage, 0, 0, null)

//            println("$mouseX, $mouseY")

            if (mouseOver && mouseX >= 280 && mouseY <= 20) {
                g.drawImage(cachedCloseEnabled, 0, 0, null)
            } else {
                g.drawImage(cachedClose, 0, 0, null)
            }
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
                g.drawImage(cachedImage, 0, 0, null)

                if (mouseOver && mouseX >= NotifyType.closeX && mouseY <= 20) {
                    g.drawImage(cachedCloseEnabled, 0, 0, null)
                } else {
                    g.drawImage(cachedClose, 0, 0, null)
                }
            } catch (ignored2: Exception) {
            }
        }

        // the progress bar can change, so we always draw it every time
        if (progress > 0) {
            // draw the progress bar along the bottom
            g.color = notification.theme.progress_FG
            g.fillRect(0, Notify.HEIGHT - 2, progress, 2)
        }
    }

    override fun setupHide() {
        if (hideTween == null && notification.hideAfterDurationInMillis > 0) {
            // begin a timeline to get rid of the popup (default is 5 seconds)
            hideTween = LAFUtil.tweenEngine.to(this, AppAccessor.PROGRESS, tweenAccessor, notification.hideAfterDurationInMillis / 1000.0f)
                .value(Notify.WIDTH.toFloat())
                .ease(TweenEquations.Linear)
                .addCallback(TweenEvents.COMPLETE) {
                    SwingUtil.invokeLater {
                        notification.onClose()
                    }
                }
                .start()
        }
    }

    override fun setupMove(y: Float) {
        if (moveTween != null) {
            moveTween!!.value(y)
        } else {
            val tween = LAFUtil.tweenEngine
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
            val tween = LAFUtil.tweenEngine
                .to(this, AppAccessor.X_Y_POS, tweenAccessor, 0.05f)
                .valueRelative(targetX, targetY)
                .repeatAutoReverse(count, 0f)
                .ease(TweenEquations.Linear)

            shakeTween = tween
            tween.start()
        }
    }

    fun onClick(x: Int, y: Int) {
        // this must happen in the Swing EDT. This is usually called by the active renderer
        SwingUtil.invokeLater {
            // Check - we were over the 'X' (and thus no notify), or was it in the general area?

            val isClickOnCloseButton = !notification.hideCloseButton && x >= 280 && y <= 20

            // reasonable position for detecting mouse over
            if (!isClickOnCloseButton) {
                // only call the general click handler IF we click in the general area!
                notification.onClickAction()
            } else {
                // we always close the notification popup
                notification.onClose()
            }
        }
    }

    override fun setLocationInternal(x: Int, y: Int) {
        setLocation(x, y)
    }

    // this is called during parent initialization (before we are initialized), so we cannot access objects here properly!
//    override fun setLocation(x: Int, y: Int) {
//        super.setLocation(x, y)
//    }

    override fun setVisible(visible: Boolean) {
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

        parent.removeWindowStateListener(windowStateListener)
        parent.removeComponentListener(parentListener)

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
