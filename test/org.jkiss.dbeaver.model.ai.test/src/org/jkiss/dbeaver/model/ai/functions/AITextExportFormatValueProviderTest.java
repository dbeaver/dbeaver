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
package org.jkiss.dbeaver.model.ai.functions;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AITextExportFormatValueProviderTest {
    @Test
    void listsRegisteredTextFormatsSortedWithoutDuplicates() {
        DataTransferNodeDescriptor node = Mockito.mock(DataTransferNodeDescriptor.class);
        DataTransferProcessorDescriptor json = processor("json", false);
        DataTransferProcessorDescriptor csv = processor("csv", false);
        DataTransferProcessorDescriptor binary = processor("xlsx", true);
        Mockito.when(node.getProcessors()).thenReturn(new DataTransferProcessorDescriptor[] {json, binary, csv, json});

        Assertions.assertArrayEquals(new String[] {"csv", "json"}, provider(node).getValidValues());
        String description = provider(node).getValidValuesDescription();
        Assertions.assertNotNull(description);
        Assertions.assertTrue(description.contains("csv, json"));
        Assertions.assertFalse(description.contains("xlsx"));

        // The values reflect changes in the registry, rather than a cached format list.
        Mockito.when(node.getProcessors()).thenReturn(new DataTransferProcessorDescriptor[] {csv});
        Assertions.assertArrayEquals(new String[] {"csv"}, provider(node).getValidValues());
    }

    @Test
    void fileExportProviderStillIncludesBinaryFormats() {
        DataTransferNodeDescriptor node = Mockito.mock(DataTransferNodeDescriptor.class);
        DataTransferProcessorDescriptor csv = processor("csv", false);
        DataTransferProcessorDescriptor binary = processor("xlsx", true);
        Mockito.when(node.getProcessors()).thenReturn(new DataTransferProcessorDescriptor[] {binary, csv});
        AIExportFormatValueProvider provider = new AIExportFormatValueProvider() {
            @NotNull
            @Override
            protected DataTransferNodeDescriptor getNode() {
                return node;
            }
        };
        Assertions.assertArrayEquals(new String[] {"csv", "xlsx"}, provider.getValidValues());
    }

    @Test
    void fallsBackWhenNoTextFormatsAreAvailable() {
        Assertions.assertNull(provider(null).getValidValues());
        DataTransferNodeDescriptor node = Mockito.mock(DataTransferNodeDescriptor.class);
        Mockito.when(node.getProcessors()).thenReturn(new DataTransferProcessorDescriptor[0]);
        Assertions.assertNull(provider(node).getValidValues());
        DataTransferProcessorDescriptor binary = processor("xlsx", true);
        Mockito.when(node.getProcessors()).thenReturn(new DataTransferProcessorDescriptor[] {binary});
        Assertions.assertNull(provider(node).getValidValues());
    }

    @NotNull
    private static AITextExportFormatValueProvider provider(@Nullable DataTransferNodeDescriptor node) {
        return new AITextExportFormatValueProvider() {
            @Nullable
            @Override
            protected DataTransferNodeDescriptor getNode() {
                return node;
            }
        };
    }

    @NotNull
    private static DataTransferProcessorDescriptor processor(@NotNull String format, boolean binary) {
        DataTransferProcessorDescriptor processor = Mockito.mock(DataTransferProcessorDescriptor.class);
        Mockito.when(processor.getAIFormatId()).thenReturn(format);
        Mockito.when(processor.isBinaryFormat()).thenReturn(binary);
        return processor;
    }
}
