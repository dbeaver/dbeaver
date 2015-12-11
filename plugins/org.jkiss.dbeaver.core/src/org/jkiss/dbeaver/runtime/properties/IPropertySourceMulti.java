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

package org.jkiss.dbeaver.runtime.properties;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Property source which allows editing of multiple objects.
 */
public interface IPropertySourceMulti extends DBPPropertySource {

    boolean isPropertySet(Object object, ObjectPropertyDescriptor id);

    Object getPropertyValue(@Nullable DBRProgressMonitor monitor, Object object, ObjectPropertyDescriptor prop);

    boolean isPropertyResettable(Object object, ObjectPropertyDescriptor prop);

    void resetPropertyValue(@Nullable DBRProgressMonitor monitor, Object object, ObjectPropertyDescriptor prop);

    void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object object, ObjectPropertyDescriptor prop, Object value);

}
