package com.example.main.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

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

    public static void ioCopyWithDecrypt(InputStream inputStream, OutputStream outputStream, byte[] key, byte[] iv) throws IOException {
        byte[] buffer = new byte[3072];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            System.out.println("数据解密中");
            byte[] encryptMessage = Arrays.copyOfRange(buffer, 0, bytesRead);
            byte[] decryptMessage = AESUtil.decrypt(encryptMessage, key, iv);
//            outputStream.write(buffer, 0, bytesRead);
            outputStream.write(decryptMessage, 0, bytesRead);
            outputStream.flush();
        }
    }

    public static void ioCopyWithEncrypt(InputStream inputStream, OutputStream outputStream, byte[] key, byte[] iv) throws IOException {
        byte[] buffer = new byte[3072];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            System.out.println("数据加密中");
            byte[] encryptMessage = AESUtil.encrypt(Arrays.copyOfRange(buffer, 0, bytesRead), key, iv);
            outputStream.write(encryptMessage, 0, bytesRead);
            outputStream.flush();
        }
    }
}
