package a98apps.recorderedge.edge;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.util.RecordService;
import a98apps.recorderedge.util.RequestPermission;
import a98apps.recorderedge.util.SecurityPreferences;
import a98apps.recorderedge.view.ListVideos;
import a98apps.recorderedge.view.SettingsActivity;

import static a98apps.recorderedge.constants.Constants.ACTION_STOP_REC;
import static a98apps.recorderedge.constants.Constants.ACTION_REMOTE_CLICK;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_CHANNEL_ID_FINISHED;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_CHANNEL_ID_RECORDING;

public class CocktailScreenRecorder extends SlookCocktailProvider {

    private static RemoteViews mClickStateView = null;

    public static MediaProjection mMediaProjection;
    public static MediaProjectionManager mProjectionManager;
    private SecurityPreferences mSecurityPreferences;

    @Override
    public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds)
    {
        if(mSecurityPreferences == null) {
            mSecurityPreferences = new SecurityPreferences(context);
            mSecurityPreferences.checkExist(context);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_FINISHED) == null || manager.getNotificationChannel(NOTIFICATION_CHANNEL_ID_RECORDING) == null) {
                NotificationChannel channelRecording = new NotificationChannel(NOTIFICATION_CHANNEL_ID_RECORDING, context.getApplicationContext().getString(R.string.record_notification), NotificationManager.IMPORTANCE_LOW);
                NotificationChannel channelFinished = new NotificationChannel(NOTIFICATION_CHANNEL_ID_FINISHED, context.getApplicationContext().getString(R.string.finish_notification), NotificationManager.IMPORTANCE_HIGH);
                channelFinished.setDescription(context.getApplicationContext().getString(R.string.description_notification_finish));
                channelRecording.setDescription(context.getApplicationContext().getString(R.string.description_notification_recording));
                manager.createNotificationChannel(channelRecording);
                manager.createNotificationChannel(channelFinished);
            }
        }

        if(mClickStateView == null) {
            mClickStateView = createStateView(context);
        }
        cocktailManager.updateCocktail(cocktailIds[0], mClickStateView);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REMOTE_CLICK.equals(action))
            performClick(context, intent);
    }

    private RemoteViews createStateView(Context context) {
        RemoteViews stateView = new RemoteViews(context.getPackageName(),
                R.layout.cocktail_layout);

        stateView.setOnClickPendingIntent(R.id.button_rec, getClickIntent(context, R.id.button_rec));
        stateView.setOnClickPendingIntent(R.id.button_settings, getClickIntent(context, R.id.button_settings));
        stateView.setOnClickPendingIntent(R.id.button_list, getClickIntent(context, R.id.button_list));
        stateView.setOnClickPendingIntent(R.id.button_mic, getClickIntent(context, R.id.button_mic));

        if(mSecurityPreferences == null)
            mSecurityPreferences = new SecurityPreferences(context);

        if(Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.SHOW_BUTTON_MIC)))
            stateView.setViewVisibility(R.id.button_mic, View.VISIBLE);

        if(Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.SHOW_BUTTON_VIDEOS)))
            stateView.setViewVisibility(R.id.button_list, View.VISIBLE);

        return stateView;
    }
    private PendingIntent getClickIntent(Context context, int id) {
        Intent clickIntent = new Intent(context, CocktailScreenRecorder.class);
        clickIntent.setAction(ACTION_REMOTE_CLICK);
        clickIntent.putExtra("id", id);
        return PendingIntent.getBroadcast(context, id, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private void performClick(Context context, Intent intent)
    {
        int id = intent.getIntExtra("id", -1);
        switch (id) {
            case R.id.button_rec:
                if (mMediaProjection == null)
                {
                    context.sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
                    Intent permissionIntent = new Intent(context, RequestPermission.class);
                    permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(permissionIntent);
                    return;
                }
                context.startService(new Intent(context, RecordService.class).setAction(ACTION_STOP_REC));
                break;
            case R.id.button_settings:
                context.startActivity(new Intent(context, SettingsActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case R.id.button_list:
                context.startActivity(new Intent(context, ListVideos.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case R.id.button_mic:
                if (mMediaProjection != null)
                {
                    Toast.makeText(context, context.getString(R.string.warning_mic_while_recording), Toast.LENGTH_LONG).show();
                    return;
                }
                if(mSecurityPreferences == null)
                    mSecurityPreferences = new SecurityPreferences(context);

                boolean record = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.RECORD_MIC));

                if (record)
                    mSecurityPreferences.saveSetting(Constants.RECORD_MIC, String.valueOf(false));
                else
                    mSecurityPreferences.saveSetting(Constants.RECORD_MIC, String.valueOf(true));

                updateButtonMic(context, !record);
                break;
            default:
                break;

        }

    }
    public void updateButtonRec(Context context, boolean isRecording)
    {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, CocktailScreenRecorder.class));
        if(mClickStateView == null)
            mClickStateView = createStateView(context);

        if (isRecording)
            mClickStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_stop);
        else
            mClickStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_rec);

        cocktailManager.updateCocktail(cocktailIds[0], mClickStateView);
    }
    public void updateButtonMic(Context context, boolean record)
    {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, CocktailScreenRecorder.class));
        if(mClickStateView == null)
            mClickStateView = createStateView(context);

        if (record)
            mClickStateView.setImageViewResource(R.id.button_mic, R.drawable.ic_mic_on);
        else
            mClickStateView.setImageViewResource(R.id.button_mic, R.drawable.ic_mic_off);

        cocktailManager.updateCocktail(cocktailIds[0], mClickStateView);
    }
    public void updateButtons(Context context, int id, boolean show)
    {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, CocktailScreenRecorder.class));
        if(mClickStateView == null)
            mClickStateView = createStateView(context);

        if (show)
            mClickStateView.setViewVisibility(id, View.VISIBLE);
        else
            mClickStateView.setViewVisibility(id, View.GONE);

        cocktailManager.updateCocktail(cocktailIds[0], mClickStateView);
    }
}
