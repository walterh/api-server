package com.wch.commons.utils;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.extern.slf4j.Slf4j;

import org.restlet.data.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InternetDomainName;

@Slf4j
public class UrlUtils {
    private static final String HTTP_PROCOTOL = "http://";
    private static final String HTTPS_PROCOTOL = "https://";
    private static final String[] HOMEPAGE_SUFFIXES = { "/index.htm",
            "/index.html",
            "/index",
            "/list",
            "/index.asp",
            "/index.jsp",
            "/index.do",
            "/home",
            "/homepage" };
    private static final String listPatternStr = "/(welcome|home|homepage|(list|index)\\.*(html|htm|asp|jsp|do)*)";
    private static final String[] SPECIAL_HOSTS = { "http://dayzdb.com/map#", "https://maps.google.com/", "http://www.foodnetwork.com/" };
    private static final String kindvprov = "aero|asia|biz|cat|com|co|coop|edu|gov|info|int|jobs|mil|mobi" + "|museum|name|net|org|pro|tel|travel";
    private static final String nations = "ac|ad|ae|af|ag|ai|al|am|ao|aq|ar|as|at|au|aw|ax|az|an"
            + "|ba|bb|bd|be|bf|bg|bh|bi|bj|bl|bq|bm|bn|bo|br|bs|bt|bw|bv|bu|by|bz" + "|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|cr|cv|cs|cu|cw|cx|cy|cz"
            + "|de|dj|dk|dm|do|dz|dd" + "|ec|ee|eg|er|es|et|eu|eh" + "|fi|fj|fk|fo|fr|fm" + "|ga|gb|gd|ge|gf|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|gy"
            + "|hk|hm|hn|hr|ht|hu" + "|id|ie|il|im|in|io|iq|ir|is|it" + "|je|jm|jo|jp" + "|ke|kg|kh|ki|km|kn|kp|kr|ky|kw|kz"
            + "|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly" + "|ma|mc|md|me|mf|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|my|mz"
            + "|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz" + "|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py" + "|qa|re|ro|rs|ru|rw"
            + "|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|ss|st|sv|sx|sy|sz|su" + "|tc|td|tf|tg|th|tj|tk|tl|tm|tn|to|tr|tt|tw|tz|tp"
            + "|ua|ug|uk|us|uy|uz|um|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw|zr";
    private static final String shortPatternStr = "^(\\w+\\.)*(\\w+\\.(" + kindvprov + "|" + nations + "))$";
    private static final String longPatternStr = "^(\\w+\\.)*(\\w+\\.(" + kindvprov + ")(\\.(" + nations + ")))$";
    private static final Pattern listPattern = Pattern.compile(listPatternStr, Pattern.CASE_INSENSITIVE);
    private static final Pattern shortPattern = Pattern.compile(shortPatternStr, Pattern.CASE_INSENSITIVE);
    private static final Pattern longPattern = Pattern.compile(longPatternStr, Pattern.CASE_INSENSITIVE);

    public static boolean isHomepage(String url) {
        if (null == url || url.trim().isEmpty()) {
            return false;
        }
        String tmpUrl = url.trim().toLowerCase();
        if (!tmpUrl.startsWith("http")) {
            tmpUrl = HTTP_PROCOTOL + tmpUrl;
        }
        for (String specialHost : SPECIAL_HOSTS) {
            if (tmpUrl.startsWith(specialHost)) {
                return true;
            }
        }
        try {
            URI uri = new URI(tmpUrl);
            String path = uri.getPath();
            if (null == path || path.isEmpty() || path.equals("/")) {
                return true;
            }
            Matcher matcher = listPattern.matcher(path);
            if (matcher.matches()) {
                return true;
            }
            if (path.endsWith("/")) {
                String parts[] = path.split("/");
                int validCount = 0;
                for (String part : parts) {
                    if (part.trim().isEmpty()) {
                        continue;
                    }
                    validCount++;
                }
                return validCount <= 1;
            }
        } catch (Exception ex) {
            log.warn("failed to parse url:{}, cause:{}", url, ex.getMessage());
        }
        return false;
    }

    public static String getHostFromUrl(String url) {
        String host = null;
        try {
            url = url.toLowerCase();
            URI uri = new URI(url);
            host = uri.getHost();
        } catch (Exception e) {
            List<String> parts = Utils.stringSplitRemoveEmptyEntries(url, "[:/]", true, true);
            if (parts != null) {
                if (parts.size() == 1) {
                    host = parts.get(0);
                } else if (parts.size() > 1) {
                    // http or https
                    if (parts.get(0).startsWith("http")) {
                        parts.remove(0);
                    }

                    host = parts.get(0);
                } else {
                    host = url;
                }
            } else {
                host = url;
            }
        }
        return host;
    }

    public static String getDomainFromHost(String host) {
        Matcher matcher = longPattern.matcher(host);
        if (matcher.matches()) {
            return matcher.group(2);
        } else {
            matcher = shortPattern.matcher(host);
            if (matcher.matches()) {
                return matcher.group(2);
            }
        }
        return host;
    }

    public static String encode(String s) {
        // we need this due to Spring's inability to deal "." in paths
        // with http://stackoverflow.com/questions/4135329/how-to-change-spring-mvcs-behavior-in-handling-url-dot-character
        if (!Utils.isNullOrEmptyString(s)) {
            try {
                return URLEncoder.encode(s, "UTF-8").replace(".", "%2E");
            } catch (UnsupportedEncodingException e) {
                // bullsh!t java...
            }
        }

        return "";
    }

    public static String decode(String s) {
        try {
            return URLDecoder.decode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // bullsh!t java...
        }

        return null;
    }

    public static Map<String, String> splitQuery(URL url) throws UnsupportedEncodingException {
        Map<String, String> query_pairs = new LinkedHashMap<String, String>();
        String query = url.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"), URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return query_pairs;
    }

    public static String toQueryString(final Map<String, String> params) {
        if (params != null && params.size() >= 1) {
            StringBuilder sb = new StringBuilder();

            for (final String name : params.keySet()) {
                sb.append(String.format("&%s=%s", name, UrlUtils.encode(params.get(name))));
            }
            return sb.toString().substring(1);
        } else {
            return "";
        }
    }

    public static String canonalizeUrl(final String taintedURL) {
        try {
            // Port from python code:
            String originalURL = taintedURL.replace(" ", "_").replace("\"", "_").replace("'", "_").replace("script%3", "script_").replace("script>", "script_");

            // TODO:
            // 1. decode unreserved char
            // 2. Capitalizing letters in escape sequences
            URI uri = new URI(originalURL);
            Reference ref = new Reference(uri);
            ref.normalize();
            String scheme = ref.getScheme();
            String useInfo = ref.getUserInfo();
            String host = ref.getHostDomain();
            int port = ref.getHostPort();
            String path = ref.getPath();
            if (null == path || path.isEmpty()) {
                path = "/";
            }
            String query = ref.getQuery();
            String fragment = ref.getFragment();
            if (scheme == null || host == null || path == null || host.isEmpty() || scheme.isEmpty()) {
                log.error("Url Format error, url: {}", taintedURL);
                return null;
            }
            return Reference.toString(scheme, host, path, query, fragment);
        } catch (URISyntaxException e) {
            log.error("Url Normalize Error, Ex: {}", e.getStackTrace());
        }
        return null;
    }

    // http://stackoverflow.com/questions/1923815/get-the-second-level-domain-of-an-url-java
    public static String extractDomain(String url) {
        String secondLevelDomain = null;
        String host = getHostFromUrl(url);

        try {
            InternetDomainName fullDomainName = InternetDomainName.from(host);
            secondLevelDomain = fullDomainName.topPrivateDomain().toString();
        } catch (Exception e) {
            // bad domain...wtf should we do?
            secondLevelDomain = host;
        }

        return secondLevelDomain;
    }

    public static String extractPath(String url) {
        String path = null;

        if (url.startsWith("//")) {
            // skip over it
            path = url.substring(url.indexOf("/", 3));
        } else {
            try {
                URL u = new URL(url);
                path = u.getPath();
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return path;
    }

    public static String ensureUrl(String url) {
        if (url.startsWith("//")) {
            return "http:" + url;
        } else {
            return url;
        }
    }

    public static void main(String[] args) {
        extractPath("//d13hnvdwbvemyo.cloudfront.net/7f/7f39f8317fbdb1988ef4c628eba02591/_jXI3gHI/s10.mb640x360.mp4");

        System.out.println(decode("asdf+sadf"));
        String urls[] = { "http://www.yahoo.com",
                "http://www.yahoo.com/",
                "http://www.yahoo.com/index",
                "http://www.yahoo.com/list.jsp",
                "http://www.yahoo.com/index.html",
                "http://www.yahoo.com/index.htm",
                "http://www.yahoo.com/index.jsp",
                "http://www.yahoo.com/index.asp",
                "http://www.yahoo.com/index.do",
                "http://www.wired.com/gadgetlab/2012/12/facebook-privacy-update/",
                "https://www.wired.com/",
                "http://news.163.com/photo/",
                "http://mashable.com/2012/12/12/crime-social-media/",
                "http://www.ifanr.com/216290",
                "http://www.yahoo.com/sports/",
                "http://techblog.zabuchy.net/" };
        for (String url : urls) {
            System.out.println(url + " is homepage: " + isHomepage(url));
        }
    }
}
