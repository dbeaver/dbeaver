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

package org.jkiss.dbeaver.ext.oracle.model.source;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Stored code interface
 */
public interface OracleSourceObject extends OracleStatefulObject {

    void setName(String name);

    OracleSourceType getSourceType();

    String getSourceDeclaration(DBRProgressMonitor monitor)
        throws DBException;

    void setSourceDeclaration(String source);

    DBEPersistAction[] getCompileActions();

}
