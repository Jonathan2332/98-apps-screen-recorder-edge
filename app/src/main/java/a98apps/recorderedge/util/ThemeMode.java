package a98apps.recorderedge.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;

import a98apps.recorderedge.R;

import static a98apps.recorderedge.constants.Constants.APP_THEME;
import static a98apps.recorderedge.constants.Constants.THEME_DARK;
import static a98apps.recorderedge.constants.Constants.THEME_LIGHT;
import static a98apps.recorderedge.constants.Constants.THEME_SYSTEM;

public class ThemeMode {

    public static void checkTheme(Activity activity, boolean needsRestart, boolean needsSet)
    {
        SecurityPreferences mSecurityPreferences = new SecurityPreferences(activity);

        if(needsSet)
            setTheme(activity, Integer.parseInt(mSecurityPreferences.getSetting(APP_THEME)));

        if(needsRestart)
            activityRestart(activity);

    }

    public static boolean isDarkMode(Context context, int mode)
    {
        switch (mode)
        {
            case THEME_SYSTEM:
            {
                int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
                return currentNightMode == Configuration.UI_MODE_NIGHT_YES;
            }
            case THEME_LIGHT:
                return false;
            default:
                return true;
        }
    }

    private static void setTheme(Activity activity, int mode)
    {
        switch (mode)
        {
            case THEME_SYSTEM:
            {
                int currentNightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                if (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
                    activity.setTheme(R.style.AppThemeDark);
                else
                    activity.setTheme(R.style.AppTheme);

                break;
            }
            case THEME_LIGHT:
                activity.setTheme(R.style.AppTheme);
                break;
            case THEME_DARK:
            {
                int currentNightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                if (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
                    activity.setTheme(R.style.AppThemeDark);
                else
                    activity.setTheme(R.style.AppThemeDarkLegacy);

                break;
            }
            default:
            {
                int currentNightMode = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

                if (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
                    activity.setTheme(R.style.AppThemeAmoled);
                else
                    activity.setTheme(R.style.AppThemeAmoledLegacy);
            }
        }
    }

    private static void activityRestart(Activity activity)
    {
        activity.finish();
        activity.overridePendingTransition(0, 0);
        activity.startActivity(activity.getIntent());
        activity.overridePendingTransition(0, 0);
    }
}
