package a98apps.recorderedge.record;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.Settings;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import a98apps.recorderedge.R;
import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.edge.CocktailScreenRecorder;
import a98apps.recorderedge.floating.FrameRateWindow;
import a98apps.recorderedge.util.PopupManager;
import a98apps.recorderedge.util.SecurityPreferences;
import wei.mark.standout.StandOutWindow;

public class RecorderController {

    private final Context context;
    public static MediaProjection mMediaProjection;
    public static MediaProjectionManager mProjectionManager;

    private boolean mRecording = false;
    private boolean isInternal;

    private VirtualDisplay mVirtualDisplay;
    private final SecurityPreferences mSecurityPreferences;

    private String fileName;
    private String filePath;
    private String filePathUri;
    private static String filePathTemp;//cache atual video recording path to don't show on list videos

    private MediaRecorder mMediaRecorder;
    private final CocktailScreenRecorder cocktail;
    private RecorderConfig recorderConfig;

    private final PopupManager popupManager;

    boolean isInternal() {
        return isInternal;
    }

    String getFileName() {
        return fileName;
    }

    private void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public static String getFilePathTemp() {
        return RecorderController.filePathTemp;
    }

    private void setFilePathTemp(String filePathTemp) {
        RecorderController.filePathTemp = filePathTemp;
    }

    String getFilePath() {
        return filePath;
    }

    private void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    String getFilePathUri() {
        return filePathUri;
    }

    private void setFilePathUri(String filePathUri) {
        this.filePathUri = filePathUri;
    }

    private boolean isRecording() {
        return mRecording;
    }

    private void setRecording(boolean mRecording) {
        this.mRecording = mRecording;
    }

    RecorderController(Context context, SecurityPreferences preferences)
    {
        this.context = context;
        this.mSecurityPreferences = preferences;
        this.cocktail = new CocktailScreenRecorder();
        this.popupManager = new PopupManager(context, preferences);
    }

