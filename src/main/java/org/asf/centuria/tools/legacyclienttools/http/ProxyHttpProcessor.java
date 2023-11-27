package org.asf.centuria.tools.legacyclienttools.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.asf.centuria.tools.legacyclienttools.translation.ApiTranslators;
import org.asf.connective.RemoteClient;
import org.asf.connective.processors.HttpPushProcessor;

public class ProxyHttpProcessor extends HttpPushProcessor {
	private String proxy;

	public ProxyHttpProcessor(String proxy) {
		this.proxy = proxy;
	}

	@Override
	public void process(String path, String method, RemoteClient client, String contentType) throws IOException {
		byte[] body = new byte[0];
		if (method.equals("POST")) {
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			try {
				getRequest().transferRequestBody(strm);
			} catch (IOException e) {
			}
			body = strm.toByteArray();
		}

		// Proxy request
		try {
			String rq = getRequest().getRawRequestResource();

			// Translate
			rq = ApiTranslators.translateApiRequestPath(rq);

			// Pull request
			HttpURLConnection conn = (HttpURLConnection) new URL(proxy + "/" + rq).openConnection();
			conn.setRequestMethod(method);
			for (String header : getRequest().getHeaderNames()) {
				for (String value : getRequest().getHeader(header).getValues())
					conn.addRequestProperty(header, value);
			}
			if (body.length != 0) {
				conn.setDoOutput(true);
				conn.getOutputStream().write(ApiTranslators.translateApiRequestBody(rq, body));
			}

			// Get response
			int res = conn.getResponseCode();
			if (res >= 200 && res < 400)
				getResponse().setContent(conn.getHeaderField("Content-Type"),
						ApiTranslators.translateApiResponseBody(path, conn.getInputStream()));
			else
				getResponse().setContent(conn.getHeaderField("Content-Type"),
						ApiTranslators.translateApiResponseBody(path, conn.getErrorStream()));
			conn.getHeaderFields().forEach((k, v) -> {
				if (k == null)
					return;
				v.forEach(t -> getResponse().addHeader(k, t, true));
			});
		} catch (IOException e) {
			setResponseStatus(503, "Service unavailable");
		}
	}

	@Override
	public HttpPushProcessor createNewInstance() {
		return new ProxyHttpProcessor(proxy);
	}

	@Override
	public String path() {
		return "/";
	}

	@Override
	public boolean supportsNonPush() {
		return true;
	}

	@Override
	public boolean supportsChildPaths() {
		return true;
	}

}
