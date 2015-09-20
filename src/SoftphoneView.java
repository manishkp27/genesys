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
package com.genesyslab.cst.softphone;

import com.genesyslab.cst.softphone.services.voice.AgentStatus;
import com.genesyslab.cst.softphone.services.ConnectionStatus;
import com.genesyslab.cst.softphone.services.ServiceFactory;
import com.genesyslab.cst.softphone.services.voice.VoiceInteraction;
import com.genesyslab.cst.softphone.services.voice.VoiceService;
import com.genesyslab.cst.softphone.services.voice.VoiceServiceConfiguration;
import com.genesyslab.cst.softphone.services.voice.VoiceServiceException;
import com.genesyslab.cst.softphone.services.voice.VoiceServiceListener;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import org.jdesktop.application.Action;
import org.jdesktop.application.FrameView;
import org.jdesktop.application.SingleFrameApplication;

/**
 * This provides a rough example of how a user interface would use and consume the
 * VoiceService. 
 * 
 * TODO A proper implementation would include better error handling and reporting,
 * enable/disable buttons preventing the user from requesting invalid operations, and 
 * expand the set of interface elements to support additional features such as call
 * party tracking, attached data modification, etc. 
 * 
 * TODO I am not very familiar with Swing (at all) so there may be best practices that
 * should be employed to ensure a clean and easy to maintain design. In WPF, for example,
 * the MVVM pattern is a popular choice to provide these benefits as well as increased
 * testability, separation of concerns, etc. etc. Proper design exercises and due
 * diligence in research is assumed.
 * 
 * @author <a href="mailto:Chris.Munn@genesyslab.com">Chris Munn</a>
 */
public class SoftphoneView extends FrameView implements VoiceServiceListener, ListSelectionListener {

    /**
     * Constructor
     */
    public SoftphoneView(final SingleFrameApplication app) {
        super(app);
        
        buildUI();
        setComponent(mainPanel);
        
        redirectSystemStreams();
        System.out.println("Log initialized.");
    }
    private JPanel mainPanel;
    private JTable tblActiveCalls;
    private JTable tblAttachedData;    
    private JTextField txtTServerUri;
    private JTextField txtBackupTServerUri;
    private JTextField txtDn;
    private JTextField txtQueue;
    private JTextField txtAgentId;
    private JTextField txtConnectionStatus;
    private JTextField txtAgentStatus;
    private JTextArea txtLog;
    private VoiceService voiceService;    
    private CallTableModel callTableModel;
    private AttachedDataTableModel attachedDataTableModel;

