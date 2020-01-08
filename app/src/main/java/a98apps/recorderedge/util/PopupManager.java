package a98apps.recorderedge.util;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import a98apps.recorderedge.BuildConfig;
import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.permission.RequestPermission;
import a98apps.recorderedge.record.RecorderConfig;

public class PopupManager {

    private final Context context;
    private final SecurityPreferences mSecurityPreferences;

    public PopupManager(Context context, SecurityPreferences mSecurityPreferences)
    {
        this.context = context;
        this.mSecurityPreferences = mSecurityPreferences;
    }

    public void createPopupReport(String text, String logMessage, final RecorderConfig recorderConfig)
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
        }
        catch (IOException e) {
            Toast.makeText(context, context.getString(R.string.failed_get_log), Toast.LENGTH_SHORT).show();
        }

        final AlertDialog errorDialog = new AlertDialog.Builder(context).create();
        errorDialog.setTitle("Ops!");

        Objects.requireNonNull(errorDialog.getWindow()).setType(checkTypePopup());

        errorDialog.setMessage(text + logMessage);
        errorDialog.setIcon(R.drawable.ic_error);
        errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.text_send),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {

                        Intent emailIntent = new Intent(Intent.ACTION_VIEW);
                        String subject = "[Report][Screen Recorder Edge]";
                        String text;

                        if(recorderConfig != null)
                        {
                            text = context.getResources().getString(R.string.dont_remove_information_text) + "<br>" + "--------------------------<br>"
                                    + Build.MANUFACTURER.toUpperCase() + "<br>" + Build.MODEL + "<br>Android: " + Build.VERSION.RELEASE + "<br>" + "App Version: " + BuildConfig.VERSION_NAME + "<br><br>" +
                                    "Config<br>---------------------------<br>" + "Resolution: "+ recorderConfig.width +"x"+ recorderConfig.height + "<br>"+ "Audio Source: "+ recorderConfig.audioSource + "<br>" + "Max File Size: "+ recorderConfig.maxFileSize + "<br>" + "Video Encoder: "+ recorderConfig.encoder +
                                    "<br>" + "Audio Encoder: "+ recorderConfig.audioEncoder + "<br>" + "Audio Channel: "+ recorderConfig.audioChannel + "<br>" +"Video Bitrate: "+ recorderConfig.bitrate + "<br>" + "Audio Bitrate: " + recorderConfig.bitRateAudio + "<br>" + "Screen Density: "+ recorderConfig.density + "<br>"
                                    + "Record Mic: " + recorderConfig.recordMic +"<br>" + "Audio Sample Rate: " + recorderConfig.sampleRateAudio + "<br><br>" + mSecurityPreferences.getSetting(Constants.RECORD_PATH) + "<br><br>";
                        }
                        else
                        {
                            text = context.getResources().getString(R.string.dont_remove_information_text) + "<br>" + "--------------------------<br>"
                                    + Build.MANUFACTURER.toUpperCase() + "<br>" + Build.MODEL + "<br>Android: " + Build.VERSION.RELEASE + "<br>" + "App Version: " + BuildConfig.VERSION_NAME + "<br><br>";
                        }

                        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"98appshelp@gmail.com"});
                        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));

                        Uri data = Uri.parse("mailto:98appshelp@gmail.com" + "?subject=" + subject + "&body=" + text);
                        emailIntent.setData(data);

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
        errorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
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

    public void createPopupMessage(String message, String title)
    {
        final AlertDialog errorDialog = new AlertDialog.Builder(context.getApplicationContext().getApplicationContext()).create();
        errorDialog.setTitle(title);

        Objects.requireNonNull(errorDialog.getWindow()).setType(checkTypePopup());

        errorDialog.setMessage(message);
        errorDialog.setIcon(R.drawable.ic_info);
        errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        errorDialog.dismiss();
                    }
                });
        errorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
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
    public void createPopupSelectPath(String message)
    {
        final AlertDialog errorDialog = new AlertDialog.Builder(context.getApplicationContext().getApplicationContext()).create();
        errorDialog.setTitle("Ops!");

        Objects.requireNonNull(errorDialog.getWindow()).setType(checkTypePopup());

        errorDialog.setMessage(message);
        errorDialog.setIcon(R.drawable.ic_error);
        errorDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.text_select),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Intent pathIntent = new Intent(context.getApplicationContext(), SelectPath.class);
                        pathIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        pathIntent.putExtra(Constants.INTENT_GET_ACTIVITY, RequestPermission.class.getName());
                        context.getApplicationContext().startActivity(pathIntent);
                    }
                });
        errorDialog.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(android.R.string.cancel), new DialogInterface.OnClickListener() {
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
    private int checkTypePopup()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
    }

    private String getCurSysDate() {
        return new SimpleDateFormat(mSecurityPreferences.getSetting(Constants.FILE_FORMAT), Locale.US).format(new Date());
    }
}
