package com.llug.api.controller;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.llug.api.ApiServerException;
import com.llug.api.domain.Account;
import com.llug.api.domain.ApiResponse;
import com.llug.api.domain.ApiResponse.ApiVersion;
import com.llug.api.domain.ApiResponse.ResultCode;
import com.llug.api.domain.IOSEnvironment;
import com.llug.api.domain.SocialType;
import com.llug.api.monitoring.Monitored;
import com.llug.api.repository.LlugRepository;
import com.llug.api.service.AccountService;
import com.llug.api.utils.RequestUtils;
import com.wch.commons.utils.EmailUtils;
import com.wch.commons.utils.LogUtils;
import com.wch.commons.utils.StreamUtils;
import com.wch.commons.utils.Utils;
import com.sun.jersey.core.util.Base64;

@Slf4j
@Controller
@RequestMapping(value = { "/api/v1/account" })
public class AccountController extends BaseApplicationController {
    static final Pattern USERNAME_BASE_PATTERN = Pattern.compile("^[a-z][a-z0-9_.]+[a-z0-9]+$");

    @Value("$api{client.device.header}")
    private String deviceHeader;

    @Autowired
    AccountService accountService;

    @Autowired
    LlugRepository accountRepository;

    private ApiResponse getResponseForError(HttpServletRequest request, final Exception ex, final ResultCode status) {
        ApiResponse api = new ApiResponse();

        api.setError(ex.getMessage());
        api.setStacktrace(LogUtils.getStackTrace(ex));
        api.setStatus(status);

        String url = RequestUtils.getUrl(request);
        if (RequestUtils.isSecureRequest(request)) {
            url = url.replace("http://", "https://");
        }

        api.setUrl(url);

        String sessionId = request.getSession().getId();
        api.setSession(sessionId);

        ApiVersion ver = RequestUtils.getApiVersion(request);
        api.setVersion(ver);

        return api;
    }

    @ExceptionHandler(Exception.class)
    public @ResponseBody ApiResponse defaultExceptionHandler(HttpServletRequest request, Exception ex) {
        return getResponseForError(request, ex, ResultCode.internal_server_failure);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public @ResponseBody ApiResponse missingServletRequestParameterExceptionHandler(HttpServletRequest request, MissingServletRequestParameterException ex) {
        return getResponseForError(request, ex, ResultCode.invalid_args);
    }

    @Monitored
    @RequestMapping(value = "/check", method = RequestMethod.GET)
    public @ResponseBody ApiResponse login(HttpServletRequest request, HttpServletResponse response) {
        ApiResponse api = new ApiResponse();

        try {
            api.setPkg(getCurrentUser());
            api.setStatus(ResultCode.success);
        } catch (Exception e) {
            RequestUtils.controllerExceptionFilter(request, response, null, api, e);
        }

        return api;
    }

    // /login = regular login
    // /rlogin = reauth login. Same as regular login, but the client needs a way of knowing so as to not recurse
    @Monitored(hashSecured = true)
    @RequestMapping(value = { "/login", "/rlogin" }, method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody ApiResponse login(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "alias_email", required = false) String aliasEmail,

            // only used for debug
            @RequestParam(value = "device_id", required = false) String deviceId) {

        ApiResponse api = new ApiResponse();

        try {
            if (request.getMethod().equals("GET")) {
                // Spring MVC decodes once by the time we get to the api call
                email = RequestUtils.urlDecodeClean(email);
                password = RequestUtils.urlDecodeClean(password);
                aliasEmail = RequestUtils.urlDecodeClean(aliasEmail);
            } else {
                // POSTs require double-decoding
                password = RequestUtils.urlDecodeClean(password);
                password = RequestUtils.urlDecodeClean(password);
            }

            final Account account = accountService.loginAccount(request, email, password, null);

            api.setPkg(account);
            api.setStatus(ResultCode.success);
        } catch (Exception e) {
            RequestUtils.controllerExceptionFilter(request, response, authenticationService, api, e);
        }

        return api;
    }

    @Monitored(hashSecured = true)
    @RequestMapping(value = { "/register", "/registerm" }, method = { RequestMethod.GET, RequestMethod.POST })
    public @ResponseBody ApiResponse registerAccount(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "email", required = true) String email,
            @RequestParam(value = "full_name", required = false) String fullName,
            @RequestParam(value = "username", required = true) String username,
            @RequestParam(value = "password", required = false) String password,

            // oauth params
            @RequestParam(value = "social_network", required = false) SocialType socialNetwork,
            @RequestParam(value = "service_user_id", required = false) String serviceUserId,
            @RequestParam(value = "service_username", required = false) String serviceUsername,
            @RequestParam(value = "service_full_name", required = false) String serviceFullName,
            @RequestParam(value = "service_account_identifier", required = false) String serviceAccountIdentifier,
            @RequestParam(value = "profile_url", required = false) String profileUrl,
            @RequestParam(value = "profile_image_url", required = false) String profileImageUrl,
            @RequestParam(value = "social_connections", required = false) Long socialConnections,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "secret", required = false) String secret,

