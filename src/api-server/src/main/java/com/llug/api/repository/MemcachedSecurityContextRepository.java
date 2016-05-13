package com.llug.api.repository;

import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.joinFrom;
import static ch.lambdaj.Lambda.on;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SaveContextOnUpdateOrErrorResponseWrapper;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Repository;

import com.wch.commons.utils.Utils;

@Slf4j
@Repository
public class MemcachedSecurityContextRepository implements SecurityContextRepository {
    private static final String PFX = "ssc"; // for SPRING_SECURITY_CONTEXT

    @Value("$api{sessionCookieName}")
    private String sessionCookieName;

    @Value("$api{memcache.session.blacklist}")
    private List<String> sessionBlacklist;

    @Inject
    MemcacheRepository memcacheRepository;

    HttpSessionSecurityContextRepository foo;

    private final Object contextObject = SecurityContextHolder.createEmptyContext();
    private boolean allowSessionCreation = true;
    private boolean disableUrlRewriting = false;

    private final AuthenticationTrustResolver authenticationTrustResolver = new AuthenticationTrustResolverImpl();

    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        //Request baseRequest = HttpConnection.getCurrentConnection().getRequest();
        //baseRequest.setSession(null);

        HttpServletRequest request = requestResponseHolder.getRequest();
        HttpServletResponse response = requestResponseHolder.getResponse();
        HttpSession httpSession = request.getSession(false);
        /*
                Cookie[] cookies = request.getCookies();
                if (cookies != null && cookies.length > 0) {
                    List<Cookie> cl = Utils.asList(cookies);
                    List<String> cookieNames = extract(cl, on(Cookie.class).getName());

                    if (logger.isDebugEnabled()) {
                        logger.debug(String.format("Cookies = %s", joinFrom(cookieNames, ",")));
                    }
                }
        */
        SecurityContext context = readSecurityContextFromSession(httpSession);

        if (context == null) {
            log.debug("No SecurityContext was available from the HttpSession: " + httpSession + ". " + "A new one will be created.");
            context = generateNewContext();
        }

        requestResponseHolder.setResponse(new SaveToSessionResponseWrapper(response, request, httpSession != null, context));

        return context;
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        if (path == null) {
            log.error("path is null");
        }
        SaveToSessionResponseWrapper responseWrapper = (SaveToSessionResponseWrapper) response;
        // saveContext() might already be called by the response wrapper
        // if something in the chain called sendError() or sendRedirect(). This ensures we only call it
        // once per request.
        if (responseWrapper.contextChanged(context) && !responseWrapper.isContextSaved() && !sessionBlacklist.contains(path)) {
            responseWrapper.saveContext(context);
        }
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        String key = getContextKey(request);

