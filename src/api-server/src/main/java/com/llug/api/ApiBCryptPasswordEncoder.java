package com.llug.api;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.wch.commons.utils.Utils;

@Component
public class ApiBCryptPasswordEncoder extends BCryptPasswordEncoder {
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        boolean matches = super.matches(rawPassword, encodedPassword);
        
        // allow the password to be the actual password hash
        boolean hashMatches = !Utils.isNullOrEmptyString(encodedPassword) && encodedPassword.equals(rawPassword);
        
        return matches || hashMatches;
    }
}
