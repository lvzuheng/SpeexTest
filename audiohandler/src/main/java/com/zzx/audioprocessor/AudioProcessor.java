package com.zzx.audioprocessor;

import android.os.SystemClock;
import android.util.Log;

import java.util.Arrays;
import java.util.LinkedList;

import HeavenTao.Audio.SpeexAec;
import HeavenTao.Audio.SpeexDecoder;
import HeavenTao.Audio.SpeexEncoder;
import HeavenTao.Audio.SpeexPreprocessor;
import HeavenTao.Audio.WebRtcAec;
import HeavenTao.Audio.WebRtcAecm;
import HeavenTao.Audio.WebRtcNsx;


/**
 * Created by lzh on 2017/12/22.
 */

public class AudioProcessor extends Thread {

    static {
        System.loadLibrary("Func"); //加载libFunc.so
        System.loadLibrary("WebRtcAec"); //加载libWebRtcAec.so
        System.loadLibrary("WebRtcAecm"); //加载libWebRtcAecm.so
        System.loadLibrary("WebRtcNs"); //加载libWebRtcNs.so
        System.loadLibrary("SpeexDsp"); //加载libSpeexDsp.so
        System.loadLibrary("Speex"); //加载libSpeex.so
        System.loadLibrary("Ajb"); //加载libAjb.so
    }

    String clCurrentClassNameString = this.getClass().getSimpleName(); //当前类名称字符串

    int iExitFlag = 0; //本线程退出标记，1表示保持运行，0表示请求退出

    long lLastPacketSendTime; //存放最后一个数据包的发送时间，用于判断连接是否中断
    long lLastPacketRecvTime; //存放最后一个数据包的接收时间，用于判断连接是否中断

    int m_iFrameSize; //一帧音频数据的采样数量，包括：8000Hz为160个采样，16000Hz为320个采样，32000Hz为640个采样
    int m_iSamplingRate; //音频数据的采样频率，包括：8000Hz，16000Hz，32000Hz

    int iIsUseWebRtcAec; //是否使用WebRtc声学回音消除器，非0表示要使用，0表示不使用
    int iWebRtcAecNlpMode; //WebRtc声学回音消除器的非线性滤波模式，0表示保守, 1表示适中, 2表示积极

    int iIsUseWebRtcAecm; //是否使用WebRtc移动版声学回音消除器，非0表示要使用，0表示不使用
    int iWebRtcAecmEchoMode; //WebRtc移动版声学回音消除器的消除模式，最低为0，最高为4
    int iWebRtcAecmDelay; //WebRtc移动版声学回音消除器的回音延迟时间，单位毫秒，-1表示自适应设置

    int iIsUseSpeexAec; //是否使用Speex声学回音消除器，非0表示要使用，0表示不使用
    int iSpeexAecFilterLength; //Speex声学回音消除器的过滤器长度，单位毫秒

    int iIsUseWebRtcNsx; //是否使用WebRtc定点噪音抑制器，非0表示要使用，0表示不使用
    int iWebRtcNsxPolicyMode; //WebRtc定点噪音抑制器的策略模式，0表示轻微, 1表示适中, 2表示积极

    int iIsUseSpeexPreprocessor; //是否使用Speex预处理器，非0表示要使用，0表示不使用
    int iSpeexPreprocessorIsUseNs; //是否使用Speex预处理器的NS噪音抑制，非0表示要使用，0表示不使用
    int iSpeexPreprocessorNoiseSuppress; //Speex预处理器在NS噪音抑制时，噪音的最大程度衰减的分贝值
    int iSpeexPreprocessorIsUseVad; //是否使用Speex预处理器的VAD语音活动检测，非0表示要使用，0表示不使用
    int iSpeexPreprocessorVadProbStart; //Speex预处理器在VAD语音活动检测时，从无语音活动到有语音活动的判断百分比概率，最小为0，最大为100
    int iSpeexPreprocessorVadProbContinue; //Speex预处理器在VAD语音活动检测时，从有语音活动到无语音活动的判断百分比概率，最小为0，最大为100
    int iSpeexPreprocessorIsUseAgc; //是否使用Speex预处理器的AGC自动增益控制，非0表示要使用，0表示不使用
    int iSpeexPreprocessorAgcLevel; //Speex预处理器在AGC自动增益控制时，自动增益的等级，最小为1，最大为32768
    int iSpeexPreprocessorIsUseRec; //是否使用Speex预处理器的REC残余回音消除，非0表示要使用，0表示不使用
    int iSpeexPreprocessorEchoSuppress; //Speex预处理器在REC残余回音消除时，残余回音的最大程度衰减的分贝值
    int iSpeexPreprocessorEchoSuppressActive; //Speex预处理器在REC残余回音消除时，有近端语音活动时的残余回音的最大程度衰减的分贝值

