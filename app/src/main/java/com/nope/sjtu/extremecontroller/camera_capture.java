package com.nope.sjtu.extremecontroller;

/**
 * Created by stelledge on 2019/11/5.
 *for camera usage
 */

import android.content.Context;
import android.graphics.Camera;
import android.hardware.camera2.*;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Log;
import android.app.Activity;

public class camera_capture {
    private static final String TAG = "Camera Service";

    public  CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private Camera mCamera;
    public camera_capture(Activity activity){
        mCameraManager= (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        //if (mCameraManager ==null)
    }
    private String getCamera() {
        String[] cameraIdList;
        try {
            cameraIdList = mCameraManager.getCameraIdList();
        } catch (CameraAccessException e){
            e.printStackTrace();
            return null;
        }
        for (String cameraId : cameraIdList) {
            CameraCharacteristics characteristic;
            try {
                characteristic = mCameraManager.getCameraCharacteristics(cameraId);
            }catch(CameraAccessException e){
                e.printStackTrace();
                return null;
            }
            Integer facing = characteristic.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                Log.d(TAG, "onSurfaceTextureAvailable: front camera is cameraid=" + cameraId);
                return cameraId;
            }
        }
        return null;
    }
    public Camera openCamera(){

    }


    private CameraDevice.StateCallback cameraCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d("CameraCallback", "Camera Opened");
            mCameraDevice = camera;
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d("CameraCallback", "Camera Disconnected");
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            Log.d("CameraCallback", "Camera Error");
            cameraDevice.close();
            mCameraDevice=null;
            //Toast.makeText(PusherSurface.this, "摄像头开启失败", Toast.LENGTH_SHORT).show();
        }
    };
}
