# Otonom Sulama Karar Destek Sistemi: Kural Motoru Mantıksal Tasarımı ve Histerezis Modeli 

## 1. Giriş ve Tarımsal Matematiksel Model 
Geleneksel akıllı sulama sistemleri, genellikle statik nem eşik değerlerine göre çalışmaktadır. Ancak bu yaklaşım; değişen hava şartlarını, buharlaşma oranlarını ve bitkinin gelişim dönemine bağlı su ihtiyacını göz ardı ettiği için su kaynaklarının israfına ya da bitki stresine yol açmaktadır.

Bu projede, Çukurova bölgesinin dinamik iklim ve toprak yapısı dikkate alınarak, dinamik Toprak Su Gerilimi ve Kullanılabilir Su Kapasitesi modeli kurgulanmıştır. Modelin temel yapı taşlarını Tarla Kapasitesi (theta_TK = %40) ve Daimi Solma Noktası (theta_SN = %20) parametreleri oluşturmaktadır.

### 1.1. Dinamik Bitki Tüketimi (Evapotranspirasyon) Formülasyonu 
Bitkinin günlük gerçek su tüketimi (ETc), Günlük Referans Evapotranspirasyon (ET0) değeri ile ilgili bitkinin fenolojik dönemine ait Bitki Katsayısının (Kc) çarpımı ile hesaplanır:

ETc = ET0 * Kc

### 1.2. Dinamik Sulama Tetikleme Eşik Modeli 
Sulama kararının verileceği anlık nem eşik değeri (theta_esik), daimi solma noktasının üzerine eklenen ve günlük su tüketim hızıyla dinamik olarak ölçeklenen ampirik bir katsayı vasıtasıyla şu şekilde formüle edilmiştir:

theta_esik = theta_SN + min(0.70, 0.50 + (0.02 * ETc)) * (theta_TK - theta_SN)

Bu formülasyona göre, aşırı sıcak ve buharlaşmanın yüksek olduğu günlerde (ETc yükseldiğinde), dinamik çarpan min fonksiyonunun üst sınırı olan 0.70 değerine yaklaşır. Böylece sistem, bitki henüz strese girmeden sulama eşiğini otomatik olarak yukarı çeker (theta_esik yaklaşık %34). Nispeten serin günlerde ise eşik değeri otomatik olarak taban seviyeye (theta_esik yaklaşık %30) indirgenerek optimize edilir.

---

## 2. Histerezis (Ölü Bant) Mantığı ve Kararlılık Analizi 
Yüksek frekanslı zaman serisi verilerinde, sensör gürültüleri veya toprak yüzeyindeki anlık nem dalgalanmaları nedeniyle nem değeri belirlenen eşik çizgisinin etrafında salınım yapabilir. Eğer sisteme doğrudan bir kontrol döngüsü bağlanırsa, nemin %29.9 ile %30.1 arasında gidip geldiği durumlarda akıllı vana saniyede defalarca açılıp kapanacak, bu da fiziksel röle ve vanaların ısınarak kısa sürede dejenere olmasına (chattering problemi) yol açacaktır.

Bu mühendislik problemini çözmek amacıyla sisteme Histerezis algoritması entegre edilmiştir. Sistem anlık durum bilgisini bellekte tutarak kararlarını sadece anlık değere göre değil, vananın o anki açık/kapalı pozisyonuna göre verir.

### 2.1. Durum Geçiş Mantığı (State Machine)

Sistem iki temel duruma göre karar mekanizmasını işletir:

1. Durum 1: Vana KAPALI ise;
   * Tetikleme Şartı: Toprak anlık nem değeri, hesaplanan dinamik eşik değerinin altına indiğinde vana açılır.
   * Matematiksel İfade: theta_anlik <= theta_esik ise Vana = 1 (AÇIK).

2. Durum 2: Vana AÇIK ise;
   * Tetikleme Şartı: Vana, nem değeri dinamik eşiğin üzerine çıktığı an kapatılmaz. Toprağın suya doygunluğa ulaşması ve kök bölgesinin tamamen beslenmesi için nem değerinin üst sınıra, yani Tarla Kapasitesine (theta_TK) ulaşması beklenir.
   * Matematiksel İfade: theta_anlik >= theta_TK ise Vana = 0 (KAPALI).

Bu sayede vana bir kez açıldığında, toprak tam anlamıyla suya doyana kadar açık kalır; böylece sistemin gereksiz yere kısa aralıklarla tetiklenmesi ve hidrolik koç darbesi etkileri tamamen engellenmiş olur.

---

## 3. Algoritma Akış Şeması (Pseudocode)

BAŞLA
    HER YENİ METRİK PAKETİ GELDİĞİNDE:
        ETc = ET0 * Kc
        k_dinamik = MIN(0.70, 0.50 + (0.02 * ETc))
        theta_esik = theta_SN + k_dinamik * (theta_TK - theta_SN)
        
        EĞER vana_durumu == KAPALI İSE:
            EĞER theta_anlik <= theta_esik İSE:
                vana_durumu = AÇIK
                Vana_Loglari_Tablosuna_Yaz(DURUM=1, TETİKLEYİCİ=OTONOM)
        
        EĞER vana_durumu == AÇIK İSE:
            EĞER theta_anlik >= theta_TK İSE:
                vana_durumu = KAPALI
                Vana_Loglari_Tablosuna_Yaz(DURUM=0, TETİKLEYİCİ=OTONOM)
SON