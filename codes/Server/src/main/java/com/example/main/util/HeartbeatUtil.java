package com.example.main.util;

import com.example.main.obj.Session;
import com.example.main.obj.SessionManager;

import java.net.Socket;
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
                        if (sessions.get(clientId).getSocket().isClosed()) {
                            System.out.println(clientId + " is offline");
                            sessionManager.closeSession(clientId);
                        } else {
                            System.out.println(clientId + " is online");
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
