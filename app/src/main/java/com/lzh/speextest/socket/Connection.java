package com.lzh.speextest.socket;

/**
 * Created by lzh on 2017/11/29.
 */

public interface Connection {

    Connection connect();
    boolean read();
    boolean write(byte[] b);
    void release();
    boolean isConnect();
}
