package com.homebudget.repository;

import com.homebudget.model.EventSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventSettingRepository extends JpaRepository<EventSetting, Long> {

    List<EventSetting> findBySettingKey(String settingKey);
}
