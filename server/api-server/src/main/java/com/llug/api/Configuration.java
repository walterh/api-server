package com.llug.api;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {
    protected Logger logger = LoggerFactory.getLogger(getClass().getName());

    private Properties properties = new Properties();

    public Configuration(String fileName) {
        load(fileName);
    }

    public boolean load(String path) {
        try {
            this.properties.clear();
            InputStream is = ClassLoader.getSystemResourceAsStream(path);
            this.properties.load(is);
        } catch (Exception ex) {
            this.logger.error("exception occurred when reading config file=" + path);
            return false;
        }
        return true;
    }

    public int getIntValue(String key) {
        String value = this.properties.getProperty(key);
        return Integer.valueOf(value).intValue();
    }

    public int getIntValue(String key, String defaultValue) {
        String value = this.properties.getProperty(key, defaultValue);
        return Integer.valueOf(value).intValue();
    }

    public String getStringValue(String key) {
        return this.properties.getProperty(key);
    }

    public String getStringValue(String key, String defaultValue) {
        return this.properties.getProperty(key, defaultValue);
    }
}