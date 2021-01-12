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

package io.github.marcocipriani01.telescopetouch.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.ListFragment;

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

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;
import io.github.marcocipriani01.telescopetouch.catalog.Catalog;
import io.github.marcocipriani01.telescopetouch.catalog.CatalogCoordinates;
import io.github.marcocipriani01.telescopetouch.catalog.CatalogEntry;
import io.github.marcocipriani01.telescopetouch.catalog.DSOEntry;
import io.github.marcocipriani01.telescopetouch.catalog.StarEntry;
import io.github.marcocipriani01.telescopetouch.prop.PropUpdater;
import io.github.marcocipriani01.telescopetouch.util.AstroTimeUtils;
import io.github.marcocipriani01.telescopetouch.views.CatalogArrayAdapter;

/**
 * Allows the user to look for an astronomical object and slew the telescope.
 */
public class GoToFragment extends ListFragment
        implements SearchView.OnQueryTextListener, Catalog.CatalogLoadingListener,
        INDIServerConnectionListener, INDIPropertyListener, INDIDeviceListener {

    private static final Catalog catalog = new Catalog();
    private static CatalogArrayAdapter entriesAdapter;
    private ConnectionManager connectionManager;
    private Context context;
    private MenuItem searchMenu;
    // INDI properties
    private INDINumberProperty telescopeCoordP = null;
    private INDINumberElement telescopeCoordRA = null;
    private INDINumberElement telescopeCoordDE = null;
    private INDISwitchProperty telescopeOnCoordSetP = null;
    private INDISwitchElement telescopeOnCoordSetSync = null;
    private INDISwitchElement telescopeOnCoordSetSlew = null;
    private INDISwitchElement telescopeOnCoordSetTrack = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.empty_catalog));
        setHasOptionsMenu(true);
        setListAdapter(entriesAdapter = new CatalogArrayAdapter(context, catalog));
        if (catalog.isReady()) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (searchMenu != null) searchMenu.setVisible(true);
            }, 50);
        } else {
            // List loading
            setListShown(false);
            catalog.setListener(this);
            if (!catalog.isLoading()) new Thread(catalog::load).start();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Set up INDI connection
        connectionManager = TelescopeTouchApp.getConnectionManager();
        connectionManager.addListener(this);
        // Enumerate existing properties
        if (connectionManager.isConnected()) {
            List<INDIDevice> list = connectionManager.getConnection().getDevicesAsList();
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
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.database_filter, menu);
        searchMenu = menu.add(R.string.mount_goto);
        searchMenu.setIcon(R.drawable.search);
        searchMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        searchMenu.setVisible(false);
        SearchView searchView = new SearchView(context);
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(this);
        searchMenu.setActionView(searchView);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.isCheckable()) {
            int itemId = item.getItemId();
            boolean checked = !item.isChecked();
            if (itemId == R.id.menu_filter_stars) {
                entriesAdapter.setShowStars(checked);
            } else if (itemId == R.id.menu_filter_dso) {
                entriesAdapter.setShowDso(checked);
            } else if (itemId == R.id.menu_filter_planets) {
                entriesAdapter.setShowPlanets(checked);
            } else {
                return super.onOptionsItemSelected(item);
            }
            item.setChecked(checked);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        catalog.setListener(null);
        connectionManager.removeListener(this);
    }

    private void clearVars() {
        telescopeCoordP = null;
        telescopeCoordRA = null;
        telescopeCoordDE = null;
        telescopeOnCoordSetP = null;
        telescopeOnCoordSetSlew = null;
        telescopeOnCoordSetTrack = null;
        telescopeOnCoordSetSync = null;
    }

    /**
     * Called when the user changes the search string.
     *
     * @param newText the new query.
     * @return {@code false}, because the action is being handled by this listener.
     */
    @Override
    public boolean onQueryTextChange(String newText) {
        if (catalog.isReady()) entriesAdapter.filter(newText);
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
    public void onListItemClick(@NonNull ListView l, @NonNull View v, int position, long id) {
        final CatalogEntry selectedEntry = entriesAdapter.getItem(position);
        final CatalogCoordinates coord = selectedEntry.getCoordinates();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(selectedEntry.createDescription(context)).setTitle(selectedEntry.getName());
        // Only display buttons if the telescope is ready
        if ((telescopeCoordP != null) && (telescopeOnCoordSetP != null)) {
            builder.setPositiveButton(R.string.go_to, (dialog, which) -> {
                try {
                    telescopeOnCoordSetTrack.setDesiredValue(Constants.SwitchStatus.ON);
                    telescopeOnCoordSetSlew.setDesiredValue(Constants.SwitchStatus.OFF);
                    telescopeOnCoordSetSync.setDesiredValue(Constants.SwitchStatus.OFF);
                    new PropUpdater(telescopeOnCoordSetP).start();
                    if ((selectedEntry instanceof StarEntry) || (selectedEntry instanceof DSOEntry)) {
                        CatalogCoordinates precessed = AstroTimeUtils.precess(Calendar.getInstance(), coord);
                        telescopeCoordRA.setDesiredValue(precessed.getRaStr());
                        telescopeCoordDE.setDesiredValue(precessed.getDeStr());
                    } else {
                        telescopeCoordRA.setDesiredValue(coord.getRaStr());
                        telescopeCoordDE.setDesiredValue(coord.getDeStr());
                    }
                    new PropUpdater(telescopeCoordP).start();
                    Toast.makeText(context, context.getString(R.string.slew_ok), Toast.LENGTH_LONG).show();
                    requireActivity().finish();
                } catch (Exception e) {
                    Toast.makeText(context, context.getString(R.string.sync_slew_error), Toast.LENGTH_LONG).show();
                }
            });
            builder.setNeutralButton(R.string.sync, (dialog, which) -> {
                try {
                    telescopeOnCoordSetSync.setDesiredValue(Constants.SwitchStatus.ON);
                    telescopeOnCoordSetTrack.setDesiredValue(Constants.SwitchStatus.OFF);
                    telescopeOnCoordSetSlew.setDesiredValue(Constants.SwitchStatus.OFF);
                    new PropUpdater(telescopeOnCoordSetP).start();
                    CatalogCoordinates precessed = AstroTimeUtils.precess(Calendar.getInstance(), coord);
                    telescopeCoordRA.setDesiredValue(precessed.getRaStr());
                    telescopeCoordDE.setDesiredValue(precessed.getDeStr());
                    new PropUpdater(telescopeCoordP).start();
                    Toast.makeText(context, context.getString(R.string.sync_ok), Toast.LENGTH_LONG).show();
                    requireActivity().finish();
                } catch (Exception e) {
                    Toast.makeText(context, context.getString(R.string.sync_slew_error), Toast.LENGTH_LONG).show();
                }
            });
        }
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    // ------ Listener functions from INDI ------

    @Override
    public void newProperty(INDIDevice device, INDIProperty<?> property) {
        String name = property.getName();
        Log.i("SearchFragment", "New Property (" + name + ") added to device " + device.getName()
                + ", elements: " + Arrays.toString(property.getElementNames()));
        switch (name) {
            case "EQUATORIAL_EOD_COORD":
                if (((telescopeCoordDE = (INDINumberElement) property.getElement("DEC")) != null) &&
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
                telescopeCoordDE = null;
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
        // Move to the connection tab
        TelescopeTouchApp.goToConnectionTab();
    }

    @Override
    public void newMessage(INDIServerConnection connection, Date timestamp, String message) {

    }

    @Override
    public void onLoaded(boolean success) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (success) {
                searchMenu.setVisible(true);
                entriesAdapter.reloadCatalog();
                if (isResumed()) {
                    setListShown(true);
                } else {
                    setListShownNoAnimation(true);
                }
            } else if (isVisible()) {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.catalog_manager)
                        .setMessage(R.string.catalog_error)
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(R.drawable.warning)
                        .show();
            }
        });
    }
}