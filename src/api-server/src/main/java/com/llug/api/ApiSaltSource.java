package com.llug.api;

import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.wch.commons.utils.Utils;


@Component
public class ApiSaltSource implements SaltSource {
	public static final int SALT_LENGTH = 8;
	
	@Override
	public Object getSalt(UserDetails user) {
		//User u = (User) user;
		String salt = null;
		
		String pwdHash = user.getPassword();
		if (!Utils.isNullOrEmptyString(pwdHash) && pwdHash.length() > SALT_LENGTH) {
		    salt = pwdHash.substring(0, SALT_LENGTH);
		}
		
		return salt;
	}
}