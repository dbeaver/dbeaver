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
package org.jkiss.dbeaver.ui.ai.model;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.validator.IntegerValidator;
import org.jkiss.utils.CommonUtils;

public class ContextWindowSizeField {
    @NotNull
    private final Text text;

    private Integer value;

    private ContextWindowSizeField(@NotNull Text text) {
        this.text = text;
        this.text.addModifyListener(e -> value = CommonUtils.toInteger(text.getText(), null));
    }

    @NotNull
    public static Builder builder() {
        return new Builder();
    }

    public void setValue(@Nullable Integer value) {
        this.text.setText(value == null ? "" : value.toString());
        this.value = value;
    }

    @Nullable
    public Integer getValue() {
        return value;
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
                "",
                SWT.BORDER
            );
            text.addVerifyListener(new IntegerValidator(1, Integer.MAX_VALUE));
            text.setLayoutData(gridData);

            return new ContextWindowSizeField(text);
        }
    }
}
