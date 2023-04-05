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
package org.jkiss.dbeaver.ui.data;

import org.eclipse.swt.widgets.Control;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.nio.file.Path;

/**
 * Value editor which provides external files
 * @param <CONTROL> editor control
 */
public interface IStreamValueEditorPersistent<CONTROL extends Control> extends IStreamValueEditor<CONTROL> {

    /**
     * returns path of the file used by editor
     *
     * @param control editor control
     * @return filepath of external file or null if not found
     */
    @Nullable
    Path getExternalFilePath(@NotNull CONTROL control);

}
