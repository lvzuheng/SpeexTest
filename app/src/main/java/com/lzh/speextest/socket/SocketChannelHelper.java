package com.lzh.speextest.socket;

import android.util.Log;


import com.example.ByteUtil;
import com.zzx.audioprocessor.AudioProcessor;
import com.zzx.police.utils.AudioEncoder;
import com.zzx.police.utils.SimpleAudioPlayer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by lzh on 2017/11/29.
 */

public class SocketChannelHelper extends ChannelHelper {
    ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 10);
    private int mFrame = 160;
    private byte[] tempData = new byte[mFrame / 4];
    private SimpleAudioPlayer simpleAudioPlayer;

    public SocketChannelHelper() {
        Timeout(5000, 5000, 5000);
    }

    private BaseLineDecoder baseLineDecoder = new BaseLineDecoder(10 * 1024, new BaseLineDecoder.DecodeListener() {
        @Override
        public void onDecoderLister(byte[] msg) {
            Log.d("lzh", "Socketread:" + new String(msg));
            Stream stream = JsonCoder.parseJson(new String(msg), Stream.class);
            if (audioProcessor != null) {
                audioProcessor.receive(stream.getStream());
            }
        }
    });
    AudioProcessor audioProcessor;

    @Override
    public void success() {
        startAudio();
    }

    @Override
    public void read(byte[] bytes) {
//        baseLineDecoder.decode(b);
        if (audioProcessor != null) {
            byteBuffer.put(bytes);
            byteBuffer.flip();
            Log.e("lzh", "byteBuffer:" + byteBuffer.remaining());
            while (byteBuffer.remaining() >= mFrame) {
                byteBuffer.get(tempData);

                if (audioProcessor != null) {
                    Log.e("lzh", "byteBuffer:tempData:" + tempData.length + "," + byteBuffer.remaining());
                    audioProcessor.receive(tempData);
                }
            }
            if (byteBuffer.hasRemaining()) {
                byteBuffer.compact();
            } else {
                byteBuffer.clear();
            }
        }
    }

    @Override
    public void read(ByteBuffer byteBuffer) {

    }

    @Override
    public void error(Throwable e) {

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
        audioProcessor.stopProcess();
        audioProcessor = null;
        Log.e("lzh", "连接已停止");
    }

    public void startAudio() {
        simpleAudioPlayer = new SimpleAudioPlayer(8000);
        simpleAudioPlayer.start();
        Log.e("lzh", "连接已建立");
        audioProcessor = new AudioProcessor(mFrame, 8000);
        audioProcessor.baseConfigure(1, 0, 0, 0, 0, 0, 0, 1, 0).webRtcConfigure(0, 0, 1, 4, 25, 1, 2).speexConfigure(1, 500, 1, 1, -200, 1, 80, 65, 1, 30000, 1, -200, -200);
//        audioProcessor.baseConfigure(0, 0, 0, 0, 0, 0, 0, 1, 1);
//        audioProcessor.baseConfigure(0, 1, 1, 10, 10, 0, 0, 1, 0);
        audioProcessor.baseConfigure(0, 0, 0, 0, 0, 0, 0, 1, 0);
        final AudioEncoder audioEncoder = new AudioEncoder();
        audioProcessor.setAudioProcessListener(new AudioProcessor.AudioProcessListener() {
            @Override
            public void process(byte[] bytes) {
//                Log.e("AudioProcessor", "byte:" + bytes.length+","+JsonCoder.formatToJson(new Stream("1001",bytes)).getBytes().length);
//                write(JsonCoder.formatToJson(new Stream("1001",bytes))+"\r\n");
//                audioProcessor.receive(bytes);
                write(bytes);
//                simpleAudioPlayer.play(bytes);
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
        final byte[] encoderesult = new byte[1024];
        final byte[] decoderesult = new byte[1024];
        audioProcessor.setCode(new AudioProcessor.CodeProcessListener() {
            @Override
            public void encode(AudioProcessor.EncodeEvent e, byte[] bytes) {

                int a = audioEncoder.encode(bytes, bytes.length, encoderesult, encoderesult.length);
//                int b = audioEncoder.decode(encoderesult,a, decoderesult,decoderesult.length);
//                simpleAudioPlayer.play(decoderesult,0,b);
                e.onNext(encoderesult, 0, a);
//                e.onNext(bytes,0,bytes.length);
            }

            @Override
            public void decode(AudioProcessor.DecodeEvent d, byte[] bytes) {

                int a = audioEncoder.decode(bytes, decoderesult);
//                simpleAudioPlayer.play(bytes);
                d.onNext(decoderesult, 0, a);
//                d.onNext(bytes,0,bytes.length);
            }
        });
        audioProcessor.startProcess();
    }
}
