package com.vpn.vpn_conf.repository;

import com.vpn.vpn_conf.entity.VpnConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VpnConfigRepository extends JpaRepository<VpnConfig,Long> {
    Optional<VpnConfig> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Optional<VpnConfig> findByUuid(String uuid);

}
