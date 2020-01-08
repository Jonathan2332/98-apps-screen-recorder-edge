package a98apps.recorderedge.record;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.Objects;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.util.SecurityPreferences;
import a98apps.recorderedge.view.ListVideos;

import static a98apps.recorderedge.constants.Constants.ACTION_CLICK_NOTIFICATION;
import static a98apps.recorderedge.constants.Constants.ACTION_CLOSE;
import static a98apps.recorderedge.constants.Constants.ACTION_STOP_REC;
import static a98apps.recorderedge.constants.Constants.ACTION_DELETE;
import static a98apps.recorderedge.constants.Constants.ACTION_SHARE;
import static a98apps.recorderedge.constants.Constants.ACTION_SHOW_NOTIFICATION;
import static a98apps.recorderedge.constants.Constants.ACTION_START_REC;
import static a98apps.recorderedge.constants.Constants.ACTION_WATCH;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_CHANNEL_ID_FINISHED;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_CHANNEL_ID_RECORDING;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_FINISHED_ID;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_RECORDING_ID;

public class RecordService extends Service
{
    private SecurityPreferences mSecurityPreferences;
    private BroadcastReceiver mReceiver;
    private AlertDialog actionDialog;
    private RecorderController recorderController;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        if(mSecurityPreferences == null)
            mSecurityPreferences = new SecurityPreferences(getApplicationContext());

        if(recorderController == null)
            recorderController = new RecorderController(getApplicationContext(), mSecurityPreferences);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(mReceiver != null) {
            getApplicationContext().unregisterReceiver(mReceiver);
            mReceiver = null;
        }

        stopForeground(true);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        super.onStartCommand(intent, flags, startId);

        String action = intent.getAction();
        if (ACTION_START_REC.equals(action))
            startRecording(getApplicationContext());
        else if (ACTION_STOP_REC.equals(action))
            stopRecording(getApplicationContext());

