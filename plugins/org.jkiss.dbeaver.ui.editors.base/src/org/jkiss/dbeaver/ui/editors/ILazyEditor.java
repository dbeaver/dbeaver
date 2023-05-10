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
package org.jkiss.dbeaver.ui.editors;

import org.eclipse.ui.IEditorPart;

/**
 * An editor that can unload its own editor input so it can be initialized on demand later.
 */
public interface ILazyEditor extends IEditorPart {

    /**
     * Attempts to load an editor input.
     *
     * @return {@code true} if the editor input was loaded successfully, or {@code false} if it can't be done
     */
    boolean loadEditorInput();

    /**
     * Attempts to unload an editor input.
     *
     * @return {@code true} if the editor input was unloaded successfully, or {@code false} if it can't be done
     */
    boolean unloadEditorInput();
}
