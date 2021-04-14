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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import io.github.ilmich.floppyt.io.connectors.ServerConnector;
import io.github.ilmich.floppyt.util.ExceptionUtils;
import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.http.HttpServerDescriptor;
import io.github.ilmich.floppyt.web.http.Request;
import io.github.ilmich.floppyt.web.http.Response;

public class SSLIOHandler implements IOHandler {

	private static final String TAG = "SSLIOHandler";

	public static ThreadPoolExecutor executor = new ThreadPoolExecutor(HttpServerDescriptor.MIN_THREADS_PROCESSOR,
			HttpServerDescriptor.MAX_THREADS_PROCESSOR, HttpServerDescriptor.KEEP_ALIVE_TIMEOUT, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>());

	private ServerConnector connector = null;

	private Protocol protocol = null;

	// protected ByteBuffer myAppData;
	protected ByteBuffer myNetData;
	protected ByteBuffer peerAppData;
	protected ByteBuffer peerNetData;
	private SSLContext context;

	public SSLIOHandler(Protocol protocol) {
		super();
		this.protocol = protocol;
		try {
			context = SSLContext.getInstance("TLS");
			context.init(createKeyManagers("/home/ilmich/eclipse-workspace/quick-start/server.jks", "keypass", "keypass"),
					createTrustManagers("/home/ilmich/eclipse-workspace/quick-start/trustedCerts.jks", "storepass"), new SecureRandom());
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//
		catch (KeyManagementException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		SSLSession dummySession = context.createSSLEngine().getSession();
		// myAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
		myNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
		peerAppData = ByteBuffer.allocate(dummySession.getApplicationBufferSize());
		peerNetData = ByteBuffer.allocate(dummySession.getPacketBufferSize());
		dummySession.invalidate();
	}

	public void setExecutor(ThreadPoolExecutor exec) {
		executor = exec;
	}

	@Override
	public void handleAccept(SelectionKey key) throws IOException {
		Log.debug(TAG, "Accepting connection");
		try {
			SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
			if (clientChannel.isOpen()) {
				clientChannel.configureBlocking(false);

				SSLEngine engine = context.createSSLEngine();
				engine.setUseClientMode(false);
				engine.beginHandshake();

				if (doHandshake(clientChannel, engine)) {
					connector.registerChannel(clientChannel, SelectionKey.OP_READ, engine);
				} else {
					connector.closeChannel(clientChannel);
					Log.debug("Connection closed due to handshake failure.");
				}
			}
			// set keepalive timeout for all incoming connections
			// (not only valid http requests)
			// I've noticed that chrome (and maybe others) try to
			// establish almost two persistent connections
			// with scheduled empty packet every 1 minute
			// (maybe for faster http interactions)

			// commented out for now
			// connector.prolongKeepAliveTimeout(clientChannel);
		} catch (IOException ex) {
			key.cancel();
			Log.error(TAG, "Error accepting connection: " + ex.getMessage());
		}
	}

	@Override
	public void handleConnect(SelectionKey key) throws IOException {

	}

	@Override
	public void handleRead(SelectionKey key) throws IOException {
		final SocketChannel client = (SocketChannel) key.channel();

		try {
			peerNetData.clear();
			if (IOSocketHelper.readBuffer(peerNetData, client) < 0) {
				// ignore empty requests
				key.cancel();
				return;
			}

			if (connector.hasKeepAliveTimeout(client)) { // prolong keep-alive timeout
				connector.prolongKeepAliveTimeout(client);
			}

			SSLEngine engine = (SSLEngine) key.attachment();
			if (engine == null) {
				return;
				
			}
			while (peerNetData.hasRemaining()) {
				peerAppData.clear();
				SSLEngineResult result = engine.unwrap(peerNetData, peerAppData);
				switch (result.getStatus()) {
				case OK:
					peerAppData.flip();
					final Request req = protocol.onRead(peerAppData, client);
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
										t.setSSLEngine(engine);
										connector.registerChannel(client, SelectionKey.OP_WRITE, t);
									} catch (IOException ex) {
										Log.error(TAG,
												"Error when processing request: " + ExceptionUtils.getStackTrace(ex));
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
					break;
				case BUFFER_OVERFLOW:
					peerAppData = enlargeApplicationBuffer(engine, peerAppData);
					break;
				case BUFFER_UNDERFLOW:
					peerNetData = handleBufferUnderflow(engine, peerNetData);
					break;
				case CLOSED:
					Log.debug("Client wants to close connection...");
					connector.closeChannel(client);
					Log.debug("Goodbye client!");
					return;
				default:
					throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
				}
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

		SocketChannel client = (SocketChannel) key.channel();
		try {
			if (key.attachment() instanceof Response) {

				Response response = (Response) key.attachment();
				ByteBuffer writeBuffer = (ByteBuffer) response.getResponseData().getByteBuffer();

				while (writeBuffer.hasRemaining()) {
					// The loop has a meaning for (outgoing) messages larger than 16KB.
					// Every wrap call will remove 16KB from the original message and send it to the
					// remote peer.
					myNetData.clear();
					SSLEngineResult result = response.getSSLEngine().wrap(writeBuffer, myNetData);
					switch (result.getStatus()) {
					case OK:
						myNetData.flip();
						while (myNetData.hasRemaining()) {
							client.write(myNetData);
						}
						break;
					case BUFFER_OVERFLOW:
						myNetData = enlargePacketBuffer(response.getSSLEngine(), myNetData);
						break;
					case BUFFER_UNDERFLOW:
						throw new SSLException(
								"Buffer underflow occured after a wrap. I don't think we should ever get here.");
					case CLOSED:
						connector.closeChannel(client);
						return;
					default:
						throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
					}
				}

				this.finishRequest(key);
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
			key.attach(response.getSSLEngine());
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

	}

	protected boolean doHandshake(SocketChannel socketChannel, SSLEngine engine) throws IOException {

		Log.debug("About to do handshake...");

		SSLEngineResult result;
		HandshakeStatus handshakeStatus;

		// NioSslPeer's fields myAppData and peerAppData are supposed to be large enough
		// to hold all message data the peer
		// will send and expects to receive from the other peer respectively. Since the
		// messages to be exchanged will usually be less
		// than 16KB long the capacity of these fields should also be smaller. Here we
		// initialize these two local buffers
		// to be used for the handshake, while keeping client's buffers at the same
		// size.
		int appBufferSize = engine.getSession().getApplicationBufferSize();
		ByteBuffer myAppData = ByteBuffer.allocate(appBufferSize);
		ByteBuffer peerAppData = ByteBuffer.allocate(appBufferSize);
		myNetData.clear();
		peerNetData.clear();

		handshakeStatus = engine.getHandshakeStatus();
		while (handshakeStatus != SSLEngineResult.HandshakeStatus.FINISHED
				&& handshakeStatus != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING) {
			switch (handshakeStatus) {
			case NEED_UNWRAP:
				if (socketChannel.read(peerNetData) < 0) {
					if (engine.isInboundDone() && engine.isOutboundDone()) {
						return false;
					}
					try {
						engine.closeInbound();
					} catch (SSLException e) {
						Log.error(
								"This engine was forced to close inbound, without having received the proper SSL/TLS close notification message from the peer, due to end of stream.");
					}
					engine.closeOutbound();
					// After closeOutbound the engine will be set to WRAP state, in order to try to
					// send a close message to the client.
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				peerNetData.flip();
				try {
					result = engine.unwrap(peerNetData, peerAppData);
					peerNetData.compact();
					handshakeStatus = result.getHandshakeStatus();
				} catch (SSLException sslException) {
					Log.error("A problem was encountered while processing the data that caused the SSLEngine to abort."
							+ sslException.getMessage());
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				switch (result.getStatus()) {
				case OK:
					break;
				case BUFFER_OVERFLOW:
					// Will occur when peerAppData's capacity is smaller than the data derived from
					// peerNetData's unwrap.
					peerAppData = enlargeApplicationBuffer(engine, peerAppData);
					break;
				case BUFFER_UNDERFLOW:
					// Will occur either when no data was read from the peer or when the peerNetData
					// buffer was too small to hold all peer's data.
					peerNetData = handleBufferUnderflow(engine, peerNetData);
					break;
				case CLOSED:
					if (engine.isOutboundDone()) {
						return false;
					} else {
						engine.closeOutbound();
						handshakeStatus = engine.getHandshakeStatus();
						connector.closeChannel(socketChannel);
						break;
					}
				default:
					throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
				}
				break;
			case NEED_WRAP:
				myNetData.clear();
				try {
					result = engine.wrap(myAppData, myNetData);
					handshakeStatus = result.getHandshakeStatus();
				} catch (SSLException sslException) {
					Log.error("A problem was encountered while processing the data that caused the SSLEngine to abort."
							+ sslException.getMessage());
					engine.closeOutbound();
					handshakeStatus = engine.getHandshakeStatus();
					break;
				}
				switch (result.getStatus()) {
				case OK:
					myNetData.flip();
					while (myNetData.hasRemaining()) {
						socketChannel.write(myNetData);
					}
					break;
				case BUFFER_OVERFLOW:
					// Will occur if there is not enough space in myNetData buffer to write all the
					// data that would be generated by the method wrap.
					// Since myNetData is set to session's packet size we should not get to this
					// point because SSLEngine is supposed
					// to produce messages smaller or equal to that, but a general handling would be
					// the following:
					myNetData = enlargePacketBuffer(engine, myNetData);
					break;
				case BUFFER_UNDERFLOW:
					throw new SSLException(
							"Buffer underflow occured after a wrap. I don't think we should ever get here.");
				case CLOSED:
					try {
						myNetData.flip();
						while (myNetData.hasRemaining()) {
							socketChannel.write(myNetData);
						}
						// At this point the handshake status will probably be NEED_UNWRAP so we make
						// sure that peerNetData is clear to read.
						peerNetData.clear();
					} catch (Exception e) {
						Log.error("Failed to send server's CLOSE message due to socket channel's failure.");
						handshakeStatus = engine.getHandshakeStatus();
					}
					break;
				default:
					throw new IllegalStateException("Invalid SSL status: " + result.getStatus());
				}
				break;
			case NEED_TASK:
				Runnable task;
				while ((task = engine.getDelegatedTask()) != null) {
					executor.execute(task);
				}
				handshakeStatus = engine.getHandshakeStatus();
				break;
			case FINISHED:
				break;
			case NOT_HANDSHAKING:
				break;
			default:
				throw new IllegalStateException("Invalid SSL status: " + handshakeStatus);
			}
		}

		return true;

	}

	protected ByteBuffer enlargePacketBuffer(SSLEngine engine, ByteBuffer buffer) {
		return enlargeBuffer(buffer, engine.getSession().getPacketBufferSize());
	}

	protected ByteBuffer enlargeApplicationBuffer(SSLEngine engine, ByteBuffer buffer) {
		return enlargeBuffer(buffer, engine.getSession().getApplicationBufferSize());
	}

	protected ByteBuffer enlargeBuffer(ByteBuffer buffer, int sessionProposedCapacity) {
		if (sessionProposedCapacity > buffer.capacity()) {
			buffer = ByteBuffer.allocate(sessionProposedCapacity);
		} else {
			buffer = ByteBuffer.allocate(buffer.capacity() * 2);
		}
		return buffer;
	}

	protected ByteBuffer handleBufferUnderflow(SSLEngine engine, ByteBuffer buffer) {
		if (engine.getSession().getPacketBufferSize() < buffer.limit()) {
			return buffer;
		} else {
			ByteBuffer replaceBuffer = enlargePacketBuffer(engine, buffer);
			buffer.flip();
			replaceBuffer.put(buffer);
			return replaceBuffer;
		}
	}

	protected javax.net.ssl.KeyManager[] createKeyManagers(String filepath, String keystorePassword, String keyPassword)
			throws Exception {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		InputStream keyStoreIS = new FileInputStream(filepath);
		try {
			keyStore.load(keyStoreIS, keystorePassword.toCharArray());
		} finally {
			if (keyStoreIS != null) {
				keyStoreIS.close();
			}
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(keyStore, keyPassword.toCharArray());
		return kmf.getKeyManagers();
	}

	/**
	 * Creates the trust managers required to initiate the {@link SSLContext}, using
	 * a JKS keystore as an input.
	 *
	 * @param filepath         - the path to the JKS keystore.
	 * @param keystorePassword - the keystore's password.
	 * @return {@link TrustManager} array, that will be used to initiate the
	 *         {@link SSLContext}.
	 * @throws Exception
	 */
	protected TrustManager[] createTrustManagers(String filepath, String keystorePassword) throws Exception {
		KeyStore trustStore = KeyStore.getInstance("JKS");
		InputStream trustStoreIS = new FileInputStream(filepath);
		try {
			trustStore.load(trustStoreIS, keystorePassword.toCharArray());
		} finally {
			if (trustStoreIS != null) {
				trustStoreIS.close();
			}
		}
		TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustFactory.init(trustStore);
		return trustFactory.getTrustManagers();
	}
}
