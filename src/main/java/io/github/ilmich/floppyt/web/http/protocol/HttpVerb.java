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
package io.github.ilmich.floppyt.web.http.protocol;

/**
 * An <code>Enumeration</code> of all available HTTP verbs, as defined by
 * <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html">Hypertext
 * Transfer Protocol -- HTTP/1.1 (RFC 2616)</a>.
 */
public enum HttpVerb {

	/**
	 * &quot;The OPTIONS method represents a request for information about the
	 * communication options available on the request/response chain identified by
	 * the Request-URI.&quot; (RFC 2616, 9.2)
	 */
	OPTIONS,
	/**
	 * &quotThe GET method means retrieve whatever information (in the form of an
	 * entity) is identified by the Request-URI.&quot; (RFC 2616, 9.3)
	 */
	GET,
	/**
	 * &quot;The HEAD method is identical to GET except that the server MUST NOT
	 * return a message-body in the response.&quot; (RFC 2616, 9.4)
	 */
	HEAD,
	/**
	 * &quot;The POST method is used to request that the origin server accept the
	 * entity enclosed in the request as a new subordinate of the resource
	 * identified by the Request-URI in the Request-Line.&quot; (RFC 2616, 9.5)
	 */
	POST,
	/**
	 * &quot;The PUT method requests that the enclosed entity be stored under the
	 * supplied Request-URI.&quot; (RFC 2616, 9.6)
	 */
	PUT,
	/**
	 * 
	 */
	PATCH,
	/**
	 * &quot;The DELETE method requests that the origin server delete the resource
	 * identified by the Request-URI.&quot; (RFC 2616, 9.7)
	 */
	DELETE,
	/**
	 * &quot;The TRACE method is used to invoke a remote, application-layer loop-
	 * back of the request message.&quot; (RFC 2616, 9.8)
	 */
	TRACE,
	/**
	 * &quot;This specification reserves the method name CONNECT for use with a
	 * proxy that can dynamically switch to being a tunnel &quot; (RFC 2616, 9.9)
	 */
	CONNECT;
}
