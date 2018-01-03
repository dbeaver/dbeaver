package org.jkiss.dbeaver.ext.postgresql.pldbg.impl;

public class DebugSessionResult {

	private final boolean result;
	
	private final Exception exception;

	public DebugSessionResult(boolean result, Exception exception) {
		super();
		this.result = result;
		this.exception = exception;
	}

	public boolean isResult() {
		return result;
	}

	public Exception getException() {
		return exception;
	}
	
	
	
}
