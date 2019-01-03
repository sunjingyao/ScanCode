package com.sky.scancodesample;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Say;

public class SayHelloActivity extends RobotActivity implements RobotLifecycleCallbacks {

    private String answer;
    private String uri;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.hello_activity);
        QiSDK.register(this, this);
        uri = getIntent().getStringExtra("uri");
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        if (uri != null) {
            getSay(uri);
        }

        Say say = SayBuilder.with(qiContext)
                .withText(answer)
                .build();
        say.run();
        finish();
    }

    @Override
    public void onRobotFocusLost() {

    }

    @Override
    public void onRobotFocusRefused(String reason) {

    }

    @Override
    protected void onDestroy() {
        QiSDK.unregister(this, this);
        super.onDestroy();
    }

    private void getSay(String textToSay) {
            answer = "嗨！二维码的内容为" + textToSay ;
    }
}
