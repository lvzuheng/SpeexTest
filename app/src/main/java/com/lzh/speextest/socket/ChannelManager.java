package com.lzh.speextest.socket;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lzh on 2017/12/13.
 */

public class ChannelManager {

    private static Map<String,ChannelHelper> channelMap = new HashMap<>();

    public static void putChannel(String key,ChannelHelper channel){
        channelMap.put(key,channel);
    }

    public static ChannelHelper getChannel(String key){
        return channelMap.get(key);
    }

    public static void remove(String key){
        channelMap.remove(key);
    }
    public static void remove(ChannelHelper channel){
        if(channelMap.containsValue(channel)){
            for(String key : channelMap.keySet()){
                if(channelMap.get(key).equals(channel)){
                    remove(key);
                    return;
                }
            }
        }
    }

    public static boolean isExist(String key){
        return channelMap.containsKey(key);
    }

}
