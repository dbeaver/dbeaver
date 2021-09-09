/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.erd.model;

import org.jkiss.code.NotNull;

public class ERDAttributeSettings {
    private final ERDAttributeVisibility visibility;
    private final boolean alphabeticalOrder;

    public ERDAttributeSettings(@NotNull ERDAttributeVisibility visibility, boolean alphabeticalOrder) {
        this.visibility = visibility;
        this.alphabeticalOrder = alphabeticalOrder;
    }

    @NotNull
    public ERDAttributeVisibility getVisibility() {
        return visibility;
    }

    public boolean isAlphabeticalOrder() {
        return alphabeticalOrder;
    }
}
