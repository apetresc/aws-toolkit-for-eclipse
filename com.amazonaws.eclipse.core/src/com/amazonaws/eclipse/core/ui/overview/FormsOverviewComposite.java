/*
 * Copyright 2010-2011 Amazon Technologies, Inc.
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
package com.amazonaws.eclipse.core.ui.overview;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;
import org.eclipse.ui.statushandlers.StatusManager;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.AwsUrls;

/**
 * Main composite displaying the AWS Toolkit for Eclipse overview page. This is
 * the top level composite, responsible for allocating shared resources through
 * the OverviewResources object, sharing those resources with all subcomponents,
 * and eventually disposing shared resources along with all subcomponents when
 * this composite is disposed.
 */
class FormsOverviewComposite extends Composite {

    /** Preferred display order for overview sections */
    private static final String[] PLUGIN_ORDER = new String[] {
                    "com.amazonaws.eclipse.sdk.ui",
                    "com.amazonaws.eclipse.elasticbeanstalk",
                    "com.amazonaws.eclipse.ec2",
                    "com.amazonaws.eclipse.datatools.enablement.simpledb.ui"
            };

    /** The main form displayed in the overview page */
    private ScrolledForm form;

    /** The getting started section of the overview page */
    private GettingStartedSection gettingStartedSection;

    /** The shared resources for all overview page components */
    private OverviewResources resources;


