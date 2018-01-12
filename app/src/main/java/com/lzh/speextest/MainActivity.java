package com.lzh.speextest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.example.ByteUtil;
import com.example.FileWriter;
import com.lzh.speextest.socket.SocketChannelHelper3;
import com.lzh.speextest.socket.TcpConnect;
import com.zzx.police.utils.AudioEncoder;
import com.zzx.police.utils.SimpleAudioPlayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    static {
        System.loadLibrary("Func"); //加载libFunc.so
        System.loadLibrary("WebRtcAec"); //加载libWebRtcAec.so
        System.loadLibrary("WebRtcAecm"); //加载libWebRtcAecm.so
        System.loadLibrary("WebRtcNs"); //加载libWebRtcNs.so
        System.loadLibrary("SpeexDsp"); //加载libSpeexDsp.so
        System.loadLibrary("Speex"); //加载libSpeex.so
        System.loadLibrary("Ajb"); //加载libAjb.so
    }

    private static final String TAG = "MainActivity";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        String[] promission = new String[]{Manifest.permission.RECORD_AUDIO
                , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, promission, 1);
        }
        init();
        initView();
    }

    private void init() {

    }

    private TcpConnect tcpConnect;

    private void initView() {
        Button audiostart = (Button) findViewById(R.id.audio_start);
        audiostart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tcpConnect == null || !tcpConnect.isConnect()) {
                    tcpConnect = new TcpConnect("10.20.175.146", 3333, new SocketChannelHelper3());
                    tcpConnect.connect();
                }
            }
        });
        Button audiostop = (Button) findViewById(R.id.audio_stop);
        audiostop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("lzh", "audiostop连接已停止"+(tcpConnect != null && tcpConnect.isConnect()));
                if (tcpConnect != null && tcpConnect.isConnect()) {
                    Log.e("lzh", "audiostop连接已停止");
                    tcpConnect.disconnect();
                }
            }
        });
        Button audioplay = (Button) findViewById(R.id.audio_play);

        audioplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//
//                try {
//                    FileOutputStream fileOutputStream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/Download/" + "voice1");
//                    byte[] bytes = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa".getBytes();
//                    fileOutputStream.write(bytes,0,bytes.length);
//                    fileOutputStream.flush();
//                    fileOutputStream.close();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SimpleAudioPlayer simpleAudioPlayer = new SimpleAudioPlayer(8000);
                            simpleAudioPlayer.start();
                            FileInputStream fileInputStream = FileWriter.getInstance().read();
                            byte[] bytes = new byte[40];
                            AudioEncoder audioEncoder = new AudioEncoder();
                            try {
                                Log.d(TAG, "onCreate: audioEncoder952:" + fileInputStream.available());
                                int len = 0;
                                while ((len = fileInputStream.read(bytes)) != -1) {
                                    Log.d(TAG, "audioEncoder:bytes：" + len+","+ ByteUtil.bytes2hex(bytes));
                                    byte[] b = new byte[320];
                                    int a = audioEncoder.decode(bytes, b);
                                    Log.d(TAG, "onCreate: audioEncoder1:" + a+","+ByteUtil.bytes2hex(b));
                                    simpleAudioPlayer.play(b, 0, a);
                                }
                                simpleAudioPlayer.stop();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }
}
