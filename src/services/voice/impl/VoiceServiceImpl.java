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
package com.genesyslab.cst.softphone.services.voice.impl;

import com.genesyslab.cst.softphone.common.LogProvider;
import com.genesyslab.cst.softphone.services.voice.AgentStatus;
import com.genesyslab.cst.softphone.services.ConnectionStatus;
import com.genesyslab.cst.softphone.services.voice.InteractionStatus;
import com.genesyslab.cst.softphone.services.voice.VoiceInteraction;
import com.genesyslab.cst.softphone.services.voice.VoiceService;
import com.genesyslab.cst.softphone.services.voice.VoiceServiceConfiguration;
import com.genesyslab.cst.softphone.services.voice.VoiceServiceException;
import com.genesyslab.cst.softphone.services.voice.VoiceServiceListener;
import com.genesyslab.platform.applicationblocks.commons.Predicate;
import com.genesyslab.platform.applicationblocks.commons.broker.EventReceivingBrokerService;
import com.genesyslab.platform.applicationblocks.commons.broker.Subscriber;
import com.genesyslab.platform.applicationblocks.commons.protocols.FaultToleranceMode;
import com.genesyslab.platform.applicationblocks.commons.protocols.ProtocolManagementServiceImpl;
import com.genesyslab.platform.applicationblocks.commons.protocols.TServerConfiguration;
import com.genesyslab.platform.commons.collections.KeyValueCollection;
import com.genesyslab.platform.commons.collections.KeyValuePair;
import com.genesyslab.platform.commons.collections.ValueType;
import com.genesyslab.platform.commons.log.ILogger;
import com.genesyslab.platform.commons.protocol.ChannelClosedEvent;
import com.genesyslab.platform.commons.protocol.ChannelErrorEvent;
import com.genesyslab.platform.commons.protocol.ChannelListener;
import com.genesyslab.platform.commons.protocol.ChannelState;
import com.genesyslab.platform.commons.protocol.Message;
import com.genesyslab.platform.commons.protocol.ProtocolException;
import com.genesyslab.platform.commons.threading.AsyncInvoker;
import com.genesyslab.platform.commons.threading.SingleThreadInvoker;
import com.genesyslab.platform.logging.runtime.LoggerException;
import com.genesyslab.platform.voice.protocol.ConnectionId;
import com.genesyslab.platform.voice.protocol.tserver.AddressType;
import com.genesyslab.platform.voice.protocol.tserver.AgentWorkMode;
import com.genesyslab.platform.voice.protocol.tserver.ControlMode;
import com.genesyslab.platform.voice.protocol.tserver.MakeCallType;
import com.genesyslab.platform.voice.protocol.tserver.RegisterMode;
import com.genesyslab.platform.voice.protocol.tserver.events.EventAbandoned;
import com.genesyslab.platform.voice.protocol.tserver.events.EventAgentLogin;
import com.genesyslab.platform.voice.protocol.tserver.events.EventAgentLogout;
import com.genesyslab.platform.voice.protocol.tserver.events.EventAgentNotReady;
import com.genesyslab.platform.voice.protocol.tserver.events.EventAgentReady;
import com.genesyslab.platform.voice.protocol.tserver.events.EventAttachedDataChanged;
import com.genesyslab.platform.voice.protocol.tserver.events.EventDestinationBusy;
import com.genesyslab.platform.voice.protocol.tserver.events.EventDialing;
import com.genesyslab.platform.voice.protocol.tserver.events.EventError;
import com.genesyslab.platform.voice.protocol.tserver.events.EventEstablished;
import com.genesyslab.platform.voice.protocol.tserver.events.EventHeld;
import com.genesyslab.platform.voice.protocol.tserver.events.EventPartyChanged;
import com.genesyslab.platform.voice.protocol.tserver.events.EventRegistered;
import com.genesyslab.platform.voice.protocol.tserver.events.EventReleased;
import com.genesyslab.platform.voice.protocol.tserver.events.EventRetrieved;
import com.genesyslab.platform.voice.protocol.tserver.events.EventRinging;
import com.genesyslab.platform.voice.protocol.tserver.requests.agent.RequestAgentLogin;
import com.genesyslab.platform.voice.protocol.tserver.requests.agent.RequestAgentLogout;
import com.genesyslab.platform.voice.protocol.tserver.requests.agent.RequestAgentNotReady;
import com.genesyslab.platform.voice.protocol.tserver.requests.agent.RequestAgentReady;
import com.genesyslab.platform.voice.protocol.tserver.requests.dn.RequestRegisterAddress;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestAnswerCall;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestCompleteConference;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestCompleteTransfer;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestHoldCall;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestInitiateConference;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestInitiateTransfer;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestMakeCall;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestReleaseCall;
import com.genesyslab.platform.voice.protocol.tserver.requests.party.RequestRetrieveCall;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This interface defines the set of voice operations used by the softphone. By
 * introducing this interface, the rest of the application is no longer dependent 
 * on the Voice PSDK. It also provides an opportunity to test the service without
 * the GUI portion, as well as an opportunity for re-use.
 * 
 * TODO This design does not currently aid consumers in determining when it is appropriate (or not)
 * to call each of the methods. This is something that is can be very handy when programming the
 * enabled/disabled state of user interface elements. 
 * 
 * TODO This sample implementation does not currently implement any kind of lifecycle management. 
 * connect() may be called multiple times, or disconnect() called when it is not connected. Additional
 * checks for these case would be appropriate.
 * 
 * TODO There are a few internal operations in the service that content for use of shared state. These cases
 * need to be protected via locking. If a consumer of the service added a listener while one of the 
 * publish helpers was running, it would result in an error. In the specific, simple application of
 * this service demonstrated by the swing application, this is unlikely to occur and may be an acceptable risk.
 * If a component like this was to be shared amongst projects, it would be appropriate to audit and correct
 * these concurrency / reentrancy related issues.
 * 
 * @author <a href="mailto:Chris.Munn@genesyslab.com">Chris Munn</a>
 */
