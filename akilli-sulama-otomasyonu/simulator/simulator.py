import time
import random
import json
import uuid
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8081/api/metrics/async"
STATION_ID = "2d28ec28-e903-43b8-85fa-d472259eba64"
TARGET_RPS = 200
WORKER_COUNT = 20

def generate_and_send():
    payload = {
        "istasyonId": STATION_ID,
        "nem": round(random.uniform(10.0, 50.0), 2),
        "sicaklik": round(random.uniform(15.0, 45.0), 2)
    }
    data = json.dumps(payload).encode('utf-8')
    req = urllib.request.Request(URL, data=data, headers={'Content-Type': 'application/json'}, method='POST')
    
    try:
        with urllib.request.urlopen(req, timeout=1):
            pass
    except (urllib.error.URLError, Exception):
        pass

def worker():
    sleep_time = WORKER_COUNT / TARGET_RPS
    while True:
        generate_and_send()
        time.sleep(sleep_time)

if __name__ == "__main__":
    with ThreadPoolExecutor(max_workers=WORKER_COUNT) as executor:
        for _ in range(WORKER_COUNT):
            executor.submit(worker)