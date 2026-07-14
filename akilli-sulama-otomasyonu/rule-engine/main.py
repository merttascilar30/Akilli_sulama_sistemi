from fastapi import FastAPI
from pydantic import BaseModel
import uvicorn

app = FastAPI()

# İstasyonların anlık durumunu bellekte tutan sözlük yapısı
vana_durumlari = {}

class KuralIstek(BaseModel):
    istasyonId: str
    anlikNem: float
    tarlaKapasitesi: float
    solmaNoktasi: float
    et0: float
    kc: float

@app.post("/api/rules/evaluate")
async def evaluate_rule(istek: KuralIstek):
    # 1. Bitki günlük gerçek su tüketimi hesaplanır
    etc = istek.et0 * istek.kc
    
    # 2. Günlük su tüketimine bağlı dinamik faktör sınırlandırılır
    k_dinamik = min(0.70, 0.50 + (0.02 * etc))
    
    # 3. Dinamik sulama tetikleme eşiği hesaplanır (Hatalı solma_noktasi alanları solmaNoktasi yapıldı)
    esik_nemi = istek.solmaNoktasi + k_dinamik * (istek.tarlaKapasitesi - istek.solmaNoktasi)
    
    # İstasyonun bellekteki mevcut vana durumunu oku (Varsayılan: False/KAPALI)
    mevcut_durum = vana_durumlari.get(istek.istasyonId, False)
    yeni_durum = mevcut_durum

    # Histerezis (Ölü Bant) Mantığı
    if not mevcut_durum:
        # Vana KAPALI ise: Nem dinamik eşik değerinin altına indiğinde vana açılır
        if istek.anlikNem <= esik_nemi:
            yeni_durum = True
    else:
        # Vana AÇIK ise: Nem Tarla Kapasitesine ulaşana kadar vana açık kalır
        if istek.anlikNem >= istek.tarlaKapasitesi:
            yeni_durum = False
            
    # Yeni durumu hafızada güncelle
    vana_durumlari[istek.istasyonId] = yeni_durum

    return {
        "istasyonId": istek.istasyonId,
        "anlikNem": istek.anlikNem,
        "esikNemi": round(esik_nemi, 2),
        "etc": round(etc, 2),
        "sulamaGerekli": yeni_durum
    }

if __name__ == "__main__":
    uvicorn.run("main:app", host="127.0.0.1", port=8000, workers=1)