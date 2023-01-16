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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPProject;

public enum RMProjectType {

    GLOBAL("g"),
    SHARED("s"),
    USER("u");

    private final String prefix;

    RMProjectType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    /**
     * Helps to find RMProjectType by prefix
     *
     * @param prefix project prefix char
     * @return RMProjectType by prefix
     */
    public static RMProjectType getByPrefix(@NotNull String prefix) {
        for (RMProjectType value : values()) {
            if (value.getPrefix().equals(prefix)) {
                return value;
            }
        }
        // Default value
        return USER;
    }

    public static String getPlainProjectId(DBPProject project) {
        String id = project.getId();
        if (id.length() > 2 && id.charAt(1) == '_') {
            char typeC = id.charAt(0);
            if (typeC == 'g' || typeC == 's' || typeC == 'u') {
                return id.substring(2);
            }
        }
        return id;
    }
}
