/*
MIT License

Copyright (c) 2020 Michele Zuccalà

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package io.github.ilmich.floppyt.web.http;

import java.nio.ByteBuffer;

/**
 * Context object holding data of the currently or last parser execution. Used
 * to maintain buffer position, last Token,
 */
public class HttpParsingContext {

	enum TokenType {
		REQUEST_LINE, REQUEST_METHOD, REQUEST_URI, HTTP_VERSION, HEADER_NAME, HEADER_VALUE, BODY, NO_CHUNK, CHUNK_OCTET,
		CHUNK;

	}

	boolean chunked;

	ByteBuffer buffer;

	TokenType currentType = TokenType.REQUEST_LINE;

	int skips = 0;

	StringBuilder tokenValue = new StringBuilder(255);

	boolean complete = false;

	int currentPointer = 0;

	String lastHeaderName = null;

	int chunkSize = 0;

	int incrementAndGetPointer() {
		currentPointer = buffer.get();
		return currentPointer;
	}

	public boolean tokenGreaterThan(int maxLen) {
		return tokenValue.length() > maxLen;
	}

	void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	boolean hasRemaining() {
		return buffer.hasRemaining();
	}

	void setBodyFound() {
		currentType = TokenType.BODY;
		tokenValue.delete(0, Integer.MAX_VALUE);
	}

	public boolean isbodyFound() {
		return TokenType.BODY.equals(currentType);
	}

	void clearTokenBuffer() {
		if (complete) { // Free buffer when last was complete
			tokenValue.delete(0, Integer.MAX_VALUE);
		}
	}

	void deleteFirstCharFromTokenBuffer() {
		tokenValue.deleteCharAt(0);
	}

	void appendChar() {
		tokenValue.append((char) currentPointer);
	}

	/**
	 * Stores the token value and define the completeness
	 */
	void storeIncompleteToken() {
		storeTokenValue(currentType, false);
	}

	void storeCompleteToken(TokenType type) {
		storeTokenValue(type, true);
	}

	private void storeTokenValue(TokenType type, boolean _complete) {

		currentType = type;
		complete = _complete;
	}

	String getTokenValue() {
		return tokenValue.toString();
	}

	public void persistHeaderName() {
		lastHeaderName = tokenValue.toString();
	}

	public String getLastHeaderName() {
		return lastHeaderName;
	}
}
