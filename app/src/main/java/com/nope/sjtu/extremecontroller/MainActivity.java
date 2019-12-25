package com.nope.sjtu.extremecontroller;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Math;
import java.net.ServerSocket;
import java.net.Socket;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.floor;

public class MainActivity extends AppCompatActivity {
    int widthPixels;//屏幕尺寸
    int heightPixels;
    int speed=0, radius=66666;
    String temp;
    //private PaintBoard paintBoard;
    private Canvas canvas;
    public float cx=500 , cy =this.heightPixels/2;

    Location mlocation = new Location();
    private BluetoothSPP bt = new BluetoothSPP(this);
    private boolean BTavailable;
    private String BT="Bluetooth";

    private String TAG="Main Activity";
    private static Handler serverHandler=new Handler();
    /**
     * here starts camera part
     */
    //private CameraManager cameraManager= (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);

    /**
     * camera part end
     */
    private void measure(){//这个函数用来获得屏幕尺寸
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.widthPixels = metrics.widthPixels;
        this.heightPixels = metrics.heightPixels;
    };
    private TextView teller;
    public Axis axis;
    private TextureView cam_preview;
    private TextureView cam_receive;
    private camera_capture cam_cap;
    SocketService socketService;
    UniqueImage uniqueImage;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //paintBoard = new PaintBoard(this);
        canvas = new Canvas();
        axis = findViewById(R.id.axis);
        BluetoothOpen();

