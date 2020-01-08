package a98apps.recorderedge.view;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.jaredrummler.android.colorpicker.ColorPreferenceCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import a98apps.recorderedge.BuildConfig;
import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.edge.CocktailScreenRecorder;
import a98apps.recorderedge.util.SecurityPreferences;
import a98apps.recorderedge.util.SelectPath;
import a98apps.recorderedge.util.ThemeMode;

import static a98apps.recorderedge.constants.Constants.APP_THEME;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeMode.checkTheme(this, false, true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_settings);

        getSupportActionBar().setTitle(R.string.title_settings_activity);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private final ViewHolder mViewHolder = new ViewHolder();
        private SecurityPreferences mSecurityPreferences;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

            mSecurityPreferences = new SecurityPreferences(getActivity());

            if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 0)
                mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "3");
            else if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 1)
                mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "5");

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            mViewHolder.hevc = findPreference(getString(R.string.key_hevc_resolution));
            mViewHolder.mpeg = findPreference(getString(R.string.key_mpeg_resolution));
            mViewHolder.categoryVideo = findPreference(getString(R.string.key_category_video));

            PreferencesListener listener = new PreferencesListener(mViewHolder, mSecurityPreferences, getActivity());

            Preference hide = findPreference(getString(R.string.key_hide_icon));
            assert hide != null;
            hide.setOnPreferenceChangeListener(listener);

            Preference panelColor = findPreference(getString(R.string.key_panel_color));
            assert panelColor != null;
            panelColor.setOnPreferenceChangeListener(listener);

            Preference listIconColor = findPreference(getString(R.string.key_list_icon_color));
            assert listIconColor != null;
            listIconColor.setOnPreferenceChangeListener(listener);

            Preference settingsIconColor = findPreference(getString(R.string.key_settings_icon_color));
            assert settingsIconColor != null;
            settingsIconColor.setOnPreferenceChangeListener(listener);

            Preference buttonRecColor = findPreference(getString(R.string.key_button_rec_theme));
            assert buttonRecColor != null;
            buttonRecColor.setOnPreferenceChangeListener(listener);

            Preference appTheme = findPreference(getString(R.string.key_app_theme));
            assert appTheme != null;
            appTheme.setOnPreferenceChangeListener(listener);

            // video quality preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_video_quality))), "0", listener);

            // video encoder preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_video_encoder))), "5", listener);

            // finish action preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_finish_action))),"2", listener);

            // audio channel preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_audio_channel))),"1", listener);

            // audio quality preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_audio_quality))),"1", listener);

            // video orientation preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_video_orientation))),"0", listener);

            // audio source preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_audio_source))),"1", listener);

            // record path preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_record_path))), Constants.DEFAULT_PATH, listener);

            // file format preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_file_format))),Constants.DEFAULT_FILE_FORMAT, listener);

            // file format preference change listener
            bindPreferenceSummaryToValue(Objects.requireNonNull(findPreference(getString(R.string.key_audio_encoder))),"3", listener);

            Preference notifications = findPreference(getString(R.string.key_recording_notification));
            assert notifications != null;
            notifications.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");

                    if(Build.VERSION.SDK_INT == Build.VERSION_CODES.N)
                    {
                        intent.putExtra("app_package", Objects.requireNonNull(getActivity()).getPackageName());
                        intent.putExtra("app_uid", getActivity().getApplicationInfo().uid);
                    }
                    else
                        intent.putExtra("android.provider.extra.APP_PACKAGE", Objects.requireNonNull(getActivity()).getPackageName());

                    startActivity(intent);
                    return true;
                }
            });

            // reset preference click listener
            Preference resetSettings = findPreference(getString(R.string.key_reset_config));
            assert resetSettings != null;
            resetSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    resetToDefault(Objects.requireNonNull(getActivity()), mSecurityPreferences);
                    return true;
                }
            });

            Preference report = findPreference(getString(R.string.key_report_bug));
            assert report != null;
            report.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (hasPermissions(getActivity()))
                        sendReport(getActivity());
                    else
                        requestWriteExternalPermission(getActivity());
                    return true;
                }
            });

            Preference selectPath = findPreference(getString(R.string.key_record_path));
            assert selectPath != null;
            selectPath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent pathIntent = new Intent(getActivity(), SelectPath.class);
                    pathIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    pathIntent.putExtra(Constants.INTENT_GET_ACTIVITY, SettingsActivity.class.getName());
                    Objects.requireNonNull(getActivity()).startActivity(pathIntent);
                    getActivity().finish();
                    return true;
                }
            });

            Preference version = findPreference(getString(R.string.key_version));
            assert version != null;
            version.setSummary(getString(R.string.text_version) +BuildConfig.VERSION_NAME + " by 98 Apps");
        }

        @Override
        public void setDivider(Drawable divider) {
            boolean isDarkMode = ThemeMode.isDarkMode(getContext(), Integer.parseInt(mSecurityPreferences.getSetting(APP_THEME)));
            ColorDrawable colorDrawable = new ColorDrawable(isDarkMode ? Objects.requireNonNull(getContext()).getColor(R.color.colorBorderDark) : Objects.requireNonNull(getContext()).getColor(R.color.colorBorderLight));
            super.setDivider(colorDrawable);
        }
        private void requestWriteExternalPermission(Activity activity) {
            if (ContextCompat.checkSelfPermission(activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                            1);

                }
                else {
                    ActivityCompat.requestPermissions(activity, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
                            1);

                }
            }
        }
        private boolean hasPermissions(Context context) {
            return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            if (requestCode == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), getResources().getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            }
            else
                sendReport(getContext());
        }
        private void sendReport(Context context)
        {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());

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
                    "log-" + new SimpleDateFormat(Constants.DEFAULT_FILE_FORMAT, Locale.US).format(new Date()) + ".txt");
            try {
                Runtime.getRuntime().exec(
                        "logcat -f " + logFile.getAbsolutePath());
            } catch (IOException e) {
                Toast.makeText(context.getApplicationContext(), context.getApplicationContext().getString(R.string.failed_get_log), Toast.LENGTH_SHORT).show();
            }

            Intent emailIntent = new Intent(Intent.ACTION_VIEW);
            String subject = "[Bug Report][Screen Recorder Edge]";
            String text = context.getApplicationContext().getResources().getString(R.string.please_write_details)
                    +"<br><br>--------------------------<br>"
                    + context.getApplicationContext().getResources().getString(R.string.dont_remove_information_text) + "<br>"
                    + Build.MANUFACTURER.toUpperCase() + "<br>"
                    + Build.MODEL + "<br>Android: "
                    + Build.VERSION.RELEASE + "<br>"
                    + "App Version: " + BuildConfig.VERSION_NAME;

            Uri data = Uri.parse("mailto:98appshelp@gmail.com" + "?subject=" + subject + "&body=" + text);
            emailIntent.setData(data);
            emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{"98appshelp@gmail.com"});
            emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));

            ComponentName emailApp = emailIntent.resolveActivity(context.getApplicationContext().getPackageManager());
            ComponentName unsupportedAction = ComponentName.unflattenFromString("com.android.fallback/.Fallback");
            if (emailApp != null && !emailApp.equals(unsupportedAction))
            {
                try
                {
                    context.getApplicationContext().startActivity(Intent.createChooser(emailIntent, context.getApplicationContext().getString(R.string.send_email)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
                catch (ActivityNotFoundException i) {
                    Toast.makeText(context.getApplicationContext(), context.getApplicationContext().getString(R.string.report_bug_error), Toast.LENGTH_LONG).show();
                }
            }
            else
            {
                Toast.makeText(context.getApplicationContext(), context.getApplicationContext().getString(R.string.report_bug_error), Toast.LENGTH_LONG).show();
            }
        }
        private void resetToDefault(Activity activity, SecurityPreferences securityPreferences) {
            ComponentName componentToEnable = new ComponentName(activity.getApplicationContext(), LauncherActivity.class);
            PackageManager pm = activity.getApplicationContext().getPackageManager();
            pm.setComponentEnabledSetting(componentToEnable, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

            securityPreferences.resetToDefault(activity);
            Toast.makeText(activity, activity.getString(R.string.text_settings_reseted), Toast.LENGTH_SHORT).show();
            activity.finish();
            activity.overridePendingTransition(0, 0);
            activity.startActivity(activity.getIntent());
            activity.overridePendingTransition(0, 0);

            CocktailScreenRecorder cocktailScreenRecorder = new CocktailScreenRecorder();
            cocktailScreenRecorder.initDecorations(getContext(), mSecurityPreferences);
        }
    }
    private static void bindPreferenceSummaryToValue(Preference preference, String defaultValue, PreferencesListener listener) {
        preference.setOnPreferenceChangeListener(listener);

        listener.onPreferenceChange(preference,
                Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), defaultValue)));
    }
    private static class PreferencesListener implements Preference.OnPreferenceChangeListener
    {
        private final ViewHolder mViewHolder;
        private final SecurityPreferences mSecurityPreferences;
        private final Activity activity;

        PreferencesListener(ViewHolder holder, SecurityPreferences preferences, Activity activity) {
            this.mViewHolder = holder;
            this.mSecurityPreferences = preferences;
            this.activity = activity;
        }
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String stringValue = newValue.toString();
            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);

                // Set the summary to reflect the new value.
                preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);

                switch (preference.getKey())
                {
                    case "key_video_encoder":
                        if (Integer.parseInt(stringValue) == 3)
                        {
                            mViewHolder.categoryVideo.addPreference(mViewHolder.mpeg);
                            mViewHolder.categoryVideo.removePreference(mViewHolder.hevc);
                            bindPreferenceSummaryToValue(mViewHolder.mpeg, "720", this);
                        }
                        else
                        {
                            mViewHolder.categoryVideo.removePreference(mViewHolder.mpeg);
                            mViewHolder.categoryVideo.addPreference(mViewHolder.hevc);
                            bindPreferenceSummaryToValue(mViewHolder.hevc, "0", this);
                        }
                        break;
                    case "key_button_rec_theme":
                        CocktailScreenRecorder cocktailScreenRecorder = new CocktailScreenRecorder();
                        cocktailScreenRecorder.setDecorations(preference.getContext(), R.id.button_rec, Integer.parseInt(stringValue));
                        break;
                    case "key_app_theme":
                        ThemeMode.checkTheme(activity, true, false);
                        break;
                }

            }
            else if(preference instanceof SwitchPreference)
            {
                if ("key_hide_icon".equals(preference.getKey()))
                {
                    if (Boolean.parseBoolean(stringValue))
                    {
                        ComponentName componentToEnable = new ComponentName(preference.getContext(), LauncherActivity.class);
                        PackageManager pm = preference.getContext().getPackageManager();
                        pm.setComponentEnabledSetting(componentToEnable, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        Toast.makeText(preference.getContext(), preference.getContext().getString(R.string.text_hiding), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        ComponentName componentToEnable = new ComponentName(preference.getContext(), LauncherActivity.class);
                        PackageManager pm = preference.getContext().getPackageManager();
                        pm.setComponentEnabledSetting(componentToEnable, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        Toast.makeText(preference.getContext(), preference.getContext().getString(R.string.text_enabling), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else if(preference instanceof ColorPreferenceCompat)
            {
                CocktailScreenRecorder cocktailScreenRecorder = new CocktailScreenRecorder();
                switch (preference.getKey())
                {
                    case "key_panel_color":
                    {
                        cocktailScreenRecorder.setDecorations(preference.getContext(), R.id.panel, (int) newValue);
                        break;
                    }
                    case "key_settings_icon_color":
                    {
                        cocktailScreenRecorder.setDecorations(preference.getContext(), R.id.button_settings, (int) newValue);
                        break;
                    }
                    case "key_list_icon_color":
                    {
                        cocktailScreenRecorder.setDecorations(preference.getContext(), R.id.button_list, (int) newValue);
                        break;
                    }
                }
            }
            else
            {
                if(preference.getKey().equals("key_record_path"))
                {
                    if(Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE)))
                        preference.setSummary(stringValue);
                    else
                        preference.setSummary(mSecurityPreferences.getSetting(Constants.READABLE_PATH));
                }
                else
                    preference.setSummary(stringValue);
            }

            return true;
        }

    }
    private static class ViewHolder
    {
        private Preference mpeg;
        private Preference hevc;
        private PreferenceCategory categoryVideo;
    }
}