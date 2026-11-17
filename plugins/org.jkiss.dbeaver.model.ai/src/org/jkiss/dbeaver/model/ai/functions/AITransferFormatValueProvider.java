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
import org.jkiss.dbeaver.model.ai.AIFunctionParameterValueProvider;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AITransferFormatValueProvider implements AIFunctionParameterValueProvider {

    @Nullable
    @Override
    public String[] getValidValues() {
        String[] formats = getProcessors()
            .map(DataTransferProcessorDescriptor::getAIFormatId)
            .distinct()
            .toArray(String[]::new);
        return formats.length == 0 ? null : formats;
    }

    @Nullable
    @Override
    public String getValidValuesDescription() {
        List<DataTransferProcessorDescriptor> processors = getProcessors().toList();
        if (processors.isEmpty()) {
            return null;
        }
        String identifiers = processors.stream()
            .map(DataTransferProcessorDescriptor::getAIFormatId)
            .distinct()
            .collect(Collectors.joining(", "));
        String aliases = processors.stream()
            .filter(processor -> !processor.getAIFormatId().equalsIgnoreCase(processor.getName()))
            .map(processor -> "'" + processor.getAIFormatId() + "' selects '" + processor.getName() + "'")
            .collect(Collectors.joining(", "));
        String description = "Every registered " + getDirection() + " format identifier is supported. " +
            "When listing or offering formats, include every identifier: " + identifiers;
        return aliases.isEmpty() ? description : description + ". Format aliases: " + aliases;
    }

    @NotNull
    protected abstract String getDirection();

    @Nullable
    protected abstract DataTransferNodeDescriptor getNode();

    @NotNull
    protected Stream<DataTransferProcessorDescriptor> getProcessors() {
        DataTransferNodeDescriptor node = getNode();
        if (node == null) {
            return Stream.empty();
        }
        return Arrays.stream(node.getProcessors())
            .sorted(Comparator.comparing(DataTransferProcessorDescriptor::getAIFormatId));
    }
}
