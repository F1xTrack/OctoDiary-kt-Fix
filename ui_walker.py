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
    print(f"Capturing logcat to {filename}...")
    try:
        with open(filename, "w", encoding="utf-8") as f:
            subprocess.run([ADB_PATH, "logcat", "-d", "-t", "1000"], stdout=f, check=True)
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
        print(f"ACTION: Testing Finished.")
        result = action_data.get("test_result", {{}})
        print(f"\n========== ОТЧЕТ О ТЕСТИРОВАНИИ ==========")
        print(f"Статус: {result.get('status')}")
        print(f"Итог: {result.get('summary')}")
        
        nitpicks = result.get('nitpicks', [])
        if nitpicks:
            print("\n--- НАЙДЕННЫЕ ПРОБЛЕМЫ И ПРИДИРКИ ---")
            for i, issue in enumerate(nitpicks, 1):
                print(f"{i}. {issue}")
        print(f"==========================================\n")
        
        if result.get("status") == "FAIL":
            capture_logcat("failure_log.txt")
        return False

    if action == "tap":
        real_x = scale_coord(action_data.get("x"), DEVICE_WIDTH)
        real_y = scale_coord(action_data.get("y"), DEVICE_HEIGHT)
        print(f"ACTION: Tapping at {real_x}, {real_y}")
        subprocess.run([ADB_PATH, "shell", "input", "tap", str(real_x), str(real_y)], check=True)
        
    elif action == "swipe":
        real_x1 = scale_coord(action_data.get("x"), DEVICE_WIDTH)
        real_y1 = scale_coord(action_data.get("y"), DEVICE_HEIGHT)
        real_x2 = scale_coord(action_data.get("end_x"), DEVICE_WIDTH)
        real_y2 = scale_coord(action_data.get("end_y"), DEVICE_HEIGHT)
        duration = action_data.get("duration_ms", 300)
        print(f"ACTION: Swiping from {real_x1},{real_y1} to {real_x2},{real_y2}")
        subprocess.run([ADB_PATH, "shell", "input", "swipe", str(real_x1), str(real_y1), str(real_x2), str(real_y2), str(duration)], check=True)
        
    return True

def get_gemini_decision(image, history_summary):
    models_to_try = ["gemini-2.0-flash", "gemini-2.5-flash"] 
    
    prompt = f"""
    Ты — **Ведущий QA-инженер и UI/UX эксперт**, проводящий глубокий аудит Android-приложения 'OctoDiary'.
    
    ТВОЯ ЗАДАЧА:
    - Провести **максимально полный визуальный и логический анализ** текущего экрана.
    - Выявить ВСЕ возможные дефекты: от критических багов до мельчайших UI-недочетов (отступы, шрифты, выравнивание).
    - Сформулировать список проблем **исключительно конструктивно и профессионально**. Используй формат "Требуется доработка: [описание]". Избегай эмоций и грубости.
    
    ЦЕЛЬ ТЕСТА: Пройти по всем доступным разделам приложения, чтобы составить исчерпывающий список улучшений.
    
    ПРИОРИТЕТНЫЕ ЗАДАЧИ:
    1. Открыть "Дневник" (Diary).
    2. Открыть "Настройки" (Settings).
    3. Открыть "AI Dashboard" / "AI Помощник".
    4. Открыть "Профиль" (Profile).
    5. Проверить элементы управления.
    
    История действий (кратко):
    {json.dumps(history_summary, indent=2, ensure_ascii=False)}
    
    АНАЛИЗ СКРИНШОТА:
    Найди максимум проблем за один раз. Обрати внимание на:
    1. **Локализация:** Смешение языков, непереведенные строки, грамматические ошибки.
    2. **UI/UX:** Обрезанный текст, наложение элементов, неправильные отступы (padding/margin), нечитаемый контраст, "мыльные" иконки, неинтуитивные элементы.
    3. **Состояние:** Несоответствие состояния переключателей/чекбоксов, "пустые" экраны без заглушек (empty state).
    4. **Технические артефакты:** Кнопки отладки (Debug), системные сообщения об ошибках, placeholder-данные (null, NaN).
    
    ВЫБОР ДЕЙСТВИЯ:
    - Если видишь навигационные элементы (меню, кнопки), которые еще не посещал — переходи к ним.
    - Если текущий экран полностью проанализирован, двигайся дальше.
    - Если прошел основные сценарии или нашел критический блокер — делай "finish".
    
    ВЫВОД JSON (Без Markdown):
    {{
        "analysis": "Профессиональное описание экрана...",
        "issues": ["Требуется доработка: Текст заголовка обрезан", "Требуется доработка: Смешение языков в меню"],
        "action": "tap" | "swipe" | "finish",
        "x": 500, "y": 500,
        "end_x": 500, "end_y": 500,
        "duration_ms": 300,
        "reason": "Обоснование действия...",
        "test_result": {{
            "status": "PASS" | "FAIL", 
            "summary": "Краткий итог сессии...", 
            "nitpicks": ["Полный список всех уникальных проблем, найденных за сессию"]
        }} (Обязательно ТОЛЬКО если action это "finish")
    }}
    Координаты нормализованы 0-1000.
    """

    for key in API_KEYS:
        try:
            client = genai.Client(api_key=key)
            for model_name in models_to_try:
                try:
                    print(f"Думаю с помощью {model_name}...")
                    response = client.models.generate_content(
                        model=model_name,
                        contents=[prompt, image],
                        config=types.GenerateContentConfig(
                            response_mime_type="application/json" 
                        )
                    )
                    if response.text:
                        return response.text
                except Exception as e:
                    # print(f"Model {model_name} failed: {e}")
                    pass
        except Exception as e:
            pass
    return None

def main():
    get_device_resolution()
    history = []
    max_steps = 100
    
    print("Запуск Hyper-Critical UI Walker (RUS)...")
    
    for step in range(max_steps):
        print(f"\n--- Шаг {step + 1}/{max_steps} ---")
        print("Жду обновления UI...")
        time.sleep(3)
        
        try:
            image = get_screenshot_image()
        except Exception as e:
            print(f"Скриншот не удался: {e}")
            capture_logcat("screenshot_fail_log.txt")
            break
            
        history_summary = [f"Шаг {h['step']}: Действие {h['action']}, Причина: {h['reason']}, Проблемы: {h['issues_found']}" for h in history]
        response_text = get_gemini_decision(image, history_summary)
        
        if not response_text:
            print("Не удалось получить решение от AI.")
            break
            
        try:
            # Clean potential markdown
            text = response_text.replace("```json", "").replace("```", "").strip()
            decision = json.loads(text)
            
            issues = decision.get('issues', [])
            if issues:
                print(f"⚠️ НАЙДЕНЫ ПРОБЛЕМЫ: {issues}")
            
            print(f"Действие: {decision.get('action')} ({decision.get('reason')})")
            
            history.append({
                "step": step + 1,
                "action": decision.get("action"),
                "reason": decision.get("reason"),
                "screen_analysis": decision.get("analysis"),
                "issues_found": issues
            })
            
            if not perform_action(decision):
                break
            
        except Exception as e:
            print(f"Ошибка выполнения шага: {e}")
            print(f"Сырой ответ: {response_text}")
            capture_logcat("execution_error_log.txt")
            break

    print("\nUI Walker завершен.")

if __name__ == "__main__":
    main()
