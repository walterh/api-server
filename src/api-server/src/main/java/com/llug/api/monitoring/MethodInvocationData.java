package com.llug.api.monitoring;

import java.util.concurrent.atomic.AtomicInteger;

public class MethodInvocationData {
	private AtomicInteger currentConcurrentInvocationCount;
	private AtomicInteger maxConcurrentInvocationCount;

	public MethodInvocationData(AtomicInteger currentConcurrentInvocationCount, AtomicInteger maxConcurrentInvocationCount) {
		this.currentConcurrentInvocationCount = currentConcurrentInvocationCount;
		this.maxConcurrentInvocationCount = maxConcurrentInvocationCount;
	}

	public AtomicInteger getCurrentConcurrentInvocationCount() {
		return currentConcurrentInvocationCount;
	}

	public AtomicInteger getMaxConcurrentInvocationCount() {
		return maxConcurrentInvocationCount;
	}
}