        teller = (TextView)findViewById(R.id.teller);
        teller.setText("temp");
        Button button_start = findViewById(R.id.button_start);
        button_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
            }
        });
        //TODO:四个按钮的监听函数
        Button button_bluetooth = findViewById(R.id.button_bluetooth);
        button_bluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bt.isBluetoothAvailable()){
                    BluetoothSend("","80 83");
                    Intent intent = new Intent(MainActivity.this, DeviceList.class);
                    startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE);
                } else {
                    Log.d(BT, "onClick: NO BLUETOOTH SUPPORT");
                }
            }
        });
        measure();

        //find view by ID 在oncreate中使用比较好。
        cam_preview=findViewById(R.id.camera_preview);
        cam_receive=findViewById(R.id.camera_receive);
        cam_cap=new camera_capture(this);

        //测试service socket通信
        uniqueImage=new UniqueImage();
        final Intent intent=new Intent(this,SocketService.class);
        bindService(intent,conn,Context.BIND_AUTO_CREATE);

    }

    ServiceConnection conn=new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个Service对象
            socketService = ((SocketService.SocketServiceBinder)service).getService();

            socketService.setOnReceiveImageListener(new SocketService.OnReceiveImageListener() {
                @Override
                public void receiveImage(UniqueImage uniqueImageS) {
                    uniqueImage.size=uniqueImageS.size;
                    uniqueImage.data=new byte[(int)uniqueImage.size];
                    uniqueImage.data=uniqueImageS.data;
                    Log.e("listen canvas","update");
                    Bitmap img = BitmapUtils.BytestoBitmap(uniqueImage.data, null);
                    final Canvas canvas = cam_receive.lockCanvas(null);
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);// 清空画布
                    Paint paint = new Paint();
                    Rect src = new Rect(0, 0, img.getWidth(), img.getHeight());
                    Rect dst = new Rect(0, 0, canvas.getWidth(), img.getHeight() * canvas.getWidth() / img.getWidth());
                    canvas.drawBitmap(img, src, dst, paint);//将bitmap画到画布上
                    cam_receive.unlockCanvasAndPost(canvas);//解锁画布同时提交
                }
            });

        }
    };

    private Handler mhandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            switch(msg.what){
                case 1:{
                    Log.d("CAMERA","New image ready.");
                }
                case 0: {
                    teller.setText((String) msg.obj);
                }
            }
        }
    };
    @Override
    protected void onResume(){
        super.onResume();
        Log.d(TAG,"On Resume");
        //重建相机权限。
        cam_cap.initTexture(cam_preview);
        //获得图像信息的方法：mOnImageDataReadyListener 事件回调
        cam_cap.setOnImageDataReadyListener(new camera_capture.OnImageDataReadyListener() {
            @Override
            public void OnImageDataReady(byte[] data) {
                //在这里写传出用的代码。
            }
        });
    }
    @Override
    protected void onPause(){
        super.onPause();
        cam_cap.closeCamera();

    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) { //判断数据是否为空
            return;
        }

        if(requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if(resultCode == RESULT_OK){
                String address = data.getExtras().getString(BluetoothState.EXTRA_DEVICE_ADDRESS);
                bt.connect(address);
            }
        } else if(requestCode == BluetoothState.REQUEST_ENABLE_BT) {
            if(resultCode == RESULT_OK) {
                bt.setupService();
                //bt.startService(BluetoothState.DEVICE_ANDROID);这里用的是HC-06
                bt.startService(BluetoothState.DEVICE_OTHER);
                //setup();
            } else {
                // Do something if user doesn't choose any device (Pressed back)
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mlocation.update(event.getX(), event.getY());
                mlocation.update_condition(true);
                break;
            case MotionEvent.ACTION_UP:
                mlocation.update_condition(false);
                break;
            default:
                break;
        }
        //axis.SetAxis(mlocation.x(),mlocation.y());

        /**
         * Called when the user touches the button
         */

        calculate(mlocation);
        BluetoothSend(" ",ConvertCommand(this.speed, this.radius));
        return true;
    }

    //李桐：下面是计算速度,对应圆盘操作模式,更新this.speed和this.radius
    private void calculate(Location mlocation){
        double centerx = 500;
        double centery = this.heightPixels/2;
        double x = mlocation.x() - centerx;
        double y = mlocation.y() - centery;
        final double R = 350, k = 1.5;//R is the radius of the visible circle
        double a, r;
        final double b = 1.0,  i = Math.PI*8.0/18, j = Math.PI*1.0/18;//i is the top angle
        double tmp;
        if (!mlocation.conditon()){
            this.speed = 0;
            this.radius = 10000;
            this.cx = 500;
            this.cy = this.heightPixels/2;

        }else{
            r = Math.sqrt(x*x+y*y);
            a = Math.atan(Math.abs(y*1.0/x));

            if (r>R && r<k*R){//k*R is the max valiad radius
                this.speed = (int)Math.signum(-y)*500;
                this.cx = (int)(R*Math.cos(a)*Math.signum(x)+centerx);
                this.cy = (int)(R*Math.sin(a)*Math.signum(y)+centery);
            }else {
                if (r <= R) {
                    this.speed = (int) (Math.signum(-y) * r * 500 / R);
                    this.cx = (int)mlocation.x();
                    this.cy = (int)mlocation.y();
                }else this.speed = 0;
            }

            if (a>i) this.radius = 10000;
            else {
                if (a<j){
                    this.radius = (int)(1*Math.signum(-x));
                }else{
                    tmp = (Math.exp(b*(a-j)) - 1.0) / (Math.exp(b * i) - 1.0);
                    this.radius = (int) (2000 * tmp * (Math.signum(-x)));
                }
            }
        }
        axis.SetAxis((int)cx,(int)cy);
    }

    // Created by hd on 2018/4/30.

    private String Hexa(int num) {
        if (num < 0)
        {
            return Integer.toHexString(num).substring(4);
        }
        String str = Integer.toHexString(num);
        while (str.length()<4)
        {
            str = "0" + str;
        }
        return str;
    }
    // 这个是我自己为了算数用的函数，真.private，不要调用

    private String CirSend(int v, int r){
        if(r == 0) {
            r = 1;
        }
        if (r>2000) {
            r = 32768;
        }
        if (r<-2000) {
            r = 32767;
        }
        String Hexstrv=Hexa(v);
        String Hexstrr=Hexa(r);
        String str = "89 " + Hexstrv.substring(0,2)+" "+Hexstrv.substring(2)
                +" "+Hexstrr.substring(0,2)+" "+Hexstrr.substring(2);

        str = str.toUpperCase();
        return str;
    }
    //这个返回大写十六进制command，对应圆形UI界面

    private String LinSend(int right, int left){
        String Hexstrl=Hexa(left);
        String Hexstrr=Hexa(right);
        String str = "91" +" "+Hexstrr.substring(0,2)+" "+Hexstrr.substring(2)
                +" "+Hexstrl.substring(0,2)+" "+Hexstrl.substring(2);
        str = str.toUpperCase();
        return str;
    }
    //这个返回大写十六进制command，对应双轮条形UI界面
    //StellEdge：不存在的
    void BluetoothOpen(){
        if(!bt.isBluetoothAvailable()) {
            BTavailable=false;
            //Log.d(BT, "onCreate: NO BLUETOOTH SUPPORT");
        } else {
            BTavailable=true;
            bt.setupService();
            bt.startService(BluetoothState.DEVICE_OTHER);
            if(!bt.isBluetoothEnabled()) {
                bt.enable();
            }
        }
        //文本接收
        bt.setOnDataReceivedListener(new BluetoothSPP.OnDataReceivedListener() {
            public void onDataReceived(byte[] data, String message) {
                // Do something when data incoming
            }
        });
    }
    void BluetoothSend(String tag,String commandline){
        //tag目前就是多留个接口
        //直接调用这个函数来进行蓝牙数据发送
        //If you want to send any data. boolean parameter is mean that data will send with ending by LF and CR or not.
        //If yes your data will added by LF & CR 末尾添加回车或换行
        Log.d(TAG, "BluetoothSend: "+commandline);
        //byte[] myb=HexCommandtoByte(commandline.getBytes());
        byte[] myb=commandline.getBytes();
        if (bt.isServiceAvailable()) {
            bt.send(myb, false);
        }else{
            Log.d(TAG, "BluetoothSend:No bluetooth connection.");
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (radius >= 2000) {
                    temp = "speed:" + speed + ", radius:" + "66666";
                }
                else{
                    temp = "speed:" + speed + ", radius:" + radius;}
                Log.d("hello", temp);
                mhandler.sendMessage(mhandler.obtainMessage(0,temp));
                //这里的操作是为了节约构造对象的内存开销
                /*try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
            }
        }).start();
    }
    private final float car_width=11.2f;
    private final float voltage_correction=0.8f;
    public String ConvertCommand(float speed,float radius){
        //VL=Vc(1-L/2r)
        //VR=Vc(1+L/2r)
        float Vc=speed*voltage_correction;
        float VL_f=Vc*(1-car_width/(2*radius));
        float VR_f=Vc*(1+car_width/(2*radius));
        int VL=(int)(abs(VL_f));
        int VR=(int)(abs(VR_f));
        char sign;
        if (Vc>0) sign='0';
        else sign='1';
        String out=""+sign+String.format("%1$04d", VL)+sign+String.format("%1$04d", VR)+';';
        return out;
    }
    //StellEdge:16进制byte字符串转byte编码
    public static byte[] HexCommandtoByte(byte[] data) {
        if (data == null) {
            return null;
        }
        int nLength = data.length;
        String TemString = new String(data, 0, nLength);
        String[] strings = TemString.split(" ");
        nLength = strings.length;
        data = new byte[nLength];

        for (int i = 0; i < nLength; i++) {
            if (strings[i].length() != 2) {
                data[i] = 00;
                continue;
            }
            try {
                data[i] = (byte)Integer.parseInt(strings[i], 16);
            } catch (Exception e) {
                data[i] = 00;
                continue;
            }
        }
        return data;
    }
}

