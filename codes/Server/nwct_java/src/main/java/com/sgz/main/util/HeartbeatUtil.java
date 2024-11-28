package com.sgz.main.util;


import com.sgz.main.obj.Session;
import com.sgz.main.obj.SessionManager;

import java.net.Socket;
import java.util.ArrayList;
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
                        Session session = sessions.get(clientId);
                        Socket socket = session.getHeartbeatSocket();
                        try {
//                            socket.sendUrgentData(1);
                            socket.getOutputStream().write(0);
                            System.out.println(clientId + " 在线");
//                            testChildSockets(session);
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

    public void testChildSockets(Session session) {
        ArrayList<Socket> sockets = session.getSocketList();
        if (sockets == null) return;
        for (Socket childSocket : sockets)
            try {
                childSocket.sendUrgentData(0);
            } catch (Exception e) {
                sockets.remove(childSocket);
            }
    }
}
