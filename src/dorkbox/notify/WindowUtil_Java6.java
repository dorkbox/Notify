package dorkbox.notify;

import com.sun.awt.AWTUtilities;

import java.awt.Window;

class WindowUtil_Java6 implements WindowUtil {

    @Override
    public
    float getOpacity(final Window window) {
        return AWTUtilities.getWindowOpacity(window);
    }

    @Override
    public
    void setOpacity(final Window window, final float opacity) {
        AWTUtilities.setWindowOpacity(window, opacity);
    }
}
