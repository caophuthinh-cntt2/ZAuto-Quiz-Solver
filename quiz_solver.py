import subprocess
import time
import os
import base64
from openai import OpenAI

# ==========================================
# CẤU HÌNH API XKIRO
# ==========================================
API_KEY = "sk-xt-a636c5d4e215e9ae969ba4c733f96a7fc7437340324c6344"
BASE_URL = "https://api.xkiro.com/v1"
MODEL_NAME = "qwen/qwen3.8-omni-flash:free"

# Khởi tạo client OpenAI
try:
    client = OpenAI(api_key=API_KEY, base_url=BASE_URL)
except Exception as e:
    print(f"Lỗi khởi tạo API: {e}")
    exit(1)

def encode_image(image_path):
    """Mã hóa ảnh sang dạng base64 để gửi qua API."""
    with open(image_path, "rb") as image_file:
        return base64.b64encode(image_file.read()).decode('utf-8')

def get_adb_device():
    """Lấy device ID đầu tiên đang kết nối."""
    try:
        result = subprocess.run(["adb", "devices"], capture_output=True, text=True, check=True)
        lines = result.stdout.strip().split('\n')[1:] 
        devices = [line.split('\t')[0] for line in lines if '\tdevice' in line]
        if not devices:
            return None
        return devices[0]
    except Exception as e:
        print(f"Lỗi khi kiểm tra thiết bị ADB: {e}")
        return None

def capture_screen(device_id, filename="screen.png"):
    """Sử dụng ADB để chụp màn hình và lưu về máy tính."""
    print(f"📸 Đang chụp màn hình (Từ thiết bị: {device_id})...")
    try:
        with open(filename, "wb") as f:
            subprocess.run(["adb", "-s", device_id, "exec-out", "screencap", "-p"], stdout=f, check=True)
        return True
    except Exception as e:
        print(f"❌ Lỗi khi chụp màn hình: {e}")
        return False

def solve_quiz(image_path):
    """Gửi ảnh qua OpenAI API tương thích."""
    print(f"🧠 Đang gửi cho model {MODEL_NAME} phân tích...")
    try:
        base64_image = encode_image(image_path)
        
        prompt = "Đây là một câu hỏi trắc nghiệm trên màn hình. Hãy đọc kỹ câu hỏi và các đáp án. Chỉ cần trả lời NGẮN GỌN đáp án đúng nhất (ví dụ: A, B, C, D hoặc nội dung đáp án). Không cần giải thích dài dòng để tiết kiệm thời gian."
        
        response = client.chat.completions.create(
            model=MODEL_NAME,
            messages=[
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {
                            "type": "image_url",
                            "image_url": {
                                "url": f"data:image/png;base64,{base64_image}"
                            }
                        }
                    ]
                }
            ],
            max_tokens=50,
            temperature=0.2
        )
        return response.choices[0].message.content.strip()
    except Exception as e:
        print(f"❌ Lỗi khi gọi API: {e}")
        return None

def main():
    print("🚀 Bắt đầu công cụ giải trắc nghiệm tự động qua WiFi (Dùng API xkiro)!")
    print("-" * 40)
    
    device_id = get_adb_device()
    if not device_id:
        print("❌ Không tìm thấy thiết bị ADB nào!")
        return
        
    print(f"✅ Đã tìm thấy thiết bị: {device_id}")
    
    while True:
        try:
            input("Nhấn [ENTER] để chụp và giải (hoặc Ctrl+C để thoát)...")
        except KeyboardInterrupt:
            print("\n👋 Đã thoát công cụ.")
            break
            
        start_time = time.time()
        
        if not capture_screen(device_id):
            continue
            
        answer = solve_quiz("screen.png")
        
        end_time = time.time()
        
        if answer:
            print("-" * 40)
            print(f"✅ ĐÁP ÁN: {answer}")
            print(f"⏱️ Thời gian xử lý: {end_time - start_time:.2f} giây")
            print("-" * 40)
        
        if os.path.exists("screen.png"):
            os.remove("screen.png")

if __name__ == "__main__":
    main()
