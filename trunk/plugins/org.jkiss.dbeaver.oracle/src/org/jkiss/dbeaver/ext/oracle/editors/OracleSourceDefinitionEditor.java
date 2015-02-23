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

package org.jkiss.dbeaver.ext.oracle.editors;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.OracleConstants;
import org.jkiss.dbeaver.ext.oracle.model.source.OracleSourceObjectEx;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorNested;

/**
 * Oracle source definition editor
 */
public class OracleSourceDefinitionEditor extends SQLEditorNested<OracleSourceObjectEx> {

    @Override
    protected String getCompileCommandId()
    {
        return OracleConstants.CMD_COMPILE;
    }

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException {
        return getSourceObject().getSourceDefinition(monitor);
    }

    @Override
    protected void setSourceText(String sourceText) {
        getEditorInput().getPropertySource().setPropertyValue(
            OracleConstants.PROP_SOURCE_DEFINITION, sourceText);
    }

}