        return memcacheRepository.exists(key);
    }

    private SecurityContext readSecurityContextFromSession(HttpSession httpSession) {
        if (httpSession == null) {
            log.debug("No HttpSession currently exists");

            return null;
        }

        // Session exists, so try to obtain a context from it.
        String key = getContextKey(httpSession.getId());
        Object contextFromSession = memcacheRepository.get(key);

        if (contextFromSession == null) {
            log.debug("HttpSession returned null object for SPRING_SECURITY_CONTEXT");

            return null;
        }

        // We now have the security context object from the session.
        if (!(contextFromSession instanceof SecurityContext)) {
            log.warn(key + " did not contain a SecurityContext but contained: '" + contextFromSession
                        + "'; are you improperly modifying the HttpSession directly "
                        + "(you should always use SecurityContextHolder) or using the HttpSession attribute " + "reserved for this class?");

            return null;
        }

        log.debug("Obtained a valid SecurityContext from " + key + ": '" + contextFromSession + "'");

        // Everything OK. The only non-null return from this method.

        return (SecurityContext) contextFromSession;
    }

    protected SecurityContext generateNewContext() {
        return SecurityContextHolder.createEmptyContext();
    }

    private String getContextKey(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null) {
            // see if we can grab from qs
            String id = request.getParameter(sessionCookieName);
            if (!Utils.isNullOrEmptyString(id)) {
                return getContextKey(id);
            } else {
                // nope.  get session and allow creates...
                session = request.getSession();
            }
        }

        return getContextKey(session.getId());
    }

    private String getContextKey(String sessionId) {
        String key = String.format("%s:%s", PFX, sessionId);

        return key;
    }

    public void setAllowSessionCreation(boolean allowSessionCreation) {
        this.allowSessionCreation = allowSessionCreation;
    }

    public void setDisableUrlRewriting(boolean disableUrlRewriting) {
        this.disableUrlRewriting = disableUrlRewriting;
    }

    final class SaveToSessionResponseWrapper extends SaveContextOnUpdateOrErrorResponseWrapper {

        private final HttpServletRequest request;
        private final boolean httpSessionExistedAtStartOfRequest;
        private final Integer contextHashBeforeExecution;
        private final Integer authHashBeforeExecution;
        private final Integer principalHashBeforeExecution;

        /**
         * Takes the parameters required to call <code>saveContext()</code> successfully in
         * addition to the request and the response object we are wrapping.
         *
         * @param request the request object (used to obtain the session, if one exists).
         * @param httpSessionExistedAtStartOfRequest indicates whether there was a session in place before the
         *        filter chain executed. If this is true, and the session is found to be null, this indicates that it was
         *        invalidated during the request and a new session will now be created.
         * @param context the context before the filter chain executed.
         *        The context will only be stored if it or its contents changed during the request.
         */
        SaveToSessionResponseWrapper(HttpServletResponse response,
                HttpServletRequest request,
                boolean httpSessionExistedAtStartOfRequest,
                SecurityContext context) {
            super(response, disableUrlRewriting);
            this.request = request;
            this.httpSessionExistedAtStartOfRequest = httpSessionExistedAtStartOfRequest;
            this.contextHashBeforeExecution = context.hashCode();

            // I think 0 is a safe "null" hashcode value
            if (context.getAuthentication() != null) {
                this.authHashBeforeExecution = context.getAuthentication().hashCode();

                if (context.getAuthentication().getPrincipal() != null) {
                    this.principalHashBeforeExecution = context.getAuthentication().getPrincipal().hashCode();
                } else {
                    this.principalHashBeforeExecution = 0;
                }
            } else {
                this.authHashBeforeExecution = 0;
                this.principalHashBeforeExecution = 0;
            }
        }

        /**
         * Stores the supplied security context in the session (if available) and if it has changed since it was
         * set at the start of the request. If the AuthenticationTrustResolver identifies the current user as
         * anonymous, then the context will not be stored.
         *
         * @param context the context object obtained from the SecurityContextHolder after the request has
         *        been processed by the filter chain. SecurityContextHolder.getContext() cannot be used to obtain
         *        the context as it has already been cleared by the time this method is called.
         *
         */
        @Override
        protected void saveContext(SecurityContext context) {
            final Authentication authentication = context.getAuthentication();
            HttpSession httpSession = request.getSession(false);

            // See SEC-776
            if (authentication == null || authenticationTrustResolver.isAnonymous(authentication)) {
                log.debug("SecurityContext is empty or contents are anonymous - context will not be stored in HttpSession.");

                if (httpSession != null) {
                    String key = getContextKey(httpSession.getId());
                    memcacheRepository.clear(key);
                }
                return;
            }

            if (httpSession == null) {
                httpSession = createNewSessionIfAllowed(context);
            }

            // If HttpSession exists, store current SecurityContext but only if it has
            // actually changed in this thread (see SEC-37, SEC-1307, SEC-1528)
            if (httpSession != null) {
                String key = getContextKey(httpSession.getId());

                // We may have a new session, so check also whether the context attribute is set SEC-1561
                if (!isContextSaved() && (contextChanged(context) || memcacheRepository.get(key) == null)) {
                    String path = request.getRequestURI();

                    if (!sessionBlacklist.contains(path)) {
                        memcacheRepository.set(key, context);

                        log.debug("SecurityContext stored to HttpSession: '" + context + "'");
                    }
                }
            }
        }

        private boolean contextChanged(SecurityContext context) {
            Integer hash1 = context.hashCode();
            Boolean contextChanged = hash1.intValue() != contextHashBeforeExecution.intValue();

            // I think 0 is a safe "null" hashcode value
            Integer hash2 = 0;
            Integer hash3 = 0;

            if (context.getAuthentication() != null) {
                hash2 = context.getAuthentication().hashCode();

                if (context.getAuthentication().getPrincipal() != null) {
                    hash3 = context.getAuthentication().getPrincipal().hashCode();
                }
            }
            boolean authChanged = hash2.intValue() != authHashBeforeExecution.intValue() || hash3.intValue() != principalHashBeforeExecution.intValue();

            //System.out.println(String.format("ContextChanged = %s authHashChanged = %s, principalHashChanged = %s", contextChanged.toString(), new Boolean(hash2.intValue() != authHashBeforeExecution.intValue()), new Boolean(hash3.intValue() != principalHashBeforeExecution.intValue())));
            return contextChanged || authChanged;
        }

        private HttpSession createNewSessionIfAllowed(SecurityContext context) {
            if (httpSessionExistedAtStartOfRequest) {
                log.debug("HttpSession is now null, but was not null at start of request; " + "session was invalidated, so do not create a new session");

                return null;
            }

            if (!allowSessionCreation) {
                log.debug("The HttpSession is currently null, and the " + HttpSessionSecurityContextRepository.class.getSimpleName()
                            + " is prohibited from creating an HttpSession "
                            + "(because the allowSessionCreation property is false) - SecurityContext thus not " + "stored for next request");

                return null;
            }
            // Generate a HttpSession only if we need to

            if (contextObject.equals(context)) {
                log.debug("HttpSession is null, but SecurityContext has not changed from default empty context: ' " + context
                            + "'; not creating HttpSession or storing SecurityContext");

                return null;
            }

            log.debug("HttpSession being created as SecurityContext is non-default");

            try {
                return request.getSession(true);
            } catch (IllegalStateException e) {
                // Response must already be committed, therefore can't create a new session
                log.error("Failed to create a session, as response has been committed. Unable to store" + " SecurityContext.");
            }

            return null;
        }
    }
}