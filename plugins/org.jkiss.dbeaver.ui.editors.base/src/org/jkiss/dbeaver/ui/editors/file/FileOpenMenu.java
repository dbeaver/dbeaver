/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.file;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.file.FileOpenHandler;
import org.jkiss.dbeaver.model.file.FileOpenPreferences;
import org.jkiss.dbeaver.model.file.FileTypeAction;
import org.jkiss.dbeaver.model.file.FileTypeHandlerDescriptor;
import org.jkiss.dbeaver.model.file.FileTypeHandlerRegistry;
import org.jkiss.dbeaver.model.navigator.fs.DBNPathBase;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.internal.EditorsMessages;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Explicit opening actions share the same availability rules as normal file opening.
 */
public class FileOpenMenu {
    private static final Log log = Log.getLog(FileOpenMenu.class);

    public static void contribute(@NotNull IMenuManager manager, @NotNull DBNPathBase node) {
        if (node.isDirectory() || node.isLocked()) {
            return;
        }
        Path path = node.getPath();
        FileTypeHandlerDescriptor handler = FileTypeHandlerRegistry.getInstance().findHandler(
            CommonUtils.notEmpty(IOUtils.getFileExtension(path)).toLowerCase(Locale.ROOT));
        if (handler == null) {
            return;
        }
        List<FileTypeAction> actions;
        try {
            FileOpenHandler fileOpenHandler = handler.createHandler();
            fileOpenHandler.setSourceNode(node);
            actions = FileOpenPreferences.getAvailableActions(fileOpenHandler.supportedActions());
        } catch (DBException e) {
            log.debug("Can't load file opening actions", e);
            return;
        }
        if (actions.isEmpty()) {
            return;
        }
        var store = DBWorkbench.getPlatform().getPreferenceStore();
        var preferences = new FileOpenPreferences(store, handler.getId());
        FileTypeAction savedAction = preferences.getSavedAction(actions);
        MenuManager menu = new MenuManager(EditorsMessages.file_open_as);
        for (FileTypeAction action : actions) {
            menu.add(new Action(FileActionSelectorDialog.getActionLabel(action), Action.AS_RADIO_BUTTON) {
                {
                    setChecked(action == savedAction);
                }

                @Override
                public void run() {
                    if (!isChecked()) {
                        return;
                    }
                    if (preferences.getSavedAction(actions) != null) {
                        preferences.setSavedAction(action);
                        PrefUtils.savePreferenceStore(store);
                    }
                    EditorUtils.openFileEditors(new Path[]{path}, null, new ArrayList<>(), false, node, action);
                }
            });
        }
        menu.add(new Separator());
        menu.add(new Action(EditorsMessages.file_open_always_ask, Action.AS_CHECK_BOX) {
            {
                setChecked(savedAction == null);
            }

            @Override
            public void run() {
                preferences.setSavedAction(isChecked() ? null : actions.getFirst());
                PrefUtils.savePreferenceStore(store);
            }
        });
        manager.add(menu);
    }
}
