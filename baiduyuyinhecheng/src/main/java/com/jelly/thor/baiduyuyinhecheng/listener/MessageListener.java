package com.jelly.thor.baiduyuyinhecheng.listener;

import android.util.Log;
import com.baidu.tts.client.SpeechError;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.jelly.thor.baiduyuyinhecheng.BuildConfig;

/**
 * SpeechSynthesizerListener 简单地实现，仅仅记录日志
 * Created by fujiayi on 2017/5/19.
 */

public class MessageListener implements SpeechSynthesizerListener {
    private static final String TAG = "MessageListener";

    /**
     * 播放开始，每句播放开始都会回调
     */
    @Override
    public void onSynthesizeStart(String utteranceId) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "准备开始合成,序列号:" + utteranceId);
        }
    }

    /**
     * 语音流 16K采样率 16bits编码 单声道 。
     *
     * @param bytes    二进制语音 ，注意可能有空data的情况，可以忽略,合成的音频数据。该音频数据是采样率为16K，2字节精度，单声道的pcm数据。
     * @param progress 如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法和合成到第几个字对应。
     */
    @Override
    public void onSynthesizeDataArrived(String utteranceId, byte[] bytes, int progress) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "合成进度回调, progress：" + progress + ";序列号:" + utteranceId);
        }
    }

    /**
     * 合成正常结束，每句合成正常结束都会回调，如果过程中出错，则回调onError，不再回调此接口
     */
    @Override
    public void onSynthesizeFinish(String utteranceId) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "合成结束回调, 序列号:" + utteranceId);
        }
    }

    @Override
    public void onSpeechStart(String utteranceId) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "播放开始回调, 序列号:" + utteranceId);
        }
    }

    /**
     * 播放进度回调接口，分多次回调
     *
     * @param progress 如合成“百度语音问题”这6个字， progress肯定是从0开始，到6结束。 但progress无法保证和合成到第几个字对应。
     */
    @Override
    public void onSpeechProgressChanged(String utteranceId, int progress) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "播放进度回调, progress：" + progress + ";序列号:" + utteranceId);
        }
    }

    /**
     * 播放正常结束，每句播放正常结束都会回调，如果过程中出错，则回调onError,不再回调此接口
     */
    @Override
    public void onSpeechFinish(String utteranceId) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG,"播放结束回调, 序列号:" + utteranceId);
        }
    }

    /**
     * 当合成或者播放过程中出错时回调此接口
     *
     * @param speechError 包含错误码和错误信息
     */
    @Override
    public void onError(String utteranceId, SpeechError speechError) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG,"错误发生：" + speechError.description + "，错误编码："
                    + speechError.code + "，序列号:" + utteranceId);
        }
    }
}
