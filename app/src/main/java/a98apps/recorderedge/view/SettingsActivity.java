package a98apps.recorderedge.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import a98apps.recorderedge.util.SecurityPreferences;
import a98apps.recorderedge.util.SelectPath;


@SuppressLint("ExportedPreferenceActivity")
public class SettingsActivity extends AppCompatPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_settings);

        getSupportActionBar().setTitle(R.string.title_settings_activity);

        // load settings fragment
        getFragmentManager().beginTransaction().replace(android.R.id.content, new MainPreferenceFragment()).commit();
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        private final ViewHolder mViewHolder = new ViewHolder();
        private SecurityPreferences mSecurityPreferences;
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mSecurityPreferences = new SecurityPreferences(getActivity());

            if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 0)
                mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "3");
            else if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 1)
                mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "5");

            addPreferencesFromResource(R.xml.pref_main);

            mViewHolder.hevc = findPreference(getString(R.string.key_hevc_resolution));
            mViewHolder.mpeg = findPreference(getString(R.string.key_mpeg_resolution));
            mViewHolder.category = (PreferenceCategory) findPreference(getString(R.string.key_category_video));

            PreferencesListener listener = new PreferencesListener(mViewHolder, mSecurityPreferences);

            Preference hide = findPreference(getString(R.string.key_hide_icon));
            hide.setOnPreferenceChangeListener(listener);

            // video quality preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_video_quality)), "0", listener);

            // video encoder preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_video_encoder)), "5", listener);

            // finish action preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_finish_action)),"2", listener);

            // audio channel preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_audio_channel)),"1", listener);

            // audio quality preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_audio_quality)),"1", listener);

            // video orientation preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_video_orientation)),"0", listener);

            // audio source preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_audio_source)),"1", listener);

            // record path preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_record_path)), Constants.DEFAULT_PATH, listener);

            // file format preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_file_format)),Constants.DEFAULT_FILE_FORMAT, listener);

            // file format preference change listener
            bindPreferenceSummaryToValue(findPreference(getString(R.string.key_audio_encoder)),"3", listener);

            Preference notifications = findPreference(getString(R.string.key_recording_notification));
            notifications.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent();
                    intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");

                    if(Build.VERSION.SDK_INT == Build.VERSION_CODES.N)
                    {
                        intent.putExtra("app_package", getActivity().getPackageName());
                        intent.putExtra("app_uid", getActivity().getApplicationInfo().uid);
                    }
                    else
                        intent.putExtra("android.provider.extra.APP_PACKAGE", getActivity().getPackageName());

                    startActivity(intent);
                    return true;
                }
            });

            // reset preference click listener
            Preference resetSettings = findPreference(getString(R.string.key_reset_config));
            resetSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    resetToDefault(getActivity(), mSecurityPreferences);
                    return true;
                }
            });

            Preference report = findPreference(getString(R.string.key_report_bug));
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
            selectPath.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent pathIntent = new Intent(getActivity(), SelectPath.class);
                    pathIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    pathIntent.putExtra(Constants.INTENT_GET_ACTIVITY, SettingsActivity.class.getName());
                    getActivity().startActivity(pathIntent);
                    getActivity().finish();
                    return true;
                }
            });

            Preference version = findPreference(getString(R.string.key_version));
            version.setSummary(BuildConfig.VERSION_NAME + " by 98 Apps");
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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, this.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
        }
        else
            sendReport(getApplicationContext());
    }
    private static void sendReport(Context context)
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
                "log-" + new SimpleDateFormat(Constants.DEFAULT_FILE_FORMAT, Locale.US).format(new Date()) + ".txt");
        try {
            Runtime.getRuntime().exec(
                    "logcat -f " + logFile.getAbsolutePath());
        } catch (IOException e) {
            Toast.makeText(context.getApplicationContext(), context.getApplicationContext().getString(R.string.failed_get_log), Toast.LENGTH_SHORT).show();
        }

        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:98appshelp@gmail.com"));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "[Bug Report][Screen Recorder Edge]");
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(logFile));
        emailIntent.putExtra(Intent.EXTRA_TEXT, context.getApplicationContext().getResources().getString(R.string.please_write_details)+"\n\n--------------------------\n" + context.getApplicationContext().getResources().getString(R.string.dont_remove_information_text) + "\n"
                + Build.MANUFACTURER.toUpperCase() + "\n" + Build.MODEL + "\nAndroid: " + Build.VERSION.RELEASE + "\n" + "App Version: " + BuildConfig.VERSION_NAME);

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
    private static void resetToDefault(Activity activity, SecurityPreferences securityPreferences) {
        ComponentName componentToEnable = new ComponentName(activity.getApplicationContext(), LauncherActivity.class);
        PackageManager pm = activity.getApplicationContext().getPackageManager();
        pm.setComponentEnabledSetting(componentToEnable, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        securityPreferences.resetToDefault(activity);
        Toast.makeText(activity, activity.getString(R.string.text_settings_reseted), Toast.LENGTH_SHORT).show();
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(activity.getIntent());
        activity.overridePendingTransition(0, 0);
    }
    private static void bindPreferenceSummaryToValue(Preference preference, String defaultValue, PreferencesListener listener) {
        preference.setOnPreferenceChangeListener(listener);

        listener.onPreferenceChange(preference,
                Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(preference.getContext()).getString(preference.getKey(), defaultValue)));
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static class PreferencesListener implements Preference.OnPreferenceChangeListener
    {
        private final ViewHolder mViewHolder;
        private final SecurityPreferences mSecurityPreferences;

        PreferencesListener(ViewHolder holder, SecurityPreferences preferences) {
            this.mViewHolder = holder;
            this.mSecurityPreferences = preferences;
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

                if(preference.getKey().equals("key_video_encoder"))
                {
                    if(Integer.parseInt(stringValue) == 3)
                    {
                        mViewHolder.category.addPreference(mViewHolder.mpeg);
                        mViewHolder.category.removePreference(mViewHolder.hevc);
                        bindPreferenceSummaryToValue(mViewHolder.mpeg,"720", this);
                    }
                    else
                    {
                        mViewHolder.category.removePreference(mViewHolder.mpeg);
                        mViewHolder.category.addPreference(mViewHolder.hevc);
                        bindPreferenceSummaryToValue(mViewHolder.hevc,"0", this);
                    }
                }

            }
            else if(preference instanceof SwitchPreference)
            {
                if(preference.getKey().equals("key_hide_icon"))
                {
                    if(Boolean.parseBoolean(stringValue))
                    {
                        ComponentName componentToEnable = new ComponentName(preference.getContext(), LauncherActivity.class);
                        PackageManager pm = preference.getContext().getPackageManager();
                        pm.setComponentEnabledSetting(componentToEnable, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
                        Toast.makeText(preference.getContext(), preference.getContext().getString(R.string.text_5_seconds_disappear), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        ComponentName componentToEnable = new ComponentName(preference.getContext(), LauncherActivity.class);
                        PackageManager pm = preference.getContext().getPackageManager();
                        pm.setComponentEnabledSetting(componentToEnable, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                        Toast.makeText(preference.getContext(), preference.getContext().getString(R.string.text_5_seconds_appear), Toast.LENGTH_SHORT).show();
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
        private PreferenceCategory category;
    }
}
