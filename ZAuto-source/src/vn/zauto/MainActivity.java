package vn.zauto;

import android.app.*;
import android.os.*;
import android.content.*;
import android.provider.Settings;
import android.net.Uri;
import android.media.projection.MediaProjectionManager;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {
    EditText key;
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1); box.setPadding(36,60,36,24);
        TextView title = new TextView(this); title.setText("🔵 ZAuto"); title.setTextSize(30); box.addView(title);
        TextView info = new TextView(this);
        info.setText("Bấm nút nổi để chụp câu hỏi và gửi đến API XKIRO. Đáp án hiện ngay trên nút. Kéo để di chuyển, giữ lâu để tắt.\n\nẢnh màn hình chỉ gửi khi bạn bấm nút. Chọn chia sẻ toàn bộ màn hình để dùng trên các ứng dụng.\n");
        box.addView(info);
        key = new EditText(this); key.setHint("API key XKIRO"); key.setSingleLine(true);
        key.setText(getPreferences(0).getString("key","")); key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); box.addView(key);

        Button start = new Button(this); start.setText("Bật nút nổi"); box.addView(start);
        start.setOnClickListener(v -> {
            if(key.getText().toString().trim().isEmpty()) { toast("Nhập API key"); return; }
            if(!Settings.canDrawOverlays(this)) {
                toast("Cho phép ZAuto hiển thị trên ứng dụng khác, rồi quay lại bấm Bật nút nổi");
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))); return;
            }
            stopService(new Intent(this,CaptureService.class));
            getPreferences(0).edit().putString("key",key.getText().toString().trim()).apply();
            startActivityForResult(((MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE)).createScreenCaptureIntent(),7);
        });
        Button stop = new Button(this); stop.setText("Tắt nút nổi"); box.addView(stop);
        stop.setOnClickListener(v -> stopService(new Intent(this,CaptureService.class)));
        TextView note = new TextView(this); note.setText("API key được lưu tự động. Không lưu ảnh chụp.");
        box.addView(note); setContentView(box);
    }
    protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==7 && result==RESULT_OK && data!=null) {
            startForegroundService(new Intent(this,CaptureService.class).putExtra("consent",data).putExtra("key",key.getText().toString().trim()).putExtra("model","qwen/qwen3.8-omni-flash:free"));
            moveTaskToBack(true);
        }
    }
    void toast(String s) { Toast.makeText(this,s,Toast.LENGTH_LONG).show(); }
}
