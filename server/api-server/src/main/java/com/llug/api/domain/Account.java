package com.llug.api.domain;

import java.io.Serializable;
import java.util.Collection;

import javax.persistence.Transient;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.llug.api.utils.UrlEncoderSerializer;
import com.llug.api.persistence.model.EntityAccount;
import com.llug.api.persistence.model.EntityUtils;
import com.wch.commons.utils.Utils;

@JsonSerialize(include = Inclusion.NON_NULL)
public class Account implements UserDetails, Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Account.class);

    private static final long serialVersionUID = -2220895762980772043L;

    private String accountId;
    private String username;
    private String email;
    private String fullName;

    @JsonSerialize(using = UrlEncoderSerializer.class, include = JsonSerialize.Inclusion.NON_NULL)
    private String passwordHash;
    private Long timeCreated;
    private Long timeUpdated;
    private Long flags;
    private String status;

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // TODO Auto-generated method stub
        return null;
    }

    @JsonIgnore
    @Override
    public String getPassword() {
        return getPasswordHash();
    }

    @Override
    public String getUsername() {
        return username;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        // TODO Auto-generated method stub
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        // TODO Auto-generated method stub
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        // TODO Auto-generated method stub
        return true;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Long getTimeCreated() {
        return timeCreated;
    }

    public Long getTimeUpdated() {
        return timeUpdated;
    }

    public Long getFlags() {
        return flags;
    }

    public String getStatus() {
        return status;
    }

    @JsonIgnore
    public Long getAccountIdAsLong() {
        return EntityUtils.decodeId(accountId);
    }

    @JsonIgnore
    public String getFriendlyName() {
        if (!Utils.isNullOrEmptyString(fullName)) {
            return fullName;
        } else if (!Utils.isNullOrEmptyString(username)) {
            return username;
        } else {
            return email;
        }
    }

    @JsonIgnore
    public String getEmailFriendlyName() {
        return String.format("\"%s\" <%s>", getFriendlyName(), email);
    }
    
    @JsonIgnore
    @Override
    public String toString() {
        return String.format("Account(%d):  %s; password = %s", getAccountIdAsLong(), getEmailFriendlyName(), passwordHash);
    }


    public Account(EntityAccount rawAccount) {        
        this.accountId = EntityUtils.encodeId(rawAccount.getId());

        this.username = rawAccount.getUsername();
        this.fullName = rawAccount.getFullName();
        this.email = rawAccount.getEmail();
        this.passwordHash = rawAccount.getPasswordHash();
        this.status = rawAccount.getStatus();
    }
}
