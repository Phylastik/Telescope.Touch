/*
 * Copyright 2020 Marco Cipriani (@marcocipriani01)
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

package io.github.marcocipriani01.telescopetouch.indi;

import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import org.indilib.i4j.client.INDIDevice;
import org.indilib.i4j.client.INDINumberElement;
import org.indilib.i4j.client.INDINumberProperty;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDIPropertyListener;
import org.indilib.i4j.client.INDISwitchElement;
import org.indilib.i4j.client.INDISwitchProperty;
import org.indilib.i4j.properties.INDIStandardElement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;

public class INDIFocuser implements INDIPropertyListener {

    private static final String TAG = TelescopeTouchApp.getTag(INDIFocuser.class);
    public final INDIDevice device;
    private final Handler uiHandler;
    private final Set<FocuserListener> listeners = new HashSet<>();
    public volatile INDISwitchProperty directionP = null;
    public volatile INDISwitchElement inwardDirectionE = null;
    public volatile INDISwitchElement outwardDirectionE = null;
    public volatile INDINumberProperty relPositionP = null;
    public volatile INDINumberElement relPositionE = null;
    public volatile INDINumberProperty absPositionP = null;
    public volatile INDINumberElement absPositionE = null;
    public volatile INDINumberProperty syncPositionP = null;
    public volatile INDINumberElement syncPositionE = null;
    public volatile INDINumberProperty speedP = null;
    public volatile INDINumberElement speedE = null;
    public volatile INDISwitchProperty abortP = null;
    public volatile INDISwitchElement abortE = null;

    public INDIFocuser(INDIDevice device, Handler uiHandler) {
        this.device = device;
        this.uiHandler = uiHandler;
    }

    public static boolean isFocuserProp(INDIProperty<?> property) {
        String name = property.getName();
        return name.startsWith("FOCUS_") || name.equals("ABS_FOCUS_POSITION") || name.equals("REL_FOCUS_POSITION");
    }

    @Override
    public void propertyChanged(INDIProperty<?> indiProperty) {
        if (indiProperty == absPositionP) {
            uiHandler.post(() -> {
                synchronized (listeners) {
                    for (FocuserListener listener : listeners) {
                        listener.onFocuserPositionChange((int) (double) absPositionE.getValue());
                    }
                }
            });
        } else if (indiProperty == speedP) {
            uiHandler.post(() -> {
                synchronized (listeners) {
                    for (FocuserListener listener : listeners) {
                        listener.onFocuserSpeedChange((int) (double) speedE.getValue());
                    }
                }
            });
        }
    }

    public synchronized void processNewProp(INDIProperty<?> property) {
        String name = property.getName(), devName = device.getName();
        Log.i(TAG, "New Property (" + name + ") added to focuser " + devName
                + ", elements: " + Arrays.toString(property.getElementNames()));
        switch (name) {
            case "ABS_FOCUS_POSITION":
                if ((absPositionE = (INDINumberElement) property.getElement(INDIStandardElement.FOCUS_ABSOLUTE_POSITION)) != null) {
                    property.addINDIPropertyListener(this);
                    absPositionP = (INDINumberProperty) property;
                }
                break;
            case "REL_FOCUS_POSITION":
                if ((relPositionE = (INDINumberElement) property.getElement(INDIStandardElement.FOCUS_RELATIVE_POSITION)) != null) {
                    relPositionP = (INDINumberProperty) property;
                }
                break;
            case "FOCUS_MOTION":
                if (((inwardDirectionE = (INDISwitchElement) property.getElement(INDIStandardElement.FOCUS_INWARD)) != null)
                        && ((outwardDirectionE = (INDISwitchElement) property.getElement(INDIStandardElement.FOCUS_OUTWARD)) != null)) {
                    directionP = (INDISwitchProperty) property;
                }
                break;
            case "FOCUS_ABORT_MOTION":
                if ((abortE = (INDISwitchElement) property.getElement(INDIStandardElement.ABORT)) != null) {
                    abortP = (INDISwitchProperty) property;
                }
                break;
            case "FOCUS_SPEED":
                if ((speedE = (INDINumberElement) property.getElement(INDIStandardElement.FOCUS_SPEED_VALUE)) != null) {
                    speedP = (INDINumberProperty) property;
                    property.addINDIPropertyListener(this);
                }
                break;
            case "FOCUS_SYNC":
                if ((syncPositionE = (INDINumberElement) property.getElement(INDIStandardElement.FOCUS_SYNC_VALUE)) != null) {
                    syncPositionP = (INDINumberProperty) property;
                }
                break;
            default:
                return;
        }
        uiHandler.post(() -> {
            synchronized (listeners) {
                for (FocuserListener listener : listeners) {
                    listener.onFocuserFunctionsChange();
                }
            }
        });
    }

    public synchronized boolean removeProp(INDIProperty<?> property) {
        switch (property.getName()) {
            case "REL_FOCUS_POSITION":
                relPositionE = null;
                relPositionP = null;
                break;
            case "FOCUS_MOTION":
                inwardDirectionE = outwardDirectionE = null;
                directionP = null;
                break;
            case "FOCUS_ABORT_MOTION":
                abortE = null;
                abortP = null;
                break;
            case "ABS_FOCUS_POSITION":
                property.removeINDIPropertyListener(this);
                absPositionP = null;
                absPositionE = null;
                break;
            case "FOCUS_SPEED":
                property.removeINDIPropertyListener(this);
                speedP = null;
                speedE = null;
                break;
            case "FOCUS_SYNC":
                syncPositionP = null;
                syncPositionE = null;
                break;
            default:
                return false;
        }
        uiHandler.post(() -> {
            synchronized (listeners) {
                for (FocuserListener listener : listeners) {
                    listener.onFocuserFunctionsChange();
                }
            }
        });
        return (directionP == null) && (relPositionP == null) && (absPositionP == null) &&
                (syncPositionP == null) && (speedP == null) && (abortP == null);
    }

    private void terminate() {
        synchronized (listeners) {
            listeners.clear();
        }
        relPositionP = null;
        relPositionE = null;
        absPositionP = null;
        absPositionE = null;
        directionP = null;
        outwardDirectionE = inwardDirectionE = null;
        syncPositionP = null;
        syncPositionE = null;
        speedP = null;
        speedE = null;
        abortP = null;
        abortE = null;
    }

    @NonNull
    @Override
    public String toString() {
        return device.getName();
    }

    public interface FocuserListener {

        void onFocuserFunctionsChange();

        void onFocuserPositionChange(int position);

        void onFocuserSpeedChange(int value);
    }
}