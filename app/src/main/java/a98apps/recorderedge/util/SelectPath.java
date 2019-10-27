package a98apps.recorderedge.util;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;

import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.view.SettingsActivity;

public class SelectPath extends Activity {
    private final int SELECT_PATH_REQUEST_CODE = 25;
    private String activity;
    private SecurityPreferences mSecurityPreferences;
    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        mSecurityPreferences = new SecurityPreferences(this);
        if(getIntent() != null && getIntent().getExtras() != null) {
            activity = getIntent().getExtras().getString(Constants.INTENT_GET_ACTIVITY, null);
        }
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(i, SELECT_PATH_REQUEST_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PATH_REQUEST_CODE && resultCode != RESULT_OK) {
            if(activity != null)
            {
                if(activity.equals(SettingsActivity.class.getName()))
                    startActivity(new Intent(this, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            finish();
        }
        else if(requestCode == SELECT_PATH_REQUEST_CODE)
        {
            Uri uri = Uri.parse(data.getDataString());
            File file = new File(uri.getPath());
            String path = file.getAbsolutePath()
                    .replace("tree", "storage")
                    .replace("primary", "emulated/0")
                    .replace(":","/");

            if(file.getAbsolutePath().contains("primary"))
            {
                mSecurityPreferences.saveSetting(Constants.RECORD_PATH, path);
                mSecurityPreferences.saveSetting(Constants.INTERNAL_STORAGE, String.valueOf(true));
                mSecurityPreferences.saveSetting(Constants.READABLE_PATH, path);
                finish();
                startActivity(new Intent(this, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            else
            {
                mSecurityPreferences.saveSetting(Constants.RECORD_PATH, uri.toString());
                mSecurityPreferences.saveSetting(Constants.INTERNAL_STORAGE, String.valueOf(false));
                mSecurityPreferences.saveSetting(Constants.READABLE_PATH, path);
                finish();
                if(activity != null)
                {
                    startActivity(new Intent(this, RequestStoragePermission.class)
                            .putExtra(Constants.INTENT_REQUEST_PATH,path)
                            .putExtra(Constants.INTENT_GET_ACTIVITY,activity.equals(SettingsActivity.class.getName()) ? SettingsActivity.class.getName() : RequestPermission.class.getName())
                            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            }
        }
    }
}
