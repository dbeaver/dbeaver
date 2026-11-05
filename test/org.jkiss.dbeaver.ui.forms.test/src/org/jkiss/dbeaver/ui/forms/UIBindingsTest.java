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

import org.jkiss.dbeaver.ui.bindings.ObservableValue;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class UIBindingsTest extends DBeaverUnitTest {
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
