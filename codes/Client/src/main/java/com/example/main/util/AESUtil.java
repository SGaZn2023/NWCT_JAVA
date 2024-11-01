package com.example.main.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class AESUtil {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public static byte[] encrypt(byte[] plaintextBytes, byte[] keyBytes, byte[] ivBytes) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            return cipher.doFinal(plaintextBytes);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public static byte[] decrypt(byte[] encrypted, byte[] keyBytes, byte[] ivBytes) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decrypted = cipher.doFinal(encrypted);
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public static byte[] getKey() {
        try {
            // 创建 KeyGenerator 实例，指定算法为 AES
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            // 指定密钥长度为128位（16字节）
            keyGenerator.init(128);
            // 生成密钥
            SecretKey secretKey = keyGenerator.generateKey();
            // 返回密钥的字节数组表示
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] getIV() {
        // 创建 SecureRandom 实例
        SecureRandom secureRandom = new SecureRandom();

        // 生成 IV，长度为16字节
        byte[] iv = new byte[16];
        secureRandom.nextBytes(iv);

        return iv;
    }
}
