# floppyt
minimal java web toolkit that stay in a floppy

# sample code
```

import io.github.ilmich.floppyt.util.Log;
import io.github.ilmich.floppyt.web.handler.PrometheusHandler;
import io.github.ilmich.floppyt.web.http.HttpRequest;
import io.github.ilmich.floppyt.web.http.HttpRequestHandler;
import io.github.ilmich.floppyt.web.http.HttpResponse;
import io.github.ilmich.floppyt.web.http.HttpServer;
import io.github.ilmich.floppyt.web.http.HttpServerBuilder;
import io.github.ilmich.floppyt.web.http.protocol.MimeTypes;

public class prova {

    public static void main(String[] args) {
	HttpServerBuilder hsb = new HttpServerBuilder();
	Log.DEBUG();
	HttpServer hs = hsb.
		bindPlain(8080) // bind port
		.addRoute("/", new HttpRequestHandler() { // sample handler
			@Override
			public void get(HttpRequest request, HttpResponse response) {
			    response.setContentType(MimeTypes.APPLICATION_JSON);
			    response.write("Ciao sono io");
			}})
		.addRoute("/metrics", new PrometheusHandler()) // metrics
		.build(); // build
	
	//start
	hs.startAndWait();

    }

}

```
