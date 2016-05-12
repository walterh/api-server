package com.llug.api.repository;

import static ch.lambdaj.Lambda.joinFrom;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.wch.commons.utils.ArgumentException;
import com.wch.commons.utils.LogUtils;
import com.wch.commons.utils.Utils;

@Slf4j
@Repository
public class MemcacheRepository {
    @Value("$api{memcache.cluster}")
    private String memcacheCluster;

    @Value("$api{memcache.defaultExpirySeconds}")
    private Integer defaultExpirySeconds;
    
    private MemcachedClient memcacheClient;

    // default constructor for @Repository bean injection
    public MemcacheRepository() {
    }


    public MemcacheRepository(String memcacheCluster, Integer defaultExpirySeconds) throws IOException {
        this.memcacheCluster = memcacheCluster;
        this.defaultExpirySeconds = defaultExpirySeconds;
        
        initialize();
    }

    @PostConstruct
    public void initialize() throws IOException {
        List<String> clusterComponents = Utils.stringSplitRemoveEmptyEntries(memcacheCluster, "[\\[\\]',]", true, true);
        String init = String.format("%s", joinFrom(clusterComponents, " "));

        log.debug(String.format("Initializing memcache with '%s'", init));

        memcacheClient = new MemcachedClient(AddrUtil.getAddresses(init));
    }

    public long incr(String key,int by,int def,int expire){
        try {
            return memcacheClient.incr(key, by, def, expire);
        } catch (Exception e) {
            log.debug(LogUtils.getStackTrace(e));
        }
        return -1;
    }

    public long decr(String key,int by,int def,int expire){
        try {
           return  memcacheClient.decr(key, by, def, expire);
        } catch (Exception e) {
            log.debug(LogUtils.getStackTrace(e));
        }
        return -1;
    }

    public void set(String key, Object obj) {
        set(key, defaultExpirySeconds, obj);
    }

    public void set(String key, int timeInSeconds, Object obj) {
        log.debug(String.format("memcache setting {\"%s\", \"%s\"}", key, obj.toString()));

        if (!(obj instanceof Serializable)) {
            throw new ArgumentException(String.format("%s is not serializable", obj.getClass().getSimpleName()));
        }
        
        try {
            memcacheClient.set(key, timeInSeconds, obj);
        } catch (Exception e) {
            log.debug(LogUtils.getStackTrace(e));
        }
    }

    public Boolean exists(String key) {
        return get(key, true, true) != null;
    }

    public Object get(String key) {
        return get(key, false, true);
    }

    private Object get(String key, Boolean existenceCheck, Boolean retry) {
        Object obj = null;

        try {
            obj = memcacheClient.get(key);
        } catch (Exception e) {
            if (retry) {
                return get(key, existenceCheck, false);
            } else {
                log.debug(LogUtils.getStackTrace(e));
            }
        }

        if (existenceCheck) {
            if (obj != null) {
                log.debug(String.format("memcache key \"%s\" exists", key));
            } else {
                log.debug(String.format("memcache key \"%s\" doesn't exist", key));
            }
        } else {
            if (obj != null) {
                log.debug(String.format("memcache getting \"%s\" = %s", key, obj.toString()));
            } else {
                log.debug(String.format("memcache getting \"%s\" = null", key));
            }
        }
        return obj;
    }

    public void clear(String key) {
        try {
            memcacheClient.delete(key);
            log.debug(String.format("memcache clearing \"%s\"", key));
        } catch (Exception e) {
            log.debug(LogUtils.getStackTrace(e));
        }
    }
}
