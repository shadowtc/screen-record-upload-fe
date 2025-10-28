package com.screenrecorder;

public class RecordConfig {
    public int width;
    public int height;
    public int fps;
    public int bitrate;
    public boolean withMic;
    public int screenDensity;
    
    public RecordConfig(int width, int height, int fps, int bitrate, boolean withMic, int screenDensity) {
        this.width = width;
        this.height = height;
        this.fps = fps;
        this.bitrate = bitrate;
        this.withMic = withMic;
        this.screenDensity = screenDensity;
    }
}
