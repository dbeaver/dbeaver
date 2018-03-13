/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.controls.autorefresh;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;

public class AutoRefreshJob extends AbstractJob {

    private final AutoRefreshControl refreshControl;

    AutoRefreshJob(AutoRefreshControl refreshControl) {
        super("Auto-refresh job (" + refreshControl.getControlId() + ")");
        setSystem(true);
        setUser(false);
        this.refreshControl = refreshControl;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        if (!monitor.isCanceled() && refreshControl.isAutoRefreshEnabled()) {
            try {
                refreshControl.getRunnable().run(monitor);
            } catch (InvocationTargetException e) {
                return GeneralUtils.makeErrorStatus("Auto-refresh error", e.getTargetException());
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return Status.OK_STATUS;
    }

}