    int iIsUsePCM; //是否使用PCM裸数据，非0表示要使用，0表示不使用

    int iIsUseSpeexCodec; //是否使用Speex编解码器，非0表示要使用，0表示不使用
    int iSpeexCodecEncoderIsUseVbr; //是否使用Speex编码器的动态比特率，非0表示要使用，0表示不使用
    int iSpeexCodecEncoderQuality; //Speex编码器的质量等级。质量等级越高，音质越好，压缩率越低。最低为0，最高为10。
    int iSpeexCodecEncoderComplexity; //Speex编码器的复杂度。复杂度越高，压缩率越高，CPU使用率越高，音质越好。最低为0，最高为10。
    int iSpeexCodecEncoderPlcExpectedLossRate; //Speex编码器的数据包丢失隐藏的预计丢失率。预计丢失率越高，抗网络抖动越强，压缩率越低。最低为0，最高为100。

    int iIsUseOpusCodec; //否使用Opus编解码器，非0表示要使用，0表示不使用

    int iIsUseAjb; //是否使用自适应抖动缓冲器，非0表示要使用，0表示不使用

    int iIsUseAACCodec;//是否使用AAC编码器，非0表示要使用，0表示不使用


    WebRtcAec clWebRtcAec; //WebRtc声学回音消除器类对象
    WebRtcAecm clWebRtcAecm; //WebRtc移动版声学回音消除器类对象
    SpeexAec clSpeexAec; //Speex声学回音消除器类对象
    WebRtcNsx clWebRtcNsx; //WebRtc定点版噪音抑制器类对象
    SpeexPreprocessor clSpeexPreprocessor; //Speex预处理器类对象
    SpeexEncoder clSpeexEncoder; //Speex编码器类对象
    SpeexDecoder clSpeexDecoder; //Speex解码器类对象
    LinkedList<short[]> m_clAlreadyAudioInputLinkedList; //存放已录音的链表类对象的内存指针
    LinkedList<short[]> m_clAlreadyAudioOutputLinkedList; //存放已播放的链表类对象的内存指针

    LinkedList<short[]> m_clAlreadyAudioReceiveLinkedList;

    private AudioInput audioInput;
    private AudioOutput audioOutput;

    short[] p_szhiPCMAudioInputData; //PCM格式音频输入数据
    short[] p_szhiPCMAudioOutputData; //PCM格式音频输出数据
    short[] p_szhiPCMAudioTempData; //PCM格式音频临时数据
    short[] p_szhiPCMReceiveAudioTempData; //PCM格式音频临时数据
    short[] p_szhiPCMAudioNullOutputData;

    Long clVoiceActivityStatus; //语音活动状态，1表示有语音活动，0表示无语音活动
    byte[] p_szhhiSpeexAudioInputData; //Speex格式音频输入数据
    Long p_clSpeexAudioInputDataSize; //Speex格式音频输入数据的内存长度，单位字节，大于0表示本帧Speex格式音频数据需要传输，等于0表示本帧Speex格式音频数据不需要传输
    byte[] p_szhhiTempData;
    int iLastAudioDataIsActive; //最后一帧音频数据是否有语音活动，1表示有语音活动，0表示无语音活动
    int iSocketPrereadSize; //本次套接字数据包的预读长度
    long lSendAudioDataTimeStamp; //发送音频数据的时间戳
    long lRecvAudioDataTimeStamp; //接收音频数据的时间戳
    int iTemp;

    byte[] p_aacCodecInputData;
    byte[] p_aacAudioTempData;

    boolean isReceiveable = false;

    public AudioProcessor(int frameSize, int samplingRate) {
        this.m_iFrameSize = frameSize;
        this.m_iSamplingRate = samplingRate;
    }


