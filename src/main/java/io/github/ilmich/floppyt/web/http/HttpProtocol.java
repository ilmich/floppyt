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
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;

import io.github.ilmich.floppyt.io.Protocol;
import io.github.ilmich.floppyt.metrics.Counter;
import io.github.ilmich.floppyt.metrics.Metrics;
import io.github.ilmich.floppyt.util.Log;

public class HttpProtocol extends Protocol {

	private static final String TAG = "HttpProtocol";
	/**
	 * a queue of half-baked (pending/unfinished) HTTP post request
	 */
	private final Map<SelectableChannel, HttpRequest> partials = new HashMap<SelectableChannel, HttpRequest>();

	/**
	 * Http request parser
	 */
	private HttpRequestParser parser = new HttpRequestParser();

	private HttpHandlerFactory factory = null;

	public HttpProtocol() {
		super();		
	}

	public HttpProtocol(HttpHandlerFactory factory) {
		super();
		this.factory = factory;		
	}

	public Request onRead(final ByteBuffer buffer, SocketChannel client) {
		HttpRequest request = parser.parseRequestBuffer(buffer, partials.get(client));
		if (!request.isFinished()) {
			partials.put(client, request);
		} else {
			partials.remove(client);
		}
		if (request.expectContinue() || request.isFinished()) {
			request.setRemoteHost(client.socket().getInetAddress());
			return request;
		}
		return null;
	}

	public Response processRequest(final Request request) {		
		HttpResponse response = new HttpResponse(request.isKeepAlive());
		HttpRequestHandler rh = (HttpRequestHandler) factory.getHandler(request);
		
		if (rh != null) {
			rh.handle((HttpRequest) request, response);
		} 
		
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("method", request.getMethod().toString());
		labels.put("status", String.valueOf(response.getStatus().code()));
		Counter ct = Metrics.getCounter("http_request_total", labels);
		ct.increment();
		if (response.getStatus().code() > 400) {
			Log.error(TAG, request.getRemoteHost() + " \"" + request.getRequestLine() + "\" " +
					response.getStatus().code() + " " + response.getResponseData().position() + " \"" + request.getUserAgent() + "\"");
		} else {
			Log.debug(TAG, request.getRemoteHost() + " \"" + request.getRequestLine() + "\" " +
				response.getStatus().code() + " " + response.getResponseData().position() + " \"" + request.getUserAgent() + "\"");
		}
		response.setHeader("Server", HttpServer.SERVER_VERSION);
		response.prepare();
		return response;
	}

	public HttpHandlerFactory getFactory() {
		return factory;
	}

	public void setFactory(HttpHandlerFactory factory) {
		this.factory = factory;
	}
}
