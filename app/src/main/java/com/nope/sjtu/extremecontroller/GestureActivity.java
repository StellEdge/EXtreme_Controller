package com.nope.sjtu.extremecontroller;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.ArrayList;

import static java.lang.Math.abs;

public class GestureActivity extends AppCompatActivity  implements
        View.OnTouchListener {

    public static final String TAG = "MoveLoggerActivity";
    private static final int dragThres_main = 300;
    private static final int dragThres_sub = 100;
    /* Slop constant for this device */
    private int mTouchSlop;
    /* Initial touch point */
    private Point mInitialTouch;
    private ArrayList<Point> allPointList;
    private SocketService socketService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        findViewById(R.id.view_logall).setOnTouchListener(this);
        //findViewById(R.id.view_logslop).setOnTouchListener(this);

        mTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();
        mInitialTouch = new Point();
        allPointList = new ArrayList<>();

        final Intent intent=new Intent(this,SocketService.class);
        bindService(intent,conn, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unbindService(conn);
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
            mInitialTouch.set((int)event.getX(), (int)event.getY());
            //Must declare interest to get more events
            allPointList.clear();
            return false;
        } else if(event.getAction() == MotionEvent.ACTION_UP){
            // accumulate what kind of touch is being done.
            if (!allPointList.isEmpty()){
                Point first_point = allPointList.get(0);
                Point last_point = allPointList.get(allPointList.size() - 1);
                int VL=0,VR=0,vmax=100;
                if(last_point.x-first_point.x>=dragThres_main && abs(last_point.y-first_point.y) <=dragThres_sub){
                    //right scroll
                    Log.i(TAG, String.format("A right scroll"));
                    VL=vmax;
                    VR=vmax/10;
                }
                else if(first_point.x-last_point.x>=dragThres_main && abs(last_point.y-first_point.y) <=dragThres_sub){
                    //left scroll
                    Log.i(TAG, String.format("A left scroll"));
                    VL=vmax/10;
                    VR=vmax;
                }
                else if(last_point.y-first_point.y>=dragThres_main && abs(last_point.x-first_point.x) <=dragThres_sub){
                    //down scroll
                    Log.i(TAG, String.format("A down scroll"));
                    VL=-vmax;
                    VR=-vmax;
                }
                else if(first_point.y-last_point.y>=dragThres_main && abs(last_point.x-first_point.x) <=dragThres_sub){
                    //up scroll
                    Log.i(TAG, String.format("A up scroll"));
                    VL=vmax;
                    VR=vmax;
                }
                else if(abs(last_point.y-first_point.y) <=dragThres_sub && abs(last_point.x-first_point.x) <=dragThres_sub && allPointList.size()>4){
                    //circle scroll
                    Point middle_point = allPointList.get((allPointList.size() - 1)/2);
                    Point quadriple_point_1_4 = allPointList.get((allPointList.size() - 1)/4);
                    Point quadriple_point_3_4 = allPointList.get((allPointList.size() - 1)*3/4);
                    Point center = new Point( (quadriple_point_1_4.x+quadriple_point_3_4.x+middle_point.x+first_point.x)/4,
                            (quadriple_point_1_4.y+quadriple_point_3_4.y+middle_point.y+first_point.y)/4);
                    //if(middle_point.x>first_point.x && middle_point.x>last_point.x)
                    if(angle(quadriple_point_1_4,center) - angle(middle_point , center) > Math.PI || angle(middle_point , center)  > angle(quadriple_point_1_4,center)){
                        //clockwise
                        Log.i(TAG, String.format("An Inverse Clockwise scroll"));
                        //stop
                        VL=-vmax;
                        VR=vmax;
                    }else{
                        Log.i(TAG, String.format("A Clockwise scroll"));
                        //stop
                        VL=0;
                        VR=0;
                    }
                }
                if(socketService!=null) {
                    try {
                        String signL=(VL<=0) ? "1" : "0";
                        String signR=(VR<=0) ? "1" : "0";
                        socketService.sendCommand(signL+String.format("%1$04d", abs(VL))+signR+String.format("%1$04d", abs(VR))+';');
                    }catch (Exception er){er.printStackTrace();}
                }
            }
            allPointList.clear();
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            switch (v.getId()) {
                case R.id.view_logall:
                    Log.i(TAG, String.format("Top Move: %.1f,%.1f", event.getX(), event.getY()));
                    allPointList.add(new Point((int)event.getX(),(int)event.getY()));
                    break;
                default:
                    break;
            }
        }
        //Don't interefere when not necessary
        return false;
    }
    public static double angle(Point a,Point center) {
        return Math.atan2(a.x-center.x, a.y-center.y);
    }
}
