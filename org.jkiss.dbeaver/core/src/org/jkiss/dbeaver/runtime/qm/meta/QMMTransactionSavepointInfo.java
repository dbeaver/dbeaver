/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

import java.lang.ref.SoftReference;

/**
 * QM Savepoint info
 */
public class QMMTransactionSavepointInfo extends QMMObject {

    private final QMMTransactionInfo transaction;
    private SoftReference<DBCSavepoint> reference;
    private final String name;
    private boolean commited;
    private final QMMTransactionSavepointInfo previous;

    QMMTransactionSavepointInfo(QMMTransactionInfo transaction, DBCSavepoint reference, String name, QMMTransactionSavepointInfo previous)
    {
        this.transaction = transaction;
        this.reference = new SoftReference<DBCSavepoint>(reference);
        this.name = name;
        this.previous = previous;
    }

    protected void close(boolean commit)
    {
        this.commited = commit;
        super.close();
    }

    public QMMTransactionInfo getTransaction()
    {
        return transaction;
    }

    public DBCSavepoint getReference()
    {
        return reference == null ? null : reference.get();
    }

    public String getName()
    {
        return name;
    }

    public boolean isCommited()
    {
        return commited;
    }

    public QMMTransactionSavepointInfo getPrevious()
    {
        return previous;
    }

    @Override
    public String toString()
    {
        return "SAVEPOINT" + (name == null ? "" : name);
    }

}