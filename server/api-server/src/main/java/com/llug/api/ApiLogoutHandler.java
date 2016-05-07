package com.llug.api;

import javax.inject.Inject;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.stereotype.Component;

import com.wch.commons.utils.Utils;

@Component
public class ApiLogoutHandler extends SimpleUrlLogoutSuccessHandler {
	@Override
	public void onLogoutSuccess(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response, Authentication authentication)
			throws java.io.IOException, javax.servlet.ServletException {
				
		String qs = request.getQueryString();
		String redirect = "/signin";
		
		if (!Utils.isNullOrEmptyString(qs)) {
			redirect = redirect + "?" + qs;
		}

		setDefaultTargetUrl(redirect);
		super.onLogoutSuccess(request, response, authentication);
	}
}
