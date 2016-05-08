package com.wch.commons.utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.hadoop.hbase.util.Bytes;

public class IdUtils {
    public static UUID MAX_UUID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    public static UUID MIN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    public static byte[] MAX_UUID_BYTES = Utils.uuidAsByteArray(MAX_UUID);
    public static byte[] MIN_UUID_BYTES = Utils.uuidAsByteArray(MIN_UUID);

    // these must be in increasing order of indexes:
    // '0' = 48, '9' = 57, 'A' = 65, 'Z' = 90, 'a' = 97, 'z' = 122
    private static String base62CharList = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static char[] base62Chars = base62CharList.toCharArray();
    public static Integer BASE_62 = new Integer(62);

    public static String converter(int base, long decimalNumber) {
        String tempVal = decimalNumber == 0 ? "0" : "";
        long mod = 0;

        while (decimalNumber != 0) {
            mod = decimalNumber % base;
            tempVal = base62CharList.substring((int) mod, (int) mod + 1) + tempVal;
            decimalNumber = decimalNumber / base;
        }
        System.out.print(tempVal);
        return tempVal;
    }

    public static String encodeBase62(UUID uuid) {
        byte[] bytes = Utils.uuidAsByteArray(uuid);

        return encodeBaseImpl(bytes, BASE_62);
    }

