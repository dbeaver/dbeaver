/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.navigator.DBNUtils;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.utils.CommonUtils;

/**
 * DatabaseNavigatorLabelProvider
*/
public class DatabaseNavigatorLabelProvider extends ColumnLabelProvider implements IFontProvider, IColorProvider
{
    protected Font normalFont;
    protected Font boldFont;
    protected Font italicFont;
    //private Font boldItalicFont;
    protected Color lockedForeground;
    protected Color transientForeground;
    private ILabelDecorator labelDecorator;

    public DatabaseNavigatorLabelProvider(Viewer viewer)
    {
        //this.view = view;
        this.normalFont = viewer.getControl().getFont();
        this.boldFont = UIUtils.makeBoldFont(normalFont);
        this.italicFont = UIUtils.modifyFont(normalFont, SWT.ITALIC);
        //this.boldItalicFont = UIUtils.modifyFont(normalFont, SWT.BOLD | SWT.ITALIC);
        this.lockedForeground = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
        this.transientForeground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
    }

    @Override
    public void dispose()
    {
        UIUtils.dispose(boldFont);
        boldFont = null;
        UIUtils.dispose(italicFont);
        italicFont = null;
//        UIUtils.dispose(boldItalicFont);
//        boldItalicFont = null;
        super.dispose();
    }

    ILabelDecorator getLabelDecorator() {
        return labelDecorator;
    }

    void setLabelDecorator(ILabelDecorator labelDecorator) {
        this.labelDecorator = labelDecorator;
    }

    @Override
    public String getText(Object obj)
    {
        String text = null;
        if (obj instanceof ILabelProvider) {
            text = ((ILabelProvider)obj).getText(obj);
/*
        } else if (obj instanceof DBSObject) {
            text = ((DBSObject) obj).getName();
*/
        } else if (obj instanceof DBNNode) {
            text = ((DBNNode) obj).getNodeName();
            if (DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_OBJECT_TIPS)) {
                String briefInfo = ((DBNNode) obj).getNodeBriefInfo();
                if (!CommonUtils.isEmpty(briefInfo)) {
                    text += " (" + briefInfo + ")";
                }
            }
        } else if (obj != null) {
            text = obj.toString();
        }
        if (text == null) {
            text = "?";
        }
        if (isFilteredElement(obj)) {
            text += " (...)";
        }
        return text;
    }

    @Override
    public Image getImage(Object obj)
    {
        Image image = null;
        if (obj instanceof ILabelProvider) {
            image = ((ILabelProvider)obj).getImage(obj);
        } else if (obj instanceof DBNNode) {
            image = DBeaverIcons.getImage(((DBNNode) obj).getNodeIconDefault());
        }

        if (labelDecorator != null && obj instanceof DBNResource) {
            image = labelDecorator.decorateImage(image, obj);
        }
        return image;
    }

    @Override
    public Font getFont(Object element)
    {
        if (DBNUtils.isDefaultElement(element)) {
            return boldFont;
        } else {
            if (element instanceof DBNDataSource) {
                final DBPDataSourceContainer ds = ((DBNDataSource) element).getDataSourceContainer();
                if (ds != null && (ds.isProvided() || ds.isTemporary())) {
                    return italicFont;
                }
            }
            return normalFont;
        }
    }

    @Override
    public Color getForeground(Object element)
    {
        if (element instanceof DBNNode) {
            DBNNode node = (DBNNode)element;
            if (node instanceof DBNDataSource) {
                DBPDataSourceContainer ds = ((DBNDataSource) element).getDataSourceContainer();
                Color bgColor = UIUtils.getConnectionColor(ds.getConnectionConfiguration());
                return bgColor == null ? null : UIUtils.getContrastColor(bgColor);
            }
            if (node.isLocked()) {
                return lockedForeground;
            }
            if (node instanceof DBSWrapper && ((DBSWrapper)node).getObject() != null && !((DBSWrapper)node).getObject().isPersisted()) {
                return transientForeground;
            }
        }
        return null;
    }

    @Override
    public Color getBackground(Object element)
    {
        if (element instanceof DBNDataSource) {
            DBPDataSourceContainer ds = ((DBNDataSource) element).getDataSourceContainer();
            if (ds != null) {
                return UIUtils.getConnectionColor(ds.getConnectionConfiguration());
            }
        }
        return null;
    }

    private boolean isFilteredElement(Object element)
    {
        return element instanceof DBNNode && ((DBNNode) element).isFiltered();
    }

    @Override
    public String getToolTipText(Object element) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_TOOLTIPS)) {
            return null;
        }
        if (element instanceof DBNDataSource) {
            final DBPDataSourceContainer ds = ((DBNDataSource) element).getDataSourceContainer();
            if (ds != null) {
                StringBuilder info = new StringBuilder();
                info.append("Name: ").append(ds.getName()).append("\n");
                final DBPConnectionConfiguration cfg = ds.getConnectionConfiguration();
                if (!CommonUtils.isEmpty(cfg.getUrl())) {
                    info.append("URL: ").append(cfg.getUrl()).append("\n");
                } else if (!CommonUtils.isEmpty(cfg.getDatabaseName())) {
                    info.append("Database: ").append(cfg.getDatabaseName()).append("\n");
                }
                if (!CommonUtils.isEmpty(cfg.getUserName())) {
                    info.append("User: ").append(cfg.getUserName()).append("\n");
                }
                if (!CommonUtils.isEmpty(ds.getDescription())) {
                    info.append("Description: ").append(ds.getDescription()).append("\n");
                }
/*
                if (cfg.getConnectionType() != null) {
                    info.append("Type: ").append(cfg.getConnectionType().getName()).append("\n");
                }
*/
                if (ds.isConnectionReadOnly()) {
                    info.append("Read-only connection\n");
                }
                if (ds.isProvided()) {
                    info.append("Provided connection\n");
                }
                if (ds.getConnectionError() != null) {
                    info.append("\nError: ").append(ds.getConnectionError());
                }

                return info.toString().trim();

            }
        } else if (element instanceof DBNNode) {
            if (element instanceof DBNResource &&
                !DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_CONTENTS_IN_TOOLTIP))
            {
                return null;
            }
            final String description = ((DBNNode) element).getNodeDescription();
            if (!CommonUtils.isEmptyTrimmed(description)) {
                return description;
            }
            return ((DBNNode) element).getNodeName();
        }
        return null;
    }

    @Override
    public Image getToolTipImage(Object element) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(NavigatorPreferences.NAVIGATOR_SHOW_TOOLTIPS)) {
            return null;
        }
        if (element instanceof DBNNode) {
            return DBeaverIcons.getImage(((DBNNode) element).getNodeIconDefault());
        }
        return null;
    }

    @Override
    public int getToolTipDisplayDelayTime(Object object) {
        return 0;
    }

    @Override
    public int getToolTipStyle(Object object) {
        return super.getToolTipStyle(object);
    }

    @Override
    public boolean useNativeToolTip(Object object) {
        return true;
    }
}
