import os
import time
import subprocess
import re
import sys

# Force UTF-8 output
sys.stdout.reconfigure(encoding='utf-8')

ADB_PATH = r"G:\AndroidStudio\platform-tools\adb.exe"
if not os.path.exists(ADB_PATH):
    ADB_PATH = "adb" # Fallback to PATH

DEVICE_WIDTH = 0
DEVICE_HEIGHT = 0
SENSOR_MAX_X = 0
SENSOR_MAX_Y = 0

def get_device_resolution():
    global DEVICE_WIDTH, DEVICE_HEIGHT
    print("Определение разрешения экрана...")
    try:
        result = subprocess.run([ADB_PATH, "shell", "wm", "size"], capture_output=True, text=True, check=True)
        # Check for Override size first
        match_override = re.search(r'Override size: (\d+)x(\d+)', result.stdout)
        if match_override:
            DEVICE_WIDTH = int(match_override.group(1))
            DEVICE_HEIGHT = int(match_override.group(2))
        else:
            match = re.search(r'Physical size: (\d+)x(\d+)', result.stdout)
            if match:
                DEVICE_WIDTH = int(match.group(1))
                DEVICE_HEIGHT = int(match.group(2))
            else:
                DEVICE_WIDTH = 1080
                DEVICE_HEIGHT = 2400
        print(f"Экран: {DEVICE_WIDTH}x{DEVICE_HEIGHT}")
    except Exception as e:
        print(f"Ошибка получения разрешения экрана: {e}")
        DEVICE_WIDTH = 1080
        DEVICE_HEIGHT = 2400

def get_sensor_calibration():
    global SENSOR_MAX_X, SENSOR_MAX_Y
    print("Калибровка сенсора (поиск max значений)...")
    try:
        result = subprocess.run([ADB_PATH, "shell", "getevent", "-p"], capture_output=True, text=True, encoding='utf-8', errors='replace')
        
        # Simple parser to find the first device with ABS_MT_POSITION_X
        # Output format example:
        # events:
        #   ABS (0003): 0035  : value 0, min 0, max 4095, fuzz 0, flat 0, resolution 0
        
        lines = result.stdout.splitlines()
        found_x = False
        found_y = False
        
        for line in lines:
            # Look for 0035 (ABS_MT_POSITION_X)
            if "0035" in line and "max" in line:
                m = re.search(r'max (\d+)', line)
                if m:
                    SENSOR_MAX_X = int(m.group(1))
                    found_x = True
            
            # Look for 0036 (ABS_MT_POSITION_Y)
            if "0036" in line and "max" in line:
                m = re.search(r'max (\d+)', line)
                if m:
                    SENSOR_MAX_Y = int(m.group(1))
                    found_y = True
            
            if found_x and found_y:
                break
        
        if not SENSOR_MAX_X or not SENSOR_MAX_Y:
            print("⚠️ ВНИМАНИЕ: Не удалось определить границы сенсора автоматически.")
            print("Использую значения по умолчанию (обычно 4095 для многих устройств или равно экрану).")
            # Fallback heuristic: assume raw matches screen or high res
            SENSOR_MAX_X = 0 # Will try to detect from file or ask user? 
            # Let's try to infer from the file data if we can't find it
    except Exception as e:
        print(f"Ошибка калибровки: {e}")

def replay_file(filename="coords.txt"):
    if not os.path.exists(filename):
        print(f"Файл {filename} не найден!")
        return

    print(f"Чтение {filename}...")
    actions = []
    
    # Pre-scan for calibration if needed
    max_file_x = 0
    max_file_y = 0

    with open(filename, "r") as f:
        # Skip header
        header = f.readline()
        
        for line in f:
            parts = line.strip().split(',')
            if len(parts) < 3:
                continue
            
            try:
                x = int(parts[0])
                y = int(parts[1])
                event_type = parts[2]
                
                max_file_x = max(max_file_x, x)
                max_file_y = max(max_file_y, y)
                
                # Filter what to replay. 
                # Replaying MOVE is too slow via 'input tap'. 
                # We replay 'DOWN' events as taps.
                if event_type == "DOWN":
                    actions.append((x, y))
            except ValueError:
                continue

    global SENSOR_MAX_X, SENSOR_MAX_Y
    
    # If getevent -p failed, use the max values seen in the file + some buffer or assume screen size
    if SENSOR_MAX_X == 0:
        if max_file_x > DEVICE_WIDTH:
             # Likely raw coordinates
             SENSOR_MAX_X = 4095 # Common default
             SENSOR_MAX_Y = 4095
             print(f"Предполагаю размер сенсора: {SENSOR_MAX_X}x{SENSOR_MAX_Y}")
        else:
             # Likely already screen coordinates?
             SENSOR_MAX_X = DEVICE_WIDTH
             SENSOR_MAX_Y = DEVICE_HEIGHT
             print("Координаты похожи на экранные, масштаб 1:1")

    print(f"Найдено {len(actions)} действий (нажатий).")
    print("Начинаю воспроизведение через 3 секунды...")
    time.sleep(3)
    
    for i, (raw_x, raw_y) in enumerate(actions):
        # Scale
        screen_x = int((raw_x / SENSOR_MAX_X) * DEVICE_WIDTH)
        screen_y = int((raw_y / SENSOR_MAX_Y) * DEVICE_HEIGHT)
        
        # Safety clamp
        screen_x = max(0, min(screen_x, DEVICE_WIDTH))
        screen_y = max(0, min(screen_y, DEVICE_HEIGHT))
        
        # Check if app is alive
        try:
            # Check if process exists
            pid_check = subprocess.run([ADB_PATH, "shell", "pidof", "org.bxkr.octodiary.debug"], capture_output=True, text=True)
            if not pid_check.stdout.strip():
                print("⚠️ Приложение упало или закрыто! Остановка скрипта.")
                break
        except Exception as e:
            print(f"Ошибка проверки состояния приложения: {e}")

        print(f"[{i+1}/{len(actions)}] Tap raw({raw_x},{raw_y}) -> screen({screen_x},{screen_y})")
        
        subprocess.run([ADB_PATH, "shell", "input", "tap", str(screen_x), str(screen_y)])
        
        # User requested 1 second delay
        time.sleep(1)

    print("Воспроизведение завершено.")

def main():
    get_device_resolution()
    get_sensor_calibration()
    
    print(f"Конфигурация: Экран {DEVICE_WIDTH}x{DEVICE_HEIGHT}, Сенсор {SENSOR_MAX_X}x{SENSOR_MAX_Y}")
    
    replay_file("coords.txt")

if __name__ == "__main__":
    main()