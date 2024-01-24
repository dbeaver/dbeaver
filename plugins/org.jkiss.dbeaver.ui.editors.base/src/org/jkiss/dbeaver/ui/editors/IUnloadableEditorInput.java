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

/**
 * An editor input can can be unloaded back to its lazy variant.
 */
public interface IUnloadableEditorInput extends IEditorInput {

    /**
     * Creates a new editor input that represent this editor input
     * but in a lazy variant so it can be loaded on demand later.
     *
     * @return a new editor input instance
     */
    @NotNull
    ILazyEditorInput unloadInput();
}
