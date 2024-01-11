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
package org.jkiss.dbeaver.model.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPAttributeReferencePurpose;

/**
 * Context-specific entity attribute
 */
public interface DBSContextBoundAttribute extends DBSEntityAttribute {
    /**
     * Makes a string addressing the attribute with respect to the context

     * @param isIncludeContainerName true to include container name, otherwise false
     * @param containerAliasOrNull data container name to use during formatting
       ({@code null} to substitute the explicit data container name, or explicit non-empty string)
     * @param purpose to specify in which part of query this reference will be used
     * @return attribute reference as a string (for example, {@code "(compositecolumn).datatypefield"})
     */
    @NotNull
    String formatMemberReference(
            boolean isIncludeContainerName,
            @Nullable String containerAliasOrNull,
            @NotNull DBPAttributeReferencePurpose purpose
    );
}
