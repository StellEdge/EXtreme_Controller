package com.nope.sjtu.extremecontroller;



import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

//import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.Math.abs;

public class NLP extends AppCompatActivity implements com.baidu.speech.EventListener {
    protected TextView txtResult;
    protected Button btn;
    private EventManager asr;

    private double v, r;
    private int state = 0;
    private float vmax = 255;
    private float rmax = 255;

    SocketService socketService;


    private void start(){
        Map<String,Object> params = new LinkedHashMap<>();//传递Map<String,Object>的参数，会将Map自动序列化为json
        String event = null;
        event = SpeechConstant.ASR_START;
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME,false);//回调当前音量
        String json = null;
        json = new JSONObject(params).toString();//demo用json数据来做数据交换的方式
        asr.send(event, json, null, 0, 0);// 初始化EventManager对象,这个实例只能创建一次，就是我们上方创建的asr，此处开始传入
    }
    private void stop(){
        txtResult.setText("正在识别...");
        asr.send(SpeechConstant.ASR_STOP, null, null, 0, 0);//此处停止
    }
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nlp);
        initView();
        initPermission();
        asr = EventManagerFactory.create(this,"asr");//注册自己的输出事件类
        asr.registerListener(this);//// 调用 EventListener 中 onEvent方法
        btn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        start();
                        btn.setBackgroundResource(
                                R.drawable.startspeak);
                        break;
                    case MotionEvent.ACTION_UP:
                        stop();
                        btn.setBackgroundResource(
                                R.drawable.stopspeak);
                        break;
                    default:
                        return false;
                }

                return true;
            }
        });

        final Intent intent=new Intent(this,SocketService.class);
        bindService(intent,conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(conn);
        asr.send(SpeechConstant.ASR_CANCEL, "{}", null, 0, 0);
        asr.unregisterListener(this);//退出事件管理器
        // 必须与registerListener成对出现，否则可能造成内存泄露
    }
    public void onEvent(String name, String params, byte[] data, int offset, int length) {
        String resultTxt = null;
        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)){//识别结果参数
            if (params.contains("\"final_result\"")){//语义结果值
                try {
                    JSONObject json = new JSONObject(params);
                    String result = json.getString("best_result");//取得key的识别结果
                    resultTxt = result;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if (resultTxt != null){
            txtResult.setText(resultTxt);
            if(resultTxt.equals("前进"))
            {
                r = rmax;
                v = vmax;
                txtResult.append((String.format("\nV:%f, R:%f",v, r)));
            }
            else if(resultTxt.equals("后退"))
            {
                r = rmax;
                v = -vmax;
                txtResult.append((String.format("\nV:%f, R:%f",v, r)));
            }
            else if(resultTxt.equals("左转"))
            {
                r = 0.01;
                v = 0.2*vmax;
                txtResult.append((String.format("\nV:%f, R:%f",v, r)));
            }
            else if(resultTxt.equals("右转"))
            {
                r = -0.01;
                v = 0.2*vmax;
                txtResult.append((String.format("\nV:%f, R:%f",v, r)));
            }
            else if(resultTxt.equals("停止"))
            {
                r = rmax;
                v = 0;
                txtResult.append((String.format("\nV:%f, R:%f",v, r)));
            }
            else{
                return;
            }

            if(socketService!=null) {
                try {
                    socketService.sendCommand(ConvertCommand((float)v,(float)r));
                }catch (Exception er){er.printStackTrace();}
            }
        }
    }
    private void initView() {
        txtResult = findViewById(R.id.txtResult);
        btn = findViewById(R.id.btn);
        btn.setBackgroundResource(
                R.drawable.stopspeak);
    }
    private void initPermission() {
        String permissions[] = {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        ArrayList<String> toApplyList = new ArrayList<String>();

        for (String perm :permissions){
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                //进入到这里代表没有权限.

            }
        }
        String tmpList[] = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()){
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
    }

    public String ConvertCommand(float speed,float radius){
        //VL=Vc(1-L/2r)
        //VR=Vc(1+L/2r)
        float Vc=speed*0.8f;
        float VL_f=Vc*(1-1.2f/(2*radius));
        float VR_f=Vc*(1+1.2f/(2*radius));
        int VL=(int)(abs(VL_f));
        int VR=(int)(abs(VR_f));
        char sign;
        if (Vc>0) sign='0';
        else sign='1';
        String out=""+sign+String.format("%1$04d", VL)+sign+String.format("%1$04d", VR)+';';
        return out;
    }

    ServiceConnection conn=new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            //返回一个Service对象
            socketService = ((SocketService.SocketServiceBinder)service).getService();
        }
    };

}
