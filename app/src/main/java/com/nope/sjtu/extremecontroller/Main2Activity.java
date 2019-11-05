package com.nope.sjtu.extremecontroller;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import java.lang.Math;

public class Main2Activity extends AppCompatActivity {
    int widthPixels;//屏幕尺寸
    int heightPixels;
    int lspeed, rspeed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        measure();
    }

    Location location1 = new Location();
    Location location2 = new Location();

    private void measure(){//这个函数用来获得屏幕尺寸
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        this.widthPixels = metrics.widthPixels;
        this.heightPixels = metrics.heightPixels;
    };

    //李桐：下面这个函数是两只手指（注意不是多只）触摸监听，但效果我不确定！！
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int num = event.getPointerCount();
        int Id1, Id2;

        if (num==1) {//此时屏幕上只有一根手指
            Id1 = event.getPointerId(1);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    location2.update_condition(false);
                    location1.update(event.getX(1), event.getY(1));
                    location1.update_condition(true);
                    break;
                case MotionEvent.ACTION_UP:
                    location1.update_condition(false);
                    location2.update_condition(false);
            }
        }
        if(num==2) {
            Id1 = event.getPointerId(1);
            Id2 = event.getPointerId(2);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    location1.update(event.getX(1), event.getY(1));
                    location1.update_condition(true);
                    location2.update(event.getX(2), event.getY(2));
                    location1.update_condition(true);
                    break;
                case MotionEvent.ACTION_UP:
                    location1.update_condition(false);
                    location2.update_condition(false);

            }
        }

        calculate2(location1);
        calculate2(location2);

        BluetoothSend(" ",LinSend(this.lspeed, this.rspeed));
        //李桐：这里是给蓝牙传输，tag我用了空字符串
        return true;
    }


    //李桐：下面对应的是滑条算法
    private void calculate2 (Location mlocation){
        double x = mlocation.x();
        double y = (this.heightPixels/2) - mlocation.y();
        final double d = this.widthPixels/8;
        final double l = this.heightPixels/4;

        if (!mlocation.conditon()){
            this.lspeed = 0;
            this.rspeed = 0;
        }else {
            if (d<x&&x<3d){//the touch point is in the left area;
                if (y<=l&&y>=-l) this.lspeed = (int)(Math.signum(y)*500*y/l);
                if (y>l&&y<=1.5*l) this.lspeed = 500;
                if (y<-l&&y>=-1.5*l) this.lspeed = -500;
            };
            if (5d<x&&x<7d){
                if (y<=l&&y>=-l) this.rspeed = (int)(Math.signum(y)*500*y/l);
                if (y>l&&y<=1.5*l) this.rspeed = 500;
                if (y<-l&&y>=-1.5*l) this.rspeed = -500;
            }
        }
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

    private String LinSend(int right, int left){
        String str = "91" + Hexa(right) + Hexa(left);
        str = str.toUpperCase();
        return str;
    }
    //这个返回大写十六进制command，对应双轮条形UI界面

    //private BluetoothController BTC;
    void BluetoothSend(String tag,String commandline){
        //BTC.SendMsg(commandline);
        Log.d("send", commandline);
        //tag目前就是多留个接口
        //直接调用这个函数来进行蓝牙数据发送
    }
    void BluetoothReceive(String tag,String commandline){
        //蓝牙数据接受状况未定，
    }
}
