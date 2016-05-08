package com.llug.api.service;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.llug.api.ApiPasswordEncoder;
import com.llug.api.domain.Account;
import com.llug.api.domain.DeviceRegistrationInfo;
import com.llug.api.domain.IOSEnvironment;
import com.llug.api.notifications.ApnsProcessor;
import com.llug.api.persistence.model.EntityClientDevice;
import com.llug.api.repository.LlugRepository;
import com.llug.api.utils.RequestUtils;
import com.llug.api.utils.Status;
import com.sun.jersey.core.util.Base64;
import com.wch.commons.utils.ChunkedMemoryStream;
import com.wch.commons.utils.InMemoryFile;
import com.wch.commons.utils.StreamUtils;
import com.wch.commons.utils.Utils;

@Service
public class AccountService {
    @Inject
    protected AuthenticationService authenticationService;

    @Autowired
    PasswordEncoder passwordEncoder;
    //ApiPasswordEncoder passwordEncoder;

    @Autowired
    LlugRepository accountRepository;

    @Autowired
    ApnsProcessor apnsProcessor;

    public Account loginAccount(HttpServletRequest request, String email, String password, String deviceId) {
        if (!Utils.isNullOrEmptyString(email)) {
            if (!Utils.isNullOrEmptyString(password)) {
                authenticationService.authenticate(request, email, password);

                final Account account = authenticationService.getAuthenticatedUser();

                // authenticationService updates/records login
                return account;
            }
        }

        return null;
    }

    public Account registerAccount(final HttpServletRequest request, final String fullName, final String username, final String email, final String password) {
        final String passwordHash = passwordEncoder.encode(password);
        Account account = null;

        try {
            accountRepository.createAccount(fullName, username, email, passwordHash, Status.PENDING);

            // force a login once we've registered
            account = loginAccount(request, email, password, null);

        } catch (DataIntegrityViolationException e) {
            // try a login instead
            account = loginAccount(request, email, password, null);
        }

        return account;
    }

    public DeviceRegistrationInfo registerDevice(final HttpServletRequest request,
            final Account account,
            MultipartFile deviceTokenFile,
            String deviceTokenBase64,
            String deviceTokenRaw,
            final String carrier,
            final String carrierCountryNetwork,
            final String os,
            final String fullVersion,
            final String versionString,
            final Long majorVersion,
            final Long minorVersion,
            final String model,
            final String manufacturer,
            final Long screenWidth,
            final Long screenHeight,
            final Long densityDpi,
            final String deviceId,
            final IOSEnvironment iosEnvironment,
            final String appVersion) throws Exception {

        byte[] deviceTokenBytes = null;

        if (Utils.isNullOrEmptyString(deviceTokenRaw)) {
            if (!Utils.isNullOrEmptyString(deviceTokenBase64)) {
                deviceTokenBytes = Base64.decode(deviceTokenBase64);
            } else {
                deviceTokenFile = RequestUtils.clean(deviceTokenFile);

                if (deviceTokenFile != null) {
                    InMemoryFile deviceTokenInMemoryFile = new InMemoryFile(deviceTokenFile);
                    ChunkedMemoryStream cms = new ChunkedMemoryStream();

                    StreamUtils.writeStreamToStream(deviceTokenInMemoryFile.getInputStream(), cms);

                    deviceTokenBytes = cms.toArray();
                }
            }

            if (deviceTokenBytes != null) {
                if (deviceTokenBytes.length != 32) {
                    throw new Exception("expected 32-byte device token");
                } else {
                    deviceTokenRaw = apnsProcessor.deviceTokenToString(deviceTokenBytes);
                }
            }
        }

        EntityClientDevice device = accountRepository.addOrUpdateDevice(account != null ? account.getAccountIdAsLong() : null,
                deviceId,
                deviceTokenRaw,
                RequestUtils.getOriginatingIp(request),
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
                densityDpi,
                iosEnvironment,
                appVersion);

        return new DeviceRegistrationInfo(deviceTokenRaw, null, device.getStatus());
    }

    public boolean sendAdminMessage(final String deviceTokenString, final String text, final IOSEnvironment env) {
        apnsProcessor.push(deviceTokenString, text, env);

        return true;
    }
}
