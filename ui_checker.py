import os
import time
import subprocess
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

def get_screenshot_image():
    subprocess.run([ADB_PATH, "shell", "screencap", "-p", "/sdcard/screen.png"], check=True)
    subprocess.run([ADB_PATH, "pull", "/sdcard/screen.png", "screen.png"], check=True)
    # Return PIL Image
    return Image.open("screen.png")

def analyze_with_genai(image, prompt):
    # Try models in order of preference/availability
    # Using 2.0 Flash as primary, then 1.5 Flash (if still alive for some keys)
    models_to_try = ["gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro"] 
    
    for key in API_KEYS:
        try:
            client = genai.Client(api_key=key)
            for model_name in models_to_try:
                try:
                    print(f"Trying model: {model_name} with key ...{key[-4:]}")
                    response = client.models.generate_content(
                        model=model_name,
                        contents=[prompt, image]
                    )
                    if response.text:
                        return response.text
                    else:
                        print("Empty response text")
                except Exception as e:
                    print(f"Model {model_name} failed: {e}")
                    # Continue to next model
        except Exception as e:
            print(f"Client init failed for key: {e}")
            # Continue to next key
    return None

def main():
    print("Taking screenshot...")
    try:
        image = get_screenshot_image()
    except Exception as e:
        print(f"Failed to take screenshot: {e}")
        return

    print("Analyzing with Gemini...")
    prompt = """
    Analyze this screenshot of the Android app 'OctoDiary'.
    1. Is the bottom navigation bar visible?
    2. What content is displayed on the screen?
    3. Are there any visible errors, empty states, or loading indicators?
    4. Does the UI look correct for a school diary app?
    Provide a concise summary.
    """
    
    result = analyze_with_genai(image, prompt)
    if result:
        print("\n--- Gemini Analysis Result ---\n")
        print(result)
        print("\n------------------------------\n")
    else:
        print("Failed to get analysis from Gemini (all keys/models failed).")

if __name__ == "__main__":
    main()