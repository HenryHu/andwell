package net.henryhu.andwell;

import java.io.IOException;

import org.json.JSONException;

public class Exceptions {
	static String getErrorMsg(Exception e) {
		if (e instanceof JSONException) {
			return "JSON parse error: " + e.getMessage();
		} else if (e instanceof IOException) {
			return "IOException " + e.getMessage();
		} else if (e instanceof NotFoundException) {
			return "Not found";
		} else if (e instanceof OutOfRangeException) {
			return "Out of range";
		} else if (e instanceof ServerErrorException) {
			return "Server error: " + e.getMessage();
		} else {
			return "Exception: " + e.getMessage();
		}
	}
}

class NotFoundException extends Exception {
	private static final long serialVersionUID = 1L;
	NotFoundException(String msg) {
		super(msg);
	}
}

class OutOfRangeException extends Exception {
	private static final long serialVersionUID = 1L;
	OutOfRangeException(String msg) {
		super(msg);
	}
}

class ServerErrorException extends Exception {
	private static final long serialVersionUID = 1L;
	ServerErrorException(String msg) {
		super(msg);
	}
}
