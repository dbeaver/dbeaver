/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.search;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Collection;

public class DatabaseSearchJob extends AbstractJob implements IObjectSearchListener {

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
        } catch (DBException e) {
            return GeneralUtils.makeExceptionStatus(e);
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
