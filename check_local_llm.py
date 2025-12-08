import urllib.request
import json
import sys

url = "http://127.0.0.1:1234/v1/models"

try:
    print(f"Checking {url}...")
    with urllib.request.urlopen(url) as response:
        print("Status:", response.status)
        print("Body:", response.read().decode('utf-8'))
except Exception as e:
    print("Error:", e)
