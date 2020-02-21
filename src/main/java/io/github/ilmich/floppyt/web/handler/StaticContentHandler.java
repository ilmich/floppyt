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

import java.io.File;
import java.io.IOException;

import javax.activation.MimetypesFileTypeMap;

import io.github.ilmich.floppyt.util.DateUtil;
import io.github.ilmich.floppyt.web.http.HttpException;
import io.github.ilmich.floppyt.web.http.HttpRequest;
import io.github.ilmich.floppyt.web.http.HttpRequestHandler;
import io.github.ilmich.floppyt.web.http.HttpResponse;
import io.github.ilmich.floppyt.web.http.Request;
import io.github.ilmich.floppyt.web.http.Response;
import io.github.ilmich.floppyt.web.http.protocol.HttpStatus;

/**
 * A RequestHandler that serves static content (files) from a predefined
 * directory.
 * 
 * "Cache-Control: public" indicates that the response MAY be cached by any
 * cache, even if it would normally be non-cacheable or cacheable only within a
 * non- shared cache.
 */

public class StaticContentHandler extends HttpRequestHandler {

	private final static StaticContentHandler instance = new StaticContentHandler();

	private MimetypesFileTypeMap mimeTypeMap;

	public static StaticContentHandler getInstance() {
		return instance;
	}

	private StaticContentHandler() {
		try {
			mimeTypeMap = new MimetypesFileTypeMap("META-INF/mime.types");
		} catch (IOException e) {
			mimeTypeMap = new MimetypesFileTypeMap();
		}
	}

	/** {inheritDoc} */
	@Override
	public void get(HttpRequest request, HttpResponse response) {
		perform(request, response, true);
	}

	/** {inheritDoc} */
	@Override
	public void head(final HttpRequest request, final HttpResponse response) {
		perform(request, response, false);
	}

	/**
	 * @param request  the <code>HttpRequest</code>
	 * @param response the <code>HttpResponse</code>
	 * @param hasBody  <code>true</code> to write the message body;
	 *                 <code>false</code> otherwise.
	 */
	private void perform(final Request request, final Response response, boolean hasBody) {

		final String path = request.getRequestedPath();
		final File file = new File(path.substring(1)); // remove the leading '/'

		if (!file.exists()) {
			throw new HttpException(HttpStatus.CLIENT_ERROR_NOT_FOUND, "File not found");
		} else if (!file.isFile()) {
			throw new HttpException(HttpStatus.CLIENT_ERROR_FORBIDDEN, path + "is not a file");
		}

		final long lastModified = file.lastModified();
		response.setHeader("Last-Modified", DateUtil.parseToRFC1123(lastModified));
		response.setHeader("Cache-Control", "public");
		String mimeType = mimeTypeMap.getContentType(file);
		if ("text/plain".equals(mimeType)) {
			mimeType += "; charset=utf-8";
		}
		response.setHeader("Content-Type", mimeType);
		final String ifModifiedSince = request.getHeader("If-Modified-Since");
		if (ifModifiedSince != null) {
			final long ims = DateUtil.parseToMilliseconds(ifModifiedSince);
			if (lastModified <= ims) {
				response.setStatus(HttpStatus.REDIRECTION_NOT_MODIFIED);
				return;
			}
		}

		if (hasBody) {
			response.write(file);
		}
	}
}
