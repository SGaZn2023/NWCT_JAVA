package com.example.main.obj;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Session {
    private static final byte CMD_HANDSHAKE = 0x01;

    private String clientId;    // 不得超过 36 个字节
    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;

    public Session(Socket socket) throws IOException {
        this.socket = socket;
        this.outputStream = socket.getOutputStream();
        this.inputStream = socket.getInputStream();
        this.clientId = decodeClientId();
    }

    public String getClientId() {
        return clientId;
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
//        System.out.println(3);
        byte[] hdr = new byte[40];
        DataInputStream reader = new DataInputStream(this.inputStream);
        reader.readFully(hdr);
//        System.out.println(4);

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

    public Socket getSocket() {
        return socket;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void close() throws IOException {
        this.socket.close();
    }

    public synchronized void send(byte[] bytes) {
        try {
            this.socket.setSoTimeout(3000);
            this.socket.getOutputStream().write(bytes);
            this.socket.getOutputStream().flush();
            this.socket.setSoTimeout(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized byte[] receive() {
        try {
            byte[] buffer = new byte[1024];
            int bytesRead = this.inputStream.read(buffer);
            return Arrays.copyOf(buffer, bytesRead);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public synchronized void sendMessage(String message) throws Exception {
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(this.outputStream));
        bufferedWriter.write(message);
        bufferedWriter.newLine();
        bufferedWriter.write("#EELSEYLJNWCTMESSAGEISEND#");
        bufferedWriter.newLine();
        bufferedWriter.flush();
    }

    public synchronized String receiveMessage() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.inputStream));
        StringBuilder result = new StringBuilder();
        String message = bufferedReader.readLine();
        while (!message.equals("#EELSEYLJNWCTMESSAGEISEND#")) {
            result.append(message);
            message = bufferedReader.readLine();
        }
        return result.toString();
    }

}
