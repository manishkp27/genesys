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

/**
 * This listener provides visibility into state changes and events
 * in the VoiceService component.
 * 
 * @author <a href="mailto:Chris.Munn@genesyslab.com">Chris Munn</a>
 */
public interface VoiceServiceListener {
    
    /*
     * This handler is called when the connection status is updated.
     */
    void onConnectionStatusChanged(ConnectionStatus status);
    /*
     * This handler is called when the agent status changes.
     */
    void onAgentStatusChanged(AgentStatus status);
    /*
     * This handler is called when a new interaction has been created.
     */
    void onInteractionAdded(VoiceInteraction ixn);
    /*
     * This handler is called when an interaction has been updated.
     */
    void onInteractionUpdated(VoiceInteraction ixn);
    /*
     * This handler is called when an interaction has been removed.
     */
    void onInteractionRemoved(VoiceInteraction ixn);     
    /*
     * This handler is called when an error is encountered.
     */
    void onError(String message);
}
