import subprocess
import os
import signal
import sys
import re

# Путь к ADB из вашего ui_walker.py
ADB_PATH = r"G:\AndroidStudio\platform-tools\adb.exe"
if not os.path.exists(ADB_PATH):
    ADB_PATH = "adb"

OUTPUT_FILE = "coords.txt"

def main():
    print(f"--- ЗАПИСЬ КООРДИНАТ (ADB) ---")
    print(f"Результат будет записан в: {os.path.abspath(OUTPUT_FILE)}")
    print("Нажмите Ctrl+C для остановки записи.")
    print("----------------------------------")

    # Очищаем файл перед записью
    with open(OUTPUT_FILE, "w") as f:
        f.write("X,Y,TYPE\n")

    cmd = [ADB_PATH, "shell", "getevent", "-l"]
    
    try:
        process = subprocess.Popen(
            cmd,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            text=True,
            bufsize=1,
            encoding='utf-8',
            errors='replace'
        )
    except FileNotFoundError:
        print(f"Ошибка: ADB не найден по пути {ADB_PATH}")
        input("Нажмите Enter для выхода...")
        return

    current_x = -1
    current_y = -1
    
    # Регулярки для поиска координат
    # Пример строки: /dev/input/event4: EV_ABS       ABS_MT_POSITION_X    000003e5
    re_x = re.compile(r"ABS_MT_POSITION_X\s+([0-9a-fA-F]+)")
    re_y = re.compile(r"ABS_MT_POSITION_Y\s+([0-9a-fA-F]+)")
    re_touch_down = re.compile(r"BTN_TOUCH\s+DOWN")
    re_touch_up = re.compile(r"BTN_TOUCH\s+UP")

    try:
        with open(OUTPUT_FILE, "a") as f:
            for line in process.stdout:
                line = line.strip()
                
                # Парсим X
                mx = re_x.search(line)
                if mx:
                    val_hex = mx.group(1)
                    current_x = int(val_hex, 16)
                
                # Парсим Y
                my = re_y.search(line)
                if my:
                    val_hex = my.group(1)
                    current_y = int(val_hex, 16)
                
                # Синхронизация события (обычно означает "кадр" данных готов)
                if "SYN_REPORT" in line:
                    if current_x != -1 and current_y != -1:
                        # Записываем текущую известную позицию
                        log_str = f"{current_x},{current_y},MOVE"
                        print(f"Captured: {log_str}")
                        f.write(log_str + "\n")
                        f.flush()

                # Отслеживание нажатий/отпусканий (опционально, для красоты лога)
                if re_touch_down.search(line):
                     f.write(f"{current_x},{current_y},DOWN\n")
                if re_touch_up.search(line):
                     f.write(f"{current_x},{current_y},UP\n")
                     # Сброс координат при отпускании часто не нужен, но можно для чистоты
                     # current_x = -1
                     # current_y = -1

    except KeyboardInterrupt:
        print("\nЗапись остановлена пользователем.")
    finally:
        process.terminate()
        print(f"Файл сохранен: {OUTPUT_FILE}")
        # Даем время прочитать вывод перед закрытием окна
        input("Нажмите Enter, чтобы закрыть окно...")

if __name__ == "__main__":
    main()
