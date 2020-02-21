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

/**
 * This class provides a possiblity to change the tunables used by floppyt for the
 * HTTP server configuration. Do not change the values unless you know what you
 * are doing.
 */
public class HttpServerDescriptor {

	/**
	 * The number of seconds floppyt will wait for subsequent socket activity
	 * before closing the connection
	 */
	public static int KEEP_ALIVE_TIMEOUT = 30 * 1000; // 30s

	/**
	 * Size of the read (receive) buffer. "Ideally, an HTTP request should not go
	 * beyond 1 packet. The most widely used networks limit packets to approximately
	 * 1500 bytes, so if you can constrain each request to fewer than 1500 bytes,
	 * you can reduce the overhead of the request stream." (from:
	 * http://bit.ly/bkksUu)
	 */
	public static int READ_BUFFER_SIZE = 1024; // 1024 bytes

	/**
	 * Size of the write (send) buffer.
	 */
	public static int WRITE_BUFFER_SIZE = 1024; // 1024 bytes

	public static int MIN_THREADS_PROCESSOR = 1;

	public static int MAX_THREADS_PROCESSOR = 1024;

	public static int THREAD_PROCESSOR_IDLE_TIME = 60;

	public static final long MAX_BODY = 1024000;

}
