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
package org.jkiss.dbeaver.runtime.qm.meta;

import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCStatement;

import java.lang.ref.SoftReference;

/**
 * DBCStatement meta info
 */
public class QMMStatementInfo extends QMMObject {

    private final QMMSessionInfo session;
    private SoftReference<DBCStatement> reference;
    private final DBCExecutionPurpose purpose;
    private final QMMStatementInfo previous;

    QMMStatementInfo(QMMSessionInfo session, DBCStatement reference, QMMStatementInfo previous)
    {
        this.session = session;
        this.reference = new SoftReference<DBCStatement>(reference);
        this.purpose = reference.getSession().getPurpose();
        this.previous = previous;
    }

    @Override
    protected void close()
    {
        super.close();
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public DBCStatement getReference()
    {
        return reference == null ? null : reference.get();
    }

    public DBCExecutionPurpose getPurpose()
    {
        return purpose;
    }

    public QMMStatementInfo getPrevious()
    {
        return previous;
    }

    @Override
    public String toString()
    {
        return "STATEMENT";
    }
}
