package com.example.main.util;

import com.example.main.obj.Session;
import com.example.main.obj.SessionManager;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public class HeartbeatUtil implements Runnable {

    private SessionManager sessionManager;


    public HeartbeatUtil(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Map<String, Session> sessions = sessionManager.getSessions();
                if (sessions != null) {
                    for (String clientId : sessions.keySet()) {

                        Socket socket = sessions.get(clientId).getSocket();
                        try {
                            socket.sendUrgentData(0);
                            System.out.println(clientId + " 在线");
                        } catch (Exception e) {
                            System.out.println(clientId + " 已离线");
                            sessionManager.closeSession(clientId);
                        }

                    }
                }
                Thread.sleep(5000);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
