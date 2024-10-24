package com.example.main.obj;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ProxyProtocol {
    // 不得大于 2k 字节

    public String clientId;
//    public String publicIp;
    public int publicPort;
    public String publicProtocol;
    public String internalIp;
    public int internalPort;
    public String internalProtocol;

    private static final byte CMD_PProtocol = 0x0;

    public byte[] encode() throws JsonProcessingException {
        byte[] hdr = new byte[4];
        hdr[0] = 0x0;
        hdr[1] = CMD_PProtocol;

        ObjectMapper objectMapper = new ObjectMapper();
        byte[] body = objectMapper.writeValueAsBytes(this);

        ByteBuffer buffer = ByteBuffer.wrap(hdr);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.putShort(2, (short) body.length);

        if (body.length > 2048) throw new RuntimeException("body too long");
        byte[] result = new byte[2048];
        System.arraycopy(hdr, 0, result, 0, hdr.length);
        System.arraycopy(body, 0, result, hdr.length, body.length);

        return result;
    }

    public static ProxyProtocol decode(InputStream inputStream) throws IOException {
        byte[] hdr = new byte[2048];
        DataInputStream reader = new DataInputStream(inputStream);
        reader.readFully(hdr);

        byte cmd = hdr[1];

        if (cmd != CMD_PProtocol) {
            throw new IOException("Invalid command: " + cmd);
        }

        ByteBuffer buffer = ByteBuffer.wrap(hdr, 2, 2);
        buffer.order(ByteOrder.BIG_ENDIAN);
        short bodyLen = buffer.getShort();

        byte[] body = new byte[bodyLen];

        for (int i = 0; i < bodyLen; i++) {
            body[i] = hdr[i + 4];
        }

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(body, ProxyProtocol.class);
    }

}
