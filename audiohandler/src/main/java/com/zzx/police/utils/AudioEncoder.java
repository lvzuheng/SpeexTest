package com.zzx.police.utils;

//g726库的规定路径
public class AudioEncoder {

    static {
        try {
            System.loadLibrary("g726_codec");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int encode(byte[] input, int count, byte[] output, int outputSize) {
        return g726_encode(input, count, output, outputSize);
    }

    public int encode(short[] input, byte[] output) {
        byte[] date = new byte[input.length * 2];
        for (int iTemp = 0; iTemp < input.length; iTemp++) {
            date[iTemp * 2] = (byte) (input[iTemp] & 0xFF);
            date[iTemp * 2 + 1] = (byte) ((input[iTemp] & 0xFF00) >> 8);
        }
        return g726_encode(date, date.length, output, output.length);
    }

    public int decode(byte[] input, int count, byte[] output, int outputSize) {
        return g726_decode(input, count, output, outputSize);
    }

    public int decode(byte[] input, byte[] output) {
        return g726_decode(input, input.length, output, output.length);
    }


    private native int g726_encode(byte[] input, int count, byte[] output, int outputSize);

    public static native int g726_decode(byte[] input, int count, byte[] output, int outputSize);

}
