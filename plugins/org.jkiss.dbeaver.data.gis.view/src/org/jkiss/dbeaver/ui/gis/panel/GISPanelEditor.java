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
package org.jkiss.dbeaver.ui.gis.panel;

import org.eclipse.jface.action.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.editors.BaseValueEditor;
import org.jkiss.dbeaver.ui.gis.IGeometryViewer;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerDescriptor;
import org.jkiss.dbeaver.ui.gis.registry.GeometryViewerRegistry;
import org.jkiss.utils.CommonUtils;

import java.util.List;

/**
 * GISPanelEditor
*/
public class GISPanelEditor extends BaseValueEditor<Control> {

    private static final Log log = Log.getLog(GISPanelEditor.class);
    private static final String PROP_VIEWER_ID = "gis.geometry.viewer.id";
    private static final String DEFAULT_VIEWER_ID = "browser";

    private GeometryViewerDescriptor curViewerDescriptor;
    private IGeometryViewer curViewer;

    public GISPanelEditor(IValueController controller) {
        super(controller);
        setDefaultViewer();
    }

    @Override
    public void primeEditorValue(Object value) throws DBException {
        if (curViewer != null) {
            curViewer.primeEditorValue(value);
        }
    }

    @Override
    public boolean isReadOnly() {
        return curViewer == null || curViewer.isReadOnly();
    }

    @Override
    public Object extractEditorValue() throws DBException {
        return curViewer == null ? null : curViewer.extractEditorValue();
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        List<GeometryViewerDescriptor> viewers = GeometryViewerRegistry.getInstance().getSupportedViewers(controller.getExecutionContext().getDataSource());
        for (int i = 0; i < viewers.size(); i++) {
            if (i > 0) {
                manager.add(new Separator());
            }
            GeometryViewerDescriptor vd = viewers.get(i);
            Action switchAction = new ViewerSetAction(vd);
            manager.add(ActionUtils.makeActionContribution(switchAction, true));
        }
        //manager.add(new ToolbarSeparatorContribution(false));
        //manager.add(new ViewerSwitchAction());
        if (curViewer != null) {
            curViewer.contributeActions(manager, controller);
        }
    }

    @Override
    protected Control createControl(Composite editPlaceholder) {
        if (curViewer == null) {
            return new Composite(editPlaceholder, SWT.NONE);
        } else {
            curViewer.createControl();
            return curViewer.getControl();
        }
    }

    private class ViewerSwitchAction extends Action implements SelectionListener {
        private Menu menu;

        ViewerSwitchAction() {
            super(null, Action.AS_DROP_DOWN_MENU);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.SCRIPTS));
            setToolTipText("Geometry viewer settings");
        }

        @Override
        public void runWithEvent(Event event)
        {
            if (event.widget instanceof ToolItem) {
                ToolItem toolItem = (ToolItem) event.widget;
                Menu menu = createMenu(toolItem);
                Rectangle bounds = toolItem.getBounds();
                Point point = toolItem.getParent().toDisplay(bounds.x, bounds.y + bounds.height);
                menu.setLocation(point.x, point.y);
                menu.setVisible(true);
            }
        }

        private Menu createMenu(ToolItem toolItem) {
            if (menu == null) {
                ToolBar toolBar = toolItem.getParent();
                menu = new Menu(toolBar);
                List<GeometryViewerDescriptor> viewers = GeometryViewerRegistry.getInstance().getSupportedViewers(
                    getValueController().getExecutionContext().getDataSource());
                for (GeometryViewerDescriptor viewerDescriptor : viewers) {
                    MenuItem item = new MenuItem(menu, SWT.RADIO);
                    item.setText(viewerDescriptor.getLabel());
                    item.setData(viewerDescriptor);
                    item.addSelectionListener(this);
                }
                if (curViewer != null) {
                    MenuManager menuManager = new MenuManager();
                    try {
                        curViewer.contributeActions(menuManager, valueController);
                        for (IContributionItem item : menuManager.getItems()) {
                            item.fill(menu, -1);
                        }
                    } catch (DBCException e) {
                        log.error(e);
                    }
                    toolBar.addDisposeListener(e -> menu.dispose());
                }
            }
            for (MenuItem item : menu.getItems()) {
                if (item.getData() instanceof GeometryViewerDescriptor) {
                    item.setSelection(item.getData() == curViewerDescriptor);
                }
            }
            return menu;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            for (MenuItem item : menu.getItems()) {
                if (item.getSelection()) {
                    Object itemData = item.getData();
                    if (itemData instanceof GeometryViewerDescriptor) {
                        GeometryViewerDescriptor newManager = (GeometryViewerDescriptor) itemData;
                        if (newManager != curViewerDescriptor) {
                            setViewer(newManager);
                            valueController.refreshEditor();
                        }
                    }
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {

        }
    }

    private void setViewer(GeometryViewerDescriptor viewerDescriptor) {
        curViewerDescriptor = viewerDescriptor;
        try {
            curViewer = curViewerDescriptor.createGeometryViewer(valueController);
        } catch (DBException e) {
            log.error(e);
        }
        valueController.getExecutionContext().getDataSource().getContainer().getPreferenceStore().setValue(
            PROP_VIEWER_ID, curViewerDescriptor.getId());
    }

    private void setDefaultViewer() {
        String viewerId = valueController.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getString(PROP_VIEWER_ID);
        if (CommonUtils.isEmpty(viewerId)) {
            viewerId = DEFAULT_VIEWER_ID;
        }
        GeometryViewerDescriptor viewerDescriptor = GeometryViewerRegistry.getInstance().getViewer(viewerId);
        if (viewerDescriptor == null || valueController.getEditType() == IValueController.EditType.INLINE && !viewerDescriptor.supportsInlineView()) {
            viewerId = DEFAULT_VIEWER_ID;
            viewerDescriptor = GeometryViewerRegistry.getInstance().getViewer(viewerId);
        }
        if (viewerDescriptor != null) {
            setViewer(viewerDescriptor);
        }
    }

    private class ViewerSetAction extends Action {
        private GeometryViewerDescriptor viewerDescriptor;

        ViewerSetAction(GeometryViewerDescriptor vd) {
            super(vd.getLabel(), Action.AS_RADIO_BUTTON);
            viewerDescriptor = vd;
            setToolTipText(viewerDescriptor.getDescription());
            if (viewerDescriptor.getIcon() != null) {
                setImageDescriptor(DBeaverIcons.getImageDescriptor(viewerDescriptor.getIcon()));
            }

        }

        @Override
        public boolean isChecked() {
            String viewerId = valueController.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getString(PROP_VIEWER_ID);
            if (CommonUtils.isEmpty(viewerId)) {
                viewerId = DEFAULT_VIEWER_ID;
            }
            return viewerDescriptor.getId().equals(viewerId);
        }

        @Override
        public void run() {
            if (curViewerDescriptor == viewerDescriptor) {
                return;
            }
            setViewer(viewerDescriptor);
            valueController.refreshEditor();
        }
    }
}
