/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database.load;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.DatabaseLoadService;

import java.lang.reflect.InvocationTargetException;

/**
 * TreeLoadService
 */
public class TreeLoadService extends DatabaseLoadService<Object[]> {

    private final DBNNode parentNode;

    public TreeLoadService(String serviceName, DBNNode parentNode)
    {
        super(serviceName, (DBNDatabaseNode)(parentNode instanceof DBNDatabaseNode ? parentNode : null));
        this.parentNode = parentNode;
    }

    public DBNNode getParentNode() {
        return parentNode;
    }

    @Override
    @NotNull
    public Object[] evaluate(@NotNull DBRProgressMonitor monitor)
        throws InvocationTargetException, InterruptedException
    {
        if (monitor.isCanceled() || parentNode.isDisposed()) {
            throw new InterruptedException("Navigator node is no longer available");
        }
        DBNNode[] children;
        try {
            children = DBNUtils.getNodeChildrenFiltered(monitor, parentNode, true);
        } catch (Throwable ex) {
            // A project can be deleted while its previously scheduled tree load is running.
            if (ex instanceof DBException && parentNode.isDisposed()) {
                throw new InterruptedException("Navigator node was disposed while loading");
            }
            throw new InvocationTargetException(ex);
        }
        if (monitor.isCanceled() || parentNode.isDisposed()) {
            throw new InterruptedException("Navigator node is no longer available");
        }
        return children == null ? new Object[0] : children;
    }

}
