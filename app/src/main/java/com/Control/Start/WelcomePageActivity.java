package com.Control.Start;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.Tool.Common.CommonApplication;
import com.Tool.Common.CommonThreadPool;

import zty.composeaudio.Control.Main.MainActivity;
import zty.composeaudio.R;

/**
 * Created by 郑童宇 on 2016/05/24.
 */
public class WelcomePageActivity extends Activity {
    private Intent intent;

    private Handler handler;

    private CommonApplication application;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        init(R.layout.activity_welcome_page);
    }

    private void init(int layoutResourceId) {
        setContentView(layoutResourceId);
        initData();
    }

    private void initData() {
        application = CommonApplication.getInstance();

        handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                application.initialiseInUIThread();

                startActivity(intent);

                finish();
            }
        };

        CommonThreadPool.getThreadPool().addFixedTask(initialiseThread);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        super.onBackPressed();
    }

    private void begin() {
        intent = new Intent(this, MainActivity.class);

        Message.obtain(handler).sendToTarget();
    }

    private Runnable initialiseThread = new Runnable() {
        @Override
        public void run() {
            application.initialise();
            begin();
        }
    };
}