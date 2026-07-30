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
package org.jkiss.dbeaver.ext.polardbx.sql;

import org.eclipse.core.runtime.IAdapterFactory;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.text.parser.TPRuleProvider;

/**
 * Adapter factory to bind PolarDBX dialect to TPRuleProvider.
 */
public class PolarDBXDialectAdapterFactory implements IAdapterFactory {

    private static final Class<?>[] ADAPTERS = new Class[] { TPRuleProvider.class };

    @Override
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adaptableObject instanceof SQLDialect && adapterType == TPRuleProvider.class) {
            return adapterType.cast(new PolarDBXDialectRules());
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return ADAPTERS;
    }
}