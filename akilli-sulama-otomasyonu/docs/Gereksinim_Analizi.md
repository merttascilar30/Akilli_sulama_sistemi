# Otonom Akıllı Sulama Karar Destek Sistemi
## Sistem Gereksinim Analizi ve Mimari Tasarım Spesifikasyonu

## 1. Ele Alınacak Mühendislik Problemi ve Kapsam

Endüstriyel tarım otomasyonlarında, sahadaki uç cihazlardan (Edge Devices - ESP32 vb.) gelen yüksek frekanslı zaman serisi (time-series) metriklerinin işlenmesi kritik bir mimari problemdir. Bu verilerin geleneksel senkron metotlarla doğrudan ilişkisel veri tabanına yazılması, disk üzerinde I/O darboğazlarına (bottleneck) ve sistem kilitlenmelerine yol açar. Ayrıca sektördeki naif "if-else" tabanlı düz kontrol mekanizmaları; toprak yapısını, buharlaşma parametrelerini ve sensör gürültülerini göz ardı ettiği için vanalarda sürekli salınıma (oscillation) ve mekanik aşınmaya sebep olmaktadır.

Bu projenin kapsamı; saniyede en az 200 adet veri paketini (>= 200 msg/s) uçtan uca 150 ms altı gecikmeyle kararlı bir şekilde işleyebilen asenkron kuyruk tabanlı bir Java Spring Boot backend mimarisi kurmaktır. Toplanan veriler, Adana/Çukurova bölgesinin toprak yapısına uygun matematiksel modelleme kullanan bağımsız bir Python Kural Motoru (Rule Engine) mimarisiyle işlenerek otonom sulama kararlarına dönüştürülecektir.

---

## 2. Fonksiyonel Gereksinimler (Functional Requirements)

* **FR-1 [İstasyon Topolojisi Yönetimi]:** Sistem, sahada yer alan akıllı vana ve sensör istasyonlarını ilişkisel veritabanı üzerinde dinamik olarak tanımlayabilmeli, güncelleyebilmeli ve tekil (UUID) olarak izleyebilmelidir.
* **FR-2 [Asenkron Veri Toplama Hattı]:** Donanım katmanından gelen yüksek frekanslı veri paketleri, ana thread havuzunu bloke etmeyecek şekilde thread-safe bir kuyruk yapısına (Inbound Queue) alınmalı ve arka planda asenkron işlenmelidir.
* **FR-3 [Gevşek Bağlı Kural Motoru Entegrasyonu]:** Veri toplama katmanı ile karar mekanizması, mikroservis ilkelerine uygun olarak REST API mimari sınırı üzerinden gevşek bağlı (loosely coupled) şekilde haberleşmelidir.
* **FR-4 [Histerezis Tabanlı Otonom Kontrol]:** Karar motoru, kritik nem sınırında otomatik olarak vana açma emri tetiklemeli; ancak nem eşik değerin üzerine çıktığı an vanayı kapatmak yerine, toprağın suya doymasını bekleyen bir histerezis (ölü bant) algoritması çalıştırmalıdır.
* **FR-5 [Gerçek Zamanlı Veri Yayını]:** Veri tabanına işlenen güncel metrikler ve vana durum değişiklikleri, istemci tarafındaki panellere WebSocket (STOMP) protokolü üzerinden anlık olarak fırlatılmalıdır (Broadcast).
* **FR-6 [Çift Yönlü Manuel Override]:** Operatör paneli üzerinden otonom mod kapatılarak sahaya anlık manuel vana kapatma/açma sinyalleri çift yönlü hat üzerinden iletilebilmelidir.

---

## 3. Fonksiyonel Olmayan Gereksinimler (Non-Functional Requirements)