        return START_NOT_STICKY;
    }

    private void startRecording(Context context)
    {
        stopForeground(true);

        if(recorderController.prepare())
        {
            if (recorderController.start())
            {
                if (Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.SHOW_RECORDING_NOTIFICATION)))
                {
                    Notification notification = getPersistentNotification(context);
                    // show the notification
                    if (notification != null) {
                        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;

                        startForeground(getPackageName().hashCode() + NOTIFICATION_RECORDING_ID, notification);
                    }
                }
            }
        }
    }
    private void stopRecording(Context context)
    {
        stopForeground(true);

        recorderController.stop();

        actionOnFinish(context);
    }

    private void updateDeleteGallery()
    {
        if(recorderController.isInternal())
        {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(new File(recorderController.getFilePath())));
            getApplicationContext().sendBroadcast(scanIntent);
        }
        else
        {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.parse(recorderController.getFilePathUri()));
            getApplicationContext().sendBroadcast(scanIntent);
        }
    }


    private void actionOnFinish(Context context)
    {
        switch (Integer.parseInt(mSecurityPreferences.getSetting(Constants.FINISH_ACTION)))
        {
            case Constants.ACTION_NOTIFICATION:
                Notification notification = getNotification(context);
                notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if(!TextUtils.isEmpty(NOTIFICATION_CHANNEL_ID_FINISHED)) {
                        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        NotificationChannel channel = manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_FINISHED);
                        if(channel.getImportance() == NotificationManager.IMPORTANCE_NONE)
                        {
                            Toast.makeText(context, getApplicationContext().getString(R.string.notification_finish_disabled), Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    else
                    {
                        Toast.makeText(context, getApplicationContext().getString(R.string.notification_finish_disabled), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                else
                {
                    if(!NotificationManagerCompat.from(context).areNotificationsEnabled())
                    {
                        Toast.makeText(context, getApplicationContext().getString(R.string.notifications_are_disabled), Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Intent close = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(close);
                // show the notification
                startForeground(getPackageName().hashCode() + NOTIFICATION_FINISHED_ID, notification);
                break;
            case Constants.ACTION_POPUP:
                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                context.sendBroadcast(it);
                if (!Settings.canDrawOverlays(getApplicationContext()))
                    Toast.makeText(context, getApplicationContext().getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
                else
                    showPopup(context);
                break;
            case Constants.ACTION_VIDEOS:
                Intent listVideos = new Intent(context, ListVideos.class);
                listVideos.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(listVideos);
                break;
            default:
                break;
        }
    }


    private void showPopup(final Context context)
    {
        actionDialog = new AlertDialog.Builder(context).create();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Objects.requireNonNull(actionDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        else
            Objects.requireNonNull(actionDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        actionDialog.setIcon(R.mipmap.ic_launcher);
        actionDialog.setTitle(context.getString(R.string.video_recorded));
        actionDialog.setMessage(context.getString(R.string.what_you_do));
        actionDialog.setButton(DialogInterface.BUTTON_NEUTRAL, context.getString(R.string.text_watch_caps),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent videoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(recorderController.getFilePath()));
                        videoIntent.setDataAndType(Uri.parse(recorderController.getFilePath()), "video/mp4");
                        videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(videoIntent);

                    }
                });
        actionDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.text_share_caps),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setDataAndType(Uri.fromFile(new File(recorderController.getFilePath())), "video/mp4");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(recorderController.getFilePath())));
                        context.startActivity(Intent.createChooser(shareIntent, getApplicationContext().getString(R.string.share_with)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
        actionDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.text_delete_caps),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(recorderController.isInternal())
                        {
                            File file = new File(recorderController.getFilePath());
                            if(file.exists())
                                if(file.delete())
                                {
                                    updateDeleteGallery();
                                    Toast.makeText(context, getApplicationContext().getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
                                }
                                else
                                    Toast.makeText(context, getApplicationContext().getString(R.string.error_on_delete), Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(context, getApplicationContext().getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            DocumentFile path = DocumentFile.fromTreeUri(context, Uri.parse(mSecurityPreferences.getSetting(Constants.RECORD_PATH)));
                            if(path != null)
                            {
                                DocumentFile file = path.findFile(recorderController.getFileName());
                                if(file != null)
                                {
                                    if (file.delete()) {
                                        Toast.makeText(context, getApplicationContext().getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
                                        updateDeleteGallery();
                                    }
                                    else
                                        Toast.makeText(context, getApplicationContext().getString(R.string.error_on_delete), Toast.LENGTH_SHORT).show();
                                }
                                else
                                    Toast.makeText(context, getApplicationContext().getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                            }
                            else
                                Toast.makeText(context, getApplicationContext().getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                        }
                    }
                });


        actionDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                actionDialog.dismiss();
            }
        });
        actionDialog.show();
    }
    @SuppressWarnings("deprecation")
    private Notification getNotification(final Context c) {
        int icon = R.mipmap.ic_launcher;
        long when = System.currentTimeMillis();


        Intent notificationIntent;
        Intent notificationWatchIntent;
        Intent notificationShareIntent;
        Intent notificationDeleteIntent;

        PendingIntent contentIntent;
        PendingIntent watchIntent;
        PendingIntent shareIntent;
        PendingIntent deleteIntent;

        notificationIntent = new Intent(ACTION_CLICK_NOTIFICATION);
        notificationIntent.putExtra(ACTION_CLICK_NOTIFICATION, ACTION_CLOSE);

        notificationWatchIntent = new Intent(ACTION_CLICK_NOTIFICATION);
        notificationWatchIntent.putExtra(ACTION_CLICK_NOTIFICATION, ACTION_WATCH);

        notificationShareIntent = new Intent(ACTION_CLICK_NOTIFICATION);
        notificationShareIntent.putExtra(ACTION_CLICK_NOTIFICATION, ACTION_SHARE);

        notificationDeleteIntent = new Intent(ACTION_CLICK_NOTIFICATION);
        notificationDeleteIntent.putExtra(ACTION_CLICK_NOTIFICATION, ACTION_DELETE);

        contentIntent = PendingIntent.getBroadcast(c, 0,
                notificationIntent,
                // flag updates existing persistent notification
                PendingIntent.FLAG_UPDATE_CURRENT);

        watchIntent = PendingIntent.getBroadcast(c, 1,
                notificationWatchIntent,
                // flag updates existing persistent notification
                PendingIntent.FLAG_UPDATE_CURRENT);

        shareIntent = PendingIntent.getBroadcast(c, 2,
                notificationShareIntent,
                // flag updates existing persistent notification
                PendingIntent.FLAG_UPDATE_CURRENT);

        deleteIntent = PendingIntent.getBroadcast(c, 3,
                notificationDeleteIntent,
                // flag updates existing persistent notification
                PendingIntent.FLAG_UPDATE_CURRENT);

        mReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent) {
                context.unregisterReceiver(this);
                mReceiver = null;
                int id = intent.getIntExtra(ACTION_CLICK_NOTIFICATION, -1);
                Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                switch (id)
                {
                    case ACTION_WATCH:
                        context.sendBroadcast(it);
                        Intent videoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(recorderController.getFilePath()));
                        videoIntent.setDataAndType(Uri.parse(recorderController.getFilePath()), "video/mp4");
                        videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(videoIntent);
                        stopForeground(true);
                        break;
                    case ACTION_SHARE:
                        context.sendBroadcast(it);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND, Uri.fromFile(new File(recorderController.getFilePath())));
                        shareIntent.setType("video/mp4");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(recorderController.getFilePath())));
                        context.startActivity(Intent.createChooser(shareIntent, getApplicationContext().getString(R.string.share_with)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        stopForeground(true);
                        break;
                    case ACTION_DELETE:
                        context.sendBroadcast(it);
                        stopForeground(true);
                        if(recorderController.isInternal())
                        {
                            File file = new File(recorderController.getFilePath());
                            if(file.exists())
                                if(file.delete())
                                {
                                    updateDeleteGallery();
                                    Toast.makeText(context, getApplicationContext().getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
                                }
                                else
                                    Toast.makeText(context, getApplicationContext().getString(R.string.error_on_delete), Toast.LENGTH_SHORT).show();
                            else
                                Toast.makeText(context, getApplicationContext().getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            DocumentFile path = DocumentFile.fromTreeUri(context, Uri.parse(mSecurityPreferences.getSetting(Constants.RECORD_PATH)));
                            if(path != null)
                            {
                                DocumentFile file = path.findFile(recorderController.getFileName());
                                if(file != null)
                                {
                                    if (file.delete()) {
                                        Toast.makeText(context, getApplicationContext().getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
                                        updateDeleteGallery();
                                    }
                                    else
                                        Toast.makeText(context, getApplicationContext().getString(R.string.error_on_delete), Toast.LENGTH_SHORT).show();
                                }
                                else
                                    Toast.makeText(context, getApplicationContext().getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                            }
                            else
                                Toast.makeText(context, getApplicationContext().getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        context.sendBroadcast(it);
                        stopForeground(true);
                        break;
                }
            }
        };
        c.getApplicationContext().registerReceiver(mReceiver, new IntentFilter(ACTION_CLICK_NOTIFICATION));

        NotificationCompat.Builder nBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            nBuilder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID_FINISHED);
            return nBuilder.setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_notification).setWhen(when)
                    .setColor(c.getColor(R.color.colorAccent))
                    .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), icon))
                    .setContentTitle(c.getString(R.string.recording_finished))
                    .setContentIntent(contentIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_watch),
                            watchIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_share),
                            shareIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_delete),
                            deleteIntent)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(c.getString(R.string.what_you_do) + getString(R.string.tap_to_dismiss)))
                    .setContentText(c.getString(R.string.what_you_do)).build();
        }
        else
        {
            nBuilder = new NotificationCompat.Builder(c);
            return nBuilder.setContentIntent(contentIntent)
                    .setSmallIcon(R.drawable.ic_notification).setWhen(when)
                    .setColor(c.getColor(R.color.colorAccent))
                    .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), icon))
                    .setContentTitle(c.getString(R.string.recording_finished))
                    .setContentIntent(contentIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_watch),
                            watchIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_share),
                            shareIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_delete),
                            deleteIntent)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setContentText(c.getString(R.string.what_you_do)).build();
        }
    }
    @SuppressWarnings("deprecation")
    private Notification getPersistentNotification(Context c) {
        int icon = R.mipmap.ic_launcher;
        long when = System.currentTimeMillis();

        Intent notificationIntent;
        Intent notificationStopIntent;

        PendingIntent contentIntent;
        PendingIntent stopIntent;

        notificationIntent = new Intent(ACTION_SHOW_NOTIFICATION);

        notificationStopIntent = new Intent(c, RecordService.class);
        notificationStopIntent.setAction(ACTION_STOP_REC);

        contentIntent = PendingIntent.getBroadcast(c, 0,
                notificationIntent,
                // flag updates existing persistent notification
                PendingIntent.FLAG_UPDATE_CURRENT);

        stopIntent = PendingIntent.getService(c, 1,
                notificationStopIntent,
                // flag updates existing persistent notification
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder nBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationManager manager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_FINISHED) == null || manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_RECORDING) == null) {
                NotificationChannel channelRecording = new NotificationChannel(NOTIFICATION_CHANNEL_ID_RECORDING, c.getApplicationContext().getString(R.string.record_notification), NotificationManager.IMPORTANCE_LOW);
                NotificationChannel channelFinished = new NotificationChannel(NOTIFICATION_CHANNEL_ID_FINISHED, c.getApplicationContext().getString(R.string.finish_notification), NotificationManager.IMPORTANCE_HIGH);
                channelFinished.setDescription(c.getApplicationContext().getString(R.string.description_notification_finish));
                channelRecording.setDescription(c.getApplicationContext().getString(R.string.description_notification_recording));
                manager.createNotificationChannel(channelRecording);
                manager.createNotificationChannel(channelFinished);
            }
            nBuilder = new NotificationCompat.Builder(c, NOTIFICATION_CHANNEL_ID_RECORDING);
            return nBuilder.setContentIntent(contentIntent)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_notification).setWhen(when)
                    .setColor(c.getColor(R.color.colorAccent))
                    .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), icon))
                    .setContentTitle(c.getString(R.string.text_recording_started))
                    .setContentIntent(contentIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_stop),
                            stopIntent)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setUsesChronometer(true)
                    .setContentText(c.getString(R.string.text_recording)).build();
        }
        else
        {
            nBuilder = new NotificationCompat.Builder(c);
            return nBuilder.setContentIntent(contentIntent)
                    .setOngoing(true).setWhen(System.currentTimeMillis())
                    .setSmallIcon(R.drawable.ic_notification).setWhen(when)
                    .setColor(c.getColor(R.color.colorAccent))
                    .setLargeIcon(BitmapFactory.decodeResource(c.getResources(), icon))
                    .setContentTitle(c.getString(R.string.text_recording_started))
                    .setContentIntent(contentIntent)
                    .addAction(R.color.colorAccent, c.getString(R.string.text_stop),
                            stopIntent)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setUsesChronometer(true)
                    .setContentText(c.getString(R.string.text_recording)).build();
        }
    }
}
