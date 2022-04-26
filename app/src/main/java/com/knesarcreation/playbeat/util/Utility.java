
package com.knesarcreation.playbeat.util;

public class Utility {

    //audio format in which file after trim will be saved.
    public static final String AUDIO_FORMAT = ".mp3";

    //audio mime type in which file after trim will be saved.
    public static final String AUDIO_MIME_TYPE = "audio/mp3";

    public static long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

}
