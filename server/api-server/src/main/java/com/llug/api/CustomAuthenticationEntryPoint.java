package com.llug.api;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

public class CustomAuthenticationEntryPoint extends LoginUrlAuthenticationEntryPoint {
	private final RedirectStrategy redirectStrategy = new FullRedirectStrategy();
	
	public CustomAuthenticationEntryPoint() {
		super("/signin");
	}
	
	public CustomAuthenticationEntryPoint(String loginFormUrl) {
		super(loginFormUrl);
	}

	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

		String redirectUrl = buildRedirectUrlToLoginPage(request, response, authException);

		redirectStrategy.sendRedirect(request, response, redirectUrl);
	}
}
