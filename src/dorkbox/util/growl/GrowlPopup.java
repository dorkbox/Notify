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

import dorkbox.util.OS;
import dorkbox.util.ScreenUtil;
import dorkbox.util.SwingUtil;
import dorkbox.util.SystemProps;
import dorkbox.util.tweenengine.BaseTween;
import dorkbox.util.tweenengine.Tween;
import dorkbox.util.tweenengine.TweenCallback;
import dorkbox.util.tweenengine.TweenEquations;
import dorkbox.util.tweenengine.TweenManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// we can't use regular popup, because if we have no owner, it won't work!
// instead, we just create a JFrame and use it to hold our content
@SuppressWarnings("Duplicates")
public
class GrowlPopup extends JFrame {

    private static final int padding = 40;

    public static final float FADE_DURATION = 1.5F;
    public static final float MOVE_DURATION = 1.0F;

    private static final Map<String, ArrayList<GrowlPopup>> popups = new HashMap<String, ArrayList<GrowlPopup>>();

    private static final GrowlPopupAccessor accessor = new GrowlPopupAccessor();
    private static final TweenManager tweenManager = new TweenManager();

    private static Timer timer;
    private static WindowUtil opacity_compat;


    static {
        // this timer is on the EDT (this is not the java.util timer)
        // 30 times a second
        //noinspection Convert2Lambda
        timer = new Timer(1000/30, new ActionListener() {
            @Override
            public
            void actionPerformed(final ActionEvent e) {
                tweenManager.update();
            }
        });
        timer.setRepeats(true);

        if (OS.javaVersion == 6) {
            opacity_compat = new WindowUtil_Java6();
        } else {
            opacity_compat = new WindowUtil_Java7plus();
        }
    }

    private static final int WIDTH = 300;
    private static final int HEIGHT = 90;



    private final int anchorX;
    private final int anchorY;

    private final WindowAdapter windowListener;
    private final MouseAdapter mouseListener;

    private final Growl notification;

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    private final String idAndPosition;

    private int popupIndex;

    private Tween tween = null;
    private Tween hideTween = null;