public class VoiceServiceImpl implements VoiceService, ChannelListener, Subscriber<Message> {

    /*
     * Constructor
     * 
     * @param config The configuration to be used by the service. This includes both
     * connection and agent login parameters.
     */
    public VoiceServiceImpl(final VoiceServiceConfiguration config)
            throws VoiceServiceException {
        if (config == null) {
            throw new IllegalArgumentException("config is required.");
        }

        this.config = config;
        dn = config.getDn();
        queue = config.getQueue();
        agentId = config.getAgentId();

        listeners = new ArrayList<>();
        interactions = new HashMap<>();

        try {
            logger = LogProvider.createLogger("VoiceService");
        } catch (LoggerException ex) {
            throw new VoiceServiceException(
                    "Failed to initialize logging.", ex);
        }

        //CM: This will be used to publish events without keeping the main
        //CM: event processing thread tied up.
        executor = Executors.newFixedThreadPool(1);
        
        logger.info("VoiceService v" + VERSION);
    }
    
    private static final String VERSION = "1.0.000.00";
    
    private final VoiceServiceConfiguration config;
    private final String dn;
    private final String queue;
    private final String agentId;
    private final List<VoiceServiceListener> listeners;
    private ConnectionStatus connectionStatus;
    private AgentStatus agentStatus;
    private Map<ConnectionId, VoiceInteraction> interactions;
    private final ILogger logger;
    private ProtocolManagementServiceImpl pmService;
    private EventReceivingBrokerService eventBroker;
    private AsyncInvoker invoker;
    private final ExecutorService executor;
    private static final String PROTOCOL_KEY = "TServer";
    private static final String CLIENT_NAME_PREFIX = "Softphone";
    private static int DEFAULT_ADDP_CLIENT_TIMEOUT = 30;
    private static int DEFAULT_ADDP_SERVER_TIMEOUT = 60;
    private static String DEFAULT_ADDP_TRACE_MODE = "none";

