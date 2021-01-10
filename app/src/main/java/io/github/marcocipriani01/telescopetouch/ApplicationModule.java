package io.github.marcocipriani01.telescopetouch;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import io.github.marcocipriani01.telescopetouch.control.AstronomerModel;
import io.github.marcocipriani01.telescopetouch.control.AstronomerModelImpl;
import io.github.marcocipriani01.telescopetouch.control.MagneticDeclinationCalculator;
import io.github.marcocipriani01.telescopetouch.control.RealMagneticDeclinationCalculator;
import io.github.marcocipriani01.telescopetouch.control.ZeroMagneticDeclinationCalculator;
import io.github.marcocipriani01.telescopetouch.layers.EclipticLayer;
import io.github.marcocipriani01.telescopetouch.layers.GridLayer;
import io.github.marcocipriani01.telescopetouch.layers.HorizonLayer;
import io.github.marcocipriani01.telescopetouch.layers.LayerManager;
import io.github.marcocipriani01.telescopetouch.layers.MeteorShowerLayer;
import io.github.marcocipriani01.telescopetouch.layers.NewConstellationsLayer;
import io.github.marcocipriani01.telescopetouch.layers.NewMessierLayer;
import io.github.marcocipriani01.telescopetouch.layers.NewStarsLayer;
import io.github.marcocipriani01.telescopetouch.layers.PlanetsLayer;
import io.github.marcocipriani01.telescopetouch.layers.SkyGradientLayer;

/**
 * Dagger module
 * Created by johntaylor on 3/26/16.
 */
@Module
public class ApplicationModule {
    private static final String TAG = TelescopeTouchApp.getTag(ApplicationModule.class);
    private final TelescopeTouchApp app;

    public ApplicationModule(TelescopeTouchApp app) {
        Log.d(TAG, "Creating application module for " + app);
        this.app = app;
    }

    @Provides
    @Singleton
    TelescopeTouchApp provideApplication() {
        return app;
    }

    @Provides
    Context provideContext() {
        return app;
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences() {
        Log.d(TAG, "Providing shared preferences");
        return PreferenceManager.getDefaultSharedPreferences(app);
    }

    @Provides
    @Singleton
    LocationManager provideLocationManager() {
        return ContextCompat.getSystemService(app, LocationManager.class);
    }

    @Provides
    @Singleton
    AstronomerModel provideAstronomerModel(
            @Named("zero") MagneticDeclinationCalculator magneticDeclinationCalculator) {
        return new AstronomerModelImpl(magneticDeclinationCalculator);
    }

    @Provides
    @Singleton
    @Named("zero")
    MagneticDeclinationCalculator provideDefaultMagneticDeclinationCalculator() {
        return new ZeroMagneticDeclinationCalculator();
    }

    @Provides
    @Singleton
    @Named("real")
    MagneticDeclinationCalculator provideRealMagneticDeclinationCalculator() {
        return new RealMagneticDeclinationCalculator();
    }

    @Provides
    @Singleton
    ExecutorService provideBackgroundExecutor() {
        return new ScheduledThreadPoolExecutor(1);
    }

    @Provides
    @Singleton
    AssetManager provideAssetManager() {
        return app.getAssets();
    }

    @Provides
    @Singleton
    Resources provideResources() {
        return app.getResources();
    }

    @Provides
    @Singleton
    SensorManager provideSensorManager() {
        return ContextCompat.getSystemService(app, SensorManager.class);
    }

    @Provides
    @Singleton
    ConnectivityManager provideConnectivityManager() {
        return ContextCompat.getSystemService(app, ConnectivityManager.class);
    }

    @Provides
    @Singleton
    LayerManager provideLayerManager(
            AssetManager assetManager, Resources resources, AstronomerModel model,
            SharedPreferences preferences) {
        Log.i(TAG, "Initializing LayerManager");
        LayerManager layerManager = new LayerManager(preferences);
        layerManager.addLayer(new NewStarsLayer(assetManager, resources));
        layerManager.addLayer(new NewMessierLayer(assetManager, resources));
        layerManager.addLayer(new NewConstellationsLayer(assetManager, resources));
        layerManager.addLayer(new PlanetsLayer(model, resources, preferences));
        layerManager.addLayer(new MeteorShowerLayer(model, resources));
        layerManager.addLayer(new GridLayer(resources, 24, 9));
        layerManager.addLayer(new HorizonLayer(model, resources));
        layerManager.addLayer(new EclipticLayer(resources));
        layerManager.addLayer(new SkyGradientLayer(model, resources));

        layerManager.initialize();
        return layerManager;
    }
}