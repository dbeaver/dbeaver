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

import org.jkiss.dbeaver.model.exec.DBCSavepoint;

/**
 * QM Transaction info
 */
public class QMMTransactionInfo extends QMMObject {

    private final QMMSessionInfo session;
    private final QMMTransactionInfo previous;
    private boolean commited;
    private QMMTransactionSavepointInfo savepointStack;

    QMMTransactionInfo(QMMSessionInfo session, QMMTransactionInfo previous)
    {
        this.session = session;
        this.previous = previous;
        this.savepointStack = new QMMTransactionSavepointInfo(this, null, null, null);
    }

    void commit()
    {
        this.commited = true;
        for (QMMTransactionSavepointInfo sp = savepointStack; sp != null; sp = sp.getPrevious()) {
            if (!sp.isClosed()) {
                // Commit all non-finished savepoints
                sp.close(true);
            }
        }
        super.close();
    }

    void rollback(DBCSavepoint toSavepoint)
    {
        this.commited = false;
        for (QMMTransactionSavepointInfo sp = savepointStack; sp != null; sp = sp.getPrevious()) {
            sp.close(false);
            if (toSavepoint != null && sp.getReference() == toSavepoint) {
                break;
            }
        }
        super.close();
    }

    public QMMSessionInfo getSession()
    {
        return session;
    }

    public QMMTransactionInfo getPrevious()
    {
        return previous;
    }

    public boolean isCommited()
    {
        return commited;
    }

    public QMMTransactionSavepointInfo getCurrentSavepoint()
    {
        return savepointStack;
    }

    public QMMObject getSavepoint(DBCSavepoint savepoint)
    {
        for (QMMTransactionSavepointInfo sp = this.savepointStack; sp != null; sp = sp.getPrevious()) {
            if (sp.getReference() == savepoint) {
                return sp;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "TRANSACTION";
    }

}
