/*******************************************************************************
 * Copyright(c) 2012 SK M&S. All rights reserved.
 * This software is the proprietary information of SK M&S.
 *******************************************************************************/
package kr.pe.deverexpert.server.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author <a href=mailto:dever@sk.com>이재형</a>
 * @since 2015. 2. 26.
 */
public class HttpUrlConnectionByPassUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpUrlConnectionByPassUtil.class);

    private String PROTOCOL_HTTP = "http";
    private String PROTOCOL_HTTPS = "https";

    public HttpURLConnection getConnection(boolean isUseProxy, String proxyUrl, URL url) {

        HttpURLConnection conn = null;

        try {

            if (logger.isDebugEnabled()) logger.debug("isUseProxy : " + isUseProxy);
            if (logger.isDebugEnabled()) logger.debug("target : " + url.toString());

            //로컬에서 proxy를 통해 갈 경우 url 변경
            if (isUseProxy) {

                String targetUrlStr = url.toString();

                URL targetUrl = new URL(proxyUrl);

                if (logger.isDebugEnabled()) logger.debug("proxy : " + proxyUrl);

                if (PROTOCOL_HTTPS.equals(targetUrl.getProtocol())) {

                    HttpsURLConnection.setDefaultHostnameVerifier(new CustomVF());
                    conn = (HttpsURLConnection) targetUrl.openConnection();

                } else {
                    conn = (HttpURLConnection) targetUrl.openConnection();
                }

                conn.setRequestProperty("x-forward-request-url", targetUrlStr);

            } else {

                //로컬에서 테스트하는 경우가 아니면 원래 가야할 곳으로 호출
                String protocol = url.getProtocol();
                if (PROTOCOL_HTTPS.equals(protocol)) {
                    HttpsURLConnection.setDefaultHostnameVerifier(new CustomVF());
                    conn = (HttpsURLConnection) url.openConnection();
                } else {
                    conn = (HttpURLConnection) url.openConnection();
                }
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }

        return conn;

    }

    private final class CustomVF implements HostnameVerifier {

        private CustomVF() {
            // default constructor
        }

        @Override
        public boolean verify(String hostname, SSLSession session) {
            // 무조건 성공
            return true;
        }

    }

}
