/*
 * Copyright 2023 dorkbox, llc
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

import dorkbox.swingActiveRender.SwingActiveRender
import dorkbox.tweenEngine.TweenEngine.Companion.create
import dorkbox.util.ScreenUtil
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.MouseInfo
import java.awt.Rectangle
import java.util.*

internal object LAFUtil {
    private val popups = mutableMapOf<String, MutableList<NotifyType<*>>>()

    // access is only from a single thread ever, so unsafe is preferred.
    val tweenEngine = create().setCombinedAttributesLimit(2).build()

    // this is for updating the tween engine during active-rendering
    private val animationUpdateHandler: (Long)->Unit = { deltaInNanos ->
        tweenEngine.update(deltaInNanos)
    }

    val RANDOM = Random()

    fun getGraphics(screen: Int): Rectangle {
        var device: GraphicsDevice? = null

        if (screen == Short.MIN_VALUE.toInt()) {
            // set screen position based on mouse
            val mouseLocation = MouseInfo.getPointerInfo().location
            device = ScreenUtil.getMonitorAtLocation(mouseLocation)
        }

        if (device == null) {
            // set screen position based on specified screen
            var screenNumber = screen
            val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
            val screenDevices = ge.screenDevices

            if (screenNumber < 0) {
                screenNumber = 0
            } else if (screenNumber > screenDevices.size - 1) {
                screenNumber = screenDevices.size - 1
            }

            device = screenDevices[screenNumber]
        }

        return device!!.defaultConfiguration.bounds
    }

    // only called on the swing EDT thread
    fun addPopupToMap(notify: NotifyType<*>) {
        // start if we have previously stopped the timer
        if (!SwingActiveRender.contains(animationUpdateHandler)) {
            tweenEngine.resetUpdateTime()
            SwingActiveRender.add(animationUpdateHandler)
        }

        synchronized(popups) {
            val id = notify.idAndPosition

            val allNotifications = popups.getOrPut(id) {
                ArrayList<NotifyType<*>>(4)
            }

            notify.popupIndex = allNotifications.size

//            println("Adding $index : ${notify.notification.title}")

            // the popups are ALL the same size!
            // popups at TOP grow down, popups at BOTTOM grow up
            val anchorX = notify.anchorX
            val anchorY = notify.anchorY

            val targetY = if (growDown(notify)) {
                anchorY + notify.popupIndex * (Notify.HEIGHT + Notify.SPACER)
            } else {
                anchorY - notify.popupIndex * (Notify.HEIGHT + Notify.SPACER)
            }

            allNotifications.add(notify)
            notify.setLocationInternal(anchorX, targetY)
            notify.setupHide()
        }
    }

    // only called on the swing app or SwingActiveRender thread
    fun removePopupFromMap(notify: NotifyType<*>) {
        val growDown = growDown(notify)

        synchronized(popups) {
            val allNotifications = popups[notify.idAndPosition]!!

            // there are two loops because it is necessary to cancel + remove all tweens BEFORE adding new ones.
            var adjustPopupPosition = false
            val iterator = allNotifications.iterator()
            while (iterator.hasNext()) {
                val next = iterator.next()
                next.cancelMove()

                if (adjustPopupPosition) {
                    // we have to adjust all the following popups to a new location
                    next.popupIndex--
                }

                if (next === notify) {
                    notify.cancelHide()

                    adjustPopupPosition = true
                    iterator.remove()
                }
            }


            allNotifications.forEach { notify ->
                // the popups are ALL the same size!
                // popups at TOP grow down, popups at BOTTOM grow up
                val changedY = if (growDown) {
                    notify.anchorY + notify.popupIndex * (Notify.HEIGHT + Notify.SPACER)
                } else {
                    notify.anchorY - notify.popupIndex * (Notify.HEIGHT + Notify.SPACER)
                }

                // now animate ALL new notification popups their new location
                notify.setupMove(changedY.toFloat())
            }

            if (allNotifications.isEmpty()) {
                popups.remove(notify.idAndPosition)
            }

            if (popups.isEmpty()) {
                // if there's nothing left, stop the timer.
                SwingActiveRender.remove(animationUpdateHandler)
            }
        }
    }

    fun growDown(notify: NotifyType<*>): Boolean {
        return when (notify.notification.position) {
            Position.TOP_LEFT, Position.TOP_RIGHT, Position.CENTER -> true
            else -> false
        }
    }
}
