import time
import random
import json
import uuid
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8081/api/metrics/sync"
STATION_ID = str(uuid.uuid4())
TARGET_RPS = 200
WORKER_COUNT = 20

def generate_and_send():
    payload = {
        "istasyon_id": STATION_ID,
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