            // only used for debugging purposes
            @RequestParam(value = "device_id", required = false) String deviceId) {
        ApiResponse api = new ApiResponse();

        RequestUtils.getAllHeaders(request, true);

        try {
            email = email.toLowerCase();

            if (!EmailUtils.validateEmail(email)) {
                throw new ApiServerException(ResultCode.invalid_args, "invalid email address");
            }
            if (!USERNAME_BASE_PATTERN.matcher(username).matches()) {
                throw new ApiServerException(ResultCode.invalid_args, "invalid username; must be alphanumeric");
            } else if (username.length() < 6) {
                throw new ApiServerException(ResultCode.invalid_args, "invalid username; too short");
            }

            // this function forces a login
            final Account account = accountService.registerAccount(request, fullName, username, email, password);

            api.setPkg(account);
            api.setStatus(ResultCode.success);
        } catch (Exception e) {
            RequestUtils.controllerExceptionFilter(request, response, authenticationService, api, e);
        }

        return api;
    }

    @Monitored(hashSecured = true)
    @RequestMapping(value = "/registeriosdevice", method = RequestMethod.POST)
    public @ResponseBody ApiResponse registerIOSDevice(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "device_token_base64", required = false) String deviceTokenBase64,

            @RequestParam(value = "carrier", required = true) String carrier,
            @RequestParam(value = "carrier_country_network", required = true) String carrierCountryNetwork,
            @RequestParam(value = "os", required = true) String os,
            @RequestParam(value = "full_version", required = true) String fullVersion,
            @RequestParam(value = "version_string", required = true) String versionString,

            @RequestParam(value = "major_version", required = true) Long majorVersion,
            @RequestParam(value = "minor_version", required = true) Long minorVersion,
            @RequestParam(value = "model", required = true) String model,
            @RequestParam(value = "manufacturer", required = true) String manufacturer,

            @RequestParam(value = "screen_width", required = false) Long screenWidth,
            @RequestParam(value = "screen_height", required = false) Long screenHeight,

            @RequestParam(value = "env", required = false) IOSEnvironment iosEnvironment,

            @RequestParam(value = "app_version", required = false) String appVersion,
            
            @RequestParam(value = "log_gzip_base64", required = false) String logGzipBase64) {

        final ApiResponse api = new ApiResponse();

        try {
            final Account account = getCurrentUserOrNull();

            api.setPkg(accountService.registerDevice(request,
                    account,
                    null,
                    deviceTokenBase64,
                    null,
                    carrier,
                    carrierCountryNetwork,
                    os,
                    fullVersion,
                    versionString,
                    majorVersion,
                    minorVersion,
                    model,
                    manufacturer,
                    screenWidth,
                    screenHeight,
                    null,
                    request.getHeader(deviceHeader),
                    iosEnvironment,
                    appVersion));

            if (!Utils.isNullOrEmptyString(logGzipBase64)) {
                final GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(Base64.decode(logGzipBase64)));
                final ByteArrayOutputStream bos = new ByteArrayOutputStream();

                StreamUtils.writeStreamToStream(gzis, bos);

                final String deviceLogs = bos.toString("UTF-8");
                
                log.info(deviceLogs);
            }
            
            api.setStatus(ResultCode.success);
        } catch (Exception e) {
            RequestUtils.controllerExceptionFilter(request, response, authenticationService, api, e);
        }

        return api;
    }

    @RequestMapping(value = { "/adminmsg/", "/adminmsg" }, method = RequestMethod.GET)
    public @ResponseBody ApiResponse registerIOSDevice(HttpServletRequest request,
            HttpServletResponse response,
            @RequestParam(value = "device_token", required = true) String deviceToken,
            @RequestParam(value = "msg", required = false) String msg) {

        ApiResponse api = new ApiResponse();

        try {
            accountService.sendAdminMessage(deviceToken, msg, IOSEnvironment.dev);
        } catch (Exception e) {
            RequestUtils.controllerExceptionFilter(request, response, authenticationService, api, e);
        }

        return api;

    }
}
