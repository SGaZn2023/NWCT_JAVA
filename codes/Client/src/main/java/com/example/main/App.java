package com.example.main;

import com.example.main.domain.Client;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) {
        // 配置文件读取
        if (args.length < 1) return;
        Yaml yaml = new Yaml();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            FileInputStream inputStream = new FileInputStream(args[0]);
            Map<String, Object> obj = yaml.load(inputStream);
            inputStream.close();
            File file = new File(obj.get("config").toString());
            Map data = objectMapper.readValue(file, Map.class);
            String clientId = (String) data.get("client_id");
            String publicIp = (String) data.get("public_ip");
            int connectPort = (int) data.get("connect_port");
            Client client = new Client(clientId, publicIp, connectPort);
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
