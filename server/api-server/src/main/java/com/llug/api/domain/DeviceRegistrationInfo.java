package com.llug.api.domain;

import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.amazonaws.auth.BasicSessionCredentials;

@JsonSerialize(include = Inclusion.NON_NULL)
public class DeviceRegistrationInfo {
    private final String deviceToken;
    private final String awsAccessKey;
    private final String awsSecretKey;
    private final String sessionToken;
    private final String status;

    public DeviceRegistrationInfo(String deviceToken, BasicSessionCredentials sessionCredentials, final String status) {

        super();
        this.deviceToken = deviceToken;
        
        if (sessionCredentials != null) {
            this.awsAccessKey = sessionCredentials.getAWSAccessKeyId();
            this.awsSecretKey = sessionCredentials.getAWSSecretKey();
            this.sessionToken = sessionCredentials.getSessionToken();
        } else {
            this.awsAccessKey = null;
            this.awsSecretKey = null;
            this.sessionToken = null;
        }
        
        this.status = status;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getAwsAccessKey() {
        return awsAccessKey;
    }

    public String getAwsSecretKey() {
        return awsSecretKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getStatus() {
        return status;
    }
}