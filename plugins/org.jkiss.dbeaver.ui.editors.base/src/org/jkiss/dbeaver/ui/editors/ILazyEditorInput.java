/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.ui.IEditorInput;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * An editor input that is initialized on demand.
 */
public interface ILazyEditorInput extends IEditorInput, IUnloadableEditorInput {

    @NotNull
    DBPProject getProject();

    /**
     * Initializes real editor input.
     *
     * @param monitor progress monitor
     * @return real editor input, or {@code null} if it can't be initialized
     * @throws DBException on any DB error
     */
    @Nullable
    IEditorInput initializeRealInput(@NotNull DBRProgressMonitor monitor) throws DBException;

    /**
     * Determines whether the real editor input can be initialized as
     * soon as the editor has received this lazy input as its input or not.
     *
     * @return {@code true} if the real input can be initialized right away, or {@code false} if not
     */
    boolean canLoadImmediately();
}
