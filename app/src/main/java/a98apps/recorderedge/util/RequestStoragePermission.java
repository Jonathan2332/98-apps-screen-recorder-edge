package a98apps.recorderedge.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.widget.Toast;

import java.io.File;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.view.SettingsActivity;

public class RequestStoragePermission extends Activity {
    private final int EXTERNAL_ACCESS_REQUEST_CODE = 26;
    private String path;
    private String activity;
    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);
        StorageManager sm = (StorageManager)getSystemService(Context.STORAGE_SERVICE);

        if(getIntent() != null && getIntent().getExtras() != null) {
            path = getIntent().getExtras().getString(Constants.INTENT_REQUEST_PATH, Constants.DEFAULT_PATH);
            activity = getIntent().getExtras().getString(Constants.INTENT_GET_ACTIVITY, null);
        }

        StorageVolume volume = sm.getStorageVolume(new File(path));
        if(volume != null)
        {
            Intent intent = volume.createAccessIntent(null);
            startActivityForResult(intent, EXTERNAL_ACCESS_REQUEST_CODE);
        }
        else
        {
            Toast.makeText(this, getString(R.string.failed_get_storage), Toast.LENGTH_LONG).show();
            finish();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == EXTERNAL_ACCESS_REQUEST_CODE && resultCode != RESULT_OK) {
            Toast.makeText(this, getString(R.string.permission_storage_needed), Toast.LENGTH_LONG).show();
            finish();
            if(activity != null)
            {
                if(activity.equals(SettingsActivity.class.getName()))
                    startActivity(new Intent(this, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }
        else if(requestCode == EXTERNAL_ACCESS_REQUEST_CODE)
        {
            Uri uri = data.getData();
            if(uri != null)
            {
                grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);

                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);


                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
            else
            {
                Toast.makeText(this, getString(R.string.failed_grant_permission), Toast.LENGTH_LONG).show();
            }

            finish();
            if(activity != null)
            {
                if(activity.equals(SettingsActivity.class.getName()))
                    startActivity(new Intent(this, SettingsActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                else if(activity.equals(RequestPermission.class.getName()))
                {
                    Intent permissionIntent = new Intent(this, RequestPermission.class);
                    permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(permissionIntent);
                }
            }
        }
    }
}
