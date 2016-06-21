package com.Tool.Function;

import zty.composeaudio.Tool.Interface.VoicePlayerInterface;
import zty.composeaudio.Tool.Interface.VoiceRecorderOperateInterface;
import zty.composeaudio.Tool.Player.VoicePlayerEngine;
import zty.composeaudio.Tool.Recorder.RecorderEngine;

/**
 * Created by zhengtongyu on 16/5/29.
 */
public class VoiceFunction {
    public static boolean IsRecordingVoice() {
        return RecorderEngine.getInstance().IsRecording();
    }

    public synchronized static void StartRecordVoice(String recordFileUrl,
                                                     VoiceRecorderOperateInterface voiceRecorderOperateInterface) {
        RecorderEngine.getInstance()
                .startRecordVoice(recordFileUrl, voiceRecorderOperateInterface);
    }

    public static void StopRecordVoice() {
        RecorderEngine.getInstance().stopRecordVoice();
    }

    public synchronized static void PrepareGiveUpRecordVoice(boolean fromHand) {
        RecorderEngine.getInstance().prepareGiveUpRecordVoice(fromHand);
    }

    public synchronized static void RecoverRecordVoice(boolean fromHand) {
        RecorderEngine.getInstance().recoverRecordVoice(fromHand);
    }

    public synchronized static void GiveUpRecordVoice(boolean fromHand) {
        RecorderEngine.getInstance().giveUpRecordVoice(fromHand);
    }

    public synchronized static String getPlayingUrl() {
        return VoicePlayerEngine.getInstance().getPlayingUrl();
    }

    public synchronized static boolean IsPlaying() {
        return VoicePlayerEngine.getInstance().isPlaying();
    }

    public synchronized static boolean IsPlayVoice(String fileUrl) {
        if (CommonFunction.isEmpty(fileUrl)) {
            return false;
        }

        return getPlayingUrl().equals(fileUrl);
    }

    public synchronized static boolean IsPlayingVoice(String fileUrl) {
        if (IsPlayVoice(fileUrl)) {
            return VoicePlayerEngine.getInstance().isPlaying();
        } else {
            return false;
        }
    }

    public synchronized static void PlayToggleVoice(String fileUrl,
                                                    VoicePlayerInterface voicePlayerInterface) {
        if (IsPlayVoice(fileUrl)) {
            VoicePlayerEngine.getInstance().stopVoice();
        } else {
            VoicePlayerEngine.getInstance()
                    .playVoice(fileUrl, voicePlayerInterface);
        }
    }

    public synchronized static void StopVoice() {
        VoicePlayerEngine.getInstance().stopVoice();
    }

    public synchronized static void StopVoice(String fileUrl) {
        if (getPlayingUrl().equals(fileUrl)) {
            VoicePlayerEngine.getInstance().stopVoice();
        }
    }
}