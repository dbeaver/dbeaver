/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.link.*;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorBase;
import org.jkiss.dbeaver.ui.editors.sql.syntax.SQLPartitionScanner;
import org.jkiss.dbeaver.ui.editors.sql.templates.SQLTemplatesPage;

import java.util.ArrayList;
import java.util.List;

public class SQLSymbolInserter implements VerifyKeyListener, ILinkedModeListener {

    static protected final Log log = Log.getLog(SQLSymbolInserter.class);

    private boolean closeSingleQuotes = true;
    private boolean closeDoubleQuotes = true;
    private boolean closeBrackets = true;

    private final String CATEGORY = toString();
    private IPositionUpdater positionUpdater = new ExclusivePositionUpdater(CATEGORY);
    private List<SymbolLevel> bracketLevelStack = new ArrayList<>();
    private SQLEditorBase editor;
    private ISourceViewer sourceViewer;

    public SQLSymbolInserter(SQLEditorBase editor)
    {
        this.editor = editor;
        this.sourceViewer = editor.getViewer();
    }

    public void setCloseSingleQuotesEnabled(boolean enabled)
    {
        closeSingleQuotes = enabled;
    }

    public void setCloseDoubleQuotesEnabled(boolean enabled)
    {
        closeDoubleQuotes = enabled;
    }

    public void setCloseBracketsEnabled(boolean enabled)
    {
        closeBrackets = enabled;
    }

    private boolean hasIdentifierToTheRight(IDocument document, int offset)
    {
        try {
            int end = offset;
            IRegion endLine = document.getLineInformationOfOffset(end);
            int maxEnd = endLine.getOffset() + endLine.getLength();
            while (end != maxEnd && Character.isWhitespace(document.getChar(end)))
                ++end;

            return end != maxEnd && Character.isJavaIdentifierPart(document.getChar(end));

        }
        catch (BadLocationException e) {
            // be conservative
            log.debug(e);
            return true;
        }
    }

    private boolean hasIdentifierToTheLeft(IDocument document, int offset)
    {
        try {
            IRegion startLine = document.getLineInformationOfOffset(offset);
            int minStart = startLine.getOffset();
            return offset != minStart && Character.isJavaIdentifierPart(document.getChar(offset - 1));

        }
        catch (BadLocationException e) {
            log.debug(e);
            return true;
        }
    }

    private boolean hasCharacterToTheRight(IDocument document, int offset, char character)
    {
        try {
            int end = offset;
            IRegion endLine = document.getLineInformationOfOffset(end);
            int maxEnd = endLine.getOffset() + endLine.getLength();
            while (end != maxEnd && Character.isWhitespace(document.getChar(end))) {
                ++end;
            }

            return end != maxEnd && document.getChar(end) == character;

        }
        catch (BadLocationException e) {
            log.debug(e);
            // be conservative
            return true;
        }
    }

