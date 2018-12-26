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
package org.jkiss.dbeaver.ui.data.editors;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.data.StringContent;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.AbstractLoadService;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.LoadingJob;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.ProgressLoaderVisualizer;
import org.jkiss.dbeaver.ui.data.IStreamValueEditor;
import org.jkiss.dbeaver.ui.data.IStreamValueManager;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.registry.StreamValueManagerDescriptor;
import org.jkiss.dbeaver.ui.data.registry.ValueManagerRegistry;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

/**
* ControlPanelEditor
*/
public class ContentPanelEditor extends BaseValueEditor<Control> implements IAdaptable {

    private static final Log log = Log.getLog(ContentPanelEditor.class);

    private static Map<String, String> valueToManagerMap = new HashMap<>();

    private Map<StreamValueManagerDescriptor, IStreamValueManager.MatchType> streamManagers;
    private volatile StreamValueManagerDescriptor curStreamManager;
    private IStreamValueEditor<Control> streamEditor;
    private Control editorControl;

    public ContentPanelEditor(IValueController controller) {
        super(controller);
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull IValueController controller) throws DBCException {
        manager.add(new ContentTypeSwitchAction());
        if (streamEditor != null) {
            streamEditor.contributeActions(manager, control);
        }
    }

    @Override
    public void primeEditorValue(@Nullable final Object value) throws DBException
    {
        primeEditorValue(value, true);
    }

    protected void primeEditorValue(@Nullable final Object value, boolean loadInService) throws DBException
    {
        final Object content = valueController.getValue();
        if (streamEditor == null) {
            // Editor not yet initialized
            return;
        }
        if (isStringValue()) {
            // It is a string
            DBPDataSource dataSource = valueController.getExecutionContext() == null ? null : valueController.getExecutionContext().getDataSource();
            streamEditor.primeEditorValue(
                new VoidProgressMonitor(),
                control,
                new StringContent(
                    dataSource, CommonUtils.toString(content)));
        } else if (content instanceof DBDContent) {
            loadInService = !(content instanceof DBDContentCached);
            if (loadInService) {
                StreamValueLoadService loadingService = new StreamValueLoadService((DBDContent) content);

                Composite ph = control instanceof Composite ? (Composite) control : valueController.getEditPlaceholder();
                LoadingJob.createService(
                    loadingService,
                    new StreamValueLoadVisualizer(loadingService, ph))
                    .schedule();
            } else {
                streamEditor.primeEditorValue(new VoidProgressMonitor(), control, (DBDContent) content);
            }

        } else if (content == null) {
            valueController.showMessage("NULL content value. Must be DBDContent.", DBPMessageType.ERROR);
        } else {
            valueController.showMessage("Unsupported content value. Must be DBDContent or String.", DBPMessageType.ERROR);
        }
    }

    private boolean isStringValue() {
        return !(valueController.getValue() instanceof DBDContent);
    }

    @Override
    public Object extractEditorValue() throws DBException
    {
        final Object content = valueController.getValue();
        if (isStringValue()) {
            StringContent stringContent = new StringContent(
                valueController.getExecutionContext().getDataSource(), null);
            streamEditor.extractEditorValue(new VoidProgressMonitor(), control, stringContent);
            return stringContent.getRawValue();
        } else {
            if (content == null) {
                log.warn("NULL content value. Must be DBDContent.");
            } else if (streamEditor == null) {
                log.warn("NULL content editor.");
            } else {
                try {
                    streamEditor.extractEditorValue(new VoidProgressMonitor(), control, (DBDContent) content);
                } catch (Throwable e) {
                    log.debug(e);
                    valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
                }
            }
            return content;
        }
    }

    @Override
    protected Control createControl(Composite editPlaceholder)
    {
        final Object content = valueController.getValue();

        if (curStreamManager == null) {
            if (isStringValue()) {
                try {
                    loadStringStreamManagers();
                } catch (Throwable e) {
                    DBWorkbench.getPlatformUI().showError("No string editor", "Can't load string content managers", e);
                }
            } else {
                //UIUtils.createLabel(editPlaceholder, UIIcon.REFRESH);
                runSreamManagerDetector((DBDContent) content, editPlaceholder);
                return editPlaceholder;
            }
        }
        return createStreamManagerControl(editPlaceholder);
    }

    private Control createStreamManagerControl(Composite editPlaceholder) {
        if (curStreamManager != null) {
            try {
                streamEditor = curStreamManager.getInstance().createPanelEditor(valueController);
            } catch (Throwable e) {
                DBWorkbench.getPlatformUI().showError("No stream editor", "Can't create stream editor", e);
            }
        }
        if (streamEditor == null) {
            return UIUtils.createInfoLabel(editPlaceholder, "No Editor");
        }

        editorControl = streamEditor.createControl(valueController);
        return editorControl;
    }

    private void loadStringStreamManagers() throws DBException {
        streamManagers = ValueManagerRegistry.getInstance().getStreamManagersByMimeType(MimeTypes.TEXT, MimeTypes.TEXT_PLAIN);
        String savedManagerId = valueToManagerMap.get(makeValueId());
        detectCurrentStreamManager(savedManagerId);
    }

    private void detectCurrentStreamManager(String savedManagerId) throws DBException {
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
    }

