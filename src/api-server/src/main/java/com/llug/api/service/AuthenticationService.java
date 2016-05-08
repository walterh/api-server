package com.llug.api.service;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.llug.api.domain.Account;

public interface AuthenticationService extends UserDetailsService {
    Account getAuthenticatedUser();

    void authenticate(HttpServletRequest request, String username, String password);

    Account signIn(String userId);

    void deleteSessionCache(HttpServletRequest request);

    void cacheSession(HttpServletRequest request, Account account);
}
