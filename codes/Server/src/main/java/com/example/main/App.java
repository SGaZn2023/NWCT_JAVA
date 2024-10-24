package com.example.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.example.main.obj.Listener;
import com.example.main.obj.ProxyProtocol;
import com.example.main.obj.Server;
import com.example.main.obj.SessionManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        // 配置文件读取
        if (args.length < 1) return;
        Yaml yaml = new Yaml();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            FileInputStream inputStream = new FileInputStream(args[0]);
            Map<String, Object> obj = yaml.load(inputStream);
            inputStream.close();
            File file = new File(obj.get("config").toString());
            List data = objectMapper.readValue(file, List.class);
            // Session管理
            SessionManager sessionManager = new SessionManager();



            // 监听服务器端口
            for (Object o : data) {
                Map<String, Object> map = (Map<String, Object>) o;
                ProxyProtocol proxyProtocol = new ProxyProtocol();
                proxyProtocol.clientId = (String) map.get("client_id");
                proxyProtocol.publicPort = (int) map.get("public_port");
                proxyProtocol.publicProtocol = (String) map.get("public_protocol");
                proxyProtocol.internalIp = (String) map.get("internal_ip");
                proxyProtocol.internalPort = (int) map.get("internal_port");
                proxyProtocol.internalProtocol = (String) map.get("internal_protocol");
                Listener listener = new Listener(proxyProtocol, sessionManager);
//                listener.listenAndServer();
                new Thread(() -> {
                    try {
                        listener.listenAndServer();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).start();
            }
            // 创建与内网的连接
            Server server = new Server(5103, sessionManager);
            server.listenAndServer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
