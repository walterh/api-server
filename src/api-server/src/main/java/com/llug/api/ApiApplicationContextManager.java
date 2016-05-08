package com.llug.api;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.wch.commons.utils.LogUtils;

//http://stackoverflow.com/questions/475785/getting-spring-application-context-from-a-non-bean-object-without-using-singleto?lq=1
@Slf4j
@Component
public class ApiApplicationContextManager implements ApplicationContextAware {
    private static ApplicationContext _appCtx;

    public static ApplicationContext getAppContext() {
        return _appCtx;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        _appCtx = applicationContext;

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                log.warn("ApiServiceApp shutting down...");

                try {
                    // http://stackoverflow.com/questions/14423980/how-to-close-a-spring-application-context
                    ((ConfigurableApplicationContext) _appCtx).close();
                } catch (Exception exception) {
                    log.error(LogUtils.getStackTrace(exception));
                    exception.printStackTrace();
                }
            }
        }, "Stop Jetty Hook"));
    }
}