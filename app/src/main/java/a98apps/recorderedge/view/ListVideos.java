package a98apps.recorderedge.view;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.util.CustomListAdapter;
import a98apps.recorderedge.util.RecordService;
import a98apps.recorderedge.util.SecurityPreferences;

public class ListVideos extends AppCompatActivity {
    private final ViewHolder mViewHolder = new ViewHolder();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_videos);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.mipmap.ic_list_padding);

        getSupportActionBar().setTitle(" Videos");

        if (!hasPermissions(this))
            requestWriteExternalPermission(this);
        else
        {
            mViewHolder.listVideos = findViewById(R.id.list_videos);
            mViewHolder.swipeRefreshLayout = findViewById(R.id.refresh);

            mViewHolder.swipeRefreshLayout.setRefreshing(true);
            new RefreshVideos(mViewHolder, ListVideos.this).execute();

            mViewHolder.swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {

                    mViewHolder.listAdapter.clear();
                    mViewHolder.listAdapter.notifyDataSetChanged();

                    mViewHolder.listVideos.setAdapter(null);

                    new RefreshVideos(mViewHolder, ListVideos.this).execute();
                }
            });
        }
    }
    private static class RefreshVideos extends AsyncTask<Void, Void, Void>
    {
        private final ViewHolder mViewHolder;
        private final WeakReference<Activity> context;
        RefreshVideos(ViewHolder holder, Activity context) {
            this.mViewHolder = holder;
            this.context = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            ArrayList<File> videos = new ArrayList<>(getListFiles(new File(new SecurityPreferences(context.get()).getSetting(Constants.READABLE_PATH))));
            ArrayList<String> names = new ArrayList<>();
            ArrayList<String> sizes = new ArrayList<>();
            ArrayList<String> paths = new ArrayList<>();
            ArrayList<String> resolutions = new ArrayList<>();
            ArrayList<String> durations = new ArrayList<>();
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            for (File f : videos)
            {
                names.add(f.getName());
                sizes.add(context.get().getString(R.string.text_size)+ formatSize(f.length()));
                paths.add(f.getPath());
                try
                {
                    retriever.setDataSource(context.get(), Uri.fromFile(f));
                    long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    durations.add(String.format(Locale.US,context.get().getString(R.string.text_duration) + " %d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(duration),
                            TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))));

                    int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                    int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                    resolutions.add(String.format(Locale.US,context.get().getString(R.string.text_resolution)+" %dx%d", width, height));

                } catch (RuntimeException e) {
                    durations.add(context.get().getString(R.string.text_unknown));
                    resolutions.add(context.get().getString(R.string.text_unknown));
                }
            }
            retriever.release();
            mViewHolder.listAdapter = new CustomListAdapter(context.get(), names, sizes, paths, resolutions, durations);
            context.get().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mViewHolder.listVideos.setAdapter(mViewHolder.listAdapter);
                }
            });
            return null;
        }
        private ArrayList<File> getListFiles(File parentDir) {
            if(!parentDir.exists()) return new ArrayList<>();
            else
            {

                ArrayList<File> inFiles = new ArrayList<>();
                File[] files;
                files = parentDir.listFiles();
                String actualVideo = RecordService.getFilePathTemp() != null ? RecordService.getFilePathTemp() : "none";
                if (files != null) {
                    for (File file : files) {
                        if (file.getName().endsWith(".mp4") && !file.getPath().equals(actualVideo)) {
                            if (!inFiles.contains(file)) inFiles.add(file);
                        }
                    }
                }

                Collections.sort(inFiles, new Comparator<File>() {
                    public int compare(File f1, File f2) {
                        return Long.compare(f1.lastModified(), f2.lastModified());
                    }
                });
                Collections.reverse(inFiles);
                return inFiles;
            }
        }
        private String formatSize(long size) {

            double k = size / 1024.0;
            double m = ((size / 1024.0) / 1024.0);
            double g = (((size / 1024.0) / 1024.0) / 1024.0);

            if (g > 1) {
                DecimalFormat dec = new DecimalFormat("#.00");
                return dec.format(g).concat(" GB");
            }
            else if (m > 1) {
                DecimalFormat dec = new DecimalFormat("#.0");
                return dec.format(m).concat(" MB");
            }
            else if (k > 1)
            {
                DecimalFormat dec = new DecimalFormat("#.0");
                return dec.format(k).concat(" KB");
            }
            else
            {
                DecimalFormat dec = new DecimalFormat("#");
                return dec.format(k).concat(" Bytes");
            }
        }

        @Override
        protected void onPostExecute(Void result) {
            mViewHolder.swipeRefreshLayout.setRefreshing(false);
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
    private boolean hasPermissions(Context context) {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, this.getString(R.string.permission_denied), Toast.LENGTH_SHORT).show();
            finish();
        }
        else
        {
            Intent intent = getIntent();
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            finish();
            startActivity(intent);
        }
    }
    private class ViewHolder
    {
        CustomListAdapter listAdapter;
        ListView listVideos;
        SwipeRefreshLayout swipeRefreshLayout;
    }
}
