/*
 * Copyright 2021 Marco Cipriani (@marcocipriani01)
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

package io.github.marcocipriani01.telescopetouch.activities.fragments;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BlendMode;
import android.graphics.BlendModeColorFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.preference.PreferenceManager;

import com.google.android.material.slider.Slider;
import com.google.android.material.tabs.TabLayout;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

import io.github.marcocipriani01.livephotoview.PhotoView;
import io.github.marcocipriani01.telescopetouch.ApplicationConstants;
import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.activities.util.ImprovedSpinnerListener;
import io.github.marcocipriani01.telescopetouch.activities.views.SameSelectionSpinner;
import io.github.marcocipriani01.telescopetouch.phd2.PHD2Client;

import static io.github.marcocipriani01.telescopetouch.TelescopeTouchApp.nsdHelper;
import static io.github.marcocipriani01.telescopetouch.TelescopeTouchApp.phd2;
import static io.github.marcocipriani01.telescopetouch.activities.ServersActivity.getServers;

public class PHD2Fragment extends ActionFragment implements PHD2Client.PHD2Listener, Slider.OnChangeListener, View.OnClickListener, CompoundButton.OnCheckedChangeListener {

    private static int selectedSpinnerItem = 0;
    private final ImprovedSpinnerListener exposureSpinnerListener = new ImprovedSpinnerListener() {
        @Override
        protected void onImprovedItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            PHD2Client.PHD2Command.set_exposure.run(phd2, (int) phd2.exposureTimes[pos] * 1000);
        }
    };
    private SharedPreferences preferences;
    private GraphView graph;
    private Button connectionButton;
    private Spinner serversSpinner;
    private EditText portEditText;
    private TextView statusLabel;
    private TextView raCorrectionLabel, decCorrectionLabel;
    private TextView hdfLabel, snrLabel;
    private ImageButton loopBtn, findStarBtn, guideBtn, stopBtn;
    private SameSelectionSpinner exposureSpinner;
    private PhotoView liveView;
    private SwitchCompat receiveImagesSwitch, stretchImagesSwitch;

    @SuppressLint("SetTextI18n")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        View rootView = inflater.inflate(R.layout.fragment_phd2, container, false);
        Slider zoomSlider = rootView.findViewById(R.id.phd2_zoom_slider);
        zoomSlider.addOnChangeListener(this);
        exposureSpinner = rootView.findViewById(R.id.phd2_exposure_spinner);
        exposureSpinnerListener.attach(exposureSpinner);
        receiveImagesSwitch = rootView.findViewById(R.id.phd_receive_images_switch);
        stretchImagesSwitch = rootView.findViewById(R.id.phd_stretch_images_switch);
        liveView = rootView.findViewById(R.id.phd2_live_view);
        statusLabel = rootView.findViewById(R.id.phd2_state);
        raCorrectionLabel = rootView.findViewById(R.id.phd2_correction_ra);
        decCorrectionLabel = rootView.findViewById(R.id.phd2_correction_dec);
        hdfLabel = rootView.findViewById(R.id.phd2_star_mass);
        snrLabel = rootView.findViewById(R.id.phd2_snr);
        loopBtn = rootView.findViewById(R.id.phd_loop_btn);
        loopBtn.setOnClickListener(this);
        findStarBtn = rootView.findViewById(R.id.phd_auto_select_btn);
        findStarBtn.setOnClickListener(this);
        guideBtn = rootView.findViewById(R.id.phd_guide_btn);
        guideBtn.setOnClickListener(this);
        stopBtn = rootView.findViewById(R.id.phd_stop_btn);
        stopBtn.setOnClickListener(this);
        connectionButton = rootView.findViewById(R.id.phd2_connect_button);
        serversSpinner = rootView.findViewById(R.id.phd2_host_spinner);
        loadServers(getServers(preferences));
        portEditText = rootView.findViewById(R.id.phd2_port_field);
        portEditText.setText(String.valueOf(preferences.getInt(ApplicationConstants.PHD2_PORT_PREF, 4400)));

        connectionButton.setOnClickListener(v -> {
            ((InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(portEditText.getWindowToken(), 0);
            String host = (String) serversSpinner.getSelectedItem();
            if (host == null) {
                requestActionSnack(R.string.unknown_error);
                return;
            }
            try {
                if (phd2.isConnected()) {
                    phd2.disconnect();
                } else {
                    if (host.contains("@")) {
                        String[] split = host.split("@");
                        if (split.length == 2) host = split[1];
                    }
                    String portStr = portEditText.getText().toString();
                    int port;
                    if (portStr.equals("")) {
                        port = 4400;
                        portEditText.setText("4400");
                    } else {
                        try {
                            port = Integer.parseInt(portStr);
                        } catch (NumberFormatException e) {
                            requestActionSnack(R.string.invalid_port);
                            return;
                        }
                        if ((port <= 0) || (port >= 0xFFFF)) {
                            requestActionSnack(R.string.invalid_port);
                            return;
                        }
                    }
                    preferences.edit().putInt(ApplicationConstants.PHD2_PORT_PREF, port).apply();
                    connectionButton.setText(context.getString(R.string.connecting));
                    connectionButton.setEnabled(false);
                    serversSpinner.setEnabled(false);
                    portEditText.setEnabled(false);
                    phd2.connect(host, port);
                }
            } catch (Exception e) {
                requestActionSnack(context.getString(R.string.error) + " " + e.getLocalizedMessage());
            }
        });

        NestedScrollView graphTab = rootView.findViewById(R.id.phd2_graph_layout);
        View viewTab = rootView.findViewById(R.id.phd2_live_layout);
        rootView.<TabLayout>findViewById(R.id.phd2_tabs)
                .addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                    @Override
                    public void onTabSelected(TabLayout.Tab tab) {
                        if (tab.getPosition() == 0) {
                            graphTab.stopNestedScroll();
                            graphTab.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    graphTab.setVisibility(View.GONE);
                                }
                            });
                            viewTab.animate().alpha(1f).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    viewTab.setVisibility(View.VISIBLE);
                                    showActionbar();
                                }
                            });
                        } else {
                            graphTab.animate().alpha(1f).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationStart(Animator animation) {
                                    graphTab.setVisibility(View.VISIBLE);
                                }
                            });
                            viewTab.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    viewTab.setVisibility(View.GONE);
                                }
                            });
                        }
                    }

                    @Override
                    public void onTabUnselected(TabLayout.Tab tab) {

                    }

                    @Override
                    public void onTabReselected(TabLayout.Tab tab) {

                    }
                });

        graph = rootView.findViewById(R.id.phd_graph);
        graph.addSeries(phd2.guidingDataRA);
        graph.addSeries(phd2.guidingDataDec);
        GridLabelRenderer gridLabel = graph.getGridLabelRenderer();
        gridLabel.setLabelFormatter(new DefaultLabelFormatter());
        gridLabel.setNumHorizontalLabels(4);
        gridLabel.setNumVerticalLabels(6);
        gridLabel.setHorizontalLabelsAngle(45);
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMinY(-4);
        viewport.setMaxY(4);
        viewport.setScalable(true);
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        phd2.addListener(this);
        if (phd2.isConnected()) {
            onPHD2Connected();
            PHD2Client.AppState state = phd2.appState;
            Resources resources = context.getResources();
            if (state == null) {
                statusLabel.setText(context.getString(R.string.current_state_default));
            } else {
                statusLabel.setText(String.format(resources.getString(R.string.current_state), state.getDescription(resources)));
            }
        } else {
            onPHD2Disconnected();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        selectedSpinnerItem = serversSpinner.getSelectedItemPosition();
    }

    @Override
    public void onStop() {
        super.onStop();
        phd2.removeListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (graph != null) phd2.clearGraphReference(graph);
    }

    public void loadServers(ArrayList<String> servers) {
        if (nsdHelper.isAvailable()) {
            HashMap<String, String> services = nsdHelper.getDiscoveredServices();
            for (String name : services.keySet()) {
                String ip = services.get(name);
                if (ip != null) servers.add(name.replace("@", "") + "@" + ip);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, servers);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        serversSpinner.setAdapter(adapter);
        if (adapter.getCount() > selectedSpinnerItem)
            serversSpinner.setSelection(selectedSpinnerItem);
    }

    @Override
    public void onClick(View v) {
        try {
            if (v == loopBtn) {
                phd2.startLoop();
            } else if (v == findStarBtn) {
                phd2.findStar();
            } else if (v == guideBtn) {
                phd2.startGuiding();
            } else if (v == stopBtn) {
                phd2.stopCapture();
            }
        } catch (Exception e) {
            requestActionSnack(context.getString(R.string.error) + " " + e.getLocalizedMessage());
        }
    }

    @Override
    public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
        if (fromUser && (graph != null)) {
            Viewport viewport = graph.getViewport();
            viewport.setMinY(-value);
            viewport.setMaxY(value);
            graph.invalidate();
            graph.onDataChanged(false, false);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == receiveImagesSwitch) {
            phd2.receiveImages = isChecked;
        } else if (buttonView == stretchImagesSwitch) {
            phd2.stretchImages = isChecked;
        }
    }

    @Override
    public boolean isActionEnabled() {
        return false;
    }

    @Override
    public int getActionDrawable() {
        return 0;
    }

    @Override
    public void run() {

    }

    private void setButtonColor(ImageButton btn, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            btn.getDrawable().setColorFilter(new BlendModeColorFilter(color, BlendMode.SRC_ATOP));
        } else {
            btn.getDrawable().setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onPHD2ParamUpdate(PHD2Client.PHD2Param param) {
        switch (param) {
            case STATE:
                if (statusLabel != null) {
                    Resources resources = context.getResources();
                    statusLabel.setText(String.format(resources.getString(R.string.current_state), phd2.appState.getDescription(resources)));
                }
                switch (phd2.appState) {
                    case Stopped:
                        if (loopBtn != null) setButtonColor(loopBtn, Color.WHITE);
                        if (guideBtn != null) setButtonColor(guideBtn, Color.WHITE);
                        if (stopBtn != null)
                            setButtonColor(stopBtn, context.getResources().getColor(R.color.light_red));
                        break;
                    case Looping:
                        if (loopBtn != null)
                            setButtonColor(loopBtn, context.getResources().getColor(R.color.light_green));
                        if (guideBtn != null) setButtonColor(guideBtn, Color.WHITE);
                        if (stopBtn != null) setButtonColor(stopBtn, Color.WHITE);
                        break;
                    case Guiding:
                        if (loopBtn != null) setButtonColor(loopBtn, Color.WHITE);
                        if (guideBtn != null)
                            setButtonColor(guideBtn, context.getResources().getColor(R.color.light_green));
                        if (stopBtn != null) setButtonColor(stopBtn, Color.WHITE);
                        break;
                    case Calibrating:
                        if (loopBtn != null) setButtonColor(loopBtn, Color.WHITE);
                        if (guideBtn != null)
                            setButtonColor(guideBtn, context.getResources().getColor(R.color.light_yellow));
                        if (stopBtn != null) setButtonColor(stopBtn, Color.WHITE);
                        break;
                }
                break;
            case GUIDE_VALUES:
                int graphIndex = phd2.getGraphIndex();
                if ((graphIndex < 50) && (graph != null)) {
                    Viewport viewport = graph.getViewport();
                    viewport.setMinX(Math.max(0, graphIndex - 50));
                    viewport.setMaxX(graphIndex);
                    graph.onDataChanged(false, false);
                }
                if (raCorrectionLabel != null) {
                    if (phd2.raCorrection == 0) {
                        raCorrectionLabel.setVisibility(View.INVISIBLE);
                    } else {
                        raCorrectionLabel.setText(phd2.raCorrection + " ms");
                        raCorrectionLabel.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context,
                                phd2.raCorrectionSign ? R.drawable.arrow_forward : R.drawable.arrow_back), null, null, null);
                        raCorrectionLabel.setVisibility(View.VISIBLE);
                    }
                }
                if (decCorrectionLabel != null) {
                    if (phd2.decCorrection == 0) {
                        decCorrectionLabel.setVisibility(View.INVISIBLE);
                    } else {
                        decCorrectionLabel.setText(phd2.decCorrection + " ms");
                        decCorrectionLabel.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context,
                                phd2.decCorrectionSign ? R.drawable.arrow_upward : R.drawable.arrow_downward), null, null, null);
                        decCorrectionLabel.setVisibility(View.VISIBLE);
                    }
                }
                if (hdfLabel != null) hdfLabel.setText("HDF " + phd2.hdf);
                if (snrLabel != null) snrLabel.setText("SNR " + phd2.snr);
                break;
            case SETTLE_DONE:
                requestActionSnack(R.string.guide_settled);
                break;
            case EXPOSURE_TIMES:
                if (exposureSpinner != null) {
                    ArrayAdapter<Double> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
                    for (double exposureTime : phd2.exposureTimes) {
                        adapter.add(exposureTime);
                    }
                    exposureSpinner.setAdapter(adapter);
                }
                break;
            case EXPOSURE_TIME:
                if (exposureSpinner != null) {
                    double[] exposureTimes = phd2.exposureTimes;
                    for (int i = 0; i < exposureTimes.length; i++) {
                        if (exposureTimes[i] == phd2.exposureTime) {
                            exposureSpinner.setSelection(i);
                            break;
                        }
                    }
                }
                break;
            case IMAGE:
                if (liveView != null) liveView.setImageBitmap(phd2.bitmap);
                break;
        }
    }

    @Override
    public void onPHD2Connected() {
        if (connectionButton != null) {
            connectionButton.setEnabled(true);
            connectionButton.setText(context.getString(R.string.disconnect));
        }
        if (serversSpinner != null) serversSpinner.setEnabled(false);
        if (portEditText != null) portEditText.setEnabled(false);
        if (loopBtn != null) loopBtn.setEnabled(true);
        if (findStarBtn != null) findStarBtn.setEnabled(true);
        if (guideBtn != null) guideBtn.setEnabled(true);
        if (stopBtn != null) stopBtn.setEnabled(true);
        if (receiveImagesSwitch != null) {
            receiveImagesSwitch.setEnabled(true);
            receiveImagesSwitch.setOnCheckedChangeListener(null);
            receiveImagesSwitch.setChecked(phd2.receiveImages);
            receiveImagesSwitch.setSelected(phd2.receiveImages);
            receiveImagesSwitch.setOnCheckedChangeListener(this);
        }
        if (stretchImagesSwitch != null) {
            stretchImagesSwitch.setEnabled(true);
            stretchImagesSwitch.setOnCheckedChangeListener(null);
            stretchImagesSwitch.setChecked(phd2.stretchImages);
            stretchImagesSwitch.setSelected(phd2.stretchImages);
            stretchImagesSwitch.setOnCheckedChangeListener(this);
        }
        if (liveView != null) liveView.setImageBitmap(phd2.bitmap);
        if (exposureSpinner != null) {
            exposureSpinner.setAdapter(null);
            if (phd2.exposureTimes == null) {
                exposureSpinner.setAdapter(null);
                exposureSpinner.setEnabled(false);
            } else {
                ArrayAdapter<Double> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item);
                int selectedItem = 0;
                double[] exposureTimes = phd2.exposureTimes;
                for (int i = 0; i < exposureTimes.length; i++) {
                    if (exposureTimes[i] == phd2.exposureTime)
                        selectedItem = i;
                    adapter.add(exposureTimes[i]);
                }
                exposureSpinner.setAdapter(adapter);
                exposureSpinner.setSelection(selectedItem);
                exposureSpinner.setEnabled(true);
            }
        }
    }

    @Override
    public void onPHD2Disconnected() {
        if (connectionButton != null) connectionButton.setText(context.getString(R.string.connect));
        if (connectionButton != null) connectionButton.setEnabled(true);
        if (serversSpinner != null) serversSpinner.setEnabled(true);
        if (portEditText != null) portEditText.setEnabled(true);
        if (statusLabel != null)
            statusLabel.setText(context.getString(R.string.current_state_default));
        if (raCorrectionLabel != null) raCorrectionLabel.setVisibility(View.INVISIBLE);
        if (decCorrectionLabel != null) decCorrectionLabel.setVisibility(View.INVISIBLE);
        if (hdfLabel != null) hdfLabel.setText(R.string.half_flux_diameter);
        if (snrLabel != null) snrLabel.setText(R.string.signal_to_noise);
        if (loopBtn != null) {
            setButtonColor(loopBtn, Color.WHITE);
            loopBtn.setEnabled(false);
        }
        if (findStarBtn != null) findStarBtn.setEnabled(false);
        if (guideBtn != null) {
            setButtonColor(guideBtn, Color.WHITE);
            guideBtn.setEnabled(false);
        }
        if (stopBtn != null) {
            setButtonColor(stopBtn, Color.WHITE);
            stopBtn.setEnabled(false);
        }
        if (receiveImagesSwitch != null) receiveImagesSwitch.setEnabled(false);
        if (stretchImagesSwitch != null) stretchImagesSwitch.setEnabled(false);
        if (liveView != null) liveView.setImageBitmap(null);
        if (exposureSpinner != null) {
            exposureSpinner.setAdapter(null);
            exposureSpinner.setEnabled(false);
        }
    }

    @Override
    public void onPHD2Error(Exception e) {
        if (!(e instanceof SocketException))
            requestActionSnack(context.getString(R.string.error) + " " + e.getLocalizedMessage());
    }
}