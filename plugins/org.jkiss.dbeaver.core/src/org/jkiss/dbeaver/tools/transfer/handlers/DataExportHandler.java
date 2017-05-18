/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.tools.transfer.handlers;

import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferNode;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.Map;

public class DataExportHandler extends DataTransferHandler implements IElementUpdater {

    @Override
    protected IDataTransferNode adaptTransferNode(Object object)
    {
        final DBSDataContainer adapted = RuntimeUtils.getObjectAdapter(object, DBSDataContainer.class);
        if (adapted != null) {
            return new DatabaseTransferProducer(adapted);
        } else {
            return null;
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        DBNNode node = NavigatorUtils.getSelectedNode(element);
        if (node != null) {
            element.setText("Export " + node.getNodeType() + " Data");
        }
    }

}