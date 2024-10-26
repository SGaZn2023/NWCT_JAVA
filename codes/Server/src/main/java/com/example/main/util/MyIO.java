package com.example.main.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MyIO {
    private static final int cacheSize = 1024;
    private static final byte CMD_END = 0x08;
    private static final String endMessage = "#EELSEYLJTHECONNECTEDISEND#";

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[cacheSize];

        int bytesRead;
        while((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
    }

    public static void copyWithListenEnd(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[cacheSize];

        int bytesRead;
        while((bytesRead = in.read(buffer)) != -1) {
            if (bytesRead == 1024 && buffer[1] == CMD_END) {
                // 检测结束标志
                ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 2, 2);
                byteBuffer.order(ByteOrder.BIG_ENDIAN);
                short length = byteBuffer.getShort();
                byte[] endMessageBytes = endMessage.getBytes();
                if (length == endMessageBytes.length) {
                    byte[] message = new byte[length];
                    for (int i = 0; i < endMessageBytes.length; i++)
                        message[i] = buffer[i + 4];
                    String messageStr = new String(message);
                    if (endMessage.equals(messageStr)) {
                        break;
                    }
                }
            }
            // 传输数据
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
    }
}
