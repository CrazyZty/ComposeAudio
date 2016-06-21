package zty.composeaudio.Tool.Recorder.Mp3;

import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.czt.mp3recorder.util.LameUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by zhengtongyu on 16/5/29.
 */
public class DataEncodeThread  extends Thread implements AudioRecord.OnRecordPositionUpdateListener {
    private byte[] mp3Buffer;

    public static final int PROCESS_STOP = 1;

    private StopHandler mHandler;

    private FileOutputStream mFileOutputStream;

    private CountDownLatch mHandlerInitLatch = new CountDownLatch(1);

    private List<Task> mTasks = Collections.synchronizedList(new ArrayList<Task>());

    /**
     * @author buihong_ha
     * @see <a>https://groups.google.com/forum/?fromgroups=#!msg/android-developers/1aPZXZG6kWk/lIYDavGYn5UJ</a>
     */
    static class StopHandler extends Handler {
        WeakReference<DataEncodeThread> encodeThread;

        public StopHandler(DataEncodeThread encodeThread) {
            this.encodeThread = new WeakReference<>(encodeThread);
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == PROCESS_STOP) {
                DataEncodeThread threadRef = encodeThread.get();
                //处理缓冲区中的数据
                while (threadRef.processData() > 0)
                    ;
                // Cancel any event left in the queue
                removeCallbacksAndMessages(null);
                threadRef.flushAndRelease();
                getLooper().quit();
            }

            super.handleMessage(msg);
        }
    }

    /**
     * Constructor
     *
     * @param file       file
     * @param bufferSize bufferSize
     * @throws FileNotFoundException
     */
    public DataEncodeThread(File file, int bufferSize) throws FileNotFoundException {
        this.mFileOutputStream = new FileOutputStream(file);
        mp3Buffer = new byte[(int) (7200 + (bufferSize * 2 * 1.25))];
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new StopHandler(this);
        mHandlerInitLatch.countDown();
        Looper.loop();
    }

    /**
     * Return the handler attach to this thread
     */
    public Handler getHandler() {
        try {
            mHandlerInitLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return mHandler;
    }

    @Override
    public void onMarkerReached(AudioRecord recorder) {
        // Do nothing
    }

    @Override
    public void onPeriodicNotification(AudioRecord recorder) {
        processData();
    }

    /**
     * 从缓冲区中读取并处理数据，使用lame编码MP3
     *
     * @return 从缓冲区中读取的数据的长度
     * 缓冲区中没有数据时返回0
     */
    private int processData() {
        if (mTasks.size() > 0) {
            Task task = mTasks.remove(0);
            short[] buffer = task.getData();
            int readSize = task.getReadSize();
            int encodedSize = LameUtil.encode(buffer, buffer, readSize, mp3Buffer);
            if (encodedSize > 0) {
                try {
                    mFileOutputStream.write(mp3Buffer, 0, encodedSize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return readSize;
        }
        return 0;
    }

    /**
     * Flush all data left in lame buffer to file
     */
    private void flushAndRelease() {
        //将MP3结尾信息写入buffer中
        final int flushResult = LameUtil.flush(mp3Buffer);
        if (flushResult > 0) {
            try {
                mFileOutputStream.write(mp3Buffer, 0, flushResult);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (mFileOutputStream != null) {
                    try {
                        mFileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                LameUtil.close();
            }
        }
    }

    public void addTask(short[] rawData, int readSize) {
        mTasks.add(new Task(rawData, readSize));
    }

    private class Task {
        private short[] rawData;
        private int readSize;

        public Task(short[] rawData, int readSize) {
            this.rawData = rawData.clone();
            this.readSize = readSize;
        }

        public short[] getData() {
            return rawData;
        }

        public int getReadSize() {
            return readSize;
        }
    }
}
