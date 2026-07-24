import time
import json
import random
import threading
import requests
from http.server import BaseHTTPRequestHandler, HTTPServer

SERVER_URL = "http://localhost:8081/api/metrics/async"
KONTROL_DINLEYICI_PORT = 5001

# Veritabanında (pgAdmin) kayıtlı olacak istasyon ID'lerini liste yapıyoruz
STATIONS = [
    "11111111-1111-1111-1111-111111111111", # 1. İstasyon (Adana Merkez)
    "22222222-2222-2222-2222-222222222222"  # 2. İstasyon (Kampüs)
    # Kendi UUID'ni de (2d28ec28...) buraya ekleyebilirsin
]

MOD_OTONOM = "OTONOM"
MOD_MANUEL = "MANUEL"

# Her istasyonun GÜNCEL NEM, VANA DURUMU ve KONTROL MODUNU ayrı ayrı tutacak sözlük
station_states = {}
state_lock = threading.Lock()

# Başlangıçta her istasyona rastgele mantıksal başlangıç değerleri atıyoruz
for sid in STATIONS:
    station_states[sid] = {
        "moisture": round(random.uniform(32.0, 38.0), 1),
        "is_watering": False,
        "mod": MOD_OTONOM
    }

def generate_sensor_data(sid):
    # İlgili istasyonun mevcut durumunu sözlükten çek
    with state_lock:
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
            # İstasyon MANUEL moddaysa backend/kullanıcı karar verene kadar vana kendiliğinden açılmaz
            if state["mod"] == MOD_OTONOM and state["moisture"] <= 30.5: # Dinamik eşik (~31.7%) altı
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

def kontrol_komutunu_uygula(istasyon_id, komut):
    """Backend'den gelen manuel/otonom veya vana kapatma komutunu ilgili istasyona uygular."""
    with state_lock:
        state = station_states.get(istasyon_id)
        if state is None:
            return False

        if komut == MOD_OTONOM:
            state["mod"] = MOD_OTONOM
        else:
            # MANUEL veya KAPAT komutu: vana zorla kapatılır ve toprak kuruma döngüsüne alınır
            state["mod"] = MOD_MANUEL
            state["is_watering"] = False

        return True

class KontrolIstegiHandler(BaseHTTPRequestHandler):
    """Backend'den gelen manuel kontrol komutlarını dinleyen basit HTTP handler'ı."""

    def do_POST(self):
        if self.path != "/api/control":
            self.send_response(404)
            self.end_headers()
            return

        icerik_uzunlugu = int(self.headers.get("Content-Length", 0))
        istek_govdesi = self.rfile.read(icerik_uzunlugu) if icerik_uzunlugu > 0 else b"{}"

        try:
            veri = json.loads(istek_govdesi)
            istasyon_id = veri.get("istasyonId")
            komut = (veri.get("komut") or "").upper()

            basarili = kontrol_komutunu_uygula(istasyon_id, komut)

            self.send_response(200 if basarili else 404)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({"basarili": basarili}).encode("utf-8"))

            if basarili:
                print(f"[>] Kontrol komutu uygulandı: [ID: {istasyon_id[:8]}...] Komut: {komut}")
        except (ValueError, TypeError):
            self.send_response(400)
            self.end_headers()

    def log_message(self, format, *args):
        # Varsayılan erişim loglarını susturuyoruz, sadece kendi print çıktılarımızı görürüz
        pass

def kontrol_dinleyicisini_baslat():
    sunucu = HTTPServer(("0.0.0.0", KONTROL_DINLEYICI_PORT), KontrolIstegiHandler)
    print(f"[*] Manuel kontrol dinleyicisi http://localhost:{KONTROL_DINLEYICI_PORT}/api/control adresinde başlatıldı.")
    sunucu.serve_forever()

def main():
    print("[*] Çoklu İstasyon Mantıksal Simülatörü Başlatıldı.")
    print(f"[*] Veriler {SERVER_URL} adresine gönderiliyor...\n")

    threading.Thread(target=kontrol_dinleyicisini_baslat, daemon=True).start()

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