    /*
     * This method implements a very clever override suggested in this
     * gentleman's blog:
     * 
     * http://unserializableone.blogspot.ca/2009/01/redirecting-systemout-and-systemerr-to.html
     * 
     * The purpose is to redirect stdout and stderr to the text area so it
     * can be seen at runtime without having to open a log file in a text
     * editor etc.
     */
    private void redirectSystemStreams() {
        OutputStream out = new OutputStream() {
            
            @Override
            public void write(int b) throws IOException {
                updateTextArea(String.valueOf((char) b));
            }
            
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                updateTextArea(new String(b, off, len));
            }
            
            @Override
            public void write(byte[] b) throws IOException {
                write(b, 0, b.length);
            }
        };
        
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    /*
     * This method updates the log text area with
     * the provided text.
     */
    private void updateTextArea(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
            
            public void run() {
                txtLog.append(text);
            }
        });
    }

    /*
     * This handler is run when the selected value changes in the active calls
     * table.
     */
    @Override
    public void valueChanged(ListSelectionEvent lse) {
        if (lse.getValueIsAdjusting()) {
            return;
        }
        
        Map<String, String> data = null;
        VoiceInteraction ixn = getSelectedCall();
        if (ixn != null) {
            data = ixn.getAttachedData();
        }
        
        attachedDataTableModel.setData(data);
    }

    /*
     * Table model for the active calls table.
     */
    private class CallTableModel extends AbstractTableModel {
        
        public CallTableModel() {
            calls = new ArrayList<>();
        }
        private final String[] columnNames = {
            "ConnectionId",
            "CallType",
            "OtherDN",
            "Status",
            "ANI",
            "DNIS"
        };        
        private final List<VoiceInteraction> calls;
        
        public int getColumnCount() {
            
            return columnNames.length;            
        }
        
        public int getRowCount() {
            return calls != null ? calls.size() : 0;
        }
        
        public String getColumnName(final int col) {
            return columnNames[col];
        }
        
        public Object getValueAt(
                final int row,
                final int col) {
            VoiceInteraction ixn = calls.get(row);            
            Object value = null;
            
            switch (col) {
                case 0:
                    value = ixn.getConnId();
                    break;
                case 1:
                    value = ixn.getCallType();
                    break;
                case 2:
                    value = ixn.getOtherDn();
                    break;
                case 3:
                    value = ixn.getStatus();
                    break;
                case 4:
                    value = ixn.getANI();
                    break;
                case 5:
                    value = ixn.getDNIS();
                    break;                
            }
            
            return value;
        }
        
        public VoiceInteraction getCall(final int row) {
            return calls.get(row);
        }
        
        public void addCall(final VoiceInteraction ixn) {
            if (calls.add(ixn)) {
                int idx = calls.indexOf(ixn);
                if (idx != -1) {
                    fireTableRowsInserted(idx, idx);
                }
            }            
        }
        
        public void removeCall(final VoiceInteraction ixn) {
            int idx = calls.indexOf(ixn);
            if (idx != -1) {
                calls.remove(idx);
                fireTableRowsDeleted(idx, idx);
            }
        }
        
        public void callUpdated(final VoiceInteraction ixn) {
            int idx = calls.indexOf(ixn);
            if (idx != -1) {
                fireTableRowsUpdated(idx, idx);
            }
        }        
        
        public void refresh() {
            this.fireTableDataChanged();
        }
    }

    /*
     * This helper returns the selected call, or null if there is
     * no call currently selected.
     * 
     * @returns The currently selected call, or null.
     */
    private VoiceInteraction getSelectedCall() {
        VoiceInteraction ixn = null;
        int row = tblActiveCalls.getSelectedRow();
        if (row != -1) {
            ixn = callTableModel.getCall(row);
        }
        
        return ixn;
    }

    /*
     * Model for the attached data table.
     */
    private class AttachedDataTableModel extends AbstractTableModel {

        private final String[] columnNames = {
            "Key",
            "Value"
        };
        private Map.Entry<String, String>[] data;
        
        public int getColumnCount() {
            return columnNames.length;            
        }
        
        public int getRowCount() {
            return data != null ? data.length : 0;
        }
        
        public String getColumnName(int col) {
            return columnNames[col];
        }
        
        public Object getValueAt(int row, int col) {
            
            Object value = null;
            if (col == 0) {
                value = data[row].getKey();
            } else if (col == 1) {
                value = data[row].getValue();
            }            
            
            return value;
        }
        
        public void setData(final Map<String, String> map) {
            if (map == null) {
                data = null;
            } else {                
                data = map.entrySet().toArray(new Map.Entry<>[0]);
            }
            
            fireTableDataChanged();
        }
    }

    /*
     * This handles connection status updates by updating the text field.
     */
    public void onConnectionStatusChanged(final ConnectionStatus status) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    
                    public void run() {
                        txtConnectionStatus.setText(status.toString());
                        if (status == ConnectionStatus.Disconnected) {
                            txtAgentStatus.setText("");
                        }
                    }
                });
    }

    /*
     * This handles agent status updates by updating the text field.
     */
    public void onAgentStatusChanged(final AgentStatus status) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    
                    public void run() {
                        txtAgentStatus.setText(status.toString());
                    }
                });
    }

    /*
     * This handles the new interaction notification by updating the
     * call table model. 
     */
    public void onInteractionAdded(final VoiceInteraction ixn) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    
                    public void run() {
                        callTableModel.addCall(ixn);
                        if(voiceService.getVoiceInteractions().size() == 1) {
                            tblActiveCalls.getSelectionModel().setSelectionInterval(0, 0);
                        }
                    }
                });
    }

    /*
     * This handles the interaction updated notification by updating
     * the call table model.
     */
    public void onInteractionUpdated(final VoiceInteraction ixn) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    
                    public void run() {
                        callTableModel.callUpdated(ixn);
                    }
                });
    }

    /*
     * This handles the interaction removed notification by updating the
     * call table model.
     */
    public void onInteractionRemoved(final VoiceInteraction ixn) {
        SwingUtilities.invokeLater(
                new Runnable() {
                    
                    public void run() {
                        callTableModel.removeCall(ixn);
                    }
                });
    }

    /*
     * This handler displays a dialog with the received message;
     */
    public void onError(final String message) {
        JOptionPane.showMessageDialog(
                null, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /*
     * This method connects to TServer.
     */
    @Action    
    public void connect() {
        try {
            String tserverUri = txtTServerUri.getText();
            String backupTServerUri = txtBackupTServerUri.getText();
            String dn = txtDn.getText();
            String queue = txtQueue.getText();
            String agentId = txtAgentId.getText();
            
            VoiceServiceConfiguration config =
                    new VoiceServiceConfiguration(
                    tserverUri,
                    backupTServerUri,
                    dn,
                    queue,
                    agentId);
            voiceService = ServiceFactory.createVoiceService(config);
            voiceService.addListener(this);
            voiceService.connect();
            
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method disconnects from TServer.
     */
    @Action
    public void disconnect() {
        try {
            if (voiceService != null) {
                voiceService.disconnect();
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method logs in the agent.     
     */
    @Action
    public void login() {
        try {
            if (voiceService != null) {
                voiceService.login();
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method logs out the agent.
     */
    @Action
    public void logout() {
        try {
            if (voiceService != null) {
                voiceService.logout();
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method makes the agent ready.
     */
    @Action
    public void ready() {
        try {
            if (voiceService != null) {
                voiceService.ready();
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method makes the agent not ready.
     */
    @Action
    public void notReady() {
        try {
            if (voiceService != null) {
                voiceService.notReady();
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method prompts for the destination and dials a new call.
     */
    @Action
    public void dial() {
        try {
            String destination = JOptionPane.showInputDialog(
                    "Enter destination:");
            if(destination != null && !destination.isEmpty()) {
                voiceService.dial(destination);
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method answers the selected call.
     */
    @Action
    public void answer() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            voiceService.answer(ixn.getConnId());
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
     * This method puts the selected call on hold.
     */
    @Action
    public void hold() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            voiceService.hold(ixn.getConnId());
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method retrieves the selected call.
     */
    @Action
    public void retrieve() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            voiceService.retrieve(ixn.getConnId());
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method releases the selected call.
     */
    @Action    
    public void release() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            voiceService.release(ixn.getConnId());
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method prompts for a destination and initiates a conference.
     */
    @Action
    public void initiateConference() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            
            String destination = JOptionPane.showInputDialog(
                    "Enter destination:");
            if(destination != null && !destination.isEmpty()) {            
                voiceService.initiateConference(ixn.getConnId(), destination);
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method completes a conference.
     */
    @Action
    public void completeConference() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            voiceService.completeConference(ixn.getConnId(), ixn.getTransferConnId());
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method prompts for a destination and initiates a transfer.
     */
    @Action
    public void initiateTransfer() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            
            String destination = JOptionPane.showInputDialog(
                    "Enter destination:");
            if(destination != null && !destination.isEmpty()) {                        
                voiceService.initiateTransfer(ixn.getConnId(), destination);
            }
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This method completes a transfer.
     */
    @Action
    public void completeTransfer() {
        try {
            VoiceInteraction ixn = getSelectedCall();
            voiceService.completeTransfer(ixn.getConnId(), ixn.getTransferConnId());
        } catch (VoiceServiceException ex) {
            JOptionPane.showMessageDialog(
                    null, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
        }        
    }

    /*
     * This helper builds the UI 
     */
    private void buildUI() {
        
        ActionMap actionMap = this.getContext().getActionMap(this);
        
        mainPanel = new JPanel();
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        GridBagLayout gbl_mainPanel = new GridBagLayout();
        gbl_mainPanel.columnWidths = new int[]{550, 400};
        gbl_mainPanel.rowHeights = new int[]{200, 0};
        gbl_mainPanel.columnWeights = new double[]{0, 1.0};
        gbl_mainPanel.rowWeights = new double[]{0.0, Double.MIN_VALUE};
        mainPanel.setLayout(gbl_mainPanel);
        
        JPanel softphonePanel = new JPanel();
        GridBagConstraints gbc_softphonePanel = new GridBagConstraints();
        gbc_softphonePanel.insets = new Insets(0, 0, 0, 5);
        gbc_softphonePanel.fill = GridBagConstraints.NONE;
        gbc_softphonePanel.gridx = 0;
        gbc_softphonePanel.gridy = 0;
        mainPanel.add(softphonePanel, gbc_softphonePanel);
        GridBagLayout gbl_softphonePanel = new GridBagLayout();
        gbl_softphonePanel.columnWidths = new int[]{450};
        gbl_softphonePanel.rowHeights = new int[]{100, 15, 120, 15, 15, 250};
        gbl_softphonePanel.columnWeights = new double[]{1.0};
        gbl_softphonePanel.rowWeights = new double[]{1.0, Double.MIN_VALUE, 1.0, Double.MIN_VALUE, Double.MIN_VALUE, 1.0};
        softphonePanel.setLayout(gbl_softphonePanel);
        
        JPanel cfgPanel = new JPanel();
        GridBagConstraints gbc_cfgPanel = new GridBagConstraints();
        gbc_cfgPanel.anchor = GridBagConstraints.WEST;
        gbc_cfgPanel.insets = new Insets(0, 0, 5, 0);
        gbc_cfgPanel.gridx = 0;
        gbc_cfgPanel.gridy = 0;
        softphonePanel.add(cfgPanel, gbc_cfgPanel);
        GridBagLayout gbl_cfgPanel = new GridBagLayout();
        gbl_cfgPanel.columnWidths = new int[]{120, 175, 120, 120};
        gbl_cfgPanel.rowHeights = new int[]{15, 15, 15, 15, 15};
        gbl_cfgPanel.columnWeights = new double[]{0.0, 1.0, 0.0, 0.0};
        gbl_cfgPanel.rowWeights = new double[]{0.0, 0.0, 0.0, 0.0, 0.0};
        
        cfgPanel.setLayout(gbl_cfgPanel);
        
        JLabel lblTserverUri = new JLabel("TServer Uri:");
        GridBagConstraints gbc_lblTserverUri = new GridBagConstraints();
        gbc_lblTserverUri.anchor = GridBagConstraints.WEST;
        gbc_lblTserverUri.insets = new Insets(0, 0, 5, 5);
        gbc_lblTserverUri.gridx = 0;
        gbc_lblTserverUri.gridy = 0;
        cfgPanel.add(lblTserverUri, gbc_lblTserverUri);
        
        txtTServerUri = new JTextField();
        txtTServerUri.setText("tcp://192.168.17.102:7035");
        GridBagConstraints gbc_txtTServerUri = new GridBagConstraints();
        gbc_txtTServerUri.insets = new Insets(0, 0, 5, 5);
        gbc_txtTServerUri.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtTServerUri.gridx = 1;
        gbc_txtTServerUri.gridy = 0;
        cfgPanel.add(txtTServerUri, gbc_txtTServerUri);
        
        JLabel lblBackupTserverUri = new JLabel("Backup TServer Uri:");
        GridBagConstraints gbc_lblBackupTServerUri = new GridBagConstraints();
        gbc_lblBackupTServerUri.anchor = GridBagConstraints.WEST;
        gbc_lblBackupTServerUri.insets = new Insets(0, 0, 5, 5);
        gbc_lblBackupTServerUri.gridx = 0;
        gbc_lblBackupTServerUri.gridy = 1;
        cfgPanel.add(lblBackupTserverUri, gbc_lblBackupTServerUri);
        
        txtBackupTServerUri = new JTextField();
        txtBackupTServerUri.setText("tcp://192.168.17.102:7035");
        GridBagConstraints gbc_txtBackupTServerUri = new GridBagConstraints();
        gbc_txtBackupTServerUri.insets = new Insets(0, 0, 5, 5);
        gbc_txtBackupTServerUri.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtBackupTServerUri.gridx = 1;
        gbc_txtBackupTServerUri.gridy = 1;
        cfgPanel.add(txtBackupTServerUri, gbc_txtBackupTServerUri);
        
        JLabel lblDn = new JLabel("DN:");
        GridBagConstraints gbc_lblDn = new GridBagConstraints();
        gbc_lblDn.anchor = GridBagConstraints.WEST;
        gbc_lblDn.insets = new Insets(0, 0, 5, 5);
        gbc_lblDn.gridx = 0;
        gbc_lblDn.gridy = 2;
        cfgPanel.add(lblDn, gbc_lblDn);
        
        txtDn = new JTextField();
        txtDn.setText("5000");
        GridBagConstraints gbc_txtDn = new GridBagConstraints();
        gbc_txtDn.insets = new Insets(0, 0, 5, 5);
        gbc_txtDn.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtDn.gridx = 1;
        gbc_txtDn.gridy = 2;
        cfgPanel.add(txtDn, gbc_txtDn);
        
        JLabel lblAgentId = new JLabel("Agent Id:");
        GridBagConstraints gbc_lblAgentId = new GridBagConstraints();
        gbc_lblAgentId.anchor = GridBagConstraints.WEST;
        gbc_lblAgentId.insets = new Insets(0, 0, 5, 5);
        gbc_lblAgentId.gridx = 0;
        gbc_lblAgentId.gridy = 3;
        cfgPanel.add(lblAgentId, gbc_lblAgentId);
        
        txtAgentId = new JTextField();
        txtAgentId.setText("7000");
        GridBagConstraints gbc_txtAgentId = new GridBagConstraints();
        gbc_txtAgentId.insets = new Insets(0, 0, 5, 5);
        gbc_txtAgentId.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtAgentId.gridx = 1;
        gbc_txtAgentId.gridy = 3;
        cfgPanel.add(txtAgentId, gbc_txtAgentId);
        
        JLabel lblQueue = new JLabel("Queue:");
        GridBagConstraints gbc_lblQueue = new GridBagConstraints();
        gbc_lblQueue.anchor = GridBagConstraints.WEST;
        gbc_lblQueue.insets = new Insets(0, 0, 5, 5);
        gbc_lblQueue.gridx = 0;
        gbc_lblQueue.gridy = 4;
        cfgPanel.add(lblQueue, gbc_lblQueue);
        
        txtQueue = new JTextField();
        txtQueue.setText("9000");
        GridBagConstraints gbc_txtQueue = new GridBagConstraints();
        gbc_txtQueue.insets = new Insets(0, 0, 5, 5);
        gbc_txtQueue.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtQueue.gridx = 1;
        gbc_txtQueue.gridy = 4;
        cfgPanel.add(txtQueue, gbc_txtQueue);
        
        JLabel lblConnectionStatus = new JLabel("Connection Status:");
        GridBagConstraints gbc_lblConnectionStatus = new GridBagConstraints();
        gbc_lblConnectionStatus.anchor = GridBagConstraints.WEST;
        gbc_lblConnectionStatus.insets = new Insets(0, 10, 5, 5);
        gbc_lblConnectionStatus.gridx = 2;
        gbc_lblConnectionStatus.gridy = 0;
        cfgPanel.add(lblConnectionStatus, gbc_lblConnectionStatus);
        
        txtConnectionStatus = new JTextField();
        txtConnectionStatus.setEditable(false);
        txtConnectionStatus.setText("Disconnected");
        GridBagConstraints gbc_txtConnectionStatus = new GridBagConstraints();
        gbc_txtConnectionStatus.insets = new Insets(0, 0, 5, 5);
        gbc_txtConnectionStatus.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtConnectionStatus.gridx = 3;
        gbc_txtConnectionStatus.gridy = 0;
        cfgPanel.add(txtConnectionStatus, gbc_txtConnectionStatus);
        
        JLabel lblAgentStatus = new JLabel("Agent Status:");
        GridBagConstraints gbc_lblAgentStatus = new GridBagConstraints();
        gbc_lblAgentStatus.anchor = GridBagConstraints.WEST;
        gbc_lblAgentStatus.insets = new Insets(0, 10, 5, 5);
        gbc_lblAgentStatus.gridx = 2;
        gbc_lblAgentStatus.gridy = 1;
        cfgPanel.add(lblAgentStatus, gbc_lblAgentStatus);
        
        txtAgentStatus = new JTextField();
        txtAgentStatus.setEditable(false);
        txtAgentStatus.setText("");
        GridBagConstraints gbc_txtAgentStatus = new GridBagConstraints();
        gbc_txtAgentStatus.insets = new Insets(0, 0, 5, 5);
        gbc_txtAgentStatus.fill = GridBagConstraints.HORIZONTAL;
        gbc_txtAgentStatus.gridx = 3;
        gbc_txtAgentStatus.gridy = 1;
        cfgPanel.add(txtAgentStatus, gbc_txtAgentStatus);
        
        JPanel dnButtonsPanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) dnButtonsPanel.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        GridBagConstraints gbc_dnButtonsPanel = new GridBagConstraints();
        gbc_dnButtonsPanel.fill = GridBagConstraints.HORIZONTAL;
        gbc_dnButtonsPanel.anchor = GridBagConstraints.WEST;
        gbc_dnButtonsPanel.insets = new Insets(0, 0, 0, 0);
        gbc_dnButtonsPanel.gridx = 0;
        gbc_dnButtonsPanel.gridy = 1;
        softphonePanel.add(dnButtonsPanel, gbc_dnButtonsPanel);
        
        JButton btnConnect = new JButton("Connect");
        btnConnect.setAction(actionMap.get("connect"));
        dnButtonsPanel.add(btnConnect);
        
        JButton btnDisconnect = new JButton("Disconnect");
        btnDisconnect.setAction(actionMap.get("disconnect"));
        dnButtonsPanel.add(btnDisconnect);
        
        JButton btnLogin = new JButton("Login");
        btnLogin.setAction(actionMap.get("login"));
        dnButtonsPanel.add(btnLogin);
        
        JButton btnLogout = new JButton("Logout");
        btnLogout.setAction(actionMap.get("logout"));
        dnButtonsPanel.add(btnLogout);
        
        JButton btnReady = new JButton("Ready");
        btnReady.setAction(actionMap.get("ready"));
        dnButtonsPanel.add(btnReady);
        
        JButton btnNotReady = new JButton("NotReady");
        btnNotReady.setAction(actionMap.get("notReady"));
        dnButtonsPanel.add(btnNotReady);
        
        JPanel activeCallsPanel = new JPanel();
        GridBagConstraints gbc_activeCallsPanel = new GridBagConstraints();
        gbc_activeCallsPanel.insets = new Insets(0, 0, 0, 0);
        gbc_activeCallsPanel.fill = GridBagConstraints.BOTH;
        gbc_activeCallsPanel.gridx = 0;
        gbc_activeCallsPanel.gridy = 2;
        softphonePanel.add(activeCallsPanel, gbc_activeCallsPanel);
        GridBagLayout gbl_activeCallsPanel = new GridBagLayout();
        gbl_activeCallsPanel.columnWidths = new int[]{0, 0};
        gbl_activeCallsPanel.rowHeights = new int[]{0, 0, 0};
        gbl_activeCallsPanel.columnWeights = new double[]{1.0, Double.MIN_VALUE};
        gbl_activeCallsPanel.rowWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
        activeCallsPanel.setLayout(gbl_activeCallsPanel);
        
        JLabel lblActiveCalls = new JLabel("Active Calls:");
        GridBagConstraints gbc_lblActiveCalls = new GridBagConstraints();
        gbc_lblActiveCalls.anchor = GridBagConstraints.WEST;
        gbc_lblActiveCalls.insets = new Insets(0, 0, 5, 0);
        gbc_lblActiveCalls.gridx = 0;
        gbc_lblActiveCalls.gridy = 0;
        activeCallsPanel.add(lblActiveCalls, gbc_lblActiveCalls);
        
        callTableModel = new CallTableModel();
        
        tblActiveCalls = new JTable(callTableModel);
        tblActiveCalls.setAutoCreateColumnsFromModel(true);
        tblActiveCalls.setFillsViewportHeight(true);
        tblActiveCalls.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblActiveCalls.getSelectionModel().addListSelectionListener(this);
        tblActiveCalls.getColumn("ConnectionId").setPreferredWidth(150);
        
        GridBagConstraints gbc_tblActiveCalls = new GridBagConstraints();
        gbc_tblActiveCalls.fill = GridBagConstraints.BOTH;
        gbc_tblActiveCalls.gridx = 0;
        gbc_tblActiveCalls.gridy = 1;
        
        JScrollPane callTableScollPane = new JScrollPane(tblActiveCalls);
        
        activeCallsPanel.add(callTableScollPane, gbc_tblActiveCalls);
        
        JPanel basicCallControlButtonsPanel = new JPanel();
        FlowLayout flowLayout_2 = (FlowLayout) basicCallControlButtonsPanel.getLayout();
        flowLayout_2.setAlignment(FlowLayout.LEFT);
        flowLayout_2.setVgap(0);
        GridBagConstraints gbc_basicCallControlButtonsPanel = new GridBagConstraints();
        gbc_basicCallControlButtonsPanel.anchor = GridBagConstraints.WEST;
        gbc_basicCallControlButtonsPanel.insets = new Insets(0, 0, 0, 0);
        gbc_basicCallControlButtonsPanel.gridx = 0;
        gbc_basicCallControlButtonsPanel.gridy = 3;
        softphonePanel.add(basicCallControlButtonsPanel, gbc_basicCallControlButtonsPanel);
        
        JButton btnDial = new JButton("Dial");
        btnDial.setAction(actionMap.get("dial"));
        basicCallControlButtonsPanel.add(btnDial);
        
        JButton btnAnswer = new JButton("Answer");
        btnAnswer.setAction(actionMap.get("answer"));
        basicCallControlButtonsPanel.add(btnAnswer);
        
        JButton btnHold = new JButton("Hold");
        btnHold.setAction(actionMap.get("hold"));
        basicCallControlButtonsPanel.add(btnHold);
        
        JButton btnRetrieve = new JButton("Retrieve");
        btnRetrieve.setAction(actionMap.get("retrieve"));
        basicCallControlButtonsPanel.add(btnRetrieve);
        
        JButton btnRelease = new JButton("Release");
        btnRelease.setAction(actionMap.get("release"));
        basicCallControlButtonsPanel.add(btnRelease);
        
        JPanel transferButtonsPanel = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) transferButtonsPanel.getLayout();
        flowLayout_1.setAlignment(FlowLayout.LEFT);
        flowLayout_1.setVgap(0);
        GridBagConstraints gbc_transferButtonsPanel = new GridBagConstraints();
        gbc_transferButtonsPanel.insets = new Insets(0, 0, 0, 0);
        gbc_transferButtonsPanel.anchor = GridBagConstraints.WEST;
        gbc_transferButtonsPanel.gridx = 0;
        gbc_transferButtonsPanel.gridy = 4;
        softphonePanel.add(transferButtonsPanel, gbc_transferButtonsPanel);
        
        JButton btnInitiateConference = new JButton("Initiate Conference");
        btnInitiateConference.setAction(actionMap.get("initiateConference"));
        transferButtonsPanel.add(btnInitiateConference);
        
        JButton btnCompleteConference = new JButton("Complete Conference");
        btnCompleteConference.setAction(actionMap.get("completeConference"));
        transferButtonsPanel.add(btnCompleteConference);
        
        JButton btnInitiateTransfer = new JButton("Initiate Transfer");
        btnInitiateTransfer.setAction(actionMap.get("initiateTransfer"));
        transferButtonsPanel.add(btnInitiateTransfer);
        
        JButton btnCompleteTransfer = new JButton("Complete Transfer");
        btnCompleteTransfer.setAction(actionMap.get("completeTransfer"));
        transferButtonsPanel.add(btnCompleteTransfer);
        
        JTabbedPane callDataTabs = new JTabbedPane(JTabbedPane.BOTTOM);
        GridBagConstraints gbc_callDataTabs = new GridBagConstraints();
        gbc_callDataTabs.fill = GridBagConstraints.BOTH;
        gbc_callDataTabs.gridx = 0;
        gbc_callDataTabs.gridy = 5;
        softphonePanel.add(callDataTabs, gbc_callDataTabs);
        
        attachedDataTableModel = new AttachedDataTableModel();
        tblAttachedData = new JTable(attachedDataTableModel);
        tblAttachedData.setFillsViewportHeight(true);        
        tblAttachedData.getColumn("Key").setPreferredWidth(250);
        tblAttachedData.getColumn("Value").setPreferredWidth(400);
        
        JScrollPane attachedDataScrollPane = new JScrollPane(tblAttachedData);        
        callDataTabs.addTab("Attached Data", null, attachedDataScrollPane, null);
                
        JPanel logPanel = new JPanel();
        GridBagConstraints gbc_logPanel = new GridBagConstraints();
        gbc_logPanel.fill = GridBagConstraints.BOTH;
        gbc_logPanel.gridx = 1;
        gbc_logPanel.gridy = 0;
        mainPanel.add(logPanel, gbc_logPanel);
        GridBagLayout gbl_logPanel = new GridBagLayout();
        gbl_logPanel.columnWidths = new int[]{0};
        gbl_logPanel.rowHeights = new int[]{0};
        gbl_logPanel.columnWeights = new double[]{1.0};
        gbl_logPanel.rowWeights = new double[]{1.0};
        logPanel.setLayout(gbl_logPanel);
        
        txtLog = new JTextArea();
        txtLog.setEditable(false);
        GridBagConstraints gbc_txtLog = new GridBagConstraints();
        gbc_txtLog.fill = GridBagConstraints.BOTH;
        gbc_txtLog.anchor = GridBagConstraints.NORTH;
        gbc_txtLog.gridx = 0;
        gbc_txtLog.gridy = 0;
        
        JScrollPane logScrollPane = new JScrollPane(txtLog);           
        logPanel.add(logScrollPane, gbc_txtLog);
    }    
}
