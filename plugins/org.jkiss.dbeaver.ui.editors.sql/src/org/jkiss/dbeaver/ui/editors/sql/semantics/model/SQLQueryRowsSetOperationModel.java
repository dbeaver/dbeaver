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
package org.jkiss.dbeaver.ui.editors.sql.semantics.model;

import org.antlr.v4.runtime.misc.Interval;
import org.jkiss.code.NotNull;

public abstract class SQLQueryRowsSetOperationModel extends SQLQueryRowsSourceModel {
    protected final SQLQueryRowsSourceModel left;
    protected final SQLQueryRowsSourceModel right;

    public SQLQueryRowsSetOperationModel(@NotNull Interval range, @NotNull SQLQueryRowsSourceModel left, @NotNull SQLQueryRowsSourceModel right) {
        super(range);
        this.left = left;
        this.right = right;
    }

    @NotNull
    public SQLQueryRowsSourceModel getLeft() {
        return left;
    }

    @NotNull
    public SQLQueryRowsSourceModel getRight() {
        return right;
    }
}
