package com.group5.ems.repository;

import com.group5.ems.entity.SystemSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingsRepository extends JpaRepository<SystemSettings, Long> {

    Optional<SystemSettings> findBySettingKey(String settingKey);
}

