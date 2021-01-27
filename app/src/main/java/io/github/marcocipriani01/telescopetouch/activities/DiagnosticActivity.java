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

package io.github.marcocipriani01.telescopetouch.activities;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.inject.Inject;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;
import io.github.marcocipriani01.telescopetouch.activities.util.DarkerModeManager;
import io.github.marcocipriani01.telescopetouch.activities.util.SensorAccuracyDecoder;
import io.github.marcocipriani01.telescopetouch.control.AstronomerModel;
import io.github.marcocipriani01.telescopetouch.control.LocationController;
import io.github.marcocipriani01.telescopetouch.units.GeocentricCoordinates;
import io.github.marcocipriani01.telescopetouch.units.LatLong;

public class DiagnosticActivity extends InjectableActivity implements SensorEventListener {

    private static final String TAG = TelescopeTouchApp.getTag(DiagnosticActivity.class);
    private static final int UPDATE_PERIOD_MILLIS = 500;
    private final Set<Sensor> knownSensorAccuracies = new HashSet<>();
    @Inject
    TelescopeTouchApp app;
    @Inject
    SensorManager sensorManager;
    @Inject
    ConnectivityManager connectivityManager;
    @Inject
    LocationManager locationManager;
    @Inject
    LocationController locationController;
    @Inject
    AstronomerModel model;
    @Inject
    Handler handler;
    @Inject
    SensorAccuracyDecoder sensorAccuracyDecoder;
    private Sensor accelSensor;
    private Sensor magSensor;
    private Sensor gyroSensor;
    private Sensor rotationVectorSensor;
    private Sensor lightSensor;
    private boolean continueUpdates;
    private DarkerModeManager darkerModeManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerDiagnosticActivityComponent.builder().applicationComponent(
                getApplicationComponent()).diagnosticActivityModule(new DiagnosticActivityModule(this))
                .build().inject(this);
        darkerModeManager = new DarkerModeManager(getWindow(), null, PreferenceManager.getDefaultSharedPreferences(this));
        setTheme(darkerModeManager.getPref() ? R.style.DarkerAppTheme : R.style.AppTheme);
        setContentView(R.layout.activity_diagnostic);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        setText(R.id.diagnose_phone_txt, Build.MODEL + " (" + Build.HARDWARE + ") " +
                Locale.getDefault().getLanguage());
        String androidVersion = String.format(Build.VERSION.RELEASE + " (%d)", Build.VERSION.SDK_INT);
        setText(R.id.diagnose_android_version_txt, androidVersion);