    @Override
    public void verifyKey(VerifyEvent event)
    {

        if (!event.doit) {
            return;
        }

        IDocument document = sourceViewer.getDocument();

        final Point selection = sourceViewer.getSelectedRange();
        final int offset = selection.x;
        final int length = selection.y;

        switch (event.character) {
            case '(':
            case '[':
                if (!closeBrackets) {
                    return;
                }
                if (hasCharacterToTheRight(document, offset + length, event.character)) {
                    return;
                }

                // fall through

            case '\'':
                if (event.character == '\'') {
                    if (!closeSingleQuotes) {
                        return;
                    }
                    if (hasIdentifierToTheLeft(document, offset)
                        || hasIdentifierToTheRight(document, offset + length)) {
                        return;
                    }
                }

                // fall through

            case '"':
                if (event.character == '"') {
                    if (!closeDoubleQuotes) {
                        return;
                    }
                    if (hasIdentifierToTheLeft(document, offset)
                        || hasIdentifierToTheRight(document, offset + length)) {
                        return;
                    }
                }

                try {
                    ITypedRegion partition = TextUtilities.getPartition(
                        document,
                        SQLPartitionScanner.SQL_PARTITIONING,
                        offset,
                        true);
                    if (!IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType())
                        && partition.getOffset() != offset) {
                        return;
                    }

                    if (!editor.validateEditorInputState()) {
                        return;
                    }

                    final char character = event.character;
                    final char closingCharacter = getPeerCharacter(character);

                    document.replace(offset, length, String.valueOf(character) + closingCharacter);

                    SymbolLevel level = new SymbolLevel();
                    bracketLevelStack.add(level);

                    LinkedPositionGroup group = new LinkedPositionGroup();
                    group.addPosition(new LinkedPosition(document, offset + 1, 0, LinkedPositionGroup.NO_STOP));

                    LinkedModeModel model = new LinkedModeModel();
                    model.addLinkingListener(this);
                    model.addGroup(group);
                    model.forceInstall();

                    level.offset = offset;
                    level.length = 2;

                    // set up position tracking for our magic peers
                    if (bracketLevelStack.size() == 1) {
                        document.addPositionCategory(CATEGORY);
                        document.addPositionUpdater(positionUpdater);
                    }
                    level.firstPosition = new Position(offset, 1);
                    level.secondPosition = new Position(offset + 1, 1);
                    document.addPosition(CATEGORY, level.firstPosition);
                    document.addPosition(CATEGORY, level.secondPosition);

                    level.uI = new EditorLinkedModeUI(model, sourceViewer);
                    level.uI.setSimpleMode(true);
                    level.uI.setExitPolicy(new ExitPolicy(closingCharacter, getEscapeCharacter(closingCharacter), bracketLevelStack));
                    level.uI.setExitPosition(sourceViewer, offset + 2, 0, Integer.MAX_VALUE);
                    level.uI.setCyclingMode(LinkedModeUI.CYCLE_NEVER);
                    level.uI.enter();

                    IRegion newSelection = level.uI.getSelectedRegion();
                    sourceViewer.setSelectedRange(newSelection.getOffset(), newSelection.getLength());

                    event.doit = false;

                }
                catch (BadLocationException | BadPositionCategoryException e) {
                    log.debug(e);
                }
                break;
            case SWT.TAB:
            {
                try {
                    int curOffset = offset;
//                    if (curOffset == document.getLength()) {
//                        curOffset--;
//                        endOffset--;
//                    }
                    while (curOffset > 0) {
                        if (!Character.isJavaIdentifierPart(document.getChar(curOffset - 1))) {
                            break;
                        }
                        curOffset--;
                    }
                    if (curOffset != offset) {
                        String templateName = document.get(curOffset, offset - curOffset);
                        SQLTemplatesPage templatesPage = editor.getTemplatesPage();
                        Template template = templatesPage.getTemplateStore().findTemplate(templateName);
                        if (template != null && template.isAutoInsertable()) {
                            sourceViewer.setSelectedRange(curOffset, offset - curOffset);
                            templatesPage.insertTemplate(template, document);
                            event.doit = false;
                        }
                    }
                } catch (BadLocationException e) {
                    log.debug(e);
                }
                break;
            }
        }
    }

    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#left(org.eclipse.jface.text.link.LinkedModeModel, int)
     */
    @Override
    public void left(LinkedModeModel environment, int flags)
    {

        final SymbolLevel level = bracketLevelStack.remove(bracketLevelStack.size() - 1);

        if (flags != ILinkedModeListener.EXTERNAL_MODIFICATION) {
            return;
        }

        // remove brackets
        final IDocument document = sourceViewer.getDocument();
        if (document instanceof IDocumentExtension) {
            IDocumentExtension extension = (IDocumentExtension) document;
            extension.registerPostNotificationReplace(null, new IDocumentExtension.IReplace() {

                @Override
                public void perform(IDocument d, IDocumentListener owner)
                {
                    if ((level.firstPosition.isDeleted || level.firstPosition.length == 0)
                        && !level.secondPosition.isDeleted
                        && level.secondPosition.offset == level.firstPosition.offset) {
                        try {
                            document.replace(level.secondPosition.offset, level.secondPosition.length, null);
                        }
                        catch (BadLocationException e) {
                            // do nothing
                        }
                    }

                    if (bracketLevelStack.size() == 0) {
                        document.removePositionUpdater(positionUpdater);
                        try {
                            document.removePositionCategory(CATEGORY);
                        }
                        catch (BadPositionCategoryException e) {
                            // do nothing
                        }
                    }
                }

            }
            );
        }

    }

    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#suspend(org.eclipse.jface.text.link.LinkedModeModel)
     */
    @Override
    public void suspend(LinkedModeModel environment)
    {
    }

    /*
     * @see org.eclipse.jface.text.link.ILinkedModeListener#resume(org.eclipse.jface.text.link.LinkedModeModel, int)
     */
    @Override
    public void resume(LinkedModeModel environment, int flags)
    {
    }

    private static class SymbolLevel {
        int offset;
        int length;
        LinkedModeUI uI;
        Position firstPosition;
        Position secondPosition;
    }

    private class ExitPolicy implements IExitPolicy {

        final char exitCharacter;
        final char escapeCharacter;
        final List<SymbolLevel> stack;
        final int size;

        public ExitPolicy(char exitCharacter, char escapeCharacter, List<SymbolLevel> stack)
        {
            this.exitCharacter = exitCharacter;
            this.escapeCharacter = escapeCharacter;
            this.stack = stack;
            size = this.stack.size();
        }

        @Override
        public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length)
        {

            if (event.character == exitCharacter) {

                if (size == stack.size() && !isMasked(offset)) {
                    SymbolLevel level = stack.get(stack.size() - 1);
                    if (level.firstPosition.offset > offset || level.secondPosition.offset < offset) {
                        return null;
                    }
                    if (level.secondPosition.offset == offset && length == 0) {
                        // don't enter the character if if its the closing peer
                        return new ExitFlags(ILinkedModeListener.UPDATE_CARET, false);
                    }
                }
            }
            return null;
        }

        private boolean isMasked(int offset)
        {
            IDocument document = sourceViewer.getDocument();
            try {
                return escapeCharacter == document.getChar(offset - 1);
            }
            catch (BadLocationException e) {
                log.debug(e);
            }
            return false;
        }
    }

    public static char getEscapeCharacter(char character)
    {
        switch (character) {
            case '"':
            case '\'':
                return '\\';
            default:
                return 0;
        }
    }


    public static char getPeerCharacter(char character)
    {
        switch (character) {
            case '(': return ')';
            case ')': return '(';
            case '[': return ']';
            case ']': return '[';
            case '"': return character;
            case '\'': return character;
            default:
                throw new IllegalArgumentException();
        }
    }

    public static class EditorLinkedModeUI extends LinkedModeUI {

        private static class EditorHistoryUpdater implements ILinkedModeUIFocusListener {

            @Override
            public void linkingFocusLost(LinkedPosition position, LinkedModeUITarget target) {
                // mark navigation history
                IWorkbenchWindow win= PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                if (win != null) {
                    IWorkbenchPage page= win.getActivePage();
                    if (page != null) {
                        IEditorPart part= page.getActiveEditor();
                        page.getNavigationHistory().markLocation(part);
                    }
                }
            }
            @Override
            public void linkingFocusGained(LinkedPosition position, LinkedModeUITarget target) {
            }
        }

        public EditorLinkedModeUI(LinkedModeModel model, ITextViewer viewer) {
            super(model, viewer);
            setPositionListener(new EditorHistoryUpdater());
        }
    }

}