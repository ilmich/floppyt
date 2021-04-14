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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import io.github.ilmich.floppyt.util.Closeables;
import io.github.ilmich.floppyt.util.CookieUtil;
import io.github.ilmich.floppyt.util.DateUtil;
import io.github.ilmich.floppyt.util.DynamicByteBuffer;
import io.github.ilmich.floppyt.util.HttpUtil;
import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.util.Strings;
import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;

public class HttpServerResponse implements Response {

	private static final String TAG = "HttpResponse";

	private HttpStatus status = HttpStatus.SUCCESS_OK;

	private final Map<String, String> headers = new HashMap<String, String>();
	private final Map<String, String> cookies = new HashMap<String, String>();
	private boolean headersCreated = false;
	private DynamicByteBuffer responseData = DynamicByteBuffer.allocate(HttpServerDescriptor.WRITE_BUFFER_SIZE);
	private FileChannel file;
	private Charset mainCharset = Charset.forName("ASCII");

	private boolean createETag;
	private SSLEngine sslEngine;

	@Override
	public DynamicByteBuffer getResponseData() {
		return responseData;
	}

	public HttpServerResponse(boolean keepAlive) {
		headers.put("Date", DateUtil.getCurrentAsString());
		setKeepAlive(keepAlive);
	}

	public void setKeepAlive(boolean keepAlive) {
		headers.put("Connection", keepAlive ? "Keep-Alive" : "close");
	}

	public boolean isKeepAlive() {
		if (headers.containsKey("Connection"))
			return (headers.get("Connection").equals("Keep-Alive") ? true : false);
		return false;
	}

	public Response setStatus(HttpStatus status) {
		this.status = status;
		return this;
	}
	
	public HttpStatus getStatus() {
		return this.status;		
	}

	public void setCreateETag(boolean create) {
		createETag = create;
	}

	public Response setHeader(String header, String value) {
		headers.put(header, value);
		return this;
	}

	public void setCookie(String name, String value) {
		setCookie(name, value, -1, null, null, false, false);
	}

	public void setCookie(String name, String value, long expiration) {
		setCookie(name, value, expiration, null, null, false, false);
	}

	public void setCookie(String name, String value, String domain) {
		setCookie(name, value, -1, domain, null, false, false);
	}

	public void setCookie(String name, String value, String domain, String path) {
		setCookie(name, value, -1, domain, path, false, false);
	}

	public void setCookie(String name, String value, long expiration, String domain) {
		setCookie(name, value, expiration, domain, null, false, false);
	}

	public void setCookie(String name, String value, long expiration, String domain, String path) {
		setCookie(name, value, expiration, domain, path, false, false);
	}

	public void setCookie(String name, String value, long expiration, String domain, String path, boolean secure,
			boolean httpOnly) {
		if (Strings.isNullOrEmpty(name)) {
			throw new IllegalArgumentException("Cookie name is empty");
		}
		if (name.trim().startsWith("$")) {
			throw new IllegalArgumentException("Cookie name is not valid");
		}
		StringBuffer sb = new StringBuffer(name.trim() + "=" + Strings.nullToEmpty(value).trim() + "; ");

		/*
		 * if (CharMatcher.JAVA_ISO_CONTROL.countIn(sb) > 0) { throw new
		 * IllegalArgumentException("Invalid cookie " + name + ": " + value); }
		 */

		if (expiration >= 0) {
			if (expiration == 0) {
				sb.append("Expires=" + DateUtil.getDateAsString(new Date(0)) + "; ");
			} else {
				sb.append("Expires=" + CookieUtil.maxAgeToExpires(expiration) + "; ");
			}
		}
		if (!Strings.isNullOrEmpty(domain)) {
			sb.append("Domain=" + domain.trim() + "; ");
		}
		if (!Strings.isNullOrEmpty(path)) {
			sb.append("Path=" + path.trim() + "; ");
		}
		if (secure) {
			sb.append("Secure; ");
		}
		if (httpOnly) {
			sb.append("HttpOnly; ");
		}
		cookies.put(name, sb.toString());
	}

	public void clearCookie(String name) {
		if (Strings.emptyToNull(name) != null) {
			setCookie(name, null, 0);
		}
	}

	public Response write(String data) {
		return write(data.getBytes(mainCharset));
	}

	@Override
	public Response write(byte[] data) {
		responseData.put(data);
		return this;
	}

	public Response setContentType(String contentType) {
		return setHeader("Content-Type", contentType);
	}

	public void prepare() {
		setEtagAndContentLength();
		if (!headersCreated) {
			String initial = createInitalLineAndHeaders();
			responseData.prepend(initial);
			headersCreated = true;
		}
		responseData.flip();
	}

	private void setEtagAndContentLength() {
		setHeader("Content-Length", String.valueOf(responseData.position()));
		if (responseData.position() > 0 && createETag) {
			setHeader("Etag", HttpUtil.getEtag(responseData.array()));						
		}
	}

	private String createInitalLineAndHeaders() {
		StringBuilder sb = new StringBuilder(status.line());
		for (Map.Entry<String, String> header : headers.entrySet()) {
			sb.append(header.getKey());
			sb.append(": ");
			sb.append(header.getValue());
			sb.append("\r\n");
		}
		for (String cookie : cookies.values()) {
			sb.append("Set-Cookie: " + cookie + "\r\n");
		}

		sb.append("\r\n");
		return sb.toString();
	}

	/**
	 * Experimental support.
	 */
	@Override
	public long write(File file) {
		// setHeader("Etag", HttpUtil.getEtag(file));
		setHeader("Content-Length", String.valueOf(file.length()));
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			this.file = in.getChannel();
		} catch (IOException e) {
			Log.error(TAG, "Error writing (static file " + file.getAbsolutePath() + ") to response: " + e.getMessage());
			// If an exception occurs here we should ensure that file is closed
			Closeables.closeQuietly(in);
		}

		return 0;
	}
	
	@Override
	public FileChannel getFile() {
		return file;
	}

	/*
	 * Reset response
	 * 
	 */
	public void reset() {
		this.responseData.clear();
		this.headers.clear();
		this.headersCreated = false;
		this.cookies.clear();
		this.file = null;
	}

	@Override
	public long flush() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long finish() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public SSLEngine getSSLEngine() {		
		return this.sslEngine;
	}

	@Override
	public void setSSLEngine(SSLEngine engine) {
		this.sslEngine = engine;
		
	}
}
