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
package org.jkiss.dbeaver.ui.forms;

import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.jface.widgets.CompositeFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * Manages creation of UI controls for forms.
 */
final class UIControlFactory {
    private UIControlFactory() {
    }

    @NotNull
    static Composite createComposite(@NotNull Composite parent) {
        return CompositeFactory.newComposite(SWT.NONE)
            .create(parent);
    }

    @NotNull
    static Composite createTitledComposite(@NotNull Composite parent, @NotNull String text) {
        // FIXME: Bug! Since titled composite is implemented as two nested composites
        //        and the inner one is returned from the function to be populated,
        //        all layout operations that are supposed to be applied to the outer
        //        composite get applied to the inner one instead.
        return UIUtils.createTitledComposite(parent, text, 1);
    }

    @NotNull
    static ExpandableComposite createExpandableComposite(@NotNull Composite parent) {
        return UIUtils.createExpandableCompositeWithSeparator(parent, ExpandableComposite.CLIENT_INDENT, ExpandableComposite.TWISTIE);
    }

    @NotNull
    static Label createLabel(@NotNull Composite parent, int style, @NotNull String text) {
        return LabelFactory.newLabel(style)
            .text(text)
            .create(parent);
    }

    @NotNull
    static Text createText(@NotNull Composite parent, int style) {
        return TextFactory.newText(style)
            .create(parent);
    }

    @NotNull
    static Button createButton(@NotNull Composite parent, int style, @NotNull String text) {
        return ButtonFactory.newButton(style)
            .text(text)
            .create(parent);
    }

    @NotNull
    static Combo createCombo(@NotNull Composite parent, int style) {
        return new Combo(parent, style);
    }
}
