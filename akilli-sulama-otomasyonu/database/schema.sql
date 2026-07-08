CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE istasyonlar (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ad VARCHAR(100) NOT NULL,
    koordinat VARCHAR(100),
    tarla_kapasitesi DECIMAL(5,2),
    solma_noktasi DECIMAL(5,2),
    bitki_katsayisi DECIMAL(3,2)
);

CREATE TABLE sensor_metrikleri (
    id BIGSERIAL PRIMARY KEY,
    istasyon_id UUID REFERENCES istasyonlar(id) ON DELETE CASCADE,
    nem DECIMAL(5,2),
    sicaklik DECIMAL(5,2),
    kayit_tarihi TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE vana_loglari (
    id BIGSERIAL PRIMARY KEY,
    istasyon_id UUID REFERENCES istasyonlar(id) ON DELETE CASCADE,
    durum INT,
    tetikleme_tipi VARCHAR(50),
    tarih TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sensor_metrikleri_istasyon_tarih ON sensor_metrikleri (istasyon_id, kayit_tarihi);