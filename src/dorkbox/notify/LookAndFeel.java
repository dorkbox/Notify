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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import dorkbox.tweenengine.BaseTween;
import dorkbox.tweenengine.Tween;
import dorkbox.tweenengine.TweenCallback;
import dorkbox.tweenengine.TweenEquations;
import dorkbox.tweenengine.TweenManager;
import dorkbox.util.ActionHandler;
import dorkbox.util.ActionHandlerLong;
import dorkbox.util.FontUtil;
import dorkbox.util.SwingUtil;
import dorkbox.util.swing.SwingActiveRender;

@SuppressWarnings({"FieldCanBeLocal"})
class LookAndFeel {
    private static final Map<String, ArrayList<LookAndFeel>> popups = new HashMap<String, ArrayList<LookAndFeel>>();

    static final NotifyAccessor accessor = new NotifyAccessor();
    static final TweenManager tweenManager = new TweenManager();
    private static final ActionHandlerLong frameStartHandler;



    private static final java.awt.event.WindowAdapter windowListener = new WindowAdapter();
    private static final MouseAdapter mouseListener = new ClickAdapter();

    private static final Stroke stroke = new BasicStroke(2);
    private static final int closeX = 282;
    private static final int closeY = 2;

    private static final int Y_1 = closeY + 5;
    private static final int X_1 = closeX + 5;
    private static final int Y_2 = closeY + 11;
    private static final int X_2 = closeX + 11;

    static final int WIDTH = 300;
    static final int HEIGHT = 87;
    private static final int PROGRESS_HEIGHT = HEIGHT - 2;

    private static final int PADDING = 40;

    private static final Random RANDOM = new Random();

    private static final float MOVE_DURATION = Notify.MOVE_DURATION;

    static {
        // this is for updating the tween engine during active-rendering
        frameStartHandler = new ActionHandlerLong() {
            @Override
            public
            void handle(final long deltaInNanos) {
                LookAndFeel.tweenManager.update(deltaInNanos);
            }
        };
    }

    private volatile int anchorX;
    private volatile int anchorY;

    private final Color panel_BG;
    private final Color titleText_FG;
    private final Color mainText_FG;
    private final Color closeX_FG;
    private final Color progress_FG;

    private final boolean showCloseButton;
    private final BufferedImage cachedImage;

    private final Window parent;

    private final float hideAfterDurationInSeconds;
    private final Pos position;

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    private final String idAndPosition;
    private int popupIndex;

    private volatile Tween tween = null;
    private volatile Tween hideTween = null;

    // for the progress bar. we directly draw this onscreen
    // non-volatile because it's always accessed in the active render thread
    private int progress = 0;

    private final ActionHandler<Notify> onCloseAction;

    LookAndFeel(final Window parent, final Notify notification, final Image image, final ImageIcon imageIcon, final Rectangle parentBounds) {
        this.parent = parent;

        parent.addWindowListener(windowListener);
        parent.addMouseListener(mouseListener);

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

        hideAfterDurationInSeconds = notification.hideAfterDurationInMillis / 1000.0F;
        position = notification.position;

        showCloseButton = !notification.hideCloseButton;

        // now we setup the rendering of the image
        cachedImage = renderBackgroundInfo(notification.title, notification.text, titleText_FG, mainText_FG, panel_BG, imageIcon);


        if (notification.onCloseAction != null) {
            onCloseAction = new ActionHandler<Notify>() {
                @Override
                public
                void handle(final Notify value) {
                    notification.onCloseAction.handle(notification);
                }
            };
        }
        else {
            onCloseAction = null;
        }

        idAndPosition = parentBounds.x + ":" + parentBounds.y + ":" + parentBounds.width + ":" + parentBounds.height + ":" + position;

        anchorX = getAnchorX(position, parentBounds);
        anchorY = getAnchorY(position, parentBounds);

        parent.setBackground(panel_BG);
        if (image != null) {
            parent.setIconImage(image);
        }
        else {
            parent.setIconImage(SwingUtil.BLANK_ICON);
        }
    }

