package com.vpn.vpn_conf.service.vpnService;

import com.vpn.vpn_conf.dto.VpnConfigResponse;
import com.vpn.vpn_conf.entity.VpnConfig;
import com.vpn.vpn_conf.exception.VpnException;
import com.vpn.vpn_conf.repository.VpnConfigRepository;
import com.vpn.vpn_conf.service.SshService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VpnServiceImpl implements VpnService {

    private final VpnConfigRepository vpnConfigRepository;
    private final SshService sshService;

    @Value("${vps.host}")
    private String host;

    @Value("${vps,xray-port:443}")
    private int port;

    @Value("${vps.public-key}")
    private String serverPublicKey;

    @Value("${vps.short-id}")
    private String shortId;

    @Override
    @Transactional
    public void createVpnConfig(Long userId) {
        log.info("Создаём VPN конфиг для userId={}", userId);

        if(vpnConfigRepository.existsByUserId(userId)) {
            vpnConfigRepository.findByUserId(userId).ifPresent(vpnConfig -> {
                vpnConfig.setActive(true);
                vpnConfigRepository.save(vpnConfig);
                sshService.addUserToXray(vpnConfig.getUuid());
                log.info("Конфиг переактивирован для userId={}", userId);
            });
            return;
        }

        // Генерируем новый UUID для XRay
        String uuid =  UUID.randomUUID().toString();

        // Добавляем пользователя в XRay на VPS
        sshService.addUserToXray(uuid);

        // Генерируем vless:// ссылку
        String vlessLink = buildVlessLink(uuid);

        VpnConfig config = VpnConfig.builder()
                .userId(userId)
                .uuid(uuid)
                .vlessLink(vlessLink)
                .active(true)
                .build();

        vpnConfigRepository.save(config);
        log.info("VPN конфиг создан для userId={}", userId);
    }

    @Override
    @Transactional
    public void deactivateVpnConfig(Long userId) {
        log.info("🔒 Деактивируем конфиг для userId={}", userId);

        vpnConfigRepository.findByUserId(userId).ifPresent(config -> {
            // Удаляем из XRay
            sshService.removeUserFromXray(config.getUuid());

            // Деактивируем в БД
            config.setActive(false);
            vpnConfigRepository.save(config);
            log.info("✅ Конфиг деактивирован для userId={}", userId);
        });
    }


    public VpnConfigResponse getVpnConfig(Long userId) {
        VpnConfig config = vpnConfigRepository.findByUserId(userId)
                .orElseThrow(() -> new VpnException(
                        "VPN конфиг не найден для userId=" + userId
                ));

        return VpnConfigResponse.builder()
                .userId(userId)
                .vlessLink(config.getVlessLink())
                .active(config.getActive())
                .createdAt(config.getCreatedAt())
                .build();
    }

    private String buildVlessLink(String uuid) {
        return String.format(
                "vless://%s@%s:%d" +
                        "?security=reality" +
                        "&encryption=none" +
                        "&pbk=%s" +
                        "&headerType=none" +
                        "&fp=chrome" +
                        "&type=tcp" +
                        "&flow=xtls-rprx-vision" +
                        "&sni=www.microsoft.com" +
                        "&sid=%s" +
                        "#Shade-VPN",
                uuid, host, port,
                serverPublicKey, shortId
        );
    }
}
