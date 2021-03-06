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

package com.mohammedsazid.android.doit.services;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.mohammedsazid.android.doit.MainActivity;
import com.mohammedsazid.android.doit.R;

import java.util.concurrent.TimeUnit;

public class TimerService extends Service {
    public static final String ACTION_TIMER_TICK
            = "com.mohammedsazid.android.doit.action.timer_tick";
    public static final String EXTRA_REMAINING_TIME
            = "com.mohammedsazid.android.doit.extra.remaining_time";
    public static final long COUNTDOWN_TIME = TimeUnit.MINUTES.toMillis(25);
    public static final long BREAK_TIME = TimeUnit.MINUTES.toMillis(5);
    public static long TIME_REMAINING = COUNTDOWN_TIME;
    private static final String LOG_TAG = TimerService.class.getSimpleName();
    public static boolean SERVICE_IS_RUNNING = false;
    private CounterClass mTimeCounter;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        TIME_REMAINING = COUNTDOWN_TIME;
        mTimeCounter.cancel();
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // startForeground(...);
        if (mTimeCounter == null) {
            mTimeCounter = new CounterClass(COUNTDOWN_TIME, 1000);
            mTimeCounter.start();
            notifyTaskStarted();
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void notifyTaskStarted() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                23,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Do it")
                .setContentText("Go back to work!")
                .setSmallIcon(R.drawable.ic_clock)
                .setContentIntent(pi)
                .setColor(0xFF000000)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();

        startForeground(23, notification);

        Intent i = new Intent(this, BreakNotifyService.class);
        PendingIntent breakServicePi = PendingIntent.getService(
                this,
                64,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(breakServicePi);
    }

    private void notifyTaskFinished() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this,
                23,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle("Do it")
                .setContentText("Take some rest :)")
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentIntent(pi)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(Notification.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFF000000)
                .setAutoCancel(true)
                .build();

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(24, notification);
    }

    private class CounterClass extends CountDownTimer {
        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            TIME_REMAINING = millisUntilFinished;
            Intent intent = new Intent(ACTION_TIMER_TICK);
            intent.putExtra(EXTRA_REMAINING_TIME, millisUntilFinished);
            sendBroadcast(intent);
        }

        @Override
        public void onFinish() {
            // create a notification here
            Log.d(LOG_TAG, "Task finished");
            stopTimerService();
        }
    }

    private void stopTimerService() {
        TIME_REMAINING = COUNTDOWN_TIME;
        SERVICE_IS_RUNNING = false;
        Intent intent = new Intent(ACTION_TIMER_TICK);
        intent.putExtra(EXTRA_REMAINING_TIME, TIME_REMAINING);
        sendBroadcast(intent);
        stopForeground(true);
        notifyTaskFinished();

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(this, BreakNotifyService.class);
        PendingIntent pi = PendingIntent.getService(
                this,
                64,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + BREAK_TIME, pi);
        } else {
            am.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + BREAK_TIME, pi);
        }

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(
                        TimerService.this,
                        "Time's up!",
                        Toast.LENGTH_SHORT).show();
            }
        });

        stopSelf();
    }
}
