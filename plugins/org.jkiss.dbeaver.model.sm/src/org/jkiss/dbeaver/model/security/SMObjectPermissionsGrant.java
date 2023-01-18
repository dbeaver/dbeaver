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
package org.jkiss.dbeaver.model.security;

import org.jkiss.dbeaver.model.security.user.SMObjectPermissions;

import java.util.HashSet;
import java.util.Set;

public class SMObjectPermissionsGrant {
    private final String subjectId;
    private final SMSubjectType subjectType;
    private final SMObjectPermissions objectPermissions;

    public SMObjectPermissionsGrant(String subjectId, SMSubjectType subjectType, SMObjectPermissions objectPermissions) {
        this.subjectId = subjectId;
        this.subjectType = subjectType;
        this.objectPermissions = objectPermissions;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public SMSubjectType getSubjectType() {
        return subjectType;
    }

    public SMObjectPermissions getObjectPermissions() {
        return objectPermissions;
    }

    public static Builder builder(String subjectId, SMSubjectType subjectType, String objectId) {
        return new Builder(subjectId, subjectType, objectId);
    }

    public static final class Builder {
        private final String subjectId;
        private final SMSubjectType subjectType;
        private final String objectId;
        private final Set<String> objectPermissions = new HashSet<>();

        public Builder(String subjectId, SMSubjectType subjectType, String objectId) {
            this.subjectId = subjectId;
            this.subjectType = subjectType;
            this.objectId = objectId;
        }

        public Builder addPermission(String permission) {
            this.objectPermissions.add(permission);
            return this;
        }

        public SMObjectPermissionsGrant build() {
            return new SMObjectPermissionsGrant(
                subjectId, subjectType,
                new SMObjectPermissions(objectId, Set.copyOf(objectPermissions))
            );
        }
    }
}
