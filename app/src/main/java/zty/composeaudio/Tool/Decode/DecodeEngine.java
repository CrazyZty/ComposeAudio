package zty.composeaudio.Tool.Decode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import com.Tool.Global.Constant;
import com.Tool.Global.Variable;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.Tool.Function.CommonFunction;
import com.Tool.Function.FileFunction;
import com.Tool.Function.LogFunction;

import vavi.sound.pcm.resampling.ssrc.SSRC;
import zty.composeaudio.Tool.Interface.DecodeOperateInterface;

/**
 * Created by 郑童宇 on 2016/03/04.
 */
public class DecodeEngine {
    private static DecodeEngine instance;

    private DecodeEngine() {
    }

    public static DecodeEngine getInstance() {
        if (instance == null) {
            synchronized (DecodeEngine.class) {
                if (instance == null) {
                    instance = new DecodeEngine();
                }
            }
        }

        return instance;
    }

    public void beginDecodeMusicFile(String musicFileUrl, String decodeFileUrl, int startSecond,
                                     int endSecond,
                                     final DecodeOperateInterface decodeOperateInterface) {
        Handler handler = new Handler(Looper.getMainLooper());

        final boolean decodeResult =
                decodeMusicFile(musicFileUrl, decodeFileUrl, startSecond, endSecond, handler,
                        decodeOperateInterface);

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (decodeResult) {
                    decodeOperateInterface.decodeSuccess();
                } else {
                    decodeOperateInterface.decodeFail();
                }
            }
        });
    }

    private boolean decodeMusicFile(String musicFileUrl, String decodeFileUrl, int startSecond,
                                    int endSecond,
                                    Handler handler,
                                    DecodeOperateInterface decodeOperateInterface) {
        int sampleRate = 0;
        int channelCount = 0;

        long duration = 0;

        String mime = null;

        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaFormat mediaFormat = null;
        MediaCodec mediaCodec = null;

        try {
            mediaExtractor.setDataSource(musicFileUrl);
        } catch (Exception e) {
            LogFunction.error("设置解码音频文件路径错误", e);
            return false;
        }

        mediaFormat = mediaExtractor.getTrackFormat(0);
        sampleRate = mediaFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) : 44100;
        channelCount = mediaFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
        duration = mediaFormat.containsKey(MediaFormat.KEY_DURATION) ? mediaFormat.getLong
                (MediaFormat.KEY_DURATION)
                : 0;
        mime = mediaFormat.containsKey(MediaFormat.KEY_MIME) ? mediaFormat.getString(MediaFormat
                .KEY_MIME) : "";

        LogFunction.log("歌曲信息",
                "Track info: mime:" + mime + " 采样率sampleRate:" + sampleRate + " channels:" +
                        channelCount + " duration:" + duration);

        if (CommonFunction.isEmpty(mime) || !mime.startsWith("audio/")) {
            LogFunction.error("解码文件不是音频文件", "mime:" + mime);
            return false;
        }

        if (mime.equals("audio/ffmpeg")) {
            mime = "audio/mpeg";
            mediaFormat.setString(MediaFormat.KEY_MIME, mime);
        }

        try {
            mediaCodec = MediaCodec.createDecoderByType(mime);

            mediaCodec.configure(mediaFormat, null, null, 0);
        } catch (Exception e) {
            LogFunction.error("解码器configure出错", e);
            return false;
        }

        getDecodeData(mediaExtractor, mediaCodec, decodeFileUrl, sampleRate, channelCount,
                startSecond,
                endSecond, handler, decodeOperateInterface);
        return true;
    }

    private void getDecodeData(MediaExtractor mediaExtractor, MediaCodec mediaCodec, String
            decodeFileUrl, int
                                       sampleRate,
                               int channelCount, int startSecond, int endSecond,
                               Handler handler,
                               final DecodeOperateInterface decodeOperateInterface) {
        boolean decodeInputEnd = false;
        boolean decodeOutputEnd = false;

        int sampleDataSize;
        int inputBufferIndex;
        int outputBufferIndex;
        int byteNumber;

        long decodeNoticeTime = System.currentTimeMillis();
        long decodeTime;
        long presentationTimeUs = 0;

        final long timeOutUs = 100;
        final long startMicroseconds = startSecond * 1000 * 1000;
        final long endMicroseconds = endSecond * 1000 * 1000;

        ByteBuffer[] inputBuffers;
        ByteBuffer[] outputBuffers;

        ByteBuffer sourceBuffer;
        ByteBuffer targetBuffer;

        MediaFormat outputFormat = mediaCodec.getOutputFormat();

        MediaCodec.BufferInfo bufferInfo;

        byteNumber =
                (outputFormat.containsKey("bit-width") ? outputFormat.getInteger("bit-width") :
                        0) / 8;

        mediaCodec.start();

        inputBuffers = mediaCodec.getInputBuffers();
        outputBuffers = mediaCodec.getOutputBuffers();

        mediaExtractor.selectTrack(0);

        bufferInfo = new MediaCodec.BufferInfo();

        BufferedOutputStream bufferedOutputStream = FileFunction
                .GetBufferedOutputStreamFromFile(decodeFileUrl);

        while (!decodeOutputEnd) {
            if (decodeInputEnd) {
                return;
            }

            decodeTime = System.currentTimeMillis();

            if (decodeTime - decodeNoticeTime > Constant.OneSecond) {
                final int decodeProgress =
                        (int) ((presentationTimeUs - startMicroseconds) * Constant
                                .NormalMaxProgress /
                                endMicroseconds);

                if (decodeProgress > 0) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            decodeOperateInterface.updateDecodeProgress(decodeProgress);
                        }
                    });
                }

                decodeNoticeTime = decodeTime;
            }

            try {
                inputBufferIndex = mediaCodec.dequeueInputBuffer(timeOutUs);

                if (inputBufferIndex >= 0) {
                    sourceBuffer = inputBuffers[inputBufferIndex];

                    sampleDataSize = mediaExtractor.readSampleData(sourceBuffer, 0);

                    if (sampleDataSize < 0) {
                        decodeInputEnd = true;
                        sampleDataSize = 0;
                    } else {
                        presentationTimeUs = mediaExtractor.getSampleTime();
                    }

                    mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleDataSize,
                            presentationTimeUs,
                            decodeInputEnd ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                    if (!decodeInputEnd) {
                        mediaExtractor.advance();
                    }
                } else {
                    LogFunction.error("inputBufferIndex", "" + inputBufferIndex);
                }

                // decode to PCM and push it to the AudioTrack player
                outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, timeOutUs);

                if (outputBufferIndex < 0) {
                    switch (outputBufferIndex) {
                        case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                            outputBuffers = mediaCodec.getOutputBuffers();
                            LogFunction.error("MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED",
                                    "[AudioDecoder]output buffers have changed.");
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            outputFormat = mediaCodec.getOutputFormat();

                            sampleRate = outputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                                    outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE) :
                                    sampleRate;
                            channelCount = outputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ?
                                    outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT) :
                                    channelCount;
                            byteNumber = (outputFormat.containsKey("bit-width") ? outputFormat
                                    .getInteger
                                            ("bit-width") : 0) / 8;

                            LogFunction.error("MediaCodec.INFO_OUTPUT_FORMAT_CHANGED",
                                    "[AudioDecoder]output format has changed to " +
                                            mediaCodec.getOutputFormat());
                            break;
                        default:
                            LogFunction.error("error",
                                    "[AudioDecoder] dequeueOutputBuffer returned " +
                                            outputBufferIndex);
                            break;
                    }
                    continue;
                }

                targetBuffer = outputBuffers[outputBufferIndex];

                byte[] sourceByteArray = new byte[bufferInfo.size];

                targetBuffer.get(sourceByteArray);
                targetBuffer.clear();

                mediaCodec.releaseOutputBuffer(outputBufferIndex, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    decodeOutputEnd = true;
                }

                if (sourceByteArray.length > 0 && bufferedOutputStream != null) {
                    if (presentationTimeUs < startMicroseconds) {
                        continue;
                    }

                    byte[] convertByteNumberByteArray = ConvertByteNumber(byteNumber, Constant
                                    .RecordByteNumber,
                            sourceByteArray);

                    byte[] resultByteArray =
                            ConvertChannelNumber(channelCount, Constant.RecordChannelNumber,
                                    Constant.RecordByteNumber,
                                    convertByteNumberByteArray);

                    try {
                        bufferedOutputStream.write(resultByteArray);
                    } catch (Exception e) {
                        LogFunction.error("输出解压音频数据异常", e);
                    }
                }

                if (presentationTimeUs > endMicroseconds) {
                    break;
                }
            } catch (Exception e) {
                LogFunction.error("getDecodeData异常", e);
            }
        }

        if (bufferedOutputStream != null) {
            try {
                bufferedOutputStream.close();
            } catch (IOException e) {
                LogFunction.error("关闭bufferedOutputStream异常", e);
            }
        }

        if (sampleRate != Constant.RecordSampleRate) {
            Resample(sampleRate, decodeFileUrl);
        }

        if (mediaCodec != null) {
            mediaCodec.stop();
            mediaCodec.release();
        }

        if (mediaExtractor != null) {
            mediaExtractor.release();
        }
    }

    private static void Resample(int sampleRate, String decodeFileUrl) {
        String newDecodeFileUrl = decodeFileUrl + "new";

        try {
            FileInputStream fileInputStream =
                    new FileInputStream(new File(decodeFileUrl));
            FileOutputStream fileOutputStream =
                    new FileOutputStream(new File(newDecodeFileUrl));

            new SSRC(fileInputStream, fileOutputStream, sampleRate, Constant.RecordSampleRate,
                    Constant.RecordByteNumber, Constant.RecordByteNumber, 1, Integer.MAX_VALUE,
                    0, 0, true);

            fileInputStream.close();
            fileOutputStream.close();

            FileFunction.RenameFile(newDecodeFileUrl, decodeFileUrl);
        } catch (IOException e) {
            LogFunction.error("关闭bufferedOutputStream异常", e);
        }
    }

    private static byte[] ConvertByteNumber(int sourceByteNumber, int outputByteNumber, byte[]
            sourceByteArray) {
        if (sourceByteNumber == outputByteNumber) {
            return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceByteNumber) {
            case 1:
                switch (outputByteNumber) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte resultByte[];

                        for (int index = 0; index < sourceByteArrayLength; index += 1) {
                            resultByte = CommonFunction.GetBytes((short) (sourceByteArray[index]
                                    * 256), Variable
                                    .isBigEnding);

                            byteArray[2 * index] = resultByte[0];
                            byteArray[2 * index + 1] = resultByte[1];
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputByteNumber) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        for (int index = 0; index < outputByteArrayLength; index += 1) {
                            byteArray[index] = (byte) (CommonFunction.GetShort(sourceByteArray[2
                                            * index],
                                    sourceByteArray[2 * index + 1], Variable.isBigEnding) / 256);
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }

    private static byte[] ConvertChannelNumber(int sourceChannelCount, int outputChannelCount,
                                               int byteNumber,
                                               byte[] sourceByteArray) {
        if (sourceChannelCount == outputChannelCount) {
            return sourceByteArray;
        }

        switch (byteNumber) {
            case 1:
            case 2:
                break;
            default:
                return sourceByteArray;
        }

        int sourceByteArrayLength = sourceByteArray.length;

        byte[] byteArray;

        switch (sourceChannelCount) {
            case 1:
                switch (outputChannelCount) {
                    case 2:
                        byteArray = new byte[sourceByteArrayLength * 2];

                        byte firstByte;
                        byte secondByte;

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < sourceByteArrayLength; index += 1) {
                                    firstByte = sourceByteArray[index];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = firstByte;
                                }
                                break;
                            case 2:
                                for (int index = 0; index < sourceByteArrayLength; index += 2) {
                                    firstByte = sourceByteArray[index];
                                    secondByte = sourceByteArray[index + 1];

                                    byteArray[2 * index] = firstByte;
                                    byteArray[2 * index + 1] = secondByte;
                                    byteArray[2 * index + 2] = firstByte;
                                    byteArray[2 * index + 3] = secondByte;
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
            case 2:
                switch (outputChannelCount) {
                    case 1:
                        int outputByteArrayLength = sourceByteArrayLength / 2;

                        byteArray = new byte[outputByteArrayLength];

                        switch (byteNumber) {
                            case 1:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    short averageNumber =
                                            (short) ((short) sourceByteArray[2 * index] + (short)
                                                    sourceByteArray[2 *
                                                            index + 1]);
                                    byteArray[index] = (byte) (averageNumber >> 1);
                                }
                                break;
                            case 2:
                                for (int index = 0; index < outputByteArrayLength; index += 2) {
                                    byte resultByte[] = CommonFunction.AverageShortByteArray
                                            (sourceByteArray[2 * index],
                                                    sourceByteArray[2 * index + 1],
                                                    sourceByteArray[2 *
                                                            index + 2],
                                                    sourceByteArray[2 * index + 3], Variable
                                                            .isBigEnding);

                                    byteArray[index] = resultByte[0];
                                    byteArray[index + 1] = resultByte[1];
                                }
                                break;
                        }

                        return byteArray;
                }
                break;
        }

        return sourceByteArray;
    }
}