    /**
     * Constructs a new AWS Toolkit for Eclipse overview composite, allocating
     * all shared resources. This class is not intended to be have more than one
     * instance instantiated at a time.
     *
     * @param parent
     *            The parent composite in which to create the new overview page.
     */
    public FormsOverviewComposite(Composite parent) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());
        resources = new OverviewResources();

        // Create the main form
        form = resources.getFormToolkit().createScrolledForm(this);
        TableWrapLayout tableWrapLayout = LayoutUtils.newSlimTableWrapLayout(2);
        tableWrapLayout.verticalSpacing = 20;
        tableWrapLayout.horizontalSpacing = 15;
        form.getBody().setLayout(tableWrapLayout);

        // Header
        Composite headerComposite = new HeaderComposite(form.getBody(), resources);
        TableWrapData tableWrapData = new TableWrapData(TableWrapData.FILL_GRAB);
        tableWrapData.colspan = 2;
        headerComposite.setLayoutData(tableWrapData);

        // Left hand column - Get Started and Resources sections
        Composite leftHandColumn = resources.getFormToolkit().createComposite(form.getBody());
        TableWrapLayout leftHandColumnLayout = new TableWrapLayout();
        leftHandColumnLayout.verticalSpacing = 20;
        leftHandColumn.setLayout(leftHandColumnLayout);
        leftHandColumn.setLayoutData(new TableWrapData(TableWrapData.FILL));
        gettingStartedSection = new GettingStartedSection(leftHandColumn, resources);
        gettingStartedSection.setLayoutData(new TableWrapData(TableWrapData.FILL));
        createResourcesSection(leftHandColumn)
            .setLayoutData(new TableWrapData(TableWrapData.FILL));

        // Right hand column - plugin contributed sections
        Composite rightHandColumn = resources.getFormToolkit().createComposite(form.getBody());
        TableWrapLayout rightHandColumnLayout = new TableWrapLayout();
        rightHandColumnLayout.verticalSpacing = 20;
        rightHandColumn.setLayout(rightHandColumnLayout);
        rightHandColumn.setLayoutData(new TableWrapData(TableWrapData.FILL_GRAB));
        createContributedOverviewSections(rightHandColumn);
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Composite#setFocus()
     */
    @Override
    public boolean setFocus() {
        return form.setFocus();
    }

    /* (non-Javadoc)
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        gettingStartedSection.dispose();
        resources.dispose();
        super.dispose();
    }


    /*
     * Private Interface
     */

    /**
     * Creates the Resources section, with links to general, external resources,
     * in the specified parent composite.
     *
     * @param parent
     *            The parent composite in which to create the new UI widgets.
     *
     * @return The new composite containing the Resources section of the
     *         overview page.
     */
    private Composite createResourcesSection(Composite parent) {
        Toolkit overviewToolkit = new Toolkit();
        overviewToolkit.setResources(resources);

        Composite composite = overviewToolkit.newSubSection(parent,
                "Resources", resources.getColor("amazon-orange"), resources.getFont("resources-header"));
        composite.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
        overviewToolkit.newListItem(composite,
                "AWS Toolkit for Eclipse Homepage",
                AwsUrls.AWS_TOOLKIT_FOR_ECLIPSE_HOMEPAGE_URL, null);
        overviewToolkit.newListItem(composite,
                "AWS Java Development Forum",
                AwsUrls.JAVA_DEVELOPMENT_FORUM_URL, null);
        overviewToolkit.newListItem(composite,
                "Frequently Asked Questions",
                AwsUrls.AWS_TOOLKIT_FOR_ECLIPSE_FAQ_URL, null);
        overviewToolkit.newListItem(composite,
                "AWS Toolkit for Eclipse Source Code",
                AwsUrls.AWS_TOOLKIT_FOR_ECLIPSE_SOURCEFORGE_URL, null);
        overviewToolkit.newListItem(composite,
                "AWS Management Console",
                AwsUrls.AWS_MANAGEMENT_CONSOLE_URL, null);

        return composite;
    }

    /**
     * Simple data container for the information about an OverviewSection
     * contributor.
     */
    private class OverviewContribution {
        final String title;
        final OverviewSection overviewSection;

        OverviewContribution(String title, OverviewSection overviewSection) {
            this.title = title;
            this.overviewSection = overviewSection;
        }
    }

    /**
     * Loads the overview contribution data from the plugins that contribute
     * overview sections, instantiates the OverviewSection subclass, and sorts
     * the returned records in the preferred display order.
     *
     * @return The sorted OverviewContribution objects based on what AWS Toolkit
     *         plugins are installed and are supplying overview section
     *         implementations.
     */
    private List<OverviewContribution> loadOverviewContributions() {
        Map<String, OverviewContribution> overviewContributionsByPluginId = new HashMap<String, OverviewContribution>();

        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(AwsToolkitCore.OVERVIEW_EXTENSION_ID);
        for (IExtension extension : extensionPoint.getExtensions()) {
            for (IConfigurationElement configurationElement : extension.getConfigurationElements()) {
                String contributorName = configurationElement.getContributor().getName();

                try {
                    OverviewContribution overviewContribution = new OverviewContribution(
                            configurationElement.getAttribute("title"),
                            (OverviewSection)configurationElement.createExecutableExtension("class"));

                    overviewContributionsByPluginId.put(contributorName, overviewContribution);
                } catch (CoreException e) {
                    String errorMessage = "Unable to create AWS Toolkit Overview section for: " + contributorName;
                    Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
                    StatusManager.getManager().handle(status, StatusManager.LOG);
                }
            }
        }

        ArrayList<OverviewContribution> overviewContributions = new ArrayList<OverviewContribution>();
        for (String pluginId : PLUGIN_ORDER) {
            if (overviewContributionsByPluginId.containsKey(pluginId)) {
                overviewContributions.add(overviewContributionsByPluginId.remove(pluginId));
            }
        }
        overviewContributions.addAll(overviewContributionsByPluginId.values());

        return overviewContributions;
    }

    /**
     * Creates all the contributed overview sections from all the installed AWS
     * Toolkit plugins that provide OverviewSection contributions.
     *
     * @param parent
     *            The parent composite in which to create the new contributed
     *            overview sections.
     */
    private void createContributedOverviewSections(Composite parent) {
        for (OverviewContribution overviewContribution : loadOverviewContributions()) {
            OverviewSection overviewSection = overviewContribution.overviewSection;

            Composite composite = createContributedOverviewSection(parent, overviewContribution.title);
            if (overviewSection instanceof OverviewSection.V2) {
                overviewSection.setResources(resources);
                composite.setLayout(newV2SectionLayout());
            } else {
                composite.setLayout(newV1SectionLayout());
            }

            try {
                overviewSection.createControls(composite);
            } catch (Exception e) {
                String errorMessage =
                    "Unable to create AWS Toolkit Overview section for " + overviewContribution.title;
                Status status = new Status(Status.ERROR, AwsToolkitCore.PLUGIN_ID, errorMessage, e);
                StatusManager.getManager().handle(status, StatusManager.LOG);
            }
        }
    }

    /**
     * Creates a new, empty overview section in the given parent composite with
     * the specified name, ready for a contributed OverviewSection to fill in
     * its controls in the returned composite.
     *
     * @param parent
     *            The parent composite in which to create the new section.
     * @param name
     *            The title for the new section.
     *
     * @return The new, empty composite in the new section, ready for the
     *         contributed OverviewSection to fill in its controls.
     */
    private Composite createContributedOverviewSection(Composite parent, String name) {
        Section section = resources.getFormToolkit().createSection(parent,
                Section.CLIENT_INDENT | Section.TITLE_BAR | Section.TWISTIE | Section.EXPANDED);
        section.setText(name);
        TableWrapData tableWrapData = new TableWrapData(TableWrapData.FILL, TableWrapData.FILL);
        tableWrapData.grabHorizontal = true;
        tableWrapData.grabVertical = true;
        section.setLayoutData(tableWrapData);

        section.setFont(resources.getFont("module-header"));
        section.setForeground(resources.getColor("module-header"));
        section.setTitleBarForeground(resources.getColor("module-header"));

        Composite composite = resources.getFormToolkit().createComposite(section);
        tableWrapData = new TableWrapData(TableWrapData.FILL, TableWrapData.FILL);
        tableWrapData.grabHorizontal = true;
        tableWrapData.grabVertical = true;
        composite.setLayoutData(tableWrapData);
        section.setClient(composite);

        return composite;
    }

    /**
     * Creates a new layout for the composite containing a V1 OverviewSection.
     * V1 OverviewSections use 2 column GridLayouts, as opposed to V2
     * OverviewSections which use 1 column TableWrapLayouts.
     *
     * @return A new GridLayout suitable for containing a V1 OverviewSection's
     *         controls.
     */
    private GridLayout newV1SectionLayout() {
        return new GridLayout(2, false);
    }

    /**
     * Creates a new layout for the composite containing a V2 OverviewSection.
     * V2 OverviewSections use 1 column TableWrapLayouts, as opposed to the
     * older 2 column grid layouts required by V1 OverviewSections.
     *
     * @return A new TableWrapLayout suitable for containing a V2
     *         OverviewSection's controls.
     */
    private TableWrapLayout newV2SectionLayout() {
        TableWrapLayout layout = new TableWrapLayout();
        layout.bottomMargin = 0;
        layout.topMargin = 0;
        return layout;
    }

}
