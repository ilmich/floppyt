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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import io.github.ilmich.floppyt.web.http.HttpRequest;

public class UrlUtil {

	/**
	 * Example:
	 * 
	 * <pre>
	 * {@code 
	 * url: http://tt.se/                 Location: /start              =>  http://tt.se/start
	 * url: http://localhost/moved_perm   Location: /                   =>  http://localhost/
	 * url: http://github.com/            Location: http://github.com/  =>  https://github.com/
	 * }
	 * 
	 * (If the new url throws a MalformedURLException the url String representation
	 * will be returned.)
	 */
	public static String urlJoin(URL url, String locationHeader) {
		try {
			if (locationHeader.startsWith("http")) {
				return new URL(locationHeader).toString();
			}
			return new URL(url.getProtocol() + "://" + url.getAuthority() + locationHeader).toString();
		} catch (MalformedURLException e) {
			return url.toString();
		}
	}

	public static Map<String, String> parseUrlParams(String req) {
		// codice preso da deft per parsare parametri che sono all'interno di
		// richieste POST e PUT
		// provare a sviluppare questa implementazione all'interno di deft.. se
		// quello si sveglia:)
		Map<String, String> builder = new HashMap<String, String>();
		String[] paramArray;
		try {
			paramArray = HttpRequest.PARAM_STRING_PATTERN.split(URLDecoder.decode(req, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
		for (String keyValue : paramArray) {
			String[] keyValueArray = HttpRequest.KEY_VALUE_PATTERN.split(keyValue);
			// We need to check if the parameter has a value associated with it.
			if (keyValueArray.length > 1) {
				builder.put(keyValueArray[0], keyValueArray[1]); // name, value
			}
		}

		return builder;
	}

}
