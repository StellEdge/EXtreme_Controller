package com.nope.sjtu.extremecontroller;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Iterator;
import java.util.List;

public class WifiAdapter extends BaseAdapter {
    private List<ScanResult> mData;
    private Context mContext;

    public WifiAdapter(){
        this.mData=null;
        this.mContext=null;
    }

    public WifiAdapter(List<ScanResult> mData,Context mContext){
        this.mData=mData;
        this.mContext=mContext;
    }

    @Override
    public int getCount(){
        return mData.size();
    }

    public void clear(){
        if(mData!=null){
            mData.clear();
        }
        notifyDataSetChanged();
    }

    @Override
    public ScanResult getItem(int position) {
        if(mData==null)
            return null;
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void set(List<ScanResult> nData){
        if(mData!=null){
            mData.clear();
        }
        mData=nData;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){

        ViewHolder holder = null;
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_list,parent,false);
            holder = new ViewHolder();
            holder.txt_content = (TextView) convertView.findViewById(R.id.txt_content);
            convertView.setTag(holder);
        }else{
            holder = (ViewHolder) convertView.getTag();
        }
        ScanResult tmp=mData.get(position);
        String res=tmp.SSID+"\n"+"     "+tmp.level;
        holder.txt_content.setText(res);
        return convertView;
    }

    private class ViewHolder{
        TextView txt_content;
    }

}
