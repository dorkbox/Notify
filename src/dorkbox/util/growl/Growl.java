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
package dorkbox.util.growl;

import dorkbox.util.ActionHandler;
import dorkbox.util.SwingUtil;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Popup notification messages, similar to the popular "Growl" notification system on macosx, that display in the corner of the monitor.
 * </p>
 * They can follow the mouse (if the screen is unspecified), and have a variety of features, such as "shaking" to draw attention,
 * animating upon movement (for collating w/ multiple in a single location), and automatically hiding after a set duration.
 * </p>
 * These notifications are for a single screen only, and cannot be anchored to an application.
 *
 * <pre>
 * {@code
 * Growl.create()
 *      .title("Title Text")
 *      .text("Hello World 0!")
 *      .useDarkStyle()
 *      .showWarning();
 * }
 * </pre>
 */
@SuppressWarnings("unused")
public final
class Growl {

    public static final int FOREVER = 0;

    private static Map<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>(4);
    private static Map<String, ImageIcon> imageIconCache = new HashMap<String, ImageIcon>(4);

    String title;
    String text;
    Pos position = Pos.BOTTOM_RIGHT;

    int hideAfterDurationInMillis = 5000;
    boolean hideCloseButton;
    boolean isDark = false;

    int screenNumber = Short.MIN_VALUE;

    private Image graphic;
    private ActionHandler<Growl> onAction;
    private GrowlPopup growlPopup;
    private String name;
    private int shakeDurationInMillis = 0;
    private int shakeAmplitude = 0;

    /**
     * Builder pattern to create the growl notification.
     */
    public static
    Growl create() {
        return new Growl();
    }

    private
    Growl() {
    }

    /**
     * Specifies the main text
     */
    public
    Growl text(String text) {
        this.text = text;
        return this;
    }

    /**
     * Specifies the title
     */
    public
    Growl title(String title) {
        this.title = title;
        return this;
    }

    /**
     * Specifies the graphic
     */
    public
    Growl graphic(Image graphic) {
        this.graphic = graphic;
        return this;
    }

    /**
     * Specifies the position of the notification on screen, by default it is {@link Pos#BOTTOM_RIGHT bottom-right}.
     */
    public
    Growl position(Pos position) {
        this.position = position;
        return this;
    }

    /**
     * Specifies the duration that the notification should show, after which it will be hidden. 0 means to show forever.
     */
    public
    Growl hideAfter(int durationInMillis) {
        if (durationInMillis < 0) {
            durationInMillis = 0;
        }
        this.hideAfterDurationInMillis = durationInMillis;
        return this;
    }

    /**
     * Specifies what to do when the user clicks on the notification (in addition o the notification hiding, which happens whenever the
     * notification is clicked on). This does not apply when clicking on the "close" button
     */
    public
    Growl onAction(ActionHandler<Growl> onAction) {
        this.onAction = onAction;
        return this;
    }

    /**
     * Specifies that the notification should use the built-in dark styling, rather than the default, light-gray notification style.
     */
    public
    Growl darkStyle() {
        isDark = true;
        return this;
    }

    /**
     * Specify that the close button in the top-right corner of the notification should not be shown.
     */
    public
    Growl hideCloseButton() {
        this.hideCloseButton = true;
        return this;
    }

    /**
     * Shows the notification with the built-in 'warning' graphic.
     */
    public
    void showWarning() {
        name = "/dorkbox/util/growl/dialog-warning.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification with the built-in 'information' graphic.
     */
    public
    void showInformation() {
        name = "/dorkbox/util/growl/dialog-information.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification with the built-in 'error' graphic.
     */
    public
    void showError() {
        name = "/dorkbox/util/growl/dialog-error.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification with the built-in 'confirm' graphic.
     */
    public
    void showConfirm() {
        name = "/dorkbox/util/growl/dialog-confirm.png";
        graphic(getImage(name));
        show();
    }

    /**
     * Shows the notification
     */
    public
    void show() {
        // must be done in the swing EDT
        //noinspection Convert2Lambda
        SwingUtil.invokeAndWait(new Runnable() {
            @Override
            public
            void run() {
                final Growl growl = Growl.this;
                final Image graphic = growl.graphic;

                if (graphic == null) {
                    growlPopup = new GrowlPopup(growl, null, null);
                }
                else {
                    // we ONLY cache our own icons
                    ImageIcon imageIcon;
                    if (name != null) {
                        imageIcon = imageIconCache.get(name);
                        if (imageIcon == null) {
                            imageIcon = new ImageIcon(graphic);
                            imageIconCache.put(name, imageIcon);
                        }
                    }
                    else {
                        imageIcon = new ImageIcon(graphic);
                    }

                    growlPopup = new GrowlPopup(growl, graphic, imageIcon);
                }

                growlPopup.setVisible(true);

                if (shakeDurationInMillis > 0) {
                    growlPopup.shake(growl.shakeDurationInMillis, growl.shakeAmplitude);
                }
            }
        });
    }

    /**
     * "shakes" the notification, to bring user attention
     */
    public
    void shake(final int durationInMillis, final int amplitude) {
        this.shakeDurationInMillis = durationInMillis;
        this.shakeAmplitude = amplitude;

        if (growlPopup != null) {
            // must be done in the swing EDT
            //noinspection Convert2Lambda
            SwingUtil.invokeLater(new Runnable() {
                @Override
                public
                void run() {
                    growlPopup.shake(durationInMillis, amplitude);
                }
            });
        }
    }

    /**
     * Closes the notification. Particularly useful if it's an "infinite" duration notification.
     */
    public
    void close() {
        if (growlPopup == null) {
            throw new NullPointerException("GrowlPopup");
        }

        // must be done in the swing EDT
        //noinspection Convert2Lambda
        SwingUtil.invokeLater(new Runnable() {
            @Override
            public
            void run() {
                growlPopup.close();
            }
        });
    }


    /**
     * Specifies which screen to display on. If <0, it will show on screen 0. If > max-screens, it will show on the last screen.
     */
    public
    Growl setScreen(final int screenNumber) {
        this.screenNumber = screenNumber;
        return this;
    }

    void onClick() {
        onAction.handle(this);
    }

    void onClose() {
        growlPopup = null;
        graphic = null;
    }

    private
    BufferedImage getImage(String imageName) {
        BufferedImage bufferedImage = imageCache.get(imageName);
        try {
            if (bufferedImage == null) {
                bufferedImage = ImageIO.read(Growl.class.getResourceAsStream(imageName));
                imageCache.put(imageName, bufferedImage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bufferedImage;
    }
}

