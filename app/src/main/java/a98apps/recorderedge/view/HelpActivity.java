package a98apps.recorderedge.view;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

import a98apps.recorderedge.R;
import a98apps.recorderedge.util.ThemeMode;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeMode.checkTheme(this, false, true);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_layout);

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_help_34dp);

        getSupportActionBar().setTitle(R.string.text_help_area_cocktail);
    }
}
