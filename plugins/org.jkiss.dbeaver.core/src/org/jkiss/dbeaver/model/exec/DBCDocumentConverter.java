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

import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDDocument;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;

/**
 * Document converter.
 * Converts binary content into documents and back
 */
public interface DBCDocumentConverter {

    boolean canConvertFromContent(DBSAttributeBase attribute)
        throws DBCException;

    DBDDocument getDocumentFromContent(DBRProgressMonitor monitor, DBSAttributeBase attribute, DBDContent content)
        throws DBCException;

    DBDContent getContentFromDocument(DBRProgressMonitor monitor, DBSAttributeBase attribute, DBDContent originalContent, DBDDocument document)
        throws DBCException;

}