    boolean start()
    {
        if(mVirtualDisplay == null)
            mVirtualDisplay = getVirtualDisplay();

        try
        {
            mMediaRecorder.start();
            setRecording(true);
            updatePanel();
            return true;
        }
        catch (final IllegalStateException i)
        {
            i.printStackTrace();
            releaseEncoders();
            undoFile();
            if (!Settings.canDrawOverlays(context.getApplicationContext()))
                Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
            else
            {
                if (Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.RECORD_MIC)))
                    popupManager.createPopupMessage(context.getString(R.string.failed_record_using_microphone), context.getString(R.string.title_mic_error));
                else
                    popupManager.createPopupReport(context.getString(R.string.failed_report_message_encoder), i.toString(), recorderConfig);
            }
            return false;
        }
    }
    void stop()
    {
        StandOutWindow.close(context.getApplicationContext(), FrameRateWindow.class, StandOutWindow.DEFAULT_ID);

        if(mMediaRecorder != null)
        {
            try
            {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
            catch (final RuntimeException r)
            {
                r.printStackTrace();
                mMediaRecorder.reset();
                mMediaRecorder.release();
                mMediaRecorder = null;

                if (!Settings.canDrawOverlays(context.getApplicationContext()))
                    Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
                else
                    popupManager.createPopupReport(context.getString(R.string.message_runtime_exception), r.toString(), recorderConfig);

                undoFile();

                if (mVirtualDisplay != null) {
                    mVirtualDisplay.release();
                    mVirtualDisplay = null;
                }
                if (mMediaProjection != null) {
                    mMediaProjection.stop();
                    mMediaProjection = null;
                }

                if (mProjectionManager != null)
                    mProjectionManager = null;

                setRecording(false);
                updatePanel();

                setFilePathTemp(null);
                return;
            }
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mProjectionManager != null)
            mProjectionManager = null;

        setRecording(false);
        updatePanel();

        setFilePathTemp(null);

        updateGallery();
    }

    boolean prepare()
    {
        FileDescriptor fileDescriptor = null;
        String directory = mSecurityPreferences.getSetting(Constants.RECORD_PATH);
        isInternal = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE));
        if(isInternal)
        {
            File folder = new File(directory);
            boolean success = true;
            if (!folder.exists())
                success = folder.mkdir();

            if (success)
            {
                setFileName(getCurSysDate() + ".mp4");
                setFilePath(directory + File.separator + getFileName());
            }
            else
            {
                showMessageFileReason(false, R.string.failed_create_directory);
                return false;
            }
        }
        else
        {
            DocumentFile dir = DocumentFile.fromTreeUri(context, Uri.parse(directory));
            if(dir != null)
            {
                if(!dir.exists())
                {
                    releaseEncoders();
                    setFilePathTemp(null);
                    if (!Settings.canDrawOverlays(context.getApplicationContext()))
                        Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
                    else
                        popupManager.createPopupSelectPath(context.getString(R.string.message_path_not_found));

                    return false;
                }

                setFileName(getCurSysDate() + ".mp4");
                DocumentFile dirFile = dir.findFile(getFileName());
                if (dirFile == null)
                {
                    dirFile = dir.createFile("video", getFileName());
                    if(dirFile != null)
                    {
                        try
                        {
                            String readablePath = mSecurityPreferences.getSetting(Constants.READABLE_PATH);
                            if(readablePath.substring(readablePath.length()-1).equals("/"))
                                setFilePath(readablePath +getFileName());
                            else
                                setFilePath(readablePath + File.separator + getFileName());

                            setFilePathUri(dirFile.getUri().toString());
                            ParcelFileDescriptor parcel = context.getApplicationContext().getContentResolver().openFileDescriptor(dirFile.getUri(), "rwt");
                            if(parcel != null)
                                fileDescriptor = parcel.getFileDescriptor();
                            else
                            {
                                showMessageFileReason(false, R.string.error_read_file);
                                return false;
                            }
                        }
                        catch (FileNotFoundException e)
                        {
                            showMessageFileReason(false, R.string.file_not_exist);
                            return false;
                        }
                    }
                    else
                    {
                        showMessageFileReason(false, R.string.text_failed_create_file);
                        return false;
                    }
                }
            }
            else
            {
                showMessageFileReason(false, R.string.failed_create_directory);
                return false;
            }
        }

        mMediaRecorder = new MediaRecorder();

        recorderConfig = getRecorderConfig();

        if(recorderConfig.recordMic)
            mMediaRecorder.setAudioSource(recorderConfig.audioSource);

        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoEncodingBitRate(recorderConfig.bitrate);
        mMediaRecorder.setVideoEncoder(recorderConfig.encoder);

        if(recorderConfig.recordMic)
            mMediaRecorder.setAudioEncoder(recorderConfig.audioEncoder);

        if(checkMinSpaceAvailable(recorderConfig.maxFileSize))
        {
            if(checkSize(recorderConfig.maxFileSize))
                mMediaRecorder.setMaxFileSize(recorderConfig.maxFileSize);

            mMediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
                @Override
                public void onInfo(MediaRecorder mr, int what, int extra) {
                    if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED)
                    {
                        stop();
                        if (!Settings.canDrawOverlays(context.getApplicationContext()))
                            Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
                        else
                            popupManager.createPopupMessage(context.getString(R.string.stopped_file_limit_memory), context.getString(R.string.title_file_limit));
                    }
                }
            });
        }
        else
        {
            if(isInternal)
            {
                File file = new File(getFilePath());
                if(file.exists() && file.delete())
                {
                    showMessageFileReason(true, 0);
                    return false;
                }
            }
            else
            {
                DocumentFile path = DocumentFile.fromTreeUri(context, Uri.parse(mSecurityPreferences.getSetting(Constants.RECORD_PATH)));
                if(path != null)
                {
                    DocumentFile file = path.findFile(getFileName());
                    if(file != null && file.delete())
                    {
                        showMessageFileReason(true, 0);
                        return false;
                    }
                }
            }

            showMessageFileReason(true, 0);
            return false;
        }
        if(recorderConfig.recordMic)
        {
            mMediaRecorder.setAudioSamplingRate(recorderConfig.sampleRateAudio);
            mMediaRecorder.setAudioChannels(recorderConfig.audioChannel);
            mMediaRecorder.setAudioEncodingBitRate(recorderConfig.bitRateAudio);
        }

        mMediaRecorder.setVideoSize(recorderConfig.width, recorderConfig.height);

        if(isInternal)
            mMediaRecorder.setOutputFile(getFilePath());
        else
            mMediaRecorder.setOutputFile(fileDescriptor);

        try
        {
            mMediaRecorder.prepare();
            setFilePathTemp(getFilePath());
            return true;
        }
        catch (final IOException e)
        {
            e.printStackTrace();
            releaseEncoders();
            undoFile();

            if (!Settings.canDrawOverlays(context.getApplicationContext()))
                Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
            else
                popupManager.createPopupReport(context.getString(R.string.failed_prepare_recorder), e.toString(), recorderConfig);

            return false;
        }
        catch(final IllegalStateException i)
        {
            i.printStackTrace();
            releaseEncoders();
            undoFile();
            if (!Settings.canDrawOverlays(context.getApplicationContext()))
                Toast.makeText(context, context.getString(R.string.give_overlay_permission), Toast.LENGTH_SHORT).show();
            else
                popupManager.createPopupReport(context.getString(R.string.failed_report_message), i.toString(), recorderConfig);

            return false;
        }
    }
    private void showMessageFileReason(boolean spaceMessage, int message)
    {
        if(spaceMessage)
        {
            if(recorderConfig.maxFileSize == -1)
            {
                releaseEncoders();
                Toast.makeText(context, context.getString(R.string.failed_get_external_storage), Toast.LENGTH_SHORT).show();
            }
            else
            {
                releaseEncoders();
                Toast.makeText(context, context.getString(R.string.little_memory_available), Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            releaseEncoders();
            Toast.makeText(context, context.getString(message), Toast.LENGTH_SHORT).show();
            setFilePathTemp(null);
        }
    }
    private void releaseEncoders()
    {
        StandOutWindow.close(context.getApplicationContext(), FrameRateWindow.class, StandOutWindow.DEFAULT_ID);

        if(mMediaRecorder!= null)
        {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        if (mProjectionManager != null)
            mProjectionManager = null;

        setRecording(false);
        updatePanel();
    }
    private void undoFile()
    {
        if(isInternal)
        {
            File file = new File(getFilePath());
            if(file.exists() && file.delete())
                setFilePathTemp(null);
            else
                setFilePathTemp(null);
        }
        else
        {
            DocumentFile path = DocumentFile.fromTreeUri(context, Uri.parse(mSecurityPreferences.getSetting(Constants.RECORD_PATH)));
            if(path != null)
            {
                DocumentFile file = path.findFile(getFileName());
                if(file != null && file.delete())
                    setFilePathTemp(null);
                else
                    setFilePathTemp(null);
            }
        }
    }
    private void updateGallery() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DATA, getFilePath());
        ContentResolver resolver = context.getApplicationContext().getContentResolver();
        resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        MediaScannerConnection.scanFile(context.getApplicationContext(),
        new String[]{new File(getFilePath()).getAbsolutePath()},
        null,
        new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String path, Uri uri) {

            }
        });
    }

    private boolean checkMinSpaceAvailable(long size) {
        double m = ((size / 1024.0) / 1024.0);

        if (m > 1) {
            DecimalFormat dec = new DecimalFormat("#.#");
            return Integer.parseInt(dec.format(m).substring(0, 1)) >= 1;//1MB
        }
        return false;
    }
    private boolean checkSize(long size) {
        double m = ((size / 1024.0) / 1024.0);
        double g = (((size / 1024.0) / 1024.0) / 1024.0);

        if (g > 1) {
            DecimalFormat dec = new DecimalFormat("#.#");
            return Integer.parseInt(dec.format(g).substring(0, 1)) < 4;//4GB
        }
        else return m > 1;
    }

    private RecorderConfig getRecorderConfig() {

        if(recorderConfig == null)
            recorderConfig = new RecorderConfig();

        return recorderConfig.prepareRecordingInfo(Constants.WIDTH, Constants.HEIGHT, Constants.DENSITY, mSecurityPreferences);
    }

    private String getCurSysDate() {
        return new SimpleDateFormat(mSecurityPreferences.getSetting(Constants.FILE_FORMAT), Locale.US).format(new Date());
    }
    private void updatePanel()
    {
        cocktail.updateButtonRec(context, isRecording());
    }
    private VirtualDisplay getVirtualDisplay() {
        if(recorderConfig == null)
            recorderConfig = getRecorderConfig();

        return mMediaProjection.createVirtualDisplay(getClass().getSimpleName(),
                recorderConfig.width, recorderConfig.height, recorderConfig.density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
    }
}
