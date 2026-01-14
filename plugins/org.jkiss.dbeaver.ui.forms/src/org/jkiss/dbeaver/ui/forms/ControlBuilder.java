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

import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.NotNull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The builder for a control.
 */
public sealed interface ControlBuilder<B extends ControlBuilder<B>>
    permits ControlBuilder.ButtonBuilder, ControlBuilder.ComboBuilder, ControlBuilder.CommentBuilder, ControlBuilder.LabelBuilder,
    ControlBuilder.TextBuilder, ControlBuilderImpl, PanelBuilder {

    @NotNull
    B visible(@NotNull Observable<Boolean> binding);

    @NotNull
    B enabled(@NotNull Observable<Boolean> binding);

    @NotNull
    B tooltip(@NotNull String value);

    /**
     * The control becomes resizable and occupies all available
     * <i>horizontal</i> space. For multiple resizable controls, the
     * extra space is equally divided between them.
     *
     * @return this builder
     */
    @NotNull
    B grow();

    @NotNull
    B align(@NotNull AlignX x, @NotNull AlignY y);

    @NotNull
    B align(@NotNull AlignX x);

    @NotNull
    B align(@NotNull AlignY y);

    @NotNull
    B hint(int width, int height);

    @NotNull
    B accept(@NotNull Consumer<? super B> consumer);

    /**
     * The builder for a label control.
     */
    sealed interface LabelBuilder extends ControlBuilder<LabelBuilder> permits ControlBuilderImpl.LabelBuilderImpl {
    }

    /**
     * The builder for a text control.
     */
    sealed interface TextBuilder<T> extends ControlBuilder<TextBuilder<T>> permits ControlBuilderImpl.TextBuilderImpl {
        @NotNull
        TextBuilder<T> toModel(
            @NotNull Function<? super String, IStatus> afterGetValidator,
            @NotNull Function<? super String, ? extends T> targetToModelConverter
        );

        @NotNull
        TextBuilder<T> fromModel(
            @NotNull Function<? super T, String> modelToTargetConverter
        );
    }

    /**
     * The builder for a button control.
     */
    sealed interface ButtonBuilder extends ControlBuilder<ButtonBuilder> permits ControlBuilderImpl.ButtonBuilderImpl {
        @NotNull
        ButtonBuilder selected(@NotNull Observable<Boolean> binding);
    }

    /**
     * The builder for a combo control.
     */
    sealed interface ComboBuilder<T> extends ControlBuilder<ComboBuilder<T>> permits ControlBuilderImpl.ComboBuilderImpl {
    }

    /**
     * The builder for a comment control.
     */
    sealed interface CommentBuilder extends ControlBuilder<CommentBuilder> permits ControlBuilderImpl.CommentBuilderImpl {
    }
}
