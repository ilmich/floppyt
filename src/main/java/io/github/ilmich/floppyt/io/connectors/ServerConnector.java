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
package io.github.ilmich.floppyt.io.connectors;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import io.github.ilmich.floppyt.io.IOHandler;
import io.github.ilmich.floppyt.io.callback.CallbackManager;
import io.github.ilmich.floppyt.io.callback.JMXCallbackManager;
import io.github.ilmich.floppyt.io.timeout.JMXTimeoutManager;
import io.github.ilmich.floppyt.io.timeout.Timeout;
import io.github.ilmich.floppyt.util.Closeables;
import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.http.HttpServerDescriptor;

public class ServerConnector extends Thread {
	
	private static final String TAG = "ServerConnector";

	// private int port;
	private ServerSocketChannel serverChannel;
	private Selector selector;
	private final CallbackManager cm = new JMXCallbackManager();
	private final JMXTimeoutManager tm = new JMXTimeoutManager();
	private IOHandler ioHandler;
	private boolean isRunning = false;

	public ServerConnector() {
		super();
	}

	public ServerConnector(IOHandler ioHandler) {
		super();
		setIoHandler(ioHandler);
	}

	/**
	 * Setup connector
	 * 
	 */
	protected void startUp() throws Exception {
		if (this.serverChannel == null) {
			this.bind(8080);
		}
		selector = Selector.open();
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	@Override
	public void run() {
		if (this.serverChannel == null) {
			this.bind(8080);
		}

		long selectorTimeout = 250; // 250 m
		try {

			selector = Selector.open();
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);

			while (isRunning) {

				if (selector.select(selectorTimeout) == 0) {
					long ms = tm.execute(); // execute all timeouts
					// eventually reduce selector timeout in order to execute next
					// timeout
					selectorTimeout = Math.min(ms, /* selectorTimeout */250);
					if (cm.execute()) { // execute all callback registered
						selectorTimeout = 1;
					}
					continue;
				}

				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
				while (keys.hasNext()) {
					SelectionKey key = keys.next();
					keys.remove();
					try {
						if (key.isValid() && key.isAcceptable()) {
							ioHandler.handleAccept(key);
							continue;
						}
						if (key.isValid() && key.isConnectable()) {
							ioHandler.handleConnect(key);
							continue;
						}
						if (key.isValid() && key.isReadable()) {
							ioHandler.handleRead(key);
							continue;
						}
						if (key.isValid() && key.isWritable()) {
							ioHandler.handleWrite(key);
							continue;
						}
					} catch (CancelledKeyException ex) {
						Log.error(TAG, "CancelledKeyException received: " + ex.getMessage());
					} catch (IOException ex) {
						closeChannel((SocketChannel) key.channel());
					}
				}
				long ms = tm.execute();
				selectorTimeout = Math.min(ms, /* selectorTimeout */250);
				if (cm.execute()) {
					selectorTimeout = 1;
				}
			}
		} catch (IOException e) {
			this.isRunning = false;
			e.printStackTrace();
		}
	}

	public void bind(int port) {
		this.bind(new InetSocketAddress(port));
	}

	public void bind(InetSocketAddress endpoint) {
		try {
			serverChannel = ServerSocketChannel.open();
			boolean reuse = serverChannel.socket().getReuseAddress();
			if (!reuse) {
				Log.trace(TAG, "Enabling SO_REUSEADDR (was disabled)");
				serverChannel.socket().setReuseAddress(true);
			}
			serverChannel.configureBlocking(false);
		} catch (IOException e) {
			Log.error(TAG, "Error creating ServerSocketChannel: " + e.getMessage());
		}

		try {
			serverChannel.socket().bind(endpoint);
		} catch (IOException e) {
			Log.error(TAG, "Could not bind socket: " + e.getMessage());
		}
		Log.info(TAG, "Listen to " + endpoint.toString());
	}

	public void registerChannel(SocketChannel channel, int interestOps) throws IOException {
		channel.register(selector, interestOps);
		selector.wakeup();
	}

	public void registerChannel(SocketChannel channel, int interestOps, Object attachment) throws IOException {
		channel.register(selector, interestOps, attachment);
		selector.wakeup();
	}

	public void closeChannel(SocketChannel channel) {
		ioHandler.handleDisconnect(channel);
		Closeables.closeQuietly(channel);
	}

	public void addKeepAliveTimeout(SelectableChannel channel, Timeout keepAliveTimeout) {
		tm.addKeepAliveTimeout(channel, keepAliveTimeout);
	}

	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		return tm.hasKeepAliveTimeout(channel);
	}

	public void removeKeepAliveTimeout(SelectableChannel channel) {
		tm.removeKeepAliveTimeout(channel);
	}

	public void prolongKeepAliveTimeout(SelectableChannel channel) {
		addKeepAliveTimeout(channel, Timeout.newKeepAliveTimeout(channel, HttpServerDescriptor.KEEP_ALIVE_TIMEOUT, this));
	}

	public void closeOrRegisterForRead(SelectionKey key, boolean keepAlive) throws IOException {
		if (key.isValid() && keepAlive) {
			try {
				registerChannel((SocketChannel) key.channel(), SelectionKey.OP_READ);
				key.selector().wakeup();
				prolongKeepAliveTimeout(key.channel());
			} catch (IOException ex) {
				Log.error(TAG, "IOException while registrating key for read: " + ex.getMessage());
				throw ex;
			}
		} else {
			closeChannel((SocketChannel) key.channel());
		}
	}

	public IOHandler getIoHandler() {
		return ioHandler;
	}

	public void setIoHandler(IOHandler ioHandler) {
		this.ioHandler = ioHandler;
		this.ioHandler.attachServerConnector(this);
	}

	public void shutDown() {
		this.isRunning = false;
	}

	public void startAndWait() {
		this.start();
		this.isRunning = true;
		try {
			this.join();
		} catch (InterruptedException e) {
			this.isRunning = false;
		}
	}

}