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

import dorkbox.util.ScreenUtil
import java.awt.Point
import java.awt.Toolkit

/**
 * Contains a list of notification popups + the Y offset (if any)
 */
internal class PopupList {
    var offsetY = 0
        private set

    private val popups = ArrayList<LookAndFeel>(4)

    /**
     * have to adjust for offsets when the window-manager has a toolbar that consumes space and prevents overlap.
     *
     * this is only done on the 2nd popup is added to the list
     */
    fun calculateOffset(showFromTop: Boolean, anchorX: Int, anchorY: Int) {
        if (offsetY == 0) {
            val point = Point(anchorX, anchorY)
            val gc = ScreenUtil.getMonitorAtLocation(point).defaultConfiguration
            val screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc)
            if (showFromTop) {
                if (screenInsets.top > 0) {
                    offsetY = screenInsets.top - LookAndFeel.MARGIN
                }
            } else {
                if (screenInsets.bottom > 0) {
                    offsetY = screenInsets.bottom + LookAndFeel.MARGIN
                }
            }
        }
    }

    fun size(): Int {
        return popups.size
    }

    fun add(lookAndFeel: LookAndFeel) {
        popups.add(lookAndFeel)
    }

    operator fun iterator(): MutableIterator<LookAndFeel> {
        return popups.iterator()
    }

    operator fun get(index: Int): LookAndFeel {
        return popups[index]
    }
}