        String skyMapVersion = String.format(
                app.getVersionName() + " (%d)", app.getVersion());
        setText(R.id.diagnose_skymap_version_txt, skyMapVersion);
    }

    @Override
    public void onResume() {
        super.onResume();
        onResumeSensors();
        continueUpdates = true;
        handler.post(new Runnable() {
            public void run() {
                updateLocation();
                updateModel();
                updateNetwork();
                if (continueUpdates) {
                    handler.postDelayed(this, UPDATE_PERIOD_MILLIS);
                }
            }
        });
        darkerModeManager.start();
    }

    private void onResumeSensors() {
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        int absentSensorColor = getResources().getColor(R.color.absent_sensor);
        if (accelSensor == null) {
            setColor(R.id.diagnose_accelerometer_values_txt, absentSensorColor);
        } else {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        magSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magSensor == null) {
            setColor(R.id.diagnose_compass_values_txt, absentSensorColor);
        } else {
            sensorManager.registerListener(this, magSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (gyroSensor == null) {
            setColor(R.id.diagnose_gyro_values_txt, absentSensorColor);
        } else {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if (rotationVectorSensor == null) {
            setColor(R.id.diagnose_rotation_values_txt, absentSensorColor);
        } else {
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if (lightSensor == null) {
            setColor(R.id.diagnose_light_values_txt, absentSensorColor);
        } else {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void updateLocation() {
        // TODO(johntaylor): add other things like number of satellites and status
        String gpsStatusMessage;
        try {
            LocationProvider gps = locationManager.getProvider(LocationManager.GPS_PROVIDER);
            boolean gpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            if (gps == null) {
                gpsStatusMessage = getString(R.string.no_gps);
            } else {
                gpsStatusMessage = gpsStatus ? getString(R.string.enabled) : getString(R.string.disabled);
            }
        } catch (SecurityException ex) {
            gpsStatusMessage = getString(R.string.permission_disabled);
        }
        setText(R.id.diagnose_gps_status_txt, gpsStatusMessage);
        LatLong currentLocation = locationController.getCurrentLocation();
        String locationMessage = currentLocation.getLatitude() + ", " + currentLocation.getLongitude();
        // Current provider not working    + " (" + locationController.getCurrentProvider() + ")";
        setText(R.id.diagnose_location_txt, locationMessage);
    }

    @SuppressLint("SimpleDateFormat")
    private void updateModel() {
        float magCorrection = model.getMagneticCorrection();
        setText(R.id.diagnose_magnetic_correction_txt,
                Math.abs(magCorrection) + " " + (magCorrection > 0
                        ? getString(R.string.east) : getString(R.string.west)) + " "
                        + getString(R.string.degrees));
        AstronomerModel.Pointing pointing = model.getPointing();
        GeocentricCoordinates lineOfSight = pointing.getLineOfSight();
        setText(R.id.diagnose_pointing_txt, getDegreeInHour(lineOfSight.getRa()) + ", " + lineOfSight.getDec());
        Calendar nowTime = model.getTime();
        SimpleDateFormat dateFormatUtc = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        dateFormatUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat dateFormatLocal = new SimpleDateFormat("yyyy-MMM-dd HH:mm:ss");
        setText(R.id.diagnose_utc_datetime_txt, dateFormatUtc.format(nowTime.getTime()));
        setText(R.id.diagnose_local_datetime_txt, dateFormatLocal.format(nowTime.getTime()));
    }

    @Override
    public void onPause() {
        super.onPause();
        continueUpdates = false;
        sensorManager.unregisterListener(this);
        darkerModeManager.stop();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        knownSensorAccuracies.add(sensor);
        Log.d(TAG, "set size" + knownSensorAccuracies.size());
        int sensorViewId;
        if (sensor == accelSensor) {
            sensorViewId = R.id.diagnose_accelerometer_values_txt;
        } else if (sensor == magSensor) {
            sensorViewId = R.id.diagnose_compass_values_txt;
        } else if (sensor == gyroSensor) {
            sensorViewId = R.id.diagnose_gyro_values_txt;
        } else if (sensor == rotationVectorSensor) {
            sensorViewId = R.id.diagnose_rotation_values_txt;
        } else if (sensor == lightSensor) {
            sensorViewId = R.id.diagnose_light_values_txt;
        } else {
            Log.e(TAG, "Receiving accuracy change for unknown sensor " + sensor);
            return;
        }
        setColor(sensorViewId, sensorAccuracyDecoder.getColorForAccuracy(accuracy));
    }

    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (!knownSensorAccuracies.contains(sensor)) {
            onAccuracyChanged(sensor, event.accuracy);
        }
        int valuesViewId;
        if (sensor == accelSensor) {
            valuesViewId = R.id.diagnose_accelerometer_values_txt;
        } else if (sensor == magSensor) {
            valuesViewId = R.id.diagnose_compass_values_txt;
        } else if (sensor == gyroSensor) {
            valuesViewId = R.id.diagnose_gyro_values_txt;
        } else if (sensor == rotationVectorSensor) {
            valuesViewId = R.id.diagnose_rotation_values_txt;
        } else if (sensor == lightSensor) {
            valuesViewId = R.id.diagnose_light_values_txt;
        } else {
            Log.e(TAG, "Receiving values for unknown sensor " + sensor);
            return;
        }
        float[] values = event.values;
        setArrayValuesInUi(valuesViewId, values);

        // Something special for rotation sensor - convert to a matrix.
        if (sensor == rotationVectorSensor) {
            float[] matrix = new float[9];
            SensorManager.getRotationMatrixFromVector(matrix, event.values);
            for (int row = 0; row < 3; ++row) {
                switch (row) {
                    case 0:
                        valuesViewId = R.id.diagnose_rotation_matrix_row1_txt;
                        break;
                    case 1:
                        valuesViewId = R.id.diagnose_rotation_matrix_row2_txt;
                        break;
                    case 2:
                    default:
                        valuesViewId = R.id.diagnose_rotation_matrix_row3_txt;
                }
                float[] rowValues = new float[3];
                System.arraycopy(matrix, row * 3, rowValues, 0, 3);
                setArrayValuesInUi(valuesViewId, rowValues);
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private void setArrayValuesInUi(int valuesViewId, float[] values) {
        StringBuilder valuesText = new StringBuilder();
        for (float value : values) {
            valuesText.append(String.format("%.2f", value));
            valuesText.append(',');
        }
        valuesText.setLength(valuesText.length() - 1);
        setText(valuesViewId, valuesText.toString());
    }

    private void updateNetwork() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        String message = isConnected ? getString(R.string.connected) : getString(R.string.disconnected);
        if (isConnected) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                message += getString(R.string.wifi);
            }
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                message += getString(R.string.cell_network);
            }
        }
        setText(R.id.diagnose_network_status_txt, message);
    }

    private void setText(int viewId, String text) {
        ((TextView) findViewById(viewId)).setText(text);
    }

    private void setColor(int viewId, int color) {
        ((TextView) findViewById(viewId)).setTextColor(color);
    }

    private String getDegreeInHour(float deg) {
        int h = (int) deg / 15;
        int m = (int) ((deg / 15 - h) * 60);
        int s = (int) ((((deg / 15 - h) * 60) - m) * 60);
        return h + "h " + m + "m " + s + "s ";
    }
}