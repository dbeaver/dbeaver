/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.qm.meta;

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
    public void close()
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