    public AudioProcessor webRtcConfigure(int isUseWebRtcAec, int isWebRtcAecNlpMode, int isUseWebRtcAecm,
                                          int isWebRtcAecmEchoMode, int isWebRtcAecmDelay,
                                          int isUseWebRtcNsx, int isWebRtcNsxPolicyMode) {
        this.iIsUseWebRtcAec = isUseWebRtcAec;
        this.iWebRtcAecNlpMode = isWebRtcAecNlpMode;
        this.iIsUseWebRtcAecm = isUseWebRtcAecm;
        this.iWebRtcAecmEchoMode = isWebRtcAecmEchoMode;
        this.iWebRtcAecmDelay = isWebRtcAecmDelay;
        this.iIsUseWebRtcNsx = isUseWebRtcNsx;
        this.iWebRtcNsxPolicyMode = isWebRtcNsxPolicyMode;
        return this;
    }

    public AudioProcessor speexConfigure(int isUseSpeexAec, int isSpeexAecFilterLength, int isUseSpeexPreprocessor,
                                         int isSpeexPreprocessorIsUseNs, int isSpeexPreprocessorNoiseSuppress, int isSpeexPreprocessorIsUseVad,
                                         int isSpeexPreprocessorVadProbStart, int isSpeexPreprocessorVadProbContinue, int isSpeexPreprocessorIsUseAgc,
                                         int isSpeexPreprocessorAgcLevel, int isSpeexPreprocessorIsUseRec, int isSpeexPreprocessorEchoSuppress,
                                         int isSpeexPreprocessorEchoSuppressActive) {
        this.iIsUseSpeexAec = isUseSpeexAec;
        this.iSpeexAecFilterLength = isSpeexAecFilterLength;
        this.iIsUseSpeexPreprocessor = isUseSpeexPreprocessor;
        this.iSpeexPreprocessorIsUseNs = isSpeexPreprocessorIsUseNs;
        this.iSpeexPreprocessorNoiseSuppress = isSpeexPreprocessorNoiseSuppress;
        this.iSpeexPreprocessorIsUseVad = isSpeexPreprocessorIsUseVad;
        this.iSpeexPreprocessorVadProbStart = isSpeexPreprocessorVadProbStart;
        this.iSpeexPreprocessorVadProbContinue = isSpeexPreprocessorVadProbContinue;
        this.iSpeexPreprocessorIsUseAgc = isSpeexPreprocessorIsUseAgc;
        this.iSpeexPreprocessorAgcLevel = isSpeexPreprocessorAgcLevel;
        this.iSpeexPreprocessorIsUseRec = isSpeexPreprocessorIsUseRec;
        this.iSpeexPreprocessorEchoSuppress = isSpeexPreprocessorEchoSuppress;
        this.iSpeexPreprocessorEchoSuppressActive = isSpeexPreprocessorEchoSuppressActive;
        return this;
    }

    public AudioProcessor baseConfigure(int isUsePCM, int isUseSpeexCodec, int isSpeexCodecEncoderIsUseVbr,
                                        int isSpeexCodecEncoderQuality, int isSpeexCodecEncoderComplexity, int isSpeexCodecEncoderPlcExpectedLossRate,
                                        int isUseOpusCodec, int isUseAjb, int isUseAACCodec) {

        this.iIsUsePCM = isUsePCM;
        this.iIsUseSpeexCodec = isUseSpeexCodec;
        this.iSpeexCodecEncoderIsUseVbr = isSpeexCodecEncoderIsUseVbr;
        this.iSpeexCodecEncoderQuality = isSpeexCodecEncoderQuality;
        this.iSpeexCodecEncoderComplexity = isSpeexCodecEncoderComplexity;
        this.iSpeexCodecEncoderPlcExpectedLossRate = isSpeexCodecEncoderPlcExpectedLossRate;
        this.iIsUseOpusCodec = isUseOpusCodec;
        this.iIsUseAjb = isUseAjb;
        this.iIsUseAACCodec = isUseAACCodec;
        return this;
    }