* **NFR-1 [Ölçeklenebilirlik ve Yazma Hızı]:** Veri hattı, en yoğun anlarda bile paket düşürmeden saniyede minimum 200 mesaj işleme kapasitesine sahip olmalıdır.
* **NFR-2 [Uçtan Uca Maksimum Gecikme]:** Bir sensör verisinin simüle edildiği andan itibaren işlenip, veri tabanına yazılması ve WebSocket ile arayüze yansıması arasındaki toplam süre 150 ms'yi geçmemelidir.
* **NFR-3 [Veri Tabanı Mimarisi ve Optimizasyon]:** Sistemde veri tutarlılığını (ACID) korumak ve yönetimsel tablolarla ilişkisel bütünlüğü sağlamak adına PostgreSQL tercih edilmiştir. Yoğun yazma yükü; asenkron toplu (batch) yazma ve zaman serisi sorgularını hızlandıracak B-Tree bileşik indeksleme (istasyon_id, kayit_tarihi) teknikleriyle optimize edilecektir.
* **NFR-4 [Hata Toleransı (Fault Tolerance)]:** Sensör kopmaları, geçersiz veri paketleri veya ağ kesintileri durumunda sistemin çökmesini engelleyecek Global Exception Handling ve merkezi loglama mimarisi entegre edilecektir.

---

## 4. Matematiksel Model ve Karar Mantığı

Sistem, Çukurova bölgesinin killi-tınlı toprak parametrelerine ve anlık çevresel su kaybı faktörlerine göre kalibre edilmiş **Dinamik Toprak Su Gerilimi** modelini temel alır.

### Toprak ve Çevresel Değişkenler
* **Tarla Kapasitesi (θ_TK):** Toprağın tutabileceği maksimum yararlı su yüzdesidir (Referans: %40).
* **Daimi Solma Noktası (θ_SN):** Bitkinin kurumaya başladığı kritik alt sınır yüzdesidir (Referans: %20).
* **Günlük Referans Evapotranspirasyon (ET0):** Bölgedeki atmosferik koşullara bağlı günlük buharlaşma ve terleme su kaybı miktarıdır (mm/gün).
* **Bitki Katsayısı (Kc):** İstasyona ekili bitkinin türüne ve gelişim evresine ait terleme faktörüdür.

### Dinamik Algoritmik Tetikleme Kriteri
Sistem, sulama eşiğini sabit bir değerde tutmak yerine, bitki günlük su tüketim katsayısını (ETc = ET0 * Kc) hesaplayarak tetikleme nemini dinamik olarak belirler. Çevresel yük arttıkça sistem bitkinin strese girmesini önlemek adına sulama başlatma nem sınırını yukarı çeker:

* **Bitki Toplam Su Tüketimi (ETc):** ETc = ET0 * Kc
* **Dinamik Tetikleme Eşiği (θ_esik):** θ_esik = θ_SN + K_dinamik * (θ_TK - θ_SN)
* **Dinamik Faktör (K_dinamik):** K_dinamik = min(0.70, 0.50 + (Alpha * ETc)) *(Duyarlılık Katsayısı Alpha = 0.02)*

**Karar Mekanizması:** θ_anlik <= θ_esik ise **Sulama Gerekli** kararı üretilir.

#### Sayısal Doğrulama ve Senaryolar:
* **Senaryo A (Serin/Nemli Gün - Düşük Tüketim):** ET0 = 2 mm/gün, Kc = 0.5 (Erken evre). ETc = 1 mm/gün olur. K_dinamik = 0.50 + (0.02 * 1) = 0.52 olarak hesaplanır. Eşik nem değeri: %20 + 0.52 * (%40 - %20) = %30.4. Sistem nem %30.4'e düşene kadar bekler.
* **Senaryo B (Sıcak/Kurak Gün - Yüksek Tüketim):** ET0 = 7 mm/gün, Kc = 0.85 (Gelişmiş evre). ETc = 5.95 mm/gün olur. K_dinamik = 0.50 + (0.02 * 5.95) = 0.62 olarak hesaplanır. Eşik nem değeri: %20 + 0.62 * (%40 - %20) = %32.4. Sistem risk almaz ve nem daha yüksekken (%32.4) sulamayı başlatır.

### Salınım Önleme (Histerezis)
Nem dinamik eşik değerinin (θ_esik) üzerine çıktığı an vananın anlık gürültüler sebebiyle sürekli açılıp kapanmasını (salınım) engellemek amacıyla, nem kararlı bir şekilde üst sınıra (Tarla Kapasitesi θ_TK değerine) ulaşana kadar sulama işlemi kesintisiz olarak sürdürülür.