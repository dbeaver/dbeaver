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
        @NotNull Consumer<PanelBuilder> handler
    ) {
        CTabItem item = new CTabItem(folder, SWT.NONE);
        item.setText(text);
        item.setControl(PanelBuilder.build(folder, handler));
    }

    @NotNull
    private static Consumer<PanelBuilder> buildShowcasePanel() {
        return pb -> pb
            .row(rb -> rb.group("Panel", buildPanelPanel()))
            .row(rb -> rb.group("Text", buildTextPanel()))
            .row(rb -> rb.group("Combo", buildComboPanel()))
            .row(rb -> rb.group("Check", buildCheckPanel()))
            .row(rb -> rb.group("Buttons", buildButtonPanel()));
    }

    @NotNull
    private static Consumer<PanelBuilder> buildControlsPanel() {
        return pb -> pb
            .row("label", rb -> rb.label("text"))
            .row("button", rb -> rb.button("text", RowBuilder.identityConsumer()))
            .row("radioButton", rb -> rb.radioButton("text", RowBuilder.identityConsumer()))
            .row("checkBox", rb -> rb.checkBox("text", RowBuilder.identityConsumer()))
            .row("textField", rb -> rb.textField(Observable.of("text")))
            .row("passwordField", rb -> rb.passwordField(Observable.of("text")))
            .row("intTextField", rb -> rb.intTextField(Observable.of(42)))
            .row("comboBox", rb -> rb.comboBox(List.of(42), Observable.of(42), String::valueOf))
            .row("comment", rb -> rb.comment("text"));
    }

    @NotNull
    private static Consumer<PanelBuilder> buildPanelPanel() {
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
                    .row(rb1 -> rb1.label("An expandable group label"))));
        // @formatter:on
    }

    @NotNull
    private static Consumer<PanelBuilder> buildTextPanel() {
        var nonBlank = Observable.of("An ugly little beast");
        var integer = Observable.of(1_000_000);
        var text = Observable.of("value");

        // @formatter:off
        return pb -> pb
            .row("Requires not blank", rb -> rb
                .textField(nonBlank, tb -> tb
                    .toModel(Validators.requireNotBlank(), RowBuilder.identityConverter())))
            .row("Requires an integer", rb -> rb
                .intTextField(integer))
            .row("Value", rb -> rb.textField(text))
            .indent(pb1 -> pb1
                .row("As uppercase", rb -> rb.textField(text, tb -> tb
                    .fromModel(String::toUpperCase)
                    .enabled(Observable.of(false))))
                .row("As lowercase", rb -> rb.textField(text, tb -> tb
                    .fromModel(String::toLowerCase)
                    .enabled(Observable.of(false)))));
        // @formatter:on
    }

    @NotNull
    private static Consumer<PanelBuilder> buildComboPanel() {
        enum Test1 {
            OPTION_A,
            OPTION_B,
            OPTION_C
        }

        var valueEnum = Observable.of(Test1.OPTION_B);
        var valueString = Observable.of("value");

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
    private static Consumer<PanelBuilder> buildCheckPanel() {
        var enabled1 = Observable.of(true);
        var enabled2 = Observable.of(true);
        var enabled3 = Observable.of(false);
        var enabled4 = Observable.of(false);
        var enabled5 = Observable.of(false);

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
                .textField(Observable.of(""), tb -> tb.enabled(enabled4))
                .checkBox("Enable super additional options", bb -> bb.selected(enabled5)))
            .row(rb -> rb
                .visible(enabled5)
                .textField(Observable.of("textField1"))
                .textField(Observable.of("textField2")));
        // @formatter:on
    }

    @NotNull
    private static Consumer<PanelBuilder> buildButtonPanel() {
        var enabled = Observable.of(false);

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
    private static Consumer<PanelBuilder> buildAiConfigurationPanel() {
        Consumer<PanelBuilder> general = pb -> pb
            .row("Language", rb -> rb.comboBox(List.of("English"), Observable.of("English")));

        Consumer<PanelBuilder> completion = pb -> pb
            .row(rb -> rb.checkBox("Include source in query comment", RowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Format SQL query", RowBuilder.identityConsumer()))
            .row(rb -> rb
                .label("Table join rule:")
                .comboBox(List.of("Default"), Observable.of("Default")))
            .row(rb -> rb.checkBox("Execute SQL immediately", RowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Enable AI query suggestion", RowBuilder.identityConsumer()));

        Consumer<PanelBuilder> execution = pb -> pb
            .row("Select:", rb -> rb.comboBox(List.of("Execute immediately"), Observable.of("Execute immediately")))
            .row("Modify:", rb -> rb.comboBox(List.of("Show confirmation"), Observable.of("Show confirmation")))
            .row("Schema:", rb -> rb.comboBox(List.of("Show confirmation"), Observable.of("Show confirmation")));

        Consumer<PanelBuilder> structure = pb -> pb
            .row(rb -> rb.checkBox("Send column data type information", RowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Send object description", RowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Send foreign keys information", RowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Send unique and primary keys information", RowBuilder.identityConsumer()));

        return pb -> pb
            .row(rb -> rb.group("General", general))
            .row(rb -> rb.group("Completion", completion))
            .row(rb -> rb.group("Execution", execution))
            .row(rb -> rb.group("Send database structure", structure))
            .row(rb -> rb.comment("This is a comment. :^)"));
    }

    @NotNull
    private static Consumer<PanelBuilder> buildGeneralPanel() {
        var checked = Observable.of(false);
        var maximumElementsShown = Observable.of(1000);
        var workbenchSaveInterval = Observable.of(5);

        // @formatter:off
        return pb -> pb
            .row(rb -> rb.checkBox("Always run in background", RowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Keep next/previous editor, view and perspectives dialog open", RowBuilder.identityConsumer()))
            .row(rb -> rb.checkBox("Show heap status", RowBuilder.identityConsumer()))
            .row(rb -> rb
                .label("Initial maximum number of elements shown in views:")
                .intTextField(maximumElementsShown, tb -> tb.align(AlignX.FILL)))
            .row(rb -> rb.checkBox("Rename resource inline if available", RowBuilder.identityConsumer()))
            .row(rb -> rb
                .label("Workbench save interval (in minutes):")
                .intTextField(workbenchSaveInterval, tb -> tb.align(AlignX.FILL)))
            .row(rb -> rb
                .group("Open mode", pb1 -> pb1
                    .row(rb1 -> rb1.radioButton("Double click", RowBuilder.identityConsumer()))
                    .row(rb1 -> rb1.radioButton("Single click", bb -> bb
                        .selected(checked)))
                    .indent(pb2 -> pb2
                        .row(rb1 -> rb1
                            .enabled(checked)
                            .checkBox("Select on hover", RowBuilder.identityConsumer()))
                        .row(rb1 -> rb1
                            .enabled(checked)
                            .checkBox("Open when using arrow keys", RowBuilder.identityConsumer())))
                    .row(rb1 -> rb1.label("Note: This preference may not take effect on all views"))));
        // @formatter:on
    }
}
