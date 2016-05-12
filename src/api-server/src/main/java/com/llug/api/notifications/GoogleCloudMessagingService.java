package com.llug.api.notifications;

import static ch.lambdaj.Lambda.*;
import static ch.lambdaj.collection.LambdaCollections.with;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.google.android.gcm.server.*;
import com.google.android.gcm.server.Message.Builder;
import com.wch.commons.utils.ResourceUrl;
import com.wch.commons.utils.Utils;

@Slf4j
@Component
public class GoogleCloudMessagingService {
    @Value("$api{gcm.keypath}")
    private String gcmKeyPath;

    @Value("$api{gcm.retries}")
    private String gcmRetriesStr;

    private String gcmKey;
    private Sender gcmSender;
    private Integer gcmRetries;

    @PostConstruct
    public void initialize() throws IOException {
        final String rsrcBase = new ClassPathResource(".").getURI().toString();

        // we need to collapse due to the back ref
        String path = ResourceUrl.parse(rsrcBase + "../" + gcmKeyPath).collapse().getUri();

        File gcmKeyFile = new File(path);
        if (!gcmKeyFile.exists()) {
            // assume unit test location
            path = ResourceUrl.parse(rsrcBase + "../../src/main/" + gcmKeyPath).collapse().getUri();
        }

        FileInputStream inputStream = new FileInputStream(path);
        try {
            gcmKey = IOUtils.toString(inputStream);
        } finally {
            inputStream.close();
        }

        gcmSender = newSender();
        gcmRetries = Integer.valueOf(gcmRetriesStr);
    }

    /*
    public void push(String deviceTokenString, VideoNotification n) {
        Builder builder = new Message.Builder().addData(ApnsProcessor.APNS_JSON_KEY_ALERT_ACTION, n.getAlertAction())
                .addData(ApnsProcessor.APNS_JSON_KEY_ALERT_BODY, n.getAlertBody())
                .addData(ApnsProcessor.APNS_JSON_KEY_VERSION, "1")
                .addData(ApnsProcessor.APNS_JSON_KEY_TYPE, n.getType().toString());
        //.addData(ApnsProcessor.APNS_JSON_KEY_TIMESTAMP, n.getInteractionTimeStamp().toString());

        if (!Utils.isNullOrEmptyString(n.getProjectId())) {
            builder = builder.addData(ApnsProcessor.APNS_JSON_KEY_PROJECT, n.getProjectId());
        }
        if (!Utils.isNullOrEmptyString(n.getUrl())) {
            builder = builder.addData(ApnsProcessor.APNS_JSON_KEY_URL, n.getUrl());
        }

        final Message gcmMsg = builder.build();

        try {
            final Result result = gcmSender.send(gcmMsg, deviceTokenString, gcmRetries);

            if (!Utils.isNullOrEmptyString(result.getErrorCodeName())) {
                logger.error(String.format("GCM messageId = '%s' received error = '%s'", result.getMessageId(), result.getErrorCodeName()));
            }
        } catch (IOException e) {
            logger.error(LogUtils.getStackTrace(e));
        }
    }
    */
    
    private Sender newSender() {
        return new Sender(gcmKey);
    }
}