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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import org.indilib.i4j.Constants;
import org.indilib.i4j.client.INDIDevice;
import org.indilib.i4j.client.INDIDeviceListener;
import org.indilib.i4j.client.INDIProperty;
import org.indilib.i4j.client.INDIPropertyListener;
import org.indilib.i4j.client.INDIServerConnection;
import org.indilib.i4j.client.INDIServerConnectionListener;
import org.indilib.i4j.client.INDISwitchElement;
import org.indilib.i4j.client.INDISwitchProperty;
import org.indilib.i4j.client.INDIValueException;
import org.indilib.i4j.properties.INDIStandardElement;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.prop.PropUpdater;
import io.github.marcocipriani01.telescopetouch.views.ImprovedSpinnerListener;

import static io.github.marcocipriani01.telescopetouch.TelescopeTouchApp.connectionManager;

/**
 * This fragment shows directional buttons to move a telescope. It also provides
 * buttons to change speed. To activate the buttons, the driver must provide the
 * following properties:
 * {@code TELESCOPE_MOTION_NS}, {@code TELESCOPE_MOTION_WE}, {@code TELESCOPE_ABORT_MOTION}, {@code TELESCOPE_MOTION_RATE}
 *
 * @author Romain Fafet
 */
public class MountControlFragment extends Fragment implements INDIServerConnectionListener, INDIPropertyListener,
        INDIDeviceListener, OnTouchListener, OnClickListener, CompoundButton.OnCheckedChangeListener {

    private Context context;
    // Properties and elements associated to the buttons
    private INDISwitchProperty telescopeMotionNSP = null;
    private INDISwitchElement telescopeMotionNE = null;
    private INDISwitchElement telescopeMotionSE = null;
    private INDISwitchProperty telescopeMotionWEP = null;
    private INDISwitchElement telescopeMotionWE = null;
    private INDISwitchElement telescopeMotionEE = null;
    private INDISwitchProperty telescopeMotionAbort = null;
    private INDISwitchElement telescopeMotionAbortE = null;
    private INDISwitchProperty telescopeSlewRateP = null;
    private INDISwitchProperty telescopeParkP = null;
    private INDISwitchElement telescopeParkE = null;
    private INDISwitchElement telescopeUnParkE = null;
    // Views
    private ToggleButton btnPark = null;
    private Button btnMoveN = null;
    private Button btnMoveS = null;
    private Button btnMoveE = null;
    private Button btnMoveW = null;
    private Button btnMoveNE = null;
    private Button btnMoveNW = null;
    private Button btnMoveSE = null;
    private Button btnMoveSW = null;
    private Button btnStop = null;
    private Spinner slewRateSpinner = null;
    private final ImprovedSpinnerListener spinnerListener = new ImprovedSpinnerListener() {
        @Override
        protected void onImprovedItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            try {
                String selected = ((String) slewRateSpinner.getAdapter().getItem(pos));
                if ((selected != null) && (!selected.equals(getString(R.string.unavailable)))) {
                    for (INDISwitchElement element : telescopeSlewRateP.getElementsAsList()) {
                        String label = element.getLabel();
                        if (label.equals(selected)) {
                            element.setDesiredValue(Constants.SwitchStatus.ON);
                        } else {
                            element.setDesiredValue(Constants.SwitchStatus.OFF);
                        }
                    }
                    new PropUpdater(telescopeSlewRateP).start();
                }
            } catch (Exception e) {
                Log.e("MotionFragment", "Slew rate error!", e);
            }
        }
    };
    private TextView mountName = null;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_mount, container, false);
        // Set up the UI
        btnPark = rootView.findViewById(R.id.mount_parked_toggle);
        btnMoveN = rootView.findViewById(R.id.buttonN);
        btnMoveNE = rootView.findViewById(R.id.buttonNE);
        btnMoveE = rootView.findViewById(R.id.buttonE);
        btnMoveSE = rootView.findViewById(R.id.buttonSE);
        btnMoveS = rootView.findViewById(R.id.buttonS);
        btnMoveSW = rootView.findViewById(R.id.buttonSW);
        btnMoveW = rootView.findViewById(R.id.buttonW);
        btnMoveNW = rootView.findViewById(R.id.buttonNW);
        btnStop = rootView.findViewById(R.id.buttonStop);
        slewRateSpinner = rootView.findViewById(R.id.mount_slew_rate);
        mountName = rootView.findViewById(R.id.mount_name);
        btnPark.setOnCheckedChangeListener(this);
        btnMoveN.setOnTouchListener(this);
        btnMoveNE.setOnTouchListener(this);
        btnMoveE.setOnTouchListener(this);
        btnMoveSE.setOnTouchListener(this);
        btnMoveS.setOnTouchListener(this);
        btnMoveSW.setOnTouchListener(this);
        btnMoveW.setOnTouchListener(this);
        btnMoveNW.setOnTouchListener(this);
        btnStop.setOnClickListener(this);
        spinnerListener.attach(slewRateSpinner);
        return rootView;
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
                    List<INDIProperty<?>> properties = device.getPropertiesAsList();
                    for (INDIProperty<?> property : properties) {
                        newProperty0(device, property);
                    }
                }
            }
        } else {
            clearVars();
        }
        // Update UI
        enableUi();
        initSlewRate();
    }

    @Override
    public void onStop() {
        super.onStop();
        connectionManager.removeINDIListener(this);
    }

    private void clearVars() {
        telescopeMotionNSP = null;
        telescopeMotionNE = null;
        telescopeMotionSE = null;
        telescopeMotionWEP = null;
        telescopeMotionWE = null;
        telescopeMotionEE = null;
        telescopeMotionAbort = null;
        telescopeMotionAbortE = null;
        telescopeSlewRateP = null;
        telescopeParkP = null;
        telescopeParkE = null;
        telescopeUnParkE = null;
    }

    private void initSlewRate() {
        if (slewRateSpinner != null) {
            slewRateSpinner.post(() -> {
                ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
                arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                int selectedItem = 0;
                if (telescopeSlewRateP == null) {
                    arrayAdapter.add(getString(R.string.unavailable));
                } else {
                    List<INDISwitchElement> elements = telescopeSlewRateP.getElementsAsList();
                    if (elements.isEmpty()) {
                        arrayAdapter.add(getString(R.string.unavailable));
                    } else {
                        for (int i = 0, elementsSize = elements.size(); i < elementsSize; i++) {
                            INDISwitchElement element = elements.get(i);
                            arrayAdapter.add(element.getLabel());
                            if (element.getValue() == Constants.SwitchStatus.ON) selectedItem = i;
                        }
                    }
                }
                slewRateSpinner.setAdapter(arrayAdapter);
                slewRateSpinner.setSelection(selectedItem);
            });
        }
    }

    /**
     * Enables the buttons if the corresponding property was found
     */
    public void enableUi() {
        if (btnPark != null) btnPark.post(() -> btnPark.setEnabled(telescopeParkP != null));
        if (btnMoveE != null) btnMoveE.post(() -> btnMoveE.setEnabled(telescopeMotionWEP != null));
        if (btnMoveW != null) btnMoveW.post(() -> btnMoveW.setEnabled(telescopeMotionWEP != null));
        if (btnMoveN != null) btnMoveN.post(() -> btnMoveN.setEnabled(telescopeMotionNSP != null));
        if (btnMoveS != null) btnMoveS.post(() -> btnMoveS.setEnabled(telescopeMotionNSP != null));
        if (btnMoveNE != null)
            btnMoveNE.post(() -> btnMoveNE.setEnabled((telescopeMotionWEP != null) && (telescopeMotionNSP != null)));
        if (btnMoveNW != null)
            btnMoveNW.post(() -> btnMoveNW.setEnabled((telescopeMotionWEP != null) && (telescopeMotionNSP != null)));
        if (btnMoveSE != null)
            btnMoveSE.post(() -> btnMoveSE.setEnabled((telescopeMotionWEP != null) && (telescopeMotionNSP != null)));
        if (btnMoveSW != null)
            btnMoveSW.post(() -> btnMoveSW.setEnabled((telescopeMotionWEP != null) && (telescopeMotionNSP != null)));
        if (btnStop != null)
            btnStop.post(() -> btnStop.setEnabled((telescopeMotionWEP != null) || (telescopeMotionNSP != null)
                    || (telescopeMotionAbort != null)));
        if (slewRateSpinner != null)
            slewRateSpinner.post(() -> slewRateSpinner.setEnabled(telescopeSlewRateP != null));
    }

    /**
     * Called when a directional button is pressed or released. Send the
     * corresponding order to the driver.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        final Constants.SwitchStatus status, offStatus = Constants.SwitchStatus.OFF;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            status = Constants.SwitchStatus.ON;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            status = Constants.SwitchStatus.OFF;
        } else {
            return true;
        }
        int id = v.getId();
        try {
            if (id == R.id.buttonE) {
                telescopeMotionEE.setDesiredValue(status);
                telescopeMotionWE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionWEP).start();
                return true;
            } else if (id == R.id.buttonW) {
                telescopeMotionWE.setDesiredValue(status);
                telescopeMotionEE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionWEP).start();
            } else if (id == R.id.buttonN) {
                telescopeMotionNE.setDesiredValue(status);
                telescopeMotionSE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionNSP).start();
            } else if (id == R.id.buttonS) {
                telescopeMotionSE.setDesiredValue(status);
                telescopeMotionNE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionNSP).start();
            } else if (id == R.id.buttonNE) {
                telescopeMotionEE.setDesiredValue(status);
                telescopeMotionWE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionWEP).start();
                telescopeMotionNE.setDesiredValue(status);
                telescopeMotionSE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionNSP).start();
            } else if (id == R.id.buttonNW) {
                telescopeMotionWE.setDesiredValue(status);
                telescopeMotionEE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionWEP).start();
                telescopeMotionNE.setDesiredValue(status);
                telescopeMotionSE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionNSP).start();
            } else if (id == R.id.buttonSE) {
                telescopeMotionEE.setDesiredValue(status);
                telescopeMotionWE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionWEP).start();
                telescopeMotionSE.setDesiredValue(status);
                telescopeMotionNE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionNSP).start();
            } else if (id == R.id.buttonSW) {
                telescopeMotionWE.setDesiredValue(status);
                telescopeMotionEE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionWEP).start();
                telescopeMotionSE.setDesiredValue(status);
                telescopeMotionNE.setDesiredValue(offStatus);
                new PropUpdater(telescopeMotionNSP).start();
            } else {
                return false;
            }
        } catch (INDIValueException e) {
            Log.e("MotionFragment", e.getLocalizedMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * Called when one of the stop, speed up and speed down buttons is clicked.
     * Sends the corresponding order to the driver.
     */
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.buttonStop) {
            try {
                if (telescopeMotionWEP != null) {
                    telescopeMotionWE.setDesiredValue(Constants.SwitchStatus.OFF);
                    telescopeMotionEE.setDesiredValue(Constants.SwitchStatus.OFF);
                    new PropUpdater(telescopeMotionWEP).start();
                }
                if (telescopeMotionNSP != null) {
                    telescopeMotionSE.setDesiredValue(Constants.SwitchStatus.OFF);
                    telescopeMotionNE.setDesiredValue(Constants.SwitchStatus.OFF);
                    new PropUpdater(telescopeMotionNSP).start();
                }
                if (telescopeMotionAbort != null) {
                    telescopeMotionAbortE.setDesiredValue(Constants.SwitchStatus.ON);
                    new PropUpdater(telescopeMotionAbort).start();
                }
            } catch (INDIValueException e) {
                Log.e("MotionFragment", e.getLocalizedMessage(), e);
            }
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        new AlertDialog.Builder(context)
                .setTitle(R.string.telescope_parking)
                .setMessage(R.string.telescope_parking_confirm)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    try {
                        if (isChecked) {
                            telescopeParkE.setDesiredValue(Constants.SwitchStatus.ON);
                            telescopeUnParkE.setDesiredValue(Constants.SwitchStatus.OFF);
                        } else {
                            telescopeUnParkE.setDesiredValue(Constants.SwitchStatus.ON);
                            telescopeParkE.setDesiredValue(Constants.SwitchStatus.OFF);
                        }
                        new PropUpdater(telescopeParkP).start();
                    } catch (INDIValueException e) {
                        Log.e("MotionFragment", e.getLocalizedMessage(), e);
                    }
                })
                .setNegativeButton(android.R.string.no, null)
                .setOnDismissListener(dialog -> {
                    btnPark.setOnCheckedChangeListener(null);
                    boolean b = telescopeParkE.getValue() == Constants.SwitchStatus.ON;
                    btnPark.setChecked(b);
                    btnPark.setSelected(b);
                    btnPark.setOnCheckedChangeListener(this);
                })
                .setIcon(R.drawable.warning)
                .show();
    }

    // ------ Listener functions from INDI ------

    @Override
    public void connectionLost(INDIServerConnection arg0) {
        clearVars();
        enableUi();
        initSlewRate();
    }

    @Override
    public void newDevice(INDIServerConnection connection, INDIDevice device) {
        Log.i("MotionFragment", "New device: " + device.getName());
        device.addINDIDeviceListener(this);
    }

    @Override
    public void removeDevice(INDIServerConnection connection, INDIDevice device) {
        Log.i("MotionFragment", "Device removed: " + device.getName());
        device.removeINDIDeviceListener(this);
    }

    @Override
    public void newMessage(INDIServerConnection connection, Date timestamp, String message) {

    }

    @Override
    public void newProperty(INDIDevice device, INDIProperty<?> property) {
        newProperty0(device, property);
        enableUi();
    }

    private void newProperty0(INDIDevice device, INDIProperty<?> property) {
        String name = property.getName(), devName = device.getName();
        Log.i("MotionFragment", "New Property (" + name + ") added to device " + devName
                + ", elements: " + Arrays.toString(property.getElementNames()));
        switch (name) {
            case "TELESCOPE_MOTION_NS": {
                if (((telescopeMotionNE = (INDISwitchElement) property.getElement(INDIStandardElement.MOTION_NORTH)) != null)
                        && ((telescopeMotionSE = (INDISwitchElement) property.getElement(INDIStandardElement.MOTION_SOUTH)) != null)) {
                    telescopeMotionNSP = (INDISwitchProperty) property;
                    property.addINDIPropertyListener(this);
                    mountName.post(() -> mountName.setText(devName));
                }
                break;
            }
            case "TELESCOPE_MOTION_WE": {
                if (((telescopeMotionEE = (INDISwitchElement) property.getElement(INDIStandardElement.MOTION_EAST)) != null)
                        && ((telescopeMotionWE = (INDISwitchElement) property.getElement(INDIStandardElement.MOTION_WEST)) != null)) {
                    telescopeMotionWEP = (INDISwitchProperty) property;
                    property.addINDIPropertyListener(this);
                }
                break;
            }
            case "TELESCOPE_ABORT_MOTION": {
                if ((telescopeMotionAbortE = (INDISwitchElement) property.getElement(INDIStandardElement.ABORT_MOTION)) != null) {
                    telescopeMotionAbort = (INDISwitchProperty) property;
                    property.addINDIPropertyListener(this);
                }
                break;
            }
            case "TELESCOPE_SLEW_RATE": {
                telescopeSlewRateP = (INDISwitchProperty) property;
                property.addINDIPropertyListener(this);
                initSlewRate();
                break;
            }
            case "TELESCOPE_PARK": {
                if (((telescopeParkE = (INDISwitchElement) property.getElement(INDIStandardElement.PARK)) != null)
                        && ((telescopeUnParkE = (INDISwitchElement) property.getElement(INDIStandardElement.UNPARK)) != null)) {
                    telescopeParkP = (INDISwitchProperty) property;
                    property.addINDIPropertyListener(this);
                }
                break;
            }
        }
    }

    @Override
    public void removeProperty(INDIDevice device, INDIProperty<?> property) {
        String name = property.getName();
        Log.d("MotionFragment", "Removed property (" + name + ") to device " + device.getName());
        switch (name) {
            case "TELESCOPE_MOTION_NS": {
                telescopeMotionNSP = null;
                telescopeMotionNE = null;
                telescopeMotionSE = null;
                mountName.post(() -> mountName.setText(R.string.mount_control));
                break;
            }
            case "TELESCOPE_MOTION_WE": {
                telescopeMotionWEP = null;
                telescopeMotionWE = null;
                telescopeMotionEE = null;
                break;
            }
            case "TELESCOPE_ABORT_MOTION": {
                telescopeMotionAbort = null;
                telescopeMotionAbortE = null;
                break;
            }
            case "TELESCOPE_SLEW_RATE": {
                telescopeSlewRateP = null;
                initSlewRate();
                break;
            }
            case "TELESCOPE_PARK": {
                telescopeParkP = null;
                telescopeParkE = null;
                telescopeUnParkE = null;
                break;
            }
            default: {
                return;
            }
        }
        enableUi();
    }

    @Override
    public void propertyChanged(final INDIProperty<?> property) {
        switch (property.getName()) {
            case "TELESCOPE_MOTION_NS": {
                if (btnMoveN != null)
                    btnMoveN.post(() -> btnMoveN.setPressed(telescopeMotionNE.getValue() == Constants.SwitchStatus.ON));
                if (btnMoveS != null)
                    btnMoveS.post(() -> btnMoveS.setPressed(telescopeMotionSE.getValue() == Constants.SwitchStatus.ON));
                break;
            }
            case "TELESCOPE_MOTION_WE": {
                if (btnMoveE != null)
                    btnMoveE.post(() -> btnMoveE.setPressed(telescopeMotionEE.getValue() == Constants.SwitchStatus.ON));
                if (btnMoveW != null)
                    btnMoveW.post(() -> btnMoveW.setPressed(telescopeMotionWE.getValue() == Constants.SwitchStatus.ON));
                break;
            }
            case "TELESCOPE_SLEW_RATE": {
                if (slewRateSpinner != null) {
                    slewRateSpinner.post(() -> {
                        String selected = null;
                        for (INDISwitchElement element : telescopeSlewRateP) {
                            if (element.getValue() == Constants.SwitchStatus.ON)
                                selected = element.getLabel();
                        }
                        if (selected != null) {
                            SpinnerAdapter adapter = slewRateSpinner.getAdapter();
                            int i;
                            for (i = 0; i < adapter.getCount(); i++) {
                                if (adapter.getItem(i).equals(selected)) break;
                            }
                            slewRateSpinner.setSelection(i);
                        }
                    });
                }
                break;
            }
            case "TELESCOPE_PARK": {
                if (btnPark != null) {
                    btnPark.post(() -> {
                        btnPark.setOnCheckedChangeListener(null);
                        boolean b = telescopeParkE.getValue() == Constants.SwitchStatus.ON;
                        btnPark.setChecked(b);
                        btnPark.setSelected(b);
                        btnPark.setOnCheckedChangeListener(this);
                    });
                }
                break;
            }
        }
    }

    @Override
    public void messageChanged(INDIDevice device) {

    }
}