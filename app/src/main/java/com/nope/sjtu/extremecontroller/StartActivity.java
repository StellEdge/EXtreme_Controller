package com.nope.sjtu.extremecontroller;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

//import pub.devrel.easypermissions.EasyPermissions;
//import pub.devrel.easypermissions.AfterPermissionGranted;
//import pub.devrel.easypermissions.AppSettingsDialog;


public class StartActivity extends AppCompatActivity{// implements EasyPermissions.PermissionCallbacks{
    TextView textShowIP;
    Button button_wifi;
    Button testButton;
    private final int REQUEST_CODE=998;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        textShowIP=findViewById(R.id.textShowIP);

        button_wifi=findViewById(R.id.button_wifi);
        button_wifi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(StartActivity.this,WifiList.class);
                startActivity(intent);
            }
        });
        showIP();

        final Intent intent=new Intent(this,SocketService.class);
        startService(intent);

        //requestPermission();
        testButton=findViewById(R.id.button_control_1);
        testButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent(StartActivity.this,MainActivity.class);
                startActivity(intent);
            }
        });
    }
    private void showIP(){
        try {
            WifiManager wifiManager=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if(wifiManager.getConnectionInfo()==null){
                textShowIP.setText("未连接WiFi");
            }
            final int ip_int = wifiManager.getConnectionInfo().getIpAddress();
            final String ip = ipTrans(ip_int);
            String show = ip + "\n port:12345";
            textShowIP.setText(show);
        }catch (Exception e){e.printStackTrace();}
    }

    private String ipTrans(int a){
        String res="";
        res+=a&0xFF;
        res+=".";
        res+=(a>>8)&0xFF;
        res+=".";
        res+=(a>>16)&0xFF;
        res+=".";
        res+=(a>>24)&0xFF;
        return res;
    }

    /*Copyright 2017 Google

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

    //check location permission
    //if not allowed,request them
//    @AfterPermissionGranted(REQUEST_CODE)
//    private void requestPermission(){
//        String[] perms={Manifest.permission.ACCESS_COARSE_LOCATION,
//                Manifest.permission.ACCESS_FINE_LOCATION};
//        if(EasyPermissions.hasPermissions(this,perms)){
//            Log.d("permissions","has granted");
//            return;
//        }
//        else{
//            EasyPermissions.requestPermissions(this,"需要获取位置信息权限",REQUEST_CODE,perms);
//            Log.d("permissions","request");
//            return;
//        }
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        // Forward results to EasyPermissions
//        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
//    }
//
//    @Override
//    public void onPermissionsGranted(int requestCode, List<String> perms) {
//        Log.d("permission","request successfully");
//    }
//
//    @Override
//    public void onPermissionsDenied(int requestCode, List<String> perms) {
//        Log.d("permission","request denied");
//        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
//        // This will display a dialog directing them to enable the permission in app settings.
//        new AppSettingsDialog.Builder(this).build().show();
//    }
}
