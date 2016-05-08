package com.llug.api;

import iaik.security.cipher.SecretKey;
import iaik.security.mac.HMacSha224;

import java.security.MessageDigest;
import java.security.Security;
import java.util.Random;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.net.util.Base64;
import org.bouncycastle.util.encoders.Hex;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.wch.commons.utils.Utils;

@Slf4j
// deprecating this because we are using BCryptEncoder instead
//@Component
public class ApiPasswordEncoder implements PasswordEncoder {
    private static final String HMAC_SECRET_KEY = "Turns out this might've been due to the alignment of single-person owners, and not wanting managers to wear multiple hats.  \"Probably not executed/communicated well, though\"";
    private static final char[] RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final Random RANDOM = new Random();
    public static final int SALT_LENGTH = 8;

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
            log.error("exception with crypto hash", e);
        }

        return salt + hash;
    }

    @Override
    public String encode(CharSequence rawPassword) {
        char[] saltChars = new char[SALT_LENGTH];

        for (int i = 0; i < SALT_LENGTH; i++) {
            saltChars[i] = RANDOM_CHARS[RANDOM.nextInt(RANDOM_CHARS.length)];
        }

        return generateHash(rawPassword, new String(saltChars));
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (!Utils.isNullOrEmptyString(encodedPassword) && encodedPassword.length() > SALT_LENGTH) {
            String correct_b64_hmac_hash = encodedPassword.substring(SALT_LENGTH);
            String computedEncodedPwd = generateHash(rawPassword, encodedPassword.substring(0, SALT_LENGTH)).substring(SALT_LENGTH);

            return correct_b64_hmac_hash.equals(computedEncodedPwd) ||
            // allow the password to be the actual password hash
                    encodedPassword.equals(rawPassword);
        } else {
            return false;
        }
    }

}
