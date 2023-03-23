/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.themes.ITheme;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
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
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.Version;

import java.util.StringJoiner;

/**
 * DatabaseNavigatorLabelProvider
*/
public class DatabaseNavigatorLabelProvider extends ColumnLabelProvider implements IFontProvider, IColorProvider
{
    public static final String TREE_TABLE_FONT = "org.eclipse.ui.workbench.TREE_TABLE_FONT";

    private final IPropertyChangeListener themeChangeListener;

    protected Font normalFont;
    protected Font boldFont;
    protected Font italicFont;
    //private Font boldItalicFont;
    protected Color lockedForeground;
    protected Color transientForeground;
    private ILabelDecorator labelDecorator;

    public DatabaseNavigatorLabelProvider(@NotNull DatabaseNavigatorTree tree) {
        this.lockedForeground = Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
        this.transientForeground = Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
        this.themeChangeListener = e -> {
            final ITheme theme = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme();
            normalFont = theme.getFontRegistry().get(TREE_TABLE_FONT);
            boldFont = theme.getFontRegistry().getBold(TREE_TABLE_FONT);
            italicFont = theme.getFontRegistry().getItalic(TREE_TABLE_FONT);

            final TreeViewer viewer = tree.getViewer();
            viewer.getControl().setFont(normalFont);
            viewer.refresh();

            final Text filter = tree.getFilterControl();
            if (filter != null) {
                filter.setFont(normalFont);
            }
        };
        this.themeChangeListener.propertyChange(null);

        PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(themeChangeListener);
    }

    @Override
    public void dispose() {
        PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(themeChangeListener);
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
                StringJoiner tooltip = new StringJoiner("\n");
                tooltip.add(NLS.bind(UINavigatorMessages.navigator_provider_element_tooltip_datasource_name, ds.getName()));
                final DBPConnectionConfiguration cfg = ds.getConnectionConfiguration();
                if (!CommonUtils.isEmpty(cfg.getUrl())) {
                    tooltip.add(NLS.bind(UINavigatorMessages.navigator_provider_element_tooltip_datasource_url, cfg.getUrl()));
                } else if (!CommonUtils.isEmpty(cfg.getDatabaseName())) {
                    tooltip.add(NLS.bind(
                        UINavigatorMessages.navigator_provider_element_tooltip_datasource_database_name,
                        cfg.getDatabaseName()));
                }
                DBPDataSource dataSource = ds.getDataSource();
                if (dataSource != null) {
                    Version databaseVersion = dataSource.getInfo().getDatabaseVersion();
                    if (databaseVersion != null) {
                        tooltip.add(NLS.bind(
                            UINavigatorMessages.navigator_provider_element_tooltip_datasource_database_version,
                            databaseVersion.toString()));
                    }
                }
                if (!CommonUtils.isEmpty(cfg.getUserName())) {
                    tooltip.add(NLS.bind(UINavigatorMessages.navigator_provider_element_tooltip_datasource_user, cfg.getUserName()));
                }
                if (!CommonUtils.isEmpty(ds.getDescription())) {
                    tooltip.add(NLS.bind(
                        UINavigatorMessages.navigator_provider_element_tooltip_datasource_description,
                        ds.getDescription()));
                }
                if (ds.isConnectionReadOnly()) {
                    tooltip.add(UINavigatorMessages.navigator_provider_element_tooltip_datasource_read_only);
                }
                if (ds.isProvided()) {
                    tooltip.add(UINavigatorMessages.navigator_provider_element_tooltip_datasource_provided);
                }
                if (ds.getConnectionError() != null) {
                    tooltip.add(NLS.bind(UINavigatorMessages.navigator_provider_element_tooltip_datasource_error, ds.getConnectionError()));
                }

                return tooltip.toString();

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
