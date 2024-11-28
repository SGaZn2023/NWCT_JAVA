package com.sgz.main.obj;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class Session {
    private static final byte CMD_HANDSHAKE = 0x01;
    private static final byte CMD_HEARTBEAT = 0x02;
    private static final String heartbeatMessage = "#EELSEYLJTHECONNECTEDISHEARTBEAT#"; // 33字符

//    private static final byte CMD_CONNECT = 0x02;
//
//    private static final String sureConnect = "#EELSEYLJSURETOCONNECT#";

    private String clientId;    // 不得超过 36 个字节
    private ServerSocket ss;
    private Socket messageSocket;  // 通信用 socket
    private Socket heartbeatSocket; // 心跳检测 socket
    private ArrayList<Socket> socketList; // 保存所有连接的 socket

    public Session(ServerSocket ss, Socket messageSocket, Socket heartbeatSocket) throws IOException {
        messageSocket.setSoTimeout(3000);
        this.ss = ss;
        this.messageSocket = messageSocket;
        this.clientId = decodeClientId();
        this.socketList = new ArrayList<>();
        decodeHeartbeat(heartbeatSocket);
        this.heartbeatSocket = heartbeatSocket;
    }

    public String getClientId() {
        return clientId;
    }

    public ArrayList<Socket> getSocketList() {
        return socketList;
    }

    public byte[] encodeClientId() throws JsonProcessingException {
        byte[] hdr = new byte[4];
        hdr[0] = 0x0;
        hdr[1] = CMD_HANDSHAKE;

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = objectMapper.writeValueAsBytes(clientId);

        ByteBuffer buffer = ByteBuffer.wrap(hdr);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(2, (short) body.length);

        if (hdr.length + body.length > 40) throw new RuntimeException("Client Id too long");
        byte[] result = new byte[40];
        System.arraycopy(hdr, 0, result, 0, hdr.length);
        System.arraycopy(body, 0, result, hdr.length, body.length);

        return result;
    }

    public String decodeClientId() throws IOException {
        return decodeClientId(this.messageSocket.getInputStream());
    }

    public static String decodeClientId(InputStream in) throws IOException {
        byte[] hdr = new byte[40];
        DataInputStream reader = new DataInputStream(in);
        reader.readFully(hdr);

        byte cmd = hdr[1];

        if (cmd != CMD_HANDSHAKE) {
            throw new IOException("Invalid command: " + cmd);
        }

        ByteBuffer buffer = ByteBuffer.wrap(hdr, 2, 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        short bodyLen = buffer.getShort();

        byte[] body = new byte[bodyLen];

        for (int i = 0; i < bodyLen; i++) {
            body[i] = hdr[4 + i];
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(body, String.class);
    }

    public static void decodeHeartbeat(Socket socket) throws IOException {
        byte[] hdr = new byte[40];
        DataInputStream reader = new DataInputStream(socket.getInputStream());
        reader.readFully(hdr);

        byte cmd = hdr[1];

        if (cmd != CMD_HEARTBEAT) {
            throw new IOException("Invalid command: " + cmd);
        }

        ByteBuffer buffer = ByteBuffer.wrap(hdr, 2, 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        short bodyLen = buffer.getShort();

        byte[] body = new byte[bodyLen];

        for (int i = 0; i < bodyLen; i++) {
            body[i] = hdr[4 + i];
        }

        String str = new String(body);
        if (!heartbeatMessage.equals(str))
            throw new IOException("Invalid heartbeat message: " + str);

//        ObjectMapper objectMapper = new ObjectMapper();
//        return objectMapper.readValue(body, String.class);
//        System.out.println("decode heartbeat success");
    }

    public Socket getMessageSocket() {
        return messageSocket;
    }

    public Socket getHeartbeatSocket() {
        return heartbeatSocket;
    }


    public void close() throws IOException {
        this.messageSocket.close();
        this.heartbeatSocket.close();
        if (this.socketList == null) return;
        for (Socket socket : this.socketList) {
            socket.close();
        }
    }

    public synchronized void send(byte[] bytes) {
        try {
            this.messageSocket.setSoTimeout(3000);
            this.messageSocket.getOutputStream().write(bytes);
            this.messageSocket.getOutputStream().flush();
            this.messageSocket.setSoTimeout(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized Socket getSocket(ProxyProtocol pProtocol) throws Exception {
//        System.out.println("startGetSocket");
        byte[] bytes = pProtocol.encode();

        this.messageSocket.setSoTimeout(3000);
        this.messageSocket.getOutputStream().write(bytes);
//        System.out.println("sendProxyProtocol");
//        this.messageSocket.setSoTimeout(0);

        Socket socket = this.ss.accept();
//        System.out.println("accept");

        decodeClientId(socket.getInputStream());
//        System.out.println("decodeClientId");

        this.socketList.add(socket);
//        System.out.println("gotten socket");
        return socket;
    }

}
