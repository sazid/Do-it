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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

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
    private static final String LOG_TAG = TimerService.class.getSimpleName();
    public static boolean SERVICE_IS_RUNNING = false;
    private CounterClass mTimeCounter;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
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
            Log.d(LOG_TAG, "Tick");
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
        Intent intent = new Intent(ACTION_TIMER_TICK);
        intent.putExtra(EXTRA_REMAINING_TIME, 0l);
        sendBroadcast(intent);
        SERVICE_IS_RUNNING = false;
        stopForeground(true);
        notifyTaskFinished();
        stopSelf();
    }
}
