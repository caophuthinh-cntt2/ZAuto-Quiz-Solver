package vn.zauto;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.concurrent.*;

public class CaptureService extends Service {
    final Handler ui=new Handler(Looper.getMainLooper());
    final ExecutorService worker=Executors.newSingleThreadExecutor();
    MediaProjection projection; VirtualDisplay display; ImageReader reader;
    WindowManager wm; WindowManager.LayoutParams params; TextView bubble;
    String key,model; boolean busy,waiting,destroyed; long readyAt;
    final Runnable timeout=()->{if(waiting) finish("⚠ Không chụp được. Bấm thử lại");};
    public IBinder onBind(Intent i){return null;}
    public int onStartCommand(Intent intent,int flags,int id) {
        if(intent==null){stopSelf(); return START_NOT_STICKY;}
        if(projection!=null) return START_NOT_STICKY;
        try {
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("capture","Nút nổi ZAuto",NotificationManager.IMPORTANCE_LOW));
            PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
            Notification notification=new Notification.Builder(this,"capture").setSmallIcon(android.R.drawable.ic_menu_camera)
                .setContentTitle("ZAuto đang bật").setContentText("Bấm nút nổi để giải · Giữ nút để tắt").setContentIntent(open).setOngoing(true).build();
            if(Build.VERSION.SDK_INT>=29) startForeground(1,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            else startForeground(1,notification);
            key=intent.getStringExtra("key"); model=intent.getStringExtra("model");
            projection=getSystemService(MediaProjectionManager.class).getMediaProjection(Activity.RESULT_OK,intent.getParcelableExtra("consent"));
            projection.registerCallback(new MediaProjection.Callback(){
                public void onStop(){stopSelf();}
                public void onCapturedContentResize(int width,int height){resize(width,height);}
            },ui);
            wm=getSystemService(WindowManager.class);
            DisplayMetrics metrics=new DisplayMetrics(); wm.getDefaultDisplay().getRealMetrics(metrics);
            reader=newReader(metrics.widthPixels,metrics.heightPixels);
            display=projection.createVirtualDisplay("ZAuto",metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,ui);
            bubble=new TextView(this); bubble.setText("🔵"); bubble.setTextColor(Color.WHITE); bubble.setTextSize(17);
            bubble.setGravity(Gravity.CENTER); bubble.setPadding(24,16,24,16); bubble.setMaxWidth((int)(metrics.widthPixels*0.85));
            bubble.setContentDescription("Chụp và giải câu hỏi. Giữ để tắt.");
            GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.rgb(21,101,220)); bg.setCornerRadius(60); bubble.setBackground(bg);
            params=new WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.LEFT; params.x=24; params.y=200; wm.addView(bubble,params);
            bubble.setOnClickListener(v->capture()); bubble.setOnLongClickListener(v->{stopSelf();return true;});
            bubble.setOnTouchListener(new View.OnTouchListener(){
                float x,y; int px,py; boolean moved;
                public boolean onTouch(View v, android.view.MotionEvent e){
                    if(e.getAction()==MotionEvent.ACTION_DOWN){x=e.getRawX();y=e.getRawY();px=params.x;py=params.y;moved=false;}
                    if(e.getAction()==MotionEvent.ACTION_MOVE){
                        if(Math.abs(e.getRawX()-x)+Math.abs(e.getRawY()-y)>16) moved=true;
                        if(moved){v.cancelLongPress();params.x=Math.max(0,Math.min(getResources().getDisplayMetrics().widthPixels-v.getWidth(),px+(int)(e.getRawX()-x)));
                            params.y=Math.max(0,Math.min(getResources().getDisplayMetrics().heightPixels-v.getHeight(),py+(int)(e.getRawY()-y)));wm.updateViewLayout(v,params); return true;}
                    }
                    if(e.getAction()==MotionEvent.ACTION_UP && moved){v.setPressed(false);return true;}return false;
                }
            });
        }catch(Exception e){Toast.makeText(this,"Không thể bắt đầu chụp. Mở app và cấp quyền lại.",Toast.LENGTH_LONG).show();stopSelf();}
        return START_NOT_STICKY;
    }
    ImageReader newReader(int width,int height){
        ImageReader r=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
        r.setOnImageAvailableListener(source->{
            try(Image frame=source.acquireLatestImage()){
                if(frame==null || !waiting || SystemClock.uptimeMillis()<readyAt) return;
                waiting=false; ui.removeCallbacks(timeout);
                Image.Plane plane=frame.getPlanes()[0];
                int padded=plane.getRowStride()/plane.getPixelStride();
                Bitmap raw=Bitmap.createBitmap(padded,frame.getHeight(),Bitmap.Config.ARGB_8888);
                raw.copyPixelsFromBuffer(plane.getBuffer());
                Bitmap crop=Bitmap.createBitmap(raw,0,0,frame.getWidth(),frame.getHeight());
                if(crop!=raw)raw.recycle();
                bubble.setAlpha(1);
                worker.submit(()->solve(crop));
            }catch(Exception e){if(busy)finish("⚠ Lỗi chụp. Bấm thử lại");}
        },ui);return r;
    }
    void resize(int width,int height){
        if(display==null || destroyed || width<1 || height<1 || (reader.getWidth()==width && reader.getHeight()==height))return;
        ImageReader old=reader; reader=newReader(width,height);
        display.resize(width,height,getResources().getDisplayMetrics().densityDpi);display.setSurface(reader.getSurface());old.close();
    }
    void capture(){
        if(busy)return;busy=true;bubble.setText("⏳ Đang giải...");
        // Hide the overlay until a fresh frame arrives so it does not cover the question.
        bubble.setAlpha(0);waiting=true;readyAt=SystemClock.uptimeMillis()+350;ui.postDelayed(timeout,5000);
    }
    void solve(Bitmap bitmap){
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.JPEG,90,bytes);
            String answer=Solver.solve(key,model,Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP));
            ui.post(()->finish("✅ ĐÁP ÁN: "+answer));
        }catch(Exception e){
            String message=e instanceof java.net.SocketTimeoutException?"API hết thời gian chờ":
                e instanceof IOException?e.getMessage():"Phản hồi API không hợp lệ";
            ui.post(()->finish("⚠ "+message+" · Bấm thử lại"));
        }finally{bitmap.recycle();}
    }
    void finish(String text){if(destroyed)return;waiting=false;busy=false;ui.removeCallbacks(timeout);bubble.setAlpha(1);bubble.setText(text);}
    public void onDestroy(){
        destroyed=true;ui.removeCallbacksAndMessages(null);
        if(bubble!=null && bubble.isAttachedToWindow())wm.removeView(bubble);
        if(display!=null)display.release();if(reader!=null)reader.close();if(projection!=null)projection.stop();
        worker.shutdownNow();key=null;super.onDestroy();
    }
}
