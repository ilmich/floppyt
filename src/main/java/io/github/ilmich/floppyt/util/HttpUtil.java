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
package io.github.ilmich.floppyt.util;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import io.github.ilmich.floppyt.web.http.Request;

public class HttpUtil {

	/*
	 * MessageDigest are not thread-safe and are expensive to create. Do it lazily
	 * for each thread that need access to one.
	 */
	private static final ThreadLocal<MessageDigest> md = new ThreadLocal<MessageDigest>();

	public static boolean verifyRequest(Request request) {
		String version = request.getVersion();
		boolean requestOk = true;
		if (version.equals("HTTP/1.1")) { // TODO might be optimized? Could do
			// version.endsWith("1"), or similar
			requestOk = request.getHeader("host") != null;
		}

		return requestOk;
	}

	public static String getEtag(byte[] bytes) {
		if (md.get() == null) {
			try {
				md.set(MessageDigest.getInstance("MD5"));
			} catch (NoSuchAlgorithmException e) {
				throw new RuntimeException("MD5 cryptographic algorithm is not available.", e);
			}
		}
		byte[] digest = md.get().digest(bytes);
		BigInteger number = new BigInteger(1, digest);
		// prepend a '0' to get a proper MD5 hash
		return '0' + number.toString(16);

	}

	public static String getEtag(File file) {
		// TODO RS 101011 Implement if etag response header should be present
		// while static file serving.
		return "";
	}
}
