package a98apps.recorderedge.edge;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;

import a98apps.recorderedge.R;
import a98apps.recorderedge.record.RecordService;
import a98apps.recorderedge.permission.RequestPermission;
import a98apps.recorderedge.record.RecorderController;
import a98apps.recorderedge.util.SecurityPreferences;
import a98apps.recorderedge.view.DonateActivity;
import a98apps.recorderedge.view.HelpActivity;
import a98apps.recorderedge.view.ListVideos;
import a98apps.recorderedge.view.SettingsActivity;

import static a98apps.recorderedge.constants.Constants.ACTION_STOP_REC;
import static a98apps.recorderedge.constants.Constants.ACTION_REMOTE_CLICK;
import static a98apps.recorderedge.constants.Constants.BUTTON_REC_COLOR;
import static a98apps.recorderedge.constants.Constants.LIST_ICON_COLOR;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_CHANNEL_ID_FINISHED;
import static a98apps.recorderedge.constants.Constants.NOTIFICATION_CHANNEL_ID_RECORDING;
import static a98apps.recorderedge.constants.Constants.PANEL_COLOR;
import static a98apps.recorderedge.constants.Constants.RECORD_MIC;
import static a98apps.recorderedge.constants.Constants.SETTINGS_ICON_COLOR;
import static a98apps.recorderedge.record.RecorderController.mMediaProjection;

public class CocktailScreenRecorder extends SlookCocktailProvider {

    private static RemoteViews mStateView = null;
    private static RemoteViews mAreaStateView = null;

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

        initDecorations(context, mSecurityPreferences);
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

        return stateView;
    }
    private RemoteViews createAreaStateView(Context context) {
        RemoteViews stateView = new RemoteViews(context.getPackageName(),
                R.layout.cocktail_layout_area);

        stateView.setOnClickPendingIntent(R.id.button_rate, getClickIntent(context, R.id.button_rate));
        stateView.setOnClickPendingIntent(R.id.button_settings, getClickIntent(context, R.id.button_settings));
        stateView.setOnClickPendingIntent(R.id.button_help, getClickIntent(context, R.id.button_help));
        stateView.setOnClickPendingIntent(R.id.button_donate, getClickIntent(context, R.id.button_donate));
        stateView.setOnClickPendingIntent(R.id.button_mic, getClickIntent(context, R.id.button_mic));

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
        switch (id)
        {
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
            case R.id.button_help:
                context.startActivity(new Intent(context, HelpActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case R.id.button_donate:
                context.startActivity(new Intent(context, DonateActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case R.id.button_mic:
                if (mMediaProjection != null)
                {
                    Toast.makeText(context, context.getString(R.string.warning_mic_while_recording), Toast.LENGTH_LONG).show();
                    return;
                }

                checkPanel(context);

                if(mSecurityPreferences == null)
                    mSecurityPreferences = new SecurityPreferences(context);

                toggleMicrophone(context, Boolean.parseBoolean(mSecurityPreferences.getSetting(RECORD_MIC)), mSecurityPreferences);

                updatePanel(context);
                break;
            case R.id.button_rate:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(context.getString(R.string.url_rate)));
                context.startActivity(i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            default:
                break;

        }

    }
    public void updateButtonRec(Context context, boolean isRecording)
    {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, CocktailScreenRecorder.class));
        if(mStateView == null)
            mStateView = createStateView(context);

        checkButtonColor(context, isRecording);

        cocktailManager.updateCocktail(cocktailIds[0], mStateView);
    }

    private void checkPanel(Context context)
    {
        if(mStateView == null || mAreaStateView == null) {

            SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
            int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, CocktailScreenRecorder.class));

            if(mStateView == null)
                mStateView = createStateView(context);

            if(mAreaStateView == null)
                mAreaStateView = createAreaStateView(context);

            cocktailManager.updateCocktail(cocktailIds[0], mStateView, mAreaStateView);
        }
    }
    private void updatePanel(Context context)
    {
        SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
        int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, CocktailScreenRecorder.class));

        if(mStateView == null)
            mStateView = createStateView(context);

        if(mAreaStateView == null)
            mAreaStateView = createAreaStateView(context);

        cocktailManager.updateCocktail(cocktailIds[0], mStateView, mAreaStateView);
    }

    public void initDecorations(Context context, SecurityPreferences mSecurityPreferences)
    {
        checkPanel(context);

        mStateView.setInt(R.id.panel, "setBackgroundColor", Integer.parseInt(mSecurityPreferences.getSetting(PANEL_COLOR)));
        mStateView.setInt(R.id.button_settings, "setColorFilter", Integer.parseInt(mSecurityPreferences.getSetting(SETTINGS_ICON_COLOR)));
        mStateView.setInt(R.id.button_list, "setColorFilter", Integer.parseInt(mSecurityPreferences.getSetting(LIST_ICON_COLOR)));

        checkButtonColor(context, RecorderController.getFilePathTemp() != null);

        toggleMicrophone(context, false, mSecurityPreferences);

        updatePanel(context);
    }

    public void setDecorations(Context context, int id, int color)
    {
        checkPanel(context);

        if(id == R.id.panel)
            mStateView.setInt(id, "setBackgroundColor", color);
        else if(id == R.id.button_rec)
            checkButtonColor(context, RecorderController.getFilePathTemp() != null, color);
        else
            mStateView.setInt(id, "setColorFilter", color);

        updatePanel(context);

    }

    private void toggleMicrophone(Context context, boolean record, SecurityPreferences mSecurityPreferences)
    {

        if (record)
        {
            mSecurityPreferences.saveSetting(RECORD_MIC, String.valueOf(false));

            mAreaStateView.setTextViewCompoundDrawables(R.id.button_mic, R.drawable.ic_mic_off, 0, 0, 0);
            mAreaStateView.setTextViewText(R.id.button_mic,context.getString(R.string.title_mic_off));
        }
        else
        {
            mSecurityPreferences.saveSetting(RECORD_MIC, String.valueOf(true));

            mAreaStateView.setTextViewCompoundDrawables(R.id.button_mic, R.drawable.ic_mic_on, 0, 0, 0);
            mAreaStateView.setTextViewText(R.id.button_mic,context.getString(R.string.title_mic_on));
        }
    }


    private void checkButtonColor(Context context, boolean isRecording)
    {
        if(mSecurityPreferences == null)
            mSecurityPreferences = new SecurityPreferences(context);

        if (isRecording)
        {
            if(Integer.parseInt(mSecurityPreferences.getSetting(BUTTON_REC_COLOR)) == 0)
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_stop);
            else
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_stop_dark);

        }
        else
        {
            if(Integer.parseInt(mSecurityPreferences.getSetting(BUTTON_REC_COLOR)) == 0)
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_rec);
            else
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_rec_dark);
        }
    }

    private void checkButtonColor(Context context, boolean isRecording, int mode)
    {
        if(mSecurityPreferences == null)
            mSecurityPreferences = new SecurityPreferences(context);

        if (isRecording)
        {
            if(mode == 0)
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_stop);
            else
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_stop_dark);

        }
        else
        {
            if(mode == 0)
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_rec);
            else
                mStateView.setImageViewResource(R.id.button_rec, R.mipmap.ic_button_rec_dark);
        }
    }
}
