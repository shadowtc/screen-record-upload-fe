package com.screenrecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.util.DisplayMetrics;

import com.alibaba.fastjson.JSONObject;

import io.dcloud.feature.uniapp.annotation.UniJSMethod;
import io.dcloud.feature.uniapp.bridge.UniJSCallback;
import io.dcloud.feature.uniapp.common.UniModule;

public class ScreenRecorderModule extends UniModule {
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;
    private static final int REQUEST_CODE_AUDIO_PERMISSION = 1002;
    
    private ScreenRecordService screenRecordService;
    private UniJSCallback startRecordCallback;
    private UniJSCallback stopRecordCallback;
    
    @UniJSMethod(uiThread = true)
    public void startRecord(JSONObject options, UniJSCallback callback) {
        this.startRecordCallback = callback;
        
        Activity activity = mUniSDKInstance.getContext();
        if (activity == null) {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("message", "Activity not found");
            callback.invoke(result);
            return;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = new String[]{
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            
            if (!hasPermissions(activity, permissions)) {
                activity.requestPermissions(permissions, REQUEST_CODE_AUDIO_PERMISSION);
            }
        }
        
        String resolution = options.getString("resolution");
        int fps = options.getIntValue("fps");
        int bitrate = options.getIntValue("bitrate");
        boolean withMic = options.getBooleanValue("withMic");
        
        int width = 1280;
        int height = 720;
        
        if ("1080p".equals(resolution)) {
            width = 1920;
            height = 1080;
        } else if ("480p".equals(resolution)) {
            width = 854;
            height = 480;
        }
        
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int screenDensity = metrics.densityDpi;
        
        RecordConfig config = new RecordConfig(width, height, fps, bitrate, withMic, screenDensity);
        
        MediaProjectionManager projectionManager = 
            (MediaProjectionManager) activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        if (projectionManager != null) {
            Intent captureIntent = projectionManager.createScreenCaptureIntent();
            activity.startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
            
            ScreenRecordService.setRecordConfig(config);
            ScreenRecordService.setCallback(new ScreenRecordService.RecordCallback() {
                @Override
                public void onRecordStarted() {
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("message", "Recording started");
                    if (startRecordCallback != null) {
                        startRecordCallback.invoke(result);
                    }
                }
                
                @Override
                public void onRecordStopped(String filePath, long duration, int width, int height, long size) {
                    JSONObject result = new JSONObject();
                    result.put("success", true);
                    result.put("filePath", filePath);
                    result.put("duration", duration);
                    result.put("width", width);
                    result.put("height", height);
                    result.put("size", size);
                    if (stopRecordCallback != null) {
                        stopRecordCallback.invoke(result);
                    }
                }
                
                @Override
                public void onError(String error) {
                    JSONObject result = new JSONObject();
                    result.put("success", false);
                    result.put("message", error);
                    if (startRecordCallback != null) {
                        startRecordCallback.invoke(result);
                    }
                }
            });
        } else {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("message", "MediaProjection not supported");
            callback.invoke(result);
        }
    }
    
    @UniJSMethod(uiThread = false)
    public void stopRecord(UniJSCallback callback) {
        this.stopRecordCallback = callback;
        
        Activity activity = mUniSDKInstance.getContext();
        if (activity == null) {
            JSONObject result = new JSONObject();
            result.put("success", false);
            result.put("message", "Activity not found");
            callback.invoke(result);
            return;
        }
        
        Intent stopIntent = new Intent(activity, ScreenRecordService.class);
        stopIntent.setAction(ScreenRecordService.ACTION_STOP);
        activity.startService(stopIntent);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            Activity activity = mUniSDKInstance.getContext();
            if (activity == null) {
                return;
            }
            
            if (resultCode == Activity.RESULT_OK) {
                Intent serviceIntent = new Intent(activity, ScreenRecordService.class);
                serviceIntent.setAction(ScreenRecordService.ACTION_START);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    activity.startForegroundService(serviceIntent);
                } else {
                    activity.startService(serviceIntent);
                }
            } else {
                JSONObject result = new JSONObject();
                result.put("success", false);
                result.put("message", "Screen capture permission denied");
                if (startRecordCallback != null) {
                    startRecordCallback.invoke(result);
                }
            }
        }
    }
    
    private boolean hasPermissions(Activity activity, String[] permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String permission : permissions) {
                if (activity.checkSelfPermission(permission) != 
                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
