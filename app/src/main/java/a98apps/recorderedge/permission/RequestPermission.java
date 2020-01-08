package a98apps.recorderedge.permission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.floating.FrameRateWindow;
import a98apps.recorderedge.record.RecordService;
import a98apps.recorderedge.record.RecorderController;
import a98apps.recorderedge.util.SecurityPreferences;
import wei.mark.standout.StandOutWindow;

import static a98apps.recorderedge.constants.Constants.ACTION_START_REC;
import static a98apps.recorderedge.record.RecorderController.mProjectionManager;

public class RequestPermission extends Activity
{
    private final int CAST_PERMISSION_CODE = 22;
    private final int OVERLAY_PERMISSION_CODE = 23;
    private boolean recordMic;
    private SecurityPreferences mSecurityPreferences;

    @Override
    protected void onCreate(Bundle instance)
    {
        super.onCreate(null);
        if (!Settings.canDrawOverlays(getApplicationContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
            return;
        }

        mSecurityPreferences = new SecurityPreferences(getApplicationContext());
        if(!Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE)))
        {
            if(!hasPermissionUri(getApplicationContext(), mSecurityPreferences))
            {
                finish();
                startActivity(new Intent(getApplicationContext(), RequestStoragePermission.class)
                        .putExtra(Constants.INTENT_REQUEST_PATH,mSecurityPreferences.getSetting(Constants.READABLE_PATH))
                        .putExtra(Constants.INTENT_GET_ACTIVITY, RequestPermission.class.getName())
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
                return;
            }
        }
        recordMic = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.RECORD_MIC));

        if(recordMic)
        {
            if (hasPermissions(this, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE))
            {
                if(mProjectionManager == null)
                    mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

                assert mProjectionManager != null;
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            }
            else
                checkAndRequestPermissions(this);
        }
        else
        {
            if (hasPermissions(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if(mProjectionManager == null)
                    mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);

                assert mProjectionManager != null;
                startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
            }
            else
                requestWriteExternalPermission(this);
        }
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == OVERLAY_PERMISSION_CODE)
        {
            if (!Settings.canDrawOverlays(getApplicationContext()))
            {
                Toast.makeText(this, this.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            else
            {
                Intent intent = getIntent();
                finish();
                startActivity(intent);
                return;
            }
        }

        if (requestCode == CAST_PERMISSION_CODE && resultCode != RESULT_OK) {
            Toast.makeText(this, this.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }


        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics mDisplayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(mDisplayMetrics);

        StandOutWindow.show(getApplicationContext(), FrameRateWindow.class, StandOutWindow.DEFAULT_ID);

        System.out.println("DISPLAY METRICS: "+ mDisplayMetrics.widthPixels + " | "+ mDisplayMetrics.heightPixels);

        boolean isLandscape = false;
        if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ORIENTATION)) == 2)
            isLandscape = true;
        else if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ORIENTATION)) == 0 && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            isLandscape = true;

        float aspectRatio = mDisplayMetrics.widthPixels > mDisplayMetrics.heightPixels ? checkDeviceResolution(mDisplayMetrics.widthPixels,mDisplayMetrics.heightPixels) : checkDeviceResolution(mDisplayMetrics.heightPixels,mDisplayMetrics.widthPixels);

        Constants.ASPECT_RATIO = aspectRatio;
        Constants.WIDTH = mDisplayMetrics.widthPixels > mDisplayMetrics.heightPixels ? mDisplayMetrics.heightPixels : mDisplayMetrics.widthPixels;
        Constants.HEIGHT = mDisplayMetrics.widthPixels > mDisplayMetrics.heightPixels ? (int) (aspectRatio * mDisplayMetrics.heightPixels) : (int) (aspectRatio * mDisplayMetrics.widthPixels);
        Constants.DENSITY = mDisplayMetrics.densityDpi;
        Constants.LANDSCAPE = isLandscape;

        System.out.println("CONSTANTS DISPLAY INFO: "+ Constants.WIDTH + " | "+ Constants.HEIGHT);

        RecorderController.mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        startService(new Intent(this, RecordService.class).setAction(ACTION_START_REC));
        finish();
    }
    private float checkDeviceResolution(int h, int w)
    {
        String model = Build.MODEL;
        if(model.contains("G925") || model.contains("G928"))//S6 Edge, S6 Edge+
            return (float) 2560 / (float) 1440;
        else if(model.contains("G935"))//S7 EDGE
            return (float) 2560 / (float) 1440;
        else if(model.contains("G950") || model.contains("G955") || model.contains("N950") //S8, S8+, Note 8
                || model.contains("G960") || model.contains("G965") || model.contains("N960"))// S9, S9+, Note 9
            return (float) 2960 / (float) 1440;
        else if(model.contains("G970"))//S10e
            return (float) 2280 / (float) 1080;
        else if(model.contains("G973") || model.contains("G975") || model.contains("G977"))//S10, S10+, S10 5G
            return (float) 3040 / (float) 1440;
        else if(model.contains("N970") || model.contains("N971"))//Note 10, Note 10 5G
            return (float) 2280 / (float) 1080;
        else if(model.contains("N975") || model.contains("N976"))//Note 10+, Note 10+ 5G
            return (float) 3040 / (float) 1440;
        else return (float) h / (float) w;

    }
    private boolean hasPermissionUri(Context context, SecurityPreferences mSecurityPreferences) {
        String path = mSecurityPreferences.getSetting(Constants.RECORD_PATH);
        List<UriPermission> persistedUriPermissions = context.getContentResolver().getPersistedUriPermissions();
        if (persistedUriPermissions.size() > 0) {
            for(UriPermission uri : persistedUriPermissions)
            {
                if(uri.getUri().getEncodedPath() != null)
                {
                    if(path.contains(uri.getUri().getEncodedPath()))
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private void checkAndRequestPermissions(Activity activity) {

        int permissionAudio = ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO);
        int permissionWriteExternal = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        // Permission List
        List<String> listPermissionsNeeded = new ArrayList<>();

        // Read/Write Permission
        if (permissionWriteExternal != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        // Record Audio Permission
        if (permissionAudio != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.RECORD_AUDIO);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(activity,
                    listPermissionsNeeded.toArray(new String[0]),
                    0);
        }

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
    private boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for(String permission : permissions)
        {
            if(permissions.length == 1)
            {
                if(permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    {
                        if(mProjectionManager == null)
                            mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        assert mProjectionManager != null;
                        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
                    } else {
                        //If user presses deny
                        Toast.makeText(this, this.getString(R.string.write_permission_needed), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                else
                {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        if(mProjectionManager == null)
                            mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        assert mProjectionManager != null;
                        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
                    } else {
                        //If user presses deny
                        Toast.makeText(this, this.getString(R.string.microphone_permission_needed), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
            else
            {
                if(permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        //If user presses deny
                        Toast.makeText(this, this.getString(R.string.write_permission_needed), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                if(permission.equals(Manifest.permission.RECORD_AUDIO)) {
                    if (grantResults.length > 0 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                        if(mProjectionManager == null)
                            mProjectionManager = (MediaProjectionManager) getApplicationContext().getSystemService(Context.MEDIA_PROJECTION_SERVICE);
                        assert mProjectionManager != null;
                        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), CAST_PERMISSION_CODE);
                    } else {
                        //If user presses deny
                        if(recordMic) {
                            Toast.makeText(this, this.getString(R.string.microphone_permission_needed), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }
            }
        }
    }
}
