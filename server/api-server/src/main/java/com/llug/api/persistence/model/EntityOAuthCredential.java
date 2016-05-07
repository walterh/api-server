package com.llug.api.persistence.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.llug.api.domain.SocialType;
import com.wch.commons.utils.DateUtils;
import com.wch.commons.utils.Utils;

@Entity
@Table(name = "oauth_credential")
public class EntityOAuthCredential implements EntityObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "account_id", columnDefinition = "int unsigned", nullable = false)
    Long accountId;

    @Column(name = "service", columnDefinition = "int(3) unsigned", nullable = false)
    Integer service;

    @Column(name = "active", columnDefinition = "BIT", length = 1)
    Boolean isActive;

    @Column(name = "screen_name")
    String screenName;

    @Column(name = "service_user_id")
    String serviceUserId;

    @Column(name = "service_full_name")
    String serviceFullName;

    @Column(name = "service_account_identifier")
    String serviceAccountIdentifier;

    @Column(name = "profile_url")
    String profileUrl;

    @Column(name = "profile_image_url")
    String profileImageUrl;

    @Column(name = "oauth_token")
    String oauthToken;

    @Column(name = "oauth_secret")
    String oauthSecret;

    @Column(name = "time_created", columnDefinition = "bigint(20) DEFAULT 0")
    Long timeCreated;

    @Column(name = "time_updated", columnDefinition = "bigint(20) DEFAULT 0")
    Long timeUpdated;

    @Column(name = "default_identity", columnDefinition = "BIT", length = 1)
    Boolean defaultIdentity;

    public EntityOAuthCredential(Long accountId,
            Integer service,
            String screenName,
            String serviceUserId,
            String serviceFullName,
            String serviceAccountIdentifier,
            String profileUrl,
            String profileImageUrl,
            String oauthToken,
            String oauthSecret) {
        this.accountId = accountId;
        this.service = service;
        this.screenName = screenName;
        this.serviceUserId = serviceUserId;
        this.serviceFullName = serviceFullName;
        this.serviceAccountIdentifier = serviceAccountIdentifier;
        this.profileUrl = profileUrl;
        this.profileImageUrl = profileImageUrl;
        this.oauthSecret = oauthSecret;
        this.oauthToken = oauthToken;

        this.isActive = Boolean.TRUE;
        this.timeCreated = DateUtils.nowAsTicks();
        this.timeUpdated = this.timeCreated;
        this.defaultIdentity = false;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Integer getService() {
        return service;
    }

    public void setService(Integer service) {
        this.service = service;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public String getScreenName() {
        return screenName;
    }

    public void setScreenName(String screenName) {
        this.screenName = screenName;
    }

    public String getServiceUserId() {
        return serviceUserId;
    }

    public void setServiceUserId(String serviceUserId) {
        this.serviceUserId = serviceUserId;
    }

    public String getServiceFullName() {
        return serviceFullName;
    }

    public void setServiceFullName(String serviceFullName) {
        this.serviceFullName = serviceFullName;
    }

    public String getServiceAccountIdentifier() {
        return serviceAccountIdentifier;
    }

    public void setServiceAccountIdentifier(String serviceAccountIdentifier) {
        this.serviceAccountIdentifier = serviceAccountIdentifier;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public void setProfileUrl(String profileUrl) {
        this.profileUrl = profileUrl;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getOauthToken() {
        return oauthToken;
    }

    public void setOauthToken(String oauthToken) {
        this.oauthToken = oauthToken;
    }

    public String getOauthSecret() {
        return oauthSecret;
    }

    public void setOauthSecret(String oauthSecret) {
        this.oauthSecret = oauthSecret;
    }

    public Long getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Long timeCreated) {
        this.timeCreated = timeCreated;
    }

    public Long getTimeUpdated() {
        return timeUpdated;
    }

    public void setTimeUpdated(Long timeUpdated) {
        this.timeUpdated = timeUpdated;
    }

    public String getServiceAsString() {
        return service.toString();
    }

    public Boolean getDefaultIdentity() {
        return defaultIdentity;
    }

    public void setDefaultIdentity(Boolean defaultIdentity) {
        this.defaultIdentity = defaultIdentity;
    }

    public String asFriendlyName() {
        SocialType socialType = SocialType.valueOf(service);

        if (socialType == SocialType.Google && !Utils.isNullOrEmptyString(serviceAccountIdentifier)) {
            return serviceAccountIdentifier;
        } else if (socialType == SocialType.Twitter && !screenName.startsWith("@")) {
            return "@" + screenName;
        } else {
            return screenName;
        }
    }
}