    private void initProcess() {

        p_szhiPCMReceiveAudioTempData = new short[m_iFrameSize];
        p_szhiPCMAudioTempData = new short[m_iFrameSize];
        p_szhhiSpeexAudioInputData = new byte[m_iFrameSize];
        p_clSpeexAudioInputDataSize = new Long(0);
        p_szhhiTempData = new byte[m_iFrameSize * 2 + 8];

        p_aacCodecInputData = new byte[m_iFrameSize * 2];
        p_aacAudioTempData = new byte[m_iFrameSize * 2];

        //初始化WebRtc声学回音消除器类对象
        if (iIsUseWebRtcAec != 0) {
            clWebRtcAec = new WebRtcAec();
            iTemp = clWebRtcAec.Init(m_iSamplingRate, iWebRtcAecNlpMode);
            if (iTemp == 0) {
                Log.i(clCurrentClassNameString, "初始化WebRtc声学回音消除器类对象成功！返回值：" + iTemp);
            } else {
                Log.i(clCurrentClassNameString, "初始化WebRtc声学回音消除器类对象失败！返回值：" + iTemp);
                return;
            }
        }

        //初始化WebRtc移动版声学回音消除器类对象
        if (iIsUseWebRtcAecm != 0) {
            clWebRtcAecm = new WebRtcAecm();
            iTemp = clWebRtcAecm.Init(m_iSamplingRate, iWebRtcAecmEchoMode, iWebRtcAecmDelay);
            if (iTemp == 0) {
                Log.i(clCurrentClassNameString, "初始化WebRtc移动版声学回音消除器类对象成功！返回值：" + iTemp);
            } else {
                Log.i(clCurrentClassNameString, "初始化WebRtc移动版声学回音消除器类对象失败！返回值：" + iTemp);
                return;
            }
        }

        //初始化Speex声学回音消除器类对象
        if (iIsUseSpeexAec != 0) {
            clSpeexAec = new SpeexAec();
            iTemp = clSpeexAec.Init(m_iFrameSize, m_iSamplingRate, iSpeexAecFilterLength);
            if (iTemp == 0) {
                Log.i(clCurrentClassNameString, "初始化Speex声学回音消除器类对象成功！返回值：" + iTemp);
            } else {
                Log.i(clCurrentClassNameString, "初始化Speex声学回音消除器类对象失败！返回值：" + iTemp);
                return;
            }
        }

        //初始化WebRtc定点版噪音抑制器类对象
        if (iIsUseWebRtcNsx != 0) {
            clWebRtcNsx = new WebRtcNsx();
            iTemp = clWebRtcNsx.Init(m_iSamplingRate, iWebRtcNsxPolicyMode);
            if (iTemp == 0) {
                Log.i(clCurrentClassNameString, "初始化Speex预处理器类对象成功！返回值：" + iTemp);
            } else {
                Log.i(clCurrentClassNameString, "初始化Speex预处理器类对象失败！返回值：" + iTemp);
                return;
            }
        }

        //初始化Speex预处理器类对象
        if (iIsUseSpeexPreprocessor != 0) {
            clSpeexPreprocessor = new SpeexPreprocessor();
            if (clSpeexAec != null)
                iTemp = clSpeexPreprocessor.Init(m_iSamplingRate, m_iFrameSize, iSpeexPreprocessorIsUseNs, iSpeexPreprocessorNoiseSuppress, iSpeexPreprocessorIsUseVad, iSpeexPreprocessorVadProbStart, iSpeexPreprocessorVadProbContinue, iSpeexPreprocessorIsUseAgc, iSpeexPreprocessorAgcLevel, iSpeexPreprocessorIsUseRec, clSpeexAec.GetSpeexEchoState().longValue(), iSpeexPreprocessorEchoSuppress, iSpeexPreprocessorEchoSuppressActive);
            else
                iTemp = clSpeexPreprocessor.Init(m_iSamplingRate, m_iFrameSize, iSpeexPreprocessorIsUseNs, iSpeexPreprocessorNoiseSuppress, iSpeexPreprocessorIsUseVad, iSpeexPreprocessorVadProbStart, iSpeexPreprocessorVadProbContinue, iSpeexPreprocessorIsUseAgc, iSpeexPreprocessorAgcLevel, 0, 0, 0, 0);
            if (iTemp == 0) {
                Log.i(clCurrentClassNameString, "初始化Speex预处理器类对象成功！返回值：" + iTemp);
            } else {
                Log.i(clCurrentClassNameString, "初始化Speex预处理器类对象失败！返回值：" + iTemp);
                return;
            }
        }

        //初始化PCM裸数据
        if (iIsUsePCM != 0) {
            //暂时没有什么要做的
        }

        //初始化Speex编码器类对象
        if (iIsUseSpeexCodec != 0) {
            clSpeexEncoder = new SpeexEncoder();
            iTemp = clSpeexEncoder.Init(m_iSamplingRate, iSpeexCodecEncoderIsUseVbr, iSpeexCodecEncoderQuality, iSpeexCodecEncoderComplexity, iSpeexCodecEncoderPlcExpectedLossRate);
            if (iTemp == 0) {
                Log.i(clCurrentClassNameString, "初始化Speex编码器类对象成功！返回值：" + iTemp);
            } else {
                Log.i(clCurrentClassNameString, "初始化Speex编码器类对象失败！返回值：" + iTemp);
                return;
            }
        }

        //初始化Speex解码器类对象
        if (iIsUseSpeexCodec != 0) {
            clSpeexDecoder = new SpeexDecoder();
            iTemp = clSpeexDecoder.Init(m_iSamplingRate);
            if (iTemp == 0) {
                Log.i(clCurrentClassNameString, "初始化Speex解码器类对象成功！返回值：" + iTemp);
            } else {
                Log.i(clCurrentClassNameString, "初始化Speex解码器类对象失败！返回值：" + iTemp);
                return;
            }
        }

        //初始化Opus编解码器
        if (iIsUseOpusCodec != 0) {
            //暂时没有什么要做的
        }


        //创建各个链表类对象
        m_clAlreadyAudioInputLinkedList = new LinkedList<short[]>(); //创建已录音的链表类对象
        m_clAlreadyAudioOutputLinkedList = new LinkedList<short[]>(); //创建已播放的链表类对象

        m_clAlreadyAudioReceiveLinkedList = new LinkedList<>();

        lLastPacketSendTime = System.currentTimeMillis(); //记录最后一个数据包的发送时间为当前时间
        lLastPacketRecvTime = System.currentTimeMillis(); //存放最后一个数据包的接收时间为当前时间

        clVoiceActivityStatus = new Long(1); //设置语音活动状态为1，为了让在没有使用语音活动检测的情况下永远都是有语音活动
        iLastAudioDataIsActive = 0; //设置最后一帧音频数据是否有语音活动为0，表示无语音活动
        iSocketPrereadSize = 0; //本次套接字数据包的预读长度为0
        lSendAudioDataTimeStamp = 0; //发送音频数据的时间戳为0
        lRecvAudioDataTimeStamp = 0; //接收音频数据的时间戳为0

        audioInput = new AudioInput(this.m_iFrameSize, this.m_iSamplingRate, m_clAlreadyAudioInputLinkedList);
        audioOutput = new AudioOutput(this.m_iFrameSize, this.m_iSamplingRate, this.m_clAlreadyAudioOutputLinkedList);
        initProcessCoder();
    }

