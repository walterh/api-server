package com.llug.api.monitoring.aspect;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import com.llug.api.monitoring.MethodInvocationData;
import com.llug.api.monitoring.Monitored;
import com.llug.api.monitoring.MonitoringException;

@Component
@Aspect
public class MonitoringConcurrentMethodInvocationAspect {
    private static Map<Method, MethodInvocationData> methodInvocations;
    static {
        methodInvocations = new HashMap<Method, MethodInvocationData>();
    }

    public static Map<Method, MethodInvocationData> getMethodInvocations() {
        return methodInvocations;
    }

    @Around("@annotation(com.llug.api.monitoring.Monitored)")
    public Object monitorMaxInvocationCounts(final ProceedingJoinPoint joinPoint) throws Throwable {
        final Class<?> clazz = joinPoint.getTarget().getClass();
        final MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        final Method method = signature.getMethod();
        final Monitored justMeMonitoredAnnotation = method.getAnnotation(Monitored.class);

        Object ret = null;

        if (justMeMonitoredAnnotation.maxConcurrentInvocations() != Monitored.INFINITE_CONCURRENT_INVOCATIONS) {
            if (!methodInvocations.containsKey(method)) {
                methodInvocations.put(method, new MethodInvocationData(new AtomicInteger(0), new AtomicInteger(0)));
            }

            AtomicInteger count = methodInvocations.get(method).getCurrentConcurrentInvocationCount();
            AtomicInteger max = methodInvocations.get(method).getMaxConcurrentInvocationCount();

            if (count.incrementAndGet() > justMeMonitoredAnnotation.maxConcurrentInvocations()) {
                count.decrementAndGet();

                throw new MonitoringException(String.format(MonitoringException.ERROR_MAX_INVOCATION_COUNT, method.getName(), clazz.getName()));
            }

            // records largest instantaneous
            max.set(Math.max(count.get(), max.get()));

            try {
                ret = joinPoint.proceed(joinPoint.getArgs());
            } finally {
                count.decrementAndGet();

                //System.out.println(String.format("meth '%s' int = %d, count = %d", method, methodInvocations.get(method).getCurrentConcurrentInvocationCount().get(), count.get()));
            }
        } else {
            ret = joinPoint.proceed(joinPoint.getArgs());
        }

        return ret;
    }
}
