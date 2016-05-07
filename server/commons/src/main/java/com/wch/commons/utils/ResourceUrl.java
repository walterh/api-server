package com.wch.commons.utils;

import static ch.lambdaj.Lambda.*;
import static org.hamcrest.Matchers.startsWith;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.lambdaj.function.closure.Closure1;
import ch.lambdaj.function.closure.Closure3;

public class ResourceUrl implements Cloneable {
    private static Logger logger = LoggerFactory.getLogger(ResourceUrl.class);

    public static final String FILE_SCHEME_HEADER1 = "file://";
    public static final String FILE_SCHEME_HEADER2 = "file:";
    public static final String FILE_SCHEME = "file";

    public static final String S3_SCHEME_HEADER = "s3://";
    public static final String S3_SCHEME = "s3";

    public static final String S3N_SCHEME_HEADER = "s3n://";
    public static final String S3N_SCHEME = "s3n";

    public static final String HDFS_SCHEME_HEADER = "hdfs://";
    public static final String HDFS_SCHEME = "hdfs";

    public static final String MONGO_SCHEME_HEADER = "mongo://";
    public static final String MONGO_SCHEME = "mongo";

    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^(s3|s3n|hdfs|mongo|file):/{0,2}/[\\w@%\\-\\./%]+$");
    //Pattern.compile("^(s3|s3n|hdfs|mongo|file)://|(file):");
    private static final Pattern FILE_RESOURCE_PATTERN = Pattern.compile("^(file|hdfs):/{0,2}(/[\\w%\\-\\./]+)$");
    //Pattern.compile("^((file)://|(file):)(/[\\w\\-\\./]+)$");
    private static final Pattern BUCKET_RESOURCE_PATTERN = Pattern.compile("^(s3|s3n|mongo):/{2,3}([\\w\\-]+)/([\\w@%\\-\\./]+)$");

    public enum Scheme {
        s3, s3n, hdfs, mongo, file
    };

    private Scheme scheme;
    // some schemes (like mongo, hdfs, and s3) have buckets.  file doesn't
    private String bucket;
    private String uri;

    public Scheme getScheme() {
        return scheme;
    }

    public String getBucket() {
        return bucket;
    }

    public String getUri() {
        return uri;
    }

    public ResourceUrl(Scheme scheme, String uri) {
        this(scheme, null, uri);
    }

    public ResourceUrl(Scheme scheme, String bucket, String uri) {
        super();
        this.scheme = scheme;
        this.bucket = bucket;

        if (scheme != Scheme.file && uri != null && uri.startsWith("/")) {
            uri = uri.substring(1);
        }

        this.uri = uri;
    }

    public ResourceUrl(String uri) {
        this(null, null, uri);
    }

    public ResourceUrl collapse() {
        Boolean startsWithSlash = uri.startsWith("/");
        Boolean endsWithSlash = uri.endsWith("/");

        List<String> uriComps = Utils.stringSplitRemoveEmptyEntries(uri, "/", false, false);

        int size = uriComps.size();
        for (int i = 0; i < size; i++) {
            if (uriComps.get(i).compareTo(".") == 0) {
                uriComps.remove(i);

                // recalculate the size
                size = uriComps.size();
            } else if (uriComps.get(i).compareTo("..") == 0) {
                uriComps.remove(i);

                if (i > 1) {
                    uriComps.remove(i - 1);
                    i -= 2;
                }

                size = uriComps.size();
            }
        }

        uri = String.format("%s%s%s", startsWithSlash ? "/" : "", joinFrom(uriComps, "/"), endsWithSlash ? "/" : "");

        return this;
    }

    @Override
    public Object clone() {
        return new ResourceUrl(scheme, bucket, uri);
    }