    public void startProcess() {
        initProcess();
        iExitFlag = 1;
        start();
        if (audioProcessListener != null) {
            audioProcessListener.start();
        }
    }

    public void stopProcess() {
        Log.e("lzh","simpleaudio:stopProcess");
        iExitFlag = 0;

    }

    private void endToSend() {
        isReceiveable = false;
        audioInput.stopRecord();
        audioOutput.stopPlay();
        if (audioProcessListener != null) {
            audioProcessListener.stop();
        }
    }
    private void release(){
        audioInput = null;
        audioOutput = null;
    }

    @Override
    public void run() {
        audioInput.startRecord();
        isReceiveable = audioOutput.startPlay();
        while (iExitFlag == 1) {
            Log.i(clCurrentClassNameString, System.currentTimeMillis() + " 编码线程开始了:" + m_clAlreadyAudioOutputLinkedList.size());
            if (m_clAlreadyAudioInputLinkedList.size() > 0 && m_clAlreadyAudioOutputLinkedList.size() > 0) {

                Log.i(clCurrentClassNameString, System.currentTimeMillis() + " 从已录音的链表中取出第一帧音频数据");
//                先从已录音的链表中取出第一帧音频输入数据
                synchronized (m_clAlreadyAudioInputLinkedList) {
                    p_szhiPCMAudioInputData = m_clAlreadyAudioInputLinkedList.getFirst();
                    m_clAlreadyAudioInputLinkedList.removeFirst();
                }
                Log.i(clCurrentClassNameString, System.currentTimeMillis() + " 从已播放的链表中取出第一帧音频数据");
                //再从已播放的链表中取出第一帧音频输出数据
                synchronized (m_clAlreadyAudioOutputLinkedList) {
                    p_szhiPCMAudioOutputData = m_clAlreadyAudioOutputLinkedList.getFirst();
                    m_clAlreadyAudioOutputLinkedList.removeFirst();
                }
//                开始使用各项功能

//                //使用WebRtc声学回音消除器
                if (clWebRtcAec != null) {
                    iTemp = clWebRtcAec.Echo(p_szhiPCMAudioInputData, p_szhiPCMAudioOutputData, p_szhiPCMAudioTempData);
                    if (iTemp == 0) {
                        for (iTemp = 0; iTemp < p_szhiPCMAudioTempData.length; iTemp++)
                            p_szhiPCMAudioInputData[iTemp] = p_szhiPCMAudioTempData[iTemp];

                        Log.i(clCurrentClassNameString, System.currentTimeMillis() + " 使用WebRtc声学回音消除器成功！" + p_szhiPCMAudioTempData.length);
                    } else {
                        errorMsg(System.currentTimeMillis() + " 使用WebRtc声学回音消除器失败！错误码：" + iTemp);
                    }
                }

                //使用WebRtc移动版声学回音消除器
                if (clWebRtcAecm != null) {
                    Log.e("lzh", "nullllll:" + p_szhiPCMAudioInputData + "," + p_szhiPCMAudioOutputData + "," + p_szhiPCMAudioTempData);
                    iTemp = clWebRtcAecm.Echo(p_szhiPCMAudioInputData, p_szhiPCMAudioOutputData, p_szhiPCMAudioTempData);
                    if (iTemp == 0) {
                        for (iTemp = 0; iTemp < p_szhiPCMAudioTempData.length; iTemp++) {
                            try {
                                p_szhiPCMAudioInputData[iTemp] = p_szhiPCMAudioTempData[iTemp];
                            } catch (Exception e) {
                                Log.e("lzh", "ArrayIndexOutOfBoundsException:" + iTemp + "," + p_szhiPCMAudioTempData.length);
                            }

                        }

                        Log.i(clCurrentClassNameString, System.currentTimeMillis() + " 使用WebRtc移动版声学回音消除器成功！" + p_szhiPCMAudioTempData.length);
                    } else {
                        errorMsg(System.currentTimeMillis() + " 使用WebRtc移动版声学回音消除器失败！错误码：" + iTemp);
                    }
                }

                //使用Speex声学回音消除器
                if (clSpeexAec != null) {
                    iTemp = clSpeexAec.Aec(p_szhiPCMAudioInputData, p_szhiPCMAudioOutputData, p_szhiPCMAudioTempData);
                    if (iTemp == 0) {
                        for (iTemp = 0; iTemp < p_szhiPCMAudioTempData.length; iTemp++)
                            p_szhiPCMAudioInputData[iTemp] = p_szhiPCMAudioTempData[iTemp];

                        Log.i(clCurrentClassNameString, System.currentTimeMillis() + " 使用Speex声学回音消除器成功！" + p_szhiPCMAudioTempData.length);
                    } else {
                        errorMsg(System.currentTimeMillis() + " 使用Speex声学回音消除器失败！错误码：" + iTemp);
                    }
                }

                //使用WebRtc定点噪音抑制器
                if (clWebRtcNsx != null) {
                    iTemp = clWebRtcNsx.Process(m_iSamplingRate, p_szhiPCMAudioInputData, p_szhiPCMAudioInputData.length);
                    if (iTemp == 0) {
                        Log.i(clCurrentClassNameString, System.currentTimeMillis() + " 使用WebRtc定点噪音抑制器成功！" + p_szhiPCMAudioTempData.length);
                    } else {
                        errorMsg(System.currentTimeMillis() + " 使用WebRtc定点噪音抑制器失败！错误码：" + iTemp);
                    }
                }

                //使用Speex预处理器
                if (clSpeexPreprocessor != null) {
                    iTemp = clSpeexPreprocessor.Preprocess(p_szhiPCMAudioInputData, clVoiceActivityStatus);
                    if (iTemp == 0) {
                        Log.i(clCurrentClassNameString, System.currentTimeMillis() + " AudioEncoder:使用Speex预处理器成功！语音活动状态：" + clVoiceActivityStatus);
                    } else {
                        errorMsg(System.currentTimeMillis() + " 使用Speex预处理器失败！错误码：" + iTemp);
                    }
                }


                if (codeProcessListener != null) {
                    for (iTemp = 0; iTemp < p_szhiPCMAudioInputData.length; iTemp++) {
                        p_aacAudioTempData[iTemp * 2] = (byte) (p_szhiPCMAudioInputData[iTemp] & 0xFF);
                        p_aacAudioTempData[iTemp * 2 + 1] = (byte) ((p_szhiPCMAudioInputData[iTemp] & 0xFF00) >> 8);
                    }
                    codeProcessListener.encode(encodeEvent, p_aacAudioTempData);
                    continue;
                }
                send(p_szhiPCMAudioInputData, 0, p_szhiPCMAudioInputData.length);
                p_szhiPCMAudioInputData = null;
                p_szhiPCMAudioOutputData = null;


            }
            SystemClock.sleep(1);
        }
        endToSend();
    }


