package com.zzx.audioprocessor;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.util.Date;
import java.util.LinkedList;

import HeavenTao.Audio.WebRtcAecm;


/**
 * Created by lzh on 2017/12/22.
 */

public class AudioInput extends Thread {

    String clCurrentClassNameString = this.getClass().getName().substring(this.getClass().getName().lastIndexOf('.') + 1); //当前类名称字符串

    int iExitFlag = 1; //本线程退出标记，0表示保持运行，1表示请求退出

    AudioRecord m_clAudioRecord; //录音类

    int m_iFrameSize; //一帧音频数据的采样数量，包括：8000Hz为160个采样，16000Hz为320个采样，32000Hz为640个采样
    int m_iSamplingRate; //音频数据的采样频率，包括：8000Hz，16000Hz，32000Hz

    LinkedList<short[]> m_clAlreadyAudioInputLinkedList; //已录音的链表

    WebRtcAecm clWebRtcAecm; //WebRtc移动版声学回音消除器类对象

    short m_szhiTempAudioInputData[];
    int iAudioDataNumber;
    int iTemp;
    Date clLastDate;
    Date clNowDate;

    public AudioInput(int frameSize, int simpleRate, LinkedList<short[]> audioInputqueue) {
        this.m_iFrameSize = frameSize;
        this.m_iSamplingRate = simpleRate;
        this.m_clAlreadyAudioInputLinkedList = audioInputqueue;
    }

    private void initRcord() {
        //初始化AudioRecord类对象
        m_clAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                m_iSamplingRate,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioRecord.getMinBufferSize(m_iSamplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT));
        if (m_clAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
            Log.i(clCurrentClassNameString, "初始化AudioRecord类对象成功！" + m_clAudioRecord.getState());
        } else {
            Log.e(clCurrentClassNameString, "初始化AudioRecord类对象失败！" + m_clAudioRecord.getState());
        }

        Log.e(clCurrentClassNameString, "音频输入线程：开始录音准备");
        iAudioDataNumber = 0;
        clLastDate = new Date();
        m_clAlreadyAudioInputLinkedList.clear();
        m_szhiTempAudioInputData = new short[m_iFrameSize];
        clNowDate = new Date();

        if ((clWebRtcAecm != null) && (clWebRtcAecm.m_iDelay == -1)) //自适应设置WebRtc移动版声学回音消除器的回音延迟时间
        {
            clWebRtcAecm.m_iDelay = (int) ((clNowDate.getTime() - clLastDate.getTime()) / 3);
            Log.e(clCurrentClassNameString, "音频输入线程：自适应设置WebRtc移动版声学回音消除器的回音延迟时间为 " + clWebRtcAecm.m_iDelay + " 毫秒");
        }
        clLastDate = clNowDate;

    }


    public void startRecord() {
        initRcord();
        iExitFlag = 1;
        m_clAudioRecord.startRecording();
        while (true) {
            m_szhiTempAudioInputData = new short[m_iFrameSize];
            m_clAudioRecord.read(m_szhiTempAudioInputData, 0, m_szhiTempAudioInputData.length);

            for (iTemp = 0; iTemp < m_szhiTempAudioInputData.length; iTemp++) {
                if (m_szhiTempAudioInputData[iTemp] != 0)
                    break;
            }
            if (iTemp < m_szhiTempAudioInputData.length) {
                break;
            }
        }
        start();
    }

    public void stopRecord() {
        iExitFlag = 0;
    }

    private void endToRecord() {
        m_clAudioRecord.stop();
    }

    public void release() {
        iExitFlag = 0;
        m_clAlreadyAudioInputLinkedList = null; //已录音的链表
        clWebRtcAecm = null; //WebRtc移动版声学回音消除器类对象

    }


    public void run() {

        //开始循环录音
        while (iExitFlag != 0) {
            m_szhiTempAudioInputData = new short[m_iFrameSize];

            m_clAudioRecord.read(m_szhiTempAudioInputData, 0, m_szhiTempAudioInputData.length);

            clNowDate = new Date();
            iAudioDataNumber++;
            Log.i(clCurrentClassNameString, "音频输入线程：" + "音频数据帧序号：" + iAudioDataNumber + "，" + "读取耗时：" + (clNowDate.getTime() - clLastDate.getTime()) + "，" + "已录音链表元素个数：" + m_clAlreadyAudioInputLinkedList.size());
            clLastDate = clNowDate;

            //追加一帧PCM格式音频数据到已录音的链表
            synchronized (m_clAlreadyAudioInputLinkedList) {

                m_clAlreadyAudioInputLinkedList.addLast(m_szhiTempAudioInputData);
                Log.e("lzh", "音频输入线程："+m_clAlreadyAudioInputLinkedList.size());
            }

            m_szhiTempAudioInputData = null;
            if (iExitFlag == 0) {
                Log.i(clCurrentClassNameString, "音频输入线程：本线程接收到退出请求，开始准备退出");
                break;
            }

        }
        Log.i(clCurrentClassNameString, "结束录音");
        endToRecord();
    }
}
