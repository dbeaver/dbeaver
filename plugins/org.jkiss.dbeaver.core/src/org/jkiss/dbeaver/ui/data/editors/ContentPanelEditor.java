/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.panel.ViewValuePanel;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.registry.StreamValueManagerDescriptor;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
* ControlPanelEditor
*/
public class ContentPanelEditor extends BaseValueEditor<Control> {

    public static final String PREF_TEXT_EDITOR_WORD_WRAP = "content.text.editor.word-wrap";

    private static final Log log = Log.getLog(ContentPanelEditor.class);

    private static Map<String, String> valueToManagerMap = new HashMap<>();

    private Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> streamManagers;
    private StreamValueManagerDescriptor curStreamManager;
    private IStreamValueEditor streamEditor;
    private Control editorControl;

    public ContentPanelEditor(IValueController controller) {
        super(controller);
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        if (streamManagers != null) {
            manager.add(new ContentTypeSwitchAction());
        }
        if (streamEditor != null) {
            streamEditor.contributeActions(manager, control);
        }
    }

    @Override
    public void primeEditorValue(@Nullable final Object value) throws DBException
    {
        final DBDContent content = (DBDContent) valueController.getValue();
        if (content == null) {
            valueController.showMessage("NULL content value. Must be DBDContent.", DBPMessageType.ERROR);
            return;
        }
        if (streamEditor == null) {
            valueController.showMessage("NULL content editor.", DBPMessageType.ERROR);
            return;
        }
        DBeaverUI.runInUI(valueController.getValueSite().getWorkbenchWindow(), new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    streamEditor.primeEditorValue(monitor, control, content);
                } catch (Throwable e) {
                    log.debug(e);
                    valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
                }
            }
        });
    }

    @Override
    public Object extractEditorValue() throws DBException
    {
        final DBDContent content = (DBDContent) valueController.getValue();
        if (content == null) {
            log.warn("NULL content value. Must be DBDContent.");
        } else if (streamEditor == null) {
            log.warn("NULL content editor.");
        } else {
            try {
                streamEditor.extractEditorValue(VoidProgressMonitor.INSTANCE, control, content);
            } catch (Throwable e) {
                log.debug(e);
                valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
            }
        }
        return content;
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        final DBDContent content = (DBDContent) valueController.getValue();

        if (curStreamManager == null) {
            detectStreamManager(content);
        }
        if (curStreamManager != null) {
            try {
                streamEditor = curStreamManager.getInstance().createPanelEditor(valueController);
            } catch (Throwable e) {
                UIUtils.showErrorDialog(editPlaceholder.getShell(), "No stream editor", "Can't create stream editor", e);
            }
        }
        if (streamEditor == null) {
            return UIUtils.createInfoLabel(editPlaceholder, "No Editor");
        }

        editorControl = streamEditor.createControl(valueController);
        return editorControl;
    }

    private void detectStreamManager(final DBDContent content) {
        DBeaverUI.runInUI(new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                monitor.beginTask("Detect appropriate editor", 1);
                try {
                    streamManagers = ValueManagerRegistry.getInstance().getApplicableStreamManagers(monitor, valueController.getValueType(), content);
                    String savedManagerId = valueToManagerMap.get(makeValueId());
                    if (savedManagerId != null) {
                        curStreamManager = findManager(savedManagerId);
                    }
                    if (curStreamManager == null) {
                        curStreamManager = findManager(IStreamValueManager.MatchType.EXCLUSIVE);
                        if (curStreamManager == null)
                            curStreamManager = findManager(IStreamValueManager.MatchType.PRIMARY);
                        if (curStreamManager == null)
                            curStreamManager = findManager(IStreamValueManager.MatchType.DEFAULT);
                        if (curStreamManager == null)
                            curStreamManager = findManager(IStreamValueManager.MatchType.APPLIES);
                        if (curStreamManager == null) {
                            throw new DBException("Can't find appropriate stream manager");
                        }
                    }
                } catch (Exception e) {
                    valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
                } finally {
                    monitor.done();
                }
            }

            private StreamValueManagerDescriptor findManager(IStreamValueManager.MatchType matchType) {
                for (Map.Entry<StreamValueManagerDescriptor, IStreamValueManager.MatchType> entry : streamManagers.entrySet()) {
                    if (entry.getValue() == matchType) {
                        return entry.getKey();
                    }
                }
                return null;
            }
            private StreamValueManagerDescriptor findManager(String id) {
                for (Map.Entry<StreamValueManagerDescriptor, IStreamValueManager.MatchType> entry : streamManagers.entrySet()) {
                    if (entry.getKey().getId().equals(id)) {
                        return entry.getKey();
                    }
                }
                return null;
            }
        });
    }

    private void setStreamManager(StreamValueManagerDescriptor newManager) {
        curStreamManager = newManager;

        if (curStreamManager != null) {
            // Save manager setting for current attribute
            String valueId = makeValueId();
            valueToManagerMap.put(valueId, curStreamManager.getId());

            valueController.refreshEditor();
        }
    }

    private String makeValueId() {
        String valueId;
        DBSTypedObject valueType = valueController.getValueType();
        if (valueType instanceof DBDAttributeBinding) {
            valueType = ((DBDAttributeBinding) valueType).getAttribute();
        }
        if (valueType instanceof DBSObject) {
            DBSObject object = (DBSObject) valueType;
            valueId = DBUtils.getObjectFullName(object, DBPEvaluationContext.DDL);
            if (object.getParentObject() != null) {
                valueId = DBUtils.getObjectFullName(object.getParentObject(), DBPEvaluationContext.DDL) + ":" + valueId;
            }

        } else {
            valueId = valueController.getValueName();
        }
        String dsId = "unknown";
        if (valueController.getExecutionContext() != null) {
            dsId = valueController.getExecutionContext().getDataSource().getContainer().getId();
        }
        return dsId + ":" + valueId;
    }

    public static void setEditorSettings(Control control) {
        if (control instanceof StyledText) {
            if (ViewValuePanel.getPanelSettings().getBoolean(PREF_TEXT_EDITOR_WORD_WRAP)) {
                ((StyledText) control).setWordWrap(true);
            }
        }
    }

    private class ContentTypeSwitchAction extends Action implements SelectionListener {
        private Menu menu;

        public ContentTypeSwitchAction() {
            super(null, Action.AS_DROP_DOWN_MENU);
            setImageDescriptor(DBeaverIcons.getImageDescriptor(UIIcon.PAGES));
            setToolTipText("Content viewer settings");
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
                List<StreamValueManagerDescriptor> managers = new ArrayList<>(streamManagers.keySet());
                Collections.sort(managers, new Comparator<StreamValueManagerDescriptor>() {
                    @Override
                    public int compare(StreamValueManagerDescriptor o1, StreamValueManagerDescriptor o2) {
                        return o1.getLabel().compareTo(o2.getLabel());
                    }
                });
                for (StreamValueManagerDescriptor manager : managers) {
                    MenuItem item = new MenuItem(menu, SWT.RADIO);
                    item.setText(manager.getLabel());
                    item.setData(manager);
                    item.addSelectionListener(this);
                }
                if (editorControl instanceof StyledText){
                    new MenuItem(menu, SWT.SEPARATOR);
                    final MenuItem wwItem = new MenuItem(menu, SWT.CHECK);
                    wwItem.setText("Word Wrap");
                    wwItem.setSelection(((StyledText) editorControl).getWordWrap());
                    wwItem.addSelectionListener(new SelectionAdapter() {
                        @Override
                        public void widgetSelected(SelectionEvent e) {
                            boolean newWW = !((StyledText) editorControl).getWordWrap();
                            wwItem.setSelection(newWW);
                            ((StyledText) editorControl).setWordWrap(newWW);
                            ViewValuePanel.getPanelSettings().put(PREF_TEXT_EDITOR_WORD_WRAP, newWW);
                        }
                    });
                }
                toolBar.addDisposeListener(new DisposeListener() {
                    @Override
                    public void widgetDisposed(DisposeEvent e) {
                        menu.dispose();
                    }
                });
            }
            for (MenuItem item : menu.getItems()) {
                if (item.getData() instanceof StreamValueManagerDescriptor) {
                    item.setSelection(item.getData() == curStreamManager);
                }
            }
            return menu;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            for (MenuItem item : menu.getItems()) {
                if (item.getSelection()) {
                    StreamValueManagerDescriptor newManager = (StreamValueManagerDescriptor) item.getData();
                    if (newManager != curStreamManager) {
                        setStreamManager(newManager);
                    }
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {

        }
    }
}
