/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;

/**
 * JDBCContentLOB
 *
 * @author Serge Rider
 */
public abstract class JDBCContentLOB extends JDBCContentAbstract implements DBDContent {

    private DBDContentStorage originalStorage;
    protected DBDContentStorage storage;

    protected JDBCContentLOB(DBCExecutionContext dataSource)
    {
        super(dataSource);
    }

    @Override
    public long getContentLength() throws DBCException {
        if (storage != null) {
            return storage.getContentLength();
        }
        return getLOBLength();
    }

    protected abstract long getLOBLength() throws DBCException;

    @Override
    public boolean updateContents(
        DBRProgressMonitor monitor,
        DBDContentStorage storage)
    {
        if (this.storage != null) {
            if (this.originalStorage != null && this.originalStorage != this.storage) {
                this.originalStorage.release();
            }
            this.originalStorage = this.storage;
        }
        this.storage = storage;
        this.modified = true;
        return true;
    }

    @Override
    public void release()
    {
        if (this.storage != null) {
            this.storage.release();
            this.storage = null;
        }
        if (this.originalStorage != null) {
            this.originalStorage.release();
            this.originalStorage = null;
        }
    }

    @Override
    public void resetContents()
    {
        if (this.originalStorage != null) {
            if (this.storage != null) {
                this.storage.release();
            }
            this.storage = this.originalStorage;
            this.modified = false;
        }
    }

    @Override
    public DBDValueCloneable cloneValue(DBRProgressMonitor monitor)
        throws DBCException
    {
        JDBCContentLOB copy = createNewContent();
        DBDContentStorage storage = getContents(monitor);
        if (storage != null) {
            try {
                copy.updateContents(monitor, storage.cloneStorage(monitor));
            } catch (IOException e) {
                throw new DBCException("IO error while clone content", e);
            }
        }
        return copy;
    }

    protected abstract JDBCContentLOB createNewContent();

}
