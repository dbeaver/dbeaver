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
package org.jkiss.dbeaver.ext.mimer.ui.config;

import org.jkiss.dbeaver.ui.editors.object.struct.BaseObjectEditPage;

/**
 * Implemented by every Mimer SQL "Create ..." wizard page so {@link AbstractMimerConfigurator}
 * can drive the shared open-page/apply-changes flow without knowing which concrete page or object
 * type it's working with. {@link #edit()} is already satisfied for free by every page's inherited
 * {@link BaseObjectEditPage#edit()} - only {@link #applyChanges()} needs a real per-page
 * implementation, which every page here already has.
 *
 * @author Mimer Information Technology
 */
public interface MimerCreatePage {

    /**
     * @return {@code true} if the user confirmed the dialog, {@code false} if they canceled -
     * same contract as {@link BaseObjectEditPage#edit()}.
     */
    boolean edit();

    /**
     * Applies the collected values to the object passed to this page's constructor. Call only
     * after {@link #edit()} returns {@code true}.
     */
    void applyChanges();
}
