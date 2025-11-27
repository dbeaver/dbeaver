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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.*;

public class DBNUtilsTest extends DBeaverUnitTest {

    private final List<String> changedProperties = new ArrayList<>();

    @After
    public void tearDown() {
        var prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        changedProperties.forEach(prefStore::setToDefault);
        changedProperties.clear();
    }

    @Test
    public void shouldNotSortByNameIfAlphabeticallyIfAlphabeticallyFalse() {
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, false);
        //then
        assertRemainUnsorted();
    }

    @Test
    public void shouldNotSortByNameIfAlphabeticallyFalseAndByFolderTrue() {
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, false);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, true);
        //then
        assertRemainUnsorted();
    }

    @Test
    public void shouldSortIgnoreCaseWhenIgnoreCaseTrue(){
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, true);
        //then
        assertCorrectSortingIgnoreCase(true);
        assertCorrectSortingIgnoreCase(false);
    }

    @Test
    public void shouldSortWithCaseWhenIgnoreCaseFalse(){
        // given
        addProperty(ModelPreferences.NAVIGATOR_SORT_IGNORE_CASE, false);
        //then
        assertCorrectSortingWithCase(true);
        assertCorrectSortingWithCase(false);
    }

    private void assertRemainUnsorted() {
        List<String> givenNames = List.of("b", "a", "A", "C");
        List<String> expectedNames = List.of("b", "a", "A", "C");
        // when
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(givenNames), true);
        // then
        assertEquals(expectedNames, Arrays.stream(result).map(DBNNode::getNodeDisplayName).toList());
    }


    private void assertCorrectSortingIgnoreCase(boolean isFoldersFirst) {
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY, true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, isFoldersFirst);

        assertCorrectSortingIgnoreCase(List.of("a", "A", "b", "C"), List.of("b", "a", "A", "C"));
        assertCorrectSortingIgnoreCase(List.of("s1", "s2", "s03", "s10"), List.of("s2", "s1", "s10", "s03"));
        assertCorrectSortingIgnoreCase(List.of("s1123456789123456789", "s2123456789123456789"), List.of("s2123456789123456789", "s1123456789123456789"));
    }

    private void assertCorrectSortingWithCase(boolean isFoldersFirst) {
        addProperty(ModelPreferences.NAVIGATOR_SORT_ALPHABETICALLY,  true);
        addProperty(ModelPreferences.NAVIGATOR_SORT_FOLDERS_FIRST, isFoldersFirst);

        assertCorrectSortingIgnoreCase(List.of("A", "C", "a", "b"), List.of("b", "a", "A", "C"));
        assertCorrectSortingIgnoreCase(List.of("s1", "s2", "s03", "s10"), List.of("s2", "s1", "s10", "s03"));
        assertCorrectSortingIgnoreCase(
            List.of("s1123456789123456789", "s2123456789123456789"),
            List.of("s2123456789123456789", "s1123456789123456789")
        );
    }

    private void assertCorrectSortingIgnoreCase(List<String> expectedNames, List<String> givenNames) {
        var result = DBNUtils.filterNavigableChildren(getNamedNodes(givenNames), true);
        // then
        assertEquals(expectedNames, Arrays.stream(result).map(DBNNode::getNodeDisplayName).toList());
    }


    private void addProperty(String key, boolean value) {
        var prefStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (!prefStore.contains(key)) {
            throw new IllegalArgumentException("No such property: " + key);
        }
        changedProperties.add(key);
        prefStore.setValue(key, value);
    }

    private DBNNode[] getNamedNodes(List<String> names) {
        return names
            .stream()
            .map(this::createMockNamedNode)
            .toArray(DBNNode[]::new);
    }

    private DBNNode createMockNamedNode(String name) {
        DBNNode node = mock(DBNNode.class, RETURNS_DEEP_STUBS);
        when(node.getNodeDisplayName()).thenReturn(name);
        return node;
    }
}
