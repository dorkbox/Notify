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

import dorkbox.util.SwingUtil
import java.awt.Frame
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.WindowStateListener
import javax.swing.ImageIcon
import javax.swing.JFrame
import javax.swing.JPanel

// this is a child to a Jframe/window (instead of globally to the screen).
class AsApplication internal constructor(private val notification: Notify, image: ImageIcon?,  private val appWindow: JFrame, theme: Theme) : INotify {
    companion object {
        private const val glassPanePrefix = "dorkbox.notify"
    }

    private val look: LookAndFeel
    private val notifyCanvas: NotifyCanvas
    private val parentListener: ComponentListener
    private val windowStateListener: WindowStateListener
    private var glassPane: JPanel? = null

    // NOTE: this is on the swing EDT
    init {
        notifyCanvas = NotifyCanvas(this, notification, image, theme)
        look = LookAndFeel(this, appWindow, notifyCanvas, notification, appWindow.bounds, false)

        // this makes sure that our notify canvas stay anchored to the parent window (if it's hidden/shown/moved/etc)
        parentListener = object : ComponentListener {
            override fun componentShown(e: ComponentEvent) {
                look.reLayout(appWindow.bounds)
            }

            override fun componentHidden(e: ComponentEvent) {}

            override fun componentResized(e: ComponentEvent) {
                look.reLayout(appWindow.bounds)
            }

            override fun componentMoved(e: ComponentEvent) {}
        }

        windowStateListener = WindowStateListener { e ->
            val state = e.newState
            if (state and Frame.ICONIFIED == 0) {
                look.reLayout(appWindow.bounds)
            }
        }

        appWindow.addWindowStateListener(windowStateListener)
        appWindow.addComponentListener(parentListener)


        val glassPane_ = appWindow.glassPane
        if (glassPane_ is JPanel) {
            glassPane = glassPane_
            val name = glassPane_.name
            if (name != glassPanePrefix) {
                // We just tweak the already existing glassPane, instead of replacing it with our own
                // glassPane = new JPanel();
                glassPane_.layout = null
                glassPane_.name = glassPanePrefix
                // glassPane.setSize(appWindow.getSize());
                // glassPane.setOpaque(false);
                // appWindow.setGlassPane(glassPane);
            }

            glassPane_.add(notifyCanvas)

            if (!glassPane_.isVisible) {
                glassPane_.isVisible = true
            }
        } else {
            System.err.println("Not able to add notification to custom glassPane")
        }
    }

    override fun onClick(x: Int, y: Int) {
        look.onClick(x, y)
    }

    /**
     * Shakes the popup
     *
     * @param durationInMillis now long it will shake
     * @param amplitude a measure of how much it needs to shake. 4 is a small amount of shaking, 10 is a lot.
     */
    override fun shake(durationInMillis: Int, amplitude: Int) {
        look.shake(durationInMillis, amplitude)
    }

    override fun setVisible(visible: Boolean) {
        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPre(visible)
        look.updatePositionsPost(visible)
    }

    override fun close() {
        // this must happen in the Swing EDT. This is usually called by the active renderer
        SwingUtil.invokeLater {
            look.close()
            glassPane!!.remove(notifyCanvas)
            appWindow.removeWindowStateListener(windowStateListener)
            appWindow.removeComponentListener(parentListener)

            var found = false
            val components = glassPane!!.components
            for (component in components) {
                if (component is NotifyCanvas) {
                    found = true
                    break
                }
            }

            if (!found) {
                // hide the glass pane if there are no more notifications on it.
                glassPane!!.isVisible = false
            }

            notification.onClose()
        }
    }
}
