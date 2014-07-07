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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.sssemil.compass.app.net.GetText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MainActivity extends Activity implements
        ILocationListener.LocationRunnable, SensorEventListener {

    private static final String TAG = "Compass";
    private String PREFS_NAME;
    private RelativeLayout relativeLayout;
    private SensorManager mSensorManager;
    private AlertDialog.Builder adb, adb2;
    private String current_mode = "def";
    private String last_mode = "def";
    private int offset = 0;
    private TextView textView, textView2, textView3, textView4,
            textView5, textView6, textView7, textView8, textView9, textView10, textView12, textView13;
    private ImageView arrow;
    private ILocationListener locationListener;
    private ArrayList<String> addr = new ArrayList<String>();
    private RadioButton lastRadioButton;

    private SharedPreferences mSettings;
    private SharedPreferences.Editor mEditor;

    private boolean mAutoMD = false;
    private double md_offset = 0;
    private float currentDegree = 0f;
    private boolean run_threads = true;

    private HandlerThread mCheckThread;
    private Handler mCheckHandler;
    private String latitudeS, longitudeS;
    private double latitude;
    private double longitude;
    private String main_url = "http://www.ngdc.noaa.gov/geomag-web/" +
            "calculators/calculateDeclination?";

    @Override
    public void locationUpdate(final Location location) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (location != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                if (location.getLatitude() > 0) {
                                    latitudeS = getString(R.string.N);
                                } else {
                                    latitudeS = getString(R.string.S);
                                }
                                if (location.getLongitude() > 0) {
                                    longitudeS = getString(R.string.E);
                                } else {
                                    longitudeS = getString(R.string.W);
                                }
                                textView2.setText(String.valueOf(location.getLatitude())
                                        + "°" + latitudeS);
                                textView7.setText(String.valueOf(location.getLongitude())
                                        + "°" + longitudeS);
                                mEditor = mSettings.edit();
                                mEditor.putString("latitude", String.valueOf(location.getLatitude())
                                        + "°" + latitudeS);
                                mEditor.putString("longitude", String.valueOf(location.getLongitude())
                                        + "°" + longitudeS);
                                mEditor.apply();
                            }
                        });
                        Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
                        List<Address> addresses = gcd.getFromLocation(location.getLatitude(),
                                location.getLongitude(), 1);

                        if (addresses != null) {
                            final Address returnedAddress = addresses.get(0);
                            for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                                addr.add(returnedAddress.getAddressLine(i));
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    textView12.setText(addr.get(0));
                                    textView9.setText(addr.get(1) + ", "
                                            + returnedAddress.getCountryName());
                                }
                            });
                            mEditor = mSettings.edit();
                            mEditor.putString("firstLine", addr.get(1) + ", "
                                    + returnedAddress.getCountryName());
                            mEditor.putString("secondLine", addr.get(0));
                            mEditor.apply();
                        }
                        handleMagneticDip();
                    }
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "catch " + e.toString() + " hit in run", e);
                    }
                } catch (NullPointerException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "catch " + e.toString() + " hit in run", e);
                    }
                }
            }
        }).start();
    }

    private void showGPSDisabledAlertToUser() {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(getString(R.string.gps_is_disabled_enable))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.go_set_gps),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(callGPSSettingIntent);
                            }
                        }
                );
        alertDialogBuilder.setNegativeButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        textView13.setVisibility(View.VISIBLE);
                    }
                }
        );
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setTheme(R.style.AppTheme);
        Tracker t = ((Analytics) getApplication()).getTracker(
                Analytics.TrackerName.APP_TRACKER);
        t.setScreenName("MainActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
        PREFS_NAME = getPackageName() + "_preferences";
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        setContentView(R.layout.activity_main);
        arrow = (ImageView) findViewById(R.id.arrow);
        textView = (TextView) findViewById(R.id.textView);
        textView2 = (TextView) findViewById(R.id.textView2);
        textView3 = (TextView) findViewById(R.id.textView3);
        textView4 = (TextView) findViewById(R.id.textView4);
        textView5 = (TextView) findViewById(R.id.textView5);
        textView6 = (TextView) findViewById(R.id.textView6);
        textView7 = (TextView) findViewById(R.id.textView7);
        textView8 = (TextView) findViewById(R.id.textView8);
        textView9 = (TextView) findViewById(R.id.textView9);
        textView10 = (TextView) findViewById(R.id.textView10);
        textView12 = (TextView) findViewById(R.id.textView12);
        textView13 = (TextView) findViewById(R.id.textView13);
        relativeLayout = (RelativeLayout) findViewById(R.id.rl1);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (mSettings.contains("firstLine")) {
            textView9.setText(mSettings.getString("firstLine", null));
        }

        if (mSettings.contains("secondLine")) {
            textView12.setText(mSettings.getString("secondLine", null));
        }

        if (mSettings.contains("latitude")) {
            textView2.setText(mSettings.getString("latitude", null));
        }

        if (mSettings.contains("longitude")) {
            textView7.setText(mSettings.getString("longitude", null));
        }

        if (mSettings.contains("pointto")) {
            current_mode = mSettings.getString("pointto", null);
            if (mSettings.contains("offset") && current_mode.equals("custom")) {
                offset = mSettings.getInt("offset", 0);
            }
        }

        locationListener = LocationListenerGPServices.getInstance(this);
        locationListener.setLocationRunnable(this);

        if (!((LocationManager) getSystemService(LOCATION_SERVICE))
                .isProviderEnabled(LocationManager.GPS_PROVIDER) && mAutoMD) {
            showGPSDisabledAlertToUser();
        } else {
            locationListener.enableMyLocation();
        }

        mCheckThread = new HandlerThread("StateChecker");
        if (run_threads) {
            if (!mCheckThread.isAlive()) {
                mCheckThread.start();
                mCheckHandler = new StateChecker(mCheckThread.getLooper());
                mCheckHandler.sendEmptyMessage(0);
            }
        } else {
            if (mCheckThread.isAlive()) {
                mCheckThread.quit();
            }
        }
        firstRunChecker();

        arrow.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onMCHClick();
                return true;
            }
        });
    }

    public void handleMagneticDip() {
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        if (mSettings.contains("automd")) {
            mAutoMD = mSettings.getBoolean("automd", false);
        }
        if (mAutoMD) {
            GetText gt1 = new GetText();
            try {
                String xml = gt1.execute(main_url + "lat1=" + latitude + "&lon1=" + longitude
                        + "&lat1Hemisphere=" + latitudeS + "&lon1Hemisphere="
                        + longitudeS + "&resultFormat=xml").get();
                md_offset = Double.parseDouble(xml);

            } catch (InterruptedException e) {
                Log.d(TAG, "catch " + e.toString() + " hit in run", e);
            } catch (ExecutionException e) {
                Log.d(TAG, "catch " + e.toString() + " hit in run", e);
            }
        }
        if (md_offset != 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView10.setText(getString(R.string.dip_angle) + ":" + String.valueOf(md_offset));
                }
            });
        }
    }

    public void onMCHClick() {
        LayoutInflater li = LayoutInflater.from(MainActivity.this);
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
                mEditor.apply();
                if (!last_mode.equals(current_mode)) {
                    if (current_mode.equals("def")) {
                        offset = 0;
                        mEditor = mSettings.edit();
                        mEditor.putInt("offset", offset);
                        mEditor.apply();
                    } else if (current_mode.equals("custom") || last_mode.equals("custom")) {
                        final View promptsView = LayoutInflater.from(MainActivity.this)
                                .inflate(R.layout.degree_mode, null);
                        adb = new AlertDialog.Builder(MainActivity.this);
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
                                                    adb2 = new AlertDialog.Builder(MainActivity.this);
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
                MainActivity.this);
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
            final View promptsView = LayoutInflater.from(MainActivity.this)
                    .inflate(R.layout.degree_mode, null);
            adb = new AlertDialog.Builder(MainActivity.this);
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
                                        adb2 = new AlertDialog.Builder(MainActivity.this);
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

    @Override
    protected void onResume() {
        locationListener.enableMyLocation();
        super.onResume();
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        locationListener.disableMyLocation();
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        run_threads = false;
        mCheckHandler.removeCallbacksAndMessages(null);
        if (mCheckThread.isAlive()) {
            mCheckThread.quit();
        }
        mCheckHandler = null;
        mCheckThread = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        run_threads = true;
        if (mCheckThread == null || !mCheckThread.isAlive()) {
            mCheckThread = new HandlerThread("StateChecker");
            mCheckThread.start();
            mCheckHandler = new StateChecker(mCheckThread.getLooper());
            mCheckHandler.sendEmptyMessage(0);
        }
        Log.i(TAG, "Tracker");
    }

    private String formatValueWithCardinalDirection(float degree) {
        int cardinalDirectionIndex = (int) (Math.floor(((degree - 22.5) % 360) / 45) + 1) % 8;
        String[] cardinalDirections = getResources().getStringArray(
                R.array.cardinal_directions);

        return cardinalDirections[cardinalDirectionIndex];
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        mSettings = getSharedPreferences(PREFS_NAME, 0);
        if (mSettings.contains("pointto")) {
            current_mode = mSettings.getString("pointto", null);
            if (mSettings.contains("offset") && current_mode.equals("custom")) {
                offset = mSettings.getInt("offset", 0);
            }
        }
        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0] + md_offset);
        if (Math.round(degree) > 360) {
            degree -= 360;
        }
        textView.setText(Math.round(degree) + "°");
        textView8.setText(formatValueWithCardinalDirection(degree));
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        ra.setDuration(210);
        ra.setFillAfter(true);

        RotateAnimation ra2 = new RotateAnimation(
                -currentDegree,
                degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        ra2.setDuration(210);
        ra2.setFillAfter(true);

        RotateAnimation ra3 = new RotateAnimation(
                0,
                offset,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);
        ra3.setDuration(210);
        ra3.setFillAfter(true);

        // Start the animation
        relativeLayout.startAnimation(ra);
        currentDegree = -degree;
        textView3.startAnimation(ra2);
        textView4.startAnimation(ra2);
        textView5.startAnimation(ra2);
        textView6.startAnimation(ra2);
        arrow.startAnimation(ra3);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            new Thread(new Runnable() {
                public void run() {
                    Intent intent = new Intent(MainActivity.this,
                            Settings.class);
                    startActivity(intent);
                }
            }).start();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void firstRunChecker() {
        boolean isFirstRun;
        if (!mSettings.contains("isFirstRun")) {
            isFirstRun = true;
        } else {
            isFirstRun = mSettings.getBoolean("isFirstRun", false);
        }

        if (isFirstRun) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.welcome));
            builder.setMessage(getString(R.string.welcome_text));
            builder.setPositiveButton(getString(R.string.pos_ans), null);
            builder.show();
            mEditor = mSettings.edit();
            mEditor.putBoolean("isFirstRun", false);
            mEditor.apply();
        }
    }

    private class StateChecker extends Handler {

        public StateChecker(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            if (run_threads) {
                locationListener.enableMyLocation();
                if (!((LocationManager) getSystemService(LOCATION_SERVICE))
                        .isProviderEnabled(LocationManager.GPS_PROVIDER) && mAutoMD) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView13.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            textView13.setVisibility(View.GONE);
                        }
                    });
                }
                sendEmptyMessageDelayed(0, 20000);
            }
        }
    }
}