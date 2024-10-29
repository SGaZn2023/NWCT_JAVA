package com.example.main.obj;

import java.net.Socket;

public class Session {
    private Socket messageSocket;
    private Socket heartbeatSocket;

    public synchronized Socket getHeartbeatSocket() {
        return heartbeatSocket;
    }

    public synchronized void setHeartbeatSocket(Socket heartbeatSocket) {
        this.heartbeatSocket = heartbeatSocket;
    }

    public synchronized Socket getMessageSocket() {
        return messageSocket;
    }

    public synchronized void setMessageSocket(Socket messageSocket) {
        this.messageSocket = messageSocket;
    }
}
