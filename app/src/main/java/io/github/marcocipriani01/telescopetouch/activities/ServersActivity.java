package io.github.marcocipriani01.telescopetouch.activities;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.InputType;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.woxthebox.draglistview.DragListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.marcocipriani01.telescopetouch.R;
import io.github.marcocipriani01.telescopetouch.TelescopeTouchApp;
import io.github.marcocipriani01.telescopetouch.views.ServersItemAdapter;

/**
 * @author marcocipriani01
 */
interface ServersReloadListener {
    void loadServers();
}

/**
 * Activity to manage the list of servers.
 *
 * @author marcocipriani01
 */
public class ServersActivity extends AppCompatActivity implements ServersReloadListener {

    public static final String PREFERENCES_TAG = "SERVERS_LIST";
    private DragListView serversListView;

    /**
     * Asks the user to add a new server.
     */
    @SuppressWarnings("SpellCheckingInspection")
    @SuppressLint("SetTextI18n")
    static void addServer(final Context context, final ServersReloadListener onServersReload) {
        final EditText input = new EditText(context);
        input.setText("192.168.");
        input.setHint(context.getString(R.string.ip_address));
        InputFilter[] filters = new InputFilter[1];
        filters[0] = (source, start, end, dest, dstart, dend) -> {
            if (end > start) {
                String destTxt = dest.toString();
                String resultingTxt = destTxt.substring(0, dstart)
                        + source.subSequence(start, end)
                        + destTxt.substring(dend);
                if (!resultingTxt
                        .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                    return "";
                } else {
                    String[] splits = resultingTxt.split("\\.");
                    for (String split : splits) {
                        if (Integer.parseInt(split) > 255) {
                            return "";
                        }
                    }
                }
            }
            return null;
        };
        input.setFilters(filters);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int padding = TelescopeTouchApp.getAppResources().getDimensionPixelSize(R.dimen.padding_medium);
        layoutParams.setMargins(padding, 0, padding, 0);
        layout.addView(input, layoutParams);
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);

        Dialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.host_prompt_text).setView(layout).setCancelable(false)
                .setPositiveButton(context.getString(R.string.ok), (dialog12, id) -> {
                    inputMethodManager.hideSoftInputFromWindow(input.getWindowToken(), 0);
                    String server = input.getText().toString();
                    if (!server.equals("")) {
                        if (!isIp(server))
                            Toast.makeText(context, context.getString(R.string.not_valid_ip), Toast.LENGTH_SHORT).show();
                        // Retrieve the list
                        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        Set<String> set = preferences.getStringSet(PREFERENCES_TAG, null);
                        List<String> serversList;
                        int max = -1;
                        if (set != null) {
                            serversList = new ArrayList<>(set);
                            for (String s : serversList) {
                                max = Math.max(max, Integer.parseInt(s.substring(0, s.indexOf('#'))));
                            }
                        } else {
                            serversList = new ArrayList<>();
                        }
                        serversList.add(0, (max + 1) + "#" + server);
                        // Save the list
                        Set<String> newSet = new HashSet<>(serversList);
                        preferences.edit().putStringSet(PREFERENCES_TAG, newSet).apply();
                        // Update
                        onServersReload.loadServers();
                    } else {
                        Toast.makeText(context, context.getString(R.string.empty_host), Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(context.getString(R.string.cancel), null).create();
        dialog.show();
        input.requestFocus();
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public static boolean isIp(final String ip) {
        return ip.matches("^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$");
    }

    public static void sortPairs(List<Pair<Long, String>> list) {
        Collections.sort(list, (o1, o2) -> (int) ((o1.first != null ? o1.first : 0) - (o2.first != null ? o2.first : 0)));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_servers);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        findViewById(R.id.addServerFab).setOnClickListener(v -> {
            save();
            addServer(ServersActivity.this, ServersActivity.this);
        });

        serversListView = findViewById(R.id.serversList);
        serversListView.setLayoutManager(new LinearLayoutManager(this));
        serversListView.setCanDragHorizontally(false);
        loadServers();

        Toast.makeText(this, R.string.servers_list_toast, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        save();
    }

    private void save() {
        // Save the new list
        List<Pair<Long, String>> list = ((ServersItemAdapter) serversListView.getAdapter()).getItemList();
        Set<String> set = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            String s = list.get(i).second;
            set.add(i + "#" + s);
        }
        PreferenceManager.getDefaultSharedPreferences(this).edit().putStringSet(PREFERENCES_TAG, set).apply();
    }

    @Override
    public void loadServers() {
        Set<String> set = PreferenceManager.getDefaultSharedPreferences(ServersActivity.this)
                .getStringSet(PREFERENCES_TAG, null);
        ArrayList<Pair<Long, String>> serversList = new ArrayList<>();
        if (set != null) {
            for (String s : set) {
                int index = s.indexOf('#');
                serversList.add(new Pair<>(Long.valueOf(s.substring(0, index)), s.substring(index + 1)));
            }
            sortPairs(serversList);
        }
        serversListView.setAdapter(new ServersItemAdapter(serversList, R.layout.servers_list_item, R.id.listview_drag, false) {
            @Override
            public void onItemLongClicked(final TextView view) {
                new AlertDialog.Builder(ServersActivity.this)
                        .setTitle(R.string.sure)
                        .setMessage(R.string.remove_server)
                        .setCancelable(false)
                        .setPositiveButton(getString(R.string.ok), (dialog, id) -> {
                            List<Pair<Long, String>> list = getItemList();
                            for (int i = 0; i < list.size(); i++) {
                                if (list.get(i).second == view.getText()) {
                                    removeItem(i);
                                    break;
                                }
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .create().show();
            }
        }, false);
    }
}