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
package services.voice.impl;

import java.util.Map;

import com.genesyslab.cst.softphone.services.voice.InteractionStatus;
import com.genesyslab.cst.softphone.services.voice.VoiceInteraction;

/**
 * This interface defines storage of active call details.
 * 
 * TODO: Track other call attributes like ANI, DNIS, parties, party roles,
 * call type, etc.
 * 
 * @author <a href="mailto:Chris.Munn@genesyslab.com">Chris Munn</a>
 */
public class VoiceInteractionImpl implements VoiceInteraction {

    /*
     * Constructor.
     * 
     * @param connId The connection id of the call.
     * @param thisDn AttributeThisDN from the call.
     * @param otherDn AttributeOtherDN from the call.
     * @param The type of the call.
     * @param AttributeDNIS from the call.
     * @param AttributeANI from the call.
     * @param status The initial status of the call (ringing or dialing)
     * @param attachedData Attached data.
     */
    public VoiceInteractionImpl(
            final String connId,
            final String thisDn,
            final String otherDn,
            final String callType,
            final String dnis,
            final String ani,
            final InteractionStatus status,
            final Map<String, String> attachedData) {
        
        if(connId == null || connId.isEmpty()) { 
            throw new IllegalArgumentException("connId is required.");
        }
        if(thisDn == null || thisDn.isEmpty()) {
            throw new IllegalArgumentException("thisDn is required.");
        }
        if(otherDn == null || otherDn.isEmpty()) { 
            throw new IllegalArgumentException("otherDn is required.");
        }
        if(callType == null || callType.isEmpty()) { 
            throw new IllegalArgumentException("callType is required.");
        }        
        
        this.connId = connId;
        this.thisDn = thisDn;
        this.otherDn = otherDn;
        this.callType = callType;
        this.dnis = dnis;
        this.ani = ani;
        this.status = status;
        this.attachedData = attachedData;
    }
    
    private String connId;
    private final String thisDn;
    private final String otherDn;
    private final String callType;
    private final String dnis;
    private final String ani;
    private InteractionStatus status;
    private String transferConnId;
    private Map<String, String> attachedData;
    
    /*
     * @returns The connection id of the call.
     */    
    public String getConnId() {
        return connId;
    }

    /*
     * @returns AttributeThisDN for the call.
     */    
    public String getThisDn() {
        return thisDn;
    }

    /*
     * @returns AttributeOtherDN for the call.
     */    
    public String getOtherDn() {
        return otherDn;
    }
    
    /*
     * @returns The type of the call.
     */
    public String getCallType() {
        return callType;
    }
    
    /*
     * @returns AttributeANI from the call.
     */
    public String getANI() {
        return ani;
    }
    
    /*
     * @returns AttributeDNIS from the call.
     */
    public String getDNIS() {
        return dnis;
    }

    /*
     * @returns The current status of the call.
     */    
    public InteractionStatus getStatus() {
        return status;
    }

    /*
     * @returns AttributeTransferConnID for the call.
     */    
    public String getTransferConnId() {
        return transferConnId;
    }
    
    /*
     * @returns A map that includes all the attached key value
     * pairs from the call that could be converted into strings.
     */
    public Map<String, String> getAttachedData() { 
        return attachedData;
    }
    
    /*
     * This method sets the connection id. This should only be used when
     * processing EventPartyChanged.
     * 
     * @param connId The new connid for the call.
     */
    public void setConnId(final String connId) {
        this.connId = connId;
    }
    
    /*
     * This method sets the tranfer conn id for the call.
     * 
     * @param transferConnId AttributeTransferConnID from the call.
     */
    public void setTransferConnId(final String transferConnId) {
        this.transferConnId = transferConnId;
    }    
    
    /*
     * This method updates the status of the call.
     * 
     * @param status The new status.
     */
    public void setInteractionStatus(final InteractionStatus status) {
        this.status = status;
    }
    
    /*
     * This method updates the attached data.
     * 
     * @param attachedData The updated attached data.
     */
    public void setAttachedData(final Map<String, String> attachedData) {
        this.attachedData = attachedData;
    }
    
    /*
     * This override provides a minimal set of information about the call for
     * display purposes.
     */
    @Override
    public String toString() {
        return connId + " [" + callType + ":" + otherDn + "] - " + status.toString();
    }
}
