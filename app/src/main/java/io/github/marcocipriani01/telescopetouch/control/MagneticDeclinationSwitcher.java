/*
 * Copyright 2021 Marco Cipriani (@marcocipriani01) and the Sky Map Team
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

package io.github.marcocipriani01.telescopetouch.control;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

import javax.inject.Inject;
import javax.inject.Named;

import io.github.marcocipriani01.telescopetouch.ApplicationConstants;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;

/**
 * Aggregates the RealMagneticDeclinationCalculator and the
 * ZeroMagneticDeclinationCalculator and switches them in the AstronomerModel.
 *
 * @author John Taylor
 */
public class MagneticDeclinationSwitcher implements OnSharedPreferenceChangeListener {

    private static final String TAG = TelescopeTouchApp.getTag(MagneticDeclinationSwitcher.class);
    private final MagneticDeclinationCalculator realCalculator;
    private final SharedPreferences preferences;
    private final MagneticDeclinationCalculator zeroCalculator;
    private final AstronomerModel model;

    /**
     * Constructs a new MagneticDeclinationCalculatorSwitcher.
     *
     * @param model       the object in which to swap the calculator
     * @param preferences a SharedPreferences object which will indicate which
     *                    calculator to use.
     */
    @Inject
    public MagneticDeclinationSwitcher(AstronomerModel model, SharedPreferences preferences,
                                       @Named("zero") MagneticDeclinationCalculator zeroCalculator,
                                       @Named("real") MagneticDeclinationCalculator realCalculator) {
        this.preferences = preferences;
        this.zeroCalculator = zeroCalculator;
        this.realCalculator = realCalculator;
        this.model = model;
    }

    public void init() {
        preferences.registerOnSharedPreferenceChangeListener(this);
        setTheModelsCalculator(preferences);
    }

    private void setTheModelsCalculator(SharedPreferences preferences) {
        if (preferences.getBoolean(ApplicationConstants.MAGNETIC_DECLINATION_PREF, true)) {
            model.setMagneticDeclinationCalculator(realCalculator);
        } else {
            model.setMagneticDeclinationCalculator(zeroCalculator);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (ApplicationConstants.MAGNETIC_DECLINATION_PREF.equals(key)) {
            Log.i(TAG, "Magnetic declination preference changed");
            setTheModelsCalculator(sharedPreferences);
        }
    }
}