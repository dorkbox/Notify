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

import dorkbox.util.FontUtil
import java.awt.Color
import java.awt.Font

/**
 * Settings available to change the theme
 */
class Theme {
    companion object {
        val defaultLight: Theme by lazy {
            Theme(Notify.TITLE_TEXT_FONT, Notify.MAIN_TEXT_FONT, false)
        }

        val defaultDark: Theme by lazy {
            Theme(Notify.TITLE_TEXT_FONT, Notify.MAIN_TEXT_FONT, true)
        }
    }

    val panel_BG: Color
    val titleText_FG: Color
    val mainText_FG: Color
    val closeX_FG: Color
    val progress_FG: Color
    val titleTextFont: Font
    val mainTextFont: Font

    constructor(titleTextFont: String, mainTextFont: String, isDarkTheme: Boolean) {
        this.titleTextFont = FontUtil.parseFont(titleTextFont)
        this.mainTextFont = FontUtil.parseFont(mainTextFont)

        if (isDarkTheme) {
            panel_BG = Color.DARK_GRAY
            titleText_FG = Color.GRAY
            mainText_FG = Color.LIGHT_GRAY
            closeX_FG = Color.GRAY
            progress_FG = Color.gray
        } else {
            panel_BG = Color.WHITE
            titleText_FG = Color.GRAY.darker()
            mainText_FG = Color.GRAY
            closeX_FG = Color.LIGHT_GRAY
            progress_FG = Color(0x42A5F5)
        }
    }

    constructor(
        titleTextFont: String,
        mainTextFont: String,
        panel_BG: Color,
        titleText_FG: Color,
        mainText_FG: Color,
        closeX_FG: Color,
        progress_FG: Color
    ) {
        this.titleTextFont = FontUtil.parseFont(titleTextFont)
        this.mainTextFont = FontUtil.parseFont(mainTextFont)
        this.panel_BG = panel_BG
        this.titleText_FG = titleText_FG
        this.mainText_FG = mainText_FG
        this.closeX_FG = closeX_FG
        this.progress_FG = progress_FG
    }

    /**
     * True if we are the default "light" theme
     */
    fun isLight(): Boolean {
        return this === defaultLight
    }

    /**
     * True if we are the default "dark" theme
     */
    fun isDark(): Boolean {
        return this === defaultDark
    }

    /**
     * True if we are a custom theme
     */
    fun isCustom(): Boolean {
        return !isLight() && !isDark()
    }
}
