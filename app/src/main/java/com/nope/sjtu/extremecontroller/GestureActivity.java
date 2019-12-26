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
                double rmax=255,vmax=255,radius=-1,velocity=-1;
                if(last_point.x-first_point.x>=dragThres_main && Math.abs(last_point.y-first_point.y) <=dragThres_sub){
                    //right scroll
                    Log.i(TAG, String.format("A right scroll"));
                    radius=-0.01;
                    velocity=0.2*vmax;
                }
                else if(first_point.x-last_point.x>=dragThres_main && Math.abs(last_point.y-first_point.y) <=dragThres_sub){
                    //left scroll
                    Log.i(TAG, String.format("A left scroll"));
                    radius=0.01;
                    velocity=0.2*vmax;
                }
                else if(last_point.y-first_point.y>=dragThres_main && Math.abs(last_point.x-first_point.x) <=dragThres_sub){
                    //down scroll
                    Log.i(TAG, String.format("A down scroll"));
                    radius=rmax;
                    velocity=-vmax;
                }
                else if(first_point.y-last_point.y>=dragThres_main && Math.abs(last_point.x-first_point.x) <=dragThres_sub){
                    //up scroll
                    Log.i(TAG, String.format("A up scroll"));
                    radius=rmax;
                    velocity=vmax;
                }
                else if(Math.abs(last_point.y-first_point.y) <=dragThres_sub && Math.abs(last_point.x-first_point.x) <=dragThres_sub && allPointList.size()>4){
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
                        radius=rmax;
                        velocity=0;
                    }else{
                        Log.i(TAG, String.format("A Clockwise scroll"));
                        //stop
                        radius=rmax;
                        velocity=0;
                    }
                }
                if(socketService!=null) {
                    try {
                        socketService.sendCommand(ConvertCommand((float)velocity,(float)radius));
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

    public String ConvertCommand(float speed,float radius){
        //VL=Vc(1-L/2r)
        //VR=Vc(1+L/2r)
        radius=radius/100;
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
}
