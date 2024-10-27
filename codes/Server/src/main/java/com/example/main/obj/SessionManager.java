package com.example.main.obj;

import com.example.main.exception.SessionNotFoundException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private final Map<String, Session> sessions = new HashMap<>();

    public synchronized Map<String, Session> getSessions() {
        return this.sessions;
    }

    public synchronized Session getSessionByClientId(String clientId) throws SessionNotFoundException {
        Session session = sessions.get(clientId);
        if (session == null) {
            throw new SessionNotFoundException("Client ID 错误: " + clientId);
        }
        return session;
    }

    public synchronized void createSession(ServerSocket ss, Socket messageSocket, Socket heartbeatSocket) throws IOException {
        Session session = new Session(ss, messageSocket, heartbeatSocket);
        String clientId = session.getClientId();
        if (sessions.containsKey(clientId)) {
            // 检查已存在的session能否联通
            Socket hb = sessions.get(clientId).getHeartbeatSocket();
            boolean canConnect = false;
            try {
                hb.getOutputStream().write(1);
                canConnect = true;
            } catch (IOException e) {
                // 如果不能联通，则关闭已存在的session
                sessions.get(clientId).close();
                sessions.remove(clientId);
            }
            if (canConnect) {
                throw new IOException("此 clientId 重复: " + clientId);
            }
        }
        sessions.put(clientId, session);
        System.out.println(clientId + " 连接成功");
    }

    public synchronized void closeSession(String clientId) throws IOException {
        Session session = sessions.get(clientId);
        session.close();
        sessions.remove(clientId);
    }

    public synchronized void closeAllSessions() {
        for (Session session : sessions.values()) {
            try {
                session.close();
            } catch (Exception e) {}
        }
        sessions.clear();
    }
}
