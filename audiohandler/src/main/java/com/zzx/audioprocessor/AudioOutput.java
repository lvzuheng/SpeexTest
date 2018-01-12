package com.zzx.audioprocessor;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by lzh on 2017/12/22.
 */

public class AudioOutput extends Thread {
    String clCurrentClassNameString = this.getClass().getSimpleName(); //当前类名称字符串

    int iExitFlag = 0; //本线程退出标记，1表示保持运行，0表示请求退出


    AudioTrack m_clAudioTrack; //播放类

    int m_iFrameSize; //一帧音频数据的采样数量，包括：8000Hz为160个采样，16000Hz为320个采样，32000Hz为640个采样
    int m_iSamplingRate; //音频数据的采样频率，包括：8000Hz，16000Hz，32000Hz

    LinkedList<short[]> m_clAlreadyAudioOutputLinkedList; //已播放的链表
    LinkedList<short[]> m_clAlreadyReceiveAudioOutputLinkedList; //已播放的链表
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 10);

    short m_szhiPcmAudioOutputData[];
    int iTemp;


    public AudioOutput(int frameSize, int simpleRate, LinkedList<short[]> audioOutputLinkedList) {
        this.m_iFrameSize = frameSize;
        this.m_iSamplingRate = simpleRate;
        this.m_clAlreadyAudioOutputLinkedList = audioOutputLinkedList;
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

        m_szhiPcmAudioOutputData = new short[m_iFrameSize];
        m_clAlreadyReceiveAudioOutputLinkedList = new LinkedList<>();
        byteBuffer.flip();
        byteBuffer.mark();
    }


    public boolean startPlay() {
        initPlayer();
        iExitFlag = 1;
        m_clAudioTrack.play();
        start();
        return true;
    }

    public void play(byte[] bytes, int off, int len) {

        for (iTemp = off; iTemp < len / 2; iTemp++) {
            m_szhiPcmAudioOutputData[iTemp] = (short) (((short) bytes[iTemp * 2]) & 0xFF | ((short) bytes[iTemp * 2 + 1]) << 8);
        }
        synchronized (m_clAlreadyReceiveAudioOutputLinkedList) {
            m_clAlreadyReceiveAudioOutputLinkedList.addLast(m_szhiPcmAudioOutputData);
        }
        m_clAudioTrack.write(m_szhiPcmAudioOutputData, 0, iTemp);
//        Log.e("lzh", "play:" + bytes.length);
//        synchronized (byteBuffer) {
//            if (bytes.length > byteBuffer.capacity() - byteBuffer.limit()) {
//                byteBuffer.clear();
//                byteBuffer.mark();
//            } else {
//                if (byteBuffer.capacity() - byteBuffer.limit() < m_iFrameSize) {
//                    byteBuffer.compact();
//                    byteBuffer.flip();
//                    byteBuffer.mark();
//                }
//                byteBuffer.position(byteBuffer.limit());
//                byteBuffer.limit(byteBuffer.capacity());
//                Log.e("lzh", "byteBuffer:play:" + byteBuffer.position()+","+byteBuffer.limit()+","+byteBuffer.capacity());
//            }
//            Log.e("lzh", "byteBuffer:play:" + byteBuffer.position()+","+byteBuffer.limit()+","+bytes.length);
//            byteBuffer.put(bytes,off,len);
//        }
    }

    public void play(short[] shorts, int off, int len) {
        if (iExitFlag == 1) {
            m_clAudioTrack.write(shorts, off, len);
            synchronized (m_clAlreadyReceiveAudioOutputLinkedList) {
                Log.e("lzh", "m_clAlreadyAudioOutputLinkedList:" + m_clAlreadyReceiveAudioOutputLinkedList.size());
                m_clAlreadyReceiveAudioOutputLinkedList.addLast(shorts);
            }

        }
    }

    public void stopPlay() {
        iExitFlag = 0;
    }

    private void endToPlay() {
        m_clAudioTrack.stop();
    }

    @Override
    public void run() {
        short[] shorts = new short[m_iFrameSize];
        while (iExitFlag == 1) {
//            synchronized (byteBuffer) {
//                Log.e("lzh","buffer:"+byteBuffer.position()+","+byteBuffer.limit()+","+byteBuffer.remaining());
//                byteBuffer.limit(byteBuffer.position());
//                byteBuffer.reset();
//                if (byteBuffer.remaining() > m_iFrameSize) {
////
//                    byteBuffer.asShortBuffer().get(shorts);
//                    byteBuffer.mark();
//                    if (shorts != null) {
//                        synchronized (m_clAlreadyAudioOutputLinkedList) {
//                            m_clAlreadyAudioOutputLinkedList.addLast(shorts);
//                        }
//                    }
//                } else {
//                    Log.i("lzh", "无语音活动" + m_clAlreadyAudioOutputLinkedList.size());
//                    synchronized (m_clAlreadyAudioOutputLinkedList) {
//                        m_clAlreadyAudioOutputLinkedList.addLast(m_szhiPcmAudioOutputData);
//                    }
//                }
//            }
//            SystemClock.sleep(1);

            if (m_clAlreadyReceiveAudioOutputLinkedList.size() > 0) {
                Log.i("lzh", "有语音活动" + m_clAlreadyReceiveAudioOutputLinkedList.size());
                shorts = m_clAlreadyReceiveAudioOutputLinkedList.poll();
                if (shorts != null) {
                    synchronized (m_clAlreadyAudioOutputLinkedList) {
                        m_clAlreadyAudioOutputLinkedList.addLast(shorts);
                    }
                }
            } else {
                Log.i("lzh", "无语音活动" + m_clAlreadyReceiveAudioOutputLinkedList.size());
                synchronized (m_clAlreadyAudioOutputLinkedList) {
                    m_clAlreadyAudioOutputLinkedList.addLast(m_szhiPcmAudioOutputData);
                }
                SystemClock.sleep(1);
            }
        }
        endToPlay();
    }
}