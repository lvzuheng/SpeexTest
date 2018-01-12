package com.lzh.speextest.socket;

import android.os.SystemClock;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by lzh on 2017/12/15.
 */

public class NioTcpConnect implements Connection {

    protected Selector selector;
    protected SocketChannel channel;
    protected static final int CONNECT_TIMEOUT = 10000;
    protected static final int READ_TIMEOUT = 10000;
    protected static final int RECONNECT_TIME = 120000;
    protected static final int RECONNECT_TIME_SECOND = RECONNECT_TIME / 1000;

    protected final byte CONNECT = 1;
    protected final byte RUNNING = 2;

    protected ChannelHelper channelHelper;

    private String ip;
    private int port;

    public NioTcpConnect(@NonNull String ip, @NonNull int port, ChannelHelper channelHelper) {
        this.ip = ip;
        this.port = port;
        this.channelHelper = channelHelper;
        channelHelper.setConnection(this);
    }

    @Override
    public Connection connect() {

        Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(ObservableEmitter<Boolean> e) throws Exception {
                long connectTime = System.currentTimeMillis();
                try {
                    selector = Selector.open();
                    InetSocketAddress isa = new InetSocketAddress(ip, port);
                    channel = SocketChannel.open();
                    // 设置连超时
                    channel.socket().connect(isa, CONNECT_TIMEOUT);
                    // 设置读超时
                    channel.socket().setSoTimeout(READ_TIMEOUT);
                    channel.configureBlocking(false);
                    channel.register(selector, SelectionKey.OP_READ, new ByteArrayOutputStream());
                    while (channel != null && !channel.isConnected()) {
                        if (System.currentTimeMillis() - connectTime > 5 * 1000) {
                            e.onError(new Throwable("time out"));
                            return;
                        }
                        SystemClock.sleep(200);
                    }
                    e.onNext(true);
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io())
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.d("lzh", "success：" + channel);
                        channelHelper.channelActive();
                        read();
                    }

                    @Override
                    public void onError(Throwable e) {
                        channelHelper.error(e);
                        release();
                    }

                    @Override
                    public void onComplete() {

                    }
                });

        return this;
    }


    @Override
    public boolean read() {


        Observable.create(new ObservableOnSubscribe<ByteBuffer>() {
            @Override
            public void subscribe(ObservableEmitter<ByteBuffer> e) throws Exception {

                SelectionKey selectionKey = null;
                try {
                    while (selector.select() > 0) {
                        Set<SelectionKey> keys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = keys.iterator();
                        while (iterator.hasNext()) {
                            selectionKey = iterator.next();
                            iterator.remove();
                            if (selectionKey.isReadable()) {
                                SocketChannel client = (SocketChannel) selectionKey.channel();
                                ByteBuffer buffer = ByteBuffer.allocate(10240);// 10kb缓存
                                while (client.read(buffer) > 0) {
                                    e.onNext(buffer);
                                    channelHelper.callIdle(IdleEvent.Event.READ_IDLE);
                                    buffer.clear();// 清空
                                }
//                                client.s(0xFF);
                            }

                        }
                    }
                    e.onComplete();
                } catch (IOException e1) {
                    e.onError(e1);
                    e1.printStackTrace();
                }
            }
        }).subscribeOn(Schedulers.io())
                .subscribe(new Observer<ByteBuffer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(ByteBuffer buffer) {
//                        Log.e("lzh", "onNext:" + socket.isConnected());
                        channelHelper.read(buffer);
                    }

                    @Override
                    public void onError(Throwable e) {
                        try {
                            channelHelper.error(e);
                            disconnect();
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }

                    }

                    @Override
                    public void onComplete() {
                        release();
                    }
                });
        return false;
    }

    @Override
    public synchronized boolean write(byte[] b) {
        Observable.just(b).subscribeOn(Schedulers.io()).subscribe(new Observer<byte[]>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onNext(byte[] bytes) {
                Log.d("WRITEIDLE", "写出：" + bytes.length);
                try {
                    if (channel.isConnected()) {
                        ByteBuffer buffer = ByteBuffer.wrap(bytes);
                        int size = buffer.remaining();
                        // 此处需加中途断开逻辑，下次再继续发送数据包
                        int actually = 0;
                        actually = channel.write(buffer);
                        channelHelper.callIdle(IdleEvent.Event.WRITE_IDLE);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });

        return false;
    }

    @Override
    public void release() {

    }

    @Override
    public boolean isConnect() {
        return false;
    }

    public void disconnect() {
        try {
            if (channel != null) {
                channel.socket().close();
                channel.close();
                channel = null;
            }
            if (selector != null) {
                selector.close();
                selector = null;
            }
            if (channelHelper != null && channelHelper.isAlive())
                channelHelper.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
