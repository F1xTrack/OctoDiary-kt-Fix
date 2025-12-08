import os
import time
import subprocess
import json
import re
import argparse
import sys
import base64
import urllib.request
import shutil
import io
from datetime import datetime
from PIL import Image, ImageChops

# Set UTF-8 for console output
sys.stdout.reconfigure(encoding='utf-8')

ADB_PATH = r"G:\AndroidStudio\platform-tools\adb.exe"
if not os.path.exists(ADB_PATH):
    ADB_PATH = "adb"

APP_PACKAGE = "org.bxkr.octodiary.debug"
APP_ACTIVITY = "org.bxkr.octodiary.MainActivity"

DEVICE_WIDTH = 0
DEVICE_HEIGHT = 0
DEBUG_DIR = ""

# Default configuration
DEFAULT_API_BASE = "https://api.aitunnel.ru/v1"
DEFAULT_MODEL = "gemini-2.5-flash"
DEFAULT_API_KEY = "sk-aitunnel-RkdtWoeHthOEexZTNbl9J95t3WqNsxE0"

def setup_debug_dir():
    global DEBUG_DIR
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    DEBUG_DIR = os.path.join("debug_sessions", timestamp)
    os.makedirs(DEBUG_DIR, exist_ok=True)
    print(f"📂 Debug artifacts: {DEBUG_DIR}")

def run_adb_command(cmd, check=True, capture_output=False, text=False, stdout=None, retries=3, delay=2):
    for attempt in range(retries):
        try:
            if stdout:
                return subprocess.run(cmd, stdout=stdout, check=check)
            else:
                return subprocess.run(cmd, capture_output=capture_output, text=text, check=check, encoding='utf-8', errors='replace')
        except Exception as e:
            print(f"ADB Error (attempt {attempt+1}): {e}")
            time.sleep(delay)
            subprocess.run([ADB_PATH, "disconnect"], capture_output=True)
            subprocess.run([ADB_PATH, "connect"], capture_output=True)
    raise Exception("ADB command failed after retries")

def launch_app():
    print(f"🚀 Launching {APP_PACKAGE}...")
    run_adb_command([ADB_PATH, "shell", "am", "force-stop", APP_PACKAGE], check=False)
    run_adb_command([ADB_PATH, "shell", "am", "start", "-n", f"{APP_PACKAGE}/{APP_ACTIVITY}"], check=True)
    print("⏳ Waiting 5s for app to load...")
    time.sleep(5)

def check_initial_crash():
    print("🚑 Checking for initial crash...")
    res = run_adb_command([ADB_PATH, "shell", "pidof", APP_PACKAGE], capture_output=True, text=True, check=False)
    pid = res.stdout.strip()
    
    if not pid:
        print(f"💀 FATAL: App {APP_PACKAGE} died immediately after launch!")
        print("📋 Last 20 lines of Logcat (Errors):")
        log = run_adb_command([ADB_PATH, "shell", "logcat", "-d", "-t", "20", "*:E"], capture_output=True, text=True, check=False)
        print(log.stdout)
        return True
    
    print(f"✅ App is alive (PID: {pid})")
    return False

def get_device_resolution():
    global DEVICE_WIDTH, DEVICE_HEIGHT
    try:
        result = run_adb_command([ADB_PATH, "shell", "wm", "size"], capture_output=True, text=True, check=True)
        match = re.search(r'Physical size: (\d+)x(\d+)', result.stdout)
        if match:
            DEVICE_WIDTH = int(match.group(1))
            DEVICE_HEIGHT = int(match.group(2))
            print(f"📱 Resolution: {DEVICE_WIDTH}x{DEVICE_HEIGHT}")
        else:
            print("⚠️ Unknown resolution, defaulting to 1080x2400.")
            DEVICE_WIDTH = 1080
            DEVICE_HEIGHT = 2400
    except:
        DEVICE_WIDTH = 1080
        DEVICE_HEIGHT = 2400

