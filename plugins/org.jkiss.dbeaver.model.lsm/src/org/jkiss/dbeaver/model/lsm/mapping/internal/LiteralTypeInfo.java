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

import javax.xml.xpath.XPathExpression;
import java.util.Map;

public class LiteralTypeInfo {
    public final String ruleName;
    public final Class<?> type;
    public final XPathExpression stringExpr;
    public final Map<Object, XPathExpression> exprByValue;
    public final Map<String, Object> valuesByName;
    public final boolean isCaseSensitive;
    
    public LiteralTypeInfo(
        @NotNull String ruleName,
        @NotNull Class<?> type,
        @NotNull XPathExpression stringExpr,
        @NotNull Map<Object, XPathExpression> exprByValue,
        @NotNull Map<String, Object> valuesByName,
        boolean isCaseSensitive
    ) {
        this.ruleName = ruleName;
        this.type = type;
        this.stringExpr = stringExpr;
        this.exprByValue = exprByValue;
        this.valuesByName = valuesByName;
        this.isCaseSensitive = isCaseSensitive;
    }        
}
