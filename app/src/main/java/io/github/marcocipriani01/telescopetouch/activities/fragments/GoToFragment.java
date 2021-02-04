/*
 * Copyright (C) 2020  Marco Cipriani (@marcocipriani01)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package io.github.marcocipriani01.telescopetouch.activities.fragments;

import android.app.Activity;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.indilib.i4j.Constants;
import org.indilib.i4j.client.INDIDevice;
import org.indilib.i4j.client.INDIDeviceListener;
import org.indilib.i4j.client.INDINumberElement;
import org.indilib.i4j.client.INDINumberProperty;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDIPropertyListener;
import org.indilib.i4j.client.INDIServerConnection;
import org.indilib.i4j.client.INDIServerConnectionListener;
import org.indilib.i4j.client.INDISwitchElement;
import org.indilib.i4j.client.INDISwitchProperty;
import org.indilib.i4j.client.INDIValueException;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.github.marcocipriani01.telescopetouch.ApplicationConstants;
import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.activities.MainActivity;
import io.github.marcocipriani01.telescopetouch.astronomy.EquatorialCoordinates;
import io.github.marcocipriani01.telescopetouch.astronomy.StarsPrecession;
import io.github.marcocipriani01.telescopetouch.catalog.Catalog;
import io.github.marcocipriani01.telescopetouch.catalog.CatalogArrayAdapter;
import io.github.marcocipriani01.telescopetouch.catalog.CatalogEntry;
import io.github.marcocipriani01.telescopetouch.catalog.DSOEntry;
import io.github.marcocipriani01.telescopetouch.catalog.StarEntry;
import io.github.marcocipriani01.telescopetouch.indi.PropUpdater;
import io.github.marcocipriani01.telescopetouch.sensors.LocationHelper;

import static io.github.marcocipriani01.telescopetouch.TelescopeTouchApp.connectionManager;

/**
 * Allows the user to look for an astronomical object and slew the telescope.
 */
