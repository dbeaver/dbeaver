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
package org.jkiss.dbeaver.model.cli.help;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.cli.CLIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

public class CLIHelp extends CommandLine.Help {
    private static final Log log = Log.getLog(CLIHelp.class);

    public CLIHelp(@NotNull CommandLine.Model.CommandSpec commandSpec, @NotNull ColorScheme colorScheme) {
        super(commandSpec, colorScheme);
    }


    @NotNull
    @Override
    public String optionListExcludingGroups(
        @NotNull List<CommandLine.Model.OptionSpec> optionList,
        @NotNull Layout layout,
        @Nullable Comparator<CommandLine.Model.OptionSpec> optionSort,
        @NotNull IParamLabelRenderer valueLabelRenderer
    ) {
        boolean isSubCommand = commandSpec().parent() != null;
        optionList = optionList.stream().filter(option -> {
            if (isSubCommand && CLIUtils.isGlobalOption(option)) {
                // skip global options for subcommand, they will be rendered in the help of the top level command
                return false;
            }
            return true;
        }).toList();
        return super.optionListExcludingGroups(optionList, layout, optionSort, valueLabelRenderer);
    }

    @NotNull
    public Comparator<CommandLine.Model.OptionSpec> createDefaultOptionSort() {
        Comparator<CommandLine.Model.OptionSpec> defaultSort = super.createDefaultOptionSort();
        return ((Comparator<CommandLine.Model.OptionSpec>) (o1, o2) -> {
            boolean o1Required = CLIUtils.isRequiredOption(o1);
            boolean o2Required = CLIUtils.isRequiredOption(o2);
            if (o1Required && !o2Required) {
                return -1;
            } else if (!o1Required && o2Required) {
                return 1;
            } else {
                return 0;
            }
        }).thenComparing(defaultSort);
    }

    @NotNull
    @Override
    public String footer(@NotNull Object... params) {
        String footer = super.footer(params);
        Object commandObject = commandSpec().userObject();
        if (commandObject != null && commandObject.getClass().isAnnotationPresent(CLIExample.class)) {
            CLIExample example = commandObject.getClass().getAnnotation(CLIExample.class);
            if (ArrayUtils.isEmpty(example.examples())) {
                return descriptionHeading(params);
            }
            CommandLine.Model.CommandSpec topLevelCommand = CLIUtils.findTopLevelCommand(commandSpec());
            StringBuilder exampleDescription = new StringBuilder("\nCommand examples:\n");
            for (String s : example.examples()) {
                exampleDescription.append(" - ")
                    //insert top level command dynamically, because command can be used in different applications
                    .append(topLevelCommand.name()).append(" ")
                    .append(s)
                    .append("\n");
            }
            footer = footer + exampleDescription;
        }
        return footer;
    }

    @NotNull
    @Override
    public IParamLabelRenderer parameterLabelRenderer() {
        return new CLIParameterRendererDelegate(super.parameterLabelRenderer());
    }

    private static class CLIParameterRendererDelegate implements IParamLabelRenderer {
        @NotNull
        private final IParamLabelRenderer delegate;

        public CLIParameterRendererDelegate(@NotNull IParamLabelRenderer delegate) {
            this.delegate = delegate;
        }

        @NotNull
        @Override
        public Ansi.Text renderParameterLabel(
            @NotNull CommandLine.Model.ArgSpec argSpec,
            @NotNull Ansi ansi,
            @NotNull List<Ansi.IStyle> styles
        ) {
            Ansi.Text label = delegate.renderParameterLabel(argSpec, ansi, styles);
            String argType = getArgType(argSpec);
            if (CommonUtils.isNotEmpty(argType)) {
                boolean insertIntoName = label.plainString().endsWith(">");
                if (insertIntoName) {
                    label = label.substring(0, label.getCJKAdjustedLength() - 1);
                }
                label = label.concat("(" + argType + ")" + (insertIntoName ? ">" : ""));
            }
            if (CLIUtils.isRequiredOption(argSpec)) {
                label = label.concat(" (required)");
            }

            return label;
        }

        @NotNull
        @Override
        public String separator() {
            return delegate.separator();
        }
    }

    @NotNull
    private static String getArgType(@NotNull CommandLine.Model.ArgSpec argSpec) {
        if (argSpec.userObject() instanceof Field field) {
            Class<?> fieldType = field.getType();
            String typeName = fieldType.getSimpleName();
            if (fieldType.isPrimitive() && fieldType.equals(int.class)) {
                //to resolve confusion with Integer and int name
                typeName = "integer";
            } else if (fieldType.isArray() && fieldType.getComponentType().equals(int.class)) {
                typeName = "integer[]";
            } else if (field.getGenericType() instanceof ParameterizedType pt) {
                Type argType = pt.getActualTypeArguments()[0];
                if (argType instanceof Class<?> typeClass) {
                    typeName = typeClass.getSimpleName();
                } else {
                    typeName = pt.getTypeName();
                }
                if (Collection.class.isAssignableFrom(fieldType)) {
                    typeName = typeName + "[]";
                }
            }
            return typeName.toLowerCase();
        }
        return "";
    }
}
