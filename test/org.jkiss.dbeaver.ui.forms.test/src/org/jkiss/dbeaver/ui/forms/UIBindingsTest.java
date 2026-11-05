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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ui.bindings.BindingContext;
import org.jkiss.dbeaver.ui.bindings.ObservableValue;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class UIBindingsTest extends DBeaverUnitTest {
    @Test
    public void convertedTextUpdatesDoNotFeedBackIntoModel() {
        var context = new BindingContext();
        var model = ObservableValue.writable("value");
        Text source = bindText(context, model, Function.identity());
        Text uppercase = bindText(context, model, value -> value.toUpperCase(Locale.ROOT));
        Text lowercase = bindText(context, model, value -> value.toLowerCase(Locale.ROOT));
        Assertions.assertEquals("value", model.get());
        Mockito.clearInvocations(source);

        // A user edit must update both derived fields without rewriting the source (and its caret).
        source.setText("vaLueX");

        Assertions.assertEquals("vaLueX", model.get());
        Assertions.assertEquals("VALUEX", uppercase.getText());
        Assertions.assertEquals("valuex", lowercase.getText());
        Mockito.verify(source, Mockito.times(1)).setText(Mockito.anyString());

        model.set("External");
        Assertions.assertEquals("External", source.getText());
        Assertions.assertEquals("EXTERNAL", uppercase.getText());
        Assertions.assertEquals("external", lowercase.getText());
        Assertions.assertEquals("External", model.get());
        context.dispose();
    }

    @Test
    public void integerEditsAreNotReformattedBackIntoSource() {
        var context = new BindingContext();
        var model = ObservableValue.writable(12);
        Text control = mockText();
        context.bind(control, model, value -> control.setText(value.toString()),
            () -> Integer.valueOf(control.getText()), SWT.Modify);

        control.setText("0123");

        Assertions.assertEquals(123, model.get());
        Assertions.assertEquals("0123", control.getText());
        model.set(456);
        Assertions.assertEquals("456", control.getText());
        context.dispose();
    }

    @NotNull
    private static Text bindText(
        @NotNull BindingContext context,
        @NotNull ObservableValue<String> model,
        @NotNull Function<String, String> converter
    ) {
        Text control = mockText();
        context.bind(control, model, value -> {
            String display = converter.apply(value);
            if (!control.getText().equals(display)) {
                control.setText(display);
            }
        }, control::getText, SWT.Modify);
        return control;
    }

    @NotNull
    private static Text mockText() {
        Text control = Mockito.mock(Text.class);
        var text = new AtomicReference<>("");
        var listener = new AtomicReference<Listener>();
        Mockito.when(control.getText()).thenAnswer(invocation -> text.get());
        Mockito.doAnswer(invocation -> {
            listener.set(invocation.getArgument(1));
            return null;
        }).when(control).addListener(Mockito.eq(SWT.Modify), Mockito.any(Listener.class));
        // Model SWT's synchronous Modify notification, including programmatic setText calls.
        Mockito.doAnswer(invocation -> {
            text.set(invocation.getArgument(0));
            if (listener.get() != null) {
                listener.get().handleEvent(new Event());
            }
            return null;
        }).when(control).setText(Mockito.anyString());
        return control;
    }

    @Test
    public void computedValuesFollowActiveDependencies() {
        var selected = ObservableValue.writable(false);
        var first = ObservableValue.writable("first");
        var second = ObservableValue.writable("second");
        var derived = ObservableValue.computed(() -> selected.get() ? first.get() : second.get());
        var changes = new ArrayList<String>();
        derived.addListener((oldValue, newValue) -> changes.add(oldValue + " -> " + newValue));

        first.set("unused");
        selected.set(true);
        second.set("unused");
        first.set("updated");

        Assertions.assertEquals(List.of("second -> unused", "unused -> updated"), changes);
    }

    @Test
    public void removedListenerDoesNotReceiveChanges() {
        var value = ObservableValue.writable("first");
        var changes = new ArrayList<String>();
        BiConsumer<String, String> listener = (oldValue, newValue) -> changes.add(newValue);
        value.addListener(listener);
        value.removeListener(listener);
        value.set("second");

        Assertions.assertEquals(List.of(), changes);
    }

    @Test
    public void listMutationsNotifyObservers() {
        var items = UIObservableListImpl.of(List.of("first", "second"), String.class);
        var snapshots = new ArrayList<List<String>>();
        items.addListener(() -> snapshots.add(List.copyOf(items)));

        items.subList(0, 1).clear();
        items.listIterator().add("new");

        Assertions.assertEquals(List.of(List.of("second"), List.of("new", "second")), snapshots);
    }

    @Test
    public void derivedFormsObservablesFollowModel() {
        var model = UIObservable.of("left");
        var selected = UIObservables.equals(model, "right");
        var visible = UIObservables.and(UIObservable.of(true), selected);
        var changes = new ArrayList<Boolean>();
        visible.addChangeListener((oldValue, newValue) -> changes.add(newValue));

        selected.set(true);
        model.set("left");

        Assertions.assertEquals(List.of(true, false), changes);
        Assertions.assertFalse(selected.get());
    }
}
