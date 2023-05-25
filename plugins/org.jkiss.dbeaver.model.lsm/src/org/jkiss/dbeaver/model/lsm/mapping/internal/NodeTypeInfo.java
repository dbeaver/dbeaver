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
package org.jkiss.dbeaver.model.lsm.mapping.internal;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.lsm.mapping.AbstractSyntaxNode;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;


public class NodeTypeInfo {
    public final String ruleName;
    public final Class<? extends AbstractSyntaxNode> type;
    public final Constructor<? extends AbstractSyntaxNode> ctor;
    public final Map<String, NodeFieldInfo> fields;

    public NodeTypeInfo(
        @NotNull String ruleName,
        @NotNull Class<? extends AbstractSyntaxNode> type,
        @NotNull Constructor<? extends AbstractSyntaxNode> ctor,
        @NotNull Map<String, NodeFieldInfo> fields
    ) {
        this.ruleName = ruleName;
        this.type = type;
        this.ctor = ctor;
        this.fields = Collections.unmodifiableMap(fields);
    }

    @NotNull
    public Collection<NodeFieldInfo> getFields() {
        return this.fields.values();
    }     
}
