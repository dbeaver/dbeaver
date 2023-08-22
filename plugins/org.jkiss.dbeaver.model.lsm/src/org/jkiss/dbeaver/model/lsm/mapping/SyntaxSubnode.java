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
package org.jkiss.dbeaver.model.lsm.mapping;

import org.jkiss.code.NotNull;

import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(SyntaxSubnodes.class)
public @interface SyntaxSubnode {
    @NotNull
    SyntaxSubnodeLookupMode lookup() default SyntaxSubnodeLookupMode.EXACT;
    @NotNull
    String xpath() default "";
    @NotNull
    Class<? extends AbstractSyntaxNode> type() default AbstractSyntaxNode.class;
}
