package com.lzh.speextest.socket;

import android.util.Log;

import java.nio.ByteBuffer;

import io.reactivex.annotations.NonNull;

/**
 * Created by lzh on 2017/11/29.
 */

public abstract class ChannelHelper {
    public abstract void success();
    public abstract void read(byte[] b);
    public abstract void read(ByteBuffer byteBuffer);
    public abstract void error(Throwable e);

    private Connection connection;

    private boolean isAlive = false;

    public ChannelHelper(){

    }

    public void write(byte[] bytes){
        Log.e("lzh","写出的消息："+new String(bytes));
        connection.write(bytes);
    }
    public void write(String msg){
        connection.write(msg.getBytes());
    }

    protected void channelActive(){
        isAlive = true;
        success();
        if(idleEvent!=null){
            idleEvent.start();
        }
    }


    private IdleEvent idleEvent;
    protected void idle(IdleEvent.Event e) {
    }
    public ChannelHelper Timeout(int readIdle, int writeIdle, int allIdle){
        idleEvent = new IdleEvent() {
            @Override
            public void onIdle(Event e) {
                idle(e);
            }
        };
        idleEvent.setIdle(readIdle,writeIdle,allIdle);
        return  this;
    }

    protected void Idle(@NonNull String idle,@NonNull  long wait){
    }

    protected void callIdle(String idle){
        idleEvent.callIdle(idle);
    }




    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }



    public void close(){
        isAlive = false;
        if(idleEvent != null){
            idleEvent.release();
            idleEvent = null;
        }
        if(connection.isConnect()){
            release();
        }
    }

    public void release(){
        if(connection.isConnect()){
            connection.release();
        }

    }

    public boolean isAlive(){
        return isAlive;
    }

}
