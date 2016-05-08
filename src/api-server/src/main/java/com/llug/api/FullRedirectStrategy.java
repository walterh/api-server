package com.llug.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.util.UrlUtils;

import com.wch.commons.utils.Utils;


public class FullRedirectStrategy implements RedirectStrategy {

    protected final Log logger = LogFactory.getLog(getClass());

    private boolean contextRelative;

    /**
     * Redirects the response to the supplied URL.
     * <p>
     * If <tt>contextRelative</tt> is set, the redirect value will be the value after the request context path. Note
     * that this will result in the loss of protocol information (HTTP or HTTPS), so will cause problems if a
     * redirect is being performed to change to HTTPS, for example.
     */
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        String redirectUrl = calculateRedirectUrl(request, url);
        redirectUrl = response.encodeRedirectURL(redirectUrl);

        if (logger.isDebugEnabled()) {
            logger.debug("Redirecting to '" + redirectUrl + "'");
        }

        response.sendRedirect(redirectUrl);
    }

    private String calculateRedirectUrl(HttpServletRequest request, String url) {
    	String contextPath = request.getContextPath();
    	
        // make sure all the qs are copied over.
        String qs = request.getQueryString();
        
        if (!Utils.isNullOrEmptyString(qs)) {
        	url = String.format("%s?%s", url, qs);
        }

        if (!UrlUtils.isAbsoluteUrl(url)) {
            if (contextRelative) {
                return url;
            } else {
                return contextPath + url;
            }
        }

        // Full URL, including http(s)://

        if (!contextRelative) {
            return url;
        }

        // Calculate the relative URL from the fully qualified URL, minus the scheme and base context.
        url = url.substring(url.indexOf("://") + 3); // strip off scheme
        url = url.substring(url.indexOf(contextPath) + contextPath.length());

        if (url.length() > 1 && url.charAt(0) == '/') {
            url = url.substring(1);
        }        

        return url;
    }

    /**
     * If <tt>true</tt>, causes any redirection URLs to be calculated minus the protocol
     * and context path (defaults to <tt>false</tt>).
     */
    public void setContextRelative(boolean useRelativeContext) {
        this.contextRelative = useRelativeContext;
    }
}