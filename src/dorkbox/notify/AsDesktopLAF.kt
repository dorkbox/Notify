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

import dorkbox.notify.LAFUtil.SPACER
import dorkbox.notify.LAFUtil.windowCloseListener
import dorkbox.util.ScreenUtil
import java.awt.Point
import java.awt.Rectangle
import java.awt.Window

internal class AsDesktopLAF(notification: Notify, notifyCanvas: NotifyCanvas, private val parent: Window, parentBounds: Rectangle)
    : LookAndFeel(parent, notifyCanvas, notification) {

    override val isDesktop = true

    init {
        parent.addWindowListener(windowCloseListener)

        val point = Point(parentBounds.getX().toInt(), parentBounds.getY().toInt())
        idAndPosition = ScreenUtil.getMonitorNumberAtLocation(point).toString() + ":" + position

        anchorX = getAnchorX(position, parentBounds)
        anchorY = getAnchorY(position, parentBounds)
    }

    private fun getAnchorX(position: Position, bounds: Rectangle): Int {
        // we use the screen that the mouse is currently on.
        val startX = bounds.getX().toInt()
        val screenWidth = bounds.getWidth().toInt()

        return when (position) {
            Position.TOP_LEFT, Position.BOTTOM_LEFT -> LAFUtil.MARGIN + startX
            Position.CENTER -> startX + screenWidth / 2 - NotifyCanvas.WIDTH / 2 - LAFUtil.MARGIN / 2
            Position.TOP_RIGHT, Position.BOTTOM_RIGHT -> startX + screenWidth - NotifyCanvas.WIDTH - LAFUtil.MARGIN
        }
    }

    private fun getAnchorY(position: Position, bounds: Rectangle): Int {
        val startY = bounds.getY().toInt()
        val screenHeight = bounds.getHeight().toInt()

        return when (position) {
            Position.TOP_LEFT, Position.TOP_RIGHT -> startY + LAFUtil.MARGIN
            Position.CENTER -> startY + screenHeight / 2 - NotifyCanvas.HEIGHT / 2 - LAFUtil.MARGIN / 2 - SPACER
            Position.BOTTOM_LEFT, Position.BOTTOM_RIGHT -> startY + screenHeight - NotifyCanvas.HEIGHT - LAFUtil.MARGIN
        }
    }

    override var y: Int
        get() = parent.y
        set(y) {
            parent.setLocation(parent.x, y)
        }

    override val x: Int
        get() = parent.x

    override fun setLocation(x: Int, y: Int) {
        parent.setLocation(x, y)
    }
}
