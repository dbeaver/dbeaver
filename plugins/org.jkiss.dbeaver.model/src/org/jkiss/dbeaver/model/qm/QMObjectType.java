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
package org.jkiss.dbeaver.model.qm;

import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Object type
 */
public enum QMObjectType {

    session("Session", List.of(QMMetaObjectType.CONNECTION_INFO)),
    txn("Transactions", List.of(QMMetaObjectType.TRANSACTION_INFO, QMMetaObjectType.TRANSACTION_SAVEPOINT_INFO)),
    query("Queries", List.of());

    private final String title;
    private final List<QMMetaObjectType> types;

    QMObjectType(String title, List<QMMetaObjectType> types) {
        this.title = title;
        this.types = types;
    }

    public List<QMMetaObjectType> getTypes() {
        return types;
    }

    public String getTitle() {
        return title;
    }

    public static String toString(Collection<QMObjectType> objectTypes) {
        List<String> names = new ArrayList<>(objectTypes.size());
        for (QMObjectType type : objectTypes) {
            names.add(type.name());
        }
        return CommonUtils.makeString(names, ',');
    }

    public static Collection<QMObjectType> fromString(String str) {
        List<QMObjectType> objectTypes = new ArrayList<>();
        for (String otName : CommonUtils.splitString(str, ',')) {
            try {
                objectTypes.add(QMObjectType.valueOf(otName));
            } catch (IllegalArgumentException e) {
                // just scrip bad names
            }
        }
        return objectTypes;
    }
}
