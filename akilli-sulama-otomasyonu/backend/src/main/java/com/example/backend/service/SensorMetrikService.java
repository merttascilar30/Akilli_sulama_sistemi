package com.example.backend.service;

import com.example.backend.dto.SensorMetrikRequestDto;

/**
 * Sensor metriklerinin yazilmasiyla ilgili is mantigini tanimlayan servis arayuzu.
 */
public interface SensorMetrikService {

    /**
     * Naif/baseline senaryo: veriyi dogrudan veritabanina yazar, ana thread'i bloklar.
     */
    void senkronKaydet(SensorMetrikRequestDto sensorMetrikRequestDto);

    /**
     * Yuksek performansli senaryo: veriyi veritabanina yazmadan kuyruga ekler,
     * gercek yazma islemi arka plandaki zamanlanmis toplu yazici tarafindan yapilir.
     */
    void kuyrugaEkle(SensorMetrikRequestDto sensorMetrikRequestDto);
}
