package com.example.backend.repository;

import com.example.backend.entity.VanaLog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * VanaLog entity'si icin veritabani erisim katmani.
 */
public interface VanaLogRepository extends JpaRepository<VanaLog, Long> {
}
