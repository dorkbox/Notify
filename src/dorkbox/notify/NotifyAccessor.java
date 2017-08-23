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

import dorkbox.tweenengine.TweenAccessor;

class NotifyAccessor implements TweenAccessor<LookAndFeel> {

    static final int Y_POS = 1;
    static final int X_Y_POS = 2;
    static final int PROGRESS = 3;


    NotifyAccessor() {
    }

    @Override
    public
    int getValues(final LookAndFeel target, final int tweenType, final float[] returnValues) {
        switch (tweenType) {
            case Y_POS:
                returnValues[0] = (float) target.getParentY();
                return 1;
            case X_Y_POS:
                returnValues[0] = (float) target.getParentX();
                returnValues[1] = (float) target.getParentY();
                return 2;
            case PROGRESS:
                returnValues[0] = (float) target.getProgress();
                return 1;
        }
        return 1;
    }

    @SuppressWarnings({"NumericCastThatLosesPrecision", "UnnecessaryReturnStatement"})
    @Override
    public
    void setValues(final LookAndFeel target, final int tweenType, final float[] newValues) {
        switch (tweenType) {
            case Y_POS:
                target.setParentY((int) newValues[0]);
                return;
            case X_Y_POS:
                target.setParentLocation((int) newValues[0], (int) newValues[1]);
                return;
            case PROGRESS:
                target.setProgress((int) newValues[0]);
                return;
        }
    }
}
