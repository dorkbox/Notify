package dorkbox.notify;

import java.awt.Window;

class WindowUtil_Java7plus implements WindowUtil {

    @Override
    public
    float getOpacity(final Window window) {
        return window.getOpacity();
    }

    @Override
    public
    void setOpacity(final Window window, final float opacity) {
        window.setOpacity(opacity);
    }
}
