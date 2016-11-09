/*******************************************************************************
 * Copyright(c) 2012 SK M&S. All rights reserved.
 * This software is the proprietary information of SK M&S.
 *******************************************************************************/
package kr.pe.deverexpert.server.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author <a href=mailto:dever@sk.com>이재형</a>
 * @since 2015. 2. 26.
 */
public class HttpClientByPassUtil {
    
    private static Logger logger = LoggerFactory.getLogger(HttpClientByPassUtil.class);

    public HttpClientByPassUtil() {
    }

    public HttpResponse excuete(boolean isUseProxy, String proxyUrl, HttpClient client, HttpRequestBase request) throws ClientProtocolException, IOException, URISyntaxException {

        //로컬 통신시 개발서버를 통해 Nxmile로 우회하도록 설정
        //local was의 실행옵션에 isLocal="Y"를 추가하면 된다.

        if (logger.isDebugEnabled()) logger.debug("isUseProxy : " + isUseProxy);

        if (isUseProxy) {

            //target url
            String targetUrl = request.getURI().toString();

            //Proxy url
            URI uri = new URI(proxyUrl);
            request.setURI(uri);

            request.setHeader("x-forward-request-url", targetUrl);

            if (logger.isDebugEnabled()) logger.debug("proxy url : " + proxyUrl);
            if (logger.isDebugEnabled()) logger.debug("target url : " + targetUrl);

        }

        return client.execute(request);

    }
}
