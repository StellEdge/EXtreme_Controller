package com.nope.sjtu.extremecontroller;

import android.app.Service;
import android.content.Intent;
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
        throw new UnsupportedOperationException("Not yet implemented");
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
                DataInputStream in=new DataInputStream(client.getInputStream());
                DataOutputStream out=new DataOutputStream(client.getOutputStream());
                Log.e("client","linked");
                while(isServiceConnect&&client.isConnected()){
                    if(in.readByte()==flag){
                        long len=in.readLong();
                        Log.e("data length:", Long.toString(len));
                        if (len < 0 || len > 2147483646)
                            continue;
                        byte[] msg = new byte[(int) len];
                        Log.e("msg", "finish");
                        in.read(msg);
                        final byte[] buffer = msg;
                        if (msg.equals("break")) {
                            in.close();
                            out.close();
                            client.close();
                            break;
                        }
                        //TODO 传出msg
                    }
                }
            }catch(Exception e){e.printStackTrace();}
        }
    }
}
