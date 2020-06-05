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
package io.github.ilmich.floppyt.web.handler;

import java.util.ArrayList;
import java.util.List;

import io.github.ilmich.floppyt.io.PlainIOHandler;
import io.github.ilmich.floppyt.io.connectors.ServerConnector;
import io.github.ilmich.floppyt.metrics.Metric;
import io.github.ilmich.floppyt.metrics.Metrics;
import io.github.ilmich.floppyt.web.http.HttpServerRequest;
import io.github.ilmich.floppyt.web.http.HttpRequestHandler;
import io.github.ilmich.floppyt.web.http.HttpServerResponse;

public class PrometheusHandler extends HttpRequestHandler {

	@Override
	public void get(HttpServerRequest request, HttpServerResponse response) {
		
		long stamp = System.currentTimeMillis();
		response.write("# TYPE threads_active_count gauge\n");
		response.write("threads_active_count "+ PlainIOHandler.executor.getActiveCount() + " " +stamp+ "\n");
		
		response.write("# TYPE threads_max_active_count gauge\n");
		response.write("threads_max_active_count "+ PlainIOHandler.executor.getLargestPoolSize() + " " +stamp+ "\n");
		
		response.write("# TYPE http_keepalive_conn gauge\n");
		response.write("http_keepalive_conn "+ ServerConnector.tm.getNumberOfKeepAliveTimeouts() + " " +stamp+ "\n");
		
		for (String name : Metrics.gauges.keySet()) {
			response.write("# TYPE "+ name + " gauge\n");
			for (Metric c : Metrics.gauges.get(name)) {
				response.write(serializeMetric(name, c) + " " +stamp);
				response.write("\n");
			}
		}
		for (String name : Metrics.counters.keySet()) {
			response.write("# TYPE "+ name + " counter\n");
			for (Metric c : Metrics.counters.get(name)) {
				response.write(serializeMetric(name, c)+ " " +stamp);
				response.write("\n");
			}
		}
		response.write(" ");
	}
	
	private String serializeMetric(String name, Metric c) {
		StringBuffer sb = new StringBuffer();
		sb.append(name);
		if (c.labels != null && !c.labels.isEmpty()) {
			sb.append("{");
			List<String> ser = new ArrayList<String>();
			for (String label : c.labels.keySet()) {
				ser.add(label + "=\"" + c.labels.get(label));
			}			
			sb.append(String.join(",", ser) + "}");
		}
		return sb.append(" " + c.value()).toString();
	}

}
