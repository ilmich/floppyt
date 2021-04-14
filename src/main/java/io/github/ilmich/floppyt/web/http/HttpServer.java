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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.github.ilmich.floppyt.io.PlainIOHandler;
import io.github.ilmich.floppyt.io.SSLIOHandler;
import io.github.ilmich.floppyt.io.connectors.ServerConnector;

public class HttpServer {

	private List<ServerConnector> connectors = new ArrayList<ServerConnector>();
	private HttpProtocol hp = new HttpProtocol();
	public static final String SERVER_VERSION = "Floppyt/0.6.0";

	public void startAndWait() {
		Iterator<ServerConnector> iter = connectors.iterator();
		ServerConnector conn = iter.next();
		while (iter.hasNext()) {
			iter.next().start();
		}		
		conn.startAndWait();
	}

	public void start() {
		Iterator<ServerConnector> iter = connectors.iterator();
		while (iter.hasNext()) {
			iter.next().start();
		}		
	}

	public void stop() {
		Iterator<ServerConnector> iter = connectors.iterator();
		while (iter.hasNext()) {
			iter.next().shutDown();
		}		
	}
	
	public HttpServer route(String route, HttpRequestHandler handler) {
		if (this.hp.getFactory() == null) {
			this.hp.setFactory(new HttpHandlerFactory());
		}
		this.hp.getFactory().route(route, handler);
		return this;
	}
	
	public HttpServer listen(InetSocketAddress addr) {
		PlainIOHandler pih = new PlainIOHandler(hp);
		ServerConnector sc = new ServerConnector(pih);
		sc.bind(addr);
		this.connectors.add(sc);
		return this;
	}

	public HttpServer listen(int port) {
		return this.listen(new InetSocketAddress(port));
	}
}
