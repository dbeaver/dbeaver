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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorPart;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.editors.json.JSONTextEditor;
import org.jkiss.dbeaver.ui.editors.json.internal.JSONEditorMessages;

/**
 * JSON text editor
 */
public class JSONEditorPart extends JSONTextEditor implements IEditorPart {

    public JSONEditorPart() {

    }

    @Override
    public String getTitle()
    {
        return JSONEditorMessages.JSONEditorPart_title;
    }

    @Override
    public Image getTitleImage()
    {
        return DBeaverIcons.getImage(DBIcon.TYPE_JSON);
    }

}
