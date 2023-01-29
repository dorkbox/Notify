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
import dorkbox.notify.LAFUtil.growDown
import dorkbox.notify.LAFUtil.popups
import java.awt.Rectangle
import java.awt.Window

internal class AsApplicationLAF(notification: Notify, notifyCanvas: NotifyCanvas, parent: Window, parentBounds: Rectangle):
    LookAndFeel(parent, notifyCanvas, notification) {

    override val isDesktop = false

    init {
        idAndPosition = parent.name + ":" + position

        anchorX = getAnchorX(position, parentBounds)
        anchorY = getAnchorY(position, parentBounds)
    }

    @Suppress("DuplicatedCode")
    private fun getAnchorX(position: Position, bounds: Rectangle): Int {
        // we use the screen that the mouse is currently on.
        val startX = 0
        val screenWidth = bounds.getWidth().toInt()

        return when (position) {
            Position.TOP_LEFT, Position.BOTTOM_LEFT -> LAFUtil.MARGIN + startX
            Position.CENTER -> startX + screenWidth / 2 - NotifyCanvas.WIDTH / 2 - LAFUtil.MARGIN / 2
            Position.TOP_RIGHT, Position.BOTTOM_RIGHT -> startX + screenWidth - NotifyCanvas.WIDTH - LAFUtil.MARGIN
        }
    }

    private fun getAnchorY(position: Position, bounds: Rectangle): Int {
        val startY = 0
        val screenHeight = bounds.getHeight().toInt()

        return when (position) {
            Position.TOP_LEFT, Position.TOP_RIGHT -> startY + LAFUtil.MARGIN
            Position.CENTER -> startY + screenHeight / 2 - NotifyCanvas.HEIGHT / 2 - LAFUtil.MARGIN / 2 - SPACER
            Position.BOTTOM_LEFT, Position.BOTTOM_RIGHT -> screenHeight - NotifyCanvas.HEIGHT - LAFUtil.MARGIN - SPACER * 2
        }
    }


    // only called from an application
    override fun reLayout(bounds: Rectangle) {
        // when the parent window moves, we stop all animation and snap the popup into place. This simplifies logic greatly
        anchorX = getAnchorX(position, bounds)
        anchorY = getAnchorY(position, bounds)

        val growDown = growDown(this)

        if (tween != null) {
            tween!!.cancel() // cancel does its thing on the next tick of animation cycle
            tween = null
        }


        var changedY: Int
        if (popupIndex == 0) {
            changedY = anchorY
        } else {
            synchronized(popups) {
                val id = idAndPosition
                val looks = popups[id]
                changedY = if (looks != null) {
                    if (growDown) {
                        anchorY + popupIndex * (NotifyCanvas.HEIGHT + SPACER)
                    } else {
                        anchorY - popupIndex * (NotifyCanvas.HEIGHT + SPACER)
                    }
                } else {
                    anchorY
                }
            }
        }

        setLocation(anchorX, changedY)
    }

    override var y: Int
        get() = notifyCanvas.y
        set(y) {
            notifyCanvas.setLocation(notifyCanvas.x, y)
        }

    override val x: Int
        get() = notifyCanvas.x

    override fun setLocation(x: Int, y: Int) {
        notifyCanvas.setLocation(x, y)
    }
}
