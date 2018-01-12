package com.lzh.speextest.socket;

import android.util.Log;


import com.example.ByteUtil;
import com.example.FileWriter;
import com.lzh.audioprocessor.AudioProcessor;
import com.zzx.police.utils.AudioEncoder;
import com.zzx.police.utils.SimpleAudioPlayer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by lzh on 2017/11/29.
 */

public class SocketChannelHelper2 extends ChannelHelper {

    private SimpleAudioPlayer simpleAudioPlayer;

    public SocketChannelHelper2() {
        Timeout(5000, 5000, 5000);
    }

    private BaseLineDecoder baseLineDecoder = new BaseLineDecoder(10 * 1024, new BaseLineDecoder.DecodeListener() {
        @Override
        public void onDecoderLister(byte[] msg) {
//            Log.d("lzh", "Socketread:" + new String(msg));
            Stream stream = JsonCoder.parseJson(new String(msg), Stream.class);
            audioProcessor.receive(stream.getStream());
        }
    });
    AudioProcessor audioProcessor;

    @Override
    public void success() {
        Log.e("lzh", "连接已建立");
        audioProcessor = new AudioProcessor(160, 8000);
        audioProcessor.baseConfigure(1, 0, 0, 0, 0, 0, 0, 1, 0).webRtcConfigure(0, 0, 1, 4, 25, 1, 2).speexConfigure(1, 500, 1, 1, -200, 1, 80, 65, 1, 30000, 1, -200, -200);
//        audioProcessor.baseConfigure(0, 0, 0, 0, 0, 0, 0, 1, 1);
//        audioProcessor.baseConfigure(0, 1, 1, 10, 10, 0, 0, 1, 0);
        audioProcessor.baseConfigure(0, 0, 0, 0, 0, 0, 0, 1, 0);
        final AudioEncoder audioEncoder = new AudioEncoder();
        audioProcessor.setCode(new AudioProcessor.CodeProcessListener() {
            @Override
            public void encode(AudioProcessor.EncodeEvent e, byte[] bytes) {

                byte[] result = new byte[bytes.length];
                int a = audioEncoder.encode(bytes,bytes.length,result,result.length);
                Log.e("lzh","AudioEncodercoder:编码成功:"+a+",:"+ByteUtil.bytes2hex(result));
                byte[] bytes2 = Arrays.copyOfRange(result,0,a);
//                try {
//                    com.example.FileWriter.getInstance().write(bytes2,0,bytes2.length);
//                } catch (Exception e1) {
//                    e1.printStackTrace();
//                }
//                byte[] bytes1 = new byte[320];
//                int b = audioEncoder.decode(bytes2,bytes1);
//                Log.e("lzh","AudioEncodercoder"+a+","+b+":预解码成功:"+ ByteUtil.bytes2hex(bytes1));
//                int b2 = audioEncoder.decode(result,a,bytes1,bytes1.length);
//                Log.e("lzh","AudioEncodercoder"+a+","+b+":二次预解码成功:"+ ByteUtil.bytes2hex(bytes1));
//                simpleAudioPlayer.play(bytes1);
                e.onNext(result,0,a);
            }

            @Override
            public void decode(AudioProcessor.DecodeEvent d, byte[] bytes) {
                byte[] result = new byte[320];
                Log.e("lzh","AudioEncodercoder:准备开始解码:"+ByteUtil.bytes2hex(bytes));
                int a = audioEncoder.decode(bytes,result);
                Log.e("lzh","AudioEncodercoder"+bytes.length+","+a+","+":解码成功:"+ByteUtil.bytes2hex(result));
//                simpleAudioPlayer.play(result);
                d.onNext(result);
            }
        });
        audioProcessor.setAudioProcessListener(new AudioProcessor.AudioProcessListener() {
            @Override
            public void process(byte[] bytes) {
//                Log.e("AudioProcessor", "byte:" + bytes.length);
                write(JsonCoder.formatToJson(new Stream("1001", bytes)) + "\r\n");
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

        audioProcessor.startProcess();
        simpleAudioPlayer = new SimpleAudioPlayer(8000);
        simpleAudioPlayer.start();
        try {
            FileWriter.getInstance().start();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void read(byte[] b) {
        baseLineDecoder.decode(b);
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
        Log.e("lzh","连接已断开");
        try {
            FileWriter.getInstance().flush();

//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    try {
//                        FileInputStream fileInputStream = com.example.FileWriter.getInstance().read();
//                        byte[] bytes = new byte[320];
//                        try {
//                            while (fileInputStream.read(bytes) != -1) {
//                                simpleAudioPlayer.play(bytes);
//                            }
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
