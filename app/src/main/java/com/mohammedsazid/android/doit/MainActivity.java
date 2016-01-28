/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Mohammed Sazid-Al-Rashid
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.mohammedsazid.android.doit;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mohammedsazid.android.doit.services.TimerService;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener {
    BroadcastReceiver timerBroadcastReceiver;

    private TextView mTimerMinTv;
    private TextView mTimerSecTv;
    private LinearLayout mTimerViewsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        acquireLock();
        initTypeface();
    }

    private void timeRemaining() {
        SimpleDateFormat sdfMin =
                new SimpleDateFormat("mm", Locale.getDefault());
        SimpleDateFormat sdfSec =
                new SimpleDateFormat("ss", Locale.getDefault());
        String timerTextMin;
        String timerTextSec;

        long time = TimerService.TIME_REMAINING;
        if (!TimerService.SERVICE_IS_RUNNING) {
            time = TimeUnit.MINUTES.toMillis(25);
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
        long secs = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.MINUTES.toMillis(minutes));

        timerTextMin = sdfMin.format(TimeUnit.MINUTES.toMillis(minutes));
        timerTextSec = sdfSec.format(TimeUnit.SECONDS.toMillis(secs));

        mTimerMinTv.setText(timerTextMin);
        mTimerSecTv.setText(timerTextSec);
    }

    private void initTypeface() {
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/d7_mono.ttf");
        mTimerMinTv.setTypeface(font);
        mTimerSecTv.setTypeface(font);
    }

    private void bindViews() {
        mTimerMinTv = (TextView) findViewById(R.id.timerMinTv);
        mTimerSecTv = (TextView) findViewById(R.id.timerSecTv);
        mTimerViewsContainer = (LinearLayout) findViewById(R.id.timer_view_container);
        mTimerViewsContainer.setOnTouchListener(this);
    }

    private void acquireLock() {
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerTimerTickReceiver();
        timeRemaining();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerBroadcastReceiver != null)
            unregisterReceiver(timerBroadcastReceiver);
    }

    private void registerTimerTickReceiver() {
        timerBroadcastReceiver = new BroadcastReceiver() {
            SimpleDateFormat sdfMin =
                    new SimpleDateFormat("mm", Locale.getDefault());
            SimpleDateFormat sdfSec =
                    new SimpleDateFormat("ss", Locale.getDefault());
            String timerTextMin;
            String timerTextSec;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(TimerService.ACTION_TIMER_TICK)) {
                    long time = intent.getLongExtra(
                            TimerService.EXTRA_REMAINING_TIME,
                            System.currentTimeMillis());

                    long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
                    long secs = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.MINUTES.toMillis(minutes));

                    timerTextMin = sdfMin.format(TimeUnit.MINUTES.toMillis(minutes));
                    timerTextSec = sdfSec.format(TimeUnit.SECONDS.toMillis(secs));

                    mTimerMinTv.setText(timerTextMin);
                    mTimerSecTv.setText(timerTextSec);
                }
            }
        };
        registerReceiver(timerBroadcastReceiver,
                new IntentFilter(TimerService.ACTION_TIMER_TICK));
    }

    @SuppressLint("SetTextI18n")
    public void toggleTimer(View v) {
        if (v.getId() == R.id.timer_view_container) {
            v.performHapticFeedback(
                    HapticFeedbackConstants.VIRTUAL_KEY,
                    HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            );

            if (!TimerService.SERVICE_IS_RUNNING) {
                Log.d("CLICK", "Starting service");
                Intent intent = new Intent(this, TimerService.class);
                startService(intent);
            } else {
                Log.d("CLICK", "Stopping service");
                Intent intent = new Intent(this, TimerService.class);
                stopService(intent);
                mTimerMinTv.setText("25");
                mTimerSecTv.setText("00");
            }
            TimerService.SERVICE_IS_RUNNING = !TimerService.SERVICE_IS_RUNNING;
        }
    }

    public void showHelp(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Info")
                .setMessage(getString(R.string.info))
                .setIcon(R.drawable.ic_help)
                .create()
                .show();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.timer_view_container) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    mTimerViewsContainer.setAlpha(0.6f);
                    break;
                case MotionEvent.ACTION_UP:
                    mTimerViewsContainer.setAlpha(1f);
                    break;
            }
        }

        return false;
    }
}
