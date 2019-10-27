package a98apps.recorderedge.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.provider.DocumentFile;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class CustomListAdapter extends ArrayAdapter<String> {

    private final ViewHolder mViewHolder = new ViewHolder();
    private final Activity context;
    private final ArrayList<String> nameVideo;
    private final ArrayList<String> sizeVideo;
    private final ArrayList<String> pathVideo;
    private final ArrayList<String> resolutionVideo;
    private final ArrayList<String> durationVideo;

    public CustomListAdapter(Activity context, ArrayList<String> nameArrayVideo,
                             ArrayList<String> sizeArrayVideo,
                             ArrayList<String> pathsArrayVideo,
                             ArrayList<String> resolutionArrayVideo,
                             ArrayList<String> durationArrayVideo){

        super(context, R.layout.listview_row, nameArrayVideo);
        this.context = context;
        this.nameVideo = new ArrayList<>(nameArrayVideo);
        this.sizeVideo = new ArrayList<>(sizeArrayVideo);
        this.pathVideo = new ArrayList<>(pathsArrayVideo);
        this.resolutionVideo = new ArrayList<>(resolutionArrayVideo);
        this.durationVideo = new ArrayList<>(durationArrayVideo);
    }
    @SuppressLint("InflateParams")
    @NonNull
    public View getView(final int position, View view, @NonNull ViewGroup parent) {
        View rowView;

        if (view == null) {
            LayoutInflater inflater = context.getLayoutInflater();
            rowView = inflater.inflate(R.layout.listview_row, null,true);
        }
        else
            rowView = view;

        mViewHolder.nameTextField = rowView.findViewById(R.id.name_video);
        mViewHolder.sizeTextField = rowView.findViewById(R.id.size_video);
        mViewHolder.resolutionTextField = rowView.findViewById(R.id.resolution_video);
        mViewHolder.durationTextField = rowView.findViewById(R.id.duration_video);
        mViewHolder.thumbnail = rowView.findViewById(R.id.thumb_video);

        mViewHolder.buttonWatch = rowView.findViewById(R.id.watch_video);
        mViewHolder.buttonShare = rowView.findViewById(R.id.share_video);
        mViewHolder.buttonDelete = rowView.findViewById(R.id.delete_video);

        mViewHolder.nameTextField.setText(nameVideo.get(position));
        mViewHolder.sizeTextField.setText(sizeVideo.get(position));
        mViewHolder.resolutionTextField.setText(resolutionVideo.get(position));
        mViewHolder.durationTextField.setText(durationVideo.get(position));

        Glide.with(context)
                .load(pathVideo.get(position))
                .override((int)context.getResources().getDimension(R.dimen.thumb_width), (int)context.getResources().getDimension(R.dimen.thumb_height))
                .placeholder(R.drawable.ic_loading)
                .error(R.drawable.ic_error)
                .centerCrop()
                .transition(withCrossFade())
                .into(mViewHolder.thumbnail);

        mViewHolder.buttonWatch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent videoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(pathVideo.get(position)));
                videoIntent.setDataAndType(Uri.parse(pathVideo.get(position)), "video/mp4");
                videoIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(videoIntent);
            }
        });
        mViewHolder.buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setDataAndType(Uri.fromFile(new File(pathVideo.get(position))), "video/mp4");
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(pathVideo.get(position))));
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_with)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        });
        mViewHolder.buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SecurityPreferences mSecurityPreferences = new SecurityPreferences(context);
                if(Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE)))
                {
                    File file = new File(pathVideo.get(position));
                    if(file.exists())
                        if(file.delete())
                        {
                            Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                            scanIntent.setData(Uri.fromFile(new File(pathVideo.get(position))));
                            context.getApplicationContext().sendBroadcast(scanIntent);
                            pathVideo.remove(position);
                            nameVideo.remove(position);
                            sizeVideo.remove(position);
                            resolutionVideo.remove(position);
                            durationVideo.remove(position);
                            remove(getItem(position));
                            notifyDataSetChanged();
                            Toast.makeText(context, context.getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
                        }
                        else
                            Toast.makeText(context, context.getString(R.string.error_on_delete), Toast.LENGTH_SHORT).show();
                    else {
                        pathVideo.remove(position);
                        nameVideo.remove(position);
                        sizeVideo.remove(position);
                        resolutionVideo.remove(position);
                        durationVideo.remove(position);
                        remove(getItem(position));
                        notifyDataSetChanged();
                        Toast.makeText(context, context.getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                    }
                }
                else
                {
                    DocumentFile path = DocumentFile.fromTreeUri(context, Uri.parse(mSecurityPreferences.getSetting(Constants.RECORD_PATH)));
                    if(path != null)
                    {
                        DocumentFile file = path.findFile(nameVideo.get(position));
                        if(file != null)
                            if(file.delete())
                            {
                                Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                scanIntent.setData(Uri.parse(file.getUri().toString()));
                                context.getApplicationContext().sendBroadcast(scanIntent);
                                pathVideo.remove(position);
                                nameVideo.remove(position);
                                sizeVideo.remove(position);
                                resolutionVideo.remove(position);
                                durationVideo.remove(position);
                                remove(getItem(position));
                                notifyDataSetChanged();
                                Toast.makeText(context, context.getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
                            }
                            else
                                Toast.makeText(context, context.getString(R.string.error_on_delete), Toast.LENGTH_SHORT).show();
                        else {
                            pathVideo.remove(position);
                            nameVideo.remove(position);
                            sizeVideo.remove(position);
                            resolutionVideo.remove(position);
                            durationVideo.remove(position);
                            remove(getItem(position));
                            notifyDataSetChanged();
                            Toast.makeText(context, context.getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                        }
                    }
                    else {
                        pathVideo.remove(position);
                        nameVideo.remove(position);
                        sizeVideo.remove(position);
                        resolutionVideo.remove(position);
                        durationVideo.remove(position);
                        remove(getItem(position));
                        notifyDataSetChanged();
                        Toast.makeText(context, context.getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
        return rowView;
    }
    private class ViewHolder
    {
        TextView nameTextField;
        TextView sizeTextField;
        TextView resolutionTextField;
        TextView durationTextField;
        ImageView thumbnail;

        Button buttonWatch;
        Button buttonShare;
        Button buttonDelete;
    }
}