    /*
     * This method connects to TServer and registers the Dn. Consumers of this 
     * service must wait for notification via the listener that the connection 
     * status is "Connected" before calling other methods, otherwise an exception
     * will be raised.
     */
    public void connect() throws VoiceServiceException {
        try {
            
            if(pmService != null) { 
                throw new VoiceServiceException(
                        "Already connected.");                        
            }

            //CM: If no backup server was provided, we will set the backup
            //CM: to the same uri as the primary. This allows us to take advantage
            //CM: of the reconnection behavior of the WarmStandbyService even
            //CM: though there will actually be no failover.
            String backupUri = config.getBackupTServerUri();
            if (backupUri == null || backupUri.isEmpty()) {
                backupUri = config.getTServerUri();
            }

            //CM: New initialization syntax for PSDK 8.1
            invoker = new SingleThreadInvoker();
            eventBroker = new EventReceivingBrokerService(invoker);
            eventBroker.register(this);

            TServerConfiguration tsConf =
                    new TServerConfiguration(PROTOCOL_KEY);
            tsConf.setUri(new URI(config.getTServerUri()));
            tsConf.setWarmStandbyUri(new URI(backupUri));
            tsConf.setFaultTolerance(FaultToleranceMode.WarmStandby);
            tsConf.setClientName(CLIENT_NAME_PREFIX + "_" + dn);
            tsConf.setUseAddp(true);
            tsConf.setAddpClientTimeout(DEFAULT_ADDP_CLIENT_TIMEOUT);
            tsConf.setAddpServerTimeout(DEFAULT_ADDP_SERVER_TIMEOUT);
            tsConf.setAddpTrace(DEFAULT_ADDP_TRACE_MODE);

            pmService = new ProtocolManagementServiceImpl(eventBroker);
            pmService.addChannelListener(this);
            pmService.register(tsConf);

            logger.info("Connecting to TServer...");

            //CM: beginOpen must be used to ensure the WarmStandbyService is
            //CM: initialized and the primary/backup connection and failover 
            //CM: behavior is taken advantage of. The implication however, is
            //CM: that consumers of this service must wait for notification via
            //CM: the listener that the connection status is "Connected", otherwise            
            //CM: calling the majority of the service methods will result in an
            //CM: exception.
            pmService.beginOpen();

        } catch (ProtocolException|URISyntaxException ex) {
            throw new VoiceServiceException(
                    "Failed to initialize service.", ex);
        } 
    }

    /*
     * This method disconnects from TServer and cleans up resources. 
     */
    public void disconnect() throws VoiceServiceException {

        throwIfNotConnected();

        logger.info("Disconnecting from TServer...");

        //CM: There are fancier things that can be done here but this is
        //CM: the minimum for a clean shutdown and it will work properly in
        //CM: the majority of cases.
        try {
            pmService.getProtocol(PROTOCOL_KEY).close();
        } catch (ProtocolException|InterruptedException|IllegalStateException ex) {
            logger.warn(ex);
        } 

        pmService = null;
        eventBroker = null;
        invoker.dispose();
        invoker = null;
    }

    /*
     * This handler is run when the connection to TServer is opened. It updates 
     * the connection status and notifies any registered listeners, then sends
     * RequestRegisterAddress to TServer so we can start working with the DN.
     */
    public void onChannelOpened(final EventObject eo) {
        try {

            logger.info("TServer connection open.");

            connectionStatus = ConnectionStatus.Connected;
            publishConnectionStatusChanged();

            RequestRegisterAddress request = RequestRegisterAddress.create();
            request.setThisDN(dn);
            request.setAddressType(AddressType.DN);
            request.setControlMode(ControlMode.RegisterDefault);
            request.setRegisterMode(RegisterMode.ModeShare);

            logger.info("Sending: \n" + request);

            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (Exception ex) {
            logger.warn("Error in onChannelOpened.", ex);
        }
    }

    /*
     * This handler is run when the connection to TServer is closed. It updates 
     * the connection status and notifies any registered listeners.
     */
    public void onChannelClosed(final ChannelClosedEvent event) {

        logger.info("TServer connection closed.");

        connectionStatus = ConnectionStatus.Disconnected;
        publishConnectionStatusChanged();
    }

    /*
     * This handler is invoked when there is a protocol error. Details
     * of the error are published through the listener.
     */
    public void onChannelError(final ChannelErrorEvent event) {

        logger.info("Channel error encountered:\n" + event.getCause());

        publishError(event.toString());
    }

    /*
     * No filter is used. We want EventReceivingBrokerService to send us
     * everything.
     */
    public Predicate<Message> getFilter() {
        return null;
    }

    /*
     * This method dispatches incoming messages to their individual event
     * handlers.
     */
    public void handle(final Message message) {

        logger.info("Received:\n" + message);
        switch (message.messageId()) {
            case EventRegistered.ID:
                onEventRegistered(message);
                break;

            case EventAgentLogin.ID:
                onEventAgentLogin(message);
                break;

            case EventAgentLogout.ID:
                onEventAgentLogout(message);
                break;

            case EventAgentReady.ID:
                onEventAgentReady(message);
                break;

            case EventAgentNotReady.ID:
                onEventAgentNotReady(message);
                break;

            case EventRinging.ID:
                onEventRinging(message);
                break;

            case EventDialing.ID:
                onEventDialing(message);
                break;

            case EventEstablished.ID:
                onEventEstablished(message);
                break;

            case EventHeld.ID:
                onEventHeld(message);
                break;

            case EventRetrieved.ID:
                onEventRetrieved(message);
                break;

            case EventReleased.ID:
                onEventReleased(message);
                break;

            case EventAbandoned.ID:
                onEventAbandoned(message);
                break;

            case EventDestinationBusy.ID:
                onEventDestinationBusy(message);
                break;

            case EventPartyChanged.ID:
                onEventPartyChanged(message);
                break;

            case EventAttachedDataChanged.ID:
                onEventAttachedDataChanged(message);
                break;

            case EventError.ID:
                onEventError(message);
                break;
        }
    }

    /*
     * This helper checks to ensure the connection to TServer is open. If it
     * is not, an exception is thrown.
     */
    private void throwIfNotConnected() throws VoiceServiceException {
        if (pmService == null || 
            pmService.getProtocol(PROTOCOL_KEY).getState() != ChannelState.Opened) {
            throw new VoiceServiceException("Connection is not open.");
        }
    }

    /*
     * This helper publishes connection status changed notifications to registered
     * listeners using the executor.
     */
    private void publishConnectionStatusChanged() {
        executor.submit(new Runnable() {

            public void run() {
                final ConnectionStatus status = connectionStatus;
                for (VoiceServiceListener listener : listeners) {
                    try {
                        listener.onConnectionStatusChanged(status);
                    } catch (Exception ex) {
                        logger.warn("Listener onConnectionStatusChanged threw an exception.", ex);
                    }
                }
            }
        });
    }

    /*
     * This helper publishes an agent status changed notification to registered
     * listeners using the executor.
     */
    private void publishAgentStatusChanged() {

        executor.submit(new Runnable() {

            public void run() {
                final AgentStatus status = agentStatus;
                for (VoiceServiceListener listener : listeners) {
                    try {
                        listener.onAgentStatusChanged(status);
                    } catch (Exception ex) {
                        logger.warn("Listener onAgentStatusChanged threw an exception.", ex);
                    }
                }
            }
        });
    }

    /*
     * This helper publishes notification of a new interaction to registered
     * listeners via the executor.
     */
    private void publishInteractionAdded(final VoiceInteraction ixn) {
        executor.submit(new Runnable() {

            public void run() {
                for (VoiceServiceListener listener : listeners) {
                    try {
                        listener.onInteractionAdded(ixn);
                    } catch (Exception ex) {
                        logger.warn("Listener onInteractionAdded threw an exception.", ex);
                    }
                }
            }
        });
    }

    /*
     * This helper publishes a notification that the specified interaction has been
     * updated.
     */
    private void publishInteractionUpdated(final VoiceInteraction ixn) {
        executor.submit(new Runnable() {

            public void run() {
                for (VoiceServiceListener listener : listeners) {
                    try {
                        listener.onInteractionUpdated(ixn);
                    } catch (Exception ex) {
                        logger.warn("Listener onInteractionUpdated threw an exception.", ex);
                    }
                }
            }
        });
    }

    /*
     * This helper publishes a notification that the specified interaction has been
     * removed.
     */
    private void publishInteractionRemoved(final VoiceInteraction ixn) {
        executor.submit(new Runnable() {

            public void run() {
                for (VoiceServiceListener listener : listeners) {
                    try {
                        listener.onInteractionRemoved(ixn);
                    } catch (Exception ex) {
                        logger.warn("Listener onInteractionRemoved threw an exception.", ex);
                    }
                }
            }
        });
    }

    /*
     * This helper publishes notification of an error to registered listeners via
     * the executor.
     */
    private void publishError(final String error) {
        executor.submit(new Runnable() {

            public void run() {
                for (VoiceServiceListener listener : listeners) {
                    try {
                        listener.onError(error);
                    } catch (Exception ex) {
                        logger.warn("Listener onError threw an exception.", ex);
                    }
                }
            }
        });
    }

    /*
     * This helper convers the Voice PSDK KeyValueCollection representation of
     * attached data to a Map<String, String>. This allows us to keep the Voice PSDK
     * types isolated behind the service interface. 
     * 
     * TODO This could be enhanced to preserve the types of the values if there was a 
     * need to do something with them aside from display. If, for example, an integer was 
     * attached to the call, and the softphone was required to update that value, 
     * this would need to be changed (perhaps by accepting use of 
     * KeyValueCollection outside the facade.
     */
    private Map<String, String> convertAttachedData(final KeyValueCollection data) {
        Map<String, String> map = new HashMap<>();
        if (data != null) {
            for (Object o : data) {
                KeyValuePair pair = (KeyValuePair) o;

                ValueType type = pair.getValueType();
                Object value = pair.getValue();

                if (value != null && (type == ValueType.INT
                        || type == ValueType.FLOAT
                        || type == ValueType.LONG
                        || type == ValueType.STRING
                        || type == ValueType.WIDE_STRING)) {
                    map.put(pair.getStringKey(), value.toString());

                } else {
                    logger.warn("Skipping attached data pair with value type"
                            + pair.getValueType());
                }
            }
        }

        return map;
    }

    /*
     * This handler processes EventRegistered. 
     * 
     * TODO This should be enhanced to recover existing agent state and 
     * calls that are in progress. A design like this that has a specific agent id
     * provided up front would need to either log out an existing agent other than
     * the one configured, or gracefully fail to initialize in that case.
     */
    private void onEventRegistered(final Message message) {
        try {
            EventRegistered event = (EventRegistered) message;

        } catch (Exception ex) {
            logger.warn("Error in onEventRegistered.", ex);
        }
    }

    /*
     * This handler processes EventAgentLogin. It updates the agent status
     * and notifies registered listeners.
     */
    private void onEventAgentLogin(final Message message) {
        try {
            EventAgentLogin event = (EventAgentLogin) message;

            agentStatus = AgentStatus.LoggedIn;
            publishAgentStatusChanged();

        } catch (Exception ex) {
            logger.warn("Error in onEventAgentLogin.", ex);
        }
    }

    /*
     * This handler processes EventAgentLogout. It updates the agent status
     * and notifies registered listeners.
     */
    private void onEventAgentLogout(final Message message) {
        try {
            EventAgentLogout event = (EventAgentLogout) message;

            agentStatus = AgentStatus.LoggedOut;
            publishAgentStatusChanged();

        } catch (Exception ex) {
            logger.warn("Error in onEventAgentLogout.", ex);
        }
    }

    /*
     * This handler processes EventAgentReady. It updates the agent status
     * and notifies registered listeners.
     */
    private void onEventAgentReady(final Message message) {
        try {
            EventAgentReady event = (EventAgentReady) message;

            agentStatus = AgentStatus.Ready;
            publishAgentStatusChanged();

        } catch (Exception ex) {
            logger.warn("Error in onEventAgentReady.", ex);
        }
    }

    /* This handler processes EventAgentNotReady. It updates the agent status
     * and notifies registered listeners.
     * 
     * TODO: The existing AgentStatus enum does not differentiate between ACW
     * and AUX, nor does it consider reason codes, all of which are commonly desired.
     */
    private void onEventAgentNotReady(final Message message) {
        try {
            EventAgentNotReady event = (EventAgentNotReady) message;

            agentStatus = AgentStatus.NotReady;
            publishAgentStatusChanged();

        } catch (Exception ex) {
            logger.warn("Error in onEventAgentNotReady.", ex);
        }
    }

    /*
     * This handler processes EventRinging. A new voice interaction is
     * created to represent the call and a notification is sent to 
     * registered listeners.
     */
    private void onEventRinging(final Message message) {
        try {
            EventRinging event = (EventRinging) message;

            ConnectionId connId = event.getConnID();
            Map<String, String> attachedData =
                    convertAttachedData(event.getUserData());

            VoiceInteraction ixn =
                    new VoiceInteractionImpl(
                    connId.toString(),
                    dn,
                    event.getOtherDN(),
                    event.getCallType().toString(),
                    event.getDNIS(),
                    event.getANI(),
                    InteractionStatus.Ringing,
                    attachedData);
            interactions.put(connId, ixn);

            publishInteractionAdded(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventRinging.", ex);
        }
    }

    /*
     * This handler processes EventDialing. A new voice interaction is
     * created to represent the call and a notification is sent to 
     * registered listeners.
     */
    private void onEventDialing(final Message message) {
        try {
            EventDialing event = (EventDialing) message;

            ConnectionId connId = event.getConnID();
            Map<String, String> attachedData =
                    convertAttachedData(event.getUserData());

            VoiceInteractionImpl ixn =
                    new VoiceInteractionImpl(
                    connId.toString(),
                    dn,
                    event.getOtherDN(),
                    event.getCallType().toString(),
                    event.getDNIS(),
                    event.getANI(),
                    InteractionStatus.Dialing,
                    attachedData);

            ConnectionId transferConnId = event.getTransferConnID();
            if (transferConnId != null && interactions.containsKey(transferConnId)) {
                ixn.setTransferConnId(transferConnId.toString());
            }

            interactions.put(connId, ixn);
            publishInteractionAdded(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventDialing.", ex);
        }
    }

    /*
     * This handler processes EventEstablished. It looks for an
     * existing voice interaction object based on the connection id,
     * updates the status of the interaction, and notifies registered
     * listeners.
     * 
     * TODO: Attached data is updated only when voice interaction objects are
     * created and when EventAttachedDataChanged is processed. There are other
     * attributes that could be tracked that may need to be updated here.
     */
    private void onEventEstablished(final Message message) {
        try {
            EventEstablished event = (EventEstablished) message;

            ConnectionId connId = event.getConnID();
            if (!interactions.containsKey(connId)) {
                throw new VoiceServiceException(
                        "ConnId [" + connId + "] was not indexed.");
            }
            VoiceInteractionImpl ixn = (VoiceInteractionImpl) interactions.get(connId);
            ixn.setInteractionStatus(InteractionStatus.Established);

            publishInteractionUpdated(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventEstablished.", ex);
        }
    }

    /*
     * This handler processes EventHeld. It looks for an
     * existing voice interaction object based on the connection id,
     * updates the status of the interaction, and notifies registered
     * listeners.
     */
    private void onEventHeld(final Message message) {
        try {
            EventHeld event = (EventHeld) message;

            ConnectionId connId = event.getConnID();
            if (!interactions.containsKey(connId)) {
                throw new VoiceServiceException(
                        "ConnId [" + connId + "] was not indexed.");
            }
            VoiceInteractionImpl ixn = (VoiceInteractionImpl) interactions.get(connId);
            ixn.setInteractionStatus(InteractionStatus.Held);

            publishInteractionUpdated(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventHeld.", ex);
        }
    }

    /*
     * This handler processes EventRetrieved. It looks for an
     * existing voice interaction object based on the connection id,
     * updates the status of the interaction, and notifies registered
     * listeners.
     */
    private void onEventRetrieved(final Message message) {
        try {
            EventRetrieved event = (EventRetrieved) message;

            ConnectionId connId = event.getConnID();
            if (!interactions.containsKey(connId)) {
                throw new VoiceServiceException(
                        "ConnId [" + connId + "] was not indexed.");
            }
            VoiceInteractionImpl ixn = (VoiceInteractionImpl) interactions.get(connId);
            ixn.setInteractionStatus(InteractionStatus.Established);

            publishInteractionUpdated(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventRetrieved.", ex);
        }
    }

    /*
     * This handler processes EventReleased. It looks for
     * an existing voice interaction instance based on the
     * connection id and removed it from the index of active calls. 
     * After doing so a notification is published to registered listeners.
     */
    private void onEventReleased(final Message message) {
        try {
            EventReleased event = (EventReleased) message;

            ConnectionId connId = event.getConnID();
            if (!interactions.containsKey(connId)) {
                throw new VoiceServiceException(
                        "ConnId [" + connId + "] was not indexed.");
            }

            VoiceInteraction ixn = interactions.remove(connId);
            publishInteractionRemoved(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventReleased.", ex);
        }
    }

    /*
     * This handler processes EventAbandoned. It looks for
     * an existing voice interaction instance based on the
     * connection id and removed it from the index of active calls. 
     * After doing so a notification is published to registered listeners.
     */
    private void onEventAbandoned(final Message message) {
        try {
            EventAbandoned event = (EventAbandoned) message;

            ConnectionId connId = event.getConnID();
            if (!interactions.containsKey(connId)) {
                throw new VoiceServiceException(
                        "ConnId [" + connId + "] was not indexed.");
            }

            VoiceInteraction ixn = interactions.remove(connId);
            publishInteractionRemoved(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventAbandoned.", ex);
        }
    }

    /*
     * This handler processes EventDestinationBusy. It looks for
     * an existing voice interaction instance based on the
     * connection id and removed it from the index of active calls. 
     * After doing so a notification is published to registered listeners.
     */
    private void onEventDestinationBusy(final Message message) {
        try {
            EventDestinationBusy event = (EventDestinationBusy) message;

            ConnectionId connId = event.getConnID();
            if (!interactions.containsKey(connId)) {
                throw new VoiceServiceException(
                        "ConnId [" + connId + "] was not indexed.");
            }

            VoiceInteraction ixn = interactions.remove(connId);
            publishInteractionRemoved(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventDestinationBusy.", ex);
        }
    }

    /*
     * This handler processes EventPartyChanged. While this implementation is
     * not currently tracking and displaying call parties or their state, it is 
     * still important that we handle the case where we are the target of a 
     * transfer. In this case, when the transfer is completed, we receive
     * EventPartyChanged to update the connection id.
     * 
     * TODO: Handling of other party related events, tracking of call parties and
     * their state.
     */
    private void onEventPartyChanged(final Message message) {
        try {
            EventPartyChanged event = (EventPartyChanged) message;

            ConnectionId previousConnId = event.getPreviousConnID();

            if (previousConnId != null && interactions.containsKey(previousConnId)) {
                VoiceInteractionImpl ixn = (VoiceInteractionImpl) interactions.get(previousConnId);
                ixn.setConnId(event.getConnID().toString());
            }

        } catch (Exception ex) {
            logger.warn("Error in onEventPartyChanged.", ex);
        }
    }

    /*
     * This handler processes EventAttachedDataChanged. It looks for an 
     * existing voice interaction instance based on connection id and
     * updates the attached data.
     */
    private void onEventAttachedDataChanged(final Message message) {

        try {
            EventAttachedDataChanged event = (EventAttachedDataChanged) message;

            Map<String, String> attachedData =
                    convertAttachedData(event.getUserData());

            ConnectionId connId = event.getConnID();
            if (!interactions.containsKey(connId)) {
                throw new VoiceServiceException(
                        "ConnId [" + connId + "] was not indexed.");
            }
            VoiceInteractionImpl ixn = (VoiceInteractionImpl) interactions.get(connId);
            ixn.setAttachedData(attachedData);

            publishInteractionUpdated(ixn);

        } catch (Exception ex) {
            logger.warn("Error in onEventAttachedDataChanged.", ex);
        }
    }

    /*
     * This handler processes EventError. The details of the error are
     * published via the listener.
     */
    private void onEventError(final Message message) {
        try {
            EventError event = (EventError) message;
            publishError(event.toString());

        } catch (Exception ex) {
            logger.warn("Error in onEventError.", ex);
        }
    }

    /*
     * This method adds a new listener to be notified of state changes and events.
     * 
     * @param listener The listener to be added.
     */
    public void addListener(final VoiceServiceListener listener)
            throws VoiceServiceException {
        if (listener == null) {
            throw new IllegalArgumentException("listener is required.");
        }
        if (listeners.contains(listener)) {
            throw new VoiceServiceException("Listener already registered.");
        }

        listeners.add(listener);
    }

    /*
     * This method removes a previous added listener.
     * 
     * @param listener The listener to be removed.
     */
    public void removeListener(final VoiceServiceListener listener)
            throws VoiceServiceException {
        if (listener == null) {
            throw new IllegalArgumentException("listener is required.");
        }
        if (!listeners.contains(listener)) {
            throw new VoiceServiceException("Listener is not registered.");
        }

        listeners.remove(listener);
    }

    /*
     * @returns The connection status of the VoiceService.
     */
    public ConnectionStatus getConnectionStatus() {
        return connectionStatus;
    }

    /*
     * @returns The configuration used to contruct the VoiceService.
     */
    public VoiceServiceConfiguration getConfiguration() {
        return config;
    }

    /*
     * @returns The current agent status.
     */
    public AgentStatus getAgentStatus() {
        return agentStatus;
    }

    /*
     * @returns The set of active calls.
     */
    public Collection<VoiceInteraction> getVoiceInteractions() {
        return interactions.values();
    }

    /*
     * This method logs in the agent using the information provided in the
     * VoiceServiceConfiguration.
     */
    public void login() throws VoiceServiceException {

        throwIfNotConnected();

        RequestAgentLogin request = RequestAgentLogin.create();
        request.setThisDN(dn);
        request.setThisQueue(queue);
        request.setAgentID(agentId);
        request.setAgentWorkMode(AgentWorkMode.ManualIn);

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestAgentLogin.", ex);
        }
    }

    /*
     * This method logs out the agent.
     */
    public void logout() throws VoiceServiceException {

        throwIfNotConnected();

        RequestAgentLogout request = RequestAgentLogout.create();
        request.setThisDN(dn);
        request.setThisQueue(queue);

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestAgentLogout.", ex);
        }
    }

    /*
     * This method makes the agent ready.
     */
    public void ready() throws VoiceServiceException {

        throwIfNotConnected();

        RequestAgentReady request = RequestAgentReady.create();
        request.setThisDN(dn);
        request.setThisQueue(queue);

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestAgentReady.", ex);
        }
    }

    /*
     * This method sets the agent not ready.
     */
    public void notReady() throws VoiceServiceException {

        throwIfNotConnected();

        RequestAgentNotReady request = RequestAgentNotReady.create();
        request.setThisDN(dn);
        request.setThisQueue(queue);
        request.setAgentWorkMode(AgentWorkMode.AfterCallWork);

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send not RequestAgentNotReady.", ex);
        }
    }

    /*
     * This method dials a new call.
     * 
     * @param otherDn The destination DN for the call.
     */
    public void dial(final String otherDn) throws VoiceServiceException {

        throwIfNotConnected();

        RequestMakeCall request = RequestMakeCall.create();
        request.setThisDN(dn);
        request.setOtherDN(otherDn);
        request.setMakeCallType(MakeCallType.Regular);

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestMakeCall.", ex);
        }
    }

    /*
     * This method answers an existing call.
     * 
     * @param connId The connectionId of the call to be answered.
     */
    public void answer(final String connId) throws VoiceServiceException {

        throwIfNotConnected();

        RequestAnswerCall request = RequestAnswerCall.create();
        request.setThisDN(dn);
        request.setConnID(new ConnectionId(connId));

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestAnswerCall.", ex);
        }
    }

    /*
     * This method puts an existing call on hold.
     * 
     * @param connId The connectionid of the call to be put on hold.
     */
    public void hold(final String connId) throws VoiceServiceException {

        throwIfNotConnected();

        RequestHoldCall request = RequestHoldCall.create();
        request.setThisDN(dn);
        request.setConnID(new ConnectionId(connId));

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestHoldCall.", ex);
        }
    }

    /*
     * This method retrieves a held call.
     * 
     * @param connId The connectionId of the call to be retrieved.
     */
    public void retrieve(final String connId) throws VoiceServiceException {

        throwIfNotConnected();

        RequestRetrieveCall request = RequestRetrieveCall.create();
        request.setThisDN(dn);
        request.setConnID(new ConnectionId(connId));

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestRetrieveCall.", ex);
        }
    }

    /*
     * This method releases the specified call.
     * 
     * @params connId The connectionId of the call to be released.
     */
    public void release(final String connId)
            throws VoiceServiceException {

        throwIfNotConnected();

        RequestReleaseCall request = RequestReleaseCall.create();
        request.setThisDN(dn);
        request.setConnID(new ConnectionId(connId));

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestReleaseCall.", ex);
        }
    }

