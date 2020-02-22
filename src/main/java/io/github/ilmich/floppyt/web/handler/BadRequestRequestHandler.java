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
package io.github.ilmich.floppyt.web.handler;

import io.github.ilmich.floppyt.web.http.HttpRequest;
import io.github.ilmich.floppyt.web.http.HttpRequestHandler;
import io.github.ilmich.floppyt.web.http.HttpResponse;
import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;

public class BadRequestRequestHandler extends HttpRequestHandler {

	private final static BadRequestRequestHandler instance = new BadRequestRequestHandler();

	public static final BadRequestRequestHandler getInstance() {
		return instance;
	}

	@Override
	public void get(HttpRequest request, HttpResponse response) {
		perform(request, response);
	}

	@Override
	public void post(HttpRequest request, HttpResponse response) {
		perform(request, response);
	}

	@Override
	public void put(HttpRequest request, HttpResponse response) {
		perform(request, response);
	}

	@Override
	public void delete(HttpRequest request, HttpResponse response) {
		perform(request, response);
	}

	@Override
	public void head(HttpRequest request, HttpResponse response) {
		perform(request, response);
	}

	@Override
	public void option(HttpRequest request, HttpResponse response) {
		perform(request, response);
	}

	public void perform(HttpRequest request, HttpResponse response) {
		response.setStatus(HttpStatus.CLIENT_ERROR_BAD_REQUEST);
		response.setHeader("Connection", "close");
		response.write("HTTP 1.1 requests must include the Host: header");
	}
}
