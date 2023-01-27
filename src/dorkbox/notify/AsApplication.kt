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

import java.awt.Frame
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.WindowStateListener
import javax.swing.JPanel

// this is a child to a Jframe/window (instead of globally to the screen).
internal class AsApplication internal constructor(
    private val notification: Notify,
    private val notifyCanvas: NotifyCanvas,) : NotifyType {

    companion object {
        private const val glassPanePrefix = "dorkbox.notify"
    }

    private val window = notification.attachedFrame!!

    private val parentListener: ComponentListener
    private val windowStateListener: WindowStateListener
    private var glassPane: JPanel

    // NOTE: this is on the swing EDT
    init {
        // this makes sure that our notify canvas stay anchored to the parent window (if it's hidden/shown/moved/etc)
        parentListener = object : ComponentListener {
            override fun componentShown(e: ComponentEvent) {
                notification.notifyLook?.reLayout(window.bounds)
            }

            override fun componentHidden(e: ComponentEvent) {}

            override fun componentResized(e: ComponentEvent) {
                notification.notifyLook?.reLayout(window.bounds)
            }

            override fun componentMoved(e: ComponentEvent) {}
        }

        windowStateListener = WindowStateListener { e ->
            val state = e.newState
            if (state and Frame.ICONIFIED == 0) {
                notification.notifyLook?.reLayout(window.bounds)
            }
        }

        window.addWindowStateListener(windowStateListener)
        window.addComponentListener(parentListener)

        val pane = window.glassPane
        if (pane is JPanel) {
            glassPane = pane
            val name = glassPane.name
            if (name != glassPanePrefix) {
                // We just tweak the already existing glassPane, instead of replacing it with our own
                // glassPane = new JPanel();
                glassPane.layout = null
                glassPane.name = glassPanePrefix
                // glassPane.setSize(appWindow.getSize());
                // glassPane.setOpaque(false);
                // appWindow.setGlassPane(glassPane);
            }

            glassPane.add(notifyCanvas)

            if (!glassPane.isVisible) {
                glassPane.isVisible = true
            }
        } else {
            throw RuntimeException("Not able to add the notification to the window glassPane")
        }
    }

    override fun setVisible(visible: Boolean, look: LookAndFeel) {
        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPre(visible)
        look.updatePositionsPost(visible)
    }

    // called on the Swing EDT.
    override fun close() {
        glassPane.remove(notifyCanvas)
        window.removeWindowStateListener(windowStateListener)
        window.removeComponentListener(parentListener)

        var found = false
        val components = glassPane.components
        for (component in components) {
            if (component is NotifyCanvas) {
                found = true
                break
            }
        }

        if (!found) {
            // hide the glass pane if there are no more notifications on it.
            glassPane.isVisible = false
        }
    }
}
