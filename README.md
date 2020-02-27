# floppyt
minimal java web toolkit that stay in a floppy

# sample code
```


import java.net.InetSocketAddress;

import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.handler.PrometheusHandler;
import io.github.ilmich.floppyt.web.http.HttpRequest;
import io.github.ilmich.floppyt.web.http.HttpRequestHandler;
import io.github.ilmich.floppyt.web.http.HttpResponse;
import io.github.ilmich.floppyt.web.http.HttpServer;
import io.github.ilmich.floppyt.web.http.protocol.MimeTypes;

public class SampleServer {

	public static void main(String[] args) {

		HttpServer hs = new HttpServer();
		Log.DEBUG();

		hs.listen(new InetSocketAddress("127.0.0.1", 8080))
		.route("/", new HttpRequestHandler() { // sample handler
			@Override
			public void handle(HttpRequest request, HttpResponse response) {
				response.setContentType(MimeTypes.APPLICATION_JSON);
				response.write("Hello World!");
			}
		})
		.route("/metrics", new PrometheusHandler()) // metrics
		.route("/user/{id}", new HttpRequestHandler() {
			@Override
			public void get(HttpRequest request, HttpResponse response) {
				response.write(request.getPathParam("id"));				
			}
		}).startAndWait(); 
		
	}

}


```
