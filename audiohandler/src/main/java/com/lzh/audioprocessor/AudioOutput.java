package com.lzh.audioprocessor;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.util.Date;
import java.util.LinkedList;

import HeavenTao.Audio.Ajb;
import HeavenTao.Audio.SpeexDecoder;

/**
 * Created by lzh on 2017/12/22.
 */

public class AudioOutput extends Thread {
    String clCurrentClassNameString = this.getClass().getSimpleName(); //当前类名称字符串

    int iExitFlag = 0; //本线程退出标记，1表示保持运行，0表示请求退出


    AudioTrack m_clAudioTrack; //播放类

    int m_iFrameSize; //一帧音频数据的采样数量，包括：8000Hz为160个采样，16000Hz为320个采样，32000Hz为640个采样
    int m_iSamplingRate; //音频数据的采样频率，包括：8000Hz，16000Hz，32000Hz

    Ajb clAjb; //自适应抖动缓冲器类对象
    Integer clAjbGetAudioDataSize; //从自适应抖动缓冲器中取出的音频数据的内存长度

    SpeexDecoder clSpeexDecoder; //Speex解码器类对象


    LinkedList<short[]> m_clAlreadyAudioOutputLinkedList; //已播放的链表


    short m_szhiPcmAudioOutputData[];



    public AudioOutput(int frameSize, int simpleRate, LinkedList<short[]> audioOutputLinkedList, Ajb ajb, SpeexDecoder speexDecoder) {
        this.m_iFrameSize = frameSize;
        this.m_iSamplingRate = simpleRate;
        this.m_clAlreadyAudioOutputLinkedList = audioOutputLinkedList;
        this.clAjb = ajb;
        this.clSpeexDecoder = speexDecoder;
        Log.e("lzh", "clSpeexDecoder:" + speexDecoder);
    }


    private void initPlayer() {
        Log.i(clCurrentClassNameString, "音频输出线程：准备开始播放");
        //初始化AudioTrack类对象
        m_clAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                m_iSamplingRate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize(m_iSamplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT),
                AudioTrack.MODE_STREAM);
    }


    public void startPlay() {
        initPlayer();
        iExitFlag = 1;
        m_clAudioTrack.play();
        start();
    }

    public void stopPlay() {
        iExitFlag = 0;
    }

    private void endToPlay() {
        m_clAudioTrack.stop();
    }

    @Override
    public void run() {
        //开始循环播放
        while (iExitFlag == 1) {
            //从自适应抖动缓冲器取出第一帧音频数据，并播放
            m_szhiPcmAudioOutputData = new short[m_iFrameSize];
            clAjbGetAudioDataSize = new Integer(m_iFrameSize);
            synchronized (clAjb) {
                clAjb.GetShortAudioData(m_szhiPcmAudioOutputData, clAjbGetAudioDataSize);
            }
            if (clAjbGetAudioDataSize.intValue() == 0) {
                Log.i(clCurrentClassNameString, "音频输出线程：从PCM自适应抖动缓冲器取出一帧无语音活动的音频数据");
            } else {
                Log.i(clCurrentClassNameString, "音频输出线程：从PCM自适应抖动缓冲器取出一帧有语音活动的音频数据");
            }
            clAjb.GetCurHaveActiveBufferSize(clAjbGetAudioDataSize);
            Log.i(clCurrentClassNameString, "音频输出线程：自适应抖动缓冲器的当前已缓冲有语音活动音频数据帧数量为 " + clAjbGetAudioDataSize.intValue() + " 个");
            clAjb.GetCurHaveInactiveBufferSize(clAjbGetAudioDataSize);
            Log.i(clCurrentClassNameString, "音频输出线程：自适应抖动缓冲器的当前已缓冲无语音活动音频数据帧数量为 " + clAjbGetAudioDataSize.intValue() + " 个");
            clAjb.GetCurNeedBufferSize(clAjbGetAudioDataSize);
            Log.i(clCurrentClassNameString, "音频输出线程：自适应抖动缓冲器的当前需缓冲音频数据帧的数量为 " + clAjbGetAudioDataSize.intValue() + " 个");
            try {
                //开始播放这一帧音频数据
                m_clAudioTrack.write(m_szhiPcmAudioOutputData, 0, m_szhiPcmAudioOutputData.length);
                //追加一帧音频数据到已播放的链表
                synchronized (m_clAlreadyAudioOutputLinkedList) {
                    m_clAlreadyAudioOutputLinkedList.addLast(m_szhiPcmAudioOutputData);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            m_szhiPcmAudioOutputData = null;
            if (iExitFlag == 0) {
                Log.i(clCurrentClassNameString, "音频输出线程：本线程接收到退出请求，开始准备退出");
                return;
            }
        }
        endToPlay();
        Log.i(clCurrentClassNameString, "音频输出线程：本线程已退出");
    }
}
