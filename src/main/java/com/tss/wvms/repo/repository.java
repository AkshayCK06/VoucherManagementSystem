package com.tss.wvms.repo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tss.wvms.model.configEntity;



@Repository

public interface repository extends JpaRepository<configEntity, String> {

    configEntity findByConfigKey(String configKey);
    
}

