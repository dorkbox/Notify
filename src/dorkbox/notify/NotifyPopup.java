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
package dorkbox.notify;

import dorkbox.util.ActionHandlerLong;
import dorkbox.util.OS;
import dorkbox.util.Property;
import dorkbox.util.ScreenUtil;
import dorkbox.util.SwingUtil;
import dorkbox.util.swing.SwingActiveRender;
import dorkbox.tweenengine.BaseTween;
import dorkbox.tweenengine.Tween;
import dorkbox.tweenengine.TweenCallback;
import dorkbox.tweenengine.TweenEquations;
import dorkbox.tweenengine.TweenManager;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

// we can't use regular popup, because if we have no owner, it won't work!
// instead, we just create a JFrame and use it to hold our content
@SuppressWarnings({"Duplicates", "FieldCanBeLocal"})
public
class NotifyPopup extends JFrame {
    private static final long serialVersionUID = 1L;

    @Property
    /** title font used by Notify */
    public static String TITLE_TEXT_FONT = "Source Code Pro BOLD 16";

    @Property
    /** main text font used by Notify */
    public static String MAIN_TEXT_FONT = "Source Code Pro BOLD 12";

    @Property
    /** How long we want it to take for the popups to relocate when one is closed */
    public static final float MOVE_DURATION = 1.0F;

    private static final int padding = 40;

    private static final Map<String, ArrayList<NotifyPopup>> popups = new HashMap<String, ArrayList<NotifyPopup>>();

    private static final NotifyPopupAccessor accessor = new NotifyPopupAccessor();
    private static final TweenManager tweenManager = new TweenManager();
    private static ActionHandlerLong frameStartHandler;

    private static WindowUtil opacity_compat;

    static {
        // this is for updating the tween engine during active-rendering
        frameStartHandler = new ActionHandlerLong() {
            @Override
            public
            void handle(final long deltaInNanos) {
                NotifyPopup.tweenManager.update(deltaInNanos);
            }
        };

        if (OS.javaVersion == 6) {
            opacity_compat = new WindowUtil_Java6();
        } else {
            opacity_compat = new WindowUtil_Java7plus();
        }
    }

    private static final int WIDTH = 300;
    private static final int HEIGHT = 87;
    private static final int PROGRESS_HEIGHT = HEIGHT - 1;

    private static final Stroke stroke = new BasicStroke(2);
    private static final int closeX = 282;
    private static final int closeY = 2;

    private static final int Y_1 = closeY + 5;
    private static final int X_1 = closeX + 5;
    private static final int Y_2 = closeY + 11;
    private static final int X_2 = closeX + 11;

    private final Color panel_BG;
    private final Color titleText_FG;
    private final Color mainText_FG;
    private final Color closeX_FG;
    private final Color progress_FG;


    private final int anchorX;
    private final int anchorY;

    private final WindowAdapter windowListener;
    private final MouseAdapter mouseListener;

    private final Notify notification;
    private final ImageIcon imageIcon;

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    private final String idAndPosition;

    private int popupIndex;

    private Tween tween = null;
    private Tween hideTween = null;

    // for the progress bar. we directly draw this onscreen
    // non-volatile because it's always accessed in the active render thread
    private int progress = 0;

    private final boolean showCloseButton;
    private BufferedImage cachedImage;
    private static final Random RANDOM = new Random();



