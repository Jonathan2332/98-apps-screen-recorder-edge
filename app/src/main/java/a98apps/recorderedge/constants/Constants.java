package a98apps.recorderedge.constants;

import android.os.Environment;

import java.io.File;

public class Constants {
    public static final String INITIALIZED = "initialized";
    public static final String RECORD_MIC = "key_record_mic";
    public static final String VIDEO_QUALITY = "key_video_quality";
    public static final String MPEG_RESOLUTION = "key_mpeg_resolution";
    public static final String HEVC_RESOLUTION = "key_hevc_resolution";
    public static final String VIDEO_ORIENTATION = "key_video_orientation";
    public static final String VIDEO_ENCODER = "key_video_encoder";
    public static final String AUDIO_QUALITY = "key_audio_quality";
    public static final String AUDIO_ENCODER = "key_audio_encoder";
    public static final String AUDIO_SOURCE = "key_audio_source";
    public static final String AUDIO_CHANNEL = "key_audio_channel";
    public static final String FINISH_ACTION = "key_finish_action";
    public static final String SHOW_RECORDING_NOTIFICATION = "key_recording_notification";
    public static final String RECORD_PATH = "key_record_path";
    public static final String FILE_FORMAT = "key_file_format";

    public static final String DEFAULT_PATH = Environment.getExternalStorageDirectory() + File.separator + "Screen Recorder Edge";
    public static final String DEFAULT_FILE_FORMAT = "yyyyddMM_HHmmss";
    public static final String INTERNAL_STORAGE = "key_internal_storage";
    public static final String READABLE_PATH = "key_readable_path";

    public static final String APP_THEME = "key_app_theme";

    public static final int ACTION_NOTIFICATION = 1;
    public static final int ACTION_POPUP = 2;
    public static final int ACTION_VIDEOS = 3;

    //---------------------COLORS--------------------------

    public static final String PANEL_COLOR = "key_panel_color";
    public static final String BUTTON_REC_COLOR = "key_button_rec_theme";
    public static final String LIST_ICON_COLOR = "key_list_icon_color";
    public static final String SETTINGS_ICON_COLOR = "key_settings_icon_color";

    //-------------------------------------------------

    //---------------------THEME--------------------------

    public static final int THEME_SYSTEM = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    //-------------------------------------------------

    //---------------------CONFIG---------------------------

    public static float ASPECT_RATIO = 0;
    public static int WIDTH = 0;
    public static int HEIGHT = 0;
    public static int DENSITY = 0;
    public static boolean LANDSCAPE = false;

    //-------------------------------------------------

    public static final String ACTION_REMOTE_CLICK = "a98apps.recorderedge.action.ACTION_REMOTE_CLICK";
    public static final String INTENT_REQUEST_PATH = "a98apps.recorderedge.action.ACTION_REQUEST_STORAGE";
    public static final String INTENT_GET_ACTIVITY = "a98apps.recorderedge.action.GET_ACTIVITY";

    public static final String ACTION_SHOW_NOTIFICATION = "a98apps.recorderedge.action.ACTION_SHOW_NOTIFICATION";
    public static final String ACTION_CLICK_NOTIFICATION = "a98apps.recorderedge.action.ACTION_CLICK_NOTIFICATION";
    public static final String ACTION_START_REC = "a98apps.recorderedge.action.ACTION_START_REC";
    public static final String ACTION_STOP_REC = "a98apps.recorderedge.action.ACTION_STOP_REC";

    public static final String NOTIFICATION_CHANNEL_ID_FINISHED = "com.a98apps.finished";
    public static final String NOTIFICATION_CHANNEL_ID_RECORDING = "com.a98apps.recording";

    public static final int ACTION_WATCH = 0;
    public static final int ACTION_SHARE = 1;
    public static final int ACTION_DELETE = 2;
    public static final int ACTION_CLOSE = 3;

    public static final int NOTIFICATION_FINISHED_ID = 0;
    public static final int NOTIFICATION_RECORDING_ID = 1;

}
