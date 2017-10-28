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

package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.contexts.IContextActivation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.themes.IThemeManager;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.ui.UIUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract presentation.
 */
public abstract class AbstractPresentation implements IResultSetPresentation, ISelectionProvider {

    private static final String PRESENTATION_CONTROL_ID = "org.jkiss.dbeaver.ui.resultset.presentation";
    //public static final String RESULTS_CONTROL_CONTEXT_ID = "org.jkiss.dbeaver.ui.context.resultset.focused";
    public static final StructuredSelection EMPTY_SELECTION = new StructuredSelection();
    public static final String RESULT_SET_PRESENTATION_CONTEXT_MENU = "org.jkiss.dbeaver.ui.controls.resultset.conext.menu";

    @NotNull
    protected IResultSetController controller;
    private final List<ISelectionChangedListener> selectionChangedListenerList = new ArrayList<>();
    protected IThemeManager themeManager;
    private IPropertyChangeListener themeChangeListener;

    @Override
    @NotNull
    public IResultSetController getController() {
        return controller;
    }

    @Override
    public void createPresentation(@NotNull final IResultSetController controller, @NotNull Composite parent) {
        this.controller = controller;

        this.themeManager = controller.getSite().getWorkbenchWindow().getWorkbench().getThemeManager();
        this.themeChangeListener = event -> {
            if (event.getProperty().startsWith(ThemeConstants.RESULTS_PROP_PREFIX)) {
                applyThemeSettings();
            }
        };
        this.themeManager.addPropertyChangeListener(themeChangeListener);
    }

    public void dispose() {
        if (themeChangeListener != null) {
            themeManager.removePropertyChangeListener(themeChangeListener);
            themeChangeListener = null;
        }
    }

    protected void applyThemeSettings() {

    }

    @Override
    public void fillMenu(@NotNull IMenuManager menu) {

    }

    /**
     * Shifts current row and redraws current control.
     * In record mode refreshes data.
     * @param position    position
     */
    @Override
    public void scrollToRow(@NotNull RowPosition position) {
        ResultSetRow currentRow = controller.getCurrentRow();
        ResultSetModel model = controller.getModel();
        switch (position) {
            case FIRST:
                if (model.getRowCount() > 0) {
                    controller.setCurrentRow(model.getRow(0));
                }
                break;
            case PREVIOUS:
                if (currentRow != null && currentRow.getVisualNumber() > 0) {
                    controller.setCurrentRow(model.getRow(currentRow.getVisualNumber() - 1));
                }
                break;
            case NEXT:
                if (currentRow != null && currentRow.getVisualNumber() < model.getRowCount() - 1) {
                    controller.setCurrentRow(model.getRow(currentRow.getVisualNumber() + 1));
                }
                break;
            case LAST:
                if (currentRow != null) {
                    controller.setCurrentRow(model.getRow(model.getRowCount() - 1));
                }
                break;
            case CURRENT:
                // do nothing
                break;
        }
        if (controller.isRecordMode()) {
            refreshData(true, false, false);
        } else {
            getControl().redraw();
        }
        controller.updateStatusMessage();
        controller.updateEditControls();
    }

    @Override
    public void setCurrentAttribute(@NotNull DBDAttributeBinding attribute) {

    }

    @Override
    public Point getCursorLocation() {
        return null;
    }

    @Override
    public void printResultSet() {

    }

    protected void registerContextMenu() {
        // Register context menu
        MenuManager menuMgr = new MenuManager(null, RESULT_SET_PRESENTATION_CONTEXT_MENU);
        Menu menu = menuMgr.createContextMenu(getControl());
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager)
            {
                controller.fillContextMenu(
                    manager,
                    getCurrentAttribute(),
                    controller.getCurrentRow());
            }
        });
        menuMgr.setRemoveAllWhenShown(true);
        getControl().setMenu(menu);
        controller.getSite().registerContextMenu(menuMgr, null);
    }

    protected void trackPresentationControl() {
        final Control control = getControl();
        final IWorkbenchPartSite site = controller.getSite();
        UIUtils.addFocusTracker(site, PRESENTATION_CONTROL_ID, control);

/*
        final IContextService contextService = site.getService(IContextService.class);
        if (contextService != null) {
            control.addFocusListener(new FocusListener() {
                IContextActivation activation;
                @Override
                public void focusGained(FocusEvent e) {
                    if (activation == null) {
                        activation = contextService.activateContext(RESULTS_CONTROL_CONTEXT_ID);
                    }
                }
                @Override
                public void focusLost(FocusEvent e) {
                    contextService.deactivateContext(activation);
                }
            });
        }
*/
        control.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                UIUtils.removeFocusTracker(site, control);
            }
        });
    }

    protected void activateTextKeyBindings(@NotNull IResultSetController controller, Control control) {
        final IContextService contextService = controller.getSite().getService(IContextService.class);
        control.addFocusListener(new FocusListener() {
            IContextActivation activation;
            @Override
            public void focusGained(FocusEvent e) {
                controller.updateEditControls();
                if (activation == null) {
                    activation = contextService.activateContext("org.eclipse.ui.textEditorScope");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                controller.updateEditControls();
                if (activation != null) {
                    contextService.deactivateContext(activation);
                    activation = null;
                }
            }
        });
    }

    ///////////////////////////////////////////////////////////////////////
    // ISelectionProvider

    protected void fireSelectionChanged(ISelection selection) {
        SelectionChangedEvent event = new SelectionChangedEvent(this, selection);
        for (ISelectionChangedListener listener : selectionChangedListenerList) {
            listener.selectionChanged(event);
        }
    }

    @Override
    public void addSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListenerList.add(listener);
    }

    @Override
    public void removeSelectionChangedListener(ISelectionChangedListener listener) {
        selectionChangedListenerList.remove(listener);
    }

    @Override
    public ISelection getSelection() {
        return EMPTY_SELECTION;
    }

    @Override
    public void setSelection(ISelection selection) {

    }

}
