package com.zzx.audioprocessor;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;


import java.nio.ByteBuffer;

/**
 * Created by lzh on 2017/12/25.
 */

public class AudioAACCoder  {

    private int rate;
    private int bitRate;
    private boolean mFirst = true;
    private MediaCodec mediaDeCoder;
    private MediaCodec mediaEnCoder;
    private MediaFormat formatDecoder;
    private MediaFormat formatEncoder;
    private boolean isCode = false;
    private final static String AAC = "audio/mp4a-latm";

    private ByteBuffer[] mInputBuffer;
    private ByteBuffer[] mOutputBuffer;
    private MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

    public AudioAACCoder(int sampleRate, int bitRate) {

        this.rate = sampleRate;
        this.bitRate = bitRate;
        initDecodec();
        initEncodec();
    }

    private void initDecodec() {
        formatDecoder = MediaFormat.createAudioFormat(AAC, rate, 1);
        formatDecoder.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        formatDecoder.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        initializeDecoder();
    }

    private void initEncodec() {
        formatEncoder = MediaFormat.createAudioFormat(AAC, rate, 1);
        formatEncoder.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        formatEncoder.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0);
        initializeEncoder();
    }

    private void initializeDecoder() {
        try {
            mediaDeCoder = MediaCodec.createDecoderByType(AAC);
            mediaDeCoder.configure(formatDecoder, null, null, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeEncoder() {
        try {
            mediaEnCoder = MediaCodec.createEncoderByType(AAC);
            mediaEnCoder.configure(formatEncoder, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        initializeDecoder();
        initializeEncoder();
        mediaDeCoder.start();
        mediaEnCoder.start();
    }

    public void stop() {
        if (Build.VERSION.SDK_INT >= 21) {
            mediaDeCoder.reset();//reset方法更加通用，可以在任何状态进行操作
            mediaEnCoder.reset();
        } else {
            mediaDeCoder.stop();
            mediaEnCoder.stop();
        }
    }

    public void release() {
        stop();
        mediaDeCoder.release();
        mediaEnCoder.release();
        mediaDeCoder = null;
        mediaEnCoder = null;
    }

    public void decode(byte[] b,AudioProcessor.DecodeEvent decodeEvent) {
        if (mFirst) {
            mFirst = false;
            mediaDecodec( getFirstByADTS(b),decodeEvent);
        }
        mediaDecodec(b, decodeEvent);
    }

    public void encode(byte[] b,AudioProcessor.EncodeEvent encodeEvent) {
        mediaEncodec(b, encodeEvent);
    }

    //当data为null时，进行流结束操作
    private void mediaDecodec(byte[] data,AudioProcessor.DecodeEvent decodeEvent) {
        int start = 0;
        do {
            int index = mediaDeCoder.dequeueInputBuffer(0);

            if (index < 0) {
                return;
            }
            ByteBuffer buffer;
            if (Build.VERSION.SDK_INT >= 21) {
                buffer = mediaDeCoder.getInputBuffer(index);
            } else {
                buffer = mediaDeCoder.getInputBuffers()[index];
            }
            if (buffer == null) {
                return;
            }
            buffer.clear();
            if (data == null) {
                mediaDeCoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                int count = Math.min(data.length - start, buffer.remaining());
                buffer.put(data, start, count);
                start += count;
                mediaDeCoder.queueInputBuffer(index, 0, count, 0, 0);
            }
            int outIndex = mediaDeCoder.dequeueOutputBuffer(bufferInfo, 0);
            while (outIndex >= 0) {
                ByteBuffer outBuffer;
                if (Build.VERSION.SDK_INT >= 21) {
                    outBuffer = mediaDeCoder.getOutputBuffer(outIndex);
                } else {
                    outBuffer = mOutputBuffer[outIndex];
                }
                if (outBuffer == null)
                    return;
                int outBitsSize = bufferInfo.size;
                outBuffer.position(bufferInfo.offset);
                outBuffer.limit(bufferInfo.offset + outBitsSize);
                try {
                    data = new byte[outBitsSize];
                    outBuffer.get(data, 0, outBitsSize);
                    audioAACCodeListener.decode(data,decodeEvent);
                    mediaDeCoder.releaseOutputBuffer(outIndex, false);
                    outIndex = mediaDeCoder.dequeueOutputBuffer(bufferInfo, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        } while (start < (data == null ? 0 : data.length));
    }


    //当data为null时，进行流结束操作
    private void mediaEncodec(byte[] data,AudioProcessor.EncodeEvent encodeEvent) {
        int start = 0;
        do {
            int index = mediaEnCoder.dequeueInputBuffer(0);
            if (index < 0) {
                return;
            }
            ByteBuffer buffer;
            if (Build.VERSION.SDK_INT >= 21) {
                buffer = mediaEnCoder.getInputBuffer(index);
            } else {
                buffer = mediaEnCoder.getInputBuffers()[index];
            }
            if (buffer == null) {
                return;
            }
            buffer.clear();
            if (data == null) {
                mediaEnCoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                int count = Math.min(data.length - start, buffer.remaining());
                buffer.put(data, start, count);
                start += count;
                mediaEnCoder.queueInputBuffer(index, 0, count, 0, 0);
            }
            int outIndex = mediaEnCoder.dequeueOutputBuffer(bufferInfo, 0);
            while (outIndex >= 0) {
                ByteBuffer outBuffer;
                if (Build.VERSION.SDK_INT >= 21) {
                    outBuffer = mediaEnCoder.getOutputBuffer(outIndex);
                } else {
                    outBuffer = mOutputBuffer[outIndex];
                }
                if (outBuffer == null)
                    return;
                int outBitsSize = bufferInfo.size;
                outBuffer.position(bufferInfo.offset);
                outBuffer.limit(bufferInfo.offset + outBitsSize);
                try {
                    data = new byte[outBitsSize];
                    outBuffer.get(data, 0, outBitsSize);
                    audioAACCodeListener.encode(data,encodeEvent);
                    mediaEnCoder.releaseOutputBuffer(outIndex, false);
                    outIndex = mediaEnCoder.dequeueOutputBuffer(bufferInfo, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    break;
                }
            }
        } while (start < (data == null ? 0 : data.length));
    }


    private byte[] getFirstByADTS(byte[] ADTS) {
        if (rate == 8000) {
            return new byte[]{(byte) 0x15, (byte) 0x90};
        } else if (rate == 11400) {
            return new byte[]{(byte) 0x12, (byte) 0x10};
        }
        int profile = (ADTS[2] & 0xC0) >>> 6;
        int freqIdx = (ADTS[2] & 0x3C) >> 2;
        int chanCfg = ((ADTS[2] & 0x03) << 2) | ((ADTS[3] & 0xC0) >> 6);
        byte[] type = new byte[2];
        type[0] = (byte) ((profile << 3) | ((freqIdx & 0xe) >> 1));
        type[1] = (byte) (((freqIdx & 0x1) << 7) | (chanCfg << 3));
        return type;
    }

    public interface AudioAACCodeListener{
        void encode(byte[] b,AudioProcessor.EncodeEvent encodeEvent);
        void decode(byte[] b,AudioProcessor.DecodeEvent decodeEvent);
    }
    private AudioAACCodeListener audioAACCodeListener;

    public void setAudioAACCodeListener(AudioAACCodeListener audioAACCodeListener){
        this.audioAACCodeListener = audioAACCodeListener;
    }
}
