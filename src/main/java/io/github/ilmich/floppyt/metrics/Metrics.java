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
package io.github.ilmich.floppyt.metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Metrics {
	
	public static final Map<String, List<Counter>> counters = new ConcurrentHashMap<String, List<Counter>>();
	public static final Map<String, List<Gauge>> gauges = new ConcurrentHashMap<String, List<Gauge>>();
	
	public static Counter getCounter(String name) {
		return getCounter(name, null);
	}
	
	public static Counter getCounter(String name, Map<String, String> labels) {
		String intMetric = name;
		List<Counter> cc = new ArrayList<Counter>();
		if (!counters.containsKey(intMetric)) {			
			Counter c = new Counter(labels);
			cc.add(c);
			counters.put(intMetric, cc);
			return c;
		}
		
		cc = counters.get(intMetric);
		for (Counter counter : cc) {
			if ((labels == null && counter.labels == null) 
					|| counter.labels.equals(labels)) {
				return counter;
			} 
		}
		
		Counter c = new Counter(labels);
		cc.add(c);
		return c;		
	}
	
	public static Gauge getGauge(String name) {
		return getGauge(name, null);
	}
	
	public static Gauge getGauge(String name, Map<String, String> labels) {
		String intMetric = name;
		List<Gauge> cc = new ArrayList<Gauge>();
		if (!gauges.containsKey(intMetric)) {			
			Gauge c = new Gauge(labels);
			cc.add(c);
			gauges.put(intMetric, cc);
			return c;
		}
		
		cc = gauges.get(intMetric);		
		for (Gauge counter : cc) {			
			if ((labels == null && counter.labels == null) 
					|| counter.labels.equals(labels)) {
				return counter;
			} 
		}
		
		Gauge c = new Gauge(labels);
		cc.add(c);
		return c;
	}

}
