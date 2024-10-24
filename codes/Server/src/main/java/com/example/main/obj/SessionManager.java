package com.example.main.obj;

import com.example.main.exception.SessionNotFoundException;

import java.io.IOException;
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
            throw new SessionNotFoundException("Session not found for client ID: " + clientId);
        }
        return session;
    }

    public synchronized void createSession(Socket socket) throws IOException {
        Session session = new Session(socket);
        String clientId = session.getClientId();
        if (sessions.containsKey(clientId)) {
            throw new IOException("此 clientId 重复: " + clientId);
        }
        sessions.put(clientId, session);
        System.out.println(clientId + " 连接成功");
    }

    public synchronized void closeSession(String clientId) throws IOException {
        Session session = sessions.get(clientId);
        session.close();
        sessions.remove(clientId);
    }
}
