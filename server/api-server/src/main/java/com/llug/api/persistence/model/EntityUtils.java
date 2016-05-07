package com.llug.api.persistence.model;

import java.math.BigInteger;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.util.Bytes;

import com.wch.commons.utils.IdUtils;
import com.wch.commons.utils.Utils;

public class EntityUtils {
    public static final Pattern DIGIT_PATTERN = Pattern.compile("[\\d]+");

    public static Long safeDecodeId(String id) {
        if (!Utils.isNullOrEmptyString(id)) {
            return DIGIT_PATTERN.matcher(id).matches() ? Long.valueOf(id) : decodeId(id);
        } else {
            return null;
        }
    }

    public static String encodeId(Long id) {
        Long l = Long.rotateLeft(id, 27);
        l = ~l;

        return IdUtils.encodeBase62(l);
    }

    public static Long decodeId(String id) {
        byte[] b = IdUtils.decodeBase62(id);
        if (b == null || b.length > 8) {
            return null;
        } else if (b.length < 8) {
            byte[] signExtended = new byte[8 - b.length];
            byte val = (byte) ((b[0] < 0) ? -1 : 0);

            for (int i = 0; i < signExtended.length; i++) {
                signExtended[i] = val;
            }

            b = Utils.concatAll(signExtended, b);
        }
        Long l = Bytes.toLong(b);
        l = ~l;
        return Long.rotateRight(l, 27);
    }

}
