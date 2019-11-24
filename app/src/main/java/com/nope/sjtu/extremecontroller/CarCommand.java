package com.nope.sjtu.extremecontroller;

/**
 * Created by stelledge on 2019/11/24.
 */

public class CarCommand {
    // format: 0 0000 0 0000 ;  L-R-;
    //5 bits: 0 for forward 1 for backward XXXX for speed
    private final float car_width=11.2f;
    private final float voltage_correction=0.8f;
    public String ConvertCommand(float speed,float radius){
        //VL=Vc(1-L/2r)
        //VR=Vc(1+L/2r)
        float Vc=speed*voltage_correction;
        float VL_f=Vc*(1-car_width/(2*radius));
        float VR_f=Vc*(1+car_width/(2*radius));
        int VL=(int)(VL_f);
        int VR=(int)(VR_f);
        char sign;
        if (Vc>0) sign='0';
        else sign='1';
        String out=""+sign+VL+sign+VR+';';
        return out;
    }
}
