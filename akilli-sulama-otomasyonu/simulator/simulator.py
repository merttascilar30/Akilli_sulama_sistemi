import time
import json
import random
import threading
import requests
from http.server import BaseHTTPRequestHandler, HTTPServer

SERVER_URL = "http://localhost:8081/api/metrics/async"
STATIONS_URL = "http://localhost:8081/api/istasyonlar"
KONTROL_DINLEYICI_PORT = 5001

MOD_OTONOM = "OTONOM"
MOD_MANUEL = "MANUEL"

station_states = {}
state_lock = threading.Lock()

# Tarımsal Eşik Hesaplama Formülü (Kural Motoru ile Birebir Aynı)
def calculate_threshold(tk, sn, kc):
    et0 = 5.0
    etc = et0 * kc
    k_dinamik = 0.50 + (0.02 * etc)
    if k_dinamik > 0.70:
        k_dinamik = 0.70
    return sn + k_dinamik * (tk - sn)

# 15. GÜN: Spring Boot'tan Gelen Manuel Kontrol İsteklerini Karşılayan HTTP Handler
class KontrolIstegiHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == '/api/control':
            content_length = int(self.headers['Content-Length'])
            post_data = self.rfile.read(content_length)
            
            try:
                body = json.loads(post_data.decode('utf-8'))
                istasyon_id = body.get('istasyonId')
                komut = body.get('komut')
                
                with state_lock:
                    if istasyon_id in station_states:
                        if komut == 'KAPAT':
                            station_states[istasyon_id]['valve'] = 0
                            station_states[istasyon_id]['mode'] = MOD_MANUEL
                            print(f"\n[⚠️ MANUEL KONTROL] {istasyon_id[:8]}... Vanası ARAYÜZDEN KAPATILDI!\n")
                        elif komut == 'MANUEL':
                            station_states[istasyon_id]['mode'] = MOD_MANUEL
                        elif komut == 'OTONOM':
                            station_states[istasyon_id]['mode'] = MOD_OTONOM
                            print(f"\n[🤖 MOD DEĞİŞİMİ] {istasyon_id[:8]}... OTONOM Moda Alındı.\n")

                self.send_response(200)
                self.send_header('Content-Type', 'application/json')
                self.end_headers()
                self.wfile.write(json.dumps({"status": "SUCCESS"}).encode('utf-8'))
            except Exception as e:
                self.send_response(400)
                self.end_headers()
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        pass

def kontrol_dinleyicisini_baslat():
    sunucu = HTTPServer(("0.0.0.0", KONTROL_DINLEYICI_PORT), KontrolIstegiHandler)
    print(f"[*] 15. Gün Manuel Kontrol Dinleyicisi http://localhost:{KONTROL_DINLEYICI_PORT}/api/control adresinde aktif.")
    sunucu.serve_forever()

def istasyon_simulasyonu(station):
    sid = station.get("id")
    tk = float(station.get("tarlaKapasitesi") or 40.0)
    sn = float(station.get("solmaNoktasi") or 20.0)
    kc = float(station.get("bitkiKatsayisi") or 0.85)

    esik = calculate_threshold(tk, sn, kc)

    with state_lock:
        station_states[sid] = {
            "moisture": round(tk - random.uniform(1.0, 3.0), 1),
            "valve": 0,
            "mode": MOD_OTONOM,
            "tk": tk,
            "sn": sn,
            "esik": esik
        }

    print(f"[+] {sid[:8]}... Başlatıldı | TK: %{tk} | Solma Noktası: %{sn} | Dinamik Eşik: ~%{esik:.1f}")

    headers = {'Content-Type': 'application/json'}

    while True:
        with state_lock:
            st = station_states[sid]
            
            # OTONOM MODDA KURUMA VE SULANMA DÖNGÜSÜ
            if st["mode"] == MOD_OTONOM:
                if st["valve"] == 0:
                    st["moisture"] -= round(random.uniform(0.2, 0.4), 1)
                    if st["moisture"] <= (st["esik"] - 0.5):
                        st["valve"] = 1
                else:
                    st["moisture"] += round(random.uniform(0.5, 0.9), 1)
                    if st["moisture"] >= st["tk"]:
                        st["moisture"] = st["tk"]
                        st["valve"] = 0
            else:
                # MANUEL MODDA VANA KAPALIYSA KURUMAYA DEVAM ET
                if st["valve"] == 0:
                    st["moisture"] -= round(random.uniform(0.1, 0.3), 1)
                    if st["moisture"] < 10.0:
                        st["moisture"] = 10.0

            current_m = round(st["moisture"], 1)

        data = {
            "istasyonId": sid,
            "nem": current_m,
            "sicaklik": round(random.uniform(25.0, 34.0), 1),
            "havaNemi": round(random.uniform(45.0, 65.0), 1),
            "suSeviyesi": round(random.uniform(80.0, 95.0), 1)
        }

        try:
            requests.post(SERVER_URL, data=json.dumps(data), headers=headers, timeout=2)
        except Exception:
            pass

        time.sleep(1.0)

def main():
    print("[*] Dinamik Çoklu İstasyon & Manuel Kontrol Destekli Simülatör Başlatılıyor...\n")
    
    # 15. Gün HTTP Dinleyicisini Arka Plan Thread'inde Başlat
    threading.Thread(target=kontrol_dinleyicisini_baslat, daemon=True).start()

    try:
        response = requests.get(STATIONS_URL, timeout=5)
        if response.status_code == 200:
            stations = response.json()
            for st in stations:
                threading.Thread(target=istasyon_simulasyonu, args=(st,), daemon=True).start()
        else:
            print("[-] İstasyonlar çekilemedi, varsayılan istasyonlar başlatılıyor...")
    except Exception as e:
        print(f"[-] Backend bağlantı hatası: {e}")

    while True:
        time.sleep(1)

if __name__ == "__main__":
    main()