package com.example.main.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

public class DataEncryptUtil {
//    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String ENCRYPT_ALGO = "ChaCha20-Poly1305";

    private static final int IV_LENGTH = 12;

    /*public static byte[] encrypt(byte[] plaintextBytes, byte[] keyBytes, byte[] ivBytes) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            return cipher.doFinal(plaintextBytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }*/
    public static byte[] encrypt(byte[] messageBytes, byte[] keyBytes, byte[] ivBytes) {
        try {
            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);

            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            SecretKeySpec key = new SecretKeySpec(keyBytes, "ChaCha20");

            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            byte[] encryptedText = cipher.doFinal(messageBytes);

            // 将 iv 添加到数据末尾处
            byte[] encrypted = ByteBuffer.allocate(encryptedText.length + IV_LENGTH)
                    .put(encryptedText)
                    .put(ivBytes)
                    .array();

            byte[] encryptedBase64 = Base64.getEncoder().encode(encrypted);

            return encryptedBase64;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /*public static byte[] decrypt(byte[] encrypted, byte[] keyBytes, byte[] ivBytes) {
        System.out.println("数据开始解密");
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            System.out.println(1);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            System.out.println(2);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            System.out.println(3);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            System.out.println(4);
            byte[] decrypted = cipher.doFinal(encrypted);
            System.out.println("数据解密完成");
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }*/
    public static synchronized byte[] decrypt(byte[] encryptedBase64, byte[] keyBytes) {
        System.out.println("数据开始解密");
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);
        try {
            ByteBuffer bb = ByteBuffer.wrap(encrypted);
            System.out.println(1);

            // split cText to get the appended nonce
            byte[] encryptedText = new byte[encrypted.length - IV_LENGTH];
            byte[] ivBytes = new byte[IV_LENGTH];
            bb.get(encryptedText);
            bb.get(ivBytes);
            System.out.println(2);

            Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
            System.out.println(3);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "ChaCha20");
            System.out.println(4);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            System.out.println(5);
            // decrypted text
            byte[] decrypted = cipher.doFinal(encryptedText);
            System.out.println("数据解密完成");

            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getRandomBytes() {
        // 创建 SecureRandom 实例
        SecureRandom secureRandom = new SecureRandom();

        // 生成 IV，长度为16字节
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);

        return bytes;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static byte[] hexToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i+1), 16));
        }
        return data;
    }
}
