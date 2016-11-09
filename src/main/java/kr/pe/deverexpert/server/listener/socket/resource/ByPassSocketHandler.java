/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener.socket.resource;

import java.net.Socket;

/**
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public interface ByPassSocketHandler {
	
	public void byPassHandle(Socket socket, String byPassIp, int byPassPort);
	
}
