package dorkbox.notify;

import java.awt.Color;
import java.awt.Font;

import dorkbox.util.FontUtil;

/**
 *
 */
public
class Theme {
    public final Color panel_BG;
    public final Color titleText_FG;
    public final Color mainText_FG;
    public final Color closeX_FG;
    public final Color progress_FG;

    public final Font titleTextFont;
    public final Font mainTextFont;


    Theme(final String titleTextFont, final String mainTextFont, boolean isDarkTheme) {
        this.titleTextFont = FontUtil.parseFont(titleTextFont);
        this.mainTextFont = FontUtil.parseFont(mainTextFont);

        if (isDarkTheme) {
            panel_BG = Color.DARK_GRAY;
            titleText_FG = Color.GRAY;
            mainText_FG = Color.LIGHT_GRAY;
            closeX_FG = Color.GRAY;
            progress_FG = Color.gray;
        }
        else {
            panel_BG = Color.WHITE;
            titleText_FG = Color.GRAY.darker();
            mainText_FG = Color.GRAY;
            closeX_FG = Color.LIGHT_GRAY;
            progress_FG = new Color(0x42A5F5);
        }
    }

    public
    Theme(final String titleTextFont, final String mainTextFont,
          final Color panel_BG, final Color titleText_FG, final Color mainText_FG, final Color closeX_FG, final Color progress_FG) {
        this.titleTextFont = FontUtil.parseFont(titleTextFont);
        this.mainTextFont = FontUtil.parseFont(mainTextFont);

        this.panel_BG = panel_BG;
        this.titleText_FG = titleText_FG;
        this.mainText_FG = mainText_FG;
        this.closeX_FG = closeX_FG;
        this.progress_FG = progress_FG;
    }
}