    public static String encodeBase62HexadecimalString(String s) {
        try {
            if (!Utils.isNullOrEmptyString(s)) {
                BigInteger input = new BigInteger(s, 16);

                return encodeBaseImpl(input, BASE_62);
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new NumberFormatException(String.format("encodeBase62HexadecimalString(\"%s\")", s));
        }
    }

    public static String encodeBase62(long id) {
        return encodeBaseImpl(Bytes.toBytes(id), BASE_62);
    }

    public static String encodeBase62(byte[] bytes) {
        return encodeBaseImpl(bytes, BASE_62);
    }

    private static String encodeBaseImpl(BigInteger input, Integer modulo) {
        BigInteger bigModulo = new BigInteger(modulo.toString());
        char[] target = new char[1024];
        int idx = 0;

        while (input != BigInteger.ZERO) {
            BigInteger[] parts = input.divideAndRemainder(bigModulo);
            int mod = parts[1].intValue();

            // store in reverse order...
            char encodedChar = base62Chars[mod];
            target[1023 - idx] = encodedChar;

            // this is the number divided by the modulo number;
            input = parts[0];

            // increment
            idx++;
        }
        char[] encodedChars = Arrays.copyOfRange(target, 1024 - idx, 1024);

        return new String(encodedChars);
    }

    private static String encodeBaseImpl(byte[] inputBytes, Integer modulo) {
        String s = null;
        // Java doesn't support unsigned...so invert if the first byte is negative
        boolean isNegative = false;
        if (inputBytes[0] < 0) {
            isNegative = true;

            // duplicate the bytes so we don't mess up the original array
            inputBytes = Arrays.copyOf(inputBytes, inputBytes.length);

            // XOR the byte array
            for (int i = 0; i < inputBytes.length; i++) {
                inputBytes[i] = (byte) ~inputBytes[i];
            }
        }

        if (inputBytes.length <= 8) {
            char[] target = new char[1024];
            int idx = 0;

            byte[] padding = new byte[8 - inputBytes.length];
            inputBytes = Utils.concatAll(padding, inputBytes);

            Long l = Bytes.toLong(inputBytes);

            while (l > 0) {
                long mod = l % modulo;
                l = (long) (l / modulo);

                // both base36 and base62 use base62Chars.  Base36
                // has a lower modulo, so all base36 chars are in
                // the base62 chars array
                // store in reverse order...
                char encodedChar = base62Chars[(int) mod];
                target[1023 - idx] = encodedChar;

                // increment
                idx++;
            }

            char[] encodedChars = Arrays.copyOfRange(target, 1024 - idx, 1024);

            s = new String(encodedChars);
        } else {
            s = encodeBaseImpl(new BigInteger(inputBytes), modulo);
        }

        if (isNegative) {
            s = "_" + s;
        }

        return s;
    }

    public static byte[] decodeBase62(String inputString) {
        return decodeBaseImpl(inputString, BASE_62);
    }

    public static String convertAndDecodeProjectId(String projectId) {
        if (!Utils.isNullOrEmptyString(projectId)) {
            return IdUtils.decodeBase62UUID(projectId).toString().toLowerCase();
        }
        return null;
    }

    public static UUID decodeBase62UUID(String inputString) {
        byte[] byteArray = decodeBaseImpl(inputString, BASE_62);
        boolean isNegative = inputString.charAt(0) == '_';

        if (byteArray.length < 16) {
            // this happens if the UUID starts with ff (-1), which gets xor'd to 0, which means
            // we lose digits.  So sign-extend to 16 with -1's
            byte[] extendedByteArray = new byte[16];
            byte extend = (byte) (isNegative ? -1 : 0);

            for (int i = 0; i < 16 - byteArray.length; i++) {
                extendedByteArray[i] = extend;
            }

            // copy over at the end
            System.arraycopy(byteArray, 0, extendedByteArray, 16 - byteArray.length, byteArray.length);

            // swap
            byteArray = extendedByteArray;
        }

        return Utils.bytesToUuid(byteArray);
    }

    public static String decodeBase62DeviceId(final String encodedDeviceId) {
        // first, decode to see if it is an email address
        byte[] bytes = IdUtils.decodeBase62(encodedDeviceId);
        String deviceId = decodeBase62HexadecimalString(encodedDeviceId);

        if (deviceId.length() == 39) {
            deviceId = org.apache.commons.lang.StringUtils.leftPad(deviceId, 40, "0");
        }

        return deviceId;
    }

    public static String encodeBase62DeviceId(final String deviceId) {
        if (!Utils.isNullOrEmptyString(deviceId)) {
            try {
                if (deviceId.contains("@")) {
                    return IdUtils.encodeBase62(deviceId.getBytes());
                } else {
                    return IdUtils.encodeBase62HexadecimalString(deviceId);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }

    private static String decodeBase62HexadecimalString(String inputString) {
        return decodeBase62HexadecimalString(inputString, null);
    }

    private static String decodeBase62HexadecimalString(String inputString, Integer zeroPadLength) {
        byte[] bytes = decodeBase62(inputString);
        BigInteger bi = new BigInteger(bytes);
        String s = bi.toString(16);

        if (zeroPadLength != null && s.length() != zeroPadLength) {
            s = org.apache.commons.lang.StringUtils.leftPad(s, zeroPadLength, "0");
        }
        return s;
    }

    private static byte[] decodeBaseImpl(String inputString, Integer modulo) {
        if (!Utils.isNullOrEmptyString(inputString)) {
            boolean isNegative = inputString.charAt(0) == '_';
            if (isNegative) {
                inputString = inputString.substring(1);
            }

            char[] inputChars = inputString.toCharArray();
            BigInteger bigModulo = new BigInteger(modulo.toString());
            BigInteger result = BigInteger.ZERO;
            BigInteger powBase = BigInteger.ZERO;

            // got to go in reverse order. We need to to a power to the n
            // operation which is expensive, so we just keep multiplying
            // by the modulo, except for iteration zero, where don't multiply
            for (int i = inputChars.length - 1; i >= 0; i--) {
                int c = inputChars[i];
                // find the correct base62 index (0-61).  +36 comes from 10 + 26,
                // which is the first 36 digits of base62 (numbers, then upper
                // case full alphabet).
                Integer idx = c >= 'a' ? (c - 'a' + 36) : (c >= 'A') ? (c - 'A' + 10) : (c - '0');
                Integer idx2 = c >= 'A' ? (c - 'A' + 10) : (c - '0');

                if (modulo == 36) {
                    System.out.println(Utils.csf("idx (experimental) = {0}, idx2 (reference) = {1}", idx, idx2));
                }

                if (powBase.equals(BigInteger.ZERO)) {
                    // bigModulo^0 = 1 * the index
                    result = new BigInteger(idx.toString());
                    powBase = BigInteger.ONE;
                } else {
                    // do += bigModulo^n*idx
                    powBase = powBase.multiply(bigModulo);

                    result = result.add(powBase.multiply(new BigInteger(idx.toString())));
                }
            }

            byte[] bytes = result.toByteArray();

            if (isNegative) {
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = (byte) ~bytes[i];
                }
            }

            return bytes;

        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static boolean parseIdAll(String key, Ref<?>[] refs, Class<?>[] types) {
        if (Utils.isNullOrEmptyString(key) || refs == null) {
            return false;
        }

        // sniff if it's a compound key or a UUID.  We could also regex integers, but we currently
        // don't have keys which are just longs/ints/bytes
        if (key.contains(",") || key.contains("-")) {
            // this is a regular comma-separated id
            List<String> keyParts = Utils.stringSplitRemoveEmptyEntries(key, ",", false, false);
            int index = 0;

            for (String keyPart : keyParts) {
                if (keyPart.length() == 36) {
                    if (Utils.tryParseUuidRaw(keyPart, (Ref<UUID>) refs[index]) == false) {
                        return false;
                    }
                } else if (keyPart.length() == 16) {
                    if (Utils.tryParseLong(keyPart, 16, (Ref<Long>) refs[index]) == false) {
                        return false;
                    }
                } else if (keyPart.length() <= 2) {
                    if (Utils.tryParseByte(keyPart, 16, (Ref<Byte>) refs[index]) == false) {
                        return false;
                    }
                } else {
                    return false;
                }

                index++;
            }

            return true;
        } else {
            // parse base62-style and feed through this function again.
            byte[] rowKeyBytes = decodeBase62(key);

            return parseRowKeyAll(rowKeyBytes, refs, types);
        }
    }

    @SuppressWarnings("unchecked")
    public static boolean parseRowKeyAll(byte[] rowKey, Ref<?>[] refs, Class<?>[] types) {
        if (rowKey == null || refs == null || types == null || refs.length != types.length) {
            throw new ArgumentException("parseRowKeyAll:  null parameters");
        }

        byte[] byteRange = null;
        int byteIndex = 0;

        for (int i = 0; i < refs.length; i++) {
            Class<?> clazz = types[i];
            if (clazz == UUID.class) {
                if (byteIndex + 16 > rowKey.length) {
                    throw new ArgumentException("parseRowKeyAll:  not enough bytes to parse UUID");
                }

                Ref<UUID> uuidRef = (Ref<UUID>) refs[i];
                byteRange = Arrays.copyOfRange(rowKey, byteIndex, byteIndex + 16);
                UUID uuid = Utils.bytesToUuid(byteRange);

                if (uuidRef != null) {
                    uuidRef.set(uuid);
                }
                byteIndex += 16;
            } else if (clazz == Long.class) {
                if (byteIndex + 8 > rowKey.length) {
                    throw new ArgumentException("parseRowKeyAll:  not enough bytes to parse Long");
                }

                Ref<Long> tsRef = (Ref<Long>) refs[i];
                byteRange = Arrays.copyOfRange(rowKey, byteIndex, byteIndex + 8);
                Long ts = toLongFromInvertedBytes(byteRange);

                if (tsRef != null) {
                    tsRef.set(ts);
                }
                byteIndex += 8;
            } else if (clazz == Byte.class) {
                if (byteIndex + 1 > rowKey.length) {
                    throw new ArgumentException("parseRowKeyAll:  not enough bytes to parse Byte");
                }

                Ref<Byte> byteRef = (Ref<Byte>) refs[i];

                if (byteRef != null) {
                    byteRef.set(rowKey[byteIndex]);
                }
                byteIndex += 1;
            } else if (clazz == Md5.class) {
                if (byteIndex + 16 > rowKey.length) {
                    throw new ArgumentException("parseRowKeyAll:  not enough bytes to parse Md5");
                }

                byteRange = Arrays.copyOfRange(rowKey, byteIndex, byteIndex + 16);

                Ref<Md5> hashRef = (Ref<Md5>) refs[i];
                if (hashRef != null) {
                    hashRef.set(Md5.createFromMd5Hash(byteRange));
                }

                byteIndex += 16;
            } else {
                throw new ArgumentException("parseRowKeyAll:  unexpected clazz = " + clazz.toString());
            }
        }

        // we shouldn't have any bytes left.
        if (rowKey.length != byteIndex) {
            throw new ArgumentException(String.format("parseRowKeyAll:  unexpected additional %d bytes after rowkey parsed", rowKey.length - byteIndex));
        }

        return true;
    }

    public static byte[] toRowKeyAll(String key, Class<?>[] types) {
        if (key.contains(",") || key.contains("-")) {
            List<byte[]> byteList = new ArrayList<byte[]>();
            List<String> keyParts = Utils.stringSplitRemoveEmptyEntries(key, ",", false, false);
            Ref<Byte> byteRef = null;
            Ref<Long> tsRef = null;
            Ref<UUID> uuidRef = null;

            for (Class<?> clazz : types) {
                if (clazz == UUID.class) {
                    uuidRef = new Ref<UUID>(null);
                } else if (clazz == Byte.class) {
                    byteRef = new Ref<Byte>(null);
                } else if (clazz == Long.class) {
                    tsRef = new Ref<Long>(null);
                }
            }

            for (String keyPart : keyParts) {
                if (keyPart.length() == 36) {
                    if (Utils.tryParseUuidRaw(keyPart, uuidRef)) {
                        byte[] uuidBytes = Utils.uuidAsByteArray(uuidRef.get());
                        byteList.add(uuidBytes);
                    } else {
                        throw new ArgumentException("toRowKeyAll couldn't parse uuid = " + keyPart);
                    }
                } else if (keyPart.length() == 16) {
                    if (Utils.tryParseLong(keyPart, 16, tsRef)) {
                        byte[] tsBytes = toInvertedBytesFromLong(tsRef.get());
                        byteList.add(tsBytes);
                    } else {
                        throw new ArgumentException("toRowKeyAll couldn't parse long = " + keyPart);
                    }
                } else if (keyPart.length() <= 2) {
                    if (Utils.tryParseByte(keyPart, 16, byteRef)) {
                        byte[] bytes = new byte[] { byteRef.get() };
                        byteList.add(bytes);
                    } else {
                        throw new ArgumentException("toRowKeyAll couldn't parse byte = " + keyPart);
                    }
                } else {
                    throw new ArgumentException("toRowKeyAll couldn't recognize part = " + keyPart);
                }
            }

            return Utils.concatAll(byteList);
        } else {
            // parse base62-style and feed through this function again.
            byte[] rowKeyBytes = decodeBase62(key);

            return rowKeyBytes;
        }
    }

    public static byte[] getRowKeyAll(Object... args) {
        Object[] objs = new Object[args.length];
        int index = 0;
        for (Object arg : args) {
            objs[index] = arg;
            index++;
        }

        return getRowKeyAll2(objs);
    }

    public static byte[] getRowKeyAll2(Object[] args) {
        List<byte[]> byteList = new ArrayList<byte[]>(args.length);
        byte[] bytes = null;

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];

            if (arg instanceof UUID) {
                bytes = Utils.uuidAsByteArray((UUID) arg);
                byteList.add(bytes);
            } else if (arg instanceof Long) {
                bytes = toInvertedBytesFromLong(truncateTime((Long) arg));
                byteList.add(bytes);
            } else if (arg instanceof Byte) {
                byteList.add(new byte[] { (Byte) arg });
            } else if (arg instanceof byte[]) {
                byteList.add((byte[]) arg);
            } else if (arg instanceof Md5) {
                byteList.add(((Md5) arg).getMd5bytes());
            } else if (arg instanceof String) {
                if (!Utils.isNullOrEmptyString((String) arg)) {
                    byteList.add(Bytes.toBytes((String) arg));
                }
            } else {
                throw new ArgumentException(String.format("unknown type at index %d; args = { %s }", i, Utils.toStringList(args)));
            }
        }

        return Utils.concatAll(byteList);
    }

    private static byte[] toInvertedBytesFromLong(long val) {
        // invert 2's complement
        val = ~val;

        byte[] b = new byte[8];
        for (int i = 7; i > 0; i--) {
            b[i] = (byte) val;
            val >>>= 8;
        }
        b[0] = (byte) val;

        return b;
    }

    private static long toLongFromInvertedBytes(byte[] bytes) {
        long l = 0;
        for (int i = 0; i < 8; i++) {
            l <<= 8;
            l ^= bytes[i] & 0xFF;
        }

        // invert 2's complement
        l = ~l;

        return l;
    }

    /*
     * See http://brunodumon.wordpress.com/2010/02/17/building-indexes-using-hbase-mapping-strings-numbers-and-dates-onto-bytes/.
     * 
     * HBase scans in lexical order, meaning from low->high. Part of our rowkeys use dates, which means newer dates have larger rowkey fragments.
     * 
     * We want reverse chronological ordering (most recent first) for queries. To get scans to increment correctly in the range [startRow,endRow) we effectively
     * have to negate. That way, ascending gets us reverse chronological order.
     * 
     * Dates are milliseconds since 1970-01-01T00:00:00.000+0000. The maximum date we are interested in is 9999-12-31T23:59:59.999+0000, although dates can be
     * much larger since they are 64-bit signed Long.
     * 
     * Therefore, the legally valid range of dates is [0,253402300799999).
     * 
     * If we see a date larger/smaller than this range, we will truncate it, knowing that anything falling outside the bounds is illegal anyway, so we're not
     * worried about bugs
     */
    public static long truncateTime(long l) {
        if (l < DateUtils.MIN_TIME) {
            l = DateUtils.MIN_TIME;
        } else if (l > DateUtils.MAX_TIME) {
            l = DateUtils.MAX_TIME;
        }

        return l;
    }

    public static String reUUID(String s) {
        if (s.length() == 32) {
            String s2 = String.format("%s-%s-%s-%s-%s", s.substring(0, 8), s.substring(8, 12), s.substring(12, 16), s.substring(16, 20), s.substring(20));

            return s2;
        } else {
            return null;
        }
    }
}