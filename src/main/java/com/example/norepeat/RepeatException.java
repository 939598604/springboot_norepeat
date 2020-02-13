package com.example.norepeat;
public class RepeatException extends RuntimeException {
	public RepeatException(String message) {
		super(message);
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}
}
