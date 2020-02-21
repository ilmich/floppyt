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
package io.github.ilmich.floppyt.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DynamicByteBuffer {

	private ByteBuffer backend;

	private Charset mainCharset = Charset.forName("ASCII");

	private DynamicByteBuffer(ByteBuffer bb) {
		this.backend = bb;
	}

	/**
	 * Allocate a new {@code DynamicByteBuffer} that will be using a
	 * {@code ByteBuffer} internally.
	 * 
	 * @param capacity initial capacity
	 */
	public static DynamicByteBuffer allocate(int capacity) {
		return new DynamicByteBuffer(ByteBuffer.allocate(capacity));
	}

	/**
	 * Append the data. Will reallocate if needed.
	 */
	public void put(byte[] src) {
		ensureCapacity(src.length);
		backend.put(src);
	}

	/**
	 * Append the bytes from the given src. Will reallocate if needed.
	 */
	public void put(ByteBuffer src) {
		ensureCapacity(src.limit());
		backend.put(src);
	}

	/**
	 * Append count bytes in the given byte array start at array position
	 * 
	 * @param array    byte array to copy
	 * @param position start position
	 * @param count    bytes to copy count
	 */
	public void put(byte[] array, int position, int count) {
		ensureCapacity(count);
		backend.put(array, position, count);
	}

	/**
	 * Prepend the data. Will reallocate if needed.
	 */
	public void prepend(String data) {
		byte[] bytes = data.getBytes(mainCharset);
		int newSize = bytes.length + backend.position();
		byte[] newBuffer = new byte[newSize];
		System.arraycopy(bytes, 0, newBuffer, 0, bytes.length); // initial line and headers
		System.arraycopy(backend.array(), 0, newBuffer, bytes.length, backend.position()); // body
		backend = ByteBuffer.wrap(newBuffer);
		backend.position(newSize);
	}

	/**
	 * Ensures that its safe to append size data to backend.
	 * 
	 * @param size The size of the data that is about to be appended.
	 */
	private void ensureCapacity(int size) {
		int remaining = backend.remaining();
		if (size > remaining) {
			int missing = size - remaining;
			int newSize = (int) ((backend.capacity() + missing) * 1.5);
			reallocate(newSize);
		}
	}

	// Preserves position.
	private void reallocate(int newCapacity) {
		int oldPosition = backend.position();
		byte[] newBuffer = new byte[newCapacity];
		System.arraycopy(backend.array(), 0, newBuffer, 0, backend.position());
		backend = ByteBuffer.wrap(newBuffer);
		backend.position(oldPosition);
	}

	/**
	 * Returns the {@code ByteBuffer} that is used internally by this
	 * {@code DynamicByteBufer}. Changes made to the returned {@code ByteBuffer}
	 * will be incur modifications in this {@code DynamicByteBufer}.
	 */
	public ByteBuffer getByteBuffer() {
		return backend;
	}

	/**
	 * See {@link ByteBuffer#get(byte[], int, int)}
	 */
	public void get(byte[] dst, int offset, int length) {
		backend.get(dst, offset, length);
	}

	public void position(int newPosition) {
		backend.position(newPosition);
	}

	/**
	 * See {@link ByteBuffer#flip}
	 */
	public void flip() {
		backend.flip();
	}

	/**
	 * See {@link ByteBuffer#limit}
	 */
	public int limit() {
		return backend.limit();
	}

	/**
	 * See {@link ByteBuffer#position}
	 */
	public int position() {
		return backend.position();
	}

	/**
	 * See {@link ByteBuffer#array}
	 */
	public byte[] array() {
		return backend.array();
	}

	/**
	 * See {@link ByteBuffer#capacity}
	 */
	public int capacity() {
		return backend.capacity();
	}

	/**
	 * See {@link ByteBuffer#hasRemaining}
	 */
	public boolean hasRemaining() {
		return backend.hasRemaining();
	}

	/**
	 * See {@link ByteBuffer#compact}
	 */
	public DynamicByteBuffer compact() {
		backend.compact();
		return this;
	}

	/**
	 * See {@link ByteBuffer#clear}
	 */
	public DynamicByteBuffer clear() {
		backend.clear();
		return this;
	}
}
