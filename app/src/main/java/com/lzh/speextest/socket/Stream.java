package com.lzh.speextest.socket;

/**
 * Created by lzh on 2017/12/23.
 */

public class Stream {

    private String code;
    private byte[] stream;
    public Stream(String code,byte[] stream){
        this.code = code;
        this.stream = stream;
    }

    public Stream(){}

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public byte[] getStream() {
        return stream;
    }

    public void setStream(byte[] stream) {
        this.stream = stream;
    }
}
