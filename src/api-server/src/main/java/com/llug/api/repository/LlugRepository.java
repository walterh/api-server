package com.llug.api.repository;

import static ch.lambdaj.Lambda.*;
import static ch.lambdaj.collection.LambdaCollections.with;

import static org.hamcrest.Matchers.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.llug.api.dao.HibernateDao;
import com.llug.api.dao.HibernateDao.IDynamicUpdateImpl;
import com.llug.api.domain.Account;
import com.llug.api.domain.IOSEnvironment;
import com.llug.api.persistence.model.EntityAccount;
import com.llug.api.persistence.model.EntityClientDevice;
import com.llug.api.persistence.model.EntityObject;
import com.llug.api.utils.RequestUtils;
import com.llug.api.utils.Status;
import com.wch.commons.utils.DateUtils;

@Repository
public class LlugRepository {
    @Autowired
    HibernateDao dao;
    
    public Account getAccountById(final Long id) {
        EntityAccount rawAccount = dao.findOne(EntityAccount.class, id);

        if (rawAccount != null) {
            return new Account(rawAccount);
        } else {
            return null;
        }
    }
    
    public Account findAccountByEmail(final String email) {
        EntityAccount rawAccount = with(dao.query(EntityAccount.class, "email", email)).first(any(EntityAccount.class));
        
        if (rawAccount != null) {
            return new Account(rawAccount);
        } else {
            return null;
        }
    }

    public Account findAccountByUsername(final String username) {
        EntityAccount rawAccount = with(dao.query(EntityAccount.class, "username", username)).first(any(EntityAccount.class));
        
        if (rawAccount != null) {
            return new Account(rawAccount);
        } else {
            return null;
        }
    }

    public Account createAccount(final String fullName, final String username, final String email, final String passwordHash, final String status) {
        final Long now = DateUtils.nowAsTicks();
        final EntityAccount rawAccount = new EntityAccount();
        
        rawAccount.setFullName(fullName);
        rawAccount.setEmail(email);
        rawAccount.setUsername(username);
        rawAccount.setPasswordHash(passwordHash);
        rawAccount.setStatus(status == null || status.equals(Status.PENDING) ? Status.PENDING : Status.ACTIVE);
        rawAccount.setTimeCreated(now);
        rawAccount.setTimeUpdated(now);
        
        dao.create(rawAccount);
        
        return new Account(rawAccount);
    }
    
    private EntityClientDevice findDeviceByDeviceId(final String deviceId) {
        return with(dao.query(EntityClientDevice.class, "device_id", deviceId)).first(any(EntityClientDevice.class));
    }
    
    public EntityClientDevice addOrUpdateDevice(final Long accountId,
            final String deviceId,
            final String deviceToken,
            final String ipAddress,
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
            final IOSEnvironment iosEnvironment,
            final String appVersion) {
        
        final Long ticks = DateUtils.nowAsTicks();
        EntityClientDevice device = findDeviceByDeviceId(deviceId);
        Boolean created = null;
        
        if (device == null) {
            created = true;
            device = new EntityClientDevice();
            device.setStatus(Status.PENDING);
            device.setTimeCreated(ticks);

            device.setDeviceId(deviceId);
        }
        
        device.setAccountId(accountId);
        device.setDeviceToken(deviceToken);
        device.setCarrier(carrier);
        device.setCarrierCountryNetwork(carrierCountryNetwork);
        device.setOperatingSystem(os);
        device.setFullVersion(fullVersion);
        device.setVersionString(versionString);
        
        if (majorVersion != null) {
            device.setMajorVersion(majorVersion.shortValue());
        }
        if (minorVersion != null) {
            device.setMinorVersion(minorVersion.shortValue());
        }
        device.setModel(model);
        device.setManufacturer(manufacturer);
        device.setTimeUpdated(ticks);
        if (screenWidth != null) {
            device.setScreenWidth(screenWidth.shortValue());
        }
        if (screenHeight != null) {
            device.setScreenHeight(screenHeight.shortValue());
        }
        if (iosEnvironment != null) {
            device.setIOSEnvironment(iosEnvironment);
        }
        if (densityDpi != null) {
            device.setDensityDpi(densityDpi.shortValue());
        }
        device.setIpAddress(ipAddress);
        device.setAppVersion(appVersion);
        
        if (created) {
            dao.create(device);
        } else {
            dao.update(device);
        }
        
        return device;
    }
    
    public void updateLogin(final Long accountId, final Long now, final String ip, final String userAgent) {
        dao.dynamicUpdate("from EntityAccount where id = :accountId ", "accountId", accountId, new IDynamicUpdateImpl() {
            
            @Override
            public <T extends EntityObject> void setParameters(T t) {
                final EntityAccount rawAccount = (EntityAccount) t;
                
                rawAccount.setLastLoginTime(now);
                rawAccount.setTimeUpdated(now);
                rawAccount.setLastDeviceIpAddressUsed(ip);
                rawAccount.setLastDeviceUsed(userAgent);
            }
        });
        
        // http://www.mkyong.com/hibernate/hibernate-dynamic-update-attribute-example/
        /*
        final org.hibernate.Session session = dao.getCurrentSession();
        final org.hibernate.Query q = session.createQuery("from Account where id = :accountId ");
        q.setParameter("accountId", accountId);
        
        final EntityAccount rawAccount = (EntityAccount) q.list().get(0);
        rawAccount.setLastLoginTime(now);
        rawAccount.setLastDeviceIpAddressUsed(ip);
        rawAccount.setLastDeviceUsed(userAgent);
        
        session.update(rawAccount);
        */
    }
}
