package com.nope.sjtu.extremecontroller;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import android.util.Log;

public class GravitySeneor extends AppCompatActivity {

    private SensorManager sensorMgr;
    Sensor sensor;

    private float x, y, z;
    private float v, r;
    private TextView report;

    private int state = 0;
    private float vmax = 255;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
        setContentView(R.layout.gravity_controller);
        sensorMgr = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensor = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        report=(TextView)findViewById(R.id.textView);

        SensorEventListener lsn = new SensorEventListener() {
            public void onSensorChanged(SensorEvent e) {
                x = e.values[SensorManager.DATA_X];
                y = e.values[SensorManager.DATA_Y];
                z = e.values[SensorManager.DATA_Z];
                v = state * vmax;
                if(y>0) { r = (float) (1.0/(y+0.00001)-0.1); }
                else { r = (float) (1.0/(y+0.00001)+0.1); }
                report.setText(String.format("X;%f, Y:%f, Z:%f, state:%d, V:%f, R:%f",x, y, z,state,v, r));

                //Send v,r here

            }
            public void onAccuracyChanged(Sensor s, int accuracy) {
            }
        };
        Button forward = (Button) this.findViewById(R.id.forward);
        forward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state = 1;
            }
        });
        Button backward = (Button) this.findViewById(R.id.backward);
        backward.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state = -1;
            }
        });
        Button stop = (Button) this.findViewById(R.id.stop);
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                state = 0;
            }
        });

        // 注册listener，第三个参数是检测的精确度
        sensorMgr.registerListener(lsn, sensor, SensorManager.SENSOR_DELAY_GAME);
    }
}