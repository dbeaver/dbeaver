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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectConfigurator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UITask;

import java.util.Map;

/**
 * Collects all attributes of a new Mimer SQL object before it is created - the
 * open-page/apply-changes/return-or-cancel flow shared by nearly every "Create ..." configurator
 * in this plugin, previously copy-pasted verbatim across ~30 classes differing only in which
 * page/object type they name. A concrete subclass only needs {@link #createPage} (which page to
 * open, and how to build it from the pending object and its container). Not every configurator
 * fits this shape - {@code MimerIndexConfigurator} builds its object from several independent
 * per-column choices with no single {@code applyChanges()} call, so it stays a standalone {@link
 * DBEObjectConfigurator} instead.
 *
 * @author Mimer Information Technology
 */
public abstract class AbstractMimerConfigurator<T extends DBSObject> implements DBEObjectConfigurator<T> {

    @Nullable
    @Override
    public final T configureObject(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBECommandContext commandContext,
        @Nullable Object container,
        @NotNull T object,
        @NotNull Map<String, Object> options
    ) {
        return UITask.run(() -> {
            MimerCreatePage page = createPage(object, container);
            if (!page.edit()) {
                return null;
            }
            page.applyChanges();
            return object;
        });
    }

    /**
     * Builds the create-dialog page for this object type. {@code container} is the object's
     * intended parent (a schema, table, ident, ...) - most pages ignore it (the pending object's
     * own constructor already carries its owner), but a few (e.g. {@code
     * MimerCreateSystemPrivilegePage}) need it directly.
     */
    @NotNull
    protected abstract MimerCreatePage createPage(@NotNull T object, @Nullable Object container);
}
