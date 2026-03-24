package com.vpn.vpn_conf.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Service
@Slf4j
public class SshService {

    @Value("${vps.host}")
    private String vpsHost;

    @Value("${vps.user:root}")
    private String vpsUser;

    @Value("${vps.port:22}")
    private int sshPort;

    @Value("${vps.key-path}")
    private String keyPath;

    @Value("${vps.xray-config:/usr/local/etc/xray/config.json}")
    private String xrayConfigPath;

    private final ObjectMapper objectMapper;

    public SshService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void addUserToXray(String uuid) {
        executeCommand(String.format(
                "python3 -c \"\n" +
                        "import json \n" +
                        "with open ('%s', 'r') as f: config = json.load(f)\n" +
                        "for inbound in config.get('inbounds', []):\n" +
                        "   if inbound.get('protocol') == 'vless':\n" +
                        "       inbound['settings']['clients'].append({'id': '%s', 'flow': 'xtls-rprx-vision'})\n" +
                        "with open('%s', 'w') as f: json.dump(config, f, indent=2)\n" +
                        "\"",xrayConfigPath, uuid, xrayConfigPath
        ));

        executeCommand("systemctl restart xray");
        log.info("UUID {} добавлен в XRay", uuid);
    }

    public void removeUserFromXray(String uuid) {
        executeCommand(String.format(
                "python3 -c \"\n" +
                        "import json\n" +
                        "with open('%s', 'r') as f: config = json.load(f)\n" +
                        "for inbound in config.get('inbounds', []):\n" +
                        "    if inbound.get('protocol') == 'vless':\n" +
                        "        inbound['settings']['clients'] = " +
                        "            [c for c in inbound['settings']['clients'] if c['id'] != '%s']\n" +
                        "with open('%s', 'w') as f: json.dump(config, f, indent=2)\n" +
                        "\"",
                xrayConfigPath, uuid, xrayConfigPath
        ));

        executeCommand("systemctl restart xray");
        log.info("UUID {} удалён из XRay", uuid);
    }

    private void executeCommand(String command) {
        JSch jSch = new JSch();
        Session session = null;
        ChannelExec channel =  null;

        try{
            jSch.addIdentity(keyPath);
            session = jSch.getSession(vpsUser, vpsHost, sshPort);

            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(30000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);

            InputStream in = channel.getInputStream();
            InputStream err = channel.getErrStream();
            channel.connect();

            String output = new String(in.readAllBytes());
            String error = new String(err.readAllBytes());


            if (!output.isBlank()) log.debug("SSH output: {}", output.trim());
            if (!error.isBlank()) log.warn("SSH stderr: {}", error.trim());

            log.info("✅ SSH команда выполнена успешно");
        } catch (JSchException | IOException e) {
            log.error("❌ SSH ошибка: {}", e.getMessage(), e);
            throw new RuntimeException("SSH команда не выполнена: " + e.getMessage(), e);
        } finally {
            if (channel != null) channel.disconnect();
            if (session != null) session.disconnect();
        }
    }
}
