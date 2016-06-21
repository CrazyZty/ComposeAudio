package zty.composeaudio.Tool.Recorder.Native;

import android.media.MediaRecorder;

import com.Tool.Function.CommonFunction;
import com.Tool.Function.LogFunction;

import java.io.File;

/**
 * Created by 郑童宇 on 2015/11/25.
 */
public class NativeRecorder extends MediaRecorder {
    private int audioEncoder;
    private int outputFormat;
    private int audioSource;

    private final int baseAmplitude = 600;

    public NativeRecorder() {
        audioEncoder = MediaRecorder.AudioEncoder.DEFAULT;
        outputFormat = MediaRecorder.OutputFormat.MPEG_4;
        audioSource = MediaRecorder.AudioSource.MIC;
    }

    public boolean startRecord(File recordFile) {
        try {
            reset();
            setAudioSource(audioSource);
            setOutputFormat(outputFormat);
            setAudioEncoder(audioEncoder);
            setOutputFile(recordFile.getPath());
            prepare();
            start();
            return true;
        } catch (Exception e) {
            LogFunction.error("开始录音异常", e);

            CommonFunction.showToast("初始化录音失败", "NativeRecorder");
        }

        return false;
    }

    public boolean stopRecord() {
        try {
            stop();
        } catch (Exception e) {
            LogFunction.error("结束录音异常", e);
            CommonFunction.showToast("录音时间太短", "NativeRecorder");
            return false;
        }

        return true;
    }

    public int getVolume() {
        int ratio =  getMaxAmplitude() / baseAmplitude;
        int mVolume = 0;// 分贝

        if (ratio > 1) {
            mVolume = (int) (20 * Math.log10(ratio));
        }

        return mVolume/4;
    }
}