    void paint(final Graphics g) {
        // we cache the text + image (to another image), and then always render the close + progressbar

        // use our cached image, so we don't have to re-render text/background/etc
        g.drawImage(cachedImage, 0, 0, null);

        // the progress bar and close button are the only things that can change, so we always draw them every time
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            if (showCloseButton) {
                Graphics2D g3 = (Graphics2D) g.create();

                g3.setColor(panel_BG);
                g3.setStroke(stroke);

                final Point p = parent.getMousePosition();
                // reasonable position for detecting mouse over
                if (p != null && p.getX() >= 280 && p.getY() <= 20) {
                    g3.setColor(Color.RED);
                }
                else {
                    g3.setColor(closeX_FG);
                }

                // draw the X
                g3.drawLine(X_1, Y_1, X_2, Y_2);
                g3.drawLine(X_2, Y_1, X_1, Y_2);
            }

            g2.setColor(progress_FG);
            g2.fillRect(0, PROGRESS_HEIGHT, progress, 2);
        } finally {
            g2.dispose();
        }
    }

    void close() {
        if (hideTween != null) {
            hideTween.cancel();
            hideTween = null;
        }

        if (tween != null) {
            tween.cancel();
            tween = null;
        }

        parent.removeWindowListener(windowListener);
        parent.removeMouseListener(mouseListener);
    }

    void onClick(final int x, final int y) {
        // Check - we were over the 'X' (and thus no notify), or was it in the general area?

        if (showCloseButton && x >= 280 && y <= 20) {
            // reasonable position for detecting mouse over
            ((INotify)parent).close();
        }
        else {
            if (onCloseAction != null) {
                onCloseAction.handle(null);
            }
            ((INotify) parent).close();
        }
    }

    void reLayout(final Rectangle bounds) {
        // when the parent window moves, we stop all animation and snap the popup into place. This simplifies logic greatly
        anchorX = getAnchorX(position, bounds);
        anchorY = getAnchorY(position, bounds);

        boolean showFromTop = isShowFromTop(this);

        if (tween != null) {
            tween.cancel(); // cancel does it's thing on the next tick of animation cycle
            tween = null;
        }

        int changedY;
        if (showFromTop) {
            changedY = anchorY + (popupIndex * (HEIGHT + 10));
        }
        else {
            changedY = anchorY - (popupIndex * (HEIGHT + 10));
        }

        parent.setLocation(anchorX, changedY);
    }

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

        Tween tween = Tween.to(this, NotifyAccessor.X_Y_POS, accessor, 0.05F)
                           .targetRelative(i1, i2)
                           .repeatAutoReverse(count, 0)
                           .ease(TweenEquations.Linear);
        tweenManager.add(tween);
    }

    void setY(final int y) {
        parent.setLocation(parent.getX(), y);
    }

    void setProgress(final int progress) {
        this.progress = progress;
    }

    int getProgress() {
        return progress;
    }

    int getY() {
        return parent.getY();
    }

    int getX() {
        return parent.getX();
    }

    void setVisible(final boolean visible) {
        if (visible) {
            parent.toFront();

            // set this jframe to use active rendering
            SwingActiveRender.addActiveRender(parent);
            addPopupToMap(this);
        }
        else {
            removePopupFromMap(this);
            SwingActiveRender.removeActiveRender(parent);
        }
    }

    private static
    int getAnchorX(final Pos position, final Rectangle bounds) {
        // we use the screen that the mouse is currently on.
        final int startX = (int) bounds.getX();
        final int screenWidth = (int) bounds.getWidth();

        // determine location for the popup
        // get anchorX
        switch (position) {
            case TOP_LEFT:
            case BOTTOM_LEFT:
                return startX + PADDING;

            case CENTER:
                return startX + (screenWidth / 2) - WIDTH / 2 - PADDING / 2;

            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return startX + screenWidth - WIDTH - PADDING;

            default:
                throw new RuntimeException("Unknown position. '" + position + "'");
        }
    }

    private static
    int getAnchorY(final Pos position, final Rectangle bounds) {
        final int startY = (int) bounds.getY();
        final int screenHeight = (int) bounds.getHeight();

        // get anchorY
        switch (position) {
            case TOP_LEFT:
            case TOP_RIGHT:
                return PADDING + startY;

            case CENTER:
                return startY + (screenHeight / 2) - HEIGHT / 2 - PADDING / 2;

            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return startY + screenHeight - HEIGHT - PADDING;

            default:
                throw new RuntimeException("Unknown position. '" + position + "'");
        }
    }

    private static
    BufferedImage renderBackgroundInfo(final String title,
                                       final String notificationText,
                                       final Color titleText_FG,
                                       final Color mainText_FG,
                                       final Color panel_BG,
                                       final ImageIcon imageIcon) {

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        try {
            g2.setColor(panel_BG);
            g2.fillRect(0, 0, WIDTH, HEIGHT);

            // Draw the title text
            java.awt.Font titleTextFont = FontUtil.parseFont(Notify.TITLE_TEXT_FONT);
            g2.setColor(titleText_FG);
            g2.setFont(titleTextFont);
            g2.drawString(title, 5, 20);


            int posX = 10;
            int posY = -8;
            int textLengthLimit = 108;

            // ICON
            if (imageIcon != null) {
                textLengthLimit = 88;
                posX = 60;
                // Draw the image
                imageIcon.paintIcon(null, g2, 5, 30);
            }

            // Draw the main text
            java.awt.Font mainTextFont = FontUtil.parseFont(Notify.MAIN_TEXT_FONT);
            int length = notificationText.length();
            StringBuilder text = new StringBuilder(length);

            // are we "html" already? just check for the starting tag and strip off END html tag
            if (length >= 13 && notificationText.regionMatches(true, length - 7, "</html>", 0, 7)) {
                text.append(notificationText);
                text.delete(text.length() - 7, text.length());

                length -= 7;
            }
            else {
                text.append("<html>");
                text.append(notificationText);
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
            mainTextLabel.setBounds(0, 0, WIDTH - posX - 2, HEIGHT);

            g2.translate(posX, posY);
            mainTextLabel.paint(g2);
            g2.translate(-posX, -posY);
        } finally {
            g2.dispose();
        }

        return image;
    }

    // only called on the swing EDT thread
    private static
    void addPopupToMap(final LookAndFeel sourceLook) {
        synchronized (popups) {
            String id = sourceLook.idAndPosition;

            ArrayList<LookAndFeel> looks = popups.get(id);
            if (looks == null) {
                looks = new ArrayList<LookAndFeel>(4);
                popups.put(id, looks);
            }
            final int popupIndex = looks.size();
            sourceLook.popupIndex = popupIndex;

            // the popups are ALL the same size!
            // popups at TOP grow down, popups at BOTTOM grow up
            int targetY;
            int anchorX = sourceLook.anchorX;
            int anchorY = sourceLook.anchorY;

            if (isShowFromTop(sourceLook)) {
                targetY = anchorY + (popupIndex * (HEIGHT + 10));
            }
            else {
                targetY = anchorY - (popupIndex * (HEIGHT + 10));
            }

            looks.add(sourceLook);
            sourceLook.setLocation(anchorX, targetY);

            if (sourceLook.hideAfterDurationInSeconds > 0 && sourceLook.hideTween == null) {
                // begin a timeline to get rid of the popup (default is 5 seconds)
                Tween hideTween = Tween.to(sourceLook, NotifyAccessor.PROGRESS, accessor, sourceLook.hideAfterDurationInSeconds)
                                       .target(WIDTH)
                                       .ease(TweenEquations.Linear)
                                       .addCallback(new TweenCallback() {
                                           @Override
                                           public
                                           void onEvent(final int type, final BaseTween<?> source) {
                                               if (type == Events.COMPLETE) {
                                                   ((INotify)sourceLook.parent).close();
                                               }
                                           }
                                       });
                tweenManager.add(hideTween);
            }
        }

        // start if we have stopped the timer
        if (!SwingActiveRender.containsActiveRenderFrameStart(frameStartHandler)) {
            tweenManager.resetUpdateTime();
            SwingActiveRender.addActiveRenderFrameStart(frameStartHandler);
        }
    }

    void setLocation(final int x, final int y) {
        parent.setLocation(x, y);
    }


    // only called on the swing app or SwingActiveRender thread
    private static
    void removePopupFromMap(final LookAndFeel sourceLook) {
        boolean showFromTop = isShowFromTop(sourceLook);
        boolean popupsAreEmpty;

        synchronized (popups) {
            popupsAreEmpty = popups.isEmpty();
            final ArrayList<LookAndFeel> allLooks = popups.get(sourceLook.idAndPosition);

            // there are two loops because it is necessary to cancel + remove all tweens BEFORE adding new ones.
            boolean adjustPopupPosition = false;
            for (Iterator<LookAndFeel> iterator = allLooks.iterator(); iterator.hasNext(); ) {
                final LookAndFeel look = iterator.next();

                if (look.tween != null) {
                    look.tween.cancel(); // cancel does it's thing on the next tick of animation cycle
                    look.tween = null;
                }

                if (look == sourceLook) {
                    if (look.hideTween != null) {
                        look.hideTween.cancel();
                        look.hideTween = null;
                    }

                    adjustPopupPosition = true;
                    iterator.remove();
                }

                if (adjustPopupPosition) {
                    look.popupIndex--;
                }
            }

            for (final LookAndFeel look : allLooks) {
                // the popups are ALL the same size!
                // popups at TOP grow down, popups at BOTTOM grow up
                int changedY;
                if (showFromTop) {
                    changedY = look.anchorY + (look.popupIndex * (HEIGHT + 10));
                }
                else {
                    changedY = look.anchorY - (look.popupIndex * (HEIGHT + 10));
                }

                // now animate that popup to it's new location
                Tween tween = Tween.to(look, NotifyAccessor.Y_POS, accessor, MOVE_DURATION)
                                   .target((float) changedY)
                                   .ease(TweenEquations.Linear)
                                   .addCallback(new TweenCallback() {
                                       @Override
                                       public
                                       void onEvent(final int type, final BaseTween<?> source) {
                                           if (type == Events.COMPLETE) {
                                               // make sure to remove the tween once it's done, otherwise .kill can do weird things.
                                               look.tween = null;
                                           }
                                       }
                                   });

                look.tween = tween;
                tweenManager.add(tween);
            }
        }

        // if there's nothing left, stop the timer.
        if (popupsAreEmpty) {
            SwingActiveRender.removeActiveRenderFrameStart(frameStartHandler);
        }
        // start if we have previously stopped the timer
        else if (!SwingActiveRender.containsActiveRenderFrameStart(frameStartHandler)) {
            tweenManager.resetUpdateTime();
            SwingActiveRender.addActiveRenderFrameStart(frameStartHandler);
        }
    }

    private static
    boolean isShowFromTop(final LookAndFeel look) {
        switch (look.position) {
            case TOP_LEFT:
            case TOP_RIGHT:
            case CENTER: // center grows down
                return true;
            default:
                return false;
        }
    }
}
