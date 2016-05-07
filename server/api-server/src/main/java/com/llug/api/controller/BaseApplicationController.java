package com.llug.api.controller;

import javax.inject.Inject;

import org.springframework.security.web.authentication.session.SessionAuthenticationException;

import com.llug.api.domain.Account;
import com.llug.api.service.AuthenticationService;

public class BaseApplicationController {
    @Inject
    protected AuthenticationService authenticationService;

    // Throws SessionAuthenticationException if not logged in
    protected Account getCurrentUser() throws SessionAuthenticationException {
         return (Account) authenticationService.getAuthenticatedUser();   
    }
    
    // Returns null if not logged in
    protected Account getCurrentUserOrNull() {
        Account account = null;
        try {
            account = getCurrentUser();
        } catch (SessionAuthenticationException e) { 
            account = null;
        }
        
        return account;        
    }

}
