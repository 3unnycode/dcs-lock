package com.xiaoseller.dcs.lock.exception;

public class DCSLockException extends RuntimeException {

	private static final long serialVersionUID = -2759496560225944412L;

	public DCSLockException() {
		super();
	}

	public DCSLockException(String arg0) {
		super(arg0);
	}

	public DCSLockException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public DCSLockException(Throwable arg0) {
		super(arg0);
	}

	protected DCSLockException(String arg0, Throwable arg1, boolean arg2, boolean arg3) {
		super(arg0, arg1, arg2, arg3);
	}
}
