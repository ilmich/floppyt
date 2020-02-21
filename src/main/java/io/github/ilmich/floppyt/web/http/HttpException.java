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

import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;

/**
 * Representation of an exception thrown by the server through the HTTP
 * protocol.
 */
public class HttpException extends RuntimeException {

	/** Serial Version UID */
	private static final long serialVersionUID = 8066634515515557043L;

	/** The HTTP status for this exception. */
	private final HttpStatus status;

	/**
	 * Create an instance of this type, with the given <code>HttpStatus</code> and
	 * an empty message.
	 * 
	 * @param status the <code>HttpStatus</code> to apply.
	 */
	public HttpException(HttpStatus status) {
		this(status, "");
	}

	/**
	 * Create an instance of this type, with the given <code>HttpStatus</code> and
	 * message.
	 * 
	 * @param status the <code>HttpStatus</code> to apply.
	 */
	public HttpException(HttpStatus status, String message) {
		super(message);
		this.status = status;
	}

	/**
	 * Retrieve the <code>HttpStatus</code> represented by this exception.
	 * 
	 * @return the represented <code>HttpStatus</code>.
	 */
	public HttpStatus getStatus() {
		return status;
	}
}
