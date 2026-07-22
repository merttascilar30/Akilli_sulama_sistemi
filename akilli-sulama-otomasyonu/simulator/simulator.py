import time
import json
import random
import requests

SERVER_URL = "http://localhost:8081/api/metrics/async"

# Veritabanında (pgAdmin) kayıtlı olacak istasyon ID'lerini liste yapıyoruz
STATIONS = [
    "11111111-1111-1111-1111-111111111111", # 1. İstasyon (Adana Merkez)
    "22222222-2222-2222-2222-222222222222"  # 2. İstasyon (Kampüs)
    # Kendi UUID'ni de (2d28ec28...) buraya ekleyebilirsin
]

# Her istasyonun GÜNCEL NEM ve VANA DURUMUNU ayrı ayrı tutacak sözlük
station_states = {}

# Başlangıçta her istasyona rastgele mantıksal başlangıç değerleri atıyoruz
for sid in STATIONS:
    station_states[sid] = {
        "moisture": round(random.uniform(32.0, 38.0), 1),
        "is_watering": False
    }

def generate_sensor_data(sid):
    # İlgili istasyonun mevcut durumunu sözlükten çek
    state = station_states[sid]
    
    if state["is_watering"]:
        # Vana AÇIK: Toprak sulanıyor, nem sakince yükseliyor
        state["moisture"] += round(random.uniform(0.5, 1.0), 1)
        if state["moisture"] >= 40.0: # Tarla Kapasitesi sınırı
            state["moisture"] = 40.0
            state["is_watering"] = False
    else:
        # Vana KAPALI: Toprak sakince kuruyor
        state["moisture"] -= round(random.uniform(0.3, 0.6), 1)
        if state["moisture"] <= 30.5: # Dinamik eşik (~31.7%) altı
            state["is_watering"] = True
            
    # Sadece o istasyona ait sensör paketi
    payload = {
        "istasyonId": sid,
        "nem": round(state["moisture"], 1),
        "sicaklik": round(random.uniform(28.0, 31.0), 1),
        "havaNemi": round(random.uniform(55.0, 62.0), 1),
        "suSeviyesi": round(random.uniform(88.0, 92.0), 1)
    }
    return payload

def main():
    print("[*] Çoklu İstasyon Mantıksal Simülatörü Başlatıldı.")
    print(f"[*] Veriler {SERVER_URL} adresine gönderiliyor...\n")
    
    headers = {'Content-Type': 'application/json'}
    
    while True:
        try:
            # Döngü ile sırayla her istasyon için veri üret ve gönder
            for sid in STATIONS:
                data = generate_sensor_data(sid)
                response = requests.post(SERVER_URL, data=json.dumps(data), headers=headers)
                
                if response.status_code in (200, 201, 202):
                    print(f"[+] [ID: {sid[:8]}...] Nem: %{data['nem']} | Sıc: {data['sicaklik']}°C")
                else:
                    print(f"[-] HATA: Sunucu Yanıtı {response.status_code} (ID: {sid[:8]}...)")
            
            print("-" * 50)
            time.sleep(1.0)
            
        except Exception as e:
            print(f"[!] Bağlantı Hatası: Backend sunucusuna erişilemiyor ({e})")
            time.sleep(2)

if __name__ == "__main__":
    main()