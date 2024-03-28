/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public final class MessageBoxBuilder {
    private final MessageBoxModern dialog;

    @Nullable
    private List<Reply> replies;
    @Nullable
    private Reply defaultReply;

    private MessageBoxBuilder(@Nullable Shell shell) {
        dialog = new MessageBoxModern(shell);
    }

    public static MessageBoxBuilder builder(@Nullable Shell shell) {
        return new MessageBoxBuilder(shell);
    }

    public static MessageBoxBuilder builder() {
        return new MessageBoxBuilder(null);
    }

    // ----- Builder methods

    @NotNull
    public MessageBoxBuilder setTitle(@NotNull String title) {
        dialog.setTitle(title);
        return this;
    }

    @NotNull
    public MessageBoxBuilder setMessage(@NotNull String message) {
        dialog.setMessage(message);
        return this;
    }

    @NotNull
    public MessageBoxBuilder setPrimaryImage(@NotNull DBPImage image) {
        dialog.setPrimaryImage(image);
        return this;
    }

    @NotNull
    public MessageBoxBuilder setReplies(@NotNull Reply... replies) {
        this.replies = new ArrayList<>(Arrays.asList(replies));
        return this;
    }

    /**
     * Sets replies for the dialog. Replaces previously set replies.
     *
     * @param replies to set
     * @return builder
     */
    @NotNull
    public MessageBoxBuilder setReplies(@NotNull List<Reply> replies) {
        this.replies = new ArrayList<>(replies);
        return this;
    }

    @NotNull
    public MessageBoxBuilder setDefaultReply(@NotNull Reply defaultReply) {
        this.defaultReply = defaultReply;
        return this;
    }
    
    @NotNull
    public MessageBoxBuilder setCustomArea(@NotNull Consumer<? super Composite> customArea) {
        dialog.setCustomArea(customArea);
        return this;
    }

    /**
     * Set custom labels
     */
    @NotNull
    public MessageBoxBuilder setLabels(@NotNull String[] buttons) {
        dialog.setLabels(Arrays.asList(buttons));
        return this;
    }

    /**
     * Set default focus value
     */
    @NotNull
    public MessageBoxBuilder setDefaultFocus(int index) {
        dialog.setDefaultAnswerIdx(index);
        return this;
    }
    // -----

    @Nullable
    public Reply showMessageBox() {
        // create labels from replies, find default reply
        int defaultIdx = 0;
        if (replies != null) {
            List<String> labels = new ArrayList<>(replies.size());
            for (int i = 0; i < replies.size(); i++) {
                Reply reply = replies.get(i);
                if (reply == defaultReply) {
                    defaultIdx = i;
                }
                labels.add(reply != null ? reply.getDisplayString() : "[null]");
            }
            dialog.setLabels(labels);
        }
        if (replies != null) {
            dialog.setDefaultAnswerIdx(defaultIdx);
        }

        // Open dialog, detect reply
        int answerIdx = dialog.open();
        if (replies == null || !CommonUtils.isValidIndex(answerIdx, replies.size())) {
            return switch (dialog.getReturnCode()) {
                case IDialogConstants.OK_ID -> Reply.OK;
                case IDialogConstants.CANCEL_ID -> Reply.CANCEL;
                case IDialogConstants.NO_ID -> Reply.NO;
                default -> null;
            };
        }
        return replies.get(answerIdx);
    }
}
