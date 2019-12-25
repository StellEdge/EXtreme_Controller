package com.nope.sjtu.extremecontroller;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.support.v4.content.res.ConfigurationHelper;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ResourceBundle;
import java.util.concurrent.ThreadPoolExecutor;

public class SocketService extends Service {
    private final String TAG="SocketService";
    private boolean isServiceConnect=true;
    private byte flag=(byte)0xee;
    private DataInputStream in;
    private DataOutputStream out;
    private UniqueImage uniqueImage=new UniqueImage();
    private OnReceiveImageListener onReceiveImageListener;

    public SocketService(){

    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.e(TAG,"service onCreate");
        new Thread(new Server()).start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //this.startId = startId;
        Log.e(TAG, "onStartCommand---startId: " + startId);
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            ResourceBundle.Control control = (ResourceBundle.Control) bundle.getSerializable("Key");
            if (control != null) {
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        isServiceConnect=false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent){
        Log.e(TAG, "onBind");
        return new SocketServiceBinder();
    }

    public class SocketServiceBinder extends Binder {
        public SocketService getService(){
            return SocketService.this;
        }
    }



    public interface OnReceiveImageListener{
        void receiveImage(UniqueImage uniqueImageS);
    }

    //外部activity使用，返回得到的字节流信息
    public UniqueImage getImage(){
        return uniqueImage;
    }

    public void setOnReceiveImageListener(OnReceiveImageListener onReceiveImageListener){
        this.onReceiveImageListener=onReceiveImageListener;
    }

    private class Server implements Runnable{
        @Override
        public void run(){
            try{
                ServerSocket serverSocket;
                Socket client=null;
                serverSocket=new ServerSocket(12345);
                Log.e(TAG,"serverSocket create");
                while(isServiceConnect&&client==null){
                    client=serverSocket.accept();
                }
                in=new DataInputStream(client.getInputStream());
                out=new DataOutputStream(client.getOutputStream());
                Log.e("client","linked");
                while(isServiceConnect&&client.isConnected()){
                    if(in.readByte()==flag){
                        uniqueImage.size=in.readLong();
                        Log.e("data length:", Long.toString(uniqueImage.size));
                        if (uniqueImage.size < 0 || uniqueImage.size > 100000000)
                            continue;
                        uniqueImage.data = new byte[(int) uniqueImage.size];
                        Log.e("msg", "finish");
                        in.read(uniqueImage.data);
                        if (uniqueImage.data.equals("break")) {
                            in.close();
                            out.close();
                            client.close();
                            break;
                        }
                        try {
                            onReceiveImageListener.receiveImage(uniqueImage);
                        }catch(Exception e){e.printStackTrace();}
                    }
                }
            }catch(Exception e){e.printStackTrace();}
        }
    }
}
