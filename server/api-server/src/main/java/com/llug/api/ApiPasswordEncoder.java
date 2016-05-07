package com.llug.api;

import static ch.lambdaj.Lambda.*;

import iaik.security.cipher.SecretKey;
import iaik.security.mac.HMacSha224;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.net.util.Base64;
import org.bouncycastle.jce.provider.JCEKeyGenerator.HMACSHA224;
import org.bouncycastle.jce.provider.JCEMac;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.wch.commons.utils.Utils;

import ch.lambdaj.function.closure.Closure;

@SuppressWarnings("deprecation")
@Component
public class ApiPasswordEncoder implements PasswordEncoder {
    private static final String HMAC_SECRET_KEY = "I turned to him, mid-pee, and said, \"Jeff, today is my last day at Amazon and I wanted to thank you for building such a great company.\"";
    private static final char[] RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final Random RANDOM = new Random();

    @PostConstruct
    public void initialize() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    private String generateHash(CharSequence rawPass, String salt) {
        String hash = null;
        try {
            HMacSha224 mac = new HMacSha224();
            SecretKey key = new SecretKey(HMAC_SECRET_KEY.getBytes(), "HMAC/SHA224");
            MessageDigest md5 = MessageDigest.getInstance("MD5");

            byte[] passwordMd5Bytes = md5.digest(rawPass.toString().getBytes());
            String passwordMd5Digest = new String(Hex.encode(passwordMd5Bytes));
            String message = salt + passwordMd5Digest;
            byte[] messageBytes = message.getBytes();

            mac.engineInit(key, null);
            mac.engineUpdate(messageBytes, 0, messageBytes.length);
            byte[] shaDigest = mac.engineDoFinal();
            hash = Base64.encodeBase64String(shaDigest).trim();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return salt + hash;
    }

    @Override
    public String encodePassword(String rawPassword, Object salt) {
        char[] saltChars = new char[ApiSaltSource.SALT_LENGTH];

        for (int i = 0; i < ApiSaltSource.SALT_LENGTH; i++) {
            saltChars[i] = RANDOM_CHARS[RANDOM.nextInt(RANDOM_CHARS.length)];
        }

        return generateHash(rawPassword, new String(saltChars));
    }

    @Override
    public boolean isPasswordValid(String encodedPassword, String rawPassword, Object salt) {
        if (!Utils.isNullOrEmptyString(encodedPassword) && encodedPassword.length() > ApiSaltSource.SALT_LENGTH) {
            String correct_b64_hmac_hash = encodedPassword.substring(ApiSaltSource.SALT_LENGTH);
            String computedEncodedPwd = generateHash(rawPassword, encodedPassword.substring(0, ApiSaltSource.SALT_LENGTH)).substring(ApiSaltSource.SALT_LENGTH);

            return correct_b64_hmac_hash.equals(computedEncodedPwd) ||
            // allow the password to be the actual password hash
                    encodedPassword.equals(rawPassword);
        } else {
            return false;
        }
    }

}
