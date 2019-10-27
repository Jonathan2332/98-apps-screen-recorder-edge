package a98apps.recorderedge.view;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.util.SecurityPreferences;


public class LauncherActivity extends Activity {
    @Override
    protected void onCreate(Bundle b)
    {
        super.onCreate(null);
        if(!Boolean.parseBoolean(new SecurityPreferences(this).getSetting(Constants.INITIALIZED)))
        {
            AlertDialog explainsDialog = new AlertDialog.Builder(this).create();
            @SuppressLint("InflateParams") View explains = getLayoutInflater().inflate(R.layout.dialog_explains, null);
            ImageView image = explains.findViewById(R.id.gif);
            Glide.with(this).asGif().load(R.drawable.gif_explains).into(image);
            explainsDialog.setView(explains);
            explainsDialog.setIcon(R.drawable.ic_info);
            explainsDialog.setTitle(getString(R.string.enable_the_panel));
            explainsDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.text_go),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            Intent intent = new Intent();
                            intent.setClassName("com.samsung.android.app.cocktailbarservice", "com.samsung.android.app.cocktailbarservice.settings.EdgePanels");
                            startActivity(intent);
                            finish();
                        }
                    });
            explainsDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if(dialog != null)
                        dialog.dismiss();
                    finish();
                }
            });
            explainsDialog.show();
        }
        else
        {
            Intent intent = new Intent();
            intent.setClassName("com.samsung.android.app.cocktailbarservice", "com.samsung.android.app.cocktailbarservice.settings.EdgePanels");
            startActivity(intent);
            finish();
        }
    }
}