    /*
     * This method initiates a transfer to the specified destination.
     * 
     * @param connId The connectionId of the call to be transfered.
     * @param otherDn The transfer destination.
     */
    public void initiateTransfer(
            final String connId,
            final String otherDn) throws VoiceServiceException {

        throwIfNotConnected();

        RequestInitiateTransfer request = RequestInitiateTransfer.create();
        request.setThisDN(dn);
        request.setOtherDN(otherDn);
        request.setConnID(new ConnectionId(connId));

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestInitiateTransfer.", ex);
        }
    }

    /*
     * This method completes a transfer.
     * 
     * @param connId The connectionId of the call to be transfered.
     * @param transferConnId The transfer connectionId of the call to be transfered.
     */
    public void completeTransfer(
            final String connId,
            final String transferConnId) throws VoiceServiceException {

        throwIfNotConnected();

        RequestCompleteTransfer request = RequestCompleteTransfer.create();
        request.setThisDN(dn);
        request.setConnID(new ConnectionId(connId));
        request.setTransferConnID(new ConnectionId(transferConnId));

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestCompleteTransfer.", ex);
        }
    }

    /*
     * This method initiates a conference to the specified destination.
     * 
     * @param connId The connectionId of the call to be conferenced.
     * @param otherDn The destination.
     */
    public void initiateConference(
            final String connId,
            final String otherDn) throws VoiceServiceException {

        throwIfNotConnected();

        RequestInitiateConference request = RequestInitiateConference.create();
        request.setThisDN(dn);
        request.setConnID(new ConnectionId(connId));
        request.setOtherDN(otherDn);

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestInitiateConference.", ex);
        }
    }

    /*
     * This method completes a transfer.
     * 
     * @param connId The connectionId of the call to be transfered.
     * @param conferenceConnId The conferenece connectionId of the call to be transfered.
     */
    public void completeConference(
            final String connId,
            final String conferenceConnId) throws VoiceServiceException {

        throwIfNotConnected();

        RequestCompleteConference request = RequestCompleteConference.create();
        request.setThisDN(dn);
        request.setConnID(new ConnectionId(connId));
        request.setConferenceConnID(new ConnectionId(conferenceConnId));

        logger.info("Sending: \n" + request);

        try {
            pmService.getProtocol(PROTOCOL_KEY).send(request);
        } catch (ProtocolException ex) {
            throw new VoiceServiceException(
                    "Failed to send RequestCompleteConference.", ex);
        }
    }
}
