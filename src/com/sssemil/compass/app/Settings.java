/*
 * Copyright (c) 2014 Emil Suleymanov <suleymanovemil8@gmail.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package com.sssemil.compass.app;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;


public class Settings extends PreferenceActivity {

    private static final String TAG = "Compass";
    private AlertDialog.Builder adb, adb2;
    private String current_mode = "def";
    private String last_mode = "def";
    private int offset = 0;
    private RadioButton lastRadioButton;
    private SharedPreferences.Editor mEditor;
    private SharedPreferences mSettings;

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Settings.this,
                MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tracker t = ((Analytics) getApplication()).getTracker(
                Analytics.TrackerName.APP_TRACKER);
        t.setScreenName("Settings");
        t.send(new HitBuilders.AppViewBuilder().build());
        addPreferencesFromResource(R.xml.settings);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        mSettings = getSharedPreferences(getPackageName() + "_preferences", 0);
        if (mSettings.contains("pointto")) {
            current_mode = mSettings.getString("pointto", null);
            if (mSettings.contains("offset") && current_mode.equals("custom")) {
                offset = mSettings.getInt("offset", 0);
            }
        }

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            assert pInfo != null;
            findPreference("buildPref").setSummary(pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "catch " + e.toString() + " hit in run", e);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                Intent intent = new Intent(Settings.this,
                        MainActivity.class);
                startActivity(intent);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                                         final Preference preference) {
        String key = preference.getKey();
        final AlertDialog.Builder adb = new AlertDialog.Builder(this);
        if (key.equals("aboutPref")) {
            adb.setTitle(getString(R.string.about));
            PackageInfo pInfo = null;
            String version = "-.-.-";
            try {
                pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "catch " + e.toString() + " hit in run", e);
                }
            }
            version = pInfo.versionName;
            adb.setMessage(getResources().getString(R.string.license1) + " v" + version + "\n" + getResources().getString(R.string.license2) + "\n");
            adb.setPositiveButton(getString(R.string.pos_ans), null);
            AlertDialog dialog = adb.show();

            TextView messageView = (TextView) dialog.findViewById(android.R.id.message);
            messageView.setGravity(Gravity.CENTER);
        } else if (key.equals("ptPref")) {
            onMCHClick();
        }
        return true;
    }

    public void onMCHClick() {
        LayoutInflater li = LayoutInflater.from(Settings.this);
        final View promptsView = li.inflate(R.layout.show_to, null);
        int rb_id = promptsView.getResources().getIdentifier(current_mode,
                "id", getPackageName());

        lastRadioButton = (RadioButton) promptsView.findViewById(rb_id);
        lastRadioButton.setChecked(true);
        final RadioGroup rg = (RadioGroup) promptsView.findViewById(R.id.radioGroup1);

        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                last_mode = current_mode;
                current_mode = promptsView.getResources()
                        .getResourceEntryName(rg.getCheckedRadioButtonId());
                mEditor = mSettings.edit();
                mEditor.putString("pointto", current_mode);
                mEditor.commit();
                if (!last_mode.equals(current_mode)) {
                    if (current_mode.equals("def")) {
                        offset = 0;
                        mEditor = mSettings.edit();
                        mEditor.putInt("offset", offset);
                        mEditor.apply();
                    } else if (current_mode.equals("custom") || last_mode.equals("custom")) {
                        final View promptsView = LayoutInflater.from(Settings.this)
                                .inflate(R.layout.degree_mode, null);
                        adb = new AlertDialog.Builder(Settings.this);
                        adb.setTitle(getString(R.string.ent_cus_dgr));
                        adb.setView(promptsView);
                        adb
                                .setCancelable(false)
                                .setPositiveButton(getString(R.string.ok),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                try {
                                                    EditText editText = (EditText) promptsView.findViewById(R.id.editText);
                                                    offset = Integer.parseInt(editText.getText().toString());
                                                    mEditor = mSettings.edit();
                                                    mEditor.putInt("offset", offset);
                                                    mEditor.apply();
                                                    last_mode = current_mode;
                                                } catch (NumberFormatException e) {
                                                    adb2 = new AlertDialog.Builder(Settings.this);
                                                    adb2.setTitle("Error");
                                                    adb2
                                                            .setCancelable(false)
                                                            .setPositiveButton(getString(R.string.pos_ans),
                                                                    new DialogInterface.OnClickListener() {
                                                                        public void onClick(DialogInterface dialog, int id) {
                                                                            dialog.cancel();
                                                                        }
                                                                    }
                                                            )
                                                            .setMessage("Bad number!");
                                                    adb2.show();
                                                    current_mode = last_mode;
                                                    mEditor = mSettings.edit();
                                                    mEditor.putString("pointto", current_mode);
                                                    mEditor.apply();
                                                    lastRadioButton.setChecked(true);
                                                    dialog.cancel();
                                                }
                                            }
                                        }
                                )
                                .setNegativeButton(getString(R.string.cancel),
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,
                                                                int id) {
                                                current_mode = last_mode;
                                                mEditor = mSettings.edit();
                                                mEditor.putString("pointto", current_mode);
                                                mEditor.apply();
                                                lastRadioButton.setChecked(true);
                                                dialog.cancel();
                                            }
                                        }
                                );
                        adb.show();
                    }
                }
            }
        });

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                Settings.this);
        alertDialogBuilder.setTitle(getString(R.string.point_to));
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton(getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                            }
                        }
                )
                .setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }
                );
        alertDialogBuilder.setView(promptsView);
        alertDialogBuilder.show();
    }

    public void onCustomClick(View view) {
        if (last_mode.equals("custom")) {
            final View promptsView = LayoutInflater.from(Settings.this)
                    .inflate(R.layout.degree_mode, null);
            adb = new AlertDialog.Builder(Settings.this);
            adb.setTitle(getString(R.string.ent_cus_dgr));
            adb.setView(promptsView);
            adb
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    try {
                                        EditText editText = (EditText) promptsView.findViewById(R.id.editText);
                                        offset = Integer.parseInt(editText.getText().toString());
                                        mEditor = mSettings.edit();
                                        mEditor.putInt("offset", offset);
                                        mEditor.apply();
                                    } catch (NumberFormatException e) {
                                        adb2 = new AlertDialog.Builder(Settings.this);
                                        adb2.setTitle("Error");
                                        adb2
                                                .setCancelable(false)
                                                .setPositiveButton(getString(R.string.pos_ans),
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                dialog.cancel();
                                                            }
                                                        }
                                                )
                                                .setMessage("Bad number!");
                                        adb2.show();
                                        current_mode = last_mode;
                                        lastRadioButton.setChecked(true);
                                        dialog.cancel();
                                    }
                                }
                            }
                    )
                    .setNegativeButton(getString(R.string.cancel),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                                    int id) {
                                    current_mode = last_mode;
                                    lastRadioButton.setChecked(true);
                                    dialog.cancel();
                                }
                            }
                    );
            adb.show();
        }
    }
}