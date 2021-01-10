package io.github.marcocipriani01.telescopetouch;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import androidx.core.content.pm.PackageInfoCompat;
import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.github.marcocipriani01.telescopetouch.layers.LayerManager;

/**
 * The main Stardroid Application class.
 *
 * @author John Taylor
 */
public class TelescopeTouchApplication extends Application {

    private static final String TAG = getTag(TelescopeTouchApplication.class);
    @Inject
    SharedPreferences preferences;
    // We keep a reference to this just to start it initializing.
    @Inject
    LayerManager layerManager;
    @Inject
    SensorManager sensorManager;

    private ApplicationComponent component;

    /**
     * Returns either the name of the sensor or a string version of the sensor type id, depending
     * on the supported OS level along with some context.
     */
    public static String getSafeNameForSensor(Sensor sensor) {
        return "Sensor type: " + sensor.getStringType() + ": " + sensor.getType();
    }

    /**
     * Returns the Tag for a class to be used in Android logging statements
     */
    public static String getTag(Object o) {
        if (o instanceof Class<?>) {
            return ApplicationConstants.APP_NAME + "." + ((Class<?>) o).getSimpleName();
        }
        return ApplicationConstants.APP_NAME + "." + o.getClass().getSimpleName();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "StardroidApplication: onCreate");
        super.onCreate();

        component = DaggerApplicationComponent.builder()
                .applicationModule(new ApplicationModule(this))
                .build();
        component.inject(this);

        Log.i(TAG, "OS Version: " + android.os.Build.VERSION.RELEASE
                + "(" + android.os.Build.VERSION.SDK_INT + ")");
        String versionName = getVersionName();
        Log.i(TAG, "Sky Map version " + versionName + " build " + getVersion());

        // This populates the default values from the preferences XML file. See
        // {@link DefaultValues} for more details.
        PreferenceManager.setDefaultValues(this, R.xml.preference_screen, false);
        performFeatureCheck();
        Log.d(TAG, "StardroidApplication: -onCreate");
    }

    public ApplicationComponent getApplicationComponent() {
        return component;
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    /**
     * Returns the version string for Sky Map.
     */
    public String getVersionName() {
        // TODO(jontayler): update to use the info created by gradle.
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(this.getPackageName(), 0);
            return info.versionName;
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to obtain package info");
            return "Unknown";
        }
    }

    /**
     * Returns the build number for Sky Map.
     */
    public long getVersion() {
        PackageManager packageManager = getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(this.getPackageName(), 0);
            return PackageInfoCompat.getLongVersionCode(info);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Unable to obtain package info");
            return -1;
        }
    }

    /**
     * Check what features are available to this phone and report back to analytics
     * so we can judge when to add/drop support.
     */
    private void performFeatureCheck() {
        if (sensorManager == null) {
            Log.e(TAG, "No sensor manager");
            return;
        }

        // Check for a particularly strange combo - it would be weird to have a rotation sensor
        // but no accelerometer or magnetic field sensor
        boolean hasRotationSensor = false;
        if (hasDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)) {
            if (hasDefaultSensor(Sensor.TYPE_ACCELEROMETER) && hasDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
                    && hasDefaultSensor(Sensor.TYPE_GYROSCOPE)) {
                hasRotationSensor = true;
            }
        }

        // Enable Gyro if available and user hasn't already disabled it.
        if (!preferences.contains(ApplicationConstants.SHARED_PREFERENCE_DISABLE_GYRO)) {
            preferences.edit().putBoolean(
                    ApplicationConstants.SHARED_PREFERENCE_DISABLE_GYRO, !hasRotationSensor).apply();
        }

        // Lastly a dump of all the sensors.
        Log.d(TAG, "All sensors:");
        List<Sensor> allSensors = sensorManager.getSensorList(Sensor.TYPE_ALL);
        Set<String> sensorTypes = new HashSet<>();
        for (Sensor sensor : allSensors) {
            Log.i(TAG, sensor.getName());
            sensorTypes.add(getSafeNameForSensor(sensor));
        }
        Log.d(TAG, "All sensors summary:");
        for (String sensorType : sensorTypes) {
            Log.i(TAG, sensorType);
        }
    }

    private boolean hasDefaultSensor(int sensorType) {
        if (sensorManager == null) {
            return false;
        }
        Sensor sensor = sensorManager.getDefaultSensor(sensorType);
        if (sensor == null) {
            return false;
        }
        SensorEventListener dummy = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                // Nothing
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Nothing
            }
        };
        boolean success = sensorManager.registerListener(
                dummy, sensor, SensorManager.SENSOR_DELAY_UI);
        sensorManager.unregisterListener(dummy);
        return success;
    }
}