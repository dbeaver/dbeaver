/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.controls;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

/**
 * Label providers
 */
public class DatabaseLabelProviders {


    public static final String EMPTY_SELECTION_TEXT = UINavigatorMessages.toolbar_datasource_selector_empty;

    public static class ConnectionLabelProvider extends LabelProvider implements IColorProvider {
        @Override
        public Image getImage(Object element) {
            if (element == null) {
                return DBeaverIcons.getImage(DBIcon.DATABASE_DEFAULT);
            }
            DBNModel nm = DBWorkbench.getPlatform().getNavigatorModel();
            nm.ensureProjectLoaded(((DBPDataSourceContainer) element).getRegistry().getProject());
            final DBNDatabaseNode node = nm.findNode((DBPDataSourceContainer) element);
            return node == null ? null : DBeaverIcons.getImage(node.getNodeIcon());
        }

        @Override
        public String getText(Object element) {
            if (element == null) {
                return EMPTY_SELECTION_TEXT;
            }
            return ((DBPDataSourceContainer) element).getName();
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            return element == null ? null : UIUtils.getConnectionColor(((DBPDataSourceContainer) element).getConnectionConfiguration());
        }
    }

    public static class DatabaseLabelProvider extends LabelProvider implements IColorProvider {
        @Override
        public Image getImage(Object element) {
            if (element == null) {
                return DBeaverIcons.getImage(DBIcon.DATABASE_DEFAULT);
            }
            return DBeaverIcons.getImage(((DBNDatabaseNode)element).getNodeIconDefault());
        }

        @Override
        public String getText(Object element) {
            if (element == null) {
                return EMPTY_SELECTION_TEXT;
            }
            return ((DBNDatabaseNode)element).getNodeName();
        }

        @Override
        public Color getForeground(Object element) {
            return null;
        }

        @Override
        public Color getBackground(Object element) {
            if (element instanceof DBNDatabaseNode) {
                final DBPDataSourceContainer container = ((DBNDatabaseNode) element).getDataSourceContainer();
                if (container != null) {
                    return UIUtils.getConnectionColor((container.getConnectionConfiguration()));
                }
            }
            return null;
        }
    }

}