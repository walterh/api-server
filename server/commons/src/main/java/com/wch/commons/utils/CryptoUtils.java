package com.wch.commons.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.net.util.Base64;

public class CryptoUtils {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    // http://stackoverflow.com/questions/7124735/hmac-sha256-algorithm-for-signature-calculation
    public static String secretHashImpl(String key, String data) throws NoSuchAlgorithmException, InvalidKeyException {
        final Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        final SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);

        String base64 = Base64.encodeBase64String(sha256_HMAC.doFinal(data.getBytes(UTF8)));
        base64 = base64.replace('+', '-');
        base64 = base64.replace('/', '_');

        return base64.replaceAll("\\s", "");
    }

    private static String generateKey() {
        String privateKey = null;
        try {
            KeyGenerator generator;
            generator = KeyGenerator.getInstance("HmacSHA256");
            generator.init(new SecureRandom());
            SecretKey key = generator.generateKey();
            key.getEncoded();

            privateKey = Base64.encodeBase64String(key.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return privateKey;
    }

    public static String decryptCookie(String key, String cookie) {
        String plainText = "";
        try {

            Key secret_key = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");

            byte[] iv = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
            cipher.init(Cipher.DECRYPT_MODE, secret_key, new IvParameterSpec(iv));

            byte[] decodedValue = Base64.decodeBase64(cookie);
            plainText = new String(cipher.doFinal(decodedValue), "UTF-8");

        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (BadPaddingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return plainText;
    }

    // easy password crtpyto: reverse -> base64
    public static String decryptPassword(String pwdCrypto) {
        byte[] bytes = Base64.decodeBase64(pwdCrypto);
        String decodedKey = new String(bytes);
        if (!Utils.isNullOrEmptyString(decodedKey)) {
            return new StringBuilder(decodedKey).reverse().toString();
        }
        return null;
    }

    public static void main(String[] args) {
        String text = decryptCookie("IneedToBeA32Characterencryptionk",
                "lcKvjnu/qg9UWqSbaK4VrPu8c3Gl5An4YCdG+EKvt4xYXluET/MFhs0djewVthTGjyx4sxKV4ul2F0SlQOIG8w==");
        System.out.println(text);
        System.out.println("New sha256 private key = " + generateKey());
    }
}
