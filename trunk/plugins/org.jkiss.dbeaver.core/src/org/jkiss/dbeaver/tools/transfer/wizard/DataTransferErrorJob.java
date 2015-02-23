/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractUIJob;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * DataTransferErrorJob
 */
public class DataTransferErrorJob extends AbstractUIJob {

    private Throwable error;

    public DataTransferErrorJob(Throwable error)
    {
        super("Data Export Error");
        this.error = error;
    }

    @Override
    public IStatus runInUIThread(DBRProgressMonitor monitor)
    {
        UIUtils.showErrorDialog(
            getDisplay().getActiveShell(),
            "Data export error",
            error.getMessage(), error);
        return Status.OK_STATUS;
    }

}