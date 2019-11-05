package com.nope.sjtu.extremecontroller;

import android.util.Log;

public class Location {

    private float x, y;
    private boolean condition;

    public Location() {
    }

    public void update(float x, float y){
        this.x = x;
        this.y = y;
        Log.d("Touch", "update: "+x);
    }

    public void update_condition(boolean condition){
        this.condition = condition;
    }

    public boolean conditon(){
        return this.condition;
    }
    public float x(){
        return this.x;
    }

    public float y(){
        return this.y;
    }

}
