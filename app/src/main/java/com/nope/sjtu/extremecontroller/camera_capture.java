package com.nope.sjtu.extremecontroller;

/**
 * Created by stelledge on 2019/11/5.
 *for camera usage
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.app.Activity;

public class camera_capture {
    private static final String TAG = "Camera Service";

    public CameraDevice mCameraDevice;
    private CameraManager mCameraManager;
    private Camera mCamera;
    private HandlerThread mhandlerThread;
    Handler mHandler;  //for async access
    Activity callingActivity;
    public camera_capture(Activity activity) {
        mCameraManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);

        //handler setting
        mhandlerThread = new HandlerThread("CameraThread");
        mhandlerThread.start();
        mHandler = new Handler(mhandlerThread.getLooper());

        callingActivity=activity;
    }

    private String getCameraID() {
        String[] cameraIdList;
        try {
            cameraIdList = mCameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            e.printStackTrace();
            return null;
        }
        for (String cameraId : cameraIdList) {
            CameraCharacteristics characteristic;
            try {
                characteristic = mCameraManager.getCameraCharacteristics(cameraId);
            } catch (CameraAccessException e) {
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

    public void openCamera(String cameraId, final CameraDevice.StateCallback callback, Handler handler) {
        try {
            if (ActivityCompat.checkSelfPermission(callingActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mCameraManager.openCamera(cameraId,new CameraDevice.StateCallback(){
                @Override
                public void onOpened(CameraDevice camera) {
                    Log.d(TAG,"onOpened");
                    mCameraDevice = camera;
                    //camera.createCaptureSession();
                }
                @Override
                public void onDisconnected(CameraDevice camera) {
                    Log.d(TAG,"onDisconnected");
                }
                @Override
                public void onError(CameraDevice camera, int e) {
                    Log.d(TAG,"onError $error");
                }
            }, handler);
        } catch (CameraAccessException e){
            e.printStackTrace();
            return;
        }
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
