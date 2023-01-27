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

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

internal class WindowAdapter : WindowAdapter() {
    override fun windowClosing(e: WindowEvent) {
        if (e.newState != WindowEvent.WINDOW_CLOSED) {
            val source = e.source as AsDesktop
            source.notification.close()
        }
    }

    override fun windowLostFocus(e: WindowEvent) {
        if (e.newState != WindowEvent.WINDOW_CLOSED) {
            val source = e.source as AsDesktop
            // these don't work
            //toFront()
            //requestFocus()
            //requestFocusInWindow()
            source.isAlwaysOnTop = false
            source.isAlwaysOnTop = true
        }
    }
}
