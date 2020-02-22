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
package io.github.ilmich.floppyt.web.http.protocol;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of known content types, for convenience.
 */
public class MimeTypes {

	/** application/xml */
	public static final String APPLICATION_XML = "application/xml";

	/** application/json */
	public static final String APPLICATION_JSON = "application/json";

	/** application/x-www-form-urlencoded */
	public static final String APPLICATION_FORM_URLENCODED = "application/x-www-form-urlencoded";

	/** application/octet-stream */
	public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

	/** multipart/form-data */
	public static final String MULTIPART_FORM_DATA = "multipart/form-data";

	/** text/html */
	public static final String TEXT_HTML = "text/html";

	/** text/plain */
	public static final String TEXT_PLAIN = "text/plain";

	/** text/xml */
	public static final String TEXT_XML = "text/xml";
	
	private static final Map<String, String> mime = new HashMap<String, String>();
	
	static {
		mime.put(".html", TEXT_HTML);
		mime.put(".xml", TEXT_XML);
		mime.put(".json", APPLICATION_JSON);
	}
	
	public static String getContentType(File file) {
		String extension = file.getName().substring(file.getName().lastIndexOf('.'));
		return mime.containsKey(extension) ? mime.get(extension) : TEXT_PLAIN;
	}
}