    // this is on the swing EDT
    @SuppressWarnings("NumericCastThatLosesPrecision")
    GrowlPopup(Growl notification, Image image, ImageIcon imageIcon) {
        this.notification = notification;

        windowListener = new GrowlPopupWindowAdapter();
        mouseListener = new GrowlPopupClickAdapter();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setUndecorated(true);
        setOpacity_Compat(1.0F);
        setAlwaysOnTop(true);

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


        final Color text_BG;
        final Color titleText_FG;
        final Color mainText_FG;
        final Color closeX_FG;

        if (notification.isDark) {
            text_BG = Color.DARK_GRAY;
            titleText_FG = Color.GRAY;
            mainText_FG = Color.LIGHT_GRAY;
            closeX_FG = Color.GRAY;
        }
        else {
            text_BG = Color.WHITE;
            titleText_FG = Color.DARK_GRAY;
            mainText_FG = Color.GRAY;
            closeX_FG = Color.LIGHT_GRAY;
        }

        setBackground(text_BG);


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

        // makes sure everything is spaced nicely
        final JPanel contentPane = new JPanel();
        contentPane.setBackground(text_BG);
        contentPane.setBorder(new EmptyBorder(0, 10, 10, 5));
        contentPane.setLayout(new BorderLayout(10, 5));
        setContentPane(contentPane);


        // closebutton is the 'x' button, but it is really just a font
        Font closeButtonFont = SwingUtil.getFontFromProperty(SystemProps.growl_closeButtonFontName, "Source Code Pro", Font.BOLD, 12);
        Font mainTextFont = SwingUtil.getFontFromProperty(SystemProps.growl_titleTextFontName, "Source Code Pro", Font.BOLD, 14);
        Font titleTextFont = SwingUtil.getFontFromProperty(SystemProps.growl_mainTextFontName, "Source Code Pro", Font.BOLD, 16);

        // TITLE AND CLOSE BUTTON
        {
            Box box = new Box(BoxLayout.X_AXIS);
            box.setBackground(text_BG);

            box.setAlignmentX(Component.CENTER_ALIGNMENT);

            {
                Box textBox = new Box(BoxLayout.X_AXIS);
                textBox.setAlignmentX(Component.LEFT_ALIGNMENT);

                final JLabel titleLabel = new JLabel();
                titleLabel.setForeground(titleText_FG);
                titleLabel.setFont(titleTextFont);
                titleLabel.setText(notification.title);

                textBox.add(titleLabel);
                textBox.add(Box.createHorizontalGlue());

                box.add(textBox);

                if (!notification.hideCloseButton) {
                    // can specify to hide the close button
                    Box closeBox = new Box(BoxLayout.X_AXIS);
                    closeBox.setBorder(new EmptyBorder(4, 4, 4, 4));
                    closeBox.setAlignmentX(Component.RIGHT_ALIGNMENT);

                    final JLabel closeButton = new JLabel();
                    closeButton.setForeground(closeX_FG);
                    closeButton.setFont(closeButtonFont);
                    closeButton.setText("x");
                    closeButton.setVerticalTextPosition(SwingConstants.TOP);

                    closeBox.addMouseListener(new GrowlCloseAdapter(this));
                    closeBox.add(closeButton);

                    box.add(closeBox);
                }
            }

            contentPane.add(box, BorderLayout.NORTH);
        }


        int textLengthLimit = 98;

        // ICON
        if (imageIcon != null) {
            textLengthLimit = 76;
            JLabel iconLabel = new JLabel(imageIcon);
            contentPane.add(iconLabel, BorderLayout.WEST);
        }

        // MAIN TEXT
        {
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
            contentPane.add(mainTextLabel, BorderLayout.CENTER);
        }



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

    public void close() {
        removePopupFromMap();

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
            ArrayList<GrowlPopup> growlPopups = popups.get(idAndPosition);
            if (growlPopups == null) {
                growlPopups = new ArrayList<GrowlPopup>(4);
                popups.put(idAndPosition, growlPopups);
            }
            final int popupIndex = growlPopups.size();
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

            growlPopups.add(this);
            setLocation(anchorX, targetY);

            if (notification.hideAfterDurationInMillis > 0 && hideTween == null) {
                // begin a timeline to get rid of the popup (default is 5 seconds)
                hideTween = Tween.set(this, GrowlPopupAccessor.OPACITY, accessor)
                                 .delay(FADE_DURATION + (notification.hideAfterDurationInMillis / 1000.0F))
                                 .target(0)
                                 .addCallback(new TweenCallback() {
                                     @Override
                                     public
                                     void onEvent(final int type, final BaseTween<?> source) {
                                         close();
                                     }
                                 });
                tweenManager.add(hideTween);

                if (!timer.isRunning()) {
                    timer.start();
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
            final ArrayList<GrowlPopup> growlPopups = popups.get(idAndPosition);
            int length = growlPopups.size();

            final ArrayList<GrowlPopup> copies = new ArrayList<GrowlPopup>(length);

            // if we are the LAST tween, don't adjust anything (since nothing will move anyways)
            if (popupIndex == length - 1) {
                growlPopups.remove(popupIndex);

                if (tween != null) {
                    tween.kill();
                }
                if (hideTween != null) {
                    hideTween.kill();
                }

                // if there's nothing left, stop the timer.
                if (copies.isEmpty()) {
                    timer.stop();
                }
                return;
            }


            int adjustedI = 0;
            for (int i = 0; i < length; i++) {
                final GrowlPopup popup = growlPopups.get(i);

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
                    Tween tween = Tween.to(popup, GrowlPopupAccessor.Y_POS, accessor, MOVE_DURATION)
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

            growlPopups.clear();
            popups.put(idAndPosition, copies);

            // if there's nothing left, stop the timer.
            if (copies.isEmpty()) {
                timer.stop();
            }
            else if (!timer.isRunning()) {
                tweenManager.resetUpdateTime();
                timer.start();
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

    public
    void onClick() {
        notification.onClick();
        close();
    }

    public
    void shake(final int durationInMillis, final int amplitude) {
        System.err.println("shake");

        Tween tween = Tween.to(this, GrowlPopupAccessor.X_Y_POS, accessor, 0.05F)
                           .targetRelative(amplitude, amplitude)
                           .repeatAutoReverse(durationInMillis / 50, 0)
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
}
