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
package io.github.ilmich.floppyt.io.callback;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import io.github.ilmich.floppyt.web.AsyncCallback;

public class JMXCallbackManager {

	private final AbstractCollection<AsyncCallback> callbacks = new ConcurrentLinkedQueue<AsyncCallback>();

	public int getNumberOfCallbacks() {
		return callbacks.size();
	}

	public void addCallback(AsyncCallback callback) {
		callbacks.add(callback);
	}

	public boolean execute() {
		// makes a defensive copy to avoid (1) CME (new callbacks are added this
		// iteration) and (2) IO starvation.
		List<AsyncCallback> defensive = new ArrayList<AsyncCallback>(callbacks);
		callbacks.clear();
		for (AsyncCallback callback : defensive) {
			callback.onCallback();
		}
		return !callbacks.isEmpty();
	}
}