    void send(byte[] b, int off, int len) {
        if (audioProcessListener != null && clVoiceActivityStatus.intValue() == 1) //如果本帧音频输入数据为有语音活动
        {
            byte[] bytes = Arrays.copyOfRange(b, off, len);
            audioProcessListener.process(bytes);
//            receive(bytes);
        }
    }

    void send(short[] s, int off, int len) {
        if (audioProcessListener != null && clVoiceActivityStatus.intValue() == 1) //如果本帧音频输入数据为有语音活动
        {
            byte[] bytes = new byte[len * 2];
            for (iTemp = 0; iTemp < len; iTemp++) {
                bytes[iTemp] = (byte) (s[off + iTemp] & 0xFF);
                bytes[iTemp] = (byte) ((s[off + iTemp] & 0xFF00) >> 8);
            }
            audioProcessListener.process(bytes);
        }
    }


    public void receive(byte[] bytes) {
        if (isReceiveable) {
            Log.e(clCurrentClassNameString, System.currentTimeMillis() + " 接收到消息为：" + bytes.length);
            if (codeProcessListener != null) {
                codeProcessListener.decode(decodeEvent, bytes);
            } else //如果没有使用Speex解码器
            {
                audioOutput.play(bytes, 0, bytes.length);
            }
        }
    }


