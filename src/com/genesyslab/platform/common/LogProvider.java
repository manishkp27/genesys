// ===============================================================================
//
//  Any authorized distribution of any copy of this code (including any related
//  documentation) must reproduce the following restrictions, disclaimer and copyright
//  notice:
//
//  The Genesys name, trademarks and/or logo(s) of Genesys shall not be used to name
//  (even as a part of another name), endorse and/or promote products derived from
//  this code without prior written permission from Genesys Telecommunications
//  Laboratories, Inc.
//
//  The use, copy, and/or distribution of this code is subject to the terms of the Genesys
//  Developer License Agreement.  This code shall not be used, copied, and/or
//  distributed under any other license agreement.
//
//  THIS CODE IS PROVIDED BY GENESYS TELECOMMUNICATIONS LABORATORIES, INC.
//  ("GENESYS") "AS IS" WITHOUT ANY WARRANTY OF ANY KIND. GENESYS HEREBY
//  DISCLAIMS ALL EXPRESS, IMPLIED, OR STATUTORY CONDITIONS, REPRESENTATIONS AND
//  WARRANTIES WITH RESPECT TO THIS CODE (OR ANY PART THEREOF), INCLUDING, BUT
//  NOT LIMITED TO, IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
//  PARTICULAR PURPOSE OR NON-INFRINGEMENT. GENESYS AND ITS SUPPLIERS SHALL
//  NOT BE LIABLE FOR ANY DAMAGE SUFFERED AS A RESULT OF USING THIS CODE. IN NO
//  EVENT SHALL GENESYS AND ITS SUPPLIERS BE LIABLE FOR ANY DIRECT, INDIRECT,
//  CONSEQUENTIAL, ECONOMIC, INCIDENTAL, OR SPECIAL DAMAGES (INCLUDING, BUT
//  NOT LIMITED TO, ANY LOST REVENUES OR PROFITS).
//
//  Copyright (c) 2006 - 2012 Genesys Telecommunications Laboratories, Inc. All rights reserved.
// ===============================================================================
package com.genesyslab.platform.common;

import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.logging.LoggerFactory;
import com.genesyslab.platform.logging.RootLogger;
import com.genesyslab.platform.logging.runtime.LoggerException;

/*
 * This component provides access to a common root logger for the application. The
 * LoggerFactory within Platform SDK does not provide static methods like the log4j factory
 * does, so I've introduced this provider to allow other internal components clean/easy
 * access to the root logger (and child loggers). 
 * 
 * @author <a href="mailto:Chris.Munn@genesyslab.com">Chris Munn</a>
 */
public class LogProvider {
    
    private static LoggerFactory loggerFactory;
    
    /*
     * This helper provides access to the loggerFactory, which is a lazy
     * initialized singleton.
     */
    private synchronized static LoggerFactory getFactory() throws LoggerException {
        if(loggerFactory == null) {
            loggerFactory = new LoggerFactory("SoftphoneSample");
        }
        
        return loggerFactory;
    }
        
    /*
     * @returns The root logger being used by the application.
     */
    public static RootLogger getRootLogger() throws LoggerException {
        
        LoggerFactory factory = getFactory();
        return factory.getRootLogger();                
    }
    
    /*
     * @returns A new child logger created using the shared logger factory.
     */
    public static ILogger createLogger(String name) throws LoggerException {
        
        LoggerFactory factory = getFactory();
        return factory.getLogger(name);        
    }
}
