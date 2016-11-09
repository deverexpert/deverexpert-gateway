/*******************************************************************************
 * Copyright(c) 2014 deverexpert. All rights reserved.
 * This software is the proprietary information of deverexpert.
 *******************************************************************************/
package kr.pe.deverexpert.server.listener.socket.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import kr.pe.deverexpert.server.listener.socket.exceptions.RequestQueueException;
import kr.pe.deverexpert.server.listener.socket.worker.ByPassSocketWorkerThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 
 * @author <a href=mailto:deverexpert@gmail.com>deverexpert</a>
 * @since 2015. 1. 30.
 */
public class ByPassSocketQueue {
	
	static Logger logger = LoggerFactory.getLogger(ByPassSocketQueue.class);

    private LinkedList<Object> 	queue = new LinkedList<Object>(); 		            // Request Queue
    private List<ByPassSocketWorkerThread> 		threadPool = new ArrayList<ByPassSocketWorkerThread>(); 	// Thread Pool Saving Area

    private int			maxQueueLength; 				// Max Queue Length
    private int 		minThreads; 					// Min Thread Count 
    private int 		maxThreads; 					// Max Thread Count
    private int 		currentThreads = 0; 			// Current Thread Count
    
    private ByPassSocketHandler 	byPassSocketHandler; 		    // By Pass Handler Class
    
    private boolean 	running = true; 				// Queue Running Flag
    
    private String          byPassIp;
    private int             byPassPort;
    
    
    public ByPassSocketQueue( ByPassSocketHandler byPassSocketHandler, int minThreads, int maxThreads, String byPassIp, int byPassPort) {
    	
    	// Parameter Init
		this.byPassSocketHandler = byPassSocketHandler;
		this.minThreads = minThreads;
		this.maxThreads = maxThreads;
		this.currentThreads = this.minThreads;
		this.byPassIp = byPassIp;
		this.byPassPort = byPassPort;

		// Min Thread Create
		for( int i = 0; i < this.minThreads; i++ ) {
			logger.debug("RequestThead Create[" + i + "]"); 
			ByPassSocketWorkerThread thread = new ByPassSocketWorkerThread( this, i, byPassSocketHandler );
			thread.start();
			this.threadPool.add( thread );
		}
	}
    
    // Return Request Handler Class Name
    public String getRequestHandlerClassName()
    {
        return this.byPassSocketHandler.getClass().getName();
    }
    
    public String getByPassIp() {
        return this.byPassIp;
    }
    
    public int getByPassPort() {
        return this.byPassPort;
    }
    
    // Add Object
    public synchronized void add( Object o ) throws RequestQueueException {

	    logger.debug( "queue.size : " +queue.size());
	    
        if( queue.size() > this.maxQueueLength )
        {
            throw new RequestQueueException( "The Request Queue is full. Max size : " + this.maxQueueLength );
        }

        // 큐에 새로운 오브젝트를 추가한다.
        queue.addLast( o );

        boolean availableThread = false;
        for( Iterator<ByPassSocketWorkerThread> i=this.threadPool.iterator(); i.hasNext(); )
        {
            ByPassSocketWorkerThread rt = ( ByPassSocketWorkerThread )i.next();
            if( !rt.isProcessing() )
            {
                logger.debug( "Found an available thread" );
                availableThread = true;
                break;
            }
            logger.debug( "Thread is busy" );

        }

        logger.debug( "CurrentThreads : " + this.currentThreads);
        logger.debug( "MaxThreads : " + this.maxThreads);
       
        if( !availableThread ) {
            if( this.currentThreads < this.maxThreads ) {
                logger.debug( "Creating a new thread to satisfy the incoming request" );
                ByPassSocketWorkerThread thread = new ByPassSocketWorkerThread( this, currentThreads++, this.byPassSocketHandler );
                thread.start();
                this.threadPool.add( thread );
            }
            else
            {
                logger.debug( "Whoops, can’t grow the thread pool, guess you have to wait" );
            }
        }

       notifyAll();
    }
    
    // In Queue First Object Return
    public synchronized Object getNextObject() {
    	
        // Setup waiting on the Request Queue
        while( queue.isEmpty() ) {
            try {
                if( !running ) {
                    return null;
                }
                wait();
            } catch( InterruptedException ie ) {}
        }
        
        return queue.removeFirst();
    }
    
    // Drop Request Thread
    public synchronized void shutdown() {
    	
        logger.debug( "Shutting down request threads..." );

       this.running = false;

        for( Iterator<ByPassSocketWorkerThread> i=this.threadPool.iterator(); i.hasNext(); ) {
            ByPassSocketWorkerThread rt = ( ByPassSocketWorkerThread )i.next();
            rt.killThread();
        }

        notifyAll();
    }    
}
