/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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

    public long getContentLength() throws DBCException {
        if (storage != null) {
            return storage.getContentLength();
        }
        return getLOBLength();
    }

    protected abstract long getLOBLength() throws DBCException;

    public boolean updateContents(
        DBRProgressMonitor monitor,
        DBDContentStorage storage)
        throws DBException
    {
        if (this.storage != null) {
            if (this.originalStorage == null) {
                this.originalStorage = this.storage;
            } else {
                this.release();
            }
        }
        this.storage = storage;
        return true;
    }

    public void release()
    {
        if (storage != null) {
            storage.release();
            storage = null;
        }
    }

    public void resetContents()
    {
        if (this.originalStorage != null) {
            this.storage = this.originalStorage;
        }
    }

    public JDBCContentLOB makeNull()
    {
        return createNewContent();
    }

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
