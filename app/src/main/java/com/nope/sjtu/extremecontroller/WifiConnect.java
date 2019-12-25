package com.nope.sjtu.extremecontroller;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.ConnectivityManager;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

public class WifiConnect extends BroadcastReceiver{

    private WifiManager mWifiManager;
    private Context receiverContext;
    public List<ScanResult> scanResults=null;
    private String SSID=null;


    public WifiConnect(Context context){
        receiverContext=context;
        mWifiManager=context==null ? null : (WifiManager)receiverContext.getApplicationContext().getSystemService(context.WIFI_SERVICE);
        scanResults=new LinkedList<ScanResult>();
        IntentFilter filter=new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);//wifi开关变化广播
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);//热点扫描结果通知广播
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);//—热点连接结果通知广播
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);  //—网络状态变化广播（与上一广播协同完成连接过程通知）
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        receiverContext.registerReceiver(this,filter);
        OpenWifi();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if(WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())){
            int wifiState=intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
            Log.d("wifi_state",Integer.toString(wifiState));
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    //Log.d("receiver", "wifi disabled");
                    //OpenWifi();
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    //Log.d("receiver", "wifi enabled");
                    //startScan();
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                    break;
                case WifiManager.WIFI_STATE_ENABLING:
                    break;
                default:
                    break;
            }
        }
        if(WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())){
            Log.d("network_state","change");
            Parcelable parcelableExtra = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (null != parcelableExtra) {
                NetworkInfo networkInfo = (NetworkInfo) parcelableExtra;
                State state = networkInfo.getState();
                boolean isConnected = state == State.CONNECTED;
                if (isConnected) {
                    Log.d("network_state","connect");
                } else {
                    Log.d("network_state","disconnect");
                }
            }
        }
        if(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction())) {
            int linkWifiResult = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 123);
            Log.d("supplicant", Integer.toString(linkWifiResult));
            if (linkWifiResult == WifiManager.ERROR_AUTHENTICATING){
                Log.d("supplicant","wrong password");
            }
        }
    }

    public String Connect(String targetSSID,String password){
        SSID=targetSSID;
        password="\""+password+"\"";
        OpenWifi();
        startScan();
        getScanResult();
        ScanResult targetWifi=findWifiBySSID(targetSSID);
        int netId=findInConfiguration(targetWifi);
        WifiInfo info=mWifiManager.getConnectionInfo();
        if(info!=null){
            Log.d("wifiinfo",info.getSSID());
            if(info.getSSID().equals("\""+targetSSID+"\"")){
                return "This wifi is connecting";
            }
            else{
                mWifiManager.disconnect();
            }
        }

        //没找到对应wifi
        if(targetWifi==null){
            return "There is no wifi named "+targetSSID;
        }


        if(netId!=-1){
            //已保存过相应信息
            mWifiManager.disconnect();
            mWifiManager.enableNetwork(netId,true);
            return "Connected old";
        }
        else{
            //找到对应wifi，但未连接过，构造config
            WifiConfiguration config=createWifiConfig(targetWifi,password);
            mWifiManager.addNetwork(config);
            List<WifiConfiguration> tmp=mWifiManager.getConfiguredNetworks();
            for(WifiConfiguration i:tmp){
                if(!TextUtils.isEmpty(i.SSID)&&TextUtils.equals(i.SSID,"\""+targetWifi.SSID+"\"")){
                    mWifiManager.disconnect();
                    mWifiManager.enableNetwork(i.networkId,true);
                }
            }
            return "unknown false";
        }

    }

    public String Connect(int netId){
        if(netId==-1)
            return "error";
        mWifiManager.disconnect();
        mWifiManager.enableNetwork(netId,true);
        return "Connected";
    }

    public boolean needPassword(ScanResult scanResult){
        String capabilities=scanResult.capabilities;

        if(!TextUtils.isEmpty(capabilities)){
            return (capabilities.contains("WPA")||capabilities.contains("wpa")
                    ||capabilities.contains("WEP")||capabilities.contains("wep"));
        }
        return false;
    }

    private void OpenWifi(){
        if(!mWifiManager.isWifiEnabled()){
            Log.d("wifi", "try to open wifi");
            mWifiManager.setWifiEnabled(true);
        }
        else{
            Log.d("wifi", "wifi has been opened");
        }
    }

    public void startScan(){
        if(!mWifiManager.isWifiEnabled()){
            OpenWifi();
        }
        mWifiManager.startScan();
        Log.d("wifi","start scan");
    }

    public List<ScanResult> getScanResult(){
        scanResults.clear();
        scanResults=mWifiManager.getScanResults();
        Log.d("scanResult get",Integer.toString(scanResults.size()));
        return scanResults;
    }

    //Search wifi by SSID in scanResults
    private ScanResult findWifiBySSID(String targetSSID){
        ScanResult targetWifi=null;
        Log.d("find in scanResult",Integer.toString(scanResults.size()));
        for(ScanResult tempScanResult:scanResults){
            if(tempScanResult.SSID.equals(targetSSID)){
                targetWifi=tempScanResult;

            }
            //Log.d("find in scanResult",tempScanResult.SSID);
        }
        return targetWifi;
    }

    public int findWifiInConfig(ScanResult scanResult){
        return findInConfiguration(scanResult);
    }

    //Judge whether this wifi has been configured before
    //Return netId or -1
    private int findInConfiguration(ScanResult targetWifi){
        int netId=-1;
        List<WifiConfiguration> tempList=mWifiManager.getConfiguredNetworks();

        if(targetWifi==null){
            return -1;
        }

        for(WifiConfiguration c:tempList){
            if(c.SSID.equals("\""+targetWifi.SSID+"\"")){
                netId=c.networkId;
            }
            //Log.d("find in configuration",c.SSID);
        }
        //Log.d("find in configuration",targetWifi.SSID);
        return netId;
    }

    //Get the SSID of connecting wifi
    public String getCurrentSSID(){
        WifiInfo info=mWifiManager.getConnectionInfo();
        if(info==null)
            return "";
        return info.getSSID();
    }

    public int getConnectingNetworkId(){
        WifiInfo info=mWifiManager.getConnectionInfo();
        if(info==null)
            return -1;
        return info.getNetworkId();
    }

    private WifiConfiguration createWifiConfig(ScanResult scanResult,String password){
        WifiConfiguration config=new WifiConfiguration();
        config.SSID=scanResult.SSID;
        Log.d("create config",config.SSID);
        String capabilities=scanResult.capabilities;

        if(!TextUtils.isEmpty(capabilities)){
            if(capabilities.contains("WPA")||capabilities.contains("wpa")){
                config.preSharedKey=password;
            }
            else if(capabilities.contains("WEP")||capabilities.contains("wep")){
                config.wepKeys[0]=password;
                config.wepTxKeyIndex=0;
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            }
            else{
                config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            }
        }
        return config;
    }

    private void deleteConfig(){
        List<WifiConfiguration> tmp=mWifiManager.getConfiguredNetworks();
        for(WifiConfiguration c:tmp){
            if(TextUtils.equals(c.SSID,SSID)){
                boolean res=mWifiManager.removeNetwork(c.networkId);
                Log.d("remove network",Boolean.toString(res));
                return;
            }
        }
        Log.d("remove network","cannot find this SSID");
    }

    public void removeConfig(int netId){
        mWifiManager.removeNetwork(netId);
    }

    public void unregisterReceiver(){
        receiverContext.unregisterReceiver(this);
    }
}