def get_screenshot_image(step_num):
    local_path = "screen.png"
    if os.path.exists(local_path):
        os.remove(local_path)
        
    run_adb_command([ADB_PATH, "shell", "screencap", "-p", "/sdcard/screen.png"], check=True)
    run_adb_command([ADB_PATH, "pull", "/sdcard/screen.png", local_path], check=True)
    
    if not os.path.exists(local_path):
        raise Exception("Failed to pull screenshot")

    if DEBUG_DIR:
        debug_path = os.path.join(DEBUG_DIR, f"step_{{step_num:03d}}_screen.png")
        shutil.copy(local_path, debug_path)

    return local_path

def scale_coord(val, axis_size):
    if val is None: return 0
    return int((val / 1000.0) * axis_size)

def perform_action(action_data):
    action = action_data.get("action")
    reason = action_data.get("reason", "No reason")
    
    print(f"🤖 Action: {action} | Reason: {reason}")

    if not action:
        if "new_issues" in action_data and action_data["new_issues"]:
            print("⚠️ No action provided, but issues found. Continuing...")
            return True
        print("❌ No action provided. Finishing...")
        return False

    if action == "finish":
        print(f"🛑 AI finished task.")
        return False

    if action == "wait":
        seconds = action_data.get("duration_seconds", 5) or 5
        print(f"⏳ Waiting {seconds}s")
        time.sleep(seconds)
        return True

    if action == "tap":
        real_x = scale_coord(action_data.get("x"), DEVICE_WIDTH)
        real_y = scale_coord(action_data.get("y"), DEVICE_HEIGHT)
        print(f"👆 Tap: {real_x}, {real_y}")
        
        # DEBUG TAP VISUALIZATION
        try:
            if DEBUG_DIR and os.path.exists("screen.png"):
                from PIL import ImageDraw
                with Image.open("screen.png") as img:
                    draw = ImageDraw.Draw(img)
                    r = 20
                    draw.ellipse((real_x-r, real_y-r, real_x+r, real_y+r), fill=(255, 0, 0, 128), outline="red")
                    timestamp = datetime.now().strftime("%H%M%S")
                    debug_tap_path = os.path.join(DEBUG_DIR, f"tap_{timestamp}_{real_x}_{real_y}.png")
                    img.save(debug_tap_path)
                    print(f"📸 Debug tap saved to {debug_tap_path}")
        except Exception as e:
            print(f"⚠️ Failed to save debug tap image: {e}")

        run_adb_command([ADB_PATH, "shell", "input", "tap", str(real_x), str(real_y)], check=True)
        
    elif action == "fill":
        text = action_data.get("text", "")
        text_adb = text.replace(" ", "%s") 
        print(f"✍️ Fill: '{text}'")
        run_adb_command([ADB_PATH, "shell", "input", "text", text_adb], check=True)

    elif action == "swipe":
        x1 = scale_coord(action_data.get("x"), DEVICE_WIDTH)
        y1 = scale_coord(action_data.get("y"), DEVICE_HEIGHT)
        x2 = scale_coord(action_data.get("end_x"), DEVICE_WIDTH)
        y2 = scale_coord(action_data.get("end_y"), DEVICE_HEIGHT)
        dur = action_data.get("duration_ms", 300) or 300
        print(f"vk Swipe: {x1},{y1} -> {x2},{y2}")
        run_adb_command([ADB_PATH, "shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(dur)], check=True)

    elif action == "back":
        print("🔙 System Back")
        run_adb_command([ADB_PATH, "shell", "input", "keyevent", "KEYCODE_BACK"], check=True)
    
    elif action == "home":
        print("🏠 System Home")
        run_adb_command([ADB_PATH, "shell", "input", "keyevent", "KEYCODE_HOME"], check=True)
    
    elif action == "recents":
        print("🗂️ System Recents")
        run_adb_command([ADB_PATH, "shell", "input", "keyevent", "KEYCODE_APP_SWITCH"], check=True)

    elif action == "rotate":
        mode = action_data.get("orientation", "portrait")
        print(f"🔄 Rotate to {mode}")
        run_adb_command([ADB_PATH, "shell", "settings", "put", "system", "accelerometer_rotation", "0"], check=True)
        if mode == "landscape":
            run_adb_command([ADB_PATH, "shell", "settings", "put", "system", "user_rotation", "1"], check=True)
        else:
            run_adb_command([ADB_PATH, "shell", "settings", "put", "system", "user_rotation", "0"], check=True)
        time.sleep(2)

    elif action == "restart_app":
        print(f"🔄 RESTARTING APP {APP_PACKAGE}...")
        run_adb_command([ADB_PATH, "shell", "am", "force-stop", APP_PACKAGE], check=True)
        time.sleep(1)
        run_adb_command([ADB_PATH, "shell", "am", "start", "-n", f"{APP_PACKAGE}/{APP_ACTIVITY}"], check=True)
        print("⏳ Waiting 5s for app to load...")
        time.sleep(5)

    return True

def clean_json_response(text):
    text = re.sub(r'<think>.*?</think>', '', text, flags=re.DOTALL)
    match = re.search(r'\{.*?\}', text, re.DOTALL)
    if match:
        return match.group(0)
    return text.strip()

def get_llm_decision(image_path, history_summary, custom_instruction, step_num, api_base, api_key, model):
    url = f"{api_base}/chat/completions"
    
    with Image.open(image_path) as img:
        # img.thumbnail((720, 2000))  <-- Disabled resizing as per user request
        img_byte_arr = io.BytesIO()
        img.save(img_byte_arr, format='PNG')
        base64_image = base64.b64encode(img_byte_arr.getvalue()).decode('utf-8')

    prompt = f"""
    ROLE: Autonomous Android App QA Engineer.
    GOAL: {custom_instruction}
    
    CONTEXT:
    - Device Resolution: {DEVICE_WIDTH}x{DEVICE_HEIGHT}
    - Current Step: {step_num}
    
    STRICT TEST PROTOCOL (Follow this logic to verify settings):
    1. TAP/TOGGLE a setting (e.g., 'Bold Text', 'Text Scale').
    2. IMMEDIATE VERIFICATION: Does the UI change instantly?
    3. EXIT VERIFICATION: You MUST tap 'back' or 'restart_app' to leave the settings screen and see if the change applied to the rest of the app (Dashboard, Profile, etc.).
    4. REPORT: Determine if the setting works immediately, requires restart, or is broken.
    5. RESET: Go back to Settings and turn it OFF/Reset before testing the next one.

    NEGATIVE CONSTRAINTS (Do NOT do this):
    - DO NOT stay on the same screen toggling the same button multiple times. 
    - DO NOT open 'System Settings' (Android Settings). Stay in 'OctoDiary'.
    - DO NOT enter a loop. If you toggled something, your next action MUST be 'back' or 'restart_app'.

    AVAILABLE ACTIONS (Output JSON only):
    - {{ "action": "tap", "x": 500, "y": 500, "reason": "UNIQUE REASON FOR THIS SPECIFIC ACTION" }} 
    - {{ "action": "swipe", "x": 500, "y": 800, "end_x": 500, "end_y": 200, "reason": "UNIQUE REASON FOR THIS SPECIFIC ACTION" }}
    - {{ "action": "back", "reason": "UNIQUE REASON FOR THIS SPECIFIC ACTION" }} 
    - {{ "action": "restart_app", "reason": "UNIQUE REASON FOR THIS SPECIFIC ACTION" }}
    - {{ "action": "finish", "reason": "UNIQUE REASON FOR THIS SPECIFIC ACTION" }}
    - {{ "new_issues": ["Description of bug 1", "Description of bug 2"], "action": "...", "reason": "UNIQUE REASON FOR THIS SPECIFIC ACTION" }}
    
    HISTORY:
    {json.dumps(history_summary[-5:], indent=2)}
    
    OUTPUT: Strictly Valid JSON.
    """

    payload = {
        "model": model,
        "messages": [
            {
                "role": "system",
                "content": "You are a helpful AI assistant that controls an Android device via ADB. You output strictly JSON."
            },
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
        "temperature": 0.1,
        "max_tokens": 1000,
        "stream": False
    }

    try:
        print(f"📡 Sending request to {url} (Model: {model})...")
        headers = {
            'Content-Type': 'application/json',
            'Authorization': f'Bearer {api_key}'
        }
        req = urllib.request.Request(url, data=json.dumps(payload).encode('utf-8'), headers=headers)
        with urllib.request.urlopen(req, timeout=1200) as response:
            print(f"📥 Response Status: {response.status} {response.reason}")
            raw_response = response.read().decode('utf-8')
            
            if DEBUG_DIR:
                 with open(os.path.join(DEBUG_DIR, f"step_{{step_num:03d}}_local_raw_json.txt"), "w", encoding='utf-8') as f:
                    f.write(raw_response)
            
            result = json.loads(raw_response)
            content = result['choices'][0]['message']['content']
            return content
    except Exception as e:
        print(f"LLM Error: {e}", file=sys.stderr, flush=True)
        return None

def check_for_loop(history, new_decision):
    """
    Detects if the AI is repeating the same action or staying on screen too long without progress.
    Returns an override action if a loop is detected.
    """
    if not history:
        return None
    
    last_action = history[-1]

    # 0. Grace period after restart (allow trying the entry action again)
    if last_action['action'] == 'restart_app':
        return None
    
    # 1. Repetition Check (Same action 5 times in a row)
    # Check if the last 4 actions in history are identical to the new decision
    if len(history) >= 4:
        last_4 = history[-4:]
        is_stuck = True
        for h in last_4:
            if not (new_decision['action'] == h['action'] and 
                    new_decision.get('x') == h.get('x') and 
                    new_decision.get('y') == h.get('y')):
                is_stuck = False
                break
        
        if is_stuck:
            print("⚠️ Loop Detected: Same action repeated 5 times.")
            return {"action": "back", "reason": "Breaking strict action loop via Force Back"}

    # 2. Pattern Loop (A -> B -> A -> B -> A -> B) - Relaxed to 3 cycles (6 steps)
    if len(history) >= 6:
        # Check pattern A-B-A-B-A-B
        h = history
        if (h[-1]['action'] == h[-3]['action'] == h[-5]['action'] and
            h[-2]['action'] == h[-4]['action'] == new_decision['action']):
             print("⚠️ Loop Detected: Oscillating actions (3 cycles).")
             return {"action": "restart_app", "reason": "Breaking oscillation loop via Restart"}

    return None

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--instruction_file", type=str)
    parser.add_argument("--instruction", type=str)
    parser.add_argument("--max_steps", type=int, default=200)
    parser.add_argument("--api_base", type=str, default=DEFAULT_API_BASE, help="OpenAI-compatible API Base URL")
    parser.add_argument("--api_key", type=str, default=DEFAULT_API_KEY, help="API Key")
    parser.add_argument("--model", type=str, default=DEFAULT_MODEL, help="Model name")
    
    args = parser.parse_args()

    setup_debug_dir()
    
    if args.instruction_file:
        with open(args.instruction_file, 'r', encoding='utf-8') as f:
            instruction = f.read().strip()
    else:
        instruction = args.instruction or "Explore the app."

    launch_app()
    
    if check_initial_crash():
        print("❌ Initial crash check failed. Aborting.")
        sys.exit(1)

    get_device_resolution()
    history = []
    prev_screen_path = None

    print(f"🚀 Starting Walker")
    print(f"🎯 Goal: {instruction}")
    print(f"🔗 API: {args.api_base}")
    print(f"🧠 Model: {args.model}")

    for step in range(1, args.max_steps + 1):
        print(f"\n--- Step {step}/{args.max_steps} ---")
        
        try:
            img_path = get_screenshot_image(step)
            
            summary = [{"action": h["action"], "reason": h.get("reason", "")} for h in history]
            
            resp_text = get_llm_decision(img_path, summary, instruction, step, args.api_base, args.api_key, args.model)
            
            if not resp_text:
                print("💀 AI Error: No response.")
                break
            
            cleaned_json = clean_json_response(resp_text)
            try:
                decision = json.loads(cleaned_json)
            except json.JSONDecodeError as je:
                print(f"❌ JSON Error: {je} | Text: {cleaned_json[:100]}...")
                continue
            
            # --- LOOP DETECTION ---
            # override_decision = check_for_loop(history, decision)
            # if override_decision:
            #     print(f"🛡️ Auto-Correction: Replacing {decision['action']} with {override_decision['action']}")
            #     decision = override_decision
            # ----------------------

            if "new_issues" in decision:
                print(f"🐛 ISSUES: {decision['new_issues']}")
            
            history.append(decision)
            
            if not perform_action(decision):
                break
                
            time.sleep(2) 
            
        except Exception as e:
            print(f"❌ Runtime Error: {e}")
            break

if __name__ == "__main__":
    main()
