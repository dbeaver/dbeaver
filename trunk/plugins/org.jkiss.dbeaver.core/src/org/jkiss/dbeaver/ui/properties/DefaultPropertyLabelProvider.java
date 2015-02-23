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
package org.jkiss.dbeaver.ui.properties;

import org.eclipse.jface.viewers.LabelProvider;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Label provider for property sources
 */
public class DefaultPropertyLabelProvider extends LabelProvider
{
    public static final DefaultPropertyLabelProvider INSTANCE = new DefaultPropertyLabelProvider();
    @Override
    public String getText(Object element)
    {
        return UIUtils.makeStringForUI(element).toString();
    }
}
