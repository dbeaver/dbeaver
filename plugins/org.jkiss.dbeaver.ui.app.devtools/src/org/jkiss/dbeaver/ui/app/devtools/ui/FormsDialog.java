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
package org.jkiss.dbeaver.ui.app.devtools.ui;

import org.eclipse.jface.dialogs.TrayDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.forms.*;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class FormsDialog extends TrayDialog {
    public FormsDialog(@NotNull Shell shell) {
        super(shell);
    }

    @NotNull
    @Override
    protected Control createDialogArea(@NotNull Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new FillLayout());

        CTabFolder folder = new CTabFolder(composite, SWT.TOP | SWT.FLAT);
        createFolderTab(folder, "Showcase", buildShowcasePanel());
        createFolderTab(folder, "Controls", buildControlsPanel());
        createFolderTab(folder, "Pref - General", buildGeneralPanel());
        createFolderTab(folder, "Pref - AI", buildAiConfigurationPanel());
        folder.setSelection(0);

        return composite;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    private static void createFolderTab(
        @NotNull CTabFolder folder,
        @NotNull String text,
        @NotNull Consumer<UIPanelBuilder> handler
    ) {
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(text);
        item.setControl(UIPanelBuilder.build(folder, handler));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildShowcasePanel() {
        return pb -> pb
            .row(rb -> rb.group("Panel", buildPanelPanel()))
            .row(rb -> rb.group("Text", buildTextPanel()))
            .row(rb -> rb.group("Combo", buildComboPanel()))
            .row(rb -> rb.group("Check", buildCheckPanel()))
            .row(rb -> rb.group("Buttons", buildButtonPanel()));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildControlsPanel() {
        return pb -> pb
            .row("label", rb -> rb.label("text"))
            .row("button", rb -> rb.button("text", UIRowBuilder.identityConsumer()))
            .row("radioButton", rb -> rb.radioButton("text", UIRowBuilder.identityConsumer()))
            .row("checkBox", rb -> rb.checkBox("text", UIRowBuilder.identityConsumer()))
            .row("textField", rb -> rb.textField(UIObservable.of("text")))
            .row("passwordField", rb -> rb.passwordField(UIObservable.of("text")))
            .row("intTextField", rb -> rb.intTextField(UIObservable.of(42)))
            .row("comboBox", rb -> rb.comboBox(List.of(42), UIObservable.of(42), String::valueOf));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildPanelPanel() {
        // @formatter:off
        return pb -> pb
            .row("Regular row", rb -> rb.label("A label"))
            .indent(pb1 -> pb1
                .row("Indented row", rb -> rb.label("An indented label"))
                .indent(pb2 -> pb2
                    .row("Indented row", rb -> rb.label("A doubly indented label"))))
            .row(rb -> rb
                .group("A named group", pb1 -> pb1
                    .row(rb1 -> rb1.label("A group label"))))
            .row(rb -> rb
                .expandableGroup("An expandable group", true, pb1 -> pb1
                    .align(UIAlignX.FILL)
                    .row(rb1 -> rb1.label("An expandable group label"))));
        // @formatter:on
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildTextPanel() {
        var nonBlank = UIObservable.of("An ugly little beast");
        var integer = UIObservable.of(1_000_000);
        var text = UIObservable.of("value");

        // @formatter:off
        return pb -> pb
            .row("Requires not blank", rb -> rb
                .textField(nonBlank, tb -> tb
                    .toModel(UIValidators.requireNotBlank(), Function.identity())))
            .row("Requires an integer", rb -> rb
                .intTextField(integer))
            .row("Value", rb -> rb.textField(text))
            .indent(pb1 -> pb1
                .row("As uppercase", rb -> rb.textField(text, tb -> tb
                    .fromModel(String::toUpperCase)
                    .enabled(UIObservable.of(false))))
                .row("As lowercase", rb -> rb.textField(text, tb -> tb
                    .fromModel(String::toLowerCase)
                    .enabled(UIObservable.of(false)))));
        // @formatter:on
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildComboPanel() {
        enum Test1 {
            OPTION_A,
            OPTION_B,
            OPTION_C
        }

        var valueEnum = UIObservable.of(Test1.OPTION_B);
        var valueString = UIObservable.of("value");

        // @formatter:off
        return pb -> pb
            .row("Combo that wraps an enum", rb -> rb
                .comboBox(valueEnum, Enum::toString))
            .row("Combo that wraps an enum (custom converter)", rb -> rb
                .comboBox(valueEnum, value -> value.name().toLowerCase(Locale.ROOT)))
            .row("Combo using a list of values", rb -> rb
                .comboBox(List.of("value", "other value", "THIRD VALUE"), valueString));
        // @formatter:on
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildCheckPanel() {
        var enabled1 = UIObservable.of(true);
        var enabled2 = UIObservable.of(true);
        var enabled3 = UIObservable.of(false);
        var enabled4 = UIObservable.of(false);
        var enabled5 = UIObservable.of(false);

        // @formatter:off
        return pb -> pb
            .row(rb -> rb
                .checkBox("Enable second check", bb -> bb.enabled(enabled1).selected(enabled2))
                .checkBox("Enable first check", bb -> bb.enabled(enabled2).selected(enabled1)))
            .row(rb -> rb
                .checkBox("Enable additional options", bb -> bb.selected(enabled3)))
            .row(rb -> rb
                .enabled(enabled3)
                .checkBox("Enable textField", bb -> bb.selected(enabled4))
                .textField(UIObservable.of(""), tb -> tb.enabled(enabled4))
                .checkBox("Enable super additional options", bb -> bb.selected(enabled5)))
            .row(rb -> rb
                .visible(enabled5)
                .textField(UIObservable.of("textField1"))
                .textField(UIObservable.of("textField2")));
        // @formatter:on
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildButtonPanel() {
        var enabled = UIObservable.of(false);

        // @formatter:off
        return pb -> pb
            .row(rb -> rb
                .button(
                    "Toggle second",
                    e -> enabled.set(!enabled.get()))
                .button(
                    "Show message",
                    e -> UIUtils.showMessageBox(UIUtils.getActiveShell(), "Hello", "Hello from forms", SWT.ICON_INFORMATION),
                    bb -> bb.enabled(enabled)));
        // @formatter:on
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildAiConfigurationPanel() {
        Consumer<UIPanelBuilder> general = pb -> pb
            .row("Language", rb -> rb.comboBox(List.of("English"), UIObservable.of("English")));

        Consumer<UIPanelBuilder> completion = pb -> pb
            .row(rb -> rb.checkBox("Include source in query comment", UIRowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Format SQL query", UIRowBuilder.identityConsumer()))
            .row(rb -> rb
                .label("Table join rule:")
                .comboBox(List.of("Default"), UIObservable.of("Default")))
            .row(rb -> rb.checkBox("Execute SQL immediately", UIRowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Enable AI query suggestion", UIRowBuilder.identityConsumer()));

        Consumer<UIPanelBuilder> execution = pb -> pb
            .row("Select:", rb -> rb.comboBox(List.of("Execute immediately"), UIObservable.of("Execute immediately")))
            .row("Modify:", rb -> rb.comboBox(List.of("Show confirmation"), UIObservable.of("Show confirmation")))
            .row("Schema:", rb -> rb.comboBox(List.of("Show confirmation"), UIObservable.of("Show confirmation")));

        Consumer<UIPanelBuilder> structure = pb -> pb
            .row(rb -> rb.checkBox("Send column data type information", UIRowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Send object description", UIRowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Send foreign keys information", UIRowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Send unique and primary keys information", UIRowBuilder.identityConsumer()));

        return pb -> pb
            .row(rb -> rb.group("General", general))
            .row(rb -> rb.group("Completion", completion))
            .row(rb -> rb.group("Execution", execution))
            .row(rb -> rb.group("Send database structure", structure));
    }

    @NotNull
    private static Consumer<UIPanelBuilder> buildGeneralPanel() {
        var checked = UIObservable.of(false);
        var maximumElementsShown = UIObservable.of(1000);
        var workbenchSaveInterval = UIObservable.of(5);

        // @formatter:off
        return pb -> pb
            .row(rb -> rb.checkBox("Always run in background", UIRowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Keep next/previous editor, view and perspectives dialog open", UIRowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Show heap status", UIRowBuilder.identityConsumer()))
            .row(rb -> rb
                .label("Initial maximum number of elements shown in views:")
                .intTextField(maximumElementsShown, tb -> tb.align(UIAlignX.FILL)))
            .row(rb -> rb.checkBox("Rename resource inline if available", UIRowBuilder.identityConsumer()))
            .row(rb -> rb
                .label("Workbench save interval (in minutes):")
                .intTextField(workbenchSaveInterval, tb -> tb.align(UIAlignX.FILL)))
            .row(rb -> rb
                .group("Open mode", pb1 -> pb1
                    .row(rb1 -> rb1.radioButton("Double click", UIRowBuilder.identityConsumer()))
                    .row(rb1 -> rb1.radioButton("Single click", bb -> bb
                        .selected(checked)))
                    .indent(pb2 -> pb2
                        .row(rb1 -> rb1
                            .enabled(checked)
                            .checkBox("Select on hover", UIRowBuilder.identityConsumer()))
                        .row(rb1 -> rb1
                            .enabled(checked)
                            .checkBox("Open when using arrow keys", UIRowBuilder.identityConsumer())))
                    .row(rb1 -> rb1.label("Note: This preference may not take effect on all views"))));
        // @formatter:on
    }
}
