/*
 * Copyright 2009-2011 Amazon Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *    http://aws.amazon.com/apache2.0
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.eclipse.ec2.ui.views.instances;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.ec2.Ec2ClientFactory;
import com.amazonaws.eclipse.ec2.Ec2Plugin;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.StartInstancesRequest;

/**
 * Action to start a stopped EBS-backed instance.
 */
public class StartInstancesAction extends Action {

    private final InstanceSelectionTable instanceSelectionTable;

    /** A shared client factory */
    private final static Ec2ClientFactory clientFactory = new Ec2ClientFactory();

    /**
     * Creates a new action which, when run, will start the instances given
     * 
     * @param instance
     *            The instances to start.
     * @param volume
     *            The volume to attach.
     */
    public StartInstancesAction(InstanceSelectionTable instanceSelectionTable) {
        this.instanceSelectionTable = instanceSelectionTable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#isEnabled()
     */
    @Override
    public boolean isEnabled() {
        for ( Instance instance : instanceSelectionTable.getAllSelectedInstances() ) {
            if ( !instance.getState().getName().equalsIgnoreCase("stopped") )
                return false;
        }
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {

        final List<String> instanceIds = new ArrayList<String>();
        for ( Instance instance : instanceSelectionTable.getAllSelectedInstances() ) {
            instanceIds.add(instance.getInstanceId());
        }
        
        new Thread() {
            public void run() {
                try {
                    StartInstancesRequest request = new StartInstancesRequest().withInstanceIds(instanceIds);
                    clientFactory.getAwsClient().startInstances(request);
                    instanceSelectionTable.refreshInstances();
                } catch ( Exception e ) {
                    Status status = new Status(IStatus.ERROR, Ec2Plugin.PLUGIN_ID, "Unable to start instances: "
                            + e.getMessage());
                    StatusManager.getManager().handle(status, StatusManager.SHOW | StatusManager.LOG);
                }
            }
        }.start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#getText()
     */
    @Override
    public String getText() {
        return "Start instances";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#getImageDescriptor()
     */
    @Override
    public ImageDescriptor getImageDescriptor() {
        return Ec2Plugin.getDefault().getImageRegistry().getDescriptor("start");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#getToolTipText()
     */
    @Override
    public String getToolTipText() {
        return "Start this instance";
    }

}
