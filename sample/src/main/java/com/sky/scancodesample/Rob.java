package com.sky.scancodesample;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.QiSDK;
import com.aldebaran.qi.sdk.RobotLifecycleCallbacks;
import com.aldebaran.qi.sdk.builder.ChatBuilder;
import com.aldebaran.qi.sdk.design.activity.RobotActivity;
import com.aldebaran.qi.sdk.object.conversation.Chat;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.skytech.scancode.CaptureActivity;
import com.skytech.scancode.ScanCallback;

public class Rob extends RobotActivity implements RobotLifecycleCallbacks {

    private static final String TAG = "Rob";
    private QiContext qiContext;
    private String receivePhrase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rob);
        QiSDK.register(this, this);
    }

    @Override
    public void onRobotFocusGained(QiContext qiContext) {
        this.qiContext = qiContext;
        //0
        MyBaseChatbot myBaseChatbot = new MyBaseChatbot(qiContext);

        //1
        Chat chat = ChatBuilder.with(qiContext)
                .withChatbot(myBaseChatbot)
                .build();

        chat.addOnNormalReplyFoundForListener(new Chat.OnNormalReplyFoundForListener() {
            @Override
            public void onNormalReplyFoundFor(Phrase input) {
                receivePhrase = input.getText();
                if (receivePhrase.contains("我要扫码")) {
                    CaptureActivity.setCallback(new ScanCallback() {
                        @Override
                        public void result(String uri) {
                            Toast.makeText(Rob.this, uri, Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(Rob.this, SayHelloActivity.class)
                                    .putExtra("uri", uri);
                            startActivity(intent);
                            CaptureActivity.getInstance().finish();
                        }
                    });
                    startActivity(new Intent(Rob.this, CaptureActivity.class));
                }
            }
        });
        //2
        chat.async().run();
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
}
