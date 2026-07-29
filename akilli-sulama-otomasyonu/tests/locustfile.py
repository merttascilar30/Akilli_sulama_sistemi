import random
import uuid
from locust import HttpUser, task, constant

class SensorMetricsLoadTest(HttpUser):
    wait_time = constant(0)

    @task
    def post_sensor_data(self):
        payload = {
            "istasyonId": str(uuid.uuid4()),
            "nem": round(random.uniform(10.0, 50.0), 1),
            "sicaklik": round(random.uniform(20.0, 40.0), 1),
            "havaNemi": round(random.uniform(30.0, 70.0), 1),
            "suSeviyesi": round(random.uniform(10.0, 100.0), 1)
        }
        
        with self.client.post("/api/metrics/async", json=payload, catch_response=True, timeout=5.0) as response:
            if response.status_code in (200, 201, 202):
                response.success()
            else:
                response.failure(f"HTTP {response.status_code}")