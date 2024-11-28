package com.example.main.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    public static void copyWithDecrypt(InputStream inputStream, OutputStream outputStream, byte[] key) throws IOException {
        byte[] buffer = new byte[cacheSize * 3];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            System.out.println("数据解密中");
//            byte[] encryptMessage = Arrays.copyOfRange(buffer, 0, bytesRead);
            byte[] encryptMessage = new byte[bytesRead];
            System.arraycopy(buffer, 0, encryptMessage, 0, bytesRead);
            System.out.println("复制加密数据");
            byte[] decryptMessage = DataEncryptUtil.decrypt(encryptMessage, key);
//            outputStream.write(buffer, 0, bytesRead);
            System.out.println("数据已解密");
            outputStream.write(decryptMessage, 0, decryptMessage.length);
            outputStream.flush();
        }
    }

    public static void copyWithEncrypt(InputStream inputStream, OutputStream outputStream, byte[] key, byte[] iv) throws IOException {
        byte[] buffer = new byte[cacheSize];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            System.out.println("数据加密中");
            byte[] encryptMessage = DataEncryptUtil.encrypt(Arrays.copyOfRange(buffer, 0, bytesRead), key, iv);
            System.out.println("加密数据已发送");
            outputStream.write(encryptMessage, 0, encryptMessage.length);
            outputStream.flush();
        }
    }
}
