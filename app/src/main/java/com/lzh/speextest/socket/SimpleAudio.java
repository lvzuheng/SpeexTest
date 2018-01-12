package com.lzh.speextest.socket;

import com.zzx.audioprocessor.AudioAACCoder;
import com.zzx.audioprocessor.AudioProcessor;
import com.zzx.police.utils.SimpleAudioPlayer;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by lzh on 2018/1/11.
 */

public class SimpleAudio {

    private int mFrame;
    private int mSimpleRate;
    private AudioProcessor audioProcessor;
    private ByteBuffer byteBuffer;
    private byte[] bytes;
    private SimpleAudioPlayer simpleAudioPlayer;

    public SimpleAudio(int frame, int simpleRate) {
        this.mFrame = frame;
        this.mSimpleRate = simpleRate;
    }

    public void start() {
        initProcessor();
        audioProcessor.startProcess();
        simpleAudioPlayer = new SimpleAudioPlayer(mSimpleRate);
        simpleAudioPlayer.start();
    }

    public void stop() {
        audioProcessor.stopProcess();
    }

    private void initProcessor() {
        byteBuffer = ByteBuffer.allocate(10240);
        bytes = new byte[mFrame * 2];
        audioProcessor = new AudioProcessor(mFrame, mSimpleRate);
//        audioProcessor.baseConfigure(1, 0, 0, 0, 0, 0, 0, 1, 0).webRtcConfigure(0, 0, 1, 4, 25, 1, 2).speexConfigure(1, 500, 1, 1, -200, 1, 80, 65, 1, 30000, 1, -200, -200);
//        audioProcessor.baseConfigure(0, 0, 0, 0, 0, 0, 0, 1, 0);
        audioProcessor.setAudioProcessListener(audioProcessListener);
        audioProcessor.setCode(new AudioProcessor.CodeProcessListener() {
            @Override
            public void encode(AudioProcessor.EncodeEvent e, byte[] bytes) {
                simpleAudioPlayer.play(Arrays.copyOfRange(bytes,6,bytes.length));
//                e.onNext(bytes, 0,bytes.length);
            }

            @Override
            public void decode(AudioProcessor.DecodeEvent d, byte[] bytes) {
                d.onNext(bytes,0,bytes.length);
            }
        });
    }

    private AudioProcessor.AudioProcessListener audioProcessListener;

    public void setAudioListener(AudioProcessor.AudioProcessListener audioListener) {
        this.audioProcessListener = audioListener;
    }

    public void receive(byte[] bytes){
        audioProcessor.receive(bytes);
    }
}
