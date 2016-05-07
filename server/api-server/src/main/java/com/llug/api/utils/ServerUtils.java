package com.llug.api.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;

@Component
public class ServerUtils {
    @Value("$api{s3.key:AKIAI6OL56LOTZ3YO4PQ}")
    private String s3AccessKey;
    @Value("$api{s3.secret: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}")
    private String s3SecretKey;

    @Value("$api{aws.reducedpolicy.sessiontimeout.seconds:900}")
    private Integer s3SessionCredentialsTimeoutSeconds;

    public BasicSessionCredentials generateTimeLimitedSessionCredentialsForS3Upload() {
        AWSCredentials credentials = new BasicAWSCredentials(s3AccessKey, s3SecretKey);
        AWSSecurityTokenServiceClient stsClient = new AWSSecurityTokenServiceClient(credentials);

        //
        // Manually start a session.
        GetSessionTokenRequest getSessionTokenRequest = new GetSessionTokenRequest();
        // Following duration can be set only if temporary credentials are requested by an IAM user.
        getSessionTokenRequest.setDurationSeconds(s3SessionCredentialsTimeoutSeconds);

        GetSessionTokenResult sessionTokenResult = stsClient.getSessionToken(getSessionTokenRequest);
        Credentials sessionCredentials = sessionTokenResult.getCredentials();

        // Package the temporary security credentials as 
        // a BasicSessionCredentials object, for an Amazon S3 client object to use.
        BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(sessionCredentials.getAccessKeyId(),
                sessionCredentials.getSecretAccessKey(),
                sessionCredentials.getSessionToken());

        return basicSessionCredentials;
    }
}
