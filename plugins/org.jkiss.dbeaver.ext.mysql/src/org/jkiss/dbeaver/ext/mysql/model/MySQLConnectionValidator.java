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
package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.AbstractJDBCConnectionValidator;

import java.util.List;

/** Enforces restricted parameters for MySQL data sources, allowing explicitly disabled boolean options. */
final class MySQLConnectionValidator extends AbstractJDBCConnectionValidator {
    private static final List<String> BOOLEAN_PROPERTIES = List.of(
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfile", "allowUrlInLocalInfile"
    );
    private static final List<String> PROHIBITED_PROPERTIES = List.of(
        "autoDeserialize", "allowLocalInfile", "allowLoadLocalInfileInPath", "allowLoadLocalInfile",
        "allowUrlInLocalInfile", "propertiesTransform", "useConfigs", "credentialType"
    );

    @NotNull
    @Override
    protected List<String> getRestrictedProperties() {
        return PROHIBITED_PROPERTIES;
    }

    @Override
    protected boolean isRestrictedPropertyAllowed(@NotNull Object name, @NotNull Object value) {
        return name instanceof String propertyName
            && value instanceof String propertyValue
            && ("false".equalsIgnoreCase(propertyValue) || "no".equalsIgnoreCase(propertyValue))
            && BOOLEAN_PROPERTIES.stream().anyMatch(propertyName::equalsIgnoreCase);
    }
}
