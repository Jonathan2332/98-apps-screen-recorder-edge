package a98apps.recorderedge.util;

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import a98apps.recorderedge.BuildConfig;
import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.floating.FrameRateWindow;
import a98apps.recorderedge.edge.CocktailScreenRecorder;
import a98apps.recorderedge.view.ListVideos;
import wei.mark.standout.StandOutWindow;

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
import static a98apps.recorderedge.edge.CocktailScreenRecorder.mMediaProjection;
import static a98apps.recorderedge.edge.CocktailScreenRecorder.mProjectionManager;

public class RecordService extends Service
{
    private boolean mRecording = false;

    private VirtualDisplay mVirtualDisplay;
    private SecurityPreferences mSecurityPreferences;
    private BroadcastReceiver mReceiver;

    private String fileName;
    private String filePath;
    private String filePathUri;
    private static String filePathTemp;

    private MediaRecorder mMediaRecorder;
    private CocktailScreenRecorder panel;
    private boolean isInternal;

    private AlertDialog actionDialog;

    private RecordingInfo recordingInfo;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private String getFileName() {
        return fileName;
    }

    private void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public static String getFilePathTemp() {
        return filePathTemp;
    }

    private void setFilePathTemp(String filePathTemp) {
        RecordService.filePathTemp = filePathTemp;
    }

    private String getFilePath() {
        return filePath;
    }

    private void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    private String getFilePathUri() {
        return filePathUri;
    }

    private void setFilePathUri(String filePathUri) {
        this.filePathUri = filePathUri;
    }
    private boolean isRecording() {
        return mRecording;
    }
    private void setRecording(boolean mRecording) {
        this.mRecording = mRecording;
    }

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

        if(panel == null)
            panel = new CocktailScreenRecorder();

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

