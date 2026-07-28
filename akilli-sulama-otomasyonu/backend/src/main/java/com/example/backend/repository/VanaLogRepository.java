package com.example.backend.repository;

import com.example.backend.entity.VanaLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * VanaLog entity'si icin veritabani erisim katmani.
 */
public interface VanaLogRepository extends JpaRepository<VanaLog, Long> {

    /**
     * Bir istasyon icin atilmis EN SON vana logunu getirir. OtonomVanaTetikleyici,
     * istasyonun gercek vana durumunu (MANUEL bir kapatma dahil) yerel bir
     * onbellege degil bu sorgunun sonucuna dayandirir.
     */
    Optional<VanaLog> findTopByIstasyonIdOrderByIdDesc(UUID istasyonId);
}
