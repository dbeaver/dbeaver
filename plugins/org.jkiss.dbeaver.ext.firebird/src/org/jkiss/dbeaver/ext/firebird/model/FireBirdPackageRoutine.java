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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Procedure or function of a Firebird package. Its source is part of the package source.
 */
public class FireBirdPackageRoutine implements DBSObject {

    @NotNull
    private final FireBirdPackage firebirdPackage;
    @NotNull
    private final String name;
    private final boolean function;
    private final boolean privateRoutine;
    @Nullable
    private final String description;

    public FireBirdPackageRoutine(
        @NotNull FireBirdPackage firebirdPackage,
        @NotNull String name,
        boolean function,
        boolean privateRoutine,
        @Nullable String description
    ) {
        this.firebirdPackage = firebirdPackage;
        this.name = name;
        this.function = function;
        this.privateRoutine = privateRoutine;
        this.description = description;
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    /**
     * True for a function, false for a procedure.
     */
    @Property(viewable = true, order = 2)
    public boolean isFunction() {
        return function;
    }

    /**
     * True if the routine is declared in the package body only and can't be called from outside the package.
     */
    @Property(viewable = true, order = 3)
    public boolean isPrivate() {
        return privateRoutine;
    }

    @Nullable
    @Override
    @Property(viewable = true, length = PropertyLength.MULTILINE, order = 10)
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public FireBirdPackage getParentObject() {
        return firebirdPackage;
    }

    @NotNull
    @Override
    public FireBirdDataSource getDataSource() {
        return firebirdPackage.getDataSource();
    }

    @Override
    public String toString() {
        return name;
    }
}
