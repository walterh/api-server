package com.wch.commons.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.hbase.util.Bytes;

public class Utils {
    public static final String UUID_PATTERN_STRING = "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})";
    public static final Pattern UUID_PATTERN = Pattern.compile(UUID_PATTERN_STRING);
    static final Pattern IP_V4_PATTERN = Pattern.compile("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)");

    // a login name must start and end with letters, but it can be
    // alphanumeric/underscore/dot inbetween.
    static String LOGIN_NAME_BASE_PATTERN_STRING = "[a-z][a-z0-9_.]+[a-z0-9]+";
    // force begin and end
    static Pattern LOGIN_NAME_PATTERN = Pattern.compile("^" + LOGIN_NAME_BASE_PATTERN_STRING + "$");

    public static Pattern BASE62_UDID_PATTERN = Pattern.compile("^([_]??[\\da-zA-Z]+)$");

    // must begin with a letter...no underscore, periods, or numbers to start
    static Pattern HASHTAG_PATTERN = Pattern.compile("(#[a-zA-Z]+)");
    static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

    // common separators: "," "!", "$", "%", "&", "?", "(", ")", "[", "]", "{", "}", "<", ">", ":", ";", """, "'", "-"
    // the period we need to be careful of because we don't want to strip emails
    // or domains, so we search for period followed by whitespace first, then the or match
    static Pattern SEARCH_PREPROCESSOR_PATTERN = Pattern.compile("(\\.[\\s]+|[,!\\$%&?\\(\\)\\[\\]\\{\\}<>;\"'\\s]+)");

    public static final Pattern DIGIT_PATTERN = Pattern.compile("[\\d]+");

    public static final String JUSTME_HOST_NAME = getJustMeHostName();
    public static final String LOOPBACK_ADDRESS = "127.0.0.1";

    private static Random RANDOM = new Random();

    public static Boolean validateIpV4Address(String addr) {
        return IP_V4_PATTERN.matcher(addr).matches();
    }

    public static boolean validateLoginName(String loginName) {
        return LOGIN_NAME_PATTERN.matcher(loginName).matches();
    }

