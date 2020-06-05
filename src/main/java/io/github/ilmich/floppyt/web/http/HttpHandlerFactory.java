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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.ilmich.floppyt.util.HttpUtil;
import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;

public class HttpHandlerFactory {

	private static final String TAG = "HttpHandlerFactory";
	/**
	 * "Normal/Absolute" (non group capturing) HttpRequestHandlers e.g. "/", "/persons"
	 */
	private Map<String, HttpRequestHandler> absoluteHandlers = new HashMap<String, HttpRequestHandler>();

	private Map<Pattern, HttpRequestHandler> capturingHandlers = new HashMap<Pattern, HttpRequestHandler>();

	private Map<Pattern, List<String>> patterns = new HashMap<Pattern, List<String>>();
	
	private Pattern pathParamPattern = Pattern.compile("\\{(.*?)\\}");
	
	private HttpRequestHandler notFoundHandler = new HttpRequestHandler() {

		@Override
		public void handle(HttpServerRequest request, HttpServerResponse response) {
			response.setStatus(HttpStatus.CLIENT_ERROR_NOT_FOUND);			
			response.write("Requested URL: " + request.getRequestedPath() + " was not found");
		}
		
	};
	
	private HttpRequestHandler badRequestHandler = new HttpRequestHandler() {

		@Override
		public void handle(HttpServerRequest request, HttpServerResponse response) {
			response.setStatus(HttpStatus.CLIENT_ERROR_BAD_REQUEST);
			response.setHeader("Connection", "close");
			response.write("Bad Request");
		}
		
	};
	
	private HttpRequestHandler httpContinueHandler = new HttpRequestHandler() {

		@Override
		public void post(HttpServerRequest request, HttpServerResponse response) {
			response.setStatus(HttpStatus.SUCCESS_CONTINUE);
		}

		@Override
		public void put(HttpServerRequest request, HttpServerResponse response) {
			response.setStatus(HttpStatus.SUCCESS_CONTINUE);
		}

		@Override
		public void patch(HttpServerRequest request, HttpServerResponse response) {
			response.setStatus(HttpStatus.SUCCESS_CONTINUE);
		}
		
	}; 
	
	public HttpHandlerFactory() {
		super();
	}

	public HttpHandlerFactory route(String path, HttpRequestHandler handler) {
		Log.trace(TAG, "Adding route \"" + path + "\"");
		StringBuilder sb = new StringBuilder();
		Matcher mt = pathParamPattern.matcher(path);
		int start = 0;
		List<String> capturing = new ArrayList<String>();
		while (mt.find()) {
			sb.append(path.substring(start, mt.start()));
			start = mt.end();
			sb.append("(?<" + mt.group(1) + ">.*?)");
			capturing.add(mt.group(1));
		}
		sb.append(path.substring(start, path.length()));
		
		if (capturing.isEmpty()) {
			absoluteHandlers.put(path, handler);
			Log.trace(TAG, "Absolute route \"" + path + "\" added");
		} else {
			Pattern pt = Pattern.compile(sb.toString());
			capturingHandlers.put(pt, handler);
			patterns.put(pt, capturing);
			Log.trace(TAG, "Capturing group route \"" +  sb.toString() + "\" added");
		}
		
		return this;
	}

	public HttpRequestHandler getHandler(Request request) {

		if (!HttpUtil.verifyRequest(request)) {
			return badRequestHandler;
		}
		
		HttpRequestHandler rh = absoluteHandlers.get(request.getRequestedPath());
		if (rh == null) {
			for (Pattern pt : capturingHandlers.keySet()) {
				Matcher mt = pt.matcher(request.getRequestedPath());
				if (mt.matches()) {
					Map<String, String> parem = new HashMap<String, String>();
					for (String par : patterns.get(pt)) {			
						parem.put(par, mt.group(par));								
					}
					((HttpServerRequest) request).setPathParams(parem);
					rh = capturingHandlers.get(pt);
				}
			}
		}
		if (rh == null) 
			return notFoundHandler;

		if (request.expectContinue()) {
			return httpContinueHandler;
		}
		return rh;
	}

}
