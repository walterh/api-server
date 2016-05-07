package com.llug.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.wch.commons.utils.LogUtils;

//http://stackoverflow.com/questions/475785/getting-spring-application-context-from-a-non-bean-object-without-using-singleto?lq=1
@Component
public class ApiApplicationContextManager implements ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(ApiApplicationContextManager.class);

    private static ApplicationContext _appCtx;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        _appCtx = ctx;

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                logger.warn("ApiServiceApp shutting down...");

                try {
                    // http://stackoverflow.com/questions/14423980/how-to-close-a-spring-application-context
                    ((ConfigurableApplicationContext) _appCtx).close();
                } catch (Exception exception) {
                    logger.error(LogUtils.getStackTrace(exception));
                    exception.printStackTrace();
                }
            }
        }, "Stop Jetty Hook"));

    }

    public static ApplicationContext getAppContext() {
        return _appCtx;
    }
}