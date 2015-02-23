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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.ext.IDataSourceProvider;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DB editor utils
 */
public class DatabaseEditorUtils {

    public static void setPartBackground(IEditorPart editor, Composite composite)
    {
        Composite rootComposite = null;
        for (Composite c = composite; c != null; c = c.getParent()) {
            if (c.getParent() instanceof CTabFolder) {
                ((CTabFolder) c.getParent()).setBorderVisible(false);
                rootComposite = c;
                break;
            }
        }
        if (rootComposite == null) {
            return;
        }

        DBPDataSource dataSource = null;
        if (editor instanceof IDataSourceProvider) {
            dataSource = ((IDataSourceProvider) editor).getDataSource();
        }
        if (dataSource == null) {
            rootComposite.setBackground(null);
            return;
        }
        rootComposite.setBackground(
            dataSource.getContainer().getConnectionInfo().getColor());
    }

}
