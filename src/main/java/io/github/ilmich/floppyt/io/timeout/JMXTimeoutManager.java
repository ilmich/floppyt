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
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.AsyncCallback;

public class JMXTimeoutManager {

	private static final String TAG = "JMXTimeoutManager";
	private final TreeSet<Timeout> timeouts = new TreeSet<Timeout>(new TimeoutComparator());
	private final TreeSet<DecoratedTimeout> keepAliveTimeouts = new TreeSet<JMXTimeoutManager.DecoratedTimeout>();
	private final Map<SelectableChannel, DecoratedTimeout> index = new ConcurrentHashMap<SelectableChannel, JMXTimeoutManager.DecoratedTimeout>();

	public void addKeepAliveTimeout(SelectableChannel channel, Timeout timeout) {
		DecoratedTimeout decorated = index.get(channel);
		if (decorated == null) {
			decorated = new DecoratedTimeout(channel, timeout);
			index.put(channel, decorated);
		}
		keepAliveTimeouts.remove(decorated);
		decorated.setTimeout(timeout);
		keepAliveTimeouts.add(decorated);
	}

	public void removeKeepAliveTimeout(SelectableChannel channel) {
		DecoratedTimeout sc = index.get(channel);
		if (sc == null)
			return;

		keepAliveTimeouts.remove(index.get(channel));
		index.remove(channel);
	}

	public void addTimeout(Timeout timeout) {
		timeouts.add(timeout);
	}

	public boolean hasKeepAliveTimeout(SelectableChannel channel) {
		return index.containsKey(channel);
	}

	public long execute() {
		return Math.min(executeKeepAliveTimeouts(), executeTimeouts());
	}

	private long executeKeepAliveTimeouts() {
		// makes a defensive copy to avoid (1) CME (new timeouts are added this
		// iteration) and (2) IO starvation.
		long now = System.currentTimeMillis();
		SortedSet<DecoratedTimeout> defensive = new TreeSet<JMXTimeoutManager.DecoratedTimeout>(keepAliveTimeouts)
				.headSet(new DecoratedTimeout(null, new Timeout(now, AsyncCallback.nopCb)));

		keepAliveTimeouts.removeAll(defensive);
		for (DecoratedTimeout decoratedTimeout : defensive) {
			decoratedTimeout.timeout.getCallback().onCallback();
			index.remove(decoratedTimeout.channel);
			Log.trace(TAG, "Keepalive Timeout triggered: ");
		}

		return keepAliveTimeouts.isEmpty() ? Long.MAX_VALUE
				: Math.max(1, keepAliveTimeouts.iterator().next().timeout.getTimeout() - now);
	}

	private long executeTimeouts() {
		// makes a defensive copy to avoid (1) CME (new timeouts are added this
		// iteration) and (2) IO starvation.
		TreeSet<Timeout> defensive = new TreeSet<Timeout>(timeouts);
		Iterator<Timeout> iter = defensive.iterator();
		final long now = System.currentTimeMillis();
		while (iter.hasNext()) {
			Timeout candidate = iter.next();
			if (candidate.getTimeout() > now) {
				break;
			}
			candidate.getCallback().onCallback();
			iter.remove();
			timeouts.remove(candidate);
			Log.trace(TAG, "Timeout triggered: ");
		}
		return timeouts.isEmpty() ? Long.MAX_VALUE : Math.max(1, timeouts.iterator().next().getTimeout() - now);
	}


	public int getNumberOfKeepAliveTimeouts() {
		return index.size();
	}

	public int getNumberOfTimeouts() {
		return keepAliveTimeouts.size() + timeouts.size();
	}

	private class DecoratedTimeout implements Comparable<DecoratedTimeout> {

		public final SelectableChannel channel;
		public Timeout timeout;

		public DecoratedTimeout(SelectableChannel channel, Timeout timeout) {
			this.channel = channel;
			this.timeout = timeout;
		}

		@Override
		public int compareTo(DecoratedTimeout that) {
			long diff = timeout.getTimeout() - that.timeout.getTimeout();
			if (diff < 0) {
				return -1;
			} else if (diff > 0) {
				return 1;
			}
			if (channel != null && that.channel != null) {
				return channel.hashCode() - that.channel.hashCode();
			} else if (channel == null && that.channel != null) {
				return -1;
			} else if (channel != null && that.channel == null) {
				return -1;
			} else {
				return 0;
			}
		}

		public void setTimeout(Timeout timeout) {
			this.timeout = timeout;
		}

	}

	private class TimeoutComparator implements Comparator<Timeout> {

		@Override
		public int compare(Timeout lhs, Timeout rhs) {
			if (lhs == rhs) {
				return 0;
			}
			long diff = lhs.getTimeout() - rhs.getTimeout();
			if (diff <= 0) {
				return -1;
			}
			return 1; // / else if (diff > 0) {
		}
	}

}
