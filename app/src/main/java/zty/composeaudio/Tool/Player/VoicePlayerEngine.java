package zty.composeaudio.Tool.Player;

import android.media.MediaPlayer;

import com.Tool.Function.CommonFunction;
import com.Tool.Function.LogFunction;
import com.Tool.Function.UpdateFunction;

import zty.composeaudio.Tool.Data.MusicData;
import zty.composeaudio.Tool.Interface.VoicePlayerInterface;

/**
 * Created by zhengtongyu on 16/5/29.
 */
public class VoicePlayerEngine {
    private int musicPlayerState;

    private String playingUrl;

    private VoicePlayerInterface voicePlayerInterface;

    private MediaPlayer voicePlayer;

    private static VoicePlayerEngine instance;

    private VoicePlayerEngine() {
        musicPlayerState = MusicData.MusicPlayerState.reset;

        voicePlayer = new MediaPlayer();

        voicePlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                start();
            }
        });

        voicePlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (voicePlayerInterface != null) {
                    voicePlayerInterface.playVoiceFinish();
                }

                playingUrl = null;
            }
        });

        voicePlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(final MediaPlayer mediaPlayer, int what, int extra) {
                playFail();

                return true;
            }
        });
    }

    public static VoicePlayerEngine getInstance() {
        if (instance == null) {
            synchronized (VoicePlayerEngine.class) {
                if (instance == null) {
                    instance = new VoicePlayerEngine();
                }
            }
        }

        return instance;
    }

    public synchronized static void Destroy() {
        if (instance != null) {
            instance.destroy();
        }

        instance = null;
    }

    private void destroy() {
        voicePlayer.release();

        voicePlayer = null;
    }

    public void playVoice(String voiceUrl, VoicePlayerInterface voicePlayerInterface) {
        if (CommonFunction.isEmpty(voiceUrl)) {
            UpdateFunction.ShowToastFromThread("不存在语音文件");
            return;
        }

        stopVoice();

        this.voicePlayerInterface = voicePlayerInterface;

        prepareMusic(voiceUrl);
    }

    private synchronized void prepareMusic(String voiceUrl) {
        playingUrl = voiceUrl;

        musicPlayerState = MusicData.MusicPlayerState.preparing;

        try {
            voicePlayer.reset();
            voicePlayer.setDataSource(voiceUrl);
            voicePlayer.prepareAsync();
        } catch (Exception e) {
            playFail();

            UpdateFunction.ShowToastFromThread("播放语音文件失败");

            LogFunction.error("播放语音异常", e);
        }
    }

    private void playFail() {
        if (voicePlayerInterface != null) {
            voicePlayerInterface.playVoiceFail();
        }

        playingUrl = null;
    }

    public boolean isPlaying() {
        return voicePlayer.isPlaying();
    }

    private void start() {
        voicePlayer.start();

        musicPlayerState = MusicData.MusicPlayerState.playing;

        if (voicePlayerInterface != null) {
            voicePlayerInterface.playVoiceBegin();
        }
    }

    private void pause() {
        if (!voicePlayer.isPlaying()) {
            return;
        }

        playingUrl = null;

        voicePlayer.pause();

        musicPlayerState = MusicData.MusicPlayerState.pausing;

        if (voicePlayerInterface != null) {
            voicePlayerInterface.playVoiceFinish();
        }
    }

    private void reset() {
        voicePlayer.reset();
        musicPlayerState = MusicData.MusicPlayerState.reset;

        playingUrl = null;
    }

    public void stopVoice() {
        switch (musicPlayerState) {
            case MusicData.MusicPlayerState.playing:
                pause();
                break;
            case MusicData.MusicPlayerState.preparing:
                reset();
                break;
        }
    }

    public String getPlayingUrl() {
        return playingUrl == null ? "" : playingUrl;
    }
}