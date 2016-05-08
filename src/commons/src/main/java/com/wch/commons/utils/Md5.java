package com.wch.commons.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.codec.DecoderException;

public class Md5 {
    private byte[] md5bytes;

    private Md5(byte[] md5bytes) {
        super();
        this.md5bytes = md5bytes;
    }

    public byte[] getMd5bytes() {
        return md5bytes;
    }

    @Override
    public String toString() {
        return org.apache.commons.codec.binary.Hex.encodeHexString(md5bytes).toLowerCase();
    }

    public static Md5 createFromString(String s) {
        try {
            byte[] input = s.getBytes();
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            md.update(input, 0, input.length);

            return new Md5(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static Md5 createFromMd5Hash(String hash) {
        try {
            return new Md5(org.apache.commons.codec.binary.Hex.decodeHex(hash.toCharArray()));
        } catch (DecoderException e) {
            throw new RuntimeException(e);
        }
    }

    public static Md5 createFromMd5Hash(byte[] hashBytes) {
        return new Md5(hashBytes);
    }
}
