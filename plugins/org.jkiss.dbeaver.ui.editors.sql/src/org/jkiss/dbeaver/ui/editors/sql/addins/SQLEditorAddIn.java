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
package org.jkiss.dbeaver.ui.editors.sql.addins;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.io.PrintWriter;

/**
 * An instance of the particular SQL Editor Add-in being associated with an instance of the SQL Editor.
 */
public interface SQLEditorAddIn {
    
    /**
     * Initialize add-in and establish any initial interconnections with the editor instance.
     */
    void init(@NotNull SQLEditor editor);
    
    /**
     * Cleanup this instance of the add-in.
     */
    void cleanup(@NotNull SQLEditor editor);
    
    /**
     * Obtain PrintWriter to feed it with database server output if any.
     * Can be null, if add-in doesn't wish to consume server output.
     */
    @Nullable
    PrintWriter getServerOutputConsumer();
}
