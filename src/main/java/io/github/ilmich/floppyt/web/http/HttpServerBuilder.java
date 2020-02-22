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

import io.github.ilmich.floppyt.io.PlainIOHandler;
import io.github.ilmich.floppyt.io.connectors.ServerConnector;
import io.github.ilmich.floppyt.web.handler.HandlerFactory;

public class HttpServerBuilder {

	private HttpServer instance = new HttpServer();
	private HttpProtocol protocol = new HttpProtocol();

	public HttpServerBuilder bindPlain(int port) {
		PlainIOHandler hndl = new PlainIOHandler(protocol);
		ServerConnector conn = new ServerConnector(hndl);
		conn.bind(port);		
		hndl.setConnector(conn);
		instance.addConnector(conn);

		return this;
	}

	public HttpServerBuilder addRoute(String route, HttpRequestHandler handler) {
		if (this.protocol.getFactory() == null) {
			this.protocol.setFactory(new HttpHandlerFactory());
		}
		this.protocol.getFactory().addRoute(route, handler);
		return this;
	}

	public HttpServerBuilder setHandlerFactory(HandlerFactory factory) {
		this.protocol.setFactory(factory);
		return this;
	}

	public HttpServer build() {
		return this.instance;
	}

}
