package net.henryhu.andwell;

import java.io.IOException;

public interface AuthHandler {
	void onAuthIOException(IOException e);
	void onAuthParseException(Exception e);
	void onAuthFail(String reason);
	void onAuthOK(String token);
}
