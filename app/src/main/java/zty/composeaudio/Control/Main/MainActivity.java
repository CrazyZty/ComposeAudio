package zty.composeaudio.Control.Main;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.Tool.Common.CommonApplication;
import com.Tool.Function.AudioFunction;
import com.Tool.Function.CommonFunction;
import com.Tool.Function.FileFunction;
import com.Tool.Function.LogFunction;
import com.Tool.Function.VoiceFunction;
import com.Tool.Global.Constant;
import com.Tool.Global.Variable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import zty.composeaudio.R;
import zty.composeaudio.Tool.Interface.ComposeAudioInterface;
import zty.composeaudio.Tool.Interface.DecodeOperateInterface;
import zty.composeaudio.Tool.Interface.VoicePlayerInterface;
import zty.composeaudio.Tool.Interface.VoiceRecorderOperateInterface;

public class MainActivity extends Activity
        implements VoicePlayerInterface, DecodeOperateInterface, ComposeAudioInterface, VoiceRecorderOperateInterface {
    private boolean recordVoiceBegin;

    private int width;
    private int height;
    private int recordTime;
    private int actualRecordTime;

    private String className;
    private String tempVoicePcmUrl;
    private String musicFileUrl;
    private String decodeFileUrl;
    private String composeVoiceUrl;

    private TextView recordHintTextView;
    private TextView recordDurationView;

    private Button recordVoiceButton;
    private Button composeVoiceButton;
    private Button deleteVoiceButton;
    private Button playComposeVoiceButton;

    private ProgressBar composeProgressBar;

    private static MainActivity instance;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        init(R.layout.activity_main);
    }

    private void init(int layoutId) {
        setContentView(layoutId);

        width = getResources().getDisplayMetrics().widthPixels;
        height = getResources().getDisplayMetrics().heightPixels;

        bindView();
        initView();
        initData();

        className = getClass().getSimpleName();

        instance = this;
    }

    public void bindView() {
        recordHintTextView = (TextView) findViewById(R.id.recordHintTextView);
        recordDurationView = (TextView) findViewById(R.id.recordDurationView);

        recordVoiceButton = (Button) findViewById(R.id.recordVoiceButton);
        composeVoiceButton = (Button) findViewById(R.id.composeVoiceButton);
        deleteVoiceButton = (Button) findViewById(R.id.deleteVoiceButton);
        playComposeVoiceButton = (Button) findViewById(R.id.playComposeVoiceButton);

        composeProgressBar = (ProgressBar) findViewById(R.id.composeProgressBar);
    }

    public void initView() {
        composeProgressBar.getLayoutParams().width = (int) (width * 0.72);
    }

    public void initData() {
        recordTime = 0;

        tempVoicePcmUrl = Variable.StorageDirectoryPath + "tempVoice.pcm";
        musicFileUrl = Variable.StorageDirectoryPath + "musicFile.mp3";
        decodeFileUrl = Variable.StorageDirectoryPath + "decodeFile.pcm";
        composeVoiceUrl = Variable.StorageDirectoryPath + "composeVoice.mp3";

        initMusicFile();

        recordVoiceButton.setEnabled(true);
        deleteVoiceButton.setEnabled(false);
        composeVoiceButton.setEnabled(false);
        playComposeVoiceButton.setEnabled(false);

        recordVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (recordVoiceBegin) {
                    VoiceFunction.StopRecordVoice();

                    recordHintTextView.setText("已结束录音");
                    recordVoiceButton.setText("录音");
                } else {
                    VoiceFunction.StartRecordVoice(tempVoicePcmUrl,
                            instance);

                    recordHintTextView.setText("松开结束录音");
                    recordVoiceButton.setText("结束录音");
                }
            }
        });

        playComposeVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (FileFunction.IsFileExists(composeVoiceUrl)) {
                    VoiceFunction
                            .PlayToggleVoice(composeVoiceUrl, instance);
                }
            }
        });

        composeVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                compose();
            }
        });

        deleteVoiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actualRecordTime = 0;

                recordVoiceButton.setEnabled(true);
                deleteVoiceButton.setEnabled(false);
                composeVoiceButton.setEnabled(false);
                playComposeVoiceButton.setEnabled(false);

                VoiceFunction.StopVoice();

                recordHintTextView.setText("按下开始录音");
            }
        });

        recordHintTextView.setText("按下开始录音");
    }

    private void initMusicFile() {
        byte buffer[] = new byte[1024];

        InputStream inputStream = null;
        FileOutputStream fileOutputStream = FileFunction.GetFileOutputStreamFromFile(musicFileUrl);

        try {
            inputStream = getResources().openRawResource(R.raw.test);

            if (fileOutputStream != null) {
                while (inputStream.read(buffer) > -1) {
                    fileOutputStream.write(buffer);
                }
            }
        } catch (Exception e) {
            LogFunction.error("write file异常", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    LogFunction.error("close file异常", e);
                }
            }

            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    LogFunction.error("close file异常", e);
                }
            }

            inputStream = null;
            fileOutputStream = null;
        }
    }

    private void goRecordSuccessState() {
        recordVoiceBegin = false;

        recordVoiceButton.setEnabled(false);
        deleteVoiceButton.setEnabled(true);
        composeVoiceButton.setEnabled(true);
        playComposeVoiceButton.setEnabled(false);

        recordDurationView.setText(CommonFunction.FormatRecordTime(actualRecordTime));

        recordHintTextView.setText("完成录音");
    }

    private void goRecordFailState() {
        recordVoiceBegin = false;

        recordDurationView.setVisibility(View.INVISIBLE);

        recordVoiceButton.setEnabled(true);

        recordHintTextView.setText("点击开始录音");
    }

    private void compose() {
        composeProgressBar.setProgress(0);

        recordHintTextView.setText("合成开始");

        composeVoiceButton.setEnabled(false);
        deleteVoiceButton.setEnabled(false);

        composeProgressBar.setVisibility(View.VISIBLE);

        AudioFunction.DecodeMusicFile(musicFileUrl, decodeFileUrl, 0,
                actualRecordTime + Constant.MusicCutEndOffset, this);
    }

    @Override
    public void recordVoiceBegin() {
        VoiceFunction.StopVoice();

        if (!recordVoiceBegin) {
            recordVoiceBegin = true;

            recordTime = 0;

            recordDurationView.setText(CommonFunction.FormatRecordTime(recordTime));

            recordDurationView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void recordVoiceStateChanged(int volume, long recordDuration) {
        if (recordDuration > 0) {
            recordTime = (int) (recordDuration / Constant.OneSecond);

            recordDurationView.setText(CommonFunction.FormatRecordTime(recordTime));
        }
    }

    @Override
    public void prepareGiveUpRecordVoice() {

    }

    @Override
    public void recoverRecordVoice() {

    }

    @Override
    public void giveUpRecordVoice() {

    }

    @Override
    public void recordVoiceFail() {
        if (recordVoiceBegin) {
            if (actualRecordTime != 0) {
                goRecordSuccessState();
            } else {
                goRecordFailState();
            }
        }
    }

    @Override
    public void recordVoiceFinish() {
        if (recordVoiceBegin) {
            actualRecordTime = recordTime;

            goRecordSuccessState();
        }
    }

    @Override
    public void playVoiceBegin() {
//        playVoiceButton.setImageResource(R.drawable.selector_record_voice_pause);
    }

    @Override
    public void playVoiceFail() {
//        playVoiceButton.setImageResource(R.drawable.selector_record_voice_play);
    }

    @Override
    public void playVoiceFinish() {
//        playVoiceButton.setImageResource(R.drawable.selector_record_voice_play);
    }

    @Override
    public void updateDecodeProgress(int decodeProgress) {
        composeProgressBar.setProgress(
                decodeProgress * Constant.MaxDecodeProgress / Constant.NormalMaxProgress);
    }

    @Override
    public void decodeSuccess() {
        composeProgressBar.setProgress(Constant.MaxDecodeProgress);

        AudioFunction.BeginComposeAudio(tempVoicePcmUrl, decodeFileUrl, composeVoiceUrl, false,
                Constant.VoiceWeight, Constant.VoiceBackgroundWeight,
                -1 * Constant.MusicCutEndOffset / 2 * Constant.RecordDataNumberInOneSecond, this);
    }

    @Override
    public void decodeFail() {
        recordHintTextView.setText("解码失败,请您检查网络后，再次尝试");

        recordVoiceButton.setEnabled(false);
        deleteVoiceButton.setEnabled(true);
        composeVoiceButton.setEnabled(true);
        playComposeVoiceButton.setEnabled(false);

        composeProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void updateComposeProgress(int composeProgress) {
        composeProgressBar.setProgress(
                composeProgress * (Constant.NormalMaxProgress - Constant.MaxDecodeProgress) /
                        Constant.NormalMaxProgress + Constant.MaxDecodeProgress);
    }

    @Override
    public void composeSuccess() {
        recordHintTextView.setText("合成成功，可播放合成语音");

        playComposeVoiceButton.setEnabled(true);
        deleteVoiceButton.setEnabled(true);

        composeProgressBar.setVisibility(View.GONE);

        CommonFunction.showToast("合成成功", className);
    }

    @Override
    public void composeFail() {
        recordHintTextView.setText("合成失败");

        composeVoiceButton.setEnabled(true);
        deleteVoiceButton.setEnabled(true);

        composeProgressBar.setVisibility(View.GONE);

        CommonFunction.showToast("合成失败", className);
    }
}
