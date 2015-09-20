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
package com.genesyslab.cst.softphone.services.voice;

/**
 * This class stores the configuration parameters required to 
 * construct and initialize the VoiceService.
 * 
 * @author <a href="mailto:Chris.Munn@genesyslab.com">Chris Munn</a>
 */
public class VoiceServiceConfiguration {
    
    /*
     * Constructor
     * 
     * @param tserverUri Uri of the primary TServer in the format tcp://host:port
     * @param backupTServerUri Uri of the backup TServer in the format tcp://host:port
     * @param dn The dn to be registered and used by the softphone.
     * @param queue The queue to be used when logging in.
     * @param agentId The agent login id to be used when logging in.
     */
    public VoiceServiceConfiguration(
            final String tserverUri,
            final String backupTServerUri,
            final String dn,
            final String queue,
            final String agentId) {
        
        if(tserverUri == null || tserverUri.isEmpty()) {
            throw new IllegalArgumentException("tserverUri is required.");
        }
        if(dn == null || dn.isEmpty()) {
            throw new IllegalArgumentException("dn is required.");
        }
        if(agentId == null || agentId.isEmpty()) {
            throw new IllegalArgumentException("agentId is required.");
        }

        this.tserverUri = tserverUri;
        this.backupTServerUri = backupTServerUri;
        this.dn = dn;
        this.queue = queue;
        this.agentId = agentId;        
    }
    
    private final String tserverUri;
    private final String backupTServerUri;
    private final String dn;
    private final String queue;
    private final String agentId;
    
    /*
     * @returns Uri of the primary TServer in the format tcp://host:port
     */
    public String getTServerUri() {
        return tserverUri;
    }
    
    /*
     * @returns Uri of the backup TServer in the format tcp://host:port
     */
    public String getBackupTServerUri() {
        return backupTServerUri;
    }
    
    /*
     * @returns The dn to be registered and used by the softphone.
     */    
    public String getDn() {
        return dn;
    }
    
    /*
     * @returns The queue to be used when logging in.
     */    
    public String getQueue() {
        return queue;
    }
    
    /*
     * @returns The agent login id to be used when logging in.
     */    
    public String getAgentId() {
        return agentId;
    }      
}