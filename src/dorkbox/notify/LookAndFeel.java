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

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import dorkbox.tweenengine.BaseTween;
import dorkbox.tweenengine.Tween;
import dorkbox.tweenengine.TweenCallback;
import dorkbox.tweenengine.TweenEngine;
import dorkbox.tweenengine.TweenEquations;
import dorkbox.util.ActionHandler;
import dorkbox.util.ActionHandlerLong;
import dorkbox.util.SwingUtil;
import dorkbox.util.swing.SwingActiveRender;

@SuppressWarnings({"FieldCanBeLocal"})
class LookAndFeel {
    private static final Map<String, ArrayList<LookAndFeel>> popups = new HashMap<String, ArrayList<LookAndFeel>>();

    static final TweenEngine animation = TweenEngine.create()
                                                    .unsafe()  // access is only from a single thread ever, so unsafe is preferred.
                                                    .build();

    static final NotifyAccessor accessor = new NotifyAccessor();
    private static final ActionHandlerLong frameStartHandler;

    static {
        // this is for updating the tween engine during active-rendering
        frameStartHandler = new ActionHandlerLong() {
            @Override
            public
            void handle(final long deltaInNanos) {
                LookAndFeel.animation.update(deltaInNanos);
            }
        };
    }


    private static final int PADDING = 40;

    private static final java.awt.event.WindowAdapter windowListener = new WindowAdapter();
    private static final MouseAdapter mouseListener = new ClickAdapter();

    private static final Random RANDOM = new Random();

    private static final float MOVE_DURATION = Notify.MOVE_DURATION;



    private volatile int anchorX;
    private volatile int anchorY;


    private final Window parent;
    private final NotifyCanvas notifyCanvas;

    private final float hideAfterDurationInSeconds;
    private final Pos position;

    // this is used in combination with position, so that we can track which screen and what position a popup is in
    private final String idAndPosition;
    private int popupIndex;

    private volatile Tween tween = null;
    private volatile Tween hideTween = null;

    private final ActionHandler<Notify> onCloseAction;

    LookAndFeel(final Window parent,
                final NotifyCanvas notifyCanvas,
                final Notify notification,
                final Image image,
                final Rectangle parentBounds) {

        this.parent = parent;
        this.notifyCanvas = notifyCanvas;


        parent.addWindowListener(windowListener);
        parent.addMouseListener(mouseListener);

        hideAfterDurationInSeconds = notification.hideAfterDurationInMillis / 1000.0F;
        position = notification.position;

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

        if (image != null) {
            parent.setIconImage(image);
        }
        else {
            parent.setIconImage(SwingUtil.BLANK_ICON);
        }
    }

    void onClick(final int x, final int y) {
        // Check - we were over the 'X' (and thus no notify), or was it in the general area?

        if (notifyCanvas.isCloseButton(x, y)) {
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
            changedY = anchorY + (popupIndex * (NotifyCanvas.HEIGHT + 10));
        }
        else {
            changedY = anchorY - (popupIndex * (NotifyCanvas.HEIGHT + 10));
        }

        parent.setLocation(anchorX, changedY);
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

        animation.to(this, NotifyAccessor.X_Y_POS, accessor, 0.05F)
                 .targetRelative(i1, i2)
                 .repeatAutoReverse(count, 0)
                 .ease(TweenEquations.Linear)
                 .start();
    }

    void setParentY(final int y) {
        parent.setLocation(parent.getX(), y);
    }

    int getParentY() {
        return parent.getY();
    }

    int getParentX() {
        return parent.getX();
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
                return startX + (screenWidth / 2) - NotifyCanvas.WIDTH / 2 - PADDING / 2;

            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return startX + screenWidth - NotifyCanvas.WIDTH - PADDING;

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
                return startY + (screenHeight / 2) - NotifyCanvas.HEIGHT / 2 - PADDING / 2;

            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                return startY + screenHeight - NotifyCanvas.HEIGHT - PADDING;

            default:
                throw new RuntimeException("Unknown position. '" + position + "'");
        }
    }

    void setParentLocation(final int x, final int y) {
        parent.setLocation(x, y);
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
                targetY = anchorY + (popupIndex * (NotifyCanvas.HEIGHT + 10));
            }
            else {
                targetY = anchorY - (popupIndex * (NotifyCanvas.HEIGHT + 10));
            }

            looks.add(sourceLook);
            sourceLook.setParentLocation(anchorX, targetY);

            if (sourceLook.hideAfterDurationInSeconds > 0 && sourceLook.hideTween == null) {
                // begin a timeline to get rid of the popup (default is 5 seconds)
                animation.to(sourceLook, NotifyAccessor.PROGRESS, accessor, sourceLook.hideAfterDurationInSeconds)
                         .target(NotifyCanvas.WIDTH)
                         .ease(TweenEquations.Linear)
                         .addCallback(new TweenCallback() {
                            @Override
                            public
                            void onEvent(final int type, final BaseTween<?> source) {
                                if (type == Events.COMPLETE) {
                                    ((INotify) sourceLook.parent).close();
                                }
                            }
                        })
                         .start();
            }
        }
    }

    // only called on the swing app or SwingActiveRender thread
    private static
    boolean removePopupFromMap(final LookAndFeel sourceLook) {
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
                    changedY = look.anchorY + (look.popupIndex * (NotifyCanvas.HEIGHT + 10));
                }
                else {
                    changedY = look.anchorY - (look.popupIndex * (NotifyCanvas.HEIGHT + 10));
                }

                // now animate that popup to it's new location
                look.tween = animation.to(look, NotifyAccessor.Y_POS, accessor, MOVE_DURATION)
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
                                     })
                                      .start();
            }
        }

        return popupsAreEmpty;
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

    void setProgress(final int progress) {
        notifyCanvas.setProgress(progress);
    }

    int getProgress() {
        return notifyCanvas.getProgress();
    }

    /**
     * we have to remove the active renderer BEFORE we set the visibility status.
     */
    void updatePositionsPre(final boolean visible) {
        if (!visible) {
            boolean popupsAreEmpty = LookAndFeel.removePopupFromMap(this);
            SwingActiveRender.removeActiveRender(notifyCanvas);

            if (popupsAreEmpty) {
                // if there's nothing left, stop the timer.
                SwingActiveRender.removeActiveRenderFrameStart(frameStartHandler);
            }
        }
    }

    /**
     * when using active rendering, we have to add it AFTER we have set the visibility status
     */
    void updatePositionsPost(final boolean visible) {
        if (visible) {

            SwingActiveRender.addActiveRender(notifyCanvas);

            // start if we have previously stopped the timer
            if (!SwingActiveRender.containsActiveRenderFrameStart(frameStartHandler)) {
                LookAndFeel.animation.resetUpdateTime();
                SwingActiveRender.addActiveRenderFrameStart(frameStartHandler);
            }

            LookAndFeel.addPopupToMap(this);
        }
    }
}
