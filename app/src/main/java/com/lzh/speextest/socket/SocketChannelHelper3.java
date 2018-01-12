package com.lzh.speextest.socket;

import android.util.Log;

import com.zzx.audioprocessor.AudioProcessor;
import com.zzx.police.utils.AudioEncoder;
import com.zzx.police.utils.SimpleAudioPlayer;

import java.nio.ByteBuffer;

/**
 * Created by lzh on 2017/11/29.
 */

public class SocketChannelHelper3 extends ChannelHelper {
    private int mFrame = 160;

    public SocketChannelHelper3() {
        Timeout(5000, 5000, 5000);
    }
    private SimpleAudio simpleAudio;

    @Override
    public void success() {
        simpleAudio = new SimpleAudio(mFrame,8000);
        simpleAudio.setAudioListener(new AudioProcessor.AudioProcessListener() {
            @Override
            public void process(byte[] bytes) {
//                write(bytes);

                simpleAudio.receive(bytes);
            }

            @Override
            public void start() {

            }

            @Override
            public void stop() {

            }

            @Override
            public void error(Exception e) {

            }
        });
        startAudio();
    }

    @Override
    public void read(byte[] bytes) {
        simpleAudio.receive(bytes);
    }

    @Override
    public void read(ByteBuffer byteBuffer) {

    }

    @Override
    public void error(Throwable e) {
        simpleAudio.stop();
    }

    @Override
    protected void idle(IdleEvent.Event e) {
        super.idle(e);

        if (e.eventState.equals(IdleEvent.Event.WRITE_IDLE)) {

        }
    }

    @Override
    public void close() {
        super.close();
        simpleAudio.stop();
    }

    public void startAudio() {
        simpleAudio.start();
    }
}
