package com.vpn.vpn_conf.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vpn_configs")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VpnConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // userId из AuthService — не храним User объект!
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    // UUID пользователя в XRay конфиге
    @Column(nullable = false, unique = true)
    private String uuid;

    // Готовая vless:// ссылка для Android
    @Column(name = "vless_link", nullable = false, length = 1000)
    private String vlLessLink;

    @Column(nullable = false)
    @Builder.Default
    private Boolean vlLess = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

}