        if(actionDialog != null && actionDialog.isShowing())
            actionDialog.dismiss();
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
    private void startRecording(final Context context)
    {
        if(actionDialog != null && actionDialog.isShowing())
            actionDialog.dismiss();

        if(prepareRecorder(getApplicationContext()))
        {
            if(mVirtualDisplay == null)
                mVirtualDisplay = getVirtualDisplay();

            try
            {
                mMediaRecorder.start();
                setRecording(true);
                panel.updateButtonRec(context, isRecording());

                stopForeground(true);

                if (Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.SHOW_RECORDING_NOTIFICATION))) {
                    Notification notification = getPersistentNotification(context);
                    // show the notification
                    if (notification != null) {
                        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR;

                        startForeground(getPackageName().hashCode() + NOTIFICATION_RECORDING_ID, notification);
                    }
                }
            }
            catch (final IllegalStateException i)
            {
                i.printStackTrace();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        releaseEncoders(context);
                        undoFile(context);
                        createPopUpReport(context, context.getString(R.string.failed_report_message_encoder), i.toString(), recordingInfo);
                    }
                });
            }
        }
    }
    private void stopRecording(Context context)
    {
        StandOutWindow.close(context.getApplicationContext(), FrameRateWindow.class, StandOutWindow.DEFAULT_ID);

        if(mMediaRecorder != null)
        {
            try
            {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            catch (final RuntimeException r)
            {
                r.printStackTrace();
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;

                if (!Settings.canDrawOverlays(getApplicationContext()))
                    Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
                else
                {
                    undoFile(context);
                    createPopUpReport(context, getString(R.string.failed_report_message), r.toString(), recordingInfo);
                }

                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                    mVirtualDisplay = null;
                }
                if (mMediaProjection != null) {
                    mMediaProjection.stop();
                    mMediaProjection = null;
                }

                if (mProjectionManager != null)
                    mProjectionManager = null;

                setRecording(false);
                panel.updateButtonRec(context, isRecording());

                stopForeground(true);

                setFilePathTemp(null);
                return;
            }
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mProjectionManager != null)
            mProjectionManager = null;

        setRecording(false);
        panel.updateButtonRec(context, isRecording());

        stopForeground(true);

        setFilePathTemp(null);

        updateGallery();

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
    private boolean prepareRecorder(final Context context)
    {

        String directory = mSecurityPreferences.getSetting(Constants.RECORD_PATH);
        isInternal = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE));
        FileDescriptor fileDescriptor = null;
        if(isInternal)
        {
            File folder = new File(directory);
            boolean success = true;
            if (!folder.exists())
                success = folder.mkdir();

            if (success)
            {
                setFileName(getCurSysDate() + ".mp4");
                setFilePath(directory + File.separator + getFileName());
            }
            else
            {
                releaseEncoders(context);
                Toast.makeText(context, context.getString(R.string.failed_create_directory), Toast.LENGTH_SHORT).show();
                setFilePathTemp(null);
                return false;
            }
        }
        else
        {
            DocumentFile dir = DocumentFile.fromTreeUri(context, Uri.parse(directory));
            if(dir != null)
            {
                if(!dir.exists())
                {
                    releaseEncoders(context);
                    setFilePathTemp(null);
                    if (!Settings.canDrawOverlays(getApplicationContext()))
                        Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
                    else
                    {
                        final AlertDialog errorDialog = new AlertDialog.Builder(getApplicationContext().getApplicationContext()).create();
                        errorDialog.setTitle("Ops!");
                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            Objects.requireNonNull(errorDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                        else
                            Objects.requireNonNull(errorDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

                        errorDialog.setMessage(getString(R.string.message_path_not_found));
                        errorDialog.setIcon(R.drawable.ic_error);
                        errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(android.R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        Intent pathIntent = new Intent(getApplicationContext(), SelectPath.class);
                                        pathIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        pathIntent.putExtra(Constants.INTENT_GET_ACTIVITY, RequestPermission.class.getName());
                                        startActivity(pathIntent);
                                    }
                                });
                        errorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                errorDialog.dismiss();
                            }
                        });
                        errorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                errorDialog.dismiss();
                            }
                        });
                        errorDialog.show();
                    }
                    return false;
                }

                setFileName(getCurSysDate() + ".mp4");
                DocumentFile dirFile = dir.findFile(getFileName());
                if (dirFile == null)
                {
                    dirFile = dir.createFile("video", getFileName());
                    if(dirFile != null)
                    {
                        try
                        {
                            String readablePath = mSecurityPreferences.getSetting(Constants.READABLE_PATH);
                            if(readablePath.substring(readablePath.length()-1).equals("/"))
                                setFilePath(readablePath +getFileName());
                            else
                                setFilePath(readablePath + File.separator + getFileName());

                            setFilePathUri(dirFile.getUri().toString());
                            ParcelFileDescriptor parcel = getContentResolver().openFileDescriptor(dirFile.getUri(), "rwt");
                            if(parcel != null)
                                fileDescriptor = parcel.getFileDescriptor();
                            else
                            {
                                releaseEncoders(context);
                                Toast.makeText(context, getString(R.string.error_read_file), Toast.LENGTH_SHORT).show();
                                setFilePathTemp(null);
                                return false;
                            }
                        } catch (FileNotFoundException e) {
                            releaseEncoders(context);
                            Toast.makeText(context, getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                            setFilePathTemp(null);
                            return false;
                        }
                    }
                    else
                    {
                        releaseEncoders(context);
                        Toast.makeText(context, getString(R.string.text_failed_create_file), Toast.LENGTH_SHORT).show();
                        setFilePathTemp(null);
                        return false;
                    }
                }
            }
            else
            {
                releaseEncoders(context);
                Toast.makeText(context, context.getString(R.string.failed_create_directory), Toast.LENGTH_SHORT).show();
                setFilePathTemp(null);
                return false;
            }
        }

        mMediaRecorder = new MediaRecorder();

        recordingInfo = getRecordingInfo();

        if(recordingInfo.recordMic)
            mMediaRecorder.setAudioSource(recordingInfo.audioSource);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoFrameRate(60);
        mMediaRecorder.setVideoEncodingBitRate(recordingInfo.bitrate);
        mMediaRecorder.setVideoEncoder(recordingInfo.encoder);

        if(recordingInfo.recordMic)
            mMediaRecorder.setAudioEncoder(recordingInfo.audioEncoder);

        if(checkMinAvailable(recordingInfo.maxFileSize))
        {
            if(checkSize(recordingInfo.maxFileSize))
                mMediaRecorder.setMaxFileSize(recordingInfo.maxFileSize);

            mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
                    {
                        stopRecording(context);
                        Toast.makeText(context,context.getString(R.string.stopped_low_memory), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        else
        {
            if(isInternal)
            {
                File file = new File(getFilePath());
                if(file.exists() && file.delete())
                {
                    if(recordingInfo.maxFileSize == -1)
                    {
                        releaseEncoders(context);
                        Toast.makeText(context, context.getString(R.string.failed_get_external_storage), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    else
                    {
                        releaseEncoders(context);
                        Toast.makeText(context, context.getString(R.string.little_memory_available), Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                if(recordingInfo.maxFileSize == -1)
                {
                    releaseEncoders(context);
                    Toast.makeText(context, context.getString(R.string.failed_get_external_storage), Toast.LENGTH_SHORT).show();
                    return false;
                }
                else
                {
                    releaseEncoders(context);
                    Toast.makeText(context, context.getString(R.string.little_memory_available), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            else
            {
                DocumentFile path = DocumentFile.fromTreeUri(context, Uri.parse(mSecurityPreferences.getSetting(Constants.RECORD_PATH)));
                if(path != null)
                {
                    DocumentFile file = path.findFile(getFileName());
                    if(file != null && file.delete())
                    {
                        if(recordingInfo.maxFileSize == -1)
                        {
                            releaseEncoders(context);
                            Toast.makeText(context, context.getString(R.string.failed_get_external_storage), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        else
                        {
                            releaseEncoders(context);
                            Toast.makeText(context, context.getString(R.string.little_memory_available), Toast.LENGTH_SHORT).show();
                            return false;
                        }
                    }
                }

                if(recordingInfo.maxFileSize == -1)
                {
                    releaseEncoders(context);
                    Toast.makeText(context, context.getString(R.string.failed_get_external_storage), Toast.LENGTH_SHORT).show();
                    return false;
                }
                else
                {
                    releaseEncoders(context);
                    Toast.makeText(context, context.getString(R.string.little_memory_available), Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        if(recordingInfo.recordMic)
        {
            mMediaRecorder.setAudioSamplingRate(recordingInfo.sampleRateAudio);
            mMediaRecorder.setAudioChannels(recordingInfo.audioChannel);
            mMediaRecorder.setAudioEncodingBitRate(recordingInfo.bitRateAudio);
        }

        mMediaRecorder.setVideoSize(recordingInfo.width, recordingInfo.height);

        if(isInternal)
            mMediaRecorder.setOutputFile(getFilePath());
        else
            mMediaRecorder.setOutputFile(fileDescriptor);

        try
        {
            mMediaRecorder.prepare();
            setFilePathTemp(getFilePath());
            return true;
        }
        catch (final IOException e) {
            e.printStackTrace();
            releaseEncoders(context);
            undoFile(context);

            //-------------------------REPORT
            if (!Settings.canDrawOverlays(context.getApplicationContext()))
                Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
            else
                createPopUpReport(context, context.getString(R.string.failed_prepare_recorder), e.toString(), recordingInfo);

            return false;
        }
        catch(final IllegalStateException i)
        {
            i.printStackTrace();
            releaseEncoders(context);
            undoFile(context);
            createPopUpReport(context, context.getString(R.string.failed_report_message), i.toString(), recordingInfo);
            return false;
        }
    }
    private void undoFile(Context context)
    {
        if(isInternal)
        {
            File file = new File(getFilePath());
            if(file.exists() && file.delete())
                setFilePathTemp(null);
            else
                setFilePathTemp(null);
        }
        else
        {
            DocumentFile path = DocumentFile.fromTreeUri(context, Uri.parse(mSecurityPreferences.getSetting(Constants.RECORD_PATH)));
            if(path != null)
            {
                DocumentFile file = path.findFile(getFileName());
                if(file != null && file.delete())
                    setFilePathTemp(null);
                else
                    setFilePathTemp(null);
            }
        }
    }
    private void updateGallery() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DATA, getFilePath());
        ContentResolver resolver = getApplicationContext().getContentResolver();
        resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }
    private void updateDeleteGallery() {
        if(isInternal)
        {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.fromFile(new File(getFilePath())));
            getApplicationContext().sendBroadcast(scanIntent);
        }
        else
        {
            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            scanIntent.setData(Uri.parse(getFilePathUri()));
            getApplicationContext().sendBroadcast(scanIntent);
        }
    }
    private void createPopUpReport(final Context context, String text, String logMessage, final RecordingInfo recordingInfo)
    {
        File folder = new File(Environment.getExternalStorageDirectory() + File.separator + "logs");
        String path;
        boolean success = true;
        if (!folder.exists())
            success = folder.mkdir();

        if (success)
            path = folder.getAbsolutePath();
        else
            path = Environment.getExternalStorageDirectory().getAbsolutePath();

        final File logFile = new File(path,
                "log-" + getCurSysDate()+ ".txt");
        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + logFile.getAbsolutePath());
        } catch (IOException e) {
            Toast.makeText(context, getString(R.string.failed_get_log), Toast.LENGTH_SHORT).show();
        }

        final AlertDialog errorDialog = new AlertDialog.Builder(context).create();
        errorDialog.setTitle("Ops!");
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Objects.requireNonNull(errorDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        else
            Objects.requireNonNull(errorDialog.getWindow()).setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        errorDialog.setMessage(text + logMessage);
        errorDialog.setIcon(R.drawable.ic_error);
        errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.text_send),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                        emailIntent.setData(Uri.parse("mailto:98appshelp@gmail.com"));
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Report][Screen Recorder Edge]");
                        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
                        if(recordingInfo != null)
                        {
                            emailIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.dont_remove_information_text) + "\n" + "--------------------------\n"
                                    + Build.MANUFACTURER.toUpperCase() + "\n" + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE + "\n" + "App Version: " + BuildConfig.VERSION_NAME + "\n\n" +
                                    "Config ---------------------------\n" + "Resolution: "+ recordingInfo.width +"x"+recordingInfo.height + "\n"+ "Audio Source: "+ recordingInfo.audioSource + "\n" + "Max File Size: "+ recordingInfo.maxFileSize + "\n" + "Video Encoder: "+recordingInfo.encoder +
                                    "\n" + "Audio Encoder: "+recordingInfo.audioEncoder + "\n" + "Audio Channel: "+ recordingInfo.audioChannel + "\n" +"Video Bitrate: "+ recordingInfo.bitrate + "\n" + "Audio Bitrate: " +recordingInfo.bitRateAudio + "\n" + "Screen Density: "+recordingInfo.density + "\n"
                                    + "Record Mic: " + recordingInfo.recordMic +"\n" + "Audio Sample Rate: " + recordingInfo.sampleRateAudio + "\n\n" + mSecurityPreferences.getSetting(Constants.RECORD_PATH) + "\n\n");
                        }
                        else
                        {
                            emailIntent.putExtra(Intent.EXTRA_TEXT, context.getResources().getString(R.string.dont_remove_information_text) + "\n" + "--------------------------\n"
                                    + Build.MANUFACTURER.toUpperCase() + "\n" + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE + "\n" + "App Version: " + BuildConfig.VERSION_NAME + "\n\n");
                        }

                        ComponentName emailApp = emailIntent.resolveActivity(context.getPackageManager());
                        ComponentName unsupportedAction = ComponentName.unflattenFromString("com.android.fallback/.Fallback");
                        if (emailApp != null && !emailApp.equals(unsupportedAction))
                        {
                            try
                            {
                                context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.send_email)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                            }
                            catch (ActivityNotFoundException i) {
                                Toast.makeText(context,context.getString(R.string.report_bug_error), Toast.LENGTH_LONG).show();

                            }
                        }
                        else
                        {
                            Toast.makeText(context, context.getString(R.string.report_bug_error), Toast.LENGTH_LONG).show();
                        }
                    }
                });
        errorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                errorDialog.dismiss();
            }
        });
        errorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                errorDialog.dismiss();
            }
        });
        errorDialog.show();
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
                        Intent videoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getFilePath()));
                        videoIntent.setDataAndType(Uri.parse(getFilePath()), "video/mp4");
                        videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(videoIntent);

                    }
                });
        actionDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.text_share_caps),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setDataAndType(Uri.fromFile(new File(getFilePath())), "video/mp4");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(getFilePath())));
                        context.startActivity(Intent.createChooser(shareIntent, getApplicationContext().getString(R.string.share_with)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    }
                });
        actionDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.text_delete_caps),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        if(isInternal)
                        {
                            File file = new File(getFilePath());
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
                                DocumentFile file = path.findFile(getFileName());
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

    private boolean checkMinAvailable(long size) {
        double m = ((size / 1024.0) / 1024.0);

        if (m > 1) {
            DecimalFormat dec = new DecimalFormat("#.#");
            return Integer.parseInt(dec.format(m).substring(0, 1)) >= 1;//1MB
        }
        return false;
    }
    private boolean checkSize(long size) {
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);

        if (g > 1) {
            DecimalFormat dec = new DecimalFormat("#.#");
            return Integer.parseInt(dec.format(g).substring(0, 1)) < 4;//4GB
        }
        else return m > 1;
    }
    private void releaseEncoders(Context context)
    {
        StandOutWindow.close(context.getApplicationContext(), FrameRateWindow.class, StandOutWindow.DEFAULT_ID);

        if(mMediaRecorder!= null)
        {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mProjectionManager != null)
            mProjectionManager = null;

        setRecording(false);
        panel.updateButtonRec(context, isRecording());
    }
    private RecordingInfo getRecordingInfo() {
        return prepareRecordingInfo(Constants.WIDTH, Constants.HEIGHT, Constants.DENSITY);
    }
    private RecordingInfo prepareRecordingInfo(int displayWidth, int displayHeight, int displayDensity) {

        final int H264 = MediaRecorder.VideoEncoder.H264;
        final int MPEG4 = MediaRecorder.VideoEncoder.MPEG_4_SP;
        final int HEVC = MediaRecorder.VideoEncoder.HEVC;

        boolean isLandscape = Constants.LANDSCAPE;
        int bitRate = Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_QUALITY)) * 1000;

        if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 0)
            mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "3");
        else if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 1)
            mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "5");

        int encoder = Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER));

        int resolution = Integer.parseInt(mSecurityPreferences.getSetting(encoder == MPEG4 ? Constants.MPEG_RESOLUTION : Constants.HEVC_RESOLUTION));
        int profileQuality = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_QUALITY));
        boolean recordMic = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.RECORD_MIC));
        int audioChannel = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_CHANNEL));
        int audioSource = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_SOURCE));
        int audioEncoder = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_ENCODER));
        int sampleRateAudio = getSampleRateAudio(profileQuality);
        int bitRateAudio = getBitRateAudio(profileQuality);
        long maxFileSize = getTotalExternalMemorySize();

        float aspectRatio = Constants.ASPECT_RATIO;

        final int newHeight;
        final int newWidth;
        if(encoder == HEVC || encoder == H264)
        {
            if(isS10() || isNote10())//S10 Variants & Note 10 Variants
            {
                if(resolution == 480)
                {
                    newHeight = (int) (aspectRatio * resolution)-3;
                    newWidth = resolution;
                }
                else
                {
                    newHeight = resolution == 0 ? displayHeight : (int) (aspectRatio * resolution);
                    newWidth = resolution == 0 ? displayWidth : resolution;
                }
            }
            else
            {
                newHeight = resolution == 0 ? displayHeight : (int) (aspectRatio * resolution);
                newWidth = resolution == 0 ? displayWidth : resolution;
            }
        }
        else
        {
            switch (resolution)
            {
                case 960:
                case 1080:
                    newHeight = 1920;
                    newWidth = resolution;
                    break;
                default:
                    if(isS10() || isNote10())
                    {
                        if(resolution == 480)
                        {
                            newHeight = (int) (aspectRatio * resolution)-3;
                            newWidth = resolution;
                        }
                        else
                        {
                            newHeight = (int) (aspectRatio * resolution);
                            newWidth = resolution;
                        }
                    }
                    else
                    {
                        newHeight = (int) (aspectRatio * resolution);
                        newWidth = resolution;
                    }
            }

        }

        if(bitRate == 0)
            bitRate = getBitRateVideo(encoder == MPEG4, resolution, newWidth);

        if (isLandscape) {
            int w, h;
            w = newHeight;
            h = newWidth;
            return new RecordingInfo(w, h, bitRate,
                    displayDensity, bitRateAudio, sampleRateAudio,
                    recordMic, maxFileSize, audioChannel,
                    encoder, audioSource, audioEncoder);
        } else {
            return new RecordingInfo(newWidth, newHeight, bitRate,
                    displayDensity, bitRateAudio, sampleRateAudio,
                    recordMic, maxFileSize, audioChannel,
                    encoder, audioSource, audioEncoder);
        }
    }

    private class RecordingInfo {
        final int width;
        final int height;
        final int bitrate;
        final int density;
        final int bitRateAudio;
        final int sampleRateAudio;
        final int audioChannel;
        final int audioSource;
        final int encoder;
        final int audioEncoder;
        final boolean recordMic;
        final long maxFileSize;


        RecordingInfo(int width, int height, int bitrate,
                      int density, int bitRateAudio, int sampleRateAudio,
                      boolean recordMic, long maxFileSize, int audioChannel,
                      int encoder, int audioSource, int audioEncoder) {
            this.width = width;
            this.height = height;
            this.bitrate = bitrate;
            this.density = density;
            this.bitRateAudio = bitRateAudio;
            this.sampleRateAudio = sampleRateAudio;
            this.recordMic = recordMic;
            this.maxFileSize = maxFileSize;
            this.audioChannel = audioChannel;
            this.encoder = encoder;
            this.audioSource = audioSource;
            this.audioEncoder = audioEncoder;
        }
    }
    private int getBitRateVideo(boolean isMpeg, int resolution, int newWidth)
    {
        if(isMpeg)
        {
            switch (resolution)
            {
                case 1080:
                    return 16384 * 1000;
                case 960:
                    return 14336 * 1000;
                case 900:
                    return 12228 * 1000;
                case 720:
                    return 10240 * 1000;
                case 540:
                    return 8192 * 1000;
                case 480:
                    return 7168 * 1000;
                default://360
                    return 6144 * 1000;
            }
        }
        else
        {
            switch (resolution)
            {
                case 1440:
                    return 9216 * 1000;
                case 1080:
                    return 6144 * 1000;
                case 720:
                    return 4096 * 1000;
                case 540:
                    return 3072 * 1000;
                case 480:
                    return 2048 * 1000;
                case 360:
                    return 1536 * 1000;
                default:
                    switch (newWidth)
                    {
                        case 1080:
                            return 6144 * 1000;
                        case 720:
                            return 4096 * 1000;
                        default:
                            return 9216 * 1000;
                    }
            }
        }
    }
    private int getSampleRateAudio(int profile) {
        switch (profile)
        {
            case 0:
            case 1:
                return 48000;
            default:
                return 44100;
        }
    }
    private int getBitRateAudio(int profile) {
        switch (profile)
        {
            case 0:
                return 512000;
            case 1:
                return 256000;
            case 2:
                return 128000;
            default:
                return 64000;
        }
    }
    private long getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                return Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE)) ? new File(Environment.getExternalStorageDirectory().toString()).getFreeSpace() : new File(mSecurityPreferences.getSetting(Constants.READABLE_PATH)).getFreeSpace();
            else
            {
                File path = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE)) ? Environment.getExternalStorageDirectory() : new File(mSecurityPreferences.getSetting(Constants.READABLE_PATH));
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSizeLong();
                long totalBlocks = stat.getBlockCountLong();
                return totalBlocks * blockSize;
            }

        } else {
            return -1;
        }
    }
    private boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }
    private boolean isS10()
    {
        return Build.MODEL.contains("G970") || Build.MODEL.contains("G973") || Build.MODEL.contains("G975") || Build.MODEL.contains("G977");//S10 Variants EDGE
    }
    private boolean isNote10()
    {
        return Build.MODEL.contains("N970") || Build.MODEL.contains("N971") || Build.MODEL.contains("N975") || Build.MODEL.contains("N976");//Note 10 Variants
    }
    private String getCurSysDate() {
        return new SimpleDateFormat(mSecurityPreferences.getSetting(Constants.FILE_FORMAT), Locale.US).format(new Date());
    }
    private VirtualDisplay getVirtualDisplay() {
        if(recordingInfo == null)
            recordingInfo = getRecordingInfo();

        return mMediaProjection.createVirtualDisplay(getClass().getSimpleName(),
                recordingInfo.width, recordingInfo.height, recordingInfo.density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
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
                        Intent videoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getFilePath()));
                        videoIntent.setDataAndType(Uri.parse(getFilePath()), "video/mp4");
                        videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(videoIntent);
                        stopForeground(true);
                        break;
                    case ACTION_SHARE:
                        context.sendBroadcast(it);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND, Uri.fromFile(new File(getFilePath())));
                        shareIntent.setType("video/mp4");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(getFilePath())));
                        context.startActivity(Intent.createChooser(shareIntent, getApplicationContext().getString(R.string.share_with)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        stopForeground(true);
                        break;
                    case ACTION_DELETE:
                        context.sendBroadcast(it);
                        stopForeground(true);
                        if(isInternal)
                        {
                            File file = new File(getFilePath());
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
                                DocumentFile file = path.findFile(getFileName());
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
