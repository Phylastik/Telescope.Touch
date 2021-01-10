package io.github.marcocipriani01.telescopetouch.control;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApplication;
import io.github.marcocipriani01.telescopetouch.units.LatLong;

/**
 * Sets the AstronomerModel's (and thus the user's) position using one of the
 * network, GPS or user-set preferences.
 *
 * @author John Taylor
 */
public class LocationController extends AbstractController implements LocationListener {

    // Must match the key in the preferences file.
    public static final String NO_AUTO_LOCATE = "no_auto_locate";
    // Must match the key in the preferences file.
    private static final String FORCE_GPS = "force_gps";
    private static final int MINIMUM_DISTANCE_BEFORE_UPDATE_METRES = 2000;
    private static final int LOCATION_UPDATE_TIME_MILLISECONDS = 600000;
    private static final String TAG = TelescopeTouchApplication.getTag(LocationController.class);
    private static final float MIN_DIST_TO_SHOW_TOAST_DEGS = 0.01f;

    private final Context context;
    private final LocationManager locationManager;
    /**
     * Last known provider;
     */
    private String currentProvider = "unknown";

    @Inject
    public LocationController(Context context, LocationManager locationManager) {
        this.context = context;
        if (locationManager != null) {
            Log.d(TAG, "Got location Manager");
        } else {
            Log.d(TAG, "Didn't get location manager");
        }
        this.locationManager = locationManager;
    }

    @Override
    public void start() {
        Log.d(TAG, "LocationController start");
        boolean noAutoLocate = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(NO_AUTO_LOCATE, false);
        boolean forceGps = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(FORCE_GPS, false);

        if (noAutoLocate) {
            Log.d(TAG, "User has elected to set location manually.");
            setLocationFromPrefs();
            Log.d(TAG, "LocationController -start");
            return;
        }

        try {
            if (locationManager == null) {
                // TODO(johntaylor): find out under what circumstances this can happen.
                Log.e(TAG, "Location manager was null - using preferences");
                setLocationFromPrefs();
                return;
            }

            Criteria locationCriteria = new Criteria();
            locationCriteria.setAccuracy(forceGps ? Criteria.ACCURACY_FINE : Criteria.ACCURACY_COARSE);
            locationCriteria.setAltitudeRequired(false);
            locationCriteria.setBearingRequired(false);
            locationCriteria.setCostAllowed(true);
            locationCriteria.setSpeedRequired(false);
            locationCriteria.setPowerRequirement(Criteria.POWER_LOW);

            String locationProvider = locationManager.getBestProvider(locationCriteria, true);
            if (locationProvider == null) {
                Log.w(TAG, "No location provider is enabled");
                String possibleLocationProvider = locationManager.getBestProvider(locationCriteria, false);
                if (possibleLocationProvider == null) {
                    Log.i(TAG, "No location provider is even available");
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
                    alertDialogBuilder
                            .setTitle("Warning")
                            .setMessage(R.string.location_no_auto)
                            .setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.dismiss())
                            .create();
                    setLocationFromPrefs();
                    return;
                }
                AlertDialog.Builder alertDialog = getSwitchOnGPSDialog();
                alertDialog.show();
                return;
            } else {
                Log.d(TAG, "Got location provider " + locationProvider);
            }

            locationManager.requestLocationUpdates(locationProvider, LOCATION_UPDATE_TIME_MILLISECONDS,
                    MINIMUM_DISTANCE_BEFORE_UPDATE_METRES,
                    this);

            Location location = locationManager.getLastKnownLocation(locationProvider);
            if (location != null) {
                LatLong myLocation = new LatLong(location.getLatitude(), location.getLongitude());
                setLocationInModel(myLocation, location.getProvider());
            }

        } catch (SecurityException securityException) {
            Log.d(TAG, "Caught " + securityException);
            Log.d(TAG, "Most likely user has not enabled this permission");
        }

