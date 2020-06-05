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

import io.github.ilmich.floppyt.util.ExceptionUtils;
import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;
import io.github.ilmich.floppyt.web.http.protocol.HttpVerb;

public abstract class HttpRequestHandler {

	private static final String TAG = "HttpRequestHandler";
	
	public String getCurrentUser(Request request) {
		return null;
	}

	public void get(HttpServerRequest request, HttpServerResponse response) {
		response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
		response.write("Not implemented");
	}

	public void post(HttpServerRequest request, HttpServerResponse response) {
		response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
		response.write("Not implemented");
	}

	public void put(HttpServerRequest request, HttpServerResponse response) {
		response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
		response.write("Not implemented");
	}

	public void delete(HttpServerRequest request, HttpServerResponse response) {
		response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
		response.write("Not implemented");
	}

	public void head(HttpServerRequest request, HttpServerResponse response) {
		response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
		response.write("Not implemented");
	}

	public void option(HttpServerRequest request, HttpServerResponse response) {
		response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
		response.write("Not implemented");
	}

	public void patch(HttpServerRequest request, HttpServerResponse response) {
		response.setStatus(HttpStatus.SERVER_ERROR_NOT_IMPLEMENTED);
		response.write("Not implemented");
	}
	
	public void handle(HttpServerRequest request, HttpServerResponse response) {		
		HttpVerb method = request.getMethod();
		try {
			switch (method) {
			case GET:
				this.get((HttpServerRequest)request, response);
				break;
			case POST:
				this.post((HttpServerRequest)request, response);
				break;
			case HEAD:
				this.head((HttpServerRequest)request, response);
				break;
			case PUT:
				this.put((HttpServerRequest)request, response);
				break;
			case PATCH:
				this.patch((HttpServerRequest)request, response);
				break;
			case DELETE:
				this.delete((HttpServerRequest)request, response);
				break;
			case OPTIONS:
				this.option((HttpServerRequest)request, response);
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
	}
	
}
