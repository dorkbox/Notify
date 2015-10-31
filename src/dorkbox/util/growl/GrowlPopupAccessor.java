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

import dorkbox.util.tweenengine.TweenAccessor;

class GrowlPopupAccessor implements TweenAccessor<GrowlPopup> {

    GrowlPopupAccessor() {
    }

    @Override
    public
    int getValues(final GrowlPopup target, final int tweenType, final float[] returnValues) {
        switch (tweenType) {
            case 0:
                returnValues[0] = target.getOpacity_Compat();
                return 1;
            case 1:
                returnValues[0] = (float) target.getY();
                return 1;
            case 2:
                returnValues[0] = (float) target.getX();
                returnValues[1] = (float) target.getY();
                return 2;
        }
        return 1;
    }

    @Override
    public
    void setValues(final GrowlPopup target, final int tweenType, final float[] newValues) {
        switch (tweenType) {
            case 0:
                target.setOpacity_Compat(newValues[0]);
                return;
            case 1:
                //noinspection NumericCastThatLosesPrecision
                target.setY((int) newValues[0]);
            case 2:
                //noinspection NumericCastThatLosesPrecision
                target.setLocation((int) newValues[0], (int) newValues[1]);
        }
    }
}
