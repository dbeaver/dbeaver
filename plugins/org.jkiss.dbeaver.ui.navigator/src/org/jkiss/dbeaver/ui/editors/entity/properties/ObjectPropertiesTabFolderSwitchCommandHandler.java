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

package org.jkiss.dbeaver.ui.editors.entity.properties;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.folders.TabbedFolderInfo;
import org.jkiss.utils.ArrayUtils;


public class ObjectPropertiesTabFolderSwitchCommandHandler extends AbstractHandler {
    
    public static final String NEXT_PAGE_COMMAND_ID = "org.jkiss.dbeaver.entity.propsTab.nextPage";
    public static final String PREV_PAGE_COMMAND_ID = "org.jkiss.dbeaver.entity.propsTab.prevPage";
    
    @Nullable
    @Override
    public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
        Integer indexDelta = NEXT_PAGE_COMMAND_ID.equals(event.getCommand().getId()) ? 1 : -1;
        IEditorPart editor = HandlerUtil.getActiveEditor(event);
        if (indexDelta != null && editor instanceof MultiPageEditorPart) {
            MultiPageEditorPart pagedEditor = (MultiPageEditorPart) editor;
            Object currentPage = pagedEditor.getSelectedPage();
            if (currentPage instanceof ObjectPropertiesEditor) {
                ObjectPropertiesEditor propsEditor = (ObjectPropertiesEditor) currentPage;
                TabbedFolderInfo[] foldersInfo = propsEditor.collectFolders(propsEditor);
                String activeFolderId = propsEditor.getActiveFolderId();
                int activeFolderIndex = ArrayUtils.indexOf(foldersInfo, f -> f.getId().equals(activeFolderId));
                int targetFolderIndex = (activeFolderIndex + indexDelta + foldersInfo.length) % foldersInfo.length;
                propsEditor.switchFolder(foldersInfo[targetFolderIndex].getId());
            }
        }
        return null;
    }
    
}
