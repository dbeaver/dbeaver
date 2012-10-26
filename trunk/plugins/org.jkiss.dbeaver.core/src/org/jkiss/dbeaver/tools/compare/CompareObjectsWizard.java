/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.tools.compare;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.DBRProcessListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.properties.DataSourcePropertyFilter;
import org.jkiss.dbeaver.ui.properties.ObjectPropertyDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class CompareObjectsWizard extends Wizard implements IExportWizard {

    private static final String RS_COMPARE_WIZARD_DIALOG_SETTINGS = "CompareWizard";//$NON-NLS-1$

    private CompareObjectsSettings settings;
    private Map<DBPDataSource, IFilter> dataSourceFilters = new IdentityHashMap<DBPDataSource, IFilter>();
    private DBRProcessListener initializeFinisher;
    private volatile int initializedCount = 0;
    private volatile IStatus initializeError;

    public CompareObjectsWizard(List<DBNDatabaseNode> nodes)
    {
        this.settings = new CompareObjectsSettings(nodes);
        IDialogSettings section = UIUtils.getDialogSettings(RS_COMPARE_WIZARD_DIALOG_SETTINGS);
        setDialogSettings(section);

        settings.loadFrom(section);
        initializeFinisher = new DBRProcessListener() {
            @Override
            public void onProcessFinish(IStatus status)
            {
                if (!status.isOK()) {
                    initializeError = status;
                } else {
                    initializedCount++;
                }
            }
        };
    }

    public CompareObjectsSettings getSettings()
    {
        return settings;
    }

    @Override
    public void addPages()
    {
        super.addPages();
        addPage(new CompareObjectsPageSettings());
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection currentSelection)
    {
        setWindowTitle("Export data");
        setNeedsProgressMonitor(true);
    }

    private void showError(String error)
    {
        ((WizardPage)getContainer().getCurrentPage()).setErrorMessage(error);
    }

    @Override
    public boolean performFinish()
    {
        // Save settings
        getSettings().saveTo(getDialogSettings());
        dataSourceFilters.clear();
        showError(null);

        // Compare
        try {
            RuntimeUtils.run(getContainer(), true, true, new DBRRunnableWithProgress() {
                @Override
                public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
                {
                    try {
                        monitor.beginTask("Compare objects", 100);
                        compareNodes(monitor, getSettings().getNodes());
                        monitor.done();
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            if (initializeError != null) {
                showError(initializeError.getMessage());
            } else {
                showError(e.getTargetException().getMessage());
            }
            return false;
        } catch (InterruptedException e) {
            return false;
        }

        // Done
        return true;
    }

    private void compareNodes(DBRProgressMonitor monitor, List<DBNDatabaseNode> nodes)
        throws DBException, InterruptedException
    {
        StringBuilder title = new StringBuilder();
        // Initialize nodes
        {
            monitor.subTask("Initialize nodes");
            this.initializedCount = 0;
            this.initializeError = null;
            for (DBNDatabaseNode node : nodes) {
                if (title.length() > 0) title.append(", ");
                title.append(node.getNodeName());
                node.initializeNode(null, initializeFinisher);
                monitor.worked(1);
            }
            while (initializedCount != nodes.size()) {
                if (initializeError != null) {
                    throw new DBException(initializeError.getMessage());
                }
                Thread.sleep(100);
            }
        }

        monitor.subTask("Compare " + title.toString());
        DBNDatabaseNode firstNode = nodes.get(0);
        List<ObjectPropertyDescriptor> properties = ObjectPropertyDescriptor.extractAnnotations(null, firstNode.getObject().getClass(), getDataSourceFilter(firstNode));
        for (int i = 0; i < nodes.size(); i++) {
            monitor.subTask("Compare " + i);

            monitor.worked(1);
        }
    }


    private IFilter getDataSourceFilter(DBNDatabaseNode node)
    {
        DBPDataSource dataSource = node.getDataSourceContainer().getDataSource();
        if (dataSource == null) {
            return null;
        }
        IFilter filter = dataSourceFilters.get(dataSource);
        if (filter == null) {
            filter = new DataSourcePropertyFilter(dataSource);
            dataSourceFilters.put(dataSource, filter);
        }
        return filter;
    }

}