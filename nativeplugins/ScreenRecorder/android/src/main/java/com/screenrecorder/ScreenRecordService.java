package com.screenrecorder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;

public class ScreenRecordService extends Service {
    private static final String TAG = "ScreenRecordService";
    private static final String CHANNEL_ID = "ScreenRecordChannel";
    private static final int NOTIFICATION_ID = 1001;
    
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    
    private static RecordConfig recordConfig;
    private static RecordCallback callback;
    
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private MediaRecorder mediaRecorder;
    private String outputFilePath;
    private long recordStartTime;
    
    public interface RecordCallback {
        void onRecordStarted();
        void onRecordStopped(String filePath, long duration, int width, int height, long size);
        void onError(String error);
    }
    
    public static void setRecordConfig(RecordConfig config) {
        recordConfig = config;
    }
    
    public static void setCallback(RecordCallback cb) {
        callback = cb;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        
        String action = intent.getAction();
        
        if (ACTION_START.equals(action)) {
            int resultCode = intent.getIntExtra("resultCode", -1);
            Intent data = intent.getParcelableExtra("data");
            startRecording(resultCode, data);
        } else if (ACTION_STOP.equals(action)) {
            stopRecording();
        }
        
        return START_STICKY;
    }
    
    private void startRecording(int resultCode, Intent data) {
        startForeground(NOTIFICATION_ID, createNotification("Recording screen..."));
        
        try {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            
            mediaProjection = projectionManager.getMediaProjection(resultCode, data);
            
            if (mediaProjection == null) {
                if (callback != null) {
                    callback.onError("Failed to create MediaProjection");
                }
                stopSelf();
                return;
            }
            
            File outputDir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "ScreenRecordings");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            String fileName = "screen_recording_" + System.currentTimeMillis() + ".mp4";
            outputFilePath = new File(outputDir, fileName).getAbsolutePath();
            
            initMediaRecorder();
            
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenRecorder",
                recordConfig.width,
                recordConfig.height,
                recordConfig.screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder.getSurface(),
                null,
                null
            );
            
            mediaRecorder.start();
            recordStartTime = System.currentTimeMillis();
            
            if (callback != null) {
                callback.onRecordStarted();
            }
            
            Log.d(TAG, "Recording started: " + outputFilePath);
        } catch (Exception e) {
            Log.e(TAG, "Error starting recording", e);
            if (callback != null) {
                callback.onError("Failed to start recording: " + e.getMessage());
            }
            stopSelf();
        }
    }
    
    private void stopRecording() {
        try {
            if (mediaRecorder != null) {
                mediaRecorder.stop();
                mediaRecorder.reset();
                mediaRecorder.release();
                mediaRecorder = null;
            }
            
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            
            if (mediaProjection != null) {
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            long duration = System.currentTimeMillis() - recordStartTime;
            
            File file = new File(outputFilePath);
            long fileSize = file.length();
            
            if (callback != null) {
                callback.onRecordStopped(
                    outputFilePath,
                    duration,
                    recordConfig.width,
                    recordConfig.height,
                    fileSize
                );
            }
            
            Log.d(TAG, "Recording stopped: " + outputFilePath);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
            if (callback != null) {
                callback.onError("Failed to stop recording: " + e.getMessage());
            }
        } finally {
            stopForeground(true);
            stopSelf();
        }
    }
    
    private void initMediaRecorder() throws IOException {
        mediaRecorder = new MediaRecorder();
        
        if (recordConfig.withMic) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if (recordConfig.withMic) {
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setAudioEncodingBitRate(128000);
            mediaRecorder.setAudioSamplingRate(44100);
        }
        
        mediaRecorder.setVideoSize(recordConfig.width, recordConfig.height);
        mediaRecorder.setVideoFrameRate(recordConfig.fps);
        mediaRecorder.setVideoEncodingBitRate(recordConfig.bitrate);
        
        mediaRecorder.setOutputFile(outputFilePath);
        
        mediaRecorder.prepare();
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Screen Recording",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Screen recording service");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    private Notification createNotification(String text) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Recorder")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true);
        
        return builder.build();
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
    }
}
