package com.nope.sjtu.extremecontroller;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class WifiList extends AppCompatActivity {
    private Button button_scan, button_enablewifi;
    private WifiManager mWifiManager;
    private WifiAdapter mAdapter = null;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION=0;
    private static final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION=1;
    private ListView list_of_wifi;
    private List<ScanResult> scanResults=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_list);
        mWifiManager=(WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        button_scan = findViewById(R.id.button_scan);
        button_enablewifi=findViewById(R.id.button_enablewifi);
        list_of_wifi=findViewById(R.id.wifi_list);
        scanResults=new LinkedList<ScanResult>();
        mAdapter=new WifiAdapter(scanResults,this);
        list_of_wifi.setAdapter(mAdapter);

        //动态申请权限
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                //TODO
            }
            else{
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            }
            //之后系统会回调onRequestPermissionsResult()，下面有实现
        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION)){
                //TODO
            }
            else{
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            }
            //之后系统会回调onRequestPermissionsResult()，下面有实现
        }
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED){
            button_enablewifi.setText("-5");
        }

        button_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //scan按钮的监听函数，打开WiFi开关并扫描热点
                if (!mWifiManager.isWifiEnabled()) {
                    //若wifi未开启，开启wifi
                    Log.d("wifi", "try to open wifi");
                    mWifiManager.setWifiEnabled(true);
                }
                if(!mWifiManager.isWifiEnabled()){
                    Log.d("wifi", "onClick: NO WIFI SUPPORT");
                }
                else {
                    mWifiManager.startScan();
                    button_enablewifi.setText("begin");
                    mAdapter.clear();
                    scanResults.clear();
                    scanResults = mWifiManager.getScanResults();
                    mAdapter.set(scanResults);
                    list_of_wifi.setAdapter(mAdapter);
                    String tmp=Integer.toString(scanResults.size());
                    button_enablewifi.setText(tmp);
                }
            }
        });

        list_of_wifi.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                mWifiManager.disconnect();
                final ScanResult scanResult=mAdapter.getItem(position);
                final String capabilities=scanResult.capabilities;
                //已连接，询问是否断开连接
                if(mWifiManager.getConnectionInfo().getSSID().equals(scanResult.SSID)){
                    final int net=mWifiManager.getConnectionInfo().getNetworkId();
                    new AlertDialog.Builder(WifiList.this)
                            .setTitle("是否断开连接")
                            .setPositiveButton("断开", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    mWifiManager.disableNetwork(net);
                                }
                            })
                            .setNegativeButton("不保存", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    mWifiManager.removeNetwork(net);
                                }
                            })
                            .create().show();
                    return;
                }

                //尚未连接
                //查询是否连接过
                int netid=-1;
                List<WifiConfiguration> tempList=mWifiManager.getConfiguredNetworks();
                for(WifiConfiguration c:tempList){
                    if(c.SSID.equals(scanResult.SSID)){
                        netid=c.networkId;
                    }
                }

                //若未连接过
                if(netid==-1){
                    int result=-1;
                    final WifiConfiguration nConfig=new WifiConfiguration();
                    nConfig.SSID=scanResult.SSID;
                    nConfig.hiddenSSID=false;
                    nConfig.status=WifiConfiguration.Status.ENABLED;
                    if(capabilities.contains("WPA")||capabilities.contains("wpa")||
                            capabilities.contains("WEP")||capabilities.contains("wep")){
                        //需要密码，弹出弹窗，用户输入
                        final EditText pwdEt=new EditText(WifiList.this);
                        new AlertDialog.Builder(WifiList.this)
                                .setView(pwdEt).setTitle("请输入密码")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        nConfig.wepKeys[0]=pwdEt.getText().toString();
                                        if(!(capabilities.contains("PSK")||capabilities.contains("psk"))) {
                                            nConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                                            nConfig.wepTxKeyIndex = 0;
                                        }
                                    }
                                })
                                .setNegativeButton("取消",null)
                                .create().show();
                    }
                    mWifiManager.addNetwork(nConfig);
                }
                List<WifiConfiguration> temp=mWifiManager.getConfiguredNetworks();
                for(WifiConfiguration c:temp){
                    if(c.SSID.equals(scanResult.SSID)){
                        netid=c.networkId;
                    }
                }
                mWifiManager.enableNetwork(netid,true);
                button_enablewifi.setText("-9");
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults){
        switch (requestCode){
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION:{
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    Log.d("location","permission granted");
                    button_enablewifi.setText("-1");
                }
                else{
                    Log.d("location","permission denied");
                    button_enablewifi.setText("-2");
                }
            }
            case MY_PERMISSIONS_REQUEST_COARSE_LOCATION:{
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    //permission granted
                    Log.d("location","permission granted");
                    button_enablewifi.setText("-3");
                }
                else{
                    Log.d("location","permission denied");
                    button_enablewifi.setText("-4");
                }
            }
        }
    }

}

