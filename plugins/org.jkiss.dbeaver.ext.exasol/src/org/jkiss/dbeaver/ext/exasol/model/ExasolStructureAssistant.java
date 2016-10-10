/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2016-2016 Karl Griesser (fullref@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.exasol.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;

import java.util.Collection;

public class ExasolStructureAssistant implements DBSStructureAssistant {

    private static final Log LOG = Log.getLog(ExasolStructureAssistant.class);

    /**
     * Exasol Structure Assistant
     *
     * @author Karl Griesser
     */

    // TODO DF: Work in progess

    private final ExasolDataSource dataSource;

    // -----------------
    // Constructors
    // -----------------
    public ExasolStructureAssistant(ExasolDataSource dataSource) {

        this.dataSource = dataSource;
    }

    // -----------------
    // Method Interface
    // -----------------

    public DBSObjectType[] getSupportedObjectTypes() {
        return new DBSObjectType[]{
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_CONSTRAINT
        };

    }

    public DBSObjectType[] getHyperlinkObjectTypes() {
        return getSupportedObjectTypes();
    }

    public DBSObjectType[] getAutoCompleteObjectTypes() {
        return getSupportedObjectTypes();
    }

    @Override
    public Collection<DBSObjectReference> findObjectsByMask(DBRProgressMonitor monitor, DBSObject parentObject,
                                                            DBSObjectType[] objectTypes, String objectNameMask, boolean caseSensitive, boolean globalSearch,
                                                            int maxResults) throws DBException {
        // TODO Auto-generated method stub
        return null;
    }


}
