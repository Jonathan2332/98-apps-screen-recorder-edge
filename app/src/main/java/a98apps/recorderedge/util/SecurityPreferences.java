package a98apps.recorderedge.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.R;

public class SecurityPreferences
{
    private final SharedPreferences mSharedPreferences;

    public SecurityPreferences(Context context)
    {
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    public String getSetting(String key)
    {
        switch (key)
        {
            case Constants.INITIALIZED:
                return String.valueOf(this.mSharedPreferences.getBoolean(key, false));
            case Constants.RECORD_MIC:
            case Constants.SHOW_RECORDING_NOTIFICATION:
            case Constants.INTERNAL_STORAGE:
                return String.valueOf(this.mSharedPreferences.getBoolean(key, true));
            case Constants.MPEG_RESOLUTION:
                return this.mSharedPreferences.getString(key, "720");
            case Constants.HEVC_RESOLUTION:
            case Constants.VIDEO_QUALITY:
            case Constants.VIDEO_ORIENTATION:
                return this.mSharedPreferences.getString(key, "0");
            case Constants.VIDEO_ENCODER:
                return this.mSharedPreferences.getString(key, "5");
            case Constants.FINISH_ACTION:
                return this.mSharedPreferences.getString(key, "2");
            case Constants.AUDIO_ENCODER:
                return this.mSharedPreferences.getString(key, "3");
            case Constants.AUDIO_CHANNEL:
            case Constants.AUDIO_QUALITY:
            case Constants.AUDIO_SOURCE:
                return this.mSharedPreferences.getString(key, "1");
            case Constants.RECORD_PATH:
                return this.mSharedPreferences.getString(key, Constants.DEFAULT_PATH);
            case Constants.FILE_FORMAT:
                return this.mSharedPreferences.getString(key, Constants.DEFAULT_FILE_FORMAT);
            case Constants.READABLE_PATH:
                return this.mSharedPreferences.getString(key, this.mSharedPreferences.getString(Constants.RECORD_PATH, Constants.DEFAULT_PATH));
            default:
                return this.mSharedPreferences.getString(key,"");
        }
    }
    public void saveSetting(String key, String value)
    {
        if(key.equals(Constants.INTERNAL_STORAGE))
            this.mSharedPreferences.edit().putBoolean(key, Boolean.parseBoolean(value)).apply();
        else
            this.mSharedPreferences.edit().putString(key, value).apply();
    }
    public void checkExist(Context context)
    {

        if(!mSharedPreferences.contains(Constants.INITIALIZED))
        {
            PreferenceManager.setDefaultValues(context, R.xml.pref_main, false);
            SharedPreferences.Editor ed;
            ed = mSharedPreferences.edit();
            ed.putBoolean(Constants.INITIALIZED, true);
            ed.putString(Constants.RECORD_PATH, Constants.DEFAULT_PATH);
            ed.putBoolean(Constants.INTERNAL_STORAGE, true);
            ed.putString(Constants.READABLE_PATH, Constants.DEFAULT_PATH);
            ed.apply();
        }
        else//Necessary to don't crash on start on new config added
        {
            SharedPreferences.Editor ed;
            if(!mSharedPreferences.contains(Constants.RECORD_MIC)) {
                ed = mSharedPreferences.edit();
                ed.putBoolean(Constants.RECORD_MIC, true);
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.VIDEO_QUALITY)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.VIDEO_QUALITY, "0");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.FINISH_ACTION)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.FINISH_ACTION, "2");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.SHOW_RECORDING_NOTIFICATION)) {
                ed = mSharedPreferences.edit();
                ed.putBoolean(Constants.SHOW_RECORDING_NOTIFICATION, true);
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.INTERNAL_STORAGE)) {
                ed = mSharedPreferences.edit();
                ed.putBoolean(Constants.INTERNAL_STORAGE, true);
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.AUDIO_QUALITY)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.AUDIO_QUALITY, "1");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.VIDEO_ORIENTATION)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.VIDEO_ORIENTATION, "0");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.HEVC_RESOLUTION)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.HEVC_RESOLUTION, "0");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.MPEG_RESOLUTION)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.MPEG_RESOLUTION, "720");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.AUDIO_CHANNEL)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.AUDIO_CHANNEL, "1");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.VIDEO_ENCODER)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.VIDEO_ENCODER, "5");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.AUDIO_SOURCE)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.AUDIO_SOURCE, "1");
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.RECORD_PATH)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.RECORD_PATH, Constants.DEFAULT_PATH);
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.READABLE_PATH)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.READABLE_PATH, this.mSharedPreferences.getString(Constants.RECORD_PATH, Constants.DEFAULT_PATH));
                ed.apply();
            }

            if(!mSharedPreferences.contains(Constants.FILE_FORMAT)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.FILE_FORMAT, Constants.DEFAULT_FILE_FORMAT);
                ed.apply();
            }
            if(!mSharedPreferences.contains(Constants.AUDIO_ENCODER)) {
                ed = mSharedPreferences.edit();
                ed.putString(Constants.AUDIO_ENCODER, "3");
                ed.apply();
            }
        }
    }
    public void resetToDefault(Context context)
    {
        this.mSharedPreferences.edit().clear().apply();
        checkExist(context);
    }
}
