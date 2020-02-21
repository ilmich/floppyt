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
import io.github.ilmich.floppyt.util.ExceptionUtils;
import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.handler.HandlerFactory;
import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;
import io.github.ilmich.floppyt.web.http.protocol.HttpVerb;

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

	private HandlerFactory factory = null;

	public HttpProtocol() {
		super();		
	}

	public HttpProtocol(HandlerFactory factory) {
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
			return request;
		}
		return null;
	}

	public Response processRequest(final Request request) {		
		HttpResponse response = new HttpResponse(request.isKeepAlive());
		HttpRequestHandler rh = (HttpRequestHandler) factory.getHandler(request);
		
		if (rh != null) {
			HttpVerb method = request.getMethod();
			try {
				switch (method) {
				case GET:
					rh.get((HttpRequest)request, response);
					break;
				case POST:
					rh.post((HttpRequest)request, response);
					break;
				case HEAD:
					rh.head((HttpRequest)request, response);
					break;
				case PUT:
					rh.put((HttpRequest)request, response);
					break;
				case PATCH:
					rh.patch((HttpRequest)request, response);
					break;
				case DELETE:
					rh.delete((HttpRequest)request, response);
					break;
				case OPTIONS:
					rh.option((HttpRequest)request, response);
					break;
				case TRACE:
				case CONNECT:
				default:
					Log.warn(TAG, "Unimplemented Http metod received: " + method);
					response.reset();
					response.setStatus(HttpStatus.CLIENT_ERROR_METHOD_NOT_ALLOWED);
				}
			} catch (HttpException he) {
				response.reset();
				response.setStatus(he.getStatus());
				Log.error(TAG, ExceptionUtils.getStackTrace(he));
				Log.error(TAG, request.toString());
				response.write(ExceptionUtils.getStackTrace(he));
			} catch (Exception ex) {
				response.reset();
				response.setStatus(HttpStatus.SERVER_ERROR_INTERNAL_SERVER_ERROR);
				Log.error(TAG, ExceptionUtils.getStackTrace(ex));
				Log.error(TAG, request.toString());
				response.write(ExceptionUtils.getStackTrace(ex));
			}
		} else {
			
		}
		
		Map<String, String> labels = new HashMap<String, String>();
		labels.put("method", request.getMethod().toString());
		labels.put("status", String.valueOf(response.getStatus().code()));
		Counter ct = Metrics.getCounter("http_request_total", labels);
		ct.increment();
		
		Log.info(TAG, request.getRequestLine() + " " + response.getStatus().code());
		response.setHeader("Server", "Floppyt/0.5.0");
		response.prepare();
		return response;
	}

	public HandlerFactory getFactory() {
		return factory;
	}

	public void setFactory(HandlerFactory factory) {
		this.factory = factory;
	}
}
