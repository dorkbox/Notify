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

internal class NotifyAccessor : TweenAccessor<LookAndFeel> {
    companion object {
        const val Y_POS = 1
        const val X_Y_POS = 2
        const val PROGRESS = 3
    }

    override fun getValues(target: LookAndFeel, tweenType: Int, returnValues: FloatArray): Int {
        when (tweenType) {
            Y_POS -> {
                returnValues[0] = target.y.toFloat()
                return 1
            }

            X_Y_POS -> {
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

    override fun setValues(target: LookAndFeel, tweenType: Int, newValues: FloatArray) {
        when (tweenType) {
            Y_POS -> {
                target.y = newValues[0].toInt()
                return
            }

            X_Y_POS -> {
                target.setLocation(newValues[0].toInt(), newValues[1].toInt())
                return
            }

            PROGRESS -> {
                target.progress = newValues[0].toInt()
                return
            }
        }
    }
}
