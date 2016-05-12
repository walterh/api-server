package com.llug.api.notifications;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.llug.api.domain.IOSEnvironment;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import com.notnoop.apns.ApnsServiceBuilder;
import com.notnoop.apns.PayloadBuilder;
import com.wch.commons.utils.FileUtils;
import com.wch.commons.utils.LogUtils;
import com.wch.commons.utils.ResourceUrl;
import com.wch.commons.utils.Utils;

@Slf4j
@Component
public class ApnsProcessor {
    public final static String APNS_JSON_KEY_TYPE = "t";
    public final static String APNS_JSON_KEY_TIMESTAMP = "ts";
    public final static String APNS_JSON_KEY_VERSION = "v";

    public final static String APNS_JSON_KEY_TEXT = "tx";
    public final static String APNS_JSON_KEY_PROJECT = "p";
    public final static String APNS_JSON_KEY_URL = "u";

    public final static String APNS_JSON_KEY_ALERT_ACTION = "aa";
    public final static String APNS_JSON_KEY_ALERT_BODY = "ab";

    @Value("#{'$api{apns.cert.files}'.split(',')}")
    private List<String> apnsCertificatePaths;

    @Value("#{'$api{apns.cert.pwds}'.split(',')}")
    private List<String> apnsCertificatePasswords;

    private List<ApnsService> apnsServices;

    @PostConstruct
    public void initialize() throws IOException {
        final String rsrcBase = new ClassPathResource(".").getURI().toString();

        apnsServices = new ArrayList<ApnsService>();

        for (int i = 0; i < apnsCertificatePaths.size(); i++) {
            String apnsCertFilePath = apnsCertificatePaths.get(i);
            String apnsCertPassword = apnsCertificatePasswords.get(i);
            IOSEnvironment env = Utils.safeParseEnum(IOSEnvironment.class, FileUtils.extractLastTag(apnsCertFilePath));

            // we need to collapse due to the back ref
            String path = ResourceUrl.parse(rsrcBase + "../" + apnsCertFilePath).collapse().getUri();

            File apnsCertFile = new File(path);
            if (apnsCertFile.exists()) {
                apnsCertFilePath = path;
            } else {
                // assume unit test location
                apnsCertFilePath = ResourceUrl.parse(rsrcBase + "../../src/main/" + apnsCertFilePath).collapse().getUri();
            }

            try {
                ApnsServiceBuilder serviceBuilder = APNS.newService().withCert(apnsCertFilePath, apnsCertPassword);
                if (env == IOSEnvironment.prod || env == null) {
                    serviceBuilder = serviceBuilder.withProductionDestination();
                } else {
                    serviceBuilder = serviceBuilder.withSandboxDestination();
                }
    
                apnsServices.add(serviceBuilder.build());
            } catch (Exception e) {
                //log.error(String.format("Could not initialize cert at path='%s'; exception = %s", apnsCertFilePath, LogUtils.getStackTrace(e)));
            }
        }
    }

    public String deviceTokenToString(byte[] deviceToken) {
        StringBuilder sb = new StringBuilder();

        for (byte b : deviceToken) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    public void push(String deviceTokenString, String text, IOSEnvironment env) {
        // deliberate toString.  See comments on the enumeration
        PayloadBuilder pb = APNS.newPayload().badge(3).customField(APNS_JSON_KEY_VERSION, "1").customField(APNS_JSON_KEY_TYPE, "t");
        //.customField(APNS_JSON_KEY_TIMESTAMP, n.getInteractionTimeStamp().toString());

        if (!Utils.isNullOrEmptyString(text)) {
            pb = pb.customField(APNS_JSON_KEY_TEXT, text);
        }

        /*
        if (!Utils.isNullOrEmptyString(n.getProjectId())) {
            pb = pb.customField(APNS_JSON_KEY_PROJECT, n.getProjectId());
        }
        if (!Utils.isNullOrEmptyString(n.getUrl())) {
            pb = pb.customField(APNS_JSON_KEY_URL, n.getUrl());
        }
         */

        //String payload = pb.actionKey(n.getAlertAction()).alertBody(n.getAlertBody()).build();
        String payload = pb.actionKey("a").alertBody(text).build();

        /*
        if (payload.length() > 256) {
            final String msg = String.format("APNS Failed: push data of length %d bytes", payload.length());
            logger.error(msg);
            SmtpUtils.sendSslMessage(null, msg, payload, null);
        }
         */
        
        apnsServices.get(0).push(deviceTokenString, payload);
    }
}
