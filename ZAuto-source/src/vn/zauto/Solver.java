package vn.zauto;

import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class Solver {
    public static String solve(String key,String model,String image) throws Exception {
        JSONObject request = new JSONObject().put("model",model).put("max_tokens",100).put("temperature",0.2)
          .put("messages",new JSONArray().put(new JSONObject().put("role","user").put("content",new JSONArray()
          .put(new JSONObject().put("type","text").put("text","Đọc câu hỏi trong ảnh. Trả lời đúng định dạng: Câu [số]: [A/B/C/D] - [Từ khóa ngắn]. Nếu đáp án không có A, B, C, D thì trả lời: Câu [số]: [Từ khóa ngắn]. Mỗi câu trả lời trên một dòng mới. Tuyệt đối không giải thích dài dòng."))
          .put(new JSONObject().put("type","image_url").put("image_url",new JSONObject().put("url","data:image/jpeg;base64,"+image))))));
        HttpURLConnection c=(HttpURLConnection)new URL("https://api.xkiro.com/v1/chat/completions").openConnection();
        try {
            c.setConnectTimeout(20000); c.setReadTimeout(60000); c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setRequestProperty("Authorization","Bearer "+key); c.setRequestProperty("Content-Type","application/json");
            try(OutputStream out=c.getOutputStream()){out.write(request.toString().getBytes(StandardCharsets.UTF_8));}
            int status=c.getResponseCode();
            if(status!=200) throw new IOException(status==401?"API key không hợp lệ":status==429?"API hết lượt / quá tải":"Lỗi API HTTP "+status);
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(InputStream in=c.getInputStream()) {
                byte[] buf=new byte[4096]; int n;
                while((n=in.read(buf))!=-1) { bytes.write(buf,0,n); if(bytes.size()>1048576) throw new IOException("Phản hồi quá lớn"); }
            }
            String answer=new JSONObject(bytes.toString("UTF-8")).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content").trim();
            if(answer.isEmpty()) throw new IOException("API không trả đáp án");
            return answer;
        } finally {c.disconnect();}
    }
}
