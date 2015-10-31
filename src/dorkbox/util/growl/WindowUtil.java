package dorkbox.util.growl;

import java.awt.Window;

public interface WindowUtil {
    float getOpacity(final Window window);
    void setOpacity(final Window window, final float opacity);
}
