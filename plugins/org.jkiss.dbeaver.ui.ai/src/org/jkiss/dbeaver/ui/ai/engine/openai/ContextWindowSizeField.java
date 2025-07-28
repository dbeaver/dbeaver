/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.ai.engine.openai;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;

public class ContextWindowSizeField {
    @NotNull
    private final Text text;

    private ContextWindowSizeField(@NotNull Text text) {
        this.text = text;
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public void setValue(@Nullable Integer value) {
        text.setText(value == null ? "" : value.toString());
    }

    @Nullable
    public Integer getValue() {
        return CommonUtils.toInteger(text.getText(), null);
    }

    public boolean isComplete() {
        return CommonUtils.toInt(text.getText(), 0) > 0;
    }

    public static class Builder {
        @NotNull
        private Composite parent;
        @NotNull
        private GridData gridData;

        public Builder withParent(@NotNull Composite parent) {
            this.parent = parent;
            return this;
        }

        public Builder withGridData(@NotNull GridData gridData) {
            this.gridData = gridData;
            return this;
        }

        public ContextWindowSizeField build() {
            Text text = UIUtils.createLabelText(
                parent,
                "Context window size",
                "Context window size in tokens",
                SWT.BORDER
            );
            text.addVerifyListener(UIUtils.getNumberVerifyListener(Locale.getDefault()));
            text.setLayoutData(gridData);

            return new ContextWindowSizeField(text);
        }
    }
}
