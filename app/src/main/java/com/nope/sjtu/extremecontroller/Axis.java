package com.nope.sjtu.extremecontroller;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.nope.sjtu.extremecontroller.R;

public class Axis extends android.support.v7.widget.AppCompatImageView {

    private Resources mResources;
    private Bitmap bitmap;
    private int bitmapHeight;
    private int bitmapWidth;
    private Context tContext;
    public float InitX=500,InitY=540;
    public float CurrentX,CurrentY;
    private boolean isClickView = false;
    private Rect dst = new Rect();
    public Axis(Context context) {
        super(context);
        tContext=context;
        mResources = tContext.getResources();
        bitmap =  ((BitmapDrawable)mResources.getDrawable(R.drawable.axis)).getBitmap();
        bitmapHeight = bitmap.getHeight();
        bitmapWidth= bitmap.getWidth();

    }
    public Axis(Context context, AttributeSet attrs) {
        super(context,attrs);
        tContext=context;
        mResources = tContext.getResources();
        bitmap =  ((BitmapDrawable)mResources.getDrawable(R.drawable.axis)).getBitmap();
        bitmapHeight = bitmap.getHeight();
        bitmapWidth= bitmap.getWidth();
    }
    @Override

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float x=CurrentX-InitX;
        float y=CurrentY-InitY;
        isClickView = true;
        float r = (float) Math.sqrt(x*x+y*y);
        float a = (float) Math.atan(Math.abs(y*1.0/x));
        final double R = 350;
        if (r>R) {//k*R is the max valiad radius
            CurrentX =(float)(R * Math.cos(a) * Math.signum(x) + InitX);
            CurrentY =(float)(R * Math.sin(a) * Math.signum(y) + InitY);
        }
        this.invalidate();
        if (isClickView == true && bitmap != null) {
            //创建画笔
            Paint p = new Paint();

            dst.left = (int) CurrentX-350/2;
            dst.top = (int) CurrentY-350/2;
            dst.right = (int) CurrentX-350/2 + 350;
            dst.bottom = (int) CurrentY-350/2 + 350;
            canvas.drawBitmap(bitmap, null, dst, p);
            //canvas.drawBitmap(bitmap,CurrentX-bitmapWidth/2, CurrentY-bitmapHeight/2, p);
            isClickView = false;
        }else if(isClickView == false && bitmap != null){
            Paint p = new Paint();
            dst.left = (int) InitX-350/2;
            dst.top = (int) InitY-350/2;
            dst.right = (int) InitX-350/2 + 350;
            dst.bottom = (int) InitY-350/2 + 350;
            canvas.drawBitmap(bitmap, null, dst, p);
            //canvas.drawBitmap(bitmap,InitX-bitmapWidth/2, InitY-bitmapHeight/2, p);
        }

    }

    public void SetAxis(int x,int y){
        CurrentX=x;
        CurrentY=y;
    }

}
