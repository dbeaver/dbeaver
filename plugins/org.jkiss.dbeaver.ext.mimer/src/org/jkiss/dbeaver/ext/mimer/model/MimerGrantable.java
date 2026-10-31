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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;

/**
 * Implemented by every Mimer SQL privilege-grant model class - both {@link
 * AbstractMimerObjectPrivilege} (one fixed privilege type per class) and {@link
 * AbstractMimerMultiTypePrivilege} (a selectable privilege type per instance) - so a single
 * shared manager base ({@link org.jkiss.dbeaver.ext.mimer.edit.AbstractMimerPrivilegeManager})
 * can drive create/drop for either family without needing to know which one it's working with.
 *
 * @author Mimer Information Technology
 */
public interface MimerGrantable {

    @NotNull
    String buildGrantDDL();

    @NotNull
    String buildRevokeDDL();
}
