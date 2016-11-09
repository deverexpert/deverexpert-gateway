/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import kr.pe.deverexpert.server.listener.socket.resource.ByPassSocketHandler;
import kr.pe.deverexpert.server.listener.socket.resource.ByPassSocketQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public class ByPassSocketListener extends Thread {
    
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    
    private ServerSocket  service;                // Server Socket
    
    /** 서비스 명 */
    private String      listenerName = "";
    /** 서비스 포트 */
    private int         listenerPort = 0;
    /** 전달 서버 IP */
    private String      byPassIp = "";
    /** 전달 서버 PORT */
    private int         byPassPort = 0;
    /** 클라이언트 타임 아웃 */
    private int         clientTimeOut = 0;
    /** 최소 Thread Count */
    private int         minThreadQuantity = 0;
    /** 최대 Thread Count */
    private int         maxThreadQuantity = 0;
    /** 수행할 ProcessClass */
    private String      processClass = "";
    
    /** Server Running Flag */
    private boolean     running; 
    /** Request Queue */
    private ByPassSocketQueue byPassQueue; 
    

    /**
     * 서비스 명
     * @param listenerName 
     */
    public void setListenerName(String listenerName) {
        this.listenerName = listenerName;
    }

    /**
     * 서비스 포트
     * @param listenerPort 
     */
    public void setListenerPort(int listenerPort) {
        this.listenerPort = listenerPort;
    }

    /**
     * 전달 서버 IP
     * @param byPassIp 
     */
    public void setByPassIp(String byPassIp) {
        this.byPassIp = byPassIp;
    }

    /**
     * 전달 서버 PORT
     * @param byPassPort 
     */
    public void setByPassPort(int byPassPort) {
        this.byPassPort = byPassPort;
    }

    /**
     * 클라이언트 타임 아웃
     * @param clientTimeOut 
     */
    public void setClientTimeOut(int clientTimeOut) {
        this.clientTimeOut = clientTimeOut;
    }

    /**
     * 최소 Thread Count
     * @param minThreadQuantity 
     */
    public void setMinThreadQuantity(int minThreadQuantity) {
        this.minThreadQuantity = minThreadQuantity;
    }

    /**
     * 최대 Thread Count
     * @param maxThreadQuantity 
     */
    public void setMaxThreadQuantity(int maxThreadQuantity) {
        this.maxThreadQuantity = maxThreadQuantity;
    }
    
    /**
     * 수행할 ProcessClass
     * @param processClass 
     */
    public void setProcessClass(String processClass) {
        this.processClass = processClass;
    }
    
    /**
     * 생성자
     */
    public ByPassSocketListener() {}

    /**
     * Parameter Checking
     */
    public void checkParameter() {
        
        logger.info("************************************************************************");
        logger.info("ListenerName      : " + this.listenerName);
        logger.info("ListenerPort      : " + this.listenerPort);
        logger.info("ByPassIp          : " + this.byPassIp);
        logger.info("ByPassPort        : " + this.byPassPort);
        logger.info("ClientTimeOut     : " + this.clientTimeOut);
        logger.info("MinThreadQuantity : " + this.minThreadQuantity);
        logger.info("MaxThreadQuantity : " + this.maxThreadQuantity);
        logger.info("ProcessClass      : " + this.processClass);
        logger.info("************************************************************************");
        
    }
    
    /**
     * Start Server
     */
    public void startServer() {
        
        try {
            ByPassSocketHandler byPassSocketHandler = (ByPassSocketHandler) (Class.forName(this.processClass).newInstance());

            this.byPassQueue = new ByPassSocketQueue(byPassSocketHandler, this.minThreadQuantity, this.maxThreadQuantity, this.byPassIp, this.byPassPort);

            service = new ServerSocket(listenerPort);
            
            checkParameter();
            
            this.start();
        } catch ( Exception e ) {
            logger.error("Server Initialize Fail!!!");
            e.printStackTrace();
        }
    }
    
    public void run() {
        
        logger.info("Server Started");
        
        this.running = true;
        
        while ( running ) {
            try {
                Socket s = service.accept();
                s.setSoTimeout(6000 * clientTimeOut);
                
                // Request Queue Add Socket
                this.byPassQueue.add(s);
                
            }catch (SocketException se){
                if (this.running){
                    logger.error(se.toString());
                }
            }catch (Exception e){
                logger.error(e.toString());
            }
        }
        
        logger.debug("Server Stop");
        this.byPassQueue.shutdown();
        
    }        
    

}
