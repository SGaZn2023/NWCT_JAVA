package com.example.main.obj;

import java.net.Socket;

public class Session {
    private Socket messageSocket;
    private Socket heartbeatSocket;

    public Socket getHeartbeatSocket() {
        return heartbeatSocket;
    }

    public void setHeartbeatSocket(Socket heartbeatSocket) {
        this.heartbeatSocket = heartbeatSocket;
    }

    public Socket getMessageSocket() {
        return messageSocket;
    }

    public void setMessageSocket(Socket messageSocket) {
        this.messageSocket = messageSocket;
    }
}
