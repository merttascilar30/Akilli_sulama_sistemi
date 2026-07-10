package com.example.backend.service;

import com.example.backend.dto.SensorMetrikRequestDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Asenkron ucundan gelen yuksek frekansli sensor verilerini gecici olarak tutan,
 * thread-safe kuyruk yapisi. Ana thread'i bloklamadan hizli sekilde veri kabul eder.
 */
@Component
public class SensorMetrikKuyrugu {

    private final ConcurrentLinkedQueue<SensorMetrikRequestDto> kuyruk = new ConcurrentLinkedQueue<>();

    public void ekle(SensorMetrikRequestDto veri) {
        kuyruk.offer(veri);
    }

    /**
     * Kuyruktaki tum verileri atomik olmayan ama thread-safe sekilde bosaltir ve dondurur.
     * Zamanlanmis (scheduled) toplu yazma isleminin her tetiklenmesinde cagrilir.
     */
    public List<SensorMetrikRequestDto> tumunuBosalt() {
        List<SensorMetrikRequestDto> bosaltilanVeriler = new ArrayList<>();
        SensorMetrikRequestDto veri;
        while ((veri = kuyruk.poll()) != null) {
            bosaltilanVeriler.add(veri);
        }
        return bosaltilanVeriler;
    }

    public int mevcutBoyut() {
        return kuyruk.size();
    }
}
