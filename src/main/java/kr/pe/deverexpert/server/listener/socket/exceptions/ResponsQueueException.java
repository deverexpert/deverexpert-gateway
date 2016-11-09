/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener.socket.exceptions;

import java.io.Serializable;

/**
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public class ResponsQueueException extends Exception implements Serializable {
	
    private static final long serialVersionUID = 7761016436917035163L;

    public ResponsQueueException(String msg) {
		super("ResponsQueueException : " + msg);
	}
	
}
