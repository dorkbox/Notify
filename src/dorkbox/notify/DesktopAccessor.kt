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

import dorkbox.tweenEngine.TweenAccessor

internal class DesktopAccessor : TweenAccessor<DesktopNotify> {
    companion object {
        const val X_POS = 0
        const val Y_POS = 1
        const val X_Y_POS = 2
        const val SHAKE = 3
        const val PROGRESS = 4
    }

    override fun getValues(target: DesktopNotify, tweenType: Int, returnValues: FloatArray): Int {
        when (tweenType) {
            X_POS -> {
                returnValues[0] = target.x.toFloat()
                return 1
            }

            Y_POS -> {
                returnValues[0] = target.y.toFloat()
                return 1
            }

            X_Y_POS -> {
                returnValues[0] = target.x.toFloat()
                returnValues[1] = target.y.toFloat()
                return 2
            }

            SHAKE -> {
                returnValues[0] = target.x.toFloat()
                returnValues[1] = target.y.toFloat()
                return 2
            }

            PROGRESS -> {
                returnValues[0] = target.progress.toFloat()
                return 1
            }
        }
        return 1
    }

    override fun setValues(target: DesktopNotify, tweenType: Int, newValues: FloatArray) {
        when (tweenType) {
            X_POS -> {
                target.setLocationInternal(newValues[0].toInt(), target.y)
                return
            }

            Y_POS -> {
                target.setLocationInternal(target.x, newValues[0].toInt())
                return
            }

            X_Y_POS -> {
                target.setLocationInternal(newValues[0].toInt(), newValues[1].toInt())
                return
            }

            SHAKE -> {
                target.setLocationInternal(newValues[0].toInt(), newValues[1].toInt())
                return
            }

            PROGRESS -> {
                target.progress = newValues[0].toInt()
                return
            }
        }
    }
}
