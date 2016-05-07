package com.llug.api.persistence.model;

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "account")
// http://stackoverflow.com/questions/8935975/dynamicinsert-dynamicupdate-does-not-work
//@DynamicUpdate
@org.hibernate.annotations.Entity(dynamicUpdate = true)
public class EntityAccount implements EntityObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "username", unique = true)
    String username;

    @Column(name = "status")
    String status;

    @Column(name = "email", unique = true)
    String email;

    @Column(name = "full_name")
    String fullName;

    @Column(name = "password_hash")
    String passwordHash;

    @Column(name = "time_created")
    Long timeCreated;

    @Column(name = "time_updated")
    Long timeUpdated;

    @Column(name = "flags")
    Long flags;

    @Column(name = "last_login_time")
    Long lastLoginTime;

    @Column(name = "last_device_used")
    String lastDeviceUsed;

    @Column(name = "last_device_ip_used")
    String lastDeviceIpAddressUsed;

    @Column(name = "admin_flags")
    String adminFlags;

    //@OneToMany(cascade = { CascadeType.ALL })
    //@JoinColumn(name = "account_fk")
    //Set<EntityAccountEmail> emails;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public Long getFlags() {
        return flags;
    }

    public void setFlags(Long flags) {
        this.flags = flags;
    }

    public Long getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(Long lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastDeviceUsed() {
        return lastDeviceUsed;
    }

    public void setLastDeviceUsed(String lastDeviceUsed) {
        this.lastDeviceUsed = lastDeviceUsed;
    }

    public String getLastDeviceIpAddressUsed() {
        return lastDeviceIpAddressUsed;
    }

    public void setLastDeviceIpAddressUsed(String lastDeviceIpAddressUsed) {
        this.lastDeviceIpAddressUsed = lastDeviceIpAddressUsed;
    }

    public String getAdminFlags() {
        return adminFlags;
    }

    public void setAdminFlags(String adminFlags) {
        this.adminFlags = adminFlags;
    }
}
