package com.nope.sjtu.extremecontroller;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientActivity extends AppCompatActivity implements Runnable{
    //传输信息：起始标志flag 1byte，长度long，最后是数据byte[]

    //定义相关变量,完成初始化
    private TextView txtshow;
    private EditText editsend,edit_ip;
    private Button btnsend,ip_send;
    private String HOST="192.168.3.3";
    private int PORT=22222;
    private boolean hasSet=false;
    private Socket socket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;
    private byte content;
    private byte flag=(byte)0xee;//起始标志
    private StringBuilder sb = null;

    //定义一个handler对象,用来刷新界面
    public Handler handler = new Handler();

    public Handler testHandler=new Handler();

    private camera_capture cam_cap;
    private TextureView cam_preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);

        sb = new StringBuilder();
        txtshow = (TextView) findViewById(R.id.msg_show);
        editsend = (EditText) findViewById(R.id.edit_msg);
        btnsend = (Button) findViewById(R.id.msg_send);
        edit_ip=findViewById(R.id.edit_ip);
        ip_send=findViewById(R.id.ip_send);

        ip_send.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String[] tmp=edit_ip.getText().toString().split(":");
                HOST = tmp[0];
                PORT=Integer.parseInt(tmp[1]);
                hasSet=true;
            }
        });


        //当程序一开始运行的时候就实例化Socket对象,与服务端进行连接,获取输入输出流
        //因为4.0以后不能再主线程中进行网络操作,所以需要另外开辟一个线程
        new Thread() {
            public void run() {
                try {
                    while(!hasSet){

                    }
                    socket = new Socket(HOST, PORT);
                    if(socket.isConnected()) {
                        testHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                txtshow.setText(txtshow.getText() + "\n" + "00000");
                            }
                        });
                    }
                    in = new DataInputStream(socket.getInputStream());
                    out = new DataOutputStream(socket.getOutputStream());
                    while(true) {
                        if ((content = in.readByte()) == flag) {
                            long len = in.readLong();
                            byte[] msg = new byte[(int) len];
                            in.read(msg);
                            //下面测试用，正式使用时可删去
                            final String s = new String(msg);
                            testHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    String tmp = txtshow.getText().toString();
                                    tmp += "\n" + s;
                                    txtshow.setText(tmp);
                                }
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();

        btnsend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String txt = editsend.getText().toString();
                new Thread(){
                    public void run() {
                        try {
                            out.writeByte(flag);
                            out.writeLong(txt.getBytes().length);
                            out.write(txt.getBytes());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
        cam_preview=findViewById(R.id.camera_preview);
        cam_cap=new camera_capture(this);
        cam_cap.initTexture(cam_preview);
        //获得图像信息的方法：mOnImageDataReadyListener 事件回调
        cam_cap.setOnImageDataReadyListener(new camera_capture.OnImageDataReadyListener() {
            @Override
            public void OnImageDataReady(byte[] data) {
                //在这里写传出用的代码。
                Log.d("CLIENT","Image ready");
            }
        });

        //new Thread(ClientActivity.this).start();
    }

    //重写run方法,在该方法中输入流的读取
    @Override
    public void run() {
        try {
//            while (true) {
//                if ((content = in.readByte()) == flag) {
//                    long len = in.readLong();
//                    byte[] msg = new byte[(int) len];
//                    in.read(msg);
//                    //下面测试用，正式使用时可删去
//                    final String s = new String(msg);
//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            String tmp = txtshow.getText().toString();
//                            tmp += "\n" + s;
//                            txtshow.setText(tmp);
//                        }
//                    });
//                }
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
