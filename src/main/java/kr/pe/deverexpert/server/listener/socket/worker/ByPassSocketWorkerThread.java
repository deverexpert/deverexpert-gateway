/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener.socket.worker;

import java.net.Socket;

import kr.pe.deverexpert.server.listener.socket.resource.ByPassSocketHandler;
import kr.pe.deverexpert.server.listener.socket.resource.ByPassSocketQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public class ByPassSocketWorkerThread extends Thread {
	
	static Logger logger = LoggerFactory.getLogger(ByPassSocketWorkerThread.class);
	
	private ByPassSocketQueue 	queue; 			// Request Queue
	private boolean 		running; 		// Thread Running Flag
	private boolean 		processing; 	// Processing Flag
	private int				threadNumber; 	// Thread Number
	private ByPassSocketHandler 	byPassSocketHandler;  // By Pass Handler
	private String          byPassIp;
	private int             byPassPort;
	
	/**
	 * @param queue
	 * @param threadNumber
	 * @param byPassSocketHandler
	 * @param byPassIp
	 * @param byPassPort
	 */
	public ByPassSocketWorkerThread(ByPassSocketQueue queue, int threadNumber, ByPassSocketHandler byPassSocketHandler) {
		this.queue = queue;
		this.threadNumber = threadNumber;
		this.byPassIp = this.queue.getByPassIp();
		this.byPassPort = this.queue.getByPassPort();
		
		try{
			// Create Handle Instance
			this.byPassSocketHandler = (ByPassSocketHandler) (byPassSocketHandler.getClass().newInstance());
		}catch (Exception e){
			logger.error(e.toString());
		}
	}
	
	// Process State
	public boolean isProcessing() {
		return this.processing;
	}

	// Kill Thread
	public void killThread() {
		logger.debug("[" + threadNumber+ "]: Attempting to kill thread.");
		this.running = false;
	}
	
	public void run() {
		
		this.running = true;
		
		while ( running ) {
			try{
				// Read Queue Object - Socket
				Object o = queue.getNextObject();
				if ( running ) {

					Socket socket = (Socket) o;

					// Process Start
					this.processing = true;
					logger.debug("[" + threadNumber+ "]: Processing request.");

					this.byPassSocketHandler.byPassHandle(socket, this.byPassIp, this.byPassPort);

					// Process Stop
					this.processing = false;
					logger.debug("[" + threadNumber+ "]: Finished Processing request.");
				}
			}catch (Exception e){
				logger.error(e.toString());
			}
		}

		logger.debug("[" + threadNumber + "]: Thread shutting down.");
	}	
}
