package com.nope.sjtu.extremecontroller;

/**
 * Created by stelledge on 2019/11/5.
 *for camera usage
 */

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;


import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.app.Activity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.graphics.SurfaceTexture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class camera_capture {
    private static final String TAG = "Camera Service";
    private Camera mCamera;
    private CameraDevice mCameraDevice;
    private String mCameraId;

    private TextureView mTextureView;
    private Size previewSize;
    private CaptureRequest.Builder mPreviewBuilder;
    private ImageReader mImageReader;
    //private OnGetBitmapInterface mOnGetBitmapInterface;

    private HandlerThread mhandlerThread;
    private Handler mHandler;  //for async access
    private Activity callingActivity;
    public byte[] image_data;
    public interface OnImageDataReadyListener {
        public void OnImageDataReady(byte[] data);
    }
    private OnImageDataReadyListener mOnImageDataReadyListener;
    public void setOnImageDataReadyListener(OnImageDataReadyListener onImageDataReadyListener) {
        mOnImageDataReadyListener = onImageDataReadyListener;
    }

    public camera_capture(Activity activity) {

        //handler setting
        mhandlerThread = new HandlerThread("CameraThread");
        mhandlerThread.start();
        mHandler = new Handler(mhandlerThread.getLooper());

        callingActivity=activity;
    }

    /**
     * setting textureView for camERA preview.
     *
     * @param textureView 需要预览的TextureView
     */
    public void initTexture(TextureView textureView) {
        mTextureView = textureView;
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openCamera(width, height);
            }
            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}
            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {return false;}
            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {}
        });
    }

    private CameraDevice.StateCallback mCameraDeviceStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            SurfaceTexture surfaceTexture=mTextureView.getSurfaceTexture();
            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            Surface previewSurface = new Surface(surfaceTexture);
            try {
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                //There it supports multi putput target
                mPreviewBuilder.addTarget(previewSurface);
                mPreviewBuilder.addTarget(mImageReader.getSurface());
                mCameraDevice.createCaptureSession(Arrays.asList(previewSurface,mImageReader.getSurface()), mCameraCaptureSessionStateCallBack , mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            camera.close();
            mCameraDevice = null;
        }
        @Override
        public void onClosed(CameraDevice camera){
            super.onClosed(camera);
        }
    };

    private CameraCaptureSession.StateCallback mCameraCaptureSessionStateCallBack = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            CaptureRequest request = mPreviewBuilder.build();
            try {
                //获取一个Image，one-shot
//                session.capture(request, null, mCameraHandler);
                //开启获取Image，repeat模式
                session.setRepeatingRequest(request, null, mHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession session) {}
    };

    private Size getOptimalPreviewSize(Size[] sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizes == null) return null;

        Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Try to find an size match aspect ratio and size
        for (Size size : sizes) {
            double ratio = (double) size.getWidth()/ size.getHeight();
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.getHeight() - targetHeight);
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Size size : sizes) {
                if (Math.abs(size.getHeight() - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.getHeight()- targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public void openCamera(int view_width,int view_height) {
        CameraManager mCameraManager = (CameraManager) callingActivity.getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(callingActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d(TAG,"No permission to access camera.");
                ActivityCompat.requestPermissions(callingActivity,new String[]{Manifest.permission.CAMERA},0);
                return;
            }
            for (String cameraId : mCameraManager.getCameraIdList()) {
                //get camera characteristics for camera with cameraID
                CameraCharacteristics cameraCharacteristics =mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = cameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                //使用后置摄像头
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    if (map != null) {
                        previewSize = getOptimalPreviewSize(map.getOutputSizes(SurfaceTexture.class), view_width, view_height);
                        mCameraId = cameraId;
                    }
                }

            }
            //open camera here if succeeded
            mImageReader = ImageReader.newInstance(previewSize.getWidth(), previewSize.getHeight(),
                    ImageFormat.JPEG, 2);
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mHandler);

            mCameraManager.openCamera(mCameraId,mCameraDeviceStateCallback, mHandler);

        } catch (CameraAccessException e){
            e.printStackTrace();
            return;
        }
    }

    private ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            //因为是ImageFormat.JPEG格式，所以 image.getPlanes()返回的数组只有一个，也就是第0个。
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);
            //JPEG to bitmap
            Bitmap temp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            //因为摄像机数据默认是横的，所以需要旋转90度。
            Bitmap newBitmap = rotate(temp, 90);
            byte[] outdata = transImage(newBitmap, 848, 480);
            int length = outdata.length;

            //out.write((byte) 0xA0);
            //out.write(intTOBytes(datalen));
            //out.write(outdata, 0, datalen);
            //mHandler.sendMessage(mHandler.obtainMessage(1,outdata));
            if(mOnImageDataReadyListener!=null){
                mOnImageDataReadyListener.OnImageDataReady(outdata);
            }

            //抛出去展示或存储。
            //Log.d(TAG,"New data ready");
            //一定需要close，否则不会收到新的Image回调。
            image.close();
        }
    };

    private byte[] transImage(Bitmap bitmap, int width, int height) {
        try {
            int bitmapWidth = bitmap.getWidth();
            int bitmapHeight = bitmap.getHeight();

            float scaleWidth = (float) width / bitmapWidth;
            float scaleHeight = (float) height / bitmapHeight;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleHeight);
            //Resize
            Bitmap resizeBitemp = Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, false);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizeBitemp.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            outputStream.close();
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            if (!resizeBitemp.isRecycled()) {
                resizeBitemp.recycle();
            }
            return byteArray;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private byte[] intTOBytes(int value){
        //Log.e("cam",value+"d");
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte)((value >> 16) & 0xFF);
        src[1] = (byte)((value >> 8) & 0xFF);
        src[0] = (byte)(value & 0xFF);
        return src;
    }

    private static Bitmap rotate(Bitmap bitmap, int angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public void closeCamera() {
        mCameraDevice.close();
    }

}
