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
    private WifiAdapter mAdapter = null;
    private ListView list_of_wifi;
    private List<ScanResult> scanResults=null;
    private WifiConnect wifiConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.wifi_list);
        button_scan = findViewById(R.id.button_scan);
        button_enablewifi=findViewById(R.id.button_enablewifi);
        list_of_wifi=findViewById(R.id.wifi_list);
        scanResults=new LinkedList<ScanResult>();
        mAdapter=new WifiAdapter(scanResults,this);
        list_of_wifi.setAdapter(mAdapter);
        wifiConnect=new WifiConnect(WifiList.this);

        button_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                wifiConnect.startScan();
                scanResults.clear();
                mAdapter.clear();
                scanResults=wifiConnect.getScanResult();
                mAdapter.set(scanResults);
                list_of_wifi.setAdapter(mAdapter);
            }
        });

        list_of_wifi.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                final ScanResult scanResult=mAdapter.getItem(position);
                int netId=wifiConnect.findWifiInConfig(scanResult);

                //已连接该WiFi
                if(wifiConnect.getCurrentSSID().equals(scanResult.SSID)){
                    final int net=wifiConnect.getConnectingNetworkId();
                    new AlertDialog.Builder(WifiList.this)
                            .setTitle("是否断开连接")
                            .setPositiveButton("取消", null)
                            .setNegativeButton("不保存", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    wifiConnect.removeConfig(net);
                                }
                            })
                            .create().show();
                    return;
                }


                if(netId==-1){
                    //未连接过
                    if(wifiConnect.needPassword(scanResult)) {
                        final EditText pwdEt = new EditText(WifiList.this);
                        new AlertDialog.Builder(WifiList.this)
                                .setView(pwdEt).setTitle("请输入密码")
                                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        String password="12345678";
                                        password = pwdEt.getText().toString();
                                        wifiConnect.Connect(scanResult.SSID,password);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .create().show();
                    }
                    else{
                        wifiConnect.Connect(scanResult.SSID,"12345678");
                    }
                }
                else{
                    //已连接过
                    wifiConnect.Connect(netId);
                }
            }
        });
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        wifiConnect.unregisterReceiver();
        finish();
    }
}

