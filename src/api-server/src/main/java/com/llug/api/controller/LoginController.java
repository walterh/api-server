package com.llug.api.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.llug.api.domain.ApiResponse;
import com.llug.api.domain.ApiResponse.ResultCode;
import com.llug.api.monitoring.Monitored;
import com.llug.api.service.AuthenticationService;
import com.llug.api.utils.RequestUtils;
import com.wch.commons.utils.Utils;

@Controller
public class LoginController {
    @Inject
    protected AuthenticationService authenticationService;

    @Monitored
    @RequestMapping(value = { "/signin", "/login" }, method = RequestMethod.GET)
    public @ResponseBody
    ApiResponse signin(HttpServletRequest request) {
        ApiResponse api = new ApiResponse();
        api.setStatus(ResultCode.expired_session);
        api.setPkg("please sign in to continue");

        return api;
    }

    @RequestMapping(value = { "/signout", "/logout" }, method = RequestMethod.GET)
    public String signout(HttpServletRequest request) {
        authenticationService.deleteSessionCache(request);

        String qs = request.getQueryString();
        String redirect = "redirect:/realsignout";

        if (!Utils.isNullOrEmptyString(qs)) {
            redirect = redirect + "?" + qs;
        }

        return redirect;
    }

    @RequestMapping(value = { "/404", "/webapp", "/webapp/**", "/" }, method = RequestMethod.GET)
    public @ResponseBody
    ApiResponse notFound(HttpServletRequest request) {
        ApiResponse api = new ApiResponse();
        api.setStatus(ResultCode.not_found);
        api.setUrl(RequestUtils.getUrl(request));
        api.setPkg("the URL you requested is not available");

        return api;
    }

    // http://stackoverflow.com/questions/11242609/default-spring-security-redirect-to-favicon/11244868
    @RequestMapping("/favicon.ico")
    public String favIconForward() {
        return "forward:/resources/favicon.ico";
    }
}
