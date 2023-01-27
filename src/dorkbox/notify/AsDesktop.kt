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

import dorkbox.util.ScreenUtil
import dorkbox.util.SwingUtil
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import javax.swing.ImageIcon
import javax.swing.JWindow

// we can't use regular popup, because if we have no owner, it won't work!
// instead, we just create a JWindow and use it to hold our content
class AsDesktop internal constructor(private val notification: Notify, image: ImageIcon?, theme: Theme) : JWindow(), INotify {
    companion object {
        private const val serialVersionUID = 1L
    }

    private val look: LookAndFeel

    // this is on the swing EDT
    init {
        isAlwaysOnTop = true

        preferredSize = Dimension(WIDTH, HEIGHT)
        maximumSize = preferredSize
        minimumSize = preferredSize

        setSize(NotifyCanvas.WIDTH, NotifyCanvas.HEIGHT)
        setLocation(Short.MIN_VALUE.toInt(), Short.MIN_VALUE.toInt())

        val device = if (notification.screenNumber == Short.MIN_VALUE.toInt()) {
            // set screen position based on mouse
            val mouseLocation = MouseInfo.getPointerInfo().location
            ScreenUtil.getMonitorAtLocation(mouseLocation)
        } else {
            // set screen position based on specified screen
            var screenNumber = notification.screenNumber
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val screenDevices = ge.screenDevices

            if (screenNumber < 0) {
                screenNumber = 0
            } else if (screenNumber > screenDevices.size - 1) {
                screenNumber = screenDevices.size - 1
            }

            screenDevices[screenNumber]
        }

        val bounds = device.defaultConfiguration.bounds


        val notifyCanvas = NotifyCanvas(this, notification, image, theme)
        contentPane.add(notifyCanvas)

        look = LookAndFeel(this, this, notifyCanvas, notification, bounds, true)
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
        // was it already visible?
        if (visible == isVisible) {
            // prevent "double setting" visible state
            return
        }

        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPre(visible)
        super.setVisible(visible)

        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPost(visible)
        if (visible) {
            toFront()
        }
    }

    // setVisible(false) with any extra logic
    fun doHide() {
        super.setVisible(false)
    }

    override fun close() {
        // this must happen in the Swing EDT. This is usually called by the active renderer
        SwingUtil.invokeLater {
            doHide()
            look.close()
            removeAll()
            dispose()
            notification.onClose()
        }
    }
}
