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

import com.genesyslab.cst.softphone.services.ConnectionStatus;
import java.util.Collection;

/**
 * This interface defines the set of voice operations used by the softphone. By
 * introducing this interface, the rest of the application is no longer dependent 
 * on the Voice PSDK. It also provides an opportunity to test the service without
 * the GUI portion, as well as an opportunity for re-use.
 * 
 * @author <a href="mailto:Chris.Munn@genesyslab.com">Chris Munn</a>
 */
public interface VoiceService {
        
    /*
     * This method adds a new listener to be notified of state changes and events.
     * 
     * @param listener The listener to be added.
     */
    void addListener(VoiceServiceListener listener) throws VoiceServiceException;
    
    /*
     * This method removes a previous added listener.
     * 
     * @param listener The listener to be removed.
     */
    void removeListener(VoiceServiceListener listener) throws VoiceServiceException;
    
    /*
     * @returns The connection status of the VoiceService.
     */
    ConnectionStatus getConnectionStatus();
    
    /*
     * @returns The configuration used to contruct the VoiceService.
     */
    VoiceServiceConfiguration getConfiguration();
    
    /*
     * @returns The current agent status.
     */
    AgentStatus getAgentStatus();   
    
    /*
     * @returns The set of active calls.
     */
    Collection<VoiceInteraction> getVoiceInteractions();
    
    /*
     * This method connects to TServer and registers the Dn.
     */
    void connect() throws VoiceServiceException;
    
    /*
     * This method disconnects from TServer and cleans up resources.
     */
    void disconnect() throws VoiceServiceException;
    
    /*
     * This method logs in the agent using the information provided in the
     * VoiceServiceConfiguration.
     */
    void login() throws VoiceServiceException;
    
    /*
     * This method logs out the agent.
     */
    void logout() throws VoiceServiceException;
    
    /*
     * This method makes the agent ready.
     */
    void ready() throws VoiceServiceException;
    
    /*
     * This method sets the agent not ready.
     */
    void notReady() throws VoiceServiceException;
    
    /*
     * This method dials a new call.
     * 
     * @param otherDn The destination DN for the call.
     */
    void dial(String otherDn) throws VoiceServiceException;
    
    /*
     * This method answers an existing call.
     * 
     * @param connId The connectionId of the call to be answered.
     */
    void answer(String connId) throws VoiceServiceException;
    
    /*
     * This method puts an existing call on hold.
     * 
     * @param connId The connectionid of the call to be put on hold.
     */
    void hold(String connId) throws VoiceServiceException;
    
    /*
     * This method retrieves a held call.
     * 
     * @param connId The connectionId of the call to be retrieved.
     */
    void retrieve(String connId) throws VoiceServiceException;
    
    /*
     * This method releases the specified call.
     * 
     * @params connId The connectionId of the call to be released.
     */
    void release(String connId) throws VoiceServiceException;
    
    /*
     * This method initiates a transfer to the specified destination.
     * 
     * @param connId The connectionId of the call to be transfered.
     * @param otherDn The transfer destination.
     */
    void initiateTransfer(String connId, String otherDn) throws VoiceServiceException;
    
    /*
     * This method completes a transfer.
     * 
     * @param connId The connectionId of the call to be transfered.
     * @param transferConnId The transfer connectionId of the call to be transfered.
     */
    void completeTransfer(String connId, String transferConnId) throws VoiceServiceException;
    
    /*
     * This method initiates a conference to the specified destination.
     * 
     * @param connId The connectionId of the call to be conferenced.
     * @param otherDn The destination.
     */
    void initiateConference(String connId, String otherDn) throws VoiceServiceException;
    
    /*
     * This method completes a transfer.
     * 
     * @param connId The connectionId of the call to be transfered.
     * @param conferenceConnId The conferenece connectionId of the call to be transfered.
     */
    void completeConference(String connId, String conferenceConnId) throws VoiceServiceException;
}
