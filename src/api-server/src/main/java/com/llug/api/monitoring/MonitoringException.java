package com.llug.api.monitoring;

@SuppressWarnings("serial")
public class MonitoringException extends RuntimeException {
	public final static String ERROR_MAX_INVOCATION_COUNT = "The method %s in class %s exceeded its concurrency call limit.";
	
	public MonitoringException() {
	}
	
	public MonitoringException(String message) {
		super(message);
	}

	public MonitoringException(Throwable cause) {
		super(cause);
	}

	public MonitoringException(String message, Throwable cause) {
		super(message, cause);
	}
}
