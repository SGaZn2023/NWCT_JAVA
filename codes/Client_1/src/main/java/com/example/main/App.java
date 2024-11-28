package com.example.main;

import com.example.main.domain.Client;
import com.example.main.obj.MyEncrypt;
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
        testInit();
    }
    public static void testInit() {
        try {
            MyEncrypt myEncrypt = new MyEncrypt(true, "7c8ce82c417155f4240b9944a56cc273984b1208cff16424106d23602293ef30", "ba4971380f93a690020f9e81");
            Client client = new Client("test_nwct", "127.0.0.1", 5103, myEncrypt);
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init(String[] args) {
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
            boolean isEncrypt = false;
            String secretKey = null;
            String IV = null;
            MyEncrypt myEncrypt = new MyEncrypt();
            try {
                isEncrypt = (boolean) data.get("is_encrypt");
                // bytesToHex 的结果
                secretKey = (String) data.get("secret_key");
                IV = (String) data.get("iv");
                myEncrypt.setIsEncrypt(isEncrypt);
                myEncrypt.setSecretKey(secretKey);
                myEncrypt.setIV(IV);
            }catch (Exception e) {}
            Client client = new Client(clientId, publicIp, connectPort, myEncrypt);
            client.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
