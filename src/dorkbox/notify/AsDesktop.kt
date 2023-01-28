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

import java.awt.Dimension
import javax.swing.JWindow

// we can't use regular popup, because if we have no owner, it won't work!
// instead, we just create a JWindow and use it to hold our content
internal class AsDesktop internal constructor(val notification: Notify, notifyCanvas: NotifyCanvas) : JWindow(), NotifyType {
    companion object {
        private const val serialVersionUID = 1L
    }

    // this is on the swing EDT
    init {
        isAlwaysOnTop = true

        preferredSize = Dimension(WIDTH, HEIGHT)
        maximumSize = preferredSize
        minimumSize = preferredSize

        setSize(NotifyCanvas.WIDTH, NotifyCanvas.HEIGHT)
        setLocation(Short.MIN_VALUE.toInt(), Short.MIN_VALUE.toInt())

        contentPane.add(notifyCanvas)
    }

    override fun setVisible(visible: Boolean, look: LookAndFeel) {
        // was it already visible?
        if (visible == isVisible) {
            // prevent "double setting" visible state
            return
        }

        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPre(visible)
        super.setVisible(visible)

        // this is because the order of operations are different based upon visibility.
        look.updatePositionsPost(visible, true)

        if (visible) {
            toFront()
        }
    }

    // called on the Swing EDT
    override fun close() {
        super.setVisible(false)
        removeAll()
        dispose()
    }
}
