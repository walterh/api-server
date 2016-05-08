package com.llug.api.persistence.model;

import static ch.lambdaj.Lambda.*;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.llug.api.domain.IOSEnvironment;

@Entity
@Table(name = "client_device")
public class EntityClientDevice implements EntityObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "device_id")
    String deviceId;

    @Column(name = "account_id")
    Long accountId;

    @Column(name = "time_created")
    Long timeCreated;

    @Column(name = "time_updated")
    Long timeUpdated;

    @Column(name = "device_token")
    String deviceToken;

    @Column(name = "ip_address")
    String ipAddress;

    @Column(name = "carrier")
    String carrier;

    @Column(name = "ccn")
    String carrierCountryNetwork;

    @Column(name = "os")
    String OperatingSystem;

    @Column(name = "full_version")
    String fullVersion;

    @Column(name = "version_string")
    String versionString;

    @Column(name = "major_version")
    Short majorVersion;

    @Column(name = "minor_version")
    Short minorVersion;

    @Column(name = "model")
    String model;

    @Column(name = "manufacturer")
    String manufacturer;

    @Column(name = "status")
    String status;

    @Column(name = "env")
    @Enumerated(EnumType.STRING)
    IOSEnvironment iosEnvironment;

    @Column(name = "screen_width")
    Short screenWidth;

    @Column(name = "screen_height")
    Short screenHeight;

    @Column(name = "density_dpi")
    Short densityDpi;

    @Column(name = "app_version")
    String appVersion;

    public Long getId() {
        return id;
    }

    public String getIdAsString() {
        return String.valueOf(id);
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public EntityClientDevice setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;

        return this;
    }

    public String getCarrier() {
        return carrier;
    }

    public void setCarrier(String carrier) {
        this.carrier = carrier;
    }

    public String getCarrierCountryNetwork() {
        return carrierCountryNetwork;
    }

    public void setCarrierCountryNetwork(String carrierCountryNetwork) {
        this.carrierCountryNetwork = carrierCountryNetwork;
    }

    public String getOperatingSystem() {
        return OperatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        OperatingSystem = operatingSystem;
    }

    public String getFullVersion() {
        return fullVersion;
    }

    public void setFullVersion(String fullVersion) {
        this.fullVersion = fullVersion;
    }

    public String getVersionString() {
        return versionString;
    }

    public void setVersionString(String versionString) {
        this.versionString = versionString;
    }

    public Short getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(Short majorVersion) {
        this.majorVersion = majorVersion;
    }

    public Short getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(Short minorVersion) {
        this.minorVersion = minorVersion;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public IOSEnvironment getIOSEnvironment() {
        return iosEnvironment;
    }

    public void setIOSEnvironment(IOSEnvironment environment) {
        this.iosEnvironment = environment;
    }

    public Short getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(Short screenWidth) {
        this.screenWidth = screenWidth;
    }

    public Short getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(Short screenHeight) {
        this.screenHeight = screenHeight;
    }

    public Short getDensityDpi() {
        return densityDpi;
    }

    public void setDensityDpi(Short densityDpi) {
        this.densityDpi = densityDpi;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }
}
