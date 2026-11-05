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
import org.jkiss.dbeaver.ui.bindings.ObservableValue;
import org.jkiss.dbeaver.ui.forms.UIObservable;
import org.jkiss.dbeaver.ui.forms.UIObservableList;
import org.jkiss.dbeaver.ui.forms.UIObservables;

import java.util.ArrayList;
import java.util.List;

public class ObservableValueTest {
    public static void main(String[] args) {
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
        if (!changes.equals(List.of("second -> unused", "unused -> updated"))) {
            throw new AssertionError("Unexpected derived changes: " + changes);
        }

        var listener = new java.util.function.BiConsumer<String, String>() {
            @Override
            public void accept(String oldValue, String newValue) {
                changes.add("source " + newValue);
            }
        };
        first.addListener(listener);
        first.removeListener(listener);
        first.set("final");
        if (changes.stream().anyMatch(s -> s.startsWith("source "))) {
            throw new AssertionError("Removed listener was called");
        }

        var items = UIObservableList.copyOf(List.of("first", "second"), String.class);
        items.subList(0, 1).clear();
        items.listIterator().add("new");
        if (!items.equals(List.of("new", "second"))) {
            throw new AssertionError("Unexpected list contents: " + items);
        }

        var model = UIObservable.of("left");
        var selectedValue = UIObservables.equals(model, "right");
        var visible = UIObservables.and(UIObservable.of(true), selectedValue);
        var visibleChanges = new ArrayList<Boolean>();
        visible.addChangeListener((oldValue, newValue) -> visibleChanges.add(newValue));
        selectedValue.set(true);
        model.set("left");
        if (!visibleChanges.equals(List.of(true, false)) || selectedValue.get()) {
            throw new AssertionError("Derived forms observables did not follow the model: " + visibleChanges);
        }
    }
}
