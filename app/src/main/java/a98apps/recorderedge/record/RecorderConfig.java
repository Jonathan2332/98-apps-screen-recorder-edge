package a98apps.recorderedge.record;

import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

import a98apps.recorderedge.constants.Constants;
import a98apps.recorderedge.util.SecurityPreferences;

public class RecorderConfig {
    public int width;
    public int height;
    public int bitrate;
    public int density;
    public int bitRateAudio;
    public int sampleRateAudio;
    public int audioChannel;
    public int audioSource;
    public int encoder;
    public int audioEncoder;
    public boolean recordMic;
    public long maxFileSize;


    RecorderConfig() {
    }

    private RecorderConfig(int width, int height, int bitrate,
                           int density, int bitRateAudio, int sampleRateAudio,
                           boolean recordMic, long maxFileSize, int audioChannel,
                           int encoder, int audioSource, int audioEncoder) {

        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.density = density;
        this.bitRateAudio = bitRateAudio;
        this.sampleRateAudio = sampleRateAudio;
        this.recordMic = recordMic;
        this.maxFileSize = maxFileSize;
        this.audioChannel = audioChannel;
        this.encoder = encoder;
        this.audioSource = audioSource;
        this.audioEncoder = audioEncoder;
    }
    RecorderConfig prepareRecordingInfo(int displayWidth, int displayHeight, int displayDensity, SecurityPreferences mSecurityPreferences) {

        final int H264 = MediaRecorder.VideoEncoder.H264;
        final int MPEG4 = MediaRecorder.VideoEncoder.MPEG_4_SP;
        final int HEVC = MediaRecorder.VideoEncoder.HEVC;

        boolean isLandscape = Constants.LANDSCAPE;
        int bitRate = Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_QUALITY)) * 1000;

        if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 0)
            mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "3");
        else if(Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER)) == 1)
            mSecurityPreferences.saveSetting(Constants.VIDEO_ENCODER, "5");

        int encoder = Integer.parseInt(mSecurityPreferences.getSetting(Constants.VIDEO_ENCODER));

        int resolution = Integer.parseInt(mSecurityPreferences.getSetting(encoder == MPEG4 ? Constants.MPEG_RESOLUTION : Constants.HEVC_RESOLUTION));
        int profileQuality = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_QUALITY));
        boolean recordMic = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.RECORD_MIC));
        int audioChannel = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_CHANNEL));
        int audioSource = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_SOURCE));
        int audioEncoder = Integer.parseInt(mSecurityPreferences.getSetting(Constants.AUDIO_ENCODER));
        int sampleRateAudio = getSampleRateAudio(profileQuality);
        int bitRateAudio = getBitRateAudio(profileQuality);
        long maxFileSize = getTotalExternalMemorySize(mSecurityPreferences);

        float aspectRatio = Constants.ASPECT_RATIO;

        final int newHeight;
        final int newWidth;
        if(encoder == HEVC || encoder == H264)
        {
            if(isS10() || isNote10())//S10 Variants & Note 10 Variants
            {
                if(resolution == 480)
                {
                    newHeight = (int) (aspectRatio * resolution)-3;
                    newWidth = resolution;
                }
                else
                {
                    newHeight = resolution == 0 ? displayHeight : (int) (aspectRatio * resolution);
                    newWidth = resolution == 0 ? displayWidth : resolution;
                }
            }
            else
            {
                newHeight = resolution == 0 ? displayHeight : (int) (aspectRatio * resolution);
                newWidth = resolution == 0 ? displayWidth : resolution;
            }
        }
        else
        {
            switch (resolution)
            {
                case 960:
                case 1080:
                    newHeight = 1920;
                    newWidth = resolution;
                    break;
                default:
                    if(isS10() || isNote10())
                    {
                        if(resolution == 480)
                        {
                            newHeight = (int) (aspectRatio * resolution)-3;
                            newWidth = resolution;
                        }
                        else
                        {
                            newHeight = (int) (aspectRatio * resolution);
                            newWidth = resolution;
                        }
                    }
                    else
                    {
                        newHeight = (int) (aspectRatio * resolution);
                        newWidth = resolution;
                    }
            }

        }

        if(bitRate == 0)
            bitRate = getBitRateVideo(encoder == MPEG4, resolution, newWidth);

        if (isLandscape)
        {
            int w, h;
            w = newHeight;
            h = newWidth;
            return new RecorderConfig(w, h, bitRate,
                    displayDensity, bitRateAudio, sampleRateAudio,
                    recordMic, maxFileSize, audioChannel,
                    encoder, audioSource, audioEncoder);
        }
        else
        {
            return new RecorderConfig(newWidth, newHeight, bitRate,
                    displayDensity, bitRateAudio, sampleRateAudio,
                    recordMic, maxFileSize, audioChannel,
                    encoder, audioSource, audioEncoder);
        }
    }
    private int getBitRateVideo(boolean isMpeg, int resolution, int newWidth)
    {
        if(isMpeg)
        {
            switch (resolution)
            {
                case 1080:
                    return 16384 * 1000;
                case 960:
                    return 14336 * 1000;
                case 900:
                    return 12228 * 1000;
                case 720:
                    return 10240 * 1000;
                case 540:
                    return 8192 * 1000;
                case 480:
                    return 7168 * 1000;
                default://360
                    return 6144 * 1000;
            }
        }
        else
        {
            switch (resolution)
            {
                case 1440:
                    return 9216 * 1000;
                case 1080:
                    return 6144 * 1000;
                case 720:
                    return 4096 * 1000;
                case 540:
                    return 3072 * 1000;
                case 480:
                    return 2048 * 1000;
                case 360:
                    return 1536 * 1000;
                default:
                    switch (newWidth)
                    {
                        case 1080:
                            return 6144 * 1000;
                        case 720:
                            return 4096 * 1000;
                        default:
                            return 9216 * 1000;
                    }
            }
        }
    }
    private int getSampleRateAudio(int profile) {
        switch (profile)
        {
            case 0:
            case 1:
                return 48000;
            default:
                return 44100;
        }
    }
    private int getBitRateAudio(int profile) {
        switch (profile)
        {
            case 0:
                return 512000;
            case 1:
                return 256000;
            case 2:
                return 128000;
            default:
                return 64000;
        }
    }
    private long getTotalExternalMemorySize(SecurityPreferences mSecurityPreferences) {
        if (externalMemoryAvailable()) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                return Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE)) ? new File(Environment.getExternalStorageDirectory().toString()).getFreeSpace() : new File(mSecurityPreferences.getSetting(Constants.READABLE_PATH)).getFreeSpace();
            else
            {
                File path = Boolean.parseBoolean(mSecurityPreferences.getSetting(Constants.INTERNAL_STORAGE)) ? Environment.getExternalStorageDirectory() : new File(mSecurityPreferences.getSetting(Constants.READABLE_PATH));
                StatFs stat = new StatFs(path.getPath());
                long blockSize = stat.getBlockSizeLong();
                long totalBlocks = stat.getBlockCountLong();
                return totalBlocks * blockSize;
            }

        } else {
            return -1;
        }
    }
    private boolean externalMemoryAvailable() {
        return android.os.Environment.getExternalStorageState().equals(
                android.os.Environment.MEDIA_MOUNTED);
    }
    private boolean isS10()
    {
        return Build.MODEL.contains("G970") || Build.MODEL.contains("G973") || Build.MODEL.contains("G975") || Build.MODEL.contains("G977");//S10 Variants EDGE
    }
    private boolean isNote10()
    {
        return Build.MODEL.contains("N970") || Build.MODEL.contains("N971") || Build.MODEL.contains("N975") || Build.MODEL.contains("N976");//Note 10 Variants
    }
}

