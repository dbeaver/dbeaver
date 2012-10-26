/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 * eugene.fradkin@gmail.com
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
package org.jkiss.dbeaver.tools.compare;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.registry.DataExporterDescriptor;
import org.jkiss.utils.CommonUtils;

import java.util.List;
import java.util.Map;

/**
 * Compare settings
 */
public class CompareObjectsSettings {


    private final List<DBNDatabaseNode> nodes;
    private boolean skipSystemObject = true;

    public CompareObjectsSettings(List<DBNDatabaseNode> nodes)
    {
        this.nodes = nodes;
    }

    public List<DBNDatabaseNode> getNodes()
    {
        return nodes;
    }

    public boolean isSkipSystemObject()
    {
        return skipSystemObject;
    }

    void loadFrom(IDialogSettings dialogSettings)
    {
        if (dialogSettings.get("skipSystem") != null) {
            skipSystemObject = dialogSettings.getBoolean("skipSystem");
        }
    }

    void saveTo(IDialogSettings dialogSettings)
    {
        dialogSettings.put("skipSystem", skipSystemObject);
    }
}
