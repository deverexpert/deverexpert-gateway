/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener.socket.worker;

import java.net.Socket;

import kr.pe.deverexpert.server.listener.socket.resource.ServerSocketHandler;
import kr.pe.deverexpert.server.listener.socket.resource.ServerSocketQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public class ServerSocketWorkerThread extends Thread {
	
	static Logger logger = LoggerFactory.getLogger(ServerSocketWorkerThread.class);
	
	private ServerSocketQueue 	queue; 			// Request Queue
	private boolean 		running; 		// Thread Running Flag
	private boolean 		processing; 	// Processing Flag
	private int				threadNumber; 	// Thread Number
	private ServerSocketHandler 	serverSocketHandler;  // Server Socket Handler
	
	/**
	 * @param queue
	 * @param threadNumber
	 * @param serverSocketHandler
	 */
	public ServerSocketWorkerThread(ServerSocketQueue queue, int threadNumber, ServerSocketHandler serverSocketHandler) {
		this.queue = queue;
		this.threadNumber = threadNumber;
		
		try{
			// Create Handle Instance
			this.serverSocketHandler = (ServerSocketHandler) (serverSocketHandler.getClass().newInstance());
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

					this.serverSocketHandler.serverHandle(socket);

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
