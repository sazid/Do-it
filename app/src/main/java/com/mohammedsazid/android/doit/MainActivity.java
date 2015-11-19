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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.mohammedsazid.android.doit.services.TimerService;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {
    BroadcastReceiver timerBroadcastReceiver;

    private FloatingActionButton mTimerBtn;
    private TextView mTimerMinTv;
    private TextView mTimerSecTv;
    private TextView mMinIndicatorTv;
    private TextView mSecIndicatorTv;

    private Animation mTimerBtnAnim;

    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        acquireLock();
        initTypeface();

        if (TimerService.SERVICE_IS_RUNNING) {
            mTimerBtn.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_stop));
        }

        mTimerBtnAnim = AnimationUtils.loadAnimation(
                this, R.anim.toggle_button_anim);
    }

    private void initTypeface() {
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/d7_mono.ttf");
        mTimerMinTv.setTypeface(font);
        mTimerSecTv.setTypeface(font);
        mMinIndicatorTv.setTypeface(font);
        mSecIndicatorTv.setTypeface(font);
    }

    private void bindViews() {
        mTimerBtn = (FloatingActionButton) findViewById(R.id.timerBtn);
        mTimerMinTv = (TextView) findViewById(R.id.timerTv);
        mTimerSecTv = (TextView) findViewById(R.id.timerSecTv);
        mMinIndicatorTv = (TextView) findViewById(R.id.minIndicatorTv);
        mSecIndicatorTv = (TextView) findViewById(R.id.secIndicatorTv);
    }

    private void animateToggleButton(boolean toggle) {
        mTimerBtn.startAnimation(mTimerBtnAnim);

        if (toggle) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTimerBtn.setImageDrawable(
                            ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_stop));
                }
            }, 200);
        } else {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTimerBtn.setImageDrawable(
                            ContextCompat.getDrawable(MainActivity.this, R.drawable.ic_play));
                }
            }, 200);
        }
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
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timerBroadcastReceiver != null)
            unregisterReceiver(timerBroadcastReceiver);
    }

    private void registerTimerTickReceiver() {
        timerBroadcastReceiver = new BroadcastReceiver() {
            Calendar tickTime = new GregorianCalendar();
            SimpleDateFormat sdfMin =
                    new SimpleDateFormat("mm", Locale.getDefault());
            SimpleDateFormat sdfSec =
                    new SimpleDateFormat("ss", Locale.getDefault());
            String timerTextMin;
            String timerTextSec;

            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(TimerService.ACTION_TIMER_TICK)) {
                    tickTime.setTimeInMillis(intent.getLongExtra(
                            TimerService.EXTRA_REMAINING_TIME,
                            System.currentTimeMillis()));
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

    public void toggleTimer(View view) {
        if (!TimerService.SERVICE_IS_RUNNING) {
            animateToggleButton(true);
            Log.d("CLICK", "Starting service");
            Intent intent = new Intent(this, TimerService.class);
            startService(intent);
        } else {
            animateToggleButton(false);
            Log.d("CLICK", "Stopping service");
            Intent intent = new Intent(this, TimerService.class);
            stopService(intent);
            mTimerMinTv.setText("25");
        }
        TimerService.SERVICE_IS_RUNNING = !TimerService.SERVICE_IS_RUNNING;
    }
}
