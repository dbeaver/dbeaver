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
<<<<<<<< HEAD:plugins/org.jkiss.dbeaver.model.ai/src/org/jkiss/dbeaver/model/ai/AIFunctionVerifier.java
package org.jkiss.dbeaver.model.ai;
========
package org.jkiss.dbeaver.ui.controls.resultset.panel.grouping.column;
>>>>>>>> 9a0b97bb94 (#40537 web request timeouts configuration):plugins/org.jkiss.dbeaver.ui.editors.data/src/org/jkiss/dbeaver/ui/controls/resultset/panel/grouping/column/GroupingFunctionColumn.java

import org.jkiss.code.NotNull;

/**
<<<<<<<< HEAD:plugins/org.jkiss.dbeaver.model.ai/src/org/jkiss/dbeaver/model/ai/AIFunctionVerifier.java
 * AI function verifier.
 */
public interface AIFunctionVerifier {

    enum FunctionState {
        APPLICABLE,
        NOT_APPLICABLE,
        AUTO_CALL
    }

    @NotNull
    FunctionState getFunctionState(
        @NotNull AIFunctionContext context,
        @NotNull AIFunctionDescriptor function
    );

========
 * GroupingFunction column. Column represents some grouping action
 */
public interface GroupingFunctionColumn extends GroupingColumn {

    @NotNull
    String getColumnExpression();

    boolean isShowToUser();
>>>>>>>> 9a0b97bb94 (#40537 web request timeouts configuration):plugins/org.jkiss.dbeaver.ui.editors.data/src/org/jkiss/dbeaver/ui/controls/resultset/panel/grouping/column/GroupingFunctionColumn.java
}
