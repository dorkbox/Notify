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

enum class Position {
    /**
     * top vertically, left horizontally
     */
    TOP_LEFT,

    /**
     * top vertically, right horizontally
     */
    TOP_RIGHT,

    /**
     * top vertically, center horizontally
     */
    TOP,

    /**
     * center both vertically and horizontally
     */
    CENTER,

    /**
     * bottom vertically, center horizontally
     */
    BOTTOM,

    /**
     * bottom vertically, left horizontally
     */
    BOTTOM_LEFT,

    /**
     * bottom vertically, right horizontally
     */
    BOTTOM_RIGHT
}
