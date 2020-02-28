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
package io.github.ilmich.floppyt.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import io.github.ilmich.floppyt.io.connectors.ServerConnector;
import io.github.ilmich.floppyt.metrics.Gauge;
import io.github.ilmich.floppyt.metrics.Metrics;
import io.github.ilmich.floppyt.util.ExceptionUtils;
import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.http.HttpServerDescriptor;
import io.github.ilmich.floppyt.web.http.Request;
import io.github.ilmich.floppyt.web.http.Response;

public class PlainIOHandler implements IOHandler {
	
	private static final String TAG = "PlainIOHandler";

	private ExecutorService executor = new ThreadPoolExecutor(HttpServerDescriptor.MIN_THREADS_PROCESSOR,
			HttpServerDescriptor.MAX_THREADS_PROCESSOR, HttpServerDescriptor.KEEP_ALIVE_TIMEOUT, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>());
	
	private ServerConnector connector = null;

	private Protocol protocol = null;

	public PlainIOHandler(Protocol protocol) {
		super();
		this.protocol = protocol;
	}

	public void setExecutor(ExecutorService executor) {	
		this.executor = executor;
	}

	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		try {
			SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
			if (clientChannel.isOpen()) {
				clientChannel.configureBlocking(false);
				Gauge c = Metrics.getGauge("http_connections");
				c.increment();
				
				// register channel for reading
				connector.registerChannel(clientChannel, SelectionKey.OP_READ);
			}
			//set keepalive timeout for all incoming connections 
			//(not only valid http requests)
			//I've noticed that chrome (and maybe others) try to
			//establish almost two persistent connections
			//with scheduled empty packet every 1 minute
			//(maybe for faster http interactions) 
			
			//commented out for now 
			//connector.prolongKeepAliveTimeout(clientChannel);
		} catch (IOException ex) {
			Log.error(TAG, "Error accepting connection: " + ex.getMessage());
		}
	}

	@Override
	public void handleConnect(SelectionKey key) throws IOException {
		
	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		final SocketChannel client = (SocketChannel) key.channel();
		final ByteBuffer readBuffer = ByteBuffer.allocate(HttpServerDescriptor.READ_BUFFER_SIZE);
		try {
			if (IOSocketHelper.readBuffer(readBuffer, client) < 0) {
				//ignore empty requests
				key.cancel();
				return;
			}

			if (connector.hasKeepAliveTimeout(client)) { // prolong keep-alive timeout
				connector.prolongKeepAliveTimeout(client);
			}

			final Request req = protocol.onRead(readBuffer, client);
			if (req != null) { // response completed
				final CompletableFuture<Response> future = new CompletableFuture<Response>();

				executor.submit(new Runnable() {

					@Override
					public void run() {
						try {
							future.complete(protocol.processRequest(req));
						} catch (Throwable ex) {
							future.completeExceptionally(ex);
						}
					}
				});

				future.whenCompleteAsync(new BiConsumer<Response, Throwable>() {

					@Override
					public void accept(Response t, Throwable u) {
						if (t != null) {
							try {								
								connector.registerChannel(client, SelectionKey.OP_WRITE, t);
							} catch (IOException ex) {
								Log.error(TAG, "Error when processing request: " + ExceptionUtils.getStackTrace(ex));
								Log.error(TAG, req.toString());
								connector.removeKeepAliveTimeout(client);
								connector.closeChannel(client);
							}
						} else {
							Log.error(TAG, "Error when processing request: " + ExceptionUtils.getStackTrace(u));
							Log.error(TAG, req.toString());
							connector.removeKeepAliveTimeout(client);
							connector.closeChannel(client);
						}

					}

				});
			}

		} catch (ClosedChannelException ex) {
			Log.trace(TAG, "ClosedChannelException when reading: client disconnect");
			connector.removeKeepAliveTimeout(client);
			throw ex;
		} catch (IOException ex) {
			Log.error(TAG, "IOException when reading: " + ex.getMessage());
			connector.removeKeepAliveTimeout(client);
			throw ex;
		}
	}

	@Override
	public void handleWrite(SelectionKey key) throws IOException {
		if (key.attachment() == null)
			return;

		boolean finished = false;
		SocketChannel client = (SocketChannel) key.channel();
		try {
			if (key.attachment() instanceof Response) {
				Response response = (Response) key.attachment();
				ByteBuffer writeBuffer = (ByteBuffer) response.getResponseData().getByteBuffer();

				IOSocketHelper.writeBuffer(writeBuffer, client);
				if (!writeBuffer.hasRemaining()) {
					if (!(finished = response.getFile() == null)) {
						FileChannel channel = (FileChannel) response.getFile();
						long bytesWritten = channel.transferTo(channel.position(), channel.size(), client);
						if (!(finished = bytesWritten < channel.size())) {
							channel.position(channel.position() + bytesWritten);
						} else {
							channel.close();
						}
					}
				}
				if (finished) {
					// connector.closeOrRegisterForRead(key, response.isKeepAlive());
					this.finishRequest(key);
				}
			}

		} catch (IOException ex) {
			Log.error(TAG, "Error writing on channel: " + ex.getMessage());
			connector.removeKeepAliveTimeout(client);
			throw ex;
		}
	}

	public void finishRequest(SelectionKey key) throws IOException {
		if (key.attachment() != null && key.attachment() instanceof Response) {
			Response response = (Response) key.attachment();
			connector.closeOrRegisterForRead(key, response.isKeepAlive());
		}
	}

	public ServerConnector getConnector() {
		return connector;
	}

	public void setConnector(ServerConnector connector) {
		this.connector = connector;
	}

	public Protocol getProtocol() {
		return protocol;
	}

	public void setProtocol(Protocol protocol) {
		this.protocol = protocol;
	}

	@Override
	public void attachServerConnector(ServerConnector conn) {
		this.connector = conn;
	}

	@Override
	public void handleDisconnect(SocketChannel key) {		
		Gauge c = Metrics.getGauge("http_connections");
		c.decrement();
	}
}
