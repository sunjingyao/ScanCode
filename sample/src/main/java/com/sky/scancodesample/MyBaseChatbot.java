package com.sky.scancodesample;

import android.util.Log;

import com.aldebaran.qi.Future;
import com.aldebaran.qi.sdk.QiContext;
import com.aldebaran.qi.sdk.builder.SayBuilder;
import com.aldebaran.qi.sdk.object.conversation.BaseChatbot;
import com.aldebaran.qi.sdk.object.conversation.BaseChatbotReaction;
import com.aldebaran.qi.sdk.object.conversation.Phrase;
import com.aldebaran.qi.sdk.object.conversation.ReplyPriority;
import com.aldebaran.qi.sdk.object.conversation.Say;
import com.aldebaran.qi.sdk.object.conversation.SpeechEngine;
import com.aldebaran.qi.sdk.object.conversation.StandardReplyReaction;
import com.aldebaran.qi.sdk.object.locale.Locale;

import java.util.concurrent.ExecutionException;

public class MyBaseChatbot extends BaseChatbot {

    private static final String TAG = "MyBaseChatbot";
    private String receivePhrase;


    protected MyBaseChatbot(QiContext context) {
        super(context);
    }

    @Override
    public StandardReplyReaction replyTo(Phrase phrase, Locale locale) {
        //用户给机器人的内容：你认识我吗
        receivePhrase = phrase.getText();
        if (receivePhrase.contains("我要扫码")){
            return new StandardReplyReaction(
                    new MyChatbotReaction(getQiContext(),"请扫码吧"),
                    ReplyPriority.NORMAL);
        }else {
            return new StandardReplyReaction(
                    new MyChatbotReaction(getQiContext(), ""),
                    ReplyPriority.FALLBACK);
        }
    }


    @Override
    public void acknowledgeHeard(Phrase phrase, Locale locale) {
        //机器人听到的最后一个短语
        super.acknowledgeHeard(phrase, locale);
        Log.i(TAG, "Last phrase heard by the robot and whose selected answer is not mine: " + phrase.getText());
    }

    @Override
    public void acknowledgeSaid(Phrase phrase, Locale locale) {
        //机器人说的最后一个短语
        super.acknowledgeSaid(phrase, locale);
        Log.i(TAG, "Another chatbot answered: " + phrase.getText());
    }


    //返回类
    class MyChatbotReaction extends BaseChatbotReaction {
        private String answer;
        private Future<Void> fsay;

        MyChatbotReaction(QiContext context, String answer) {
            super(context);
            this.answer = answer;//机器人的回答
        }

        @Override
        public void runWith(SpeechEngine speechEngine) {
            Say say = SayBuilder.with(speechEngine)
                    .withText(answer)
                    .build();

            fsay = say.async().run();

            try {
                fsay.get(); // Do not leave the method before the actions are done
            } catch (ExecutionException e) {
                Log.e(TAG, "Error during say", e);
            }

        }

        @Override
        public void stop() {
            fsay.cancel(true);
        }
    }


}
