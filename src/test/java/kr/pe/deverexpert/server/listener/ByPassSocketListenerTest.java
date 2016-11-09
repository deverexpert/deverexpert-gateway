/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author <a href=mailto:dever@sk.com>이재형</a>
 * @since 2015. 2. 4.
 */
public class ByPassSocketListenerTest {
    
    private final int listenerPort = 9999;
    
    @Before
    public void before() {
        
        ByPassSocketListener listener = new ByPassSocketListener();

        listener.setListenerName("By Pass Socket Listener");
        listener.setListenerPort(this.listenerPort);
        listener.setByPassIp("");
        listener.setByPassPort(80);
        listener.setClientTimeOut(5);
        listener.setMinThreadQuantity(1);
        listener.setMaxThreadQuantity(1);
        listener.setProcessClass("kr.pe.deverexpert.server.listener.socket.processer.ByPassSocketProcessor");
        
        listener.startServer();
        
    }

    @Test
    public void startServer() {
        
        try {
            SocketAddress sockaddress = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), listenerPort);
            Socket socket = new Socket();
            socket.connect(sockaddress);
            
            Assert.assertTrue(socket.isConnected());
            
        } catch ( Exception e ) {
            e.printStackTrace();
        }
        
        
    }

}
