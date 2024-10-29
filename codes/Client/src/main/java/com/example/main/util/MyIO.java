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
}
