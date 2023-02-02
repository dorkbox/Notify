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

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

internal class AppMouseAdapter : MouseAdapter() {

    override fun mouseEntered(e: MouseEvent) {
        val notifyCanvas = e.source as AppNotify
        notifyCanvas.mouseOver = true
    }

    override fun mouseExited(e: MouseEvent) {
        val notifyCanvas = e.source as AppNotify
        notifyCanvas.mouseOver = false
    }

    override fun mouseMoved(e: MouseEvent) {
        val notifyCanvas = e.source as AppNotify
        notifyCanvas.mouseX = e.x
        notifyCanvas.mouseY = e.y
    }

    override fun mouseDragged(e: MouseEvent) {
    }

    override fun mouseReleased(e: MouseEvent) {
        val notifyCanvas = e.source as AppNotify
        notifyCanvas.onClick(e.x, e.y)
    }
}
