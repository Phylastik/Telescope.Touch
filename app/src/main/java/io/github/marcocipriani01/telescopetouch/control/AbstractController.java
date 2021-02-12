/*
 * Copyright 2020 Marco Cipriani (@marcocipriani01) and the Sky Map Team
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

import android.util.Log;

import javax.inject.Inject;

import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;

/**
 * Implements some of the boilerplate of a {@link Controller}.
 *
 * @author John Taylor
 */
public abstract class AbstractController implements Controller {

    private static final String TAG = TelescopeTouchApp.getTag(AbstractController.class);
    protected boolean enabled = true;
    @Inject
    AstronomerModel model;

    @Override
    public void setModel(AstronomerModel model) {
        this.model = model;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (enabled) {
            Log.d(TAG, "Enabling controller " + this);
        } else {
            Log.d(TAG, "Disabling controller " + this);
        }
        this.enabled = enabled;
    }
}