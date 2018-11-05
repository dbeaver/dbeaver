/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.AbstractUIJob;

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
        DBUserInterface.getInstance().showError(
                "Data export error",
            error.getMessage(), error);
        return Status.OK_STATUS;
    }

}