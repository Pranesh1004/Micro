package com.genc.arfoms.loyalty.repository;

import com.genc.arfoms.loyalty.model.FrequentFlyer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrequentFlyerRepository extends JpaRepository<FrequentFlyer, Long> {
}

