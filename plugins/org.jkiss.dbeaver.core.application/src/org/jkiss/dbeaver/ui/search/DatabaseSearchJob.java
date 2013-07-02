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
package org.jkiss.dbeaver.ui.search;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.HelpEnabledDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;

public class DatabaseSearchJob extends AbstractJob implements IObjectSearchListener {

    static final Log log = LogFactory.getLog(DatabaseSearchJob.class);

    private  IObjectSearchQuery query;
    private  IObjectSearchResultPage resultsPage;

    protected DatabaseSearchJob(IObjectSearchQuery query, IObjectSearchResultPage resultsPage)
    {
        super("Database search");
        setUser(true);

        this.query = query;
        this.resultsPage = resultsPage;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        try {
            query.runQuery(monitor, this);
        } catch (InvocationTargetException e) {
            return RuntimeUtils.makeExceptionStatus(e.getTargetException());
        } catch (InterruptedException e) {
            // ok
        }
        return Status.OK_STATUS;
    }

    @Override
    public void searchStarted()
    {

    }

    @Override
    public boolean objectsFound(final DBRProgressMonitor monitor, final Collection<?> objects)
    {
        UIUtils.runInUI(null, new Runnable() {
            @Override
            public void run()
            {
                resultsPage.populateObjects(monitor, objects);
            }
        });
        return true;
    }

    @Override
    public void searchFinished()
    {

    }
}
