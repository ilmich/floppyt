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

import io.github.ilmich.floppyt.util.DynamicByteBuffer;
import io.github.ilmich.floppyt.web.http.protocol.HttpVerb;

/**
 * Builds HttpRequest using a given ByteBuffer and already existing request
 * object (unfinished). Uses an HttpBufferedLexer to retrieve Http Tokens and
 * the HttpParsingContext stored in the request to maintain parsing state.
 */
public class HttpRequestParser {

	private final HttpBufferedLexer lexer;

	public HttpRequestParser() {
		lexer = new HttpBufferedLexer();
	}

	public HttpRequest parseRequestBuffer(ByteBuffer buffer) {
		return parseRequestBuffer(buffer, null);
	}

	/**
	 * Parse the data in the given buffer as an Http request. It handles segmented
	 * buffer when the given request is not null.
	 * 
	 * @param buffer ByteBuffer containing data to parse
	 * @param result null if it's a new request or the incomplete request
	 * @return new HttpRequestImpl if result is null representing a complete or
	 *         incomplete request on error, it will return a MalformedHttpRequest.
	 */
	public HttpRequest parseRequestBuffer(ByteBuffer buffer, HttpRequest result) {

		if (result == null) {
			result = new HttpRequest();
		}
		int status = 1;
		HttpParsingContext context = result.getContext();
		context.setBuffer(buffer);

		if (context.chunkSize > 0) {
			status = pushChunkToBody(buffer, result, context);
		}
		// Copy body data to the request bodyBuffer
		if (context.isbodyFound() && result.getContentLength() > 0) {
			pushRemainingToBody(context.buffer, result.getBodyBuffer(), result.getContentLength());
			status = 0;
		}

		// while no errors and buffer not finished
		while ((status = lexer.nextToken(context)) > 0) {
			switch (context.currentType) {
			case REQUEST_METHOD: {
				result.setMethod(HttpVerb.valueOf(context.getTokenValue().toUpperCase()));
				break;
			}
			case REQUEST_URI: {
				result.setURI(context.getTokenValue());
				break;
			}
			case HTTP_VERSION: {
				result.setVersion(context.getTokenValue());
				break;
			}
			case HEADER_NAME: {
				context.persistHeaderName();
				break;
			}
			case HEADER_VALUE: {
				result.pushToHeaders(context.getLastHeaderName(), context.getTokenValue());
				break;
			}
			case BODY: {
				result.initKeepAlive();
				// Copy body data to the request bodyBuffer
				if (result.getContentLength() > 0) {
					pushRemainingToBody(context.buffer, result.getBodyBuffer(), result.getContentLength());
					status = 0;
				} else if (result.isChunked() && !context.chunked) {
					context.chunked = true;
					context.currentType = HttpParsingContext.TokenType.CHUNK_OCTET;
					result.buildChunkedBody();
				} else { // BODY Found on chunked encoding so request done
					status = 0;
				}
				break;
			}
			case CHUNK_OCTET: {
				String[] parts = context.getTokenValue().split(";");
				if (parts.length > 0) {
					try {
						context.chunkSize = Integer.parseInt(parts[0].trim(), 16);
						if (context.chunkSize == 0) {// Last Chunk gets 0 so we
							// can try to read footers
							context.currentType = HttpParsingContext.TokenType.HTTP_VERSION;
						} else {
							result.incrementChunkSize(context.chunkSize);
							context.incrementAndGetPointer();
							status = pushChunkToBody(buffer, result, context);
						}
					} catch (NumberFormatException e) {
						// Error while reading size BadFormat :p
						status = -1;
					}
				}
			}
			}
			if (status <= 0) {
				break;
			}

		}

		// There was an error while parsing request
		if (status < 0) {
			result = MalFormedHttpRequest.instance;
		}

		return result;
	}

	private int pushChunkToBody(ByteBuffer buffer, HttpRequest result, HttpParsingContext context) {
		int size = (buffer.remaining() > context.chunkSize ? context.chunkSize : buffer.remaining());
		result.getBodyBuffer().put(buffer.array(), buffer.position(), size);
		context.chunkSize = context.chunkSize - size;

		buffer.position(buffer.position() + size);
		// Chunk not complete we need more data
		if (context.chunkSize > 0) {
			return 0;
		}
		return 1;
	}

	/**
	 * Fill's the body buffer with the data retrieved from the given buffer starting
	 * at buffer position and copying given size byte.<br/>
	 * This will ensure that body buffer does not contain more than size byte.
	 */
	private void pushRemainingToBody(ByteBuffer buffer, DynamicByteBuffer body, int size) {
		// If buffer is empty or there is no clength then skip this
		if (size == 0 || !buffer.hasRemaining()) {
			return;
		}

		if (body.position() + buffer.remaining() > size) {
			body.put(buffer.array(), buffer.position(), size - body.position());
		} else {
			body.put(buffer.array(), buffer.position(), buffer.remaining());
		}
	}
}