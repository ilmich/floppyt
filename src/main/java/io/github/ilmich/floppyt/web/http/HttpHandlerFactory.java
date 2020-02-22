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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.ilmich.floppyt.util.HttpUtil;
import io.github.ilmich.floppyt.web.handler.BadRequestRequestHandler;
import io.github.ilmich.floppyt.web.handler.HandlerFactory;
import io.github.ilmich.floppyt.web.handler.HttpContinueRequestHandler;
import io.github.ilmich.floppyt.web.handler.NotFoundRequestHandler;

public class HttpHandlerFactory implements HandlerFactory {

	/**
	 * "Normal/Absolute" (non group capturing) RequestHandlers e.g. "/", "/persons"
	 */
	private Map<String, HttpRequestHandler> absoluteHandlers = new HashMap<String, HttpRequestHandler>();

	/**
	 * Group capturing RequestHandlers e.g. "/persons/([0-9]+)",
	 * "/persons/(\\d{1,3})"
	 */
	private Map<String, HttpRequestHandler> capturingHandlers = new HashMap<String, HttpRequestHandler>();

	/**
	 * A mapping between group capturing RequestHandlers and their corresponding
	 * pattern ( e.g. "([0-9]+)" )
	 */
	private Map<HttpRequestHandler, Pattern> patterns = new HashMap<HttpRequestHandler, Pattern>();
	
	public HttpHandlerFactory() {
		super();
	}

	public HttpHandlerFactory addRoute(String path, HttpRequestHandler handler) {
		int index = path.lastIndexOf("/");
		String group = path.substring(index + 1, path.length());
		if (containsCapturingGroup(group)) {
			// path ends with capturing group, e.g path ==
			// "/person/([0-9]+)"
			capturingHandlers.put(path.substring(0, index + 1), handler);
			patterns.put(handler, Pattern.compile(group));
		} else {
			// "normal" path, e.g. path == "/"
			absoluteHandlers.put(path, handler);
		}
		return this;
	}

	/**
	 * 
	 * @param path Requested path
	 * @return Returns the {@link RequestHandler} associated with the given path. If
	 *         no mapping exists a {@link NotFoundRequestHandler} is returned.
	 */
	private HttpRequestHandler getHandler(String path) {

		HttpRequestHandler rh = absoluteHandlers.get(path);
		if (rh != null) {
			return rh;
		}
		rh = getCapturingHandler(path);
		if (rh != null) {
			return rh;
		}
		
		return NotFoundRequestHandler.getInstance();
	}

	public HttpRequestHandler getHandler(Request request) {

		if (!HttpUtil.verifyRequest(request)) {
			return BadRequestRequestHandler.getInstance();
		}
		HttpRequestHandler rh = getHandler(request.getRequestedPath());
		if (rh == null) {
			return NotFoundRequestHandler.getInstance();
		}

		if (request.expectContinue()) {
			return HttpContinueRequestHandler.getInstance();
		}

		return rh;
	}

	private static boolean containsCapturingGroup(String group) {
		boolean containsGroup = group.matches("^\\(.*\\)$");
		Pattern.compile(group); // throws PatternSyntaxException if group is
		// malformed regular expression
		return containsGroup;
	}

	private HttpRequestHandler getCapturingHandler(String path) {
		int index = path.lastIndexOf("/");
		if (index != -1) {
			String init = path.substring(0, index + 1); // path without its last
			// segment
			String group = path.substring(index + 1, path.length());
			HttpRequestHandler handler = capturingHandlers.get(init);
			if (handler != null) {
				Pattern regex = patterns.get(handler);
				if (regex.matcher(group).matches()) {
					return handler;
				}
			}
		}
		return null;
	}

}
