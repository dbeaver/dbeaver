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
package org.jkiss.dbeaver.model.exec;

import org.jkiss.dbeaver.model.DBPCloseableObject;
import org.jkiss.dbeaver.model.DBPObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.PrintWriter;
import java.io.Writer;

/**
 * Provides ability to read server logs for certain session
 */
public interface DBCServerOutputReader extends DBPObject,DBPCloseableObject
{
    boolean isServerOutputEnabled();

    void enableServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, boolean enable) throws DBCException;

    void readServerOutput(DBRProgressMonitor monitor, DBCExecutionContext context, PrintWriter output)
        throws DBCException;
}
