/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.message.HeaderGroup;
import org.apache.http.util.EntityUtils;

/**
 * 
 * @author <a href=mailto:dever@sk.com>이재형</a>
 * @since 2015. 2. 26.
 */
public class ByPassUrlServletListener extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /* INIT PARAMETER NAME CONSTANTS */

    /** A boolean parameter name to enable logging of input and target URLs to the servlet log. */
    public static final String P_LOG = "log";

    /* MISC */

    protected boolean doLog = false;
    protected boolean doForwardIP = true;

    private HttpClient proxyClient;

    @Override
    public String getServletInfo() {
        return "A proxy servlet by deverexpert, deverexpert@gmail.com";
    }

    /**
     * Reads a configuration parameter. By default it reads servlet init parameters but
     * it can be overridden.
     */
    protected String getConfigParam(String key) {
        return getServletConfig().getInitParameter(key);
    }

    @Override
    public void init() throws ServletException {
        String doLogStr = getConfigParam(P_LOG);
        if (doLogStr != null) {
            this.doLog = Boolean.parseBoolean(doLogStr);
        }

        proxyClient = new DefaultHttpClient();

    }

    @Override
    public void destroy() {
        //As of HttpComponents v4.3, clients implement closeable
        if (proxyClient instanceof Closeable) {//TODO AutoCloseable in Java 1.6
            try {
                ((Closeable) proxyClient).close();
            } catch (IOException e) {
                log("While destroying servlet, shutting down HttpClient: " + e, e);
            }
        } else {
        }
        super.destroy();
    }

    @Override
    protected void service(HttpServletRequest servletRequest, HttpServletResponse servletResponse) throws ServletException, IOException {
        // Make the Request
        //note: we won't transfer the protocol version because I'm not sure it would truly be compatible
        String method = servletRequest.getMethod();
        // String proxyRequestUri = rewriteUrlFromRequest(servletRequest);

        HttpHost targetHost;//URIUtils.extractHost(targetUriObj);
        String proxyRequestUri = servletRequest.getHeader("x-forward-request-url");
        try {
            targetHost = URIUtils.extractHost(new URI(proxyRequestUri));
        } catch (URISyntaxException e1) {
            throw new ServletException(e1);
        }

        HttpRequest proxyRequest;
        //spec: RFC 2616, sec 4.3: either of these two headers signal that there is a message body.
        if (servletRequest.getHeader(HttpHeaders.CONTENT_LENGTH) != null || servletRequest.getHeader(HttpHeaders.TRANSFER_ENCODING) != null) {
            HttpEntityEnclosingRequest eProxyRequest = new BasicHttpEntityEnclosingRequest(method, proxyRequestUri);
            // Add the input entity (streamed)
            //  note: we don't bother ensuring we close the servletInputStream since the container handles it
            eProxyRequest.setEntity(new InputStreamEntity(servletRequest.getInputStream(), servletRequest.getContentLength()));
            proxyRequest = eProxyRequest;
        } else proxyRequest = new BasicHttpRequest(method, proxyRequestUri);

        copyRequestHeaders(servletRequest, proxyRequest);

        setXForwardedForHeader(servletRequest, proxyRequest);

        HttpResponse proxyResponse = null;
        try {
            // Execute the request
            if (doLog) {
                // log("server" + servletRequest.getServerName()+" "+servletRequest.getServerPort());
                log("proxy " + method + " uri: " + servletRequest.getRequestURI() + " -- " + proxyRequest.getRequestLine().getUri());
            }
            // proxyResponse = proxyClient.execute(getTargetHost(servletRequest), proxyRequest);
            proxyResponse = proxyClient.execute(targetHost, proxyRequest);

            // Process the response
            int statusCode = proxyResponse.getStatusLine().getStatusCode();

            // Pass the response code. This method with the "reason phrase" is deprecated but it's the only way to pass the
            //  reason along too.
            //noinspection deprecation
            servletResponse.setStatus(statusCode);

            copyResponseHeaders(proxyResponse, servletRequest, servletResponse);

            // Send the content to the client
            copyResponseEntity(proxyResponse, servletResponse);

        } catch (Exception e) {
            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof ServletException) throw (ServletException) e;
            //noinspection ConstantConditions
            if (e instanceof IOException) throw (IOException) e;
            throw new RuntimeException(e);

        } finally {
            // make sure the entire entity was consumed, so the connection is released
            if (proxyResponse != null) consumeQuietly(proxyResponse.getEntity());
            //Note: Don't need to close servlet outputStream:
            // http://stackoverflow.com/questions/1159168/should-one-call-close-on-httpservletresponse-getoutputstream-getwriter
        }
    }

    protected void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException e) {
            log(e.getMessage(), e);
        }
    }

    /** HttpClient v4.1 doesn't have the
     * {@link org.apache.http.util.EntityUtils#consumeQuietly(org.apache.http.HttpEntity)} method. */
    protected void consumeQuietly(HttpEntity entity) {
        try {
            EntityUtils.consume(entity);
        } catch (IOException e) {//ignore
            log(e.getMessage(), e);
        }
    }

    /** These are the "hop-by-hop" headers that should not be copied.
     * http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html
     * I use an HttpClient HeaderGroup class instead of Set<String> because this
     * approach does case insensitive lookup faster.
     */
    protected static final HeaderGroup hopByHopHeaders;
    static {
        hopByHopHeaders = new HeaderGroup();
        String[] headers = new String[] { "Connection", "Keep-Alive", "Proxy-Authenticate", "Proxy-Authorization", "TE", "Trailers",
                "Transfer-Encoding", "Upgrade" };
        for (String header : headers) {
            hopByHopHeaders.addHeader(new BasicHeader(header, null));
        }
    }

    /** Copy request headers from the servlet client to the proxy request. */
    @SuppressWarnings("rawtypes")
    protected void copyRequestHeaders(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
        // Get an Enumeration of all of the header names sent by the client
        Enumeration enumerationOfHeaderNames = servletRequest.getHeaderNames();
        while (enumerationOfHeaderNames.hasMoreElements()) {
            String headerName = (String) enumerationOfHeaderNames.nextElement();
            //Instead the content-length is effectively set via InputStreamEntity
            if (headerName.equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)) continue;
            if (hopByHopHeaders.containsHeader(headerName)) continue;

            Enumeration headers = servletRequest.getHeaders(headerName);
            while (headers.hasMoreElements()) {//sometimes more than one value
                String headerValue = (String) headers.nextElement();
                // In case the proxy host is running multiple virtual servers,
                // rewrite the Host header to ensure that we get content from
                // the correct virtual server
                if (headerName.equalsIgnoreCase(HttpHeaders.HOST)) {
                    //
                } else if (headerName.equalsIgnoreCase(org.apache.http.cookie.SM.COOKIE)) {
                    headerValue = getRealCookie(headerValue);
                }
                proxyRequest.addHeader(headerName, headerValue);
            }
        }
    }

    private void setXForwardedForHeader(HttpServletRequest servletRequest, HttpRequest proxyRequest) {
        String headerName = "X-Forwarded-For";
        if (doForwardIP) {
            String newHeader = servletRequest.getRemoteAddr();
            String existingHeader = servletRequest.getHeader(headerName);
            if (existingHeader != null) {
                newHeader = existingHeader + ", " + newHeader;
            }
            proxyRequest.setHeader(headerName, newHeader);
        }
    }

    /** Copy proxied response headers back to the servlet client. */
    protected void copyResponseHeaders(HttpResponse proxyResponse, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        for (Header header : proxyResponse.getAllHeaders()) {
            if (hopByHopHeaders.containsHeader(header.getName())) continue;
            if (header.getName().equals(org.apache.http.cookie.SM.SET_COOKIE) || header.getName().equals(org.apache.http.cookie.SM.SET_COOKIE2)) {
                copyProxyCookie(servletRequest, servletResponse, header);
            } else {
                servletResponse.addHeader(header.getName(), header.getValue());
            }
        }
    }

    /** Copy cookie from the proxy to the servlet client.
     *  Replaces cookie path to local path and renames cookie to avoid collisions.
     */
    protected void copyProxyCookie(HttpServletRequest servletRequest, HttpServletResponse servletResponse, Header header) {
        List<HttpCookie> cookies = HttpCookie.parse(header.getValue());
        String path = getServletContext().getServletContextName();
        if (path == null) {
            path = "";
        }
        path += servletRequest.getServletPath();

        for (HttpCookie cookie : cookies) {
            //set cookie name prefixed w/ a proxy value so it won't collide w/ other cookies
            String proxyCookieName = getCookieNamePrefix() + cookie.getName();
            Cookie servletCookie = new Cookie(proxyCookieName, cookie.getValue());
            servletCookie.setComment(cookie.getComment());
            servletCookie.setMaxAge((int) cookie.getMaxAge());
            servletCookie.setPath(path); //set to the path of the proxy servlet
            // don't set cookie domain
            servletCookie.setSecure(cookie.getSecure());
            servletCookie.setVersion(cookie.getVersion());
            servletResponse.addCookie(servletCookie);
        }
    }

    /** Take any client cookies that were originally from the proxy and prepare them to send to the
     * proxy.  This relies on cookie headers being set correctly according to RFC 6265 Sec 5.4.
     * This also blocks any local cookies from being sent to the proxy.
     */
    protected String getRealCookie(String cookieValue) {
        StringBuilder escapedCookie = new StringBuilder();
        String cookies[] = cookieValue.split("; ");
        for (String cookie : cookies) {
            String cookieSplit[] = cookie.split("=");
            if (cookieSplit.length == 2) {
                String cookieName = cookieSplit[0];
                if (cookieName.startsWith(getCookieNamePrefix())) {
                    cookieName = cookieName.substring(getCookieNamePrefix().length());
                    if (escapedCookie.length() > 0) {
                        escapedCookie.append("; ");
                    }
                    escapedCookie.append(cookieName).append("=").append(cookieSplit[1]);
                }
            }

            cookieValue = escapedCookie.toString();
        }
        return cookieValue;
    }

    /** The string prefixing rewritten cookies. */
    protected String getCookieNamePrefix() {
        return "!Proxy!" + getServletConfig().getServletName();
    }

    /** Copy response body data (the entity) from the proxy to the servlet client. */
    protected void copyResponseEntity(HttpResponse proxyResponse, HttpServletResponse servletResponse) throws IOException {
        HttpEntity entity = proxyResponse.getEntity();
        if (entity != null) {
            OutputStream servletOutputStream = servletResponse.getOutputStream();
            entity.writeTo(servletOutputStream);
        }
    }

}