    private void errorMsg(String msg) {
        Log.i(clCurrentClassNameString, msg);
        if (audioProcessListener != null) {
            this.audioProcessListener.error(new Exception(msg));
        }

    }

    AudioProcessListener audioProcessListener;

    public interface AudioProcessListener {
        void process(byte[] bytes);

        void start();

        void stop();

        void error(Exception e);
    }

    public void setAudioProcessListener(AudioProcessListener audioProcessListener) {
        this.audioProcessListener = audioProcessListener;
    }


    private DecodeEvent decodeEvent;
    private EncodeEvent encodeEvent;

    private CodeProcessListener codeProcessListener;

    public void setCode(CodeProcessListener codeProcessListener) {
        this.codeProcessListener = codeProcessListener;
    }

    public interface CodeProcessListener {
        void encode(EncodeEvent e, byte[] bytes);

        void decode(DecodeEvent d, byte[] bytes);
    }

    public interface EncodeEvent {
        void onNext(byte[] bytes, int off, int len);

        void onNext(short[] shorts, int off, int len);
    }

    public interface DecodeEvent {
        void onNext(byte[] bytes, int off, int len);

        void onNext(short[] shorts, int off, int len);
    }

    private void initProcessCoder() {

        this.encodeEvent = new EncodeEvent() {
            @Override
            public void onNext(byte[] bytes, int off, int len) {
                send(bytes, off, len);
            }

            @Override
            public void onNext(short[] shorts, int off, int len) {
                send(shorts, off, len);
            }
        };


        this.decodeEvent = new DecodeEvent() {
            @Override
            public void onNext(byte[] bytes, int off, int len) {
                Log.e("lzh", "p_szhiPCMReceiveAudioTempData:" + p_szhiPCMReceiveAudioTempData.length + "," + len);
                audioOutput.play(bytes, 0, len);
//                for (iTemp = off; iTemp < m_iFrameSize; iTemp++) {
//                    p_szhiPCMReceiveAudioTempData[iTemp] = (short) (((short) bytes[iTemp * 2]) & 0xFF | ((short) bytes[iTemp * 2 + 1]) << 8);
//                }
//
//                onNext(Arrays.copyOfRange(p_szhiPCMReceiveAudioTempData,0,iTemp),0,iTemp);
            }

            @Override
            public void onNext(short[] shorts, int off, int len) {
                //将本帧音频输出数据放入自适应抖动缓冲器
                Log.e("lzh", "m_clAlreadyAudioReceiveLinkedList:存入解码后的接收链表");
                audioOutput.play(shorts, 0, shorts.length);
            }
        };
    }
}
