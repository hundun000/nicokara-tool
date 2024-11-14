package com.github.dnbn.submerge.api.parser.exception;

public class InvalidFileException extends RuntimeException {

	private static final long serialVersionUID = -943455563476464982L;

	public InvalidFileException() {
	}

	public InvalidFileException(String message) {
		super(message);
	}

	public InvalidFileException(Throwable cause) {
		super(cause);
	}

	public InvalidFileException(String message, Throwable cause) {
		super(message, cause);
	}

}
