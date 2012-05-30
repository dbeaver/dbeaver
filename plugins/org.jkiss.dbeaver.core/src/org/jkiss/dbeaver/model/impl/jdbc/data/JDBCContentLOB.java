/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.jdbc.data;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * JDBCContentLOB
 *
 * @author Serge Rider
 */
public abstract class JDBCContentLOB extends JDBCContentAbstract implements DBDContent {

    static final Log log = LogFactory.getLog(JDBCContentLOB.class);

    private DBDContentStorage originalStorage;
    protected DBDContentStorage storage;

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
        throws DBException
    {
        if (this.storage != null) {
            if (this.originalStorage != null) {
                this.originalStorage.release();
            }
            this.originalStorage = this.storage;
        }
        this.storage = storage;
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
        }
    }

    @Override
    public JDBCContentLOB makeNull()
    {
        return createNewContent();
    }

    @Override
    public DBDValueCloneable cloneValue(DBRProgressMonitor monitor)
        throws DBCException
    {
        JDBCContentLOB copy = createNewContent();
        DBDContentStorage storage = getContents(monitor);
        try {
            copy.updateContents(monitor, storage.cloneStorage(monitor));
        }
        catch (Exception e) {
            throw new DBCException(e);
        }
        return copy;
    }

    protected abstract JDBCContentLOB createNewContent();

}