    private void runSreamManagerDetector(final DBDContent content, Composite editPlaceholder) {
        StreamManagerDetectService loadingService = new StreamManagerDetectService(content);

        LoadingJob.createService(
            loadingService,
            new StreamManagerDetectVisualizer(loadingService, editPlaceholder))
            .schedule();
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

    private StreamValueManagerDescriptor findManager(String id) {
        for (Map.Entry<StreamValueManagerDescriptor, IStreamValueManager.MatchType> entry : streamManagers.entrySet()) {
            if (entry.getKey().getId().equals(id)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private StreamValueManagerDescriptor findManager(IStreamValueManager.MatchType matchType) {
        for (Map.Entry<StreamValueManagerDescriptor, IStreamValueManager.MatchType> entry : streamManagers.entrySet()) {
            if (entry.getValue() == matchType) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (streamEditor != null) {
            if (adapter.isAssignableFrom(streamEditor.getClass())) {
                return adapter.cast(streamEditor);
            }
            if (streamEditor instanceof IAdaptable) {
                return ((IAdaptable) streamEditor).getAdapter(adapter);
            }
        }
        return null;
    }

    private class ContentTypeSwitchAction extends Action implements SelectionListener {
        private Menu menu;

        ContentTypeSwitchAction() {
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
                managers.sort(Comparator.comparing(StreamValueManagerDescriptor::getLabel));
                for (StreamValueManagerDescriptor manager : managers) {
                    MenuItem item = new MenuItem(menu, SWT.RADIO);
                    item.setText(manager.getLabel());
                    item.setData(manager);
                    item.addSelectionListener(this);
                }
                MenuManager menuManager = new MenuManager();
                try {
                    streamEditor.contributeSettings(menuManager, editorControl);
                    for (IContributionItem item : menuManager.getItems()) {
                        item.fill(menu, -1);
                    }
                } catch (DBCException e) {
                    log.error(e);
                }
                toolBar.addDisposeListener(e -> menu.dispose());
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
                    Object itemData = item.getData();
                    if (itemData instanceof StreamValueManagerDescriptor) {
                        StreamValueManagerDescriptor newManager = (StreamValueManagerDescriptor) itemData;
                        if (newManager != curStreamManager) {
                            setStreamManager(newManager);
                        }
                    }
                }
            }
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {

        }
    }

    abstract class ContentLoaderService extends AbstractLoadService<DBDContent> {

        protected DBDContent content;

        protected ContentLoaderService(DBDContent content) {
            super("Load LOB value");
            this.content = content;
        }

        @Override
        public Object getFamily() {
            return valueController.getExecutionContext();
        }
    }

    private class ContentLoadVisualizer extends ProgressLoaderVisualizer<DBDContent> {
        protected Composite editPlaceholder;
        public ContentLoadVisualizer(ContentLoaderService loadingService, Composite parent) {
            super(loadingService, parent);
            this.editPlaceholder = parent;
        }

        @Override
        public void completeLoading(DBDContent result) {
            super.completeLoading(result);
            super.visualizeLoading();
        }
    }

    private class StreamManagerDetectService extends ContentLoaderService {

        public StreamManagerDetectService(DBDContent content) {
            super(content);
        }

        @Override
        public DBDContent evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Detect appropriate editor", 1);
            try {
                monitor.subTask("Load LOB value");
                streamManagers = ValueManagerRegistry.getInstance().getApplicableStreamManagers(monitor, valueController.getValueType(), content);
                String savedManagerId = valueToManagerMap.get(makeValueId());
                detectCurrentStreamManager(savedManagerId);
            } catch (Exception e) {
                valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
            } finally {
                monitor.done();
            }
            return content;
        }
    }


    private class StreamManagerDetectVisualizer extends ContentLoadVisualizer {
        public StreamManagerDetectVisualizer(StreamManagerDetectService loadingService, Composite parent) {
            super(loadingService, parent);
        }

        @Override
        public void completeLoading(DBDContent result) {
            super.completeLoading(result);
            // Clear placeholder
            for (Control child : this.editPlaceholder.getChildren()) {
                child.dispose();
            }
            // Create and layout new editor
            Control editorControl = createStreamManagerControl(this.editPlaceholder);
            this.editPlaceholder.layout(true);
            setControl(editorControl);
            try {
                primeEditorValue(result, false);
            } catch (Exception e) {
                valueController.showMessage(CommonUtils.notEmpty(e.getMessage()), DBPMessageType.ERROR);
            }
        }
    }


    private class StreamValueLoadService extends ContentLoaderService {

        public StreamValueLoadService(DBDContent content) {
            super(content);
        }

        @Override
        public DBDContent evaluate(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            monitor.beginTask("Detect appropriate editor", 1);
            try {
                monitor.subTask("Prime LOB value");
                streamEditor.primeEditorValue(monitor, control, content);
            } catch (Exception e) {
                valueController.showMessage(e.getMessage(), DBPMessageType.ERROR);
            } finally {
                monitor.done();
            }
            return content;
        }
    }


    private class StreamValueLoadVisualizer extends ContentLoadVisualizer {
        public StreamValueLoadVisualizer(StreamValueLoadService loadingService, Composite parent) {
            super(loadingService, parent);
        }

        @Override
        public void completeLoading(DBDContent result) {
            super.completeLoading(result);
        }
    }

}