public class GoToFragment extends ActionFragment
        implements SearchView.OnQueryTextListener, Catalog.CatalogLoadingListener,
        INDIServerConnectionListener, INDIPropertyListener, INDIDeviceListener, CatalogArrayAdapter.CatalogItemListener {

    private static final Catalog catalog = new Catalog();
    private static String requestedSearch = null;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences preferences;
    private CatalogArrayAdapter entriesAdapter;
    private MenuItem searchMenu;
    private SearchView searchView;
    private RecyclerView list;
    private ProgressBar progressBar;
    private TextView emptyLabel;
    private LocationHelper locationHelper;
    private Location location = null;
    private boolean searching = false;
    // INDI properties
    private INDINumberProperty telescopeCoordP = null;
    private INDINumberElement telescopeCoordRA = null;
    private INDINumberElement telescopeCoordDec = null;
    private INDISwitchProperty telescopeOnCoordSetP = null;
    private INDISwitchElement telescopeOnCoordSetSync = null;
    private INDISwitchElement telescopeOnCoordSetSlew = null;
    private INDISwitchElement telescopeOnCoordSetTrack = null;

    public static void setRequestedSearch(String query) {
        requestedSearch = query;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_goto, container, false);
        searchMenu = rootView.<Toolbar>findViewById(R.id.goto_toolbar).getMenu().add(R.string.mount_goto);
        searchMenu.setIcon(R.drawable.search);
        searchMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        searchMenu.setVisible(catalog.isReady());
        searchView = new SearchView(context);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(this);
        searchMenu.setActionView(searchView);
        searchView.setImeOptions(searchView.getImeOptions() | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        searching = false;
        searchView.setOnSearchClickListener(v -> {
            searching = true;
            notifyActionChange();
        });
        searchView.setOnCloseListener(() -> {
            searching = false;
            notifyActionChange();
            return false;
        });
        list = rootView.findViewById(R.id.goto_database_list);
        entriesAdapter = new CatalogArrayAdapter(context, catalog);
        list.setAdapter(entriesAdapter);
        list.setLayoutManager(new LinearLayoutManager(context));
        entriesAdapter.setCatalogItemListener(this);
        emptyLabel = rootView.findViewById(R.id.goto_empy_label);
        progressBar = rootView.findViewById(R.id.goto_loading);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        locationHelper = new LocationHelper(context) {
            @Override
            protected void onLocationOk(Location location) {
                GoToFragment.this.location = location;
                entriesAdapter.setLocation(location);
            }

            @Override
            protected void requestLocationPermission() {
                FragmentActivity activity = getActivity();
                if (activity instanceof MainActivity)
                    ((MainActivity) activity).requestLocationPermission();
            }
        };
        return rootView;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (entriesAdapter != null)
            entriesAdapter.detachPref();
    }

    @Override
    public void onStart() {
        super.onStart();
        connectionManager.addINDIListener(this);
        // Enumerate existing properties
        if (connectionManager.isConnected()) {
            List<INDIDevice> list = connectionManager.getIndiConnection().getDevicesAsList();
            if (list != null) {
                for (INDIDevice device : list) {
                    device.addINDIDeviceListener(this);
                    for (INDIProperty<?> property : device.getPropertiesAsList()) {
                        newProperty(device, property);
                    }
                }
            }
        } else {
            clearVars();
        }
        if (catalog.isReady()) {
            setListShown(true);
        } else {
            setListShown(false);
            catalog.setListener(this);
            // List loading
            if (!catalog.isLoading()) new Thread(catalog::load).start();
        }
        locationHelper.start();
        handler.postDelayed(this::maybeStartIntentSearch, 300);
    }

    @Override
    public void onStop() {
        super.onStop();
        catalog.setListener(null);
        connectionManager.removeINDIListener(this);
        locationHelper.start();
        list.stopNestedScroll();
        list.stopScroll();
    }

    @Override
    public boolean isActionEnabled() {
        return catalog.isReady() && (!searching);
    }

    @Override
    public int getActionDrawable() {
        return R.drawable.filter;
    }

    @Override
    public void run() {
        final boolean[] choices = {entriesAdapter.isShowStars(),
                entriesAdapter.isShowDso(),
                entriesAdapter.planetsShown(),
                entriesAdapter.isOnlyAboveHorizon()};
        new AlertDialog.Builder(context).setTitle(R.string.database_filter)
                .setIcon(R.drawable.filter)
                .setMultiChoiceItems(R.array.database_filter_elements, choices,
                        (dialog, which, isChecked) -> choices[which] = isChecked)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    entriesAdapter.starsShown(choices[0]);
                    entriesAdapter.dsoShown(choices[1]);
                    entriesAdapter.setShowPlanets(choices[2]);
                    entriesAdapter.setOnlyAboveHorizon(choices[3]);
                    if (entriesAdapter.isEmpty()) {
                        emptyLabel.setVisibility(View.VISIBLE);
                        list.setVisibility(View.GONE);
                    } else {
                        list.setVisibility(View.VISIBLE);
                        emptyLabel.setVisibility(View.GONE);
                    }
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void clearVars() {
        telescopeCoordP = null;
        telescopeCoordRA = null;
        telescopeCoordDec = null;
        telescopeOnCoordSetP = null;
        telescopeOnCoordSetSlew = null;
        telescopeOnCoordSetTrack = null;
        telescopeOnCoordSetSync = null;
    }

    private void setListShown(boolean b) {
        list.setVisibility(b ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(b ? View.GONE : View.VISIBLE);
        notifyActionChange();
    }

    private void maybeStartIntentSearch() {
        if (catalog.isReady() && (requestedSearch != null) && (searchView != null) && isVisible()) {
            searchView.setIconified(false);
            searchView.setQuery(requestedSearch, false);
            searchView.clearFocus();
            searching = true;
            notifyActionChange();
        }
    }

    /**
     * Called when the user changes the search string.
     *
     * @param newText the new query.
     * @return {@code false}, because the action is being handled by this listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        if (catalog.isReady()) entriesAdapter.filter(newText.trim().toLowerCase());
        if (requestedSearch != null) {
            if (entriesAdapter.visibleItemsCount() == 1) onListItemClick0(0);
            requestedSearch = null;
        }
        return false;
    }

    /**
     * Called when the user submits the query. Nothing to do since it is done in {@link #onQueryTextChange(String)}
     *
     * @param query the new query. Ignored.
     * @return always {@code false}.
     */
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public void onCatalogItemClick(View v) {
        int position = list.indexOfChild(v);
        if (position != -1) onListItemClick0(position);
    }

    private void onListItemClick0(int position) {
        final CatalogEntry entry = entriesAdapter.getEntryAt(position);
        final EquatorialCoordinates coordinates = entry.getCoordinates();
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setMessage(entry.createDescription(context, location))
                .setTitle(entry.getName());
        // Only display buttons if the telescope is ready
        if ((telescopeCoordP != null) && (telescopeOnCoordSetP != null)) {
            builder.setPositiveButton(R.string.go_to, (dialog, which) -> {
                try {
                    telescopeOnCoordSetTrack.setDesiredValue(Constants.SwitchStatus.ON);
                    telescopeOnCoordSetSlew.setDesiredValue(Constants.SwitchStatus.OFF);
                    telescopeOnCoordSetSync.setDesiredValue(Constants.SwitchStatus.OFF);
                    new PropUpdater(telescopeOnCoordSetP).start();
                    setCoordinatesMaybePrecess(entry, coordinates);
                    new PropUpdater(telescopeCoordP).start();
                    requestActionSnack(R.string.slew_ok);
                } catch (Exception e) {
                    requestActionSnack(R.string.sync_slew_error);
                }
                hideKeyboard();
            });
            builder.setNeutralButton(R.string.sync, (dialog, which) -> {
                try {
                    telescopeOnCoordSetSync.setDesiredValue(Constants.SwitchStatus.ON);
                    telescopeOnCoordSetTrack.setDesiredValue(Constants.SwitchStatus.OFF);
                    telescopeOnCoordSetSlew.setDesiredValue(Constants.SwitchStatus.OFF);
                    new PropUpdater(telescopeOnCoordSetP).start();
                    setCoordinatesMaybePrecess(entry, coordinates);
                    new PropUpdater(telescopeCoordP).start();
                    requestActionSnack(R.string.sync_ok);
                } catch (Exception e) {
                    requestActionSnack(R.string.sync_slew_error);
                }
                hideKeyboard();
            });
        }
        builder.setNegativeButton(android.R.string.cancel, null)
                .setIcon(entry.getIconResource()).show();
    }

    private void setCoordinatesMaybePrecess(CatalogEntry selectedEntry, EquatorialCoordinates coordinates) throws INDIValueException {
        if ((preferences.getBoolean(ApplicationConstants.COMPENSATE_PRECESSION_PREF, true)) &&
                ((selectedEntry instanceof StarEntry) || (selectedEntry instanceof DSOEntry))) {
            EquatorialCoordinates precessed = StarsPrecession.precess(Calendar.getInstance(), coordinates);
            telescopeCoordRA.setDesiredValue(precessed.getRAString());
            telescopeCoordDec.setDesiredValue(precessed.getDecString());
        } else {
            telescopeCoordRA.setDesiredValue(coordinates.getRAString());
            telescopeCoordDec.setDesiredValue(coordinates.getDecString());
        }
    }

    private void hideKeyboard() {
        ((InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(searchMenu.getActionView().getWindowToken(), 0);
    }

    // ------ Listener functions from INDI ------

    @Override
    public void newProperty(INDIDevice device, INDIProperty<?> property) {
        String name = property.getName();
        Log.i("SearchFragment", "New Property (" + name + ") added to device " + device.getName()
                + ", elements: " + Arrays.toString(property.getElementNames()));
        switch (name) {
            case "EQUATORIAL_EOD_COORD":
                if (((telescopeCoordDec = (INDINumberElement) property.getElement("DEC")) != null) &&
                        ((telescopeCoordRA = (INDINumberElement) property.getElement("RA")) != null) &&
                        (property instanceof INDINumberProperty)) {
                    property.addINDIPropertyListener(this);
                    telescopeCoordP = (INDINumberProperty) property;
                }
                break;
            case "ON_COORD_SET":
                if (((telescopeOnCoordSetTrack = (INDISwitchElement) property.getElement("TRACK")) != null) &&
                        ((telescopeOnCoordSetSlew = (INDISwitchElement) property.getElement("SLEW")) != null) &&
                        ((telescopeOnCoordSetSync = (INDISwitchElement) property.getElement("SYNC")) != null)) {
                    property.addINDIPropertyListener(this);
                    telescopeOnCoordSetP = (INDISwitchProperty) property;
                }
                break;
        }
    }

    @Override
    public void removeProperty(INDIDevice device, INDIProperty<?> property) {
        String name = property.getName();
        Log.d("SearchFragment", "Removed property (" + name + ") to device " + device.getName());
        switch (name) {
            case "EQUATORIAL_EOD_COORD":
                telescopeOnCoordSetP = null;
                telescopeOnCoordSetSlew = null;
                telescopeOnCoordSetTrack = null;
                telescopeOnCoordSetSync = null;
                break;
            case "ON_COORD_SET":
                telescopeCoordP = null;
                telescopeCoordRA = null;
                telescopeCoordDec = null;
                break;
        }
    }

    @Override
    public void messageChanged(INDIDevice device) {

    }

    @Override
    public void propertyChanged(INDIProperty<?> property) {

    }

    @Override
    public void newDevice(INDIServerConnection connection, INDIDevice device) {
        Log.i("SearchFragment", "New device: " + device.getName());
        device.addINDIDeviceListener(this);
    }

    @Override
    public void removeDevice(INDIServerConnection connection, INDIDevice device) {
        Log.d("SearchFragment", "Device removed: " + device.getName());
        device.removeINDIDeviceListener(this);
    }

    @Override
    public void connectionLost(INDIServerConnection connection) {
        clearVars();
    }

    @Override
    public void newMessage(INDIServerConnection connection, Date timestamp, String message) {

    }

    @Override
    public void onLoaded(boolean success) {
        handler.post(() -> {
            if (success) {
                if (searchMenu != null) searchMenu.setVisible(true);
                entriesAdapter.reloadCatalog();
                setListShown(true);
                if (isResumed()) maybeStartIntentSearch();
            } else if (isVisible()) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.catalog_manager)
                        .setMessage(R.string.catalog_error)
                        .setPositiveButton(android.R.string.ok, null)
                        .setIcon(R.drawable.warning)
                        .show();
            }
        });
    }
}