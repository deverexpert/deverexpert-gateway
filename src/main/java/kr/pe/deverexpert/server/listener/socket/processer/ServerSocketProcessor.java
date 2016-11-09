/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener.socket.processer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import kr.pe.deverexpert.server.listener.socket.resource.ServerSocketHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * 클라이언트의 요청 데이터를 처리하는 Process
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public class ServerSocketProcessor implements ServerSocketHandler {
	
    static Logger logger = LoggerFactory.getLogger(ServerSocketProcessor.class);
    
    private BufferedReader clientSocketIn = null;
    private PrintWriter clientSocketPw = null;
    
    String msg = null;
    
    @Override
    public void serverHandle(Socket clientSocket) {
        
        try {
            clientSocketIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            clientSocketPw = new PrintWriter(clientSocket.getOutputStream(),true);
            
            // Client에서 받은 Message 를 저장
            msg = clientSocketIn.readLine();
            logger.info("ServerHandle[RECV] : " + msg);
            
            // ByPass Server에서 받은 Message를 Client로 전송
            clientSocketPw.println(msg);
            logger.info("ServerHandle[SEND] : " + msg);

        } catch ( IOException e ) {
            logger.error("", e);
        }
    }
	
}
