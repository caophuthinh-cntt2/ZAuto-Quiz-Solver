# ZAuto - AI-Powered Quiz Solver 🚀

ZAuto is an automated, AI-driven tool designed to instantly solve multiple-choice questions displayed on an Android screen. By leveraging screen capture and powerful Large Language Models (LLMs) via the XKIRO API, ZAuto reads the question, identifies the context, and returns the correct answer within seconds.

This repository contains two main components:
1. **ZAuto Android App:** A native Android application that provides a convenient floating button overlay. It allows you to capture the screen and get answers directly on your phone without leaving your quiz app.
2. **Python Desktop Script (`quiz_solver.py`):** A lightweight script that connects to your Android device via ADB (USB or Wi-Fi). It acts as a remote trigger to capture the screen and solve questions directly from your PC.

## ✨ Features

- **Floating Overlay Button:** One-tap solution right on your mobile screen.
- **Fast & Accurate:** Powered by `qwen/qwen3.8-omni-flash:free` model for rapid processing.
- **Auto-Formatting:** Smartly extracts and formats the answer (e.g., `Câu 7: C - [keyword]`) even if standard A/B/C/D choices are missing.
- **Persistent API Key:** Automatically saves your XKIRO API key in the Android app for seamless future sessions.
- **Privacy-Focused:** Screen captures are sent directly to the API in memory and are never saved permanently to your device storage.

## 🛠️ Installation & Usage

### 1. Android App
- Navigate to the `ZAuto-source` directory.
- Build the APK using the provided `build.ps1` script (requires Android SDK & JDK).
- Install the generated `ZAuto.apk` on your device.
- Open the app, enter your XKIRO API Key, and tap **Bật nút nổi** (Enable Floating Button).
- Grant "Draw over other apps" and "Screen recording" permissions.
- Tap the floating button whenever a question appears on your screen!

### 2. Python Script (PC)
- Ensure you have Python installed.
- Install the required dependencies (in `.venv`).
- Connect your Android device to your PC via ADB.
- Run the script: `python quiz_solver.py`
- Press `Enter` on your keyboard whenever you want to capture the phone's screen and solve the question.

## ⚠️ Disclaimer
This tool is created for educational and experimental purposes. Please use it responsibly and adhere to the academic integrity policies of your institution.
