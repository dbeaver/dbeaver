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
package org.jkiss.dbeaver.ui.ai.chat.controls;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.layout.FillLayoutFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.ImageTransfer;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPartSite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.ai.AIChatConversation;
import org.jkiss.dbeaver.model.ai.AIChatListener;
import org.jkiss.dbeaver.model.ai.AIChatMessage;
import org.jkiss.dbeaver.model.ai.AIImageAttachment;
import org.jkiss.dbeaver.model.ai.AIMessageType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.*;
import org.jkiss.dbeaver.ui.ai.chat.AIChatController;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatIcons;
import org.jkiss.dbeaver.ui.ai.chat.internal.AIChatMessagesUI;
import org.jkiss.dbeaver.ui.editors.TextEditorUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PromptComposite extends Composite {

    // A stroke used to submit the prompt
    private final AIChatControl chat;
    private final StyledText promptText;
    private final Button sendButton;
    private final Button attachButton;
    private final AIImageAttachmentsComposite imageAttachments;
    private final List<AIImageAttachment> images = new ArrayList<>();
    private final Map<UUID, Draft> drafts = new HashMap<>();
    private UUID draftConversationId;
    private final Map<UUID, Integer> loadingImages = new HashMap<>();

    public PromptComposite(@NotNull AIChatControl chat, @NotNull Composite parent) {
        super(parent, SWT.NONE);
        this.chat = chat;
        draftConversationId = chat.getActiveConversation().getId();

        final IWorkbenchPartSite site = chat.getController().getSite();

        chat.getChatSession().addListener(new AIChatListener() {
            @Override
            public void conversationChanged(@NotNull AIChatConversation conversation) {
                UIUtils.asyncExec(() -> {
                    restoreDraft(conversation.getId());
                });
            }

            @Override
            public void busyChanged(boolean busy) {
                if (chat.isDisposed()) {
                    return;
                }
                UIUtils.asyncExec(() -> {
                    if (isDisposed()) {
                        return;
                    }
                    imageAttachments.setEnabled(!busy);
                    promptText.setEnabled(!busy);
                    sendButton.setEnabled(!busy && !isLoadingImages());
                    attachButton.setEnabled(!busy);

                    if (!busy) {
                        sendButton.setToolTipText(AIChatMessagesUI.ai_chat_send_button_tip);
                        sendButton.setImage(DBeaverIcons.getImage(AIChatIcons.SEND));
                        setFocusOnPrompt();
                    } else {
                        new AbstractUIJob("Enable AI cancellation") {
                            @NotNull
                            @Override
                            protected IStatus runInUIThread(@NotNull DBRProgressMonitor monitor) {
                                if (!isDisposed() && chat.getActiveConversation().isActive()) {
                                    sendButton.setImage(null);
                                    sendButton.setImage(DBeaverIcons.getImage(UIIcon.CLOSE));
                                    sendButton.setToolTipText(AIChatMessagesUI.ai_chat_cancel_button_tip);
                                    sendButton.setEnabled(true);
                                }
                                return Status.OK_STATUS;
                            }
                        }.schedule(250);
                    }
                });
            }
        });

        setLayout(GridLayoutFactory.fillDefaults().margins(1, 1).numColumns(3).create());
        imageAttachments = new AIImageAttachmentsComposite(this, index -> {
            if (!chat.getChatSession().isBusy()) {
                images.remove(index);
                refreshImages();
            }
        });
        GridData attachmentData = new GridData(SWT.FILL, SWT.TOP, true, false, 3, 1);
        attachmentData.exclude = true;
        imageAttachments.setLayoutData(attachmentData);
        imageAttachments.setVisible(false);
        Composite leftControls = UIUtils.createComposite(this, 1);
        String shortcut1 = ActionUtils.findCommandDescription(AIChatController.CMD_ATTACH, site, true);
        attachButton = UIUtils.createPushButton(
            leftControls,
            null,
            CommonUtils.isEmpty(shortcut1) ? AIChatMessagesUI.ai_chat_attach_button_tip
                : AIChatMessagesUI.ai_chat_attach_button_tip + " (" + shortcut1 + ")",
            AIChatIcons.ATTACH,
            SelectionListener.widgetSelectedAdapter(e -> ActionUtils.runCommand(
                AIChatController.CMD_ATTACH,
                site
            ))
        );

        Composite promptBorder = new Composite(this, SWT.NONE);
        promptBorder.setLayoutData(new GridData(GridData.FILL_BOTH));
        promptBorder.setLayout(FillLayoutFactory.fillDefaults().margins(2, 2).create());

        promptText = new AIPromptStyledText(promptBorder, this::pasteImages);
        promptText.setLayoutData(new GridData(GridData.FILL_BOTH));
        promptText.addKeyListener(new PromptKeyAdapter(chat));
        chat.enableDragAndDrop(promptText);
        promptText.addTraverseListener(e -> {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                e.doit = true;
                if (e.detail == SWT.TRAVERSE_TAB_PREVIOUS) {
                    UIUtils.asyncExec(chat::setFocusOnMessages);
                }
            }
        });
        promptText.getAccessible().addAccessibleListener(new AccessibleAdapter() {
            @Override
            public void getName(AccessibleEvent e) {
                e.result = AIChatMessagesUI.ai_chat_a11y_prompt_name;
            }
        });
        updatePromptEditorSize();
        promptText.addModifyListener(e -> updatePromptEditorSize());

        TextEditorUtils.enableHostEditorKeyBindingsSupport(site, promptText);
        String sendShortcut = CommonUtils.notEmpty(ActionUtils.findCommandDescription(
            AIChatController.CMD_SEND_PROMPT,
            site,
            true
        ));
        UIUtils.addEmptyTextHint(promptText, text -> NLS.bind(AIChatMessagesUI.ai_chat_prompt_text_hint, sendShortcut));

        new CompositeBorderPainter(promptBorder);

        Composite rightControls = UIUtils.createComposite(this, 1);
        {
            // Toolbar
            Composite buttonsBar = UIUtils.createComposite(rightControls, 1);
            buttonsBar.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
            String shortcut = ActionUtils.findCommandDescription(AIChatController.CMD_SEND_PROMPT, site, true);
            sendButton = UIUtils.createPushButton(
                buttonsBar,
                null,
                CommonUtils.isEmpty(shortcut) ? AIChatMessagesUI.ai_chat_send_button_tip
                    : AIChatMessagesUI.ai_chat_send_button_tip + " (" + shortcut + ")",
                AIChatIcons.SEND,
                SelectionListener.widgetSelectedAdapter(e -> submitPrompt())
            );
        }

        setTabList(new Control[]{promptBorder, leftControls, rightControls, imageAttachments});
    }

    @NotNull
    public List<AIImageAttachment> getImages() {
        return List.copyOf(images);
    }

    public void addImages(@NotNull List<AIImageAttachment> attachments) {
        if (!chat.getChatSession().isBusy()) {
            addImages(draftConversationId, attachments);
        }
    }

    public void addImages(@NotNull UUID conversationId, @NotNull List<AIImageAttachment> attachments) {
        if (isDisposed()) {
            return;
        }
        boolean currentDraft = conversationId.equals(draftConversationId);
        Draft draft = drafts.getOrDefault(conversationId, new Draft("", List.of()));
        List<AIImageAttachment> targetImages = currentDraft ? images : draft.images();
        int bytes = targetImages.stream().mapToInt(image -> image.data().length()).sum()
            + attachments.stream().mapToInt(image -> image.data().length()).sum();
        if (targetImages.size() + attachments.size() > AIImageAttachment.MAX_IMAGES
            || bytes > (AIImageAttachment.MAX_IMAGE_BYTES + 2) / 3 * 4) {
            DBWorkbench.getPlatformUI().showError(AIChatMessagesUI.ai_chat_image_error, AIChatMessagesUI.ai_chat_image_limit);
            return;
        }
        if (currentDraft) {
            images.addAll(attachments);
            refreshImages();
            setFocusOnPrompt();
        } else {
            List<AIImageAttachment> combined = new ArrayList<>(targetImages);
            combined.addAll(attachments);
            drafts.put(conversationId, new Draft(draft.text(), List.copyOf(combined)));
        }
    }

    public boolean isLoadingImages() {
        return loadingImages.getOrDefault(chat.getActiveConversation().getId(), 0) > 0;
    }

    public void imageLoadingStarted(@NotNull UUID conversationId) {
        loadingImages.merge(conversationId, 1, Integer::sum);
        updateSendButton();
    }

    public void imageLoadingFinished(@NotNull UUID conversationId) {
        loadingImages.computeIfPresent(conversationId, (id, count) -> count > 1 ? count - 1 : null);
        updateSendButton();
    }

    private void updateSendButton() {
        if (!chat.getChatSession().isBusy()) {
            sendButton.setEnabled(!isLoadingImages());
        }
    }

    private void restoreDraft(@NotNull UUID conversationId) {
        if (isDisposed()) {
            return;
        }
        if (draftConversationId.equals(conversationId)) {
            return;
        }
        drafts.put(draftConversationId, new Draft(getPromptText(), List.copyOf(images)));
        draftConversationId = conversationId;
        Draft draft = drafts.getOrDefault(draftConversationId, new Draft("", List.of()));
        setPromptText(draft.text());
        images.clear();
        images.addAll(draft.images());
        refreshImages();
        updateSendButton();
    }

    public void draftSubmitted() {
        setPromptText("");
        images.clear();
        drafts.remove(draftConversationId);
        refreshImages();
    }

    private void refreshImages() {
        imageAttachments.setImages(images);
        ((GridData) imageAttachments.getLayoutData()).exclude = images.isEmpty();
        imageAttachments.setVisible(!images.isEmpty());
        getParent().layout(true, true);
    }

    private boolean pasteImages() {
        if (chat.getChatSession().isBusy()) {
            return false;
        }
        Clipboard clipboard = new Clipboard(getDisplay());
        try {
            // finder also offers file icons as clipboard images
            Object files = clipboard.getContents(FileTransfer.getInstance());
            if (files instanceof String[] paths && paths.length > 0) {
                List<Path> imageFiles = java.util.Arrays.stream(paths).map(Path::of).filter(AIChatControl::isImageFile).toList();
                if (!imageFiles.isEmpty()) {
                    chat.attachFiles(imageFiles);
                    return true;
                }
                return false;
            }
            Object png = clipboard.getContents(AIImageClipboardTransfer.INSTANCE);
            if (png instanceof byte[] bytes) {
                chat.attachImage(AIImageAttachment.fromBytes(AIChatMessagesUI.ai_chat_image_clipboard_name, bytes));
                return true;
            }
            Object contents = clipboard.getContents(ImageTransfer.getInstance());
            if (contents instanceof ImageData image) {
                chat.attachImage(image);
                return true;
            }
        } catch (Exception exception) {
            DBWorkbench.getPlatformUI().showError(
                AIChatMessagesUI.ai_chat_image_error, AIChatMessagesUI.ai_chat_image_load_error, exception);
            return true;
        } finally {
            clipboard.dispose();
        }
        return false;
    }

    private record Draft(@NotNull String text, @NotNull List<AIImageAttachment> images) { }

    public void submitPrompt() {
        if (chat.getActiveConversation().isActive()) {
            // Cancel?
            chat.cancelPrompt();
        } else if (!isLoadingImages()) {
            String text = getPromptText();
            chat.submitPrompt(text);
        }
    }

    private void updatePromptEditorSize() {
        int textLength = promptText.getCharCount();
        int lineHeight = promptText.getLineCount() * promptText.getLineHeight();
        Rectangle textBounds = textLength <= 0 ?
            new Rectangle(0, 0, 0, 0) :
            promptText.getTextBounds(0, textLength - 1);
        if (textBounds.height > lineHeight) {
            lineHeight = textBounds.height;
        }
        int maxHeight = UIUtils.getFontHeight(promptText) * 10;
        int minHeight = promptText.getLineHeight() * 2;
        final int height = Math.max(
            Math.min(maxHeight, lineHeight),
            minHeight) + 4;
        final GridData data = (GridData) promptText.getParent().getLayoutData();

        if (data.heightHint != height) {
            data.heightHint = height;

            final Composite container = getParent();
            container.setRedraw(false);
            container.layout(true, true);
            container.setRedraw(true);
        }
    }

    @NotNull
    public String getPromptText() {
        if (promptText.isDisposed()) {
            return "";
        }
        return promptText.getText();
    }

    public void setPromptText(@NotNull String text) {
        if (promptText.isDisposed()) {
            return;
        }
        promptText.setText(text);
        promptText.setCaretOffset(text.length());
        updatePromptEditorSize();
    }

    public boolean setFocusOnPrompt() {
        boolean focused = promptText.setFocus();
        promptText.notifyListeners(SWT.Modify, new Event());
        return focused;
    }

    private class PromptKeyAdapter extends KeyAdapter {
        private final AIChatControl chat;

        public PromptKeyAdapter(@NotNull AIChatControl chat) {
            this.chat = chat;
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.keyCode == SWT.ARROW_UP && promptText.getText().isBlank()) {
                AIChatConversation conversation = chat.getActiveConversation();
                List<AIChatMessage> messages = conversation.getMessages();
                for (int i = messages.size() - 1; i >= 0; i--) {
                    AIChatMessage message = messages.get(i);
                    if (message.message().getRole() == AIMessageType.USER) {
                        String oldPrompt = message.message().getContent();
                        promptText.setText(oldPrompt);
                        promptText.setSelection(oldPrompt.length());
                        break;
                    }
                }

                e.doit = false;
            } else if (e.keyCode == SWT.ESC) {
                // ?
            }
        }
    }

}