    public static String intToIpV4(int i) {
        return ((i >> 24) & 0xFF) + "." + ((i >> 16) & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + (i & 0xFF);
    }

    public static Long ipV4ToInt(String addr) {
        String[] addrArray = addr.split("\\.");

        long num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            int power = 3 - i;

            num += ((Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power)));
        }
        return num;
    }

    public static byte[] uuidAsByteArray(UUID uuid) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] buffer = new byte[16];

        for (int i = 0; i < 8; i++) {
            buffer[i] = (byte) (msb >>> 8 * (7 - i));
        }
        for (int i = 8; i < 16; i++) {
            buffer[i] = (byte) (lsb >>> 8 * (7 - i));
        }
        return buffer;
    }

    public static UUID bytesToUuid(byte[] byteArray) {
        UUID result = null;

        if (byteArray == null) {
            throw new ArgumentException("invalid bytes: no bytes");
        } else if (byteArray.length != 16) {
            throw new ArgumentException("invalid bytes: bytes.length = " + byteArray.length);
        }

        if (byteArray != null) {
            long msb = 0;
            long lsb = 0;
            for (int i = 0; i < 8; i++) {
                msb = (msb << 8) | (byteArray[i] & 0xff);
            }
            for (int i = 8; i < 16; i++) {
                lsb = (lsb << 8) | (byteArray[i] & 0xff);
            }
            result = new UUID(msb, lsb);
        }
        return result;
    }

    public static List<UUID> parseUuidList(String list) {
        String[] uuids = list.split(",");
        List<UUID> uuidList = new ArrayList<UUID>(uuids.length);

        for (String uuidStr : uuids) {
            // will through IllegalArgumentException if there are any problems
            UUID uuid = UUID.fromString(uuidStr);

            if (uuid != null) {
                uuidList.add(uuid);
            }
        }

        return uuidList;
    }

    public static String uuidListToCsv(List<UUID> uuids) {
        String csv = "";

        if (uuids != null && uuids.size() > 0) {
            StringBuilder sb = new StringBuilder();

            for (UUID uuid : uuids) {
                sb.append(uuid.toString());
                sb.append(",");
            }

            // remove the last ",", we do (len-1).
            csv = sb.substring(0, sb.length() - 1);
        }
        return csv;
    }

    public static Boolean isNullOrEmpty(List<?> l) {
        return l == null || l.size() == 0;
    }

    public static Boolean isNullOrEmptyString(String s) {
        return s == null || s.length() == 0;
    }

    public static Boolean isSemanticallyEqual(String s1, String s2) {
        if (isNullOrEmptyString(s1)) {
            return isNullOrEmptyString(s2);
        } else {
            return s1.equals(s2);
        }
    }

    public static Boolean isSemanticallyEqual(Integer i1, Integer i2) {
        if (i1 == null) {
            return i2 == null;
        } else {
            return i1.equals(i2);
        }
    }

    public static Boolean isSemanticallyEqual(Long l1, Long l2) {
        if (l1 == null) {
            return l2 == null;
        } else {
            return l1.equals(l2);
        }
    }

    public static <T extends Enum<T>> Boolean isSemanticallyEqualEnums(T e1, T e2) {
        if (e1 == null) {
            return e2 == null;
        } else {
            return e1.equals(e2);
        }
    }

    public static String ensureString(String s) {
        return isNullOrEmptyString(s) ? "" : s;
    }

    public static String csf(String fmtStr, Object... args) {
        int i = 0;
        for (Object arg : args) {
            if (arg == null) {
                arg = "";
            }

            String replaceStr = String.format("\\{%d\\}", i);
            // braces are part of regex syntax, so escape them to get the literal match
            try {
                fmtStr = fmtStr.replaceAll(replaceStr, arg.toString());
            } catch (IllegalArgumentException ex) {
                System.out.println(ex.toString());
            }
            i++;
        }

        return fmtStr;
    }

    public static <T> String join(T[] s, String delimiter) {
        List<T> l = new ArrayList<T>(s.length);
        Collections.addAll(l, s);

        return join(l, delimiter);
    }

    public static String join(Collection<?> s, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iter = s.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (!iter.hasNext()) {
                break;
            }
            builder.append(delimiter);
        }
        return builder.toString();
    }

    public static <K, T> List<T> coalesceLists(Map<K, List<T>> map) {
        List<T> newList = new ArrayList<T>();
        for (List<T> l : map.values()) {

            if (l != null) {
                newList.addAll(l);
            }
        }

        return newList;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Class<T> cl, List<T> list) {
        T[] newTList = null;

        if (list != null) {
            newTList = (T[]) Array.newInstance(cl, list.size());
            list.toArray(newTList);
        }

        return newTList;
    }

    public static <T> List<T> asList(T... rest) {
        List<T> list = new ArrayList<T>();

        if (rest != null) {
            for (T t : rest) {
                if (t != null) {
                    list.add(t);
                }
            }
        }

        return list;
    }

    public static <T> List<T> flatten(List<List<T>> rest) {
        List<T> flattenedList = new ArrayList<T>();

        if (rest != null) {
            for (List<T> l : rest) {
                if (l != null && l.size() > 0) {
                    flattenedList.addAll(l);
                }
            }
        }

        return flattenedList;
    }

    public static <T> List<T> flatten(List<T>... rest) {
        List<T> flattenedList = new ArrayList<T>();

        if (rest != null) {
            for (List<T> l : rest) {
                if (l != null && l.size() > 0) {
                    flattenedList.addAll(l);
                }
            }
        }

        return flattenedList;
    }

    public static <T> List<T> asNonNullValuedList(T... rest) {
        List<T> list = new ArrayList<T>();

        for (T t : rest) {
            if (t != null) {
                list.add(t);
            }
        }

        return list;
    }

    public static <T> Object[] toObjectArray(List<T> args) {
        Object[] objs = new Object[args.size()];

        for (int i = 0; i < objs.length; i++) {
            objs[i] = args.get(i);
        }

        return objs;
    }

    public static <T> Object[] toObjectArray(T[] args) {
        Object[] objs = new Object[args.length];

        for (int i = 0; i < args.length; i++) {
            objs[i] = args[i];
        }

        return objs;
    }

    /*
     * Unfortunately, Arrays.asList doesn't implement full List functionality, like remove.
     */
    public static <T> List<T> toList(T[] tarray) {
        List<T> list = new ArrayList<T>(tarray.length);

        for (T t : tarray) {
            list.add(t);
        }

        return list;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Object> List<T> clone(List<T> list) {
        return ((List<T>) ((ArrayList<T>) list).clone());
    }

    public static String toStringList(Object[] args) {
        StringBuilder sb = new StringBuilder();

        if (args != null && args.length > 0) {
            for (Object arg : args) {
                sb.append(arg);
                sb.append(",");
            }

            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    public static <T> String toStringList(List<T> list) {
        return toStringList(list, ",");
    }

    public static <T> String toStringList(List<T> list, String separator) {
        StringBuilder sb = new StringBuilder();

        if (list != null && list.size() > 0) {
            for (T t : list) {
                if (t instanceof byte[]) {
                    sb.append(Bytes.toString((byte[]) t));
                } else {
                    sb.append(t.toString());
                }
                sb.append(separator);
            }

            sb.setLength(sb.length() - 1);
        }

        return sb.toString();
    }

    public static <T extends Enum<T>> Integer[] toOrdinalArray(T[] list) {
        Integer[] ordinals = new Integer[list.length];

        for (int i = 0; i < ordinals.length; i++) {
            ordinals[i] = list[i].ordinal();
        }

        return ordinals;
    }

    public static <T> List<T> clean(final List<T> l) {
        if (!Utils.isNullOrEmpty(l)) {
            l.removeAll(Collections.singleton(null));

            // do not dedupe...this will fuck up order
            return l;
            // also dedupe
            //return new ArrayList<T>(new HashSet<T>(l));
        } else {
            return new ArrayList<T>();
        }
    }

    /*
     * I have to implement this crazy-signature function because Java does contains() using equals(), which won't work for classes like UUID
     */
    public static <T extends Comparable<? super T>> boolean containsUsingComparator(List<T> tList, T tCheck) {
        for (T tItem : tList) {
            if (tItem.compareTo(tCheck) == 0) {
                return true;
            }
        }

        return false;
    }

    public static <K, V> Map<K, V> mapWithKeysAndValues(K[] keys, V[] values) {
        Map<K, V> hash = new HashMap<K, V>(keys.length);

        for (int i = 0; i < keys.length; i++) {
            hash.put(keys[i], values[i]);
        }

        return hash;
    }

    /*
     * This function implements C# trim(char[] { ...}) by looping through and finding the start/end.
     */
    public static String trim(String s, char[] trimChars) {
        char[] chs = s.toCharArray();
        int start = 0;
        int end = chs.length - 1;
        boolean tryTrimMore = true;
        while (tryTrimMore && start < end) {
            tryTrimMore = false;
            for (char c : trimChars) {
                if (chs[start] == c) {
                    start++;
                    tryTrimMore = true;
                }
                if (chs[end] == c) {
                    end--;
                    tryTrimMore = true;
                }
            }
        }

        if (start < end) {
            // we want "end" char, which is at index {end, end+1}
            return s.substring(start, end + 1);
        } else {
            return "";
        }
    }

    public static boolean hasHighAscii(String s) {
        char[] chars = s.toCharArray();
        for (char ch : chars) {
            if (ch > 0x7f) {
                return true;
            }
        }

        return false;
    }

    private static final Class<?>[] ID_TYPE_LIST = new Class[] { UUID.class };

    public static UUID safeUuidFromString(String s) {
        UUID retUuid = null;

        if (!isNullOrEmptyString(s) && (BASE62_UDID_PATTERN.matcher(s).find() || UUID_PATTERN.matcher(s).find())) {
            // encoded form.
            Ref<UUID> uuidRef = new Ref<UUID>(null);

            if (IdUtils.parseIdAll(s, new Ref<?>[] { uuidRef }, ID_TYPE_LIST)) {
                retUuid = uuidRef.get();
            }
        }
        return retUuid;
    }

    public static UUID safeUuidFromStringRaw(String s) {
        UUID retUuid = null;
        if (!isNullOrEmptyString(s) && UUID_PATTERN.matcher(s).find()) {
            try {
                retUuid = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                retUuid = null;
            }
        }

        return retUuid;
    }

    public static <T extends Enum<T>> boolean tryParseEnum(Class<T> enumType, int ordinal, Ref<T> enumRef) {
        T en = safeParseEnum(enumType, ordinal, null);
        if (en != null) {
            if (enumRef != null) {
                enumRef.set(en);
            }

            return true;
        } else {
            return false;
        }
    }

    public static <T extends Enum<T>> T safeParseEnum(Class<T> enumType, int ordinal) {
        return safeParseEnum(enumType, ordinal, null);
    }

    public static <T extends Enum<T>> T safeParseEnum(Class<T> enumType, int ordinal, T defEnum) {
        T[] allValues = enumType.getEnumConstants();

        for (int i = 0; i < allValues.length; i++) {
            if (allValues[i].ordinal() == ordinal) {
                return allValues[i];
            }
        }

        return defEnum;
    }

    public static <T extends Enum<T>> T safeParseEnum(Class<T> enumType, String val) {
        return safeParseEnum(enumType, val, null);
    }

    /*
     * The hoops we have to jump thru to implement C#'s Enum.Parse(typeof(... Note that this is really bad design with returns scattered throught...but it's a
     * compact method.
     */
    public static <T extends Enum<T>> T safeParseEnum(Class<T> enumType, String val, T defEnum) {
        if (val != null) {
            val = val.trim();
        }
        try {
            return safeParseEnum(enumType, Integer.parseInt(val));
        } catch (NumberFormatException e) {
        }

        T[] allValues = enumType.getEnumConstants();
        for (int i = 0; i < allValues.length; i++) {
            if (allValues[i].toString().equals(val)) {
                return allValues[i];
            }
        }

        return defEnum;
    }

    public static <T extends Enum<T>> List<T> safeParseEnumList(Class<T> enumType, String val, boolean forceUnique) {
        List<T> l = new ArrayList<T>();
        String[] vals = val.split(",");

        for (String v : vals) {
            T e1 = safeParseEnum(enumType, v);

            if (e1 != null) {
                if (forceUnique && l.contains(e1)) {
                    continue;
                }
                l.add(e1);
            }
        }

        return l;
    }

    public static Long safeParseLong(String s, int radix) {
        long l = 0L;

        try {
            l = Long.parseLong(s, radix);
        } catch (NumberFormatException e) {
            l = 0L;
        }

        return l;
    }

    public static Double safeParseDouble(String s) {
        Double d = null;

        try {
            if (!Utils.isNullOrEmptyString(s)) {
                if (s.indexOf(",") >= 0) {
                    s = s.replace(",", ".");
                }

                d = Double.parseDouble(s);
            }
        } catch (NumberFormatException e) {
            d = null;
        }

        return d;
    }

    public static boolean tryParseDouble(String s, Ref<Double> dr) {
        boolean ret = false;
        try {
            Double d = Double.parseDouble(s);
            dr.set(d);
            return true;
        } catch (Exception e) {
        }

        return ret;
    }

    public static String asFixedDoubleForMobile(final Double d) {
        if (d != null) {
            // warning!!! 6 significant digits must line up for iOS and Android
            return String.format("%.6f", d);
        } else {
            return "";
        }
    }

    public static boolean tryParseInteger(String s, Ref<Integer> intRef) {
        boolean ret = true;

        try {
            Integer i = Integer.parseInt(s);
            if (intRef != null) {
                intRef.set(i);
            }
        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    public static boolean tryParseByte(String s, int radix, Ref<Byte> byteRef) {
        boolean ret = true;

        try {
            Byte b = Byte.parseByte(s, radix);
            if (byteRef != null) {
                byteRef.set(b);
            }
        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    public static boolean tryParseLong(String s, Ref<Long> longRef) {
        return tryParseLong(s, 10, longRef);
    }

    public static boolean tryParseLong(String s, int radix, Ref<Long> longRef) {
        boolean ret = true;

        try {
            Long l = Long.parseLong(s, radix);
            if (longRef != null) {
                longRef.set(l);
            }
        } catch (Exception e) {
            ret = false;
        }

        return ret;
    }

    public static boolean tryParseUuidRaw(String s, Ref<UUID> uuidRef) {
        UUID uuid = safeUuidFromStringRaw(s);
        if (uuid != null) {
            if (uuidRef != null) {
                // in some cases, we don't care about the actual UUID, only that it's valid
                uuidRef.set(uuid);
            }
            return true;
        } else {
            return false;
        }
    }

    /*
     * This function is exclusively used for HBase versioning. The order of the set is arbitrary, so we need to produce a sorted list of the versions in
     * decending order (meaning, from most recent to oldest)
     */
    public static List<Long> asSortedVersionList(Set<Long> set) {
        List<Long> versions = new ArrayList<Long>(set.size());
        versions.addAll(set);
        
        Collections.sort(versions, new Comparator<Long>() {
            public int compare(Long l1, Long l2) {
                return l2.compareTo(l1);
            }
        });

        return versions;
    }

    /*
     * This does a regular sort using the default comparator. It is used in aggregateMultiVersionedColumnFamilyAndPopulateMap to ensure that properties are
     * loaded oldest to latest. This way, aggregated objects are loaded correctly; newer properties override older properties.
     */
    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        Collections.sort(list);
        return list;
    }

    /*
     * Aggregates arrays. Taken from stackoverflow at http://stackoverflow.com/questions/80476/how-to-concatenate-two-arrays-in-java
     */
    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;

        for (T[] array : rest) {
            totalLength += array.length;
        }

        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;

        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    public static byte[] concatAll(List<byte[]> list) {
        int totalLength = 0;

        for (byte[] array : list) {
            totalLength += array.length;
        }

        byte[] result = Arrays.copyOf(list.get(0), totalLength);
        int offset = list.get(0).length;

        // skip over the first, since we resized the first
        for (int i = 1; i < list.size(); i++) {
            byte[] array = list.get(i);

            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] safeConcatAll(Class<T> cl, T[]... rest) {
        int totalLength = 0;

        for (T[] array : rest) {
            if (array != null) {
                totalLength += array.length;
            }
        }

        // create the resultant array
        T[] result = (T[]) Array.newInstance(cl, totalLength);

        // now copy the arrays...
        int offset = 0;
        for (T[] array : rest) {
            if (array != null) {
                System.arraycopy(array, 0, result, offset, array.length);
                offset += array.length;
            }
        }

        return result;
    }

    /*
     * Apparently I have to create a specific implementation for primitive types.
     */
    public static byte[] concatAll(byte[] first, byte[]... rest) {
        int totalLength = first.length;

        for (byte[] array : rest) {
            totalLength += array.length;
        }

        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;

        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }

    public static List<String> stringSplitNonRegex(String input, String delim) {
        List<String> l = new ArrayList<String>();

        while (true) {
            int index = input.indexOf(delim);
            if (index == -1) {
                l.add(input);
                return l;
            } else {
                l.add(input.substring(0, index));
                input = input.substring(index + delim.length());
            }
        }
    }

    public static List<String> stringSplitRemoveEmptyEntries(String s, String regex, boolean shouldTrim, boolean removeDuplicates) {
        List<String> list = null;

        if (s != null) {
            String[] strings = s.split(regex);
            list = new ArrayList<String>(strings.length);
            for (String str : strings) {
                if (shouldTrim) {
                    // should never be null...split shouldn't return null
                    str = str.trim();
                }

                if (!Utils.isNullOrEmptyString(str)) {
                    if (removeDuplicates) {
                        if (list.contains(str)) {
                            continue;
                        }
                    }
                    list.add(str);
                }
            }
        }
        return list;
    }

    public static List<String> createStringList(String... role) {
        List<String> roles = new ArrayList<String>();

        for (String r : role) {
            roles.add(r);
        }

        return roles;
    }

    public static List<String> parseStringList(String roleString) {
        List<String> roles = new ArrayList<String>();

        if (!Utils.isNullOrEmptyString(roleString)) {
            for (String s : roleString.split(";")) {
                if (!Utils.isNullOrEmptyString(s)) {
                    roles.add(s);
                }
            }
        }
        return roles;
    }

    /*
     * Properly parses the HTTP Range header.
     */
    public static boolean parseByteRangeHeader(String rangeHeader, Ref<Long> byteStartRef, Ref<Long> byteEndRef) {
        if (!isNullOrEmptyString(rangeHeader)) {
            String[] parts = rangeHeader.split("=");

            if (parts.length == 2 && parts[0].compareTo("bytes") == 0) {

                String[] range = parts[1].split("-");

                if (range.length > 0) {
                    if (tryParseLong(range[0], byteStartRef) == false) {
                        return false;
                    }
                }
                if (range.length == 2) {
                    if (tryParseLong(range[1], byteEndRef) == false) {
                        return false;
                    }
                }

                return true;
            }
        }

        return false;
    }

    public static String getHostName() {
        String hostName = "";
        try {
            InetAddress localMachine = InetAddress.getLocalHost();
            hostName = localMachine.getHostName();
        } catch (java.net.UnknownHostException uhe) {

        }

        return hostName;
    }

    /*
     * TODO: find some config-based way of doing this
     */
    public static String getJustMeHostName() {
        String ret = "just.me";

        try {
            String testJustMeAddr = InetAddress.getByName("test.just.me").getHostAddress();
            String justMeAddr = InetAddress.getByName("just.me").getHostAddress();

            if (testJustMeAddr.equals(LOOPBACK_ADDRESS)) {
                ret = "test.just.me";
            } else if (justMeAddr.equals(LOOPBACK_ADDRESS)) {
                ret = "just.me";
            }

            /*
             * InetAddress addrs[] = InetAddress.getAllByName(hostName);
             * 
             * for (InetAddress addr : addrs) { System.out.println(String.format("Hostname = %s", addr.getHostName())); }
             * 
             * InetAddress addr = InetAddress.getLocalHost();
             * 
             * // Get IP Address byte[] ipAddr = addr.getAddress();
             * 
             * // Get hostname String hostname = addr.getHostName(); System.out.println(String.format("Hostname = %s", hostname));
             */
        } catch (UnknownHostException e) {
        }

        return ret;
    }

    public static <T> int indexOf(T[] arr, T val) {
        int idx = -1;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == val) {
                idx = i;
                break;
            }
        }

        return idx;
    }

    public static int nextPseudoRandomInt() {
        return RANDOM.nextInt();
    }

    public static int nextPseudoRandomInt(int maxInt) {
        return RANDOM.nextInt(maxInt);
    }

    public static Double deltaTime(Long startTime) {
        Double deltaTimeSeconds = (double) (deltaTimeMs(startTime)) / 1000;
        return deltaTimeSeconds;
    }

    public static Long deltaTimeMs(Long startTime) {
        Long deltaTimeMs = DateUtils.nowAsTicks() - startTime;
        return deltaTimeMs;
    }

    public static String normalizeString(String s) {
        if (s == null) {
            s = "";
        }

        if (s.length() > 0) {
            s = Normalizer.normalize(s, Form.NFC).toLowerCase(Locale.ENGLISH);
        }

        return s;
    }

    public static String dumpArray(byte[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");

        if (array.length > 0) {
            for (byte b : array) {
                sb.append(b);
                sb.append(", ");
            }

            // remove the trailing ", ";
            sb.setLength(sb.length() - 2);
        }

        sb.append(" }");

        return sb.toString();
    }

    public static <T> String dumpArray(T[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");

        if (array.length > 0) {
            for (T t : array) {
                sb.append(t.toString() + ", ");
            }

            // remove the trailing ", ";
            sb.setLength(sb.length() - 2);
        }

        sb.append(" }");

        return sb.toString();
    }

    public static List<String> extractHashTags(String s) {
        List<String> hashTags = null;
        if (!Utils.isNullOrEmptyString(s)) {
            hashTags = new ArrayList<String>();

            Matcher matcher = HASHTAG_PATTERN.matcher(s);

            while (matcher.find()) {
                String hashTag = matcher.group(1).toLowerCase();
                hashTags.add(hashTag);
            }
        }

        return hashTags;
    }

    public static String removeHashTags(String s) {
        Matcher m = HASHTAG_PATTERN.matcher(s);

        return m.replaceAll(" ");
    }

    public static String preprocessCheapSearchIndex(String s) {
        Matcher m = SEARCH_PREPROCESSOR_PATTERN.matcher(s);

        return m.replaceAll(" ");
    }

    public static String expandWhiteSpace(String s) {
        Matcher m = WHITESPACE_PATTERN.matcher(s);

        return m.replaceAll(" ");
    }

    public static boolean isInitializedInjectedParameter(String s) {
        if (s == null || (s.startsWith("$") && s.endsWith("}"))) {
            return false;
        } else {
            return true;
        }
    }

    public static String generateRandomString(int length) {
        byte[] salt = new byte[length];
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(salt);

        return Base64.encodeBase64String(salt);
    }

    public static void safeDispose(Disposable d) {
        if (d != null) {
            try {
                d.dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String s = generateRandomString(32);
        System.out.println(s);

        String base62Udid = "30XOk3UDvT052idt5Js9y&C";
        Matcher m = BASE62_UDID_PATTERN.matcher(base62Udid);
        while (m.find()) {
            String id = normalizeString(m.group(1));
            System.out.println("id = " + id);
        }

    }

    private static final char[] TRIM_CHARS = new char[] { '"' };

    public static String conformAndEncodeKeywordsAndTags(List<String> terms) {
        List<String> encodedTerms = null;
        String queryToken = "";
        if (terms != null && !terms.isEmpty()) {
            // iterate over the closure.
            // encodedTerms = (List<String>) encodeWord.each(l);

            encodedTerms = new ArrayList<String>();

            for (String term : terms) {
                if (Utils.isNullOrEmptyString(term)) {
                    continue;
                }

                term = Utils.trim(term, TRIM_CHARS);

                // get rid of prefixes such as "via:" and "system:"
                int colonIndex = term.indexOf(':');
                if (colonIndex >= 0) {
                    term = term.substring(colonIndex + 1);
                }

                if (term.indexOf(' ') > 0) {
                    // this term is a phrase
                    term = String.format("\"%s\"", term);
                }

                if (term.length() > 0) {
                    encodedTerms.add(UrlUtils.encode(term));
                }
            }

            queryToken = join(encodedTerms, " AND ");
        }

        return queryToken;
    }

    public static String escapeQueryCulprits(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // These characters are part of the query syntax and must be escaped
            if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '[' || c == ']' || c == '\"' || c == '{'
                    || c == '}' || c == '~' || c == '/' || c == '*' || c == '?' || c == '|' || c == '&' || c == ';') {
                sb.append('\\');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    // http://stackoverflow.com/questions/6542332/java-compress-large-file
    public static void zipper(String[] filenames, String zipfile) {
        try {
            String outFilename = zipfile;
            ZipOutputStream out = new ZipOutputStream(new FileOutputStream(outFilename));
            for (int i = 0; i < filenames.length; i++) {
                FileInputStream in = new FileInputStream(filenames[i]);
                File file = new File(filenames[i]);
                out.putNextEntry(new ZipEntry(file.getName()));

                StreamUtils.writeStreamToStream(in, out);

                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (IOException e) {
        }
    }
}