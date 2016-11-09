/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener.socket.processer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import kr.pe.deverexpert.server.listener.socket.resource.ByPassSocketHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * 클라이언트의 요청 데이터를 ByPass에 전달하여 응답 받는 Process
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public class ByPassSocketProcessor implements ByPassSocketHandler {
	
    static Logger logger = LoggerFactory.getLogger(ByPassSocketProcessor.class);
    
    private Socket byPassSocket = null;
    private SocketAddress sockaddr = null;
    
    private BufferedReader clientSocketIn = null;
    private PrintWriter clientSocketPw = null;
    private BufferedReader byPassSocketIn = null;
    private PrintWriter byPassSocketOut = null;
    
    String msg = null;
    
    @Override
    public void byPassHandle(Socket clientSocket, String byPassIp, int byPassPort) {
        
        try {
            clientSocketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientSocketPw = new PrintWriter(clientSocket.getOutputStream(),true);
            
            while(true)
            {               
                // Client에서 받은 Message 를 저장
                msg = clientSocketIn.readLine();
                logger.info("ByPassHandle[RECV] : " + msg);
                
                sockaddr = new InetSocketAddress(InetAddress.getByName(byPassIp), byPassPort);
                byPassSocket = new Socket();
                byPassSocket.connect(sockaddr);
                
                if ( byPassSocket.isConnected() ) {
                    
                    byPassSocketIn = new BufferedReader(new InputStreamReader(byPassSocket.getInputStream()));
                    byPassSocketOut = new PrintWriter(byPassSocket.getOutputStream(),true);
                    
                    // Client에서 받은 Message를 ByPass Server로 전송
                    byPassSocketOut.println(msg);
                    logger.info("ByPassHandle[ByPass -> Target] : " + msg);
                    
                    // ByPass Server에서 받은 Message를 저장
                    msg = byPassSocketIn.readLine();
                    logger.info("ByPassHandle[ByPass <- Target] : " + msg);

                    // ByPass Server에서 받은 Message를 Client로 전송
                    clientSocketPw.println(msg);
                    logger.info("ByPassHandle[SEND] : " + msg);
                }
            }
        } catch ( IOException e ) {
            logger.error("", e);
        } finally {
            try { if ( byPassSocketOut != null ) byPassSocketOut.close(); } catch ( Exception e ) {}
            try { if ( byPassSocketIn != null ) byPassSocketIn.close(); } catch ( Exception e ) {}
            try { if ( byPassSocket != null ) byPassSocket.close(); } catch ( Exception e ) {}
        }
    }
	
}
