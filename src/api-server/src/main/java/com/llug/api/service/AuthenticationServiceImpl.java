package com.llug.api.service;

import static ch.lambdaj.Lambda.having;
import static ch.lambdaj.Lambda.on;
import static ch.lambdaj.Lambda.select;
import static org.hamcrest.Matchers.equalTo;

import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.session.SessionAuthenticationException;
import org.springframework.stereotype.Service;

import com.llug.api.domain.Account;
import com.llug.api.persistence.model.EntityUtils;
import com.llug.api.repository.LlugRepository;
import com.llug.api.repository.MemcacheRepository;
import com.llug.api.utils.RequestUtils;
import com.wch.commons.utils.DateUtils;
import com.wch.commons.utils.EmailUtils;
import com.wch.commons.utils.Md5;

@Slf4j
@Service
public class AuthenticationServiceImpl implements AuthenticationService {
    @Value("$api{sessionCookieName}")
    private String defaultSessionCookieName;

    @Value("$api{memcache.emailTokenVerificationExpirySeconds}")
    private int emailTokenVerificationExpirySeconds;

    @Value("$api{memcache.loginExpirySeconds}")
    private Integer loginExpirySeconds;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private MemcacheRepository memcacheRepository;

    @Autowired
    LlugRepository llugRepository;

    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        Account account = null;
        if (EmailUtils.validateEmail(username)) {
            account = llugRepository.findAccountByEmail(username);
        } else {
            account = llugRepository.findAccountByUsername(username);
        }

        if (account == null) {
            throw new UsernameNotFoundException("The username by which the corresponding user is to be loaded is empty or not in database");
        }

        return account;
    }

    @Override
    public Account getAuthenticatedUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = null;

        if (principal == null || principal instanceof String) {
            throw new SessionAuthenticationException("no user for a request that requires authentication");
        } else if (principal instanceof Account) {
            account = (Account) principal;
        } else {
            throw new SessionAuthenticationException("Unknown principal of type " + principal.toString());
        }

        return account;
    }

    @Override
    public void authenticate(HttpServletRequest request, String username, String password) {
        final UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username, password);
        token.setDetails(new WebAuthenticationDetails(request));
        
        final Authentication authenticatedUser = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(authenticatedUser);

        final Account account = getAuthenticatedUser();
        final Long now = DateUtils.nowAsTicks();
        final String ip = RequestUtils.getOriginatingIp(request);
        final String userAgent = request.getHeader("User-Agent");
        
        llugRepository.updateLogin(account.getAccountIdAsLong(), now, ip, userAgent);

        cacheSession(request, account);
    }

    @Override
    public Account signIn(String userId) {
        final Long id = EntityUtils.safeDecodeId(userId);
        final Account account = llugRepository.getAccountById(id);

        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(account, account.getPassword());
        Authentication authenticatedUser = authenticationManager.authenticate(token);

        SecurityContextHolder.getContext().setAuthentication(authenticatedUser);

        return account;
    }

    @Override
    public void deleteSessionCache(HttpServletRequest request) {
        String apiKey = getApiServiceSessionCacheKey(request);
        memcacheRepository.clear(apiKey);

        Account account = getAuthenticatedUser();
        String cacheKey = String.format("u:%d", account.getAccountIdAsLong());
        memcacheRepository.clear(cacheKey);

        log.info(String.format("deleting cache for user %s", account.getUsername()));

        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    public void cacheSession(HttpServletRequest request, Account account) {
        String apiKey = getApiServiceSessionCacheKey(request);

        // set the caches...
        memcacheRepository.set(apiKey, loginExpirySeconds, account.getAccountIdAsLong());

        cacheUser(account, true);
    }

    public void cacheUser(Account account, boolean force) {
        String userCacheKey = String.format("u:%s", account.getAccountIdAsLong());

        if (force || memcacheRepository.exists(userCacheKey)) {
            memcacheRepository.set(userCacheKey, loginExpirySeconds, account);
        }
    }

    private String getApiServiceSessionCacheKey(HttpServletRequest request) {
        // generate session cookie
        HttpSession session = request.getSession();
        String sessionId = session.getId();

        // sadly, I have to do things this way because request.getServletContext().getSessionCookieConfig().getName() doesn't work
        String sessionCookieName = defaultSessionCookieName; // NF idea how to do this...hardcode for now
        Cookie[] cookies = request.getCookies();
        List<Cookie> sessionCookies = select(cookies, having(on(Cookie.class).getValue(), equalTo(sessionId)));

        if (sessionCookies != null && sessionCookies.size() > 0) {
            sessionCookieName = sessionCookies.get(0).getName();
        }

        String key = String.format("%s:%s", sessionCookieName, sessionId);

        return key;

        /*
        // generate session cookie
        HttpSession session = request.getSession();
        ServletContext context = request.getServletContext();
        ContextHandler.SContext scontext = (ContextHandler.SContext) request.getServletContext();
        Enumeration names = scontext.getAttributeNames();
        SessionCookieConfig cookieConfig = context.getSessionCookieConfig();
        String cookieName = cookieConfig.getName();
        User u = getAuthenticatedUser();

        // set the caches...
        memcacheRepository.set(String.format("%s:%s", cookieName, session.getId()), u.getId());
         */
    }

    public String validateEmail(String token) {
        boolean rt = this.memcacheRepository.exists(token);
        if (rt) {
            String username = (String) this.memcacheRepository.get(token);
            this.memcacheRepository.clear(token);
            return username;
        }
        return null;
    }

    public String generateEmailToken(Account account) {
        String token = Md5.createFromString(String.format("%s:%s:%d", account.getUsername(), account.getEmail(), DateUtils.nowAsTicks())).toString();

        // store the token to memcached.
        this.memcacheRepository.set(token, emailTokenVerificationExpirySeconds, account.getUsername());
        return token;
    }

    public AuthenticationManager getAuthenticationManager() {
        return authenticationManager;
    }

    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
}
