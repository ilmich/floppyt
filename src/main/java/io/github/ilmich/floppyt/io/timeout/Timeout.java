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
package io.github.ilmich.floppyt.io.timeout;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import io.github.ilmich.floppyt.io.connectors.ServerConnector;
import io.github.ilmich.floppyt.web.AsyncCallback;

public class Timeout {

	private final long timeout;
	private final AsyncCallback cb;
	private boolean cancelled = false;
	
	public Timeout(long timeout, AsyncCallback cb) {
		this.timeout = timeout;
		this.cb = cb;
	}

	public long getTimeout() {
		return timeout;
	}

	public void cancel() {
		cancelled = true;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public AsyncCallback getCallback() {
		return cancelled ? AsyncCallback.nopCb : cb;
	}

	public static Timeout newKeepAliveTimeout(SelectableChannel clientChannel, long keepAliveTimeout,
			ServerConnector conn) {
		return new Timeout(System.currentTimeMillis() + keepAliveTimeout, new AsyncCallback() {
			public void onCallback() {
				conn.closeChannel((SocketChannel) clientChannel);
			}
		});
	}
}