        Log.d(TAG, "LocationController -start");
    }

    private void setLocationInModel(LatLong location, String provider) {
        LatLong oldLocation = model.getLocation();
        if (location.distanceFrom(oldLocation) > MIN_DIST_TO_SHOW_TOAST_DEGS) {
            Log.d(TAG, "Informing user of change of location");
            showLocationToUser(location, provider);
        } else {
            Log.d(TAG, "Location not changed sufficiently to tell the user");
        }
        currentProvider = provider;
        model.setLocation(location);
    }

    public String getCurrentProvider() {
        return currentProvider;
    }

    public LatLong getCurrentLocation() {
        return model.getLocation();
    }

    private AlertDialog.Builder getSwitchOnGPSDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(R.string.location_offer_to_enable_gps_title);
        dialog.setMessage(R.string.location_offer_to_enable);
        dialog.setPositiveButton(android.R.string.ok, (dialog12, which) -> {
            Log.d(TAG, "Sending to editor location prefs page");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            context.startActivity(intent);
        });
        dialog.setNegativeButton(android.R.string.cancel, (dialog1, which) -> {
            Log.d(TAG, "User doesn't want to enable location.");
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            Editor editor = prefs.edit();
            editor.putBoolean(NO_AUTO_LOCATE, true);
            editor.apply();
            setLocationFromPrefs();
        });
        return dialog;
    }

    private void setLocationFromPrefs() {
        Log.d(TAG, "Setting location from preferences");
        String longitude_s = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("longitude", "0");
        String latitude_s = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("latitude", "0");

        float longitude = 0, latitude = 0;
        try {
            longitude = Float.parseFloat(longitude_s);
            latitude = Float.parseFloat(latitude_s);
        } catch (NumberFormatException nfe) {
            Log.e(TAG, "Error parsing latitude or longitude preference");
            Toast.makeText(context, R.string.malformed_loc_error, Toast.LENGTH_SHORT).show();
        }

        Location location = new Location(context.getString(R.string.preferences));
        location.setLatitude(latitude);
        location.setLongitude(longitude);

        Log.d(TAG, "Latitude " + longitude);
        Log.d(TAG, "Longitude " + latitude);
        LatLong myPosition = new LatLong(latitude, longitude);
        setLocationInModel(myPosition, context.getString(R.string.preferences));
    }

    @Override
    public void stop() {
        Log.d(TAG, "LocationController stop");

        if (locationManager == null) {
            return;
        }
        locationManager.removeUpdates(this);

        Log.d(TAG, "LocationController -stop");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "LocationController onLocationChanged");

        if (location == null) {
            Log.e(TAG, "Didn't get location even though onLocationChanged called");
            setLocationFromPrefs();
            return;
        }

        LatLong newLocation = new LatLong(location.getLatitude(), location.getLongitude());

        Log.d(TAG, "Latitude " + newLocation.getLatitude());
        Log.d(TAG, "Longitude " + newLocation.getLongitude());
        setLocationInModel(newLocation, location.getProvider());

        // Only need get the location once.
        locationManager.removeUpdates(this);

        Log.d(TAG, "LocationController -onLocationChanged");
    }

    private void showLocationToUser(LatLong location, String provider) {
        // TODO(johntaylor): move this notification to a separate thread)
        Log.d(TAG, "Reverse geocoding location");
        Geocoder geoCoder = new Geocoder(context);
        List<Address> addresses = new ArrayList<>();
        String place;
        try {
            addresses = geoCoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            Log.e(TAG, "Unable to reverse geocode location " + location);
        }

        if (addresses == null || addresses.isEmpty()) {
            Log.d(TAG, "No addresses returned");
            place = String.format(context.getString(R.string.location_long_lat), location.getLongitude(),
                    location.getLatitude());
        } else {
            place = getSummaryOfPlace(location, addresses.get(0));
        }

        Log.d(TAG, "Location set to " + place);

        String messageTemplate = context.getString(R.string.location_set_auto);
        String message = String.format(messageTemplate, provider, place);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private String getSummaryOfPlace(LatLong location, Address address) {
        String template = context.getString(R.string.location_long_lat);
        String longLat = String.format(template, location.getLongitude(), location.getLatitude());
        if (address == null) {
            return longLat;
        }
        String place = address.getLocality();
        if (place == null) {
            place = address.getSubAdminArea();
        }
        if (place == null) {
            place = address.getAdminArea();
        }
        if (place == null) {
            place = longLat;
        }
        return place;
    }

    @Override
    public void onProviderDisabled(String provider) {
        // No action.
    }

    @Override
    public void onProviderEnabled(String provider) {
        // No action.
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // No action.
    }
}