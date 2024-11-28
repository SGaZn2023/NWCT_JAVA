package com.sgz.main.obj;


import com.sgz.main.util.DataEncryptUtil;

public class MyEncrypt {
    private boolean isEncrypt;
    private byte[] secretKey;
    private byte[] IV;

    public MyEncrypt() {
        this(false);
    }

    public MyEncrypt(boolean isEcrypt) {
        this(isEcrypt, (byte[]) null, null);
    }

    public MyEncrypt(boolean isEcrypt, String secretKey, String IV) {
        this(isEcrypt, DataEncryptUtil.hexToBytes(secretKey), DataEncryptUtil.hexToBytes(IV));
    }

    public MyEncrypt(boolean isEcrypt, byte[] secretKey, byte[] IV) {
        this.isEncrypt = isEcrypt;
        this.secretKey = secretKey;
        this.IV = IV;
    }

    public boolean getIsEncrypt() {
        return isEncrypt;
    }

    public void setIsEncrypt(boolean ecrypt) {
        isEncrypt = ecrypt;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(byte[] secretKey) {
        this.secretKey = secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = DataEncryptUtil.hexToBytes(secretKey);
    }

    public byte[] getIV() {
        return IV;
    }

    public void setIV(byte[] IV) {
        this.IV = IV;
    }

    public void setIV(String IV) {
        this.IV = DataEncryptUtil.hexToBytes(IV);
    }
}
