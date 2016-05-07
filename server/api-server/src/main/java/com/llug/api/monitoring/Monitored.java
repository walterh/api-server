package com.llug.api.monitoring;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Monitored {
	int INFINITE_CONCURRENT_INVOCATIONS = Integer.MIN_VALUE; 
	
	boolean rateLimited() default true;
	boolean ipWhitelisted() default false;
	boolean timedPerformance() default true;
	
	boolean hashSecured() default false;
	
	int maxConcurrentInvocations() default 5000;
	
	boolean cached() default false;
	int cacheTimeSeconds() default 300;
}