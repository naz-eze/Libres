package app.libres.android.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.os.IBinder;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

import app.libres.android.R;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.ACCESS_NETWORK_STATE;

public class NotificationService extends Service {

    public static final String NOTIFICATION_CHANNEL_ID = "10001";
    private final static String default_notification_channel_id = "default";

    private final Handler handler = new Handler();
    private Timer timer;
    private TimerTask timerTask;
    private Location userHome;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent != null) {
            userHome = new Location("gps");
            userHome.setLatitude(intent.getDoubleExtra("latitude", 0));
            userHome.setLongitude(intent.getDoubleExtra("longitude", 0));
            startTimer();
            return START_STICKY;
        } else {
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        stopTimerTask();
        super.onDestroy();
    }

    public void startTimer() {
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 5000, 5 * 1000); //
    }

    public void stopTimerTask() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    public void run() {
                        if (checkUserIsFarFromHome()) createNotification();
                    }
                });
            }
        };
    }

    private boolean checkUserIsFarFromHome() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            Location currentLocation = locationManager.getLastKnownLocation("gps");
            return userHome.getLatitude() != 0 && currentLocation.distanceTo(userHome) > 1000;
        }
    }

    private void createNotification() {
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(getApplicationContext(), default_notification_channel_id);
        mBuilder.setContentTitle("Libres");
        mBuilder.setContentText("Estás a más de 1 KM de casa! #quedateEnCasa");
        mBuilder.setSmallIcon(R.drawable.ic_launcher);
        mBuilder.setAutoCancel(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "NOTIFICATION_CHANNEL_NAME", importance);
            mBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
            assert mNotificationManager != null;
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
        assert mNotificationManager != null;
        mNotificationManager.notify((int) System.currentTimeMillis(), mBuilder.build());
        stopTimerTask();
    }
}
