import os
import time
import subprocess
import json
import re
from google import genai
from google.genai import types
from PIL import Image
import io

# API Keys provided by user
API_KEYS = [
    "AIzaSyCFOVSrC_9czfSy9-itaYGv3VVu5JrFaEQ",
    "AIzaSyDa3ycpKRYfm2O-LCOJYm41kyfdCucU3AU",
    "AIzaSyArr9UOyunCdP64Lfn01S6L4v3rS3MzVIw",
    "AIzaSyCkHro9fWyNr9kpqOw1Q9xihPtLiAmkGiE"
]

ADB_PATH = r"G:\AndroidStudio\platform-tools\adb.exe"
if not os.path.exists(ADB_PATH):
    ADB_PATH = "adb"

DEVICE_WIDTH = 0
DEVICE_HEIGHT = 0

def get_device_resolution():
    global DEVICE_WIDTH, DEVICE_HEIGHT
    try:
        result = subprocess.run([ADB_PATH, "shell", "wm", "size"], capture_output=True, text=True, check=True)
        match = re.search(r'Physical size: (\d+)x(\d+)', result.stdout)
        if match:
            DEVICE_WIDTH = int(match.group(1))
            DEVICE_HEIGHT = int(match.group(2))
            print(f"Device resolution detected: {DEVICE_WIDTH}x{DEVICE_HEIGHT}")
        else:
            DEVICE_WIDTH = 1080
            DEVICE_HEIGHT = 2400
    except:
        DEVICE_WIDTH = 1080
        DEVICE_HEIGHT = 2400

def capture_logcat(filename="crash_log.txt"):
    try:
        with open(filename, "w", encoding="utf-8") as f:
            subprocess.run([ADB_PATH, "logcat", "-d", "-t", "500"], stdout=f, check=False)
    except Exception as e:
        print(f"Failed to capture logcat: {e}")

def get_screenshot_image():
    subprocess.run([ADB_PATH, "shell", "screencap", "-p", "/sdcard/screen.png"], check=True)
    subprocess.run([ADB_PATH, "pull", "/sdcard/screen.png", "screen.png"], check=True)
    return Image.open("screen.png")

def scale_coord(val, axis_size):
    return int((val / 1000.0) * axis_size)

def perform_action(action_data):
    action = action_data.get("action")
    
    if action == "finish":
        return False

    if action == "tap":
        real_x = scale_coord(action_data.get("x"), DEVICE_WIDTH)
        real_y = scale_coord(action_data.get("y"), DEVICE_HEIGHT)
        print(f"ACTION: Tap {real_x}, {real_y}")
        subprocess.run([ADB_PATH, "shell", "input", "tap", str(real_x), str(real_y)], check=True)
        
    elif action == "swipe":
        real_x1 = scale_coord(action_data.get("x"), DEVICE_WIDTH)
        real_y1 = scale_coord(action_data.get("y"), DEVICE_HEIGHT)
        real_x2 = scale_coord(action_data.get("end_x"), DEVICE_WIDTH)
        real_y2 = scale_coord(action_data.get("end_y"), DEVICE_HEIGHT)
        duration = action_data.get("duration_ms", 300)
        print(f"ACTION: Swipe {real_x1},{real_y1} -> {real_x2},{real_y2}")
        subprocess.run([ADB_PATH, "shell", "input", "swipe", str(real_x1), str(real_y1), str(real_x2), str(real_y2), str(duration)], check=True)
        
    return True

def get_gemini_decision(image, history_summary):
    models_to_try = ["gemini-2.0-flash", "gemini-2.5-flash"] 
    
    prompt = f"""
    Ты — Автономный Тестировщик Android приложений.
    
    ТВОЯ ЦЕЛЬ: Обойти ВСЕ экраны приложения (Дневник, Оценки, Домашние задания, Профиль, Настройки).
    
    ИНСТРУКЦИЯ:
    1. Внимательно посмотри на скриншот. Где ты находишься?
    2. Вспомни историю действий. Если ты тут уже был - УХОДИ. Жми на навигацию, кнопки назад, пункты меню.
    3. ИЩИ БАГИ попутно: Кривая верстка, английский текст, наложения, битые иконки.
    4. НЕ ПИШИ БАГИ, КОТОРЫЕ ТЫ УЖЕ НАХОДИЛ РАНЕЕ.
    5. Если ты прошел 5+ экранов и нашел достаточно проблем, или зашел в тупик - делай "finish".
    
    ВАЖНО: Приоритет - НАВИГАЦИЯ. Не сиди на одном экране. Ты должен прокликать всё меню.
    
    История действий:
    {json.dumps(history_summary[-5:], indent=2, ensure_ascii=False)}
    
    ВЫВОД JSON:
    {{
        "analysis": "Где я, что вижу...",
        "new_issues": ["Только НОВЫЕ проблемы на этом экране"],
        "action": "tap" | "swipe" | "finish",
        "x": 500, "y": 500,
        "end_x": 500, "end_y": 500,
        "duration_ms": 300,
        "reason": "Почему я это делаю (например: 'Иду в раздел Оценки')"
    }}
    Координаты 0-1000.
    """

    for key in API_KEYS:
        try:
            client = genai.Client(api_key=key)
            for model_name in models_to_try:
                try:
                    response = client.models.generate_content(
                        model=model_name,
                        contents=[prompt, image],
                        config=types.GenerateContentConfig(
                            response_mime_type="application/json" 
                        )
                    )
                    if response.text:
                        return response.text
                except:
                    continue
        except:
            continue
    return None

def main():
    get_device_resolution()
    history = []
    all_issues = []
    max_steps = 20 # Ограничим 20 шагами для скорости, но качественными
    
    print("Запуск AI Explorer (Navigation Focus)...")
    
    for step in range(max_steps):
        print(f"\n--- Шаг {step + 1}/{max_steps} ---")
        time.sleep(2) # Wait for UI
        
        try:
            image = get_screenshot_image()
        except Exception as e:
            print(f"Screenshot failed: {e}")
            break
            
        history_summary = [{"action": h["action"], "reason": h["reason"], "screen": h.get("analysis", "")[:20]} for h in history]
        response_text = get_gemini_decision(image, history_summary)
        
        if not response_text:
            print("AI молчит.")
            break
            
        try:
            text = response_text.replace("```json", "").replace("```", "").strip()
            decision = json.loads(text)
            
            new_issues = decision.get('new_issues', [])
            if new_issues:
                print(f"  -> Найдено проблем: {len(new_issues)}")
                all_issues.extend(new_issues)
            
            print(f"Analysis: {decision.get('analysis')}")
            print(f"Decision: {decision.get('action')} - {decision.get('reason')}")
            
            history.append(decision)
            
            if not perform_action(decision):
                print("AI решил закончить тест.")
                break
            
        except Exception as e:
            print(f"Error parsing/executing: {e}")
            break

    print("\n" + "="*30)
    print("ФИНАЛЬНЫЙ ОТЧЕТ О ПРОБЛЕМАХ")
    print("="*30)
    unique_issues = list(set(all_issues))
    if not unique_issues:
        print("Проблем не найдено (или AI их пропустил).")
    else:
        for i, issue in enumerate(unique_issues, 1):
            print(f"{i}. {issue}")
    print("="*30)

if __name__ == "__main__":
    main()