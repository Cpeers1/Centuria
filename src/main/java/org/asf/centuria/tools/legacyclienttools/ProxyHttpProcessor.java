package org.asf.centuria.tools.legacyclienttools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.UUID;

import org.asf.rats.processors.HttpUploadProcessor;

public class ProxyHttpProcessor extends HttpUploadProcessor {
        private String proxy;
        public ProxyHttpProcessor(String proxy) {
            this.proxy = proxy;
        }

        @Override
        public void process(String contentType, Socket client, String method) {
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
            	String rq = getRequest().path;
            	System.out.println("Proxy HTTP: " + rq);
            	if (rq.startsWith("/r/player/")) {
            		String plr = rq.substring("/r/player/".length());
            		if (plr.contains("/")) {
            			plr = plr.substring(0, plr.indexOf("/"));
            			try {
            				UUID.fromString(plr);
            				rq = rq.substring("/r/player/".length());
            				rq = rq.substring(rq.indexOf("/") + 1);
            				rq = "/r/" + rq; 
            			}catch (Exception e) {}
            		}
            	}
                HttpURLConnection conn = (HttpURLConnection)new URL(proxy + "/" + rq).openConnection();
                conn.setRequestMethod(method);
                for (String header : getRequest().headers.keySet()) {
                    conn.addRequestProperty(header, getRequest().headers.get(header));
                }
                if (body.length != 0) {
                    conn.setDoOutput(true);
                    conn.getOutputStream().write(body);
                }

                // Get response
                int res = conn.getResponseCode();
                if (res >= 200 && res < 400)
                    getResponse().setContent(conn.getHeaderField("Content-Type"), conn.getInputStream());
                else
                    getResponse().setContent(conn.getHeaderField("Content-Type"), conn.getErrorStream());
                conn.getHeaderFields().forEach((k, v) -> {
                    if (k == null)
                        return;
                    v.forEach(t -> getResponse().setHeader(k, t, true));
                });
            } catch (IOException e) {
                setResponseCode(503);
                setResponseMessage("Service unavailable");
            }
        }

        @Override
        public HttpUploadProcessor createNewInstance() {
            return new ProxyHttpProcessor(proxy);
        }

        @Override
        public String path() {
            return "/";
        }

        @Override
        public boolean supportsGet() {
            return true;
        }

        @Override
        public boolean supportsChildPaths() {
            return true;
        }
    }
