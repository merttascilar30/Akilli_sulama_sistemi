import time
import json
import random
import requests

# Doğru Backend API Asenkron Uç Noktası
SERVER_URL = "http://localhost:8081/api/metrics/async"

# Veri tabanında kayıtlı olan istasyon UUID'si
STATION_ID = "2d28ec28-e903-43b8-85fa-d472259eba64"

# Global Mantıksal Kuruma / Sulama Simülasyon Değişkenleri
current_soil_moisture = 38.0
is_watering = False

def generate_sensor_data():
    global current_soil_moisture, is_watering
    
    if is_watering:
        # Vana AÇIK: Toprak sulanıyor, nem sakince yükseliyor
        current_soil_moisture += round(random.uniform(0.5, 1.0), 1)
        if current_soil_moisture >= 40.0: # Tarla Kapasitesi sınırı
            current_soil_moisture = 40.0
            is_watering = False
    else:
        # Vana KAPALI: Toprak sakince kuruyor
        current_soil_moisture -= round(random.uniform(0.3, 0.6), 1)
        if current_soil_moisture <= 30.5: # Dinamik eşik (~31.7%) altı
            is_watering = True
            
    soil_moisture = round(current_soil_moisture, 1)
    temp = round(random.uniform(28.0, 31.0), 1)
    air_humidity = round(random.uniform(55.0, 62.0), 1)
    water_level = round(random.uniform(88.0, 92.0), 1)
    
    payload = {
        "istasyonId": STATION_ID,
        "nem": soil_moisture,
        "sicaklik": temp,
        "havaNemi": air_humidity,
        "suSeviyesi": water_level
    }
    return payload

def main():
    print("[*] Mantıksal Donanım Simülatörü Başlatıldı.")
    print(f"[*] Veriler {SERVER_URL} adresine gönderiliyor...\n")
    
    headers = {'Content-Type': 'application/json'}
    
    while True:
        try:
            data = generate_sensor_data()
            response = requests.post(SERVER_URL, data=json.dumps(data), headers=headers)
            
            if response.status_code in (200, 201, 202):
                print(f"[+] [202 ACCEPTED] Toprak Nemi: %{data['nem']} | Sıcaklık: {data['sicaklik']}°C | Hava Nemi: %{data['havaNemi']} | Su: %{data['suSeviyesi']}")
            else:
                print(f"[-] HATA: Sunucu Yanıtı {response.status_code}")
                
            time.sleep(1.0)
            
        except Exception as e:
            print(f"[!] Bağlantı Hatası: Backend sunucusuna erişilemiyor ({e})")
            time.sleep(2)

if __name__ == "__main__":
    main()