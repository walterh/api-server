package com.llug.api.utils;

import static ch.lambdaj.Lambda.closure;
import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static ch.lambdaj.Lambda.var;
import static org.hamcrest.Matchers.equalTo;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.hibernate.QueryException;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.restlet.data.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.multipart.MultipartFile;

import com.llug.api.ApiServerException;
import com.llug.api.domain.Account;
import com.llug.api.domain.ApiResponse;
import com.llug.api.domain.ApiResponse.ApiVersion;
import com.llug.api.domain.ApiResponse.ResultCode;
import com.llug.api.service.AuthenticationService;
import com.wch.commons.utils.DateUtils;
import com.wch.commons.utils.LogUtils;
import com.wch.commons.utils.OSValidator;
import com.wch.commons.utils.EmailUtils;
import com.wch.commons.utils.UrlUtils;
import com.wch.commons.utils.Utils;

import ch.lambdaj.function.closure.Closure;

@Slf4j
public class RequestUtils {
    public static ApiVersion getApiVersion(HttpServletRequest request) {
        String path = request.getRequestURI();

        return getApiVersion(path);
    }

    public static ApiVersion getApiVersion(String path) {
        if (path.startsWith(String.format("/api/%s/", ApiVersion.v1)) || !path.startsWith("/api/")) {
            return ApiVersion.v1;
        } else {
            return ApiVersion.v0;
        }
    }

    public static void controllerExceptionFilter(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationService authenticationService,
            ApiResponse api,
            Exception e) {

        if (e instanceof BadCredentialsException) {
            api.setStatus(ResultCode.invalid_credentials);
            api.setError(e.getMessage());
        } else if (e instanceof SessionAuthenticationException) {
            api.setStatus(ResultCode.expired_session);
            api.setError(e.getMessage());
        } else if (e instanceof ApiServerException) {
            ApiServerException wsex = (ApiServerException) e;
            api.setStatus(wsex.getResultCode());

            if (!Utils.isNullOrEmptyString(wsex.getMessage())) {
                api.setError(wsex.getMessage());
            }
        } else {
            final String stackTrace = LogUtils.getStackTrace(e);
            final String extendedError = getExtendedRequestErrorInfo(request, authenticationService, e);

            api.setStatus(ResultCode.internal_server_failure);
            api.setError(e.getMessage());
            api.setStacktrace(stackTrace);

            log.error(extendedError);

            /*
            if (!OSValidator.isMac()) {
                EmailUtils.sendSslMessage(null, String.format("Unhandled Exception:  %s", e.getMessage()), extendedError, null);
            }
            */
        }
        /*      
                // for the benefit of backbone.js...
                try {
                    response.setStatus(400);
                } catch (Exception ex) {
                    //wtf...
                    // doesn't seem to have an effect...400 is set and Content-Encoding is gzip
                    // http://dev.eclipse.org/mhonarc/lists/jetty-dev/msg01339.html
                    //System.out.println(LogUtils.getStackTrace(ex));
                }
        */
    }

    public static boolean isSecureRequest(NativeWebRequest request) {
        // regular https
        return request.isSecure() ||
        // under ELB
                ("https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) && "443".equals(request.getHeader("X-Forwarded-Port")));
    }

    public static boolean isSecureRequest(HttpServletRequest request) {
        // regular https
        return request.isSecure() || request.getScheme().equalsIgnoreCase("https") ||
        // under ELB
                ("https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto")) && "443".equals(request.getHeader("X-Forwarded-Port")));
    }

    public static String getOriginatingIp(HttpServletRequest request) {
        String ip = null;

        if (request != null) {
            ip = request.getHeader("REMOTE_ADDR");

            if (Utils.isNullOrEmptyString(ip)) {
                ip = request.getHeader("X-Forwarded-For");
            }

            if (Utils.isNullOrEmptyString(ip)) {
                ip = "127.0.0.1";
            }
        }

        List<String> ips = Utils.stringSplitRemoveEmptyEntries(ip, ",", true, true);

        return ips.get(0);
    }

    private static final Pattern NONCE_PATTERN = Pattern.compile("&_=[\\d]+");

    public static String getUrl(HttpServletRequest req) {
        String reqUrl = "";

        if (req != null) {
            reqUrl = req.getRequestURL().toString();
            String queryString = req.getQueryString(); // d=789
            if (queryString != null) {
                reqUrl += "?" + queryString;
            }
        }

        return reqUrl;
    }

    public static String getHost(HttpServletRequest req) {
        try {
            return new URL(getUrl(req)).getHost();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    public static String getPathAndQuery(HttpServletRequest req, boolean removeNonce) {
        String pathAndQuery = "";

        if (req != null) {
            String path = req.getRequestURI();
            String queryString = req.getQueryString(); // d=789
            if (queryString != null) {
                if (removeNonce) {
                    queryString = NONCE_PATTERN.matcher(queryString).replaceAll("");
                }

                pathAndQuery = path + "?" + queryString;
            } else {
                pathAndQuery = path;
            }
        }

        return pathAndQuery;
    }

    public static String getParamOrCookieValue(final HttpServletRequest request, final String param) {
        String key = null;

        if (request != null) {
            // look for it in the querystring
            key = request.getParameter(param);

            if (Utils.isNullOrEmptyString(key)) {
                // look for it in a cookie
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    List<Cookie> c = select(Utils.toList(cookies), having(on(Cookie.class).getName(), equalTo(param)));

                    if (c != null && c.size() > 0) {
                        key = c.get(0).getValue();
                    }
                }
            }
        }

        return key;
    }

    public static String getExtendedRequestErrorInfo(HttpServletRequest request, AuthenticationService authenticationService, Exception e) {
        String username = "***unauthenticated***";
        String pwdhash = username;
        String extendedInfo = null;
        boolean authenticatedRequest = false;

        try {
            Object o = authenticationService.getAuthenticatedUser();
            if (o instanceof Account) {
                Account u = (Account) o;

                username = u.getUsername();
                pwdhash = u.getPassword();
                authenticatedRequest = true;
            }
        } catch (Exception ex) {
            // unauthenticated request
        }

        extendedInfo = String.format("Internal Server Error *****************************************\nRequest %s\nRequest GET %s\nUser = %s\nHash = %s\n",
                request.getMethod().equals("GET") ? getUrl(request) : getAllParameters(request, true, false),
                getAllParameters(request, false, false),
                username,
                pwdhash);

        if (authenticatedRequest) {
            extendedInfo = extendedInfo
                    + String.format("Convenient Auth Url = %s\n",
                            String.format("https://api.llug.com/api/v1/account/login?email=%s&password=%s", username, UrlUtils.encode(pwdhash)));
        }

        if (e != null) {
            extendedInfo = extendedInfo + String.format("Exception = %s", LogUtils.getStackTrace(e));
        }

        return extendedInfo;
    }

    public static MultipartFile clean(MultipartFile file) {
        return (file != null && file.getSize() > 0) ? file : null;
    }

    public static String getPostData(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader = req.getReader();
            reader.mark(10000);

            String line;
            do {
                line = reader.readLine();

                // otherwise we'll capture the last null of the string
                if (!Utils.isNullOrEmptyString(line)) {
                    sb.append(line).append("\n");
                }
            } while (line != null);
            reader.reset();
            // do NOT close the reader here, or you won't be able to get the post data twice
        } catch (IOException e) {
            log.warn("getPostData couldn't get the post data", e); // This has happened if the request's reader is closed    
        }

        return sb.toString();
    }

    public static String getAllHeaders(HttpServletRequest request) {
        final Map<String, String> headers = new HashMap<String, String>();
        final StringBuilder sb = new StringBuilder();
        final Enumeration<String> e = request.getHeaderNames();

        sb.append(String.format("headers for %s\n", getUrl(request)));

        while (e.hasMoreElements()) {
            final String header = (String) e.nextElement();
            if (header != null) {
                headers.put(header, request.getHeader(header));
                sb.append(String.format("\t%s: %s\n", header, request.getHeader(header)));
            }
        }

        return sb.toString();
    }

    public static Map<String, String> getAllHeaders(HttpServletRequest request, boolean dump) {
        final Map<String, String> headers = new HashMap<String, String>();
        final StringBuilder sb = new StringBuilder();
        final Enumeration<String> e = request.getHeaderNames();

        sb.append(String.format("headers for %s\n", getUrl(request)));

        while (e.hasMoreElements()) {
            final String header = (String) e.nextElement();
            if (header != null) {
                headers.put(header, request.getHeader(header));
                sb.append(String.format("\t%s: %s\n", header, request.getHeader(header)));
            }
        }

        if (dump) {
            log.warn(sb.toString());
        }

        return headers;
    }

    public static String getAllParameters(final HttpServletRequest request, final boolean asPost, final boolean dump) {
        final StringBuilder sb = new StringBuilder();
        final Enumeration<String> e = request.getParameterNames();

        sb.append(String.format("parameters for %s\n", getUrl(request)));

        while (e.hasMoreElements()) {
            final String param = (String) e.nextElement();
            if (param != null) {
                if (asPost) {
                    sb.append(String.format("\t%s: %s\n", param, request.getParameter(param)));
                } else {
                    // asGet
                    sb.append(String.format("&%s=%s", param, UrlUtils.encode(request.getParameter(param))));
                }
            }
        }

        if (dump) {
            log.warn(sb.toString());
        }

        return sb.toString();
    }

    public static String deHtml(String s) {
        if (!Utils.isNullOrEmptyString(s)) {
            s = Jsoup.parse(s).text();
        }

        return s;
    }

    // http://stackoverflow.com/questions/7023994/removing-html-tags-except-few-specific-ones-from-string-in-java
    public static String cleanHtml(String s) {
        Whitelist whitelist = Whitelist.none();
        whitelist.addTags(new String[] { "p", "br", "ul", "div", "span", "a", "h1", "h2" });

        if (!Utils.isNullOrEmptyString(s)) {
            s = Jsoup.clean(s, whitelist);
        }

        return s;
    }

    public static String urlDecodeClean(String s) {
        try {
            if (s != null && s.contains("%")) {
                s = URLDecoder.decode(s, "UTF-8");
            }
        } catch (Exception e) {
            // ignore errors
        }
        return s;
    }

    public static Long clean(Long anchor) {
        Long now = DateUtils.nowAsTicks();
        // convert to epoch seconds
        now = now / 1000;

        if (anchor == null || anchor < 0 || anchor > now) {
            anchor = now;
        }

        return anchor;
    }

    public static Long cleanMs(Long anchorMs) {
        Long now = DateUtils.nowAsTicks();

        if (anchorMs == null || anchorMs < 0 || anchorMs > now) {
            anchorMs = now;
        }

        return anchorMs;
    }

    public static Integer clean(Integer offset) {
        if (offset == null || offset < 0) {
            offset = 0;
        }

        return offset;
    }

    public static Long clean(Long limit, Long maxLimit) {
        if (limit == null || limit > maxLimit) {
            limit = maxLimit;
        }

        return limit;
    }

    public static Integer clean(Integer limit, Integer maxLimit) {
        if (limit == null || limit > maxLimit) {
            limit = maxLimit;
        }

        return limit;
    }

    public static Boolean clean(Boolean bool, Boolean def) {
        if (bool == null) {
            bool = def;
        }

        return bool;
    }

    public static <T extends Enum<T>> T cleanEnum(T t, T defaultT) {
        if (t == null) {
            t = defaultT;
        }

        return t;
    }

    public static List<String> clean(List<String> l) {
        if (l != null) {
            Set<String> strs = new HashSet<String>();
            for (String s : l) {
                if (s != null) {
                    s = s.trim();
                }

                if (!Utils.isNullOrEmptyString(s)) {
                    strs.add(s);
                }
            }

            if (strs.size() > 0) {
                return new ArrayList<String>(strs);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public static String getApiStringForHash(final HttpServletRequest request) {
        final StringBuilder sb = new StringBuilder();
        final Enumeration<String> e = request.getParameterNames();
        final List<String> l = Collections.list(e);

        Collections.sort(l);

        for (final String key : l) {
            final String val = request.getParameter(key);

            if (!Utils.isNullOrEmptyString(val)) {
                sb.append(String.format("&%s=%s", key, UrlUtils.encode(val)));
            }
        }

        if (sb.length() > 0) {
            // get rid of the first "&"
            sb.replace(0, 1, "");
        }

        // add the uri up at front
        sb.insert(0, String.format("%s?", request.getRequestURI()));

        return sb.toString();
    }
}
