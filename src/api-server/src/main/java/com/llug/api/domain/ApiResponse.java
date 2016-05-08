package com.llug.api.domain;

import java.io.Serializable;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;


public class ApiResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum ApiVersion {
        v0, v1
    }

    /**
     * Make sure these line up with iOS enum's in ServerPackage.h
     * @author walterh
     *
     */
    public enum ResultCode {
        success,
        error,
        invalid_credentials,
        expired_session,
        insecure_connection,
        not_found,
        signed_out,

        invalid_args,
        invalid_permissions,
        duplicate_entry,
        not_implemented,
        validation_failed,
        contact_delicious_support,
        rate_limit_exceeded,
        //deactivated_user,
        
        ip_whitelist_block,
        device_id_blocked,
        account_blocked,
        hash_mismatch,
        inactive_account,

        // generic fail (internal server failure)
        internal_server_failure,
        connection_failure,
        invalid_captcha,
        
        token_expired,
        invalid_password, 
        not_changed,
        another_account_exist
    }

    public ApiResponse() {
        this.status = ResultCode.success;
    }

    public ApiResponse(Object pkg) {
        this.pkg = pkg;
        this.status = ResultCode.success;
    }

    private Object pkg;
    private ResultCode status;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String message;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String error;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String url;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String query;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String stacktrace;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private Long delta_ms;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String client_ip;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String server;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private String session;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private Long rate_limit_quota;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private Long api_mgmt_ms;

    @JsonSerialize(include = Inclusion.NON_NULL)
    private ApiVersion version;
    
	public Object getPkg() {
        return pkg;
    }

    public void setPkg(Object pkg) {
        this.pkg = pkg;
    }

    public ResultCode getStatus() {
        return status;
    }

    public void setStatus(ResultCode status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public String getUrl() {
        return url;
    }

    public String getQuery() {
        return query;
    }

    public void setError(String error) {
        this.error = error;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public void setStacktrace(String stacktrace) {
        this.stacktrace = stacktrace;
    }

    public Long getDelta_ms() {
        return delta_ms;
    }

    public void setDelta_ms(Long delta_ms) {
        this.delta_ms = delta_ms;
    }

    public String getClient_ip() {
        return client_ip;
    }

    public void setClient_ip(String client_ip) {
        this.client_ip = client_ip;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getSession() {
        return session;
    }

    public void setSession(String session) {
        this.session = session;
    }
    
    public ApiVersion getVersion() {
		return version;
	}

    public Long getRate_limit_quota() {
        return rate_limit_quota;
    }

    public void setRate_limit_quota(Long rateLimitQuota) {
        this.rate_limit_quota = rateLimitQuota;
    }

    public Long getApi_mgmt_ms() {
        return api_mgmt_ms;
    }

    public void setApi_mgmt_ms(Long apiMgmtMs) {
        this.api_mgmt_ms = apiMgmtMs;
    }

	public void setVersion(ApiVersion version) {
		this.version = version;
	}
}
