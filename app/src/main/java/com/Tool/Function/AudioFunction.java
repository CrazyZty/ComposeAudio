package com.Tool.Function;

import android.os.Handler;
import android.os.Looper;

import com.czt.mp3recorder.util.LameUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import zty.composeaudio.Tool.Decode.DecodeEngine;

import com.Tool.Global.Constant;
import com.Tool.Global.Variable;

import zty.composeaudio.Tool.Interface.ComposeAudioInterface;
import zty.composeaudio.Tool.Interface.DecodeOperateInterface;

/**
 * Created by zhengtongyu on 16/5/29.
 */
public class AudioFunction {
    public static void DecodeMusicFile(final String musicFileUrl, final String decodeFileUrl, final int startSecond,
                                       final int endSecond,
                                       final DecodeOperateInterface decodeOperateInterface) {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                DecodeEngine.getInstance().beginDecodeMusicFile(musicFileUrl, decodeFileUrl, startSecond, endSecond,
                        decodeOperateInterface);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        LogFunction.error("异常观察", e.toString());
                    }

                    @Override
                    public void onNext(String string) {
                    }
                });
    }

    public static void BeginComposeAudio(final String firstAudioPath, final String secondAudioPath,
                                         final String composeFilePath, final boolean deleteSource,
                                         final float firstAudioWeight,
                                         final float secondAudioWeight, final int audioOffset,
                                         final ComposeAudioInterface composeAudioInterface) {
        Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                ComposeAudio(firstAudioPath, secondAudioPath, composeFilePath, deleteSource,
                        firstAudioWeight, secondAudioWeight, audioOffset, composeAudioInterface);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<String>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                    }

                    @Override
                    public void onNext(String string) {
                    }
                });
    }

    public static void ComposeAudio(String firstAudioFilePath, String secondAudioFilePath,
                                    String composeAudioFilePath, boolean deleteSource,
                                    float firstAudioWeight, float secondAudioWeight,
                                    int audioOffset,
                                    final ComposeAudioInterface composeAudioInterface) {
        boolean firstAudioFinish = false;
        boolean secondAudioFinish = false;

        byte[] firstAudioByteBuffer;
        byte[] secondAudioByteBuffer;
        byte[] mp3Buffer;

        short resultShort;
        short[] outputShortArray;

        int index;
        int firstAudioReadNumber;
        int secondAudioReadNumber;
        int outputShortArrayLength;
        final int byteBufferSize = 1024;

        firstAudioByteBuffer = new byte[byteBufferSize];
        secondAudioByteBuffer = new byte[byteBufferSize];
        mp3Buffer = new byte[(int) (7200 + (byteBufferSize * 1.25))];

        outputShortArray = new short[byteBufferSize / 2];

        Handler handler = new Handler(Looper.getMainLooper());

        FileInputStream firstAudioInputStream = FileFunction.GetFileInputStreamFromFile
                (firstAudioFilePath);
        FileInputStream secondAudioInputStream = FileFunction.GetFileInputStreamFromFile
                (secondAudioFilePath);
        FileOutputStream composeAudioOutputStream = FileFunction.GetFileOutputStreamFromFile
                (composeAudioFilePath);

        LameUtil.init(Constant.RecordSampleRate, Constant.LameBehaviorChannelNumber,
                Constant.BehaviorSampleRate, Constant.LameBehaviorBitRate, Constant.LameMp3Quality);

        try {
            while (!firstAudioFinish && !secondAudioFinish) {
                index = 0;

                if (audioOffset < 0) {
                    secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                    outputShortArrayLength = secondAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                    }

                    audioOffset += secondAudioReadNumber;

                    if (secondAudioReadNumber < 0) {
                        secondAudioFinish = true;
                        break;
                    }

                    if (audioOffset >= 0) {
                        break;
                    }
                } else {
                    firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);

                    outputShortArrayLength = firstAudioReadNumber / 2;

                    for (; index < outputShortArrayLength; index++) {
                        resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                        outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                    }

                    audioOffset -= firstAudioReadNumber;

                    if (firstAudioReadNumber < 0) {
                        firstAudioFinish = true;
                        break;
                    }

                    if (audioOffset <= 0) {
                        break;
                    }
                }

                if (outputShortArrayLength > 0) {
                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
                            outputShortArrayLength, mp3Buffer);

                    if (encodedSize > 0) {
                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
                    }
                }
            }

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.updateComposeProgress(20);
                    }
                }
            });

            while (!firstAudioFinish || !secondAudioFinish) {
                index = 0;

                firstAudioReadNumber = firstAudioInputStream.read(firstAudioByteBuffer);
                secondAudioReadNumber = secondAudioInputStream.read(secondAudioByteBuffer);

                int minAudioReadNumber = Math.min(firstAudioReadNumber, secondAudioReadNumber);
                int maxAudioReadNumber = Math.max(firstAudioReadNumber, secondAudioReadNumber);

                if (firstAudioReadNumber < 0) {
                    firstAudioFinish = true;
                }

                if (secondAudioReadNumber < 0) {
                    secondAudioFinish = true;
                }

                int halfMinAudioReadNumber = minAudioReadNumber / 2;

                outputShortArrayLength = maxAudioReadNumber / 2;

                for (; index < halfMinAudioReadNumber; index++) {
                    resultShort = CommonFunction.WeightShort(firstAudioByteBuffer[index * 2],
                            firstAudioByteBuffer[index * 2 + 1], secondAudioByteBuffer[index * 2],
                            secondAudioByteBuffer[index * 2 + 1], firstAudioWeight,
                            secondAudioWeight, Variable.isBigEnding);

                    outputShortArray[index] = resultShort;
                }

                if (firstAudioReadNumber != secondAudioReadNumber) {
                    if (firstAudioReadNumber > secondAudioReadNumber) {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(firstAudioByteBuffer[index * 2],
                                    firstAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * firstAudioWeight);
                        }
                    } else {
                        for (; index < outputShortArrayLength; index++) {
                            resultShort = CommonFunction.GetShort(secondAudioByteBuffer[index * 2],
                                    secondAudioByteBuffer[index * 2 + 1], Variable.isBigEnding);

                            outputShortArray[index] = (short) (resultShort * secondAudioWeight);
                        }
                    }
                }

                if (outputShortArrayLength > 0) {
                    int encodedSize = LameUtil.encode(outputShortArray, outputShortArray,
                            outputShortArrayLength, mp3Buffer);

                    if (encodedSize > 0) {
                        composeAudioOutputStream.write(mp3Buffer, 0, encodedSize);
                    }
                }
            }
        } catch (Exception e) {
            LogFunction.error("ComposeAudio异常", e);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (composeAudioInterface != null) {
                        composeAudioInterface.composeFail();
                    }
                }
            });

            return;
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.updateComposeProgress(50);
                }
            }
        });

        try {
            final int flushResult = LameUtil.flush(mp3Buffer);

            if (flushResult > 0) {
                composeAudioOutputStream.write(mp3Buffer, 0, flushResult);
            }
        } catch (Exception e) {
            LogFunction.error("释放ComposeAudio LameUtil异常", e);
        } finally {
            try {
                composeAudioOutputStream.close();
            } catch (Exception e) {
                LogFunction.error("关闭合成输出音频流异常", e);
            }

            LameUtil.close();
        }

        if (deleteSource) {
            FileFunction.DeleteFile(firstAudioFilePath);
            FileFunction.DeleteFile(secondAudioFilePath);
        }

        try {
            firstAudioInputStream.close();
            secondAudioInputStream.close();
        } catch (IOException e) {
            LogFunction.error("关闭合成输入音频流异常", e);
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (composeAudioInterface != null) {
                    composeAudioInterface.composeSuccess();
                }
            }
        });
    }
}
