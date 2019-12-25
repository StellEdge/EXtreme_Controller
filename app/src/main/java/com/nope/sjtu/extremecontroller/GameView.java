package com.nope.sjtu.extremecontroller;

/**
 * Created by 10301 on 2019/12/25.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class GameView extends View{

    private Paint paint = null; //

    private Bitmap originalBitmap = null;//原始图

    private Bitmap new1Bitmap = null;

    private Bitmap new2Bitmap = null;

    private float clickX =0;

    private float clickY=0;

    private float startX=0;

    private float startY=0;

    private boolean isMove = true;

    private boolean isClear = false;

    private int color =Color.RED;

    private float strokeWidth =20f;

    private Path mPath;

    private Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();
        int newWidth = w;
        int newHeight = h;

        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // if you want to rotate the Bitmap
        // matrix.postRotate(45);
        Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                height, matrix, true);
        return resizedBitmap;
    }


    public GameView(Context context) {
        this(context,null);
        // TODO Auto-generated constructor stub
    }
    public GameView(Context context,AttributeSet atts) {
        this(context,atts,0);
        // TODO Auto-generated constructor stub
    }
    @SuppressWarnings("static-access")
    public GameView(Context context,AttributeSet atts,int defStyle) {
        super(context,atts,defStyle);
        // TODO Auto-generated constructor stub


    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.default_pic).copy(Bitmap.Config.ARGB_8888, true);//白色的画板
        originalBitmap=resizeImage(originalBitmap,  getWidth(), getHeight());
        new1Bitmap=originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        new2Bitmap=originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        mPath=new Path();
    }



    //清楚
    @SuppressWarnings("static-access")
    public void clear(){
        isClear =true;
        new2Bitmap=originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        invalidate();//重置
    }

    public void background_clear(){
        isClear =true;
        new1Bitmap=originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        new2Bitmap=originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        invalidate();//重置
    }
    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        super.onDraw(canvas);
        canvas.drawBitmap(writer(new1Bitmap),0,0, null);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // TODO Auto-generated method stub
        clickX =event.getX();

        clickY=event.getY();

        if(event.getAction()==MotionEvent.ACTION_DOWN){
            //手指点下屏幕时触发
            isClear =false;
            startX=clickX;
            startY=clickY;
            //mPath= new Path();
            mPath.reset();
            //mPath.rewind();
            mPath.moveTo(clickX, clickY);
        }
        else if(event.getAction()==MotionEvent.ACTION_MOVE){
            //手指移动时触发
            float dx=Math.abs(clickX-startX);
            float dy=Math.abs(clickY-startY);
            //   if(dx>=3||dy>=3){
            //设置贝塞尔曲线的操作点为起点和终点的一半
            float cX = (clickX + startX) / 2;
            float cY = (clickY + startY) / 2;
            mPath.quadTo(startX,startY, cX, cY);

            startX=clickX;
            startY=clickY;
        }
        else if(event.getAction()==MotionEvent.ACTION_UP){
            background_clear();
        }


        invalidate();
        return true;
    }



    public Bitmap writer(Bitmap pic){
        initPaint();

        Canvas canvas =null;
        if(isClear){
            canvas=new Canvas(new2Bitmap);
        }else{
            canvas=new Canvas(pic);
        }

        canvas.drawPath(mPath, paint);
        if(isClear){
            return new2Bitmap;
        }
        return pic;
    }

    private void initPaint(){

        paint = new Paint();//初始化画笔

        paint.setStyle(Style.STROKE);//设置为画线

        paint.setAntiAlias(true);//设置画笔抗锯齿

        paint.setColor(color);//设置画笔颜色

        paint.setStrokeWidth(strokeWidth);//设置画笔宽度
    }


    public void setColor(int color){

        this.color=color;
        initPaint();
    }
}