package com.zzx.police.utils;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;


/**
 * Created by lzh on 2017/12/12.
 */

public class SimpleAudioPlayer {


    private AudioTrack mAudioTrack;
    private int mRate;

    public SimpleAudioPlayer(int simpleRate) {
        this.mRate = simpleRate;
    }

    public synchronized void play(byte[] data) {
        if (data.length > 0 && mAudioTrack != null) {
            mAudioTrack.write(data, 0, data.length);
        }
    }
    public synchronized void play(byte[] data,int start,int length) {
        if (data.length > 0 && mAudioTrack != null) {
            mAudioTrack.write(data, start, length);
        }
    }
    public synchronized void play(short[] data) {
        if (data.length > 0 && mAudioTrack != null) {
            mAudioTrack.write(data, 0, data.length);
        }
    }
    public synchronized void play(short[] data,int start,int length) {
        if (data.length > 0 && mAudioTrack != null) {
            mAudioTrack.write(data, start, length);
        }
    }

    public synchronized void start() {
        int bufferSize = AudioTrack.getMinBufferSize(mRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        this.mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
    }

    public void stop() {
        if (mAudioTrack != null) {
            mAudioTrack.flush();
            mAudioTrack.stop();
        }
    }

    public synchronized void release() {
        stop();
        if (mAudioTrack != null) {
            this.mAudioTrack = null;
        }
    }
}
