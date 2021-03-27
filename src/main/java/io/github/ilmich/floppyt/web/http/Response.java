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
import java.nio.channels.FileChannel;

import io.github.ilmich.floppyt.util.DynamicByteBuffer;
import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;

/**
 * An HTTP response build and sent to a client in response to a {@link Request}
 */
public interface Response {

	/**
	 * The given data data will be sent as the HTTP response upon next flush or when
	 * the response is finished.
	 * 
	 * @return this for chaining purposes.
	 */
	Response write(String data);

	/**
	 * The given data data will be sent as the HTTP response upon next flush or when
	 * the response is finished.http://mail.google.com/mail/?shva=1#inbox
	 * 
	 * @param data the data to write.
	 * @return <code>this</code>, for chaining.
	 */
	Response write(byte[] data);

	/**
	 * Experimental support.
	 */
	long write(File file);

	/**
	 * Explicit flush.
	 * 
	 * @return the number of bytes that were actually written as the result of this
	 *         flush.
	 */
	long flush();

	/**
	 * Should only be invoked by third party asynchronous request handlers (or by
	 * the floppyt framework for synchronous request handlers). If no previous
	 * (explicit) flush is invoked, the "Content-Length" and (where configured)
	 * "ETag" header will be calculated and inserted to the HTTP response.
	 * 
	 * @see #setCreateETag(boolean)
	 */
	long finish();

	public void reset();

	public void prepare();

	public DynamicByteBuffer getResponseData();	

	public boolean isKeepAlive();

	public Response setStatus(HttpStatus status);

	public Response setHeader(String header, String value);

	public void setCookie(String name, String value);

	public void setCookie(String name, String value, long expiration);

	public void setCookie(String name, String value, String domain);

	public void setCookie(String name, String value, String domain, String path);

	public void setCookie(String name, String value, long expiration, String domain);

	public void setCookie(String name, String value, long expiration, String domain, String path);

	public void clearCookie(String name);

}