    // this is on the swing EDT
    @SuppressWarnings("NumericCastThatLosesPrecision")
    NotifyPopup(Notify notification, Image image, ImageIcon imageIcon) {
        this.notification = notification;
        this.imageIcon = imageIcon;

        windowListener = new NotifyPopupWindowAdapter();
        mouseListener = new NotifyPopupClickAdapter();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setOpacity_Compat(1.0F);
        setAlwaysOnTop(false);
        setAlwaysOnTop(true);
        setLayout(null);

        setSize(WIDTH, HEIGHT);
        setLocation(Short.MIN_VALUE, Short.MIN_VALUE);

        setTitle(notification.title);
        setResizable(false);

        if (image != null) {
            setIconImage(image);
        } else {
            setIconImage(SwingUtil.BLANK_ICON);
        }


        addWindowListener(windowListener);
        addMouseListener(mouseListener);


        if (notification.isDark) {
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

        setBackground(panel_BG);
        showCloseButton = !notification.hideCloseButton;

        GraphicsDevice device;
        if (notification.screenNumber == Short.MIN_VALUE) {
            // set screen position based on mouse
            Point mouseLocation = MouseInfo.getPointerInfo()
                                           .getLocation();

            device = ScreenUtil.getGraphicsDeviceAt(mouseLocation);
        }
        else {
            // set screen position based on specified screen
            int screenNumber = notification.screenNumber;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice screenDevices[] = ge.getScreenDevices();

            if (screenNumber < 0) {
                screenNumber = 0;
            }
            else if (screenNumber > screenDevices.length - 1) {
                screenNumber = screenDevices.length - 1;
            }

            device = screenDevices[screenNumber];
        }

        idAndPosition = device.getIDstring() + notification.position;

        Rectangle screenBounds = device.getDefaultConfiguration()
                                       .getBounds();

        // we use the screen that the mouse is currently on.
        final int startX = (int) screenBounds.getX();
        final int startY = (int) screenBounds.getY();
        final int screenWidth = (int) screenBounds.getWidth();
        final int screenHeight = (int) screenBounds.getHeight();


        // determine location for the popup
        final Pos position = notification.position;

        // get anchorX
        switch (position) {
            case TOP_LEFT:
            case BOTTOM_LEFT:
                anchorX = startX + padding;
                break;

            case CENTER:
                anchorX = startX + (screenWidth / 2) - WIDTH / 2 - padding / 2;
                break;

            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                anchorX = startX + screenWidth - WIDTH - padding;
                break;

            default:
                throw new RuntimeException("Unknown position. '" + position + "'");
        }

        // get anchorY
        switch (position) {
            case TOP_LEFT:
            case TOP_RIGHT:
                anchorY = padding + startY;
                break;

            case CENTER:
                anchorY = startY + (screenHeight / 2) - HEIGHT / 2 - padding / 2;
                break;

            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                anchorY = startY + screenHeight - HEIGHT - padding;
                break;

            default:
                throw new RuntimeException("Unknown position. '" + position + "'");
        }
    }

    @Override
    public
    void paint(Graphics g) {
        // we cache the text + image (to another image), and then always render the close + progressbar
        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }
        if (cachedImage == null) {
            cachedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            Graphics g2 = cachedImage.createGraphics();
            try {
                g2.setColor(panel_BG);
                g2.fillRect(0, 0, WIDTH, HEIGHT);

                // Draw the title text
                Font titleTextFont = SwingUtil.parseFont(TITLE_TEXT_FONT);
                g2.setColor(titleText_FG);
                g2.setFont(titleTextFont);
                g2.drawString(notification.title, 5, 20);



                int posX = 10;
                int textLengthLimit = 108;

                // ICON
                if (imageIcon != null) {
                    textLengthLimit = 88;
                    posX = 60;
                    // Draw the image
                    imageIcon.paintIcon(this, g2, 5, 30);
                }

                // Draw the main text
                Font mainTextFont = SwingUtil.parseFont(MAIN_TEXT_FONT);
                String notText = notification.text;
                int length = notText.length();
                StringBuilder text = new StringBuilder(length);

                // are we "html" already? just check for the starting tag and strip off END html tag
                if (length >= 13 && notText.regionMatches(true, length-7, "</html>", 0, 7)) {
                    text.append(notText);
                    text.delete(text.length() - 7, text.length());

                    length -= 7;
                }
                else {
                    text.append("<html>");
                    text.append(notText);
                }

                // make sure the text is the correct length
                if (length > textLengthLimit) {
                    text.delete(6 + textLengthLimit, text.length());
                    text.append("...");
                }
                text.append("</html>");

                JLabel mainTextLabel = new JLabel();
                mainTextLabel.setForeground(mainText_FG);
                mainTextLabel.setFont(mainTextFont);
                mainTextLabel.setText(text.toString());

                int posY = -8;
                mainTextLabel.setBounds(0, 0, WIDTH - posX - 2, HEIGHT);

                g2.translate(posX, posY);
                mainTextLabel.paint(g2);
                g2.translate(-posX, -posY);
            } finally {
                g2.dispose();
            }

            g.drawImage(cachedImage, 0, 0, null);
        }
        else {
            // use our cached image, so we don't have to re-render text
            g.drawImage(cachedImage, getX(), getY(), null);

            // the progress bar and close button are the only things that can change, so we always draw them
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                if (showCloseButton) {
                    Graphics2D g3 = (Graphics2D) g.create();

                    g3.setColor(panel_BG);
                    g3.setStroke(stroke);

                    final Point p = getMousePosition();
                    // reasonable position for detecting mouse over
                    if (p != null && p.getX() >= 280 && p.getY() <= 20) {
                        g3.setColor(Color.RED);
                    } else {
                        g3.setColor(closeX_FG);
                    }

                    // draw the X
                    g3.drawLine(X_1, Y_1, X_2, Y_2);
                    g3.drawLine(X_2, Y_1, X_1, Y_2);
                }

                g2.setColor(progress_FG);
                g2.fillRect(0, PROGRESS_HEIGHT, progress, 1);
            } finally {
                g2.dispose();
            }
        }
    }

    public
    void onClick(final int x, final int y) {
        // Check - we were over the 'X' (and thus no notify), or was it in the general area?

        if (showCloseButton && x >= 280 && y <= 20) {
            // reasonable position for detecting mouse over
            close();
        }
        else {
            notification.onClick();
            close();
        }
    }

    @Override
    public
    void setVisible(final boolean b) {
        // necessary for active rendering
        setIgnoreRepaint(true);

        super.setVisible(b);

        if (b) {
            toFront();

            // set this jframe to use active rendering
            SwingActiveRender.addActiveRender(this);
            addPopupToMap();
        }
        else {
            removePopupFromMap();
            SwingActiveRender.removeActiveRender(this);
        }
    }


    public void close() {
        WindowEvent winClosingEvent = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);
        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(winClosingEvent);

        // set it off screen (which is what the close method also does)
        setVisible(false);
        removeAll();
        removeWindowListener(windowListener);
        removeMouseListener(mouseListener);
        setIconImage(null);
        dispose();

        notification.onClose();
    }


    // only called on the swing thread
    void addPopupToMap() {
        Pos position = notification.position;

        synchronized (popups) {
            ArrayList<NotifyPopup> notifyPopups = popups.get(idAndPosition);
            if (notifyPopups == null) {
                notifyPopups = new ArrayList<NotifyPopup>(4);
                popups.put(idAndPosition, notifyPopups);
            }
            final int popupIndex = notifyPopups.size();
            this.popupIndex = popupIndex;

            // the popups are ALL the same size!
            // popups at TOP grow down, popups at BOTTOM grow up

            int targetY;
            if (isShowFromTop(position)) {
                targetY = anchorY + (popupIndex * (HEIGHT + 10));
            }
            else {
                targetY = anchorY - (popupIndex * (HEIGHT + 10));
            }

            notifyPopups.add(this);
            setLocation(anchorX, targetY);

            if (notification.hideAfterDurationInMillis > 0 && hideTween == null) {
                // begin a timeline to get rid of the popup (default is 5 seconds)
                final float durationInSeconds = notification.hideAfterDurationInMillis / 1000.0F;

                hideTween = Tween.to(this, NotifyPopupAccessor.PROGRESS, accessor, durationInSeconds)
                                 .target(WIDTH)
                                 .ease(TweenEquations.Linear)
                                 .addCallback(new TweenCallback() {
                                     @Override
                                     public
                                     void onEvent(final int type, final BaseTween<?> source) {
                                         close();
                                     }
                                 });
                tweenManager.add(hideTween);

                if (!SwingActiveRender.containsActiveRenderFrameStart(frameStartHandler)) {
                    tweenManager.resetUpdateTime();
                    SwingActiveRender.addActiveRenderFrameStart(frameStartHandler);
                }
            }
        }
    }


    // only called on the swing app thread
    private
    void removePopupFromMap() {
        Pos position = notification.position;
        boolean showFromTop = isShowFromTop(position);

        synchronized (popups) {
            final int popupIndex = this.popupIndex;
            final ArrayList<NotifyPopup> notifyPopups = popups.get(idAndPosition);
            int length = notifyPopups.size();

            final ArrayList<NotifyPopup> copies = new ArrayList<NotifyPopup>(length);

            // if we are the LAST tween, don't adjust anything (since nothing will move anyways)
            if (popupIndex == length - 1) {
                notifyPopups.remove(popupIndex);

                if (tween != null) {
                    tween.kill();
                }
                if (hideTween != null) {
                    hideTween.kill();
                }

                // if there's nothing left, stop the timer.
                if (popups.isEmpty()) {
                    SwingActiveRender.removeActiveRenderFrameStart(frameStartHandler);
                }
                return;
            }


            int adjustedI = 0;
            for (int i = 0; i < length; i++) {
                final NotifyPopup popup = notifyPopups.get(i);

                if (popup.tween != null) {
                    popup.tween.kill();
                }

                if (i != popupIndex) {
                    // move the others into the correct position
                    int newPopupIndex = adjustedI++;
                    popup.popupIndex = newPopupIndex;

                    // the popups are ALL the same size!
                    // popups at TOP grow down, popups at BOTTOM grow up
                    int changedY;
                    if (showFromTop) {
                        changedY = popup.anchorY + (newPopupIndex * (HEIGHT + 10));
                    }
                    else {
                        changedY = popup.anchorY - (newPopupIndex * (HEIGHT + 10));
                    }
                    copies.add(popup);

                    // now animate that popup to it's new location
                    Tween tween = Tween.to(popup, NotifyPopupAccessor.Y_POS, accessor, MOVE_DURATION)
                                       .target((float) changedY)
                                       .ease(TweenEquations.Linear);

                    tweenManager.add(tween);
                    popup.tween = tween;
                }
                else {
                    if (hideTween != null) {
                        hideTween.kill();
                    }
                }
            }

            notifyPopups.clear();
            popups.put(idAndPosition, copies);

            // if there's nothing left, stop the timer.
            if (!SwingActiveRender.containsActiveRenderFrameStart(frameStartHandler)) {
                tweenManager.resetUpdateTime();
                SwingActiveRender.addActiveRenderFrameStart(frameStartHandler);
            }
        }
    }

    private static
    boolean isShowFromTop(final Pos p) {
        switch (p) {
            case TOP_LEFT:
            case TOP_RIGHT:
            case CENTER: // center grows down
                return true;
            default:
                return false;
        }
    }

    public
    void setY(final int newY) {
        setLocation(getX(), newY);
    }


    /**
     * Shakes the popup
     *
     * @param durationInMillis now long it will shake
     * @param amplitude a measure of how much it needs to shake. 4 is a small amount of shaking, 10 is a lot.
     */
    public
    void shake(final int durationInMillis, final int amplitude) {
        int i1 = RANDOM.nextInt((amplitude << 2) + 1) - amplitude;
        int i2 = RANDOM.nextInt((amplitude << 2) + 1) - amplitude;

        i1 = i1 >> 2;
        i2 = i2 >> 2;

        // make sure it always moves by some amount
        if (i1 < 0) {
            i1 -= amplitude >> 2;
        }
        else {
            i1 += amplitude >> 2;
        }

        if (i2 < 0) {
            i2 -= amplitude >> 2;
        }
        else {
            i2 += amplitude >> 2;
        }

        int count = durationInMillis / 50;
        // make sure we always end the animation where we start
        if ((count & 1) == 0) {
            count++;
        }

        Tween tween = Tween.to(this, NotifyPopupAccessor.X_Y_POS, accessor, 0.05F)
                           .targetRelative(i1, i2)
                           .repeatAutoReverse(count, 0)
                           .ease(TweenEquations.Linear);
        tweenManager.add(tween);
    }


    void setOpacity_Compat(float opacity) {
        opacity_compat.setOpacity(this, opacity);
    }

    public
    float getOpacity_Compat() {
        return opacity_compat.getOpacity(this);
    }

    public
    int getProgress() {
        return progress;
    }

    public
    void setProgress(final int progress) {
        this.progress = progress;
    }
}