    @Override
    public String toString() {
        if (scheme == null) {
            return uri;
        } else if (scheme == Scheme.file || scheme == Scheme.hdfs) {
            return String.format("%s://%s", scheme, uri);
        } else {
            return String.format("%s://%s/%s", scheme, bucket, uri);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((bucket == null) ? 0 : bucket.hashCode());
        result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
        result = prime * result + ((uri == null) ? 0 : uri.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResourceUrl other = (ResourceUrl) obj;
        if (bucket == null) {
            if (other.bucket != null)
                return false;
        } else if (!bucket.equals(other.bucket))
            return false;
        if (scheme != other.scheme)
            return false;
        if (uri == null) {
            if (other.uri != null)
                return false;
        } else if (!uri.equals(other.uri))
            return false;
        return true;
    }

    public void replaceLast(String a, String b) {
        uri = FileUtils.replaceLast(uri, a, b);
    }

    public String getFileName() {
        return FileUtils.getFileName(this.uri);
    }

    public String getFileNameSansExtension() {
        return FileUtils.getFileNameSansExtension(this.uri);
    }

    public ResourceUrl asDirectory() {
        if (!Utils.isNullOrEmptyString(uri) && !uri.endsWith("/")) {
            return new ResourceUrl(getScheme(), getBucket(), FileUtils.getDirectory(getUri()));
        } else {
            return this;
        }
    }

    public ResourceUrl embedTag(final String tag) {
        this.uri = FileUtils.embedTag(this.uri, tag);

        return this;
    }

    public boolean fixBadDeviceId() {
        List<String> uriParts = Utils.stringSplitNonRegex(uri, "/");
        if (!Utils.isNullOrEmpty(uriParts) && uriParts.size() == 4 && !Utils.isNullOrEmptyString(uriParts.get(0)) && uriParts.get(0).length() == 2
                && !Utils.isNullOrEmptyString(uriParts.get(1)) && uriParts.get(1).length() == 32) {

            final String twoChars = uriParts.get(0).toUpperCase();
            final String s = uriParts.get(1);
            final String reUuid = IdUtils.reUUID(s).toUpperCase();

            if (!Utils.isNullOrEmptyString(reUuid)) {
                uri = String.format("%s/%s/%s/%s", twoChars, reUuid, uriParts.get(2), uriParts.get(3));

                return true;
            }
        }

        return false;
    }

    public static ResourceUrl parse(String s) {
        ResourceUrl parsedResource = null;
        Scheme scheme = null;
        String uri = null;
        String bucket = null;

        if (s.startsWith(FileUtils.separator)) {
            // assume this is a file resource
            s = String.format("%s://%s", Scheme.file, s);
        }

        Matcher m = RESOURCE_PATTERN.matcher(s);

        if (m.matches()) {
            scheme = Utils.safeParseEnum(Scheme.class, m.group(1));

            if (scheme == Scheme.file || scheme == Scheme.hdfs) {
                Matcher m2 = FILE_RESOURCE_PATTERN.matcher(s);

                m2.find();
                uri = m2.group(2);
            } else {
                Matcher m3 = BUCKET_RESOURCE_PATTERN.matcher(s);
                m3.find();
                bucket = m3.group(2);
                uri = m3.group(3);
            }

            parsedResource = new ResourceUrl(scheme, bucket, uri);
        }

        return parsedResource;
    }

    public static List<ResourceUrl> parse(final List<String> uriList) {
        final List<ResourceUrl> resourceUrls = (List<ResourceUrl>) closure().of(ResourceUrl.class, "parse", var(String.class)).each(uriList);

        return resourceUrls;
    }

    public static List<ResourceUrl> asResourceUrlList(final Scheme scheme, final String bucket, final List<String> keys) {
        // create resource url's out of these
        final Closure3<Scheme, String, String> c = closure(Scheme.class, String.class, String.class).of(ResourceUrl.class,
                "<init>",
                var(Scheme.class),
                var(String.class),
                var(String.class));

        // this order works properly...
        final Closure1<String> f = c.curry2(bucket).curry1(scheme);
        final List<ResourceUrl> sourceUrls = (List<ResourceUrl>) f.each(keys);

        return sourceUrls;
    }

    public static void printMatches(Matcher matcher) {
        StringBuilder sb = new StringBuilder();

        sb.append("\nMatches:\n");
        while (matcher.find()) {
            // Get all groups for this match
            for (int i = 0; i <= matcher.groupCount(); i++) {
                String groupStr = matcher.group(i);

                sb.append(String.format("\t%s\n", groupStr));
            }
        }
        logger.info(sb.toString());
        System.out.println(sb.toString());
    }

    public static List<Pair<ResourceUrl, Long>> getSortedFragmentKeysAndSizes(final ResourceUrl remoteSrcUrl, final List<Pair<ResourceUrl, Long>> keySizeListing) {
        final List<Pair<ResourceUrl, Long>> fragmentKeys = select(keySizeListing,
                having(on(Pair.class).getLeft().toString(), startsWith(remoteSrcUrl.toString())));

        if (!Utils.isNullOrEmpty(fragmentKeys)) {
            if (fragmentKeys.size() > 1) {
                // sort based on the fragment number at the end
                Collections.sort(fragmentKeys, new Comparator<Pair<ResourceUrl, Long>>() {

                    public int compare(Pair<ResourceUrl, Long> arg0, Pair<ResourceUrl, Long> arg1) {
                        Integer frag1 = Integer.parseInt(FileUtils.getExtension(arg0.getLeft().toString()));
                        Integer frag2 = Integer.parseInt(FileUtils.getExtension(arg1.getLeft().toString()));

                        return frag1.compareTo(frag2);
                    }
                });
            }
        }

        return fragmentKeys;
    }

    public static List<String> getSortedFragmentKeys(final ResourceUrl remoteSrcUrl, final List<String> keys) {
        final List<String> fragmentKeys = select(keys, having(on(Object.class).toString(), startsWith(remoteSrcUrl.toString())));

        if (!Utils.isNullOrEmpty(fragmentKeys)) {
            if (fragmentKeys.size() > 1) {
                // sort based on the fragment number at the end
                Collections.sort(fragmentKeys, new Comparator<String>() {

                    public int compare(String arg0, String arg1) {
                        Integer frag1 = Integer.parseInt(FileUtils.getExtension(arg0));
                        Integer frag2 = Integer.parseInt(FileUtils.getExtension(arg1));

                        return frag1.compareTo(frag2);
                    }
                });
            }
        }

        return fragmentKeys;
    }
}
