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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;

/**
 * Result set attribute meta data
 */
public interface DBCAttributeMetaData extends DBSAttributeBase
{
    /**
     * Source of this metadata object
     */
    @Nullable
    Object getSource();
    /**
     * Attribute label in result set
     * @return label
     */
    @NotNull
    String getLabel();

    /**
     * Owner entity name
     * @return entity name
     */
    @Nullable
    String getEntityName();

    /**
     * Read-only flag
     * @return read-only attribute state
     */
    boolean isReadOnly();

    /**
     * Pseudo attribute information. Null for non-pseudo attributes
     * @return pseudo attribute information or null
     */
    @Nullable
    DBDPseudoAttribute getPseudoAttribute();

    /**
     * Owner table metadata
     * @return table metadata
     */
    @Nullable
    DBCEntityMetaData getEntityMetaData();

}
