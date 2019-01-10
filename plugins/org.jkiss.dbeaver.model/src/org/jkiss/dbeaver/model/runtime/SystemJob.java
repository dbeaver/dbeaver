/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.runtime;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.lang.reflect.InvocationTargetException;

/**
 * Abstract Database Job
 */
public class SystemJob extends AbstractJob
{
    private final DBRRunnableWithProgress runnable;
    public SystemJob(String name, DBRRunnableWithProgress runnable) {
        super(name);
        setSystem(true);
        setUser(false);
        this.runnable = runnable;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        try {
            runnable.run(monitor);
        } catch (InvocationTargetException e) {
            return GeneralUtils.makeExceptionStatus(e.getTargetException());
        } catch (InterruptedException e) {
            return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

}