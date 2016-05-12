package com.llug.api.monitoring.aspect;

import static ch.lambdaj.Lambda.*;

import java.lang.reflect.Method;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.perf4j.chart.GoogleChartGenerator;
import org.perf4j.log4j.AsyncCoalescingStatisticsAppender;
import org.perf4j.log4j.GraphingStatisticsAppender;
import org.perf4j.log4j.Log4JStopWatch;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.llug.api.domain.ApiResponse;
import com.llug.api.domain.ApiResponse.ApiVersion;
import com.llug.api.domain.ApiResponse.ResultCode;
import com.llug.api.monitoring.Monitored;
import com.llug.api.utils.RequestUtils;
import com.wch.commons.utils.CryptoUtils;
import com.wch.commons.utils.Utils;

@Slf4j
@Component
@Aspect
public class MonitoringMethodInvocationTimingAspect {
    @Value("$api{sha256Key.ios}")
    private String sha256Key1;

    @Value("$api{sha256Key.android}")
    private String sha256Key2;

    @Value("$api{client.hash.header.ios}")
    private String securityHeader1;

    @Value("$api{client.hash.header.android}")
    private String securityHeader2;

    @Value("$api{enforce.securityhashes}")
    private Boolean enforceSecurityHashes;

    @Value("$api{client.device.header}")
    private String clientDeviceHeader;

    private String serverIpAddress;

    @SuppressWarnings("unused")
    @PostConstruct
    private void init() {
        serverIpAddress = Utils.getHostName();

        Reflections reflections = new Reflections(new ConfigurationBuilder().addUrls(ClasspathHelper.forPackage("com.lugg.api.controllers"))
                .setScanners(new MethodAnnotationsScanner()));
        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(Monitored.class);

        for (Method method : annotatedMethods) {
            Logger timingLogger = Logger.getLogger("org.perf4j.TimingLogger");
            AsyncCoalescingStatisticsAppender coalescingStatisticsAppender = (AsyncCoalescingStatisticsAppender) timingLogger.getAllAppenders().nextElement();
            @SuppressWarnings("unchecked")
            Enumeration<Appender> appenders = coalescingStatisticsAppender.getAllAppenders();

            while (appenders.hasMoreElements()) {
                Appender appender = appenders.nextElement();
                if (appender instanceof GraphingStatisticsAppender) {
                    GraphingStatisticsAppender graphingStatisticsAppender = (GraphingStatisticsAppender) appender;
                    graphingStatisticsAppender.setTagNamesToGraph(StringUtils.strip(new StringBuilder().append(graphingStatisticsAppender.getTagNamesToGraph())
                            .append(",")
                            .append(getTagName(method))
                            .append(",")
                            .toString(), ","));
                    GoogleChartGenerator googleChartGenerator = (GoogleChartGenerator) graphingStatisticsAppender.getChartGenerator();
                    googleChartGenerator.setWidth(900);
                    googleChartGenerator.setHeight(300);
                    googleChartGenerator.setMaxDataPoints(60);
                }
            }
        }
    }

    @Around("@annotation(com.llug.api.monitoring.Monitored)")
    public Object monitorInvocationTiming(final ProceedingJoinPoint joinPoint) throws Throwable {
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();
        final Monitored apiMonitoredAnnotation = method.getAnnotation(Monitored.class);
        final HttpServletRequest request = findRequest(joinPoint.getArgs());
        final String ip = RequestUtils.getOriginatingIp(request);

        Long apiManagementOverheadMs = null;
        Long rateLimitQuota = null;
        Log4JStopWatch stopWatch = null;

        Object ret = null;

        if (apiMonitoredAnnotation.hashSecured()) {
            final Boolean verified = verifyHash(request);

            if (!verified && enforceSecurityHashes) {
                ApiResponse api = new ApiResponse();

                api.setClient_ip(ip);
                api.setSession(request.getSession().getId());
                api.setStatus(ResultCode.validation_failed);

                return api;
            }
        }

        if (apiMonitoredAnnotation.timedPerformance()) {
            stopWatch = new Log4JStopWatch(getTagName(method));
        }

        apiManagementOverheadMs = stopWatch.getElapsedTime();

        if (ret == null) {
            ret = joinPoint.proceed(joinPoint.getArgs());
        }

        if (apiMonitoredAnnotation.timedPerformance()) {
            stopWatch.stop();
        }

        if (ret instanceof ApiResponse) {
            ApiResponse api = (ApiResponse) ret;
            api.setDelta_ms(stopWatch.getElapsedTime());
            api.setServer(serverIpAddress);

            String url = RequestUtils.getUrl(request);
            if (RequestUtils.isSecureRequest(request)) {
                url = url.replace("http://", "https://");
            }

            api.setUrl(url);

            if (request != null) {
                String sessionId = request.getSession().getId();
                api.setSession(sessionId);

                ApiVersion ver = RequestUtils.getApiVersion(request);
                api.setVersion(ver);
            }

            if (apiManagementOverheadMs != null) {
                api.setApi_mgmt_ms(apiManagementOverheadMs);
            }
        }
        return ret;
    }

    private String getTagName(Method method) {
        return StringUtils.substring(new StringBuilder(method.getName()).append("-").append(method.getDeclaringClass().getSimpleName()).toString(), 0, 19);
    }

    private HttpServletRequest findRequest(Object[] args) {
        if (args != null && args.length > 0) {
            for (Object arg : args) {
                if (arg instanceof HttpServletRequest) {
                    return (HttpServletRequest) arg;
                }
            }
        }

        return null;
    }

    private boolean verifyHash(final HttpServletRequest request) {
        boolean ret = false;
        try {
            final String deviceId = request.getHeader(clientDeviceHeader);
            final String hash1 = request.getHeader(securityHeader1);
            final String hash2 = request.getHeader(securityHeader2);
            final String apiString = RequestUtils.getApiStringForHash(request);

            final String calculatedHash = secretHash(apiString, hash1, hash2);
            final String hash = (!Utils.isNullOrEmptyString(hash1)) ? hash1 : hash2;

            if (Utils.isNullOrEmptyString(deviceId)) {
                log.warn(String.format("No deviceId provided for request = %s", apiString));
            } else if (Utils.isNullOrEmptyString(hash)) {
                log.warn(String.format("No security hash provided by deviceId = %s for request = %s", deviceId, apiString));
            } else if (hash.compareTo(calculatedHash) != 0) {
                log.error(String.format("Security hash mismatch (expected '%s', found '%s') provided by deviceId = %s for request = %s",
                        calculatedHash,
                        hash,
                        deviceId,
                        apiString));
            } else {
                log.warn(String.format("Security hash matched (expected '%s', found '%s') provided by deviceId = %s for request = %s",
                        calculatedHash,
                        hash,
                        deviceId,
                        apiString));
                ret = true;
            }
        } catch (Exception e) {
            log.error(RequestUtils.getExtendedRequestErrorInfo(request, null, e));
        }

        return ret;
    }

    private String secretHash(final String data, final String iosHash, final String androidHash) throws InvalidKeyException, NoSuchAlgorithmException {
        if (!Utils.isNullOrEmptyString(iosHash)) {
            return CryptoUtils.secretHashImpl(sha256Key1, data);
        } else if (!Utils.isNullOrEmptyString(androidHash)) {
            return CryptoUtils.secretHashImpl(sha256Key2, data);
        } else {
            return null;
        }
    }

}
