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
package org.jkiss.dbeaver.model.runtime.load;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.BlockCanceler;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Lazy loading service
 * @param <RESULT> result type
 */
public abstract class AbstractLoadService<RESULT> implements ILoadService<RESULT> {
    private String serviceName;
    private DBRProgressMonitor progressMonitor;
    private AbstractJob ownerJob;

    protected AbstractLoadService(String serviceName)
    {
        this.serviceName = serviceName;
    }

    protected AbstractLoadService()
    {
        this("Loading");
    }

    @Override
    public String getServiceName()
    {
        return serviceName;
    }

    public void initService(DBRProgressMonitor monitor, AbstractJob ownerJob)
    {
        this.progressMonitor = monitor;
        this.ownerJob = ownerJob;
    }

    @Override
    public boolean cancel() throws InvocationTargetException
    {
        if (this.ownerJob != null) {
            return this.ownerJob.cancel();
        } else if (progressMonitor != null) {
            try {
                List<DBRBlockingObject> activeBlocks = progressMonitor.getActiveBlocks();
                if (!CommonUtils.isEmpty(activeBlocks)) {
                    BlockCanceler.cancelBlock(progressMonitor, activeBlocks.get(activeBlocks.size() - 1), null);
                }
                return true;
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
        return false;
    }

}