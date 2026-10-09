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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.code.NotNull;

import java.util.function.BooleanSupplier;

// eclipse invokes paste reflectively, so the widget class must be public
public final class AIPromptStyledText extends StyledText {
    private final BooleanSupplier pasteImages;

    public AIPromptStyledText(@NotNull Composite parent, @NotNull BooleanSupplier pasteImages) {
        super(parent, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        this.pasteImages = pasteImages;
    }

    @Override
    public void paste() {
        if (!isFocusControl() || !pasteImages.getAsBoolean()) {
            super.paste();
        }
    }
}
