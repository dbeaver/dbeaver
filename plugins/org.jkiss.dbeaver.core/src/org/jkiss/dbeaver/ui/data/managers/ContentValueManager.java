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
package org.jkiss.dbeaver.ui.data.managers;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IContributionManager;
import org.eclipse.jface.action.Separator;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.preferences.DBPPropertyManager;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.IValueEditor;
import org.jkiss.dbeaver.ui.data.editors.ContentInlineEditor;
import org.jkiss.dbeaver.ui.data.editors.ContentPanelEditor;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.data.TextViewDialog;
import org.jkiss.dbeaver.ui.editors.content.ContentEditor;
import org.jkiss.dbeaver.utils.ContentUtils;

/**
 * JDBC Content value handler.
 * Handle LOBs, LONGs and BINARY types.
 *
 * @author Serge Rider
 */
public class ContentValueManager extends BaseValueManager {

    private static final Log log = Log.getLog(ContentValueManager.class);

    public static final String PROP_CATEGORY_CONTENT = "CONTENT";

    public static void contributeContentActions(@NotNull IContributionManager manager, @NotNull final IValueController controller, final IValueEditor activeEditor)
        throws DBCException
    {
        if (controller.getValue() instanceof DBDContent && !((DBDContent)controller.getValue()).isNull()) {
            manager.add(new Action(CoreMessages.model_jdbc_save_to_file_, DBeaverIcons.getImageDescriptor(UIIcon.SAVE_AS)) {
                @Override
                public void run() {
                    DialogUtils.saveToFile(controller);
                }
            });
        }
        manager.add(new Action(CoreMessages.model_jdbc_load_from_file_, DBeaverIcons.getImageDescriptor(UIIcon.LOAD)) {
            @Override
            public void run() {
                if (DialogUtils.loadFromFile(controller)) {
                    if (activeEditor != null) {
                        try {
                            activeEditor.primeEditorValue(controller.getValue());
                        } catch (DBException e) {
                            DBUserInterface.getInstance().showError("Load from file", "Error loading contents from file", e);
                        }
                    }
                }
            }
        });
        manager.add(new Separator());
    }

    public static IValueEditor openContentEditor(@NotNull IValueController controller)
    {
        Object value = controller.getValue();
        IValueController.EditType binaryEditType = IValueController.EditType.valueOf(
            controller.getExecutionContext().getDataSource().getContainer().getPreferenceStore().getString(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE));
        if (binaryEditType != IValueController.EditType.EDITOR && value instanceof DBDContentCached) {
            // Use string editor for cached content
            return new TextViewDialog(controller);
        } else if (value instanceof DBDContent) {
            return ContentEditor.openEditor(controller);
        } else {
            controller.showMessage(CoreMessages.model_jdbc_unsupported_content_value_type_, DBPMessageType.ERROR);
            return null;
        }
    }

    @Override
    public void contributeActions(@NotNull IContributionManager manager, @NotNull final IValueController controller, @Nullable IValueEditor activeEditor)
        throws DBCException
    {
        super.contributeActions(manager, controller, activeEditor);
        contributeContentActions(manager, controller, activeEditor);
    }

    @Override
    public void contributeProperties(@NotNull DBPPropertyManager propertySource, @NotNull IValueController controller)
    {
        super.contributeProperties(propertySource, controller);
        try {
            Object value = controller.getValue();
            if (value instanceof DBDContent) {
                propertySource.addProperty(
                    PROP_CATEGORY_CONTENT,
                    "content_type", //$NON-NLS-1$
                    CoreMessages.model_jdbc_content_type,
                    ((DBDContent)value).getContentType());
                final long contentLength = ((DBDContent) value).getContentLength();
                if (contentLength >= 0) {
                    propertySource.addProperty(
                        PROP_CATEGORY_CONTENT,
                        "content_length", //$NON-NLS-1$
                        CoreMessages.model_jdbc_content_length,
                        contentLength);
                }
            }
        }
        catch (Exception e) {
            log.warn("Can't extract CONTENT value information", e); //$NON-NLS-1$
        }
    }

    @NotNull
    @Override
    public IValueController.EditType[] getSupportedEditTypes() {
        return new IValueController.EditType[] {IValueController.EditType.PANEL, IValueController.EditType.EDITOR};
    }

    @Override
    public IValueEditor createEditor(@NotNull final IValueController controller)
        throws DBException
    {
        switch (controller.getEditType()) {
            case INLINE:
                // Open inline/panel editor
                if (controller.getValue() instanceof DBDContentCached &&
                    ContentUtils.isTextValue(((DBDContentCached)controller.getValue()).getCachedValue()))
            {
                    return new ContentInlineEditor(controller);
                } else {
                    return null;
                }
            case EDITOR:
                return openContentEditor(controller);
            case PANEL:
                return new ContentPanelEditor(controller);
            default:
                return null;
        }
    }

}
