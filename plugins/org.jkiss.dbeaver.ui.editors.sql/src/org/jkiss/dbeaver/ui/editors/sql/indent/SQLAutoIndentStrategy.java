/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.indent;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.ISourceViewer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPKeywordType;
import org.jkiss.dbeaver.model.DBPMessageType;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.sql.SQLSyntaxManager;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.parser.SQLParserPartitions;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.DBeaverNotifications;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.*;

public class SQLAutoIndentStrategy extends DefaultIndentLineAutoEditStrategy {
    
    private static class BlockCompletionInfo {
        public final int headTokenId;
        public final String[] completionParts;
        public final int tailTokenId;
        public final int tailEndTokenId;
        public final int headCancelTokenId;
    
        public BlockCompletionInfo(int headTokenId, String[] completionParts, int tailTokenId, int tailEndTokenId, int prevCancelTokenId) {
            this.headTokenId = headTokenId;
            this.completionParts = completionParts;
            this.tailTokenId = tailTokenId;
            this.tailEndTokenId = tailEndTokenId;
            this.headCancelTokenId = prevCancelTokenId;
        }
        
        private static String getTokenString(int tokenId) {
            return tokenId == SQLHeuristicScanner.UNBOUND ? "<UNBOUND>" : SQLHeuristicScanner.getTokenString(tokenId);
        }
        
        @Override
        public String toString() {
            return (headCancelTokenId == SQLHeuristicScanner.UNBOUND ? "" : ("[! " + getTokenString(headCancelTokenId) + "]")) +
                    getTokenString(headTokenId) + " ... " + getTokenString(tailTokenId) + " " + getTokenString(tailEndTokenId);
        }
    }
    
    private static final Log log = Log.getLog(SQLAutoIndentStrategy.class);
    private static final int MINIMUM_SOUCE_CODE_LENGTH = 10;
    private static final boolean KEYWORD_INDENT_ENABLED = false;

    private final String oneIndent = SQLIndenter.createIndent().toString();
    
    private String partitioning;
    private ISourceViewer sourceViewer;
    private SQLSyntaxManager syntaxManager;

    private final Map<Integer, BlockCompletionInfo> blockCompletionByHeadToken = new HashMap<>();
    private final Map<Integer, Map<Integer, Set<BlockCompletionInfo>>> blockCompletionByTailToken = new HashMap<>();
    private String[] delimiters;

    private enum CommentType {
    	Unknown,
    	Block,
    	EndOfLine
    }

    /**
     * Creates a new SQL auto indent strategy for the given document partitioning.
     */
    public SQLAutoIndentStrategy(String partitioning, ISourceViewer sourceViewer, SQLSyntaxManager syntaxManager) {
        this.partitioning = partitioning;
        this.sourceViewer = sourceViewer;
        this.syntaxManager = syntaxManager;
    }


    @Override
    public void customizeDocumentCommand(IDocument document, DocumentCommand command)
    {
        // Do not check for doit because it is disabled in LinkedModeUI (e.g. when braket triggers position group)
//        if (!command.doit) {
//            return;
//        }
        if (command.offset < 0) {
            return;
        }

        if (command.text != null && command.text.length() > MINIMUM_SOUCE_CODE_LENGTH) {
            if (syntaxManager.getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_EXTRACT_FROM_SOURCE)) {
                if (transformSourceCode(document, command)) {
                    DBeaverNotifications.showNotification(
                        "sql.sourceCode.transform",
                        "SQL transformation (click to undo)",
                        "SQL query was extracted from the source code",
                        DBPMessageType.INFORMATION,
                        () -> {
                            if (sourceViewer instanceof ITextOperationTarget) {
                                ((ITextOperationTarget) sourceViewer).doOperation(ITextOperationTarget.UNDO);
                            }
                        });
                }
            }
        } else if (command.length == 0 && command.text != null) {
            final boolean lineDelimiter = isLineDelimiter(document, command.text);
            try {
                boolean isPrevLetter = command.offset > 0 && Character.isJavaIdentifierPart(document.getChar(command.offset - 1));
                boolean isQuote = isIdentifierQuoteString(command.text);
                if (command.offset > 1 && isPrevLetter && !isQuote &&
                    (lineDelimiter || (command.text.length() == 1 && !Character.isJavaIdentifierPart(command.text.charAt(0)))) &&
                    syntaxManager.getPreferenceStore().getBoolean(SQLPreferenceConstants.SQL_FORMAT_KEYWORD_CASE_AUTO))
                {
                    IRegion lineRegion = document.getLineInformationOfOffset(command.offset);
                    String line = document.get(lineRegion.getOffset(), lineRegion.getLength()).trim();

                    if (!SQLUtils.isCommentLine(syntaxManager.getDialect(), line)) {
                        updateKeywordCase(document, command);
                    }
                }
            } catch (BadLocationException e) {
                log.debug(e);
            }
            if (lineDelimiter) {
                smartIndentAfterNewLine(document, command);
            }
        }
    }

    private boolean isIdentifierQuoteString(String str) {
        String[][] quoteStrings = syntaxManager.getIdentifierQuoteStrings();
        if (quoteStrings != null) {
            for (String[] qs : quoteStrings) {
                if (str.equals(SQLConstants.STR_QUOTE_SINGLE) || str.equals(qs[0]) || str.equals(qs[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean transformSourceCode(IDocument document, DocumentCommand command) {
        String sourceCode = command.text;
        int quoteStart = -1, quoteEnd = -1;
        for (int i = 0; i < sourceCode.length(); i++) {
            final char ch = sourceCode.charAt(i);
            if (ch == '"') {
                quoteStart = i;
                break;
            } else if (Character.isUnicodeIdentifierPart(ch) || ch == '{' || ch == '<' || ch == '[') {
                // Letter or bracket before quote
                return false;
            }
        }
        for (int i = sourceCode.length() - 1; i >= 0; i--) {
            final char ch = sourceCode.charAt(i);
            if (ch == '"') {
                quoteEnd = i;
                break;
            } else if (Character.isUnicodeIdentifierPart(ch)) {
                // Letter before quote
                return false;
            }
        }
        if (quoteStart == -1 || quoteEnd == -1) {
            return false;
        }
        // Let's check that source code has some whitespaces
        int wsCount = 0;
        for (int i = quoteStart + 1; i < quoteEnd; i++) {
            if (Character.isWhitespace(sourceCode.charAt(i))) {
                wsCount++;
            }
        }
        if (wsCount < 3) {
            return false;
        }
        StringBuilder result = new StringBuilder(sourceCode.length());
        char prevChar = (char)-1;
        char escapeChar = '\\';
        boolean inString = false;
        boolean inComment = false;
        CommentType commentType = CommentType.Unknown; 
        
        for (int i = quoteStart; i < quoteEnd; i++) {
            final char ch = sourceCode.charAt(i);
            
	        if (inString) {
	        	if (prevChar == escapeChar) {
		            switch (ch) {
	                case 'n':
	                    if (!endsWithLF(result, '\n')) {
	                        result.append("\n");
	                    }
	                    break;
	                case 'r':
	                    if (!endsWithLF(result, '\r')) {
	                        result.append("\r");
	                    }
	                    break;
	                case 't':
	                    result.append("\t");
	                    break;
	                default:
	                    result.append(ch);
	                    break;
		            }
	        	}
	        	else {
		            switch (ch) {
	                case '"':
	                    inString = false;
	                    break;
	                default:
	                    if (ch == escapeChar) {
	                        break;
	                    }
                        result.append(ch);
                    }
		        }
	        }
            else if (inComment) {
        		if (commentType == CommentType.Unknown && prevChar == '/' && ch == '*') {
        			commentType = CommentType.Block;
        		}
        		else if (commentType == CommentType.Unknown && prevChar == '/' && ch == '/') {
        			commentType = CommentType.EndOfLine;
        		}
        		else if (commentType == CommentType.Block && prevChar == '*' && ch == '/' ) {
    				inComment = false;
        		}
        		else if (commentType == CommentType.EndOfLine && ch == '\n') {
    				inComment = false;
        		}
        	}
            else {
            	switch (ch) {
            	case '/':
            		inComment = true;
            		commentType = CommentType.Unknown;
            		break;
            	case '"':
            		inString = true;
            		break;
                case '\n':
                case '\r':
                    // Append linefeed even if it is outside of quotes
                    // (but only if string in quotes doesn't end with linefeed - we don't need doubles)
                    if (result.length() > 0 && !endsWithLF(result, '\n') && !endsWithLF(result, '\r')) {
                        result.append(ch == '\n' ? "\n" : "\r");
                    }
                    break;
            	}
            }
            
            prevChar = ch;
        }

        try {
            document.replace(command.offset, command.length, command.text);
            document.replace(command.offset, command.text.length(), result.toString());
        } catch (Exception e) {
            log.warn(e);
        }

        command.caretOffset = command.offset + result.length();
        command.text = null;
        command.length = 0;
        command.doit = false;

        return true;
    }

    private boolean endsWithLF(StringBuilder result, char lfChar) {
        boolean endsWithLF = false;
        for (int k = result.length(); k > 0; k--) {
            final char lch = result.charAt(k - 1);
            if (!Character.isWhitespace(lch)) {
                break;
            }
            if (lch == lfChar) {
                endsWithLF = true;
                break;
            }
        }
        return endsWithLF;
    }

    private boolean updateKeywordCase(final IDocument document, DocumentCommand command) throws BadLocationException {
        final String commandPrefix = syntaxManager.getControlCommandPrefix();

        // Whitespace - check for keyword
        final int startPos, endPos;
        int pos = command.offset - 1;
        while (pos >= 0 && Character.isWhitespace(document.getChar(pos))) {
            pos--;
        }
        endPos = pos + 1;
        while (pos >= 0) {
            char ch = document.getChar(pos);
            if (!Character.isJavaIdentifierPart(ch) && commandPrefix.indexOf(ch) == -1) {
                break;
            }
            pos--;
        }
        startPos = pos + 1;
        final String keyword = document.get(startPos, endPos - startPos);

        if (syntaxManager.getDialect().getKeywordType(keyword) == DBPKeywordType.KEYWORD) {
            final String fixedKeyword = syntaxManager.getKeywordCase().transform(keyword);
            if (!fixedKeyword.equals(keyword)) {
                command.addCommand(startPos, endPos - startPos, fixedKeyword, null);
                command.doit = false;
                return true;
            }
        }
        return false;
    }

    private void smartIndentAfterNewLine(IDocument document, DocumentCommand command) {
        clearCachedValues();

        int docLength = document.getLength();
        if (docLength == 0) {
            return;
        }

        SQLHeuristicScanner scanner = new SQLHeuristicScanner(document, syntaxManager);
        SQLIndenter indenter = new SQLIndenter(document, scanner);

        //get previous token
        int previousToken = scanner.previousToken(command.offset - 1, SQLHeuristicScanner.UNBOUND);
        int previousTokenPos = scanner.getPosition();
        String lastTokenString = scanner.getLastToken();
        int nextToken = scanner.nextToken(command.offset, SQLHeuristicScanner.UNBOUND);

        BlockCompletionInfo completion = findAutoCompletionTrail(previousToken);
        int prevPreviousToken = completion == null || completion.headCancelTokenId == SQLHeuristicScanner.UNBOUND ?
            SQLHeuristicScanner.NOT_FOUND : scanner.previousToken(previousTokenPos, SQLHeuristicScanner.UNBOUND);
        boolean autoCompletionSupported = completion != null &&
            (completion.headCancelTokenId == SQLHeuristicScanner.UNBOUND || completion.headCancelTokenId != prevPreviousToken);

        String indent;
        String beginIndentaion = "";

        if (autoCompletionSupported) {
            indent = indenter.computeIndentation(command.offset);
            beginIndentaion = indenter.getReferenceIndentation(command.offset);
        } else if (nextToken == SQLIndentSymbols.TokenEND) {
            indent = indenter.getReferenceIndentation(command.offset + 1);
        } else if (KEYWORD_INDENT_ENABLED) {
            if (previousToken == SQLIndentSymbols.TokenKeyword) {
                int nlIndent = syntaxManager.getDialect().getKeywordNextLineIndent(lastTokenString);
                beginIndentaion = indenter.getReferenceIndentation(command.offset);
                if (nlIndent > 0) {
                    //if (beginIndentaion.isEmpty()) {
                    indent = beginIndentaion + indenter.createIndent(nlIndent).toString();
//                } else {
//                    indent = beginIndentaion;
//                }
                } else if (nlIndent < 0) {
                    indent = indenter.unindent(beginIndentaion, nlIndent);
                } else {
                    indent = beginIndentaion;
                }
            } else {
                indent = indenter.getReferenceIndentation(command.offset);
                if (lastTokenString != null) {
                    lastTokenString = lastTokenString.trim();
                    if (lastTokenString.length() > 0) {
                        char lastTokenChar = lastTokenString.charAt(lastTokenString.length() - 1);
                        if (lastTokenChar == ',' || lastTokenChar == ':' || lastTokenChar == '-') {
                            // Keep current indent
                        } else {
                            // Last token seems to be some identifier (table or column or function name)
                            // Next line should contain some keyword then - let's unindent
                            indent = indenter.unindent(indent, 1);
                            // Do not unindent (#5753)
                        }
                    }
                }
            }
        } else {
            indent = indenter.getReferenceIndentation(command.offset);
        }

        if (indent == null) {
            indent = ""; //$NON-NLS-1$
        }

        try {
            int p = (command.offset == docLength ? command.offset - 1 : command.offset);
            int line = document.getLineOfOffset(p);

            StringBuilder buf = new StringBuilder(command.text + indent);

            IRegion reg = document.getLineInformation(line);
            int lineEnd = reg.getOffset() + reg.getLength();

            int contentStart = findEndOfWhiteSpace(document, command.offset, lineEnd);
            command.length = Math.max(contentStart - command.offset, 0);

            int start = reg.getOffset();
            ITypedRegion region = TextUtilities.getPartition(document, partitioning, start, true);
            if (SQLParserPartitions.CONTENT_TYPE_SQL_MULTILINE_COMMENT.equals(region.getType())) {
                start = document.getLineInformationOfOffset(region.getOffset()).getOffset();
            }

            if (autoCompletionSupported && !isClosed(document, command.offset, completion) && getTokenCount(start, command.offset, scanner, previousToken) > 0) {
                buf.setLength(0);
                for (String part: completion.completionParts) {
                    if (part == null) {
                        buf.append(getLineDelimiter(document));
                        buf.append(beginIndentaion);
                    } else {
                        buf.append(adjustCase(lastTokenString, part));
                    }
                }
                command.caretOffset = command.offset;
            } else {
                command.caretOffset = command.offset + buf.length();
            }
            command.shiftsCaret = false;
            command.text = buf.toString();

        } catch (BadLocationException e) {
            log.error(e);
        }
    }
    
    private static String adjustCase(String example, String value) {
        return isLowerCase(example) ? value.toLowerCase() : value.toUpperCase();
    }
    
    private static boolean isLowerCase(String value) {
        for (int i = 0, l = value.length(); i < l; i++) {
            if (Character.isUpperCase(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String getLineDelimiter(IDocument document)
    {
        try {
            if (document.getNumberOfLines() > 1) {
                return document.getLineDelimiter(0);
            }
        }
        catch (BadLocationException e) {
            log.error(e);
        }

        return GeneralUtils.getDefaultLineSeparator();
    }

    private boolean isLineDelimiter(IDocument document, String text)
    {
        if (delimiters == null) {
            delimiters = document.getLegalLineDelimiters();
        }
        return delimiters != null && TextUtilities.equals(delimiters, text) > -1;
    }
    
    private void registerCompletionPair(int headToken, int tailToken) {
        this.registerBlockCompletionInfo(
            new BlockCompletionInfo(headToken, new String[] { null, oneIndent, null, SQLHeuristicScanner.getTokenString(tailToken), null },
                tailToken, SQLHeuristicScanner.UNBOUND, SQLHeuristicScanner.UNBOUND)
        );
    }

    private void registerCompletionPair(int headToken, int tailToken, int tailEndToken) {
        this.registerCompletionInfo(headToken, new String[] { null, SQLHeuristicScanner.getTokenString(tailToken) + " " + SQLHeuristicScanner.getTokenString(tailEndToken), null }, tailToken, tailEndToken);
    }

    private void registerCompletionInfo(int headToken, String[] completionParts, int tailToken, int tailEndToken) {
        this.registerBlockCompletionInfo(new BlockCompletionInfo(
            headToken, completionParts, tailToken, tailEndToken, tailEndToken == headToken ? tailToken : SQLHeuristicScanner.UNBOUND)
        );
    }
    
    private void registerBlockCompletionInfo(BlockCompletionInfo info) {
        this.blockCompletionByHeadToken.put(info.headTokenId, info);    
        this.blockCompletionByTailToken.computeIfAbsent(info.tailTokenId, n -> new HashMap<>())
           .computeIfAbsent(info.tailEndTokenId, n -> new HashSet<>())
           .add(info);
    }

    private void clearCachedValues()
    {
        blockCompletionByHeadToken.clear();
        DBPPreferenceStore preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        boolean closeBlocks = preferenceStore.getBoolean(SQLPreferenceConstants.SQLEDITOR_CLOSE_BLOCKS);
        if (closeBlocks) {
            this.registerCompletionPair(SQLIndentSymbols.TokenBEGIN, SQLIndentSymbols.TokenEND);
            this.registerCompletionPair(SQLIndentSymbols.TokenCASE, SQLIndentSymbols.TokenEND);
            this.registerCompletionPair(SQLIndentSymbols.TokenLOOP, SQLIndentSymbols.TokenEND, SQLIndentSymbols.TokenLOOP);
            this.registerCompletionInfo(SQLIndentSymbols.TokenIF, new String[] { " THEN", null, oneIndent, null, "END IF", null }, SQLIndentSymbols.TokenEND, SQLIndentSymbols.TokenIF);
        }
    }

    private BlockCompletionInfo findAutoCompletionTrail(int token) {
        return blockCompletionByHeadToken.get(token);
    }


    /**
     * To count token numbers from start offset to end offset.
     */
    private int getTokenCount(int startOffset, int endOffset, SQLHeuristicScanner scanner,
                              int token)
    {

        int tokenCount = 0;
        while (startOffset < endOffset) {
            int nextToken = scanner.nextToken(startOffset, endOffset);
            int position = scanner.getPosition();
            if (nextToken != SQLIndentSymbols.TokenEOF && nextToken == token) {
                tokenCount++;
            }
            startOffset = position;
        }
        return tokenCount;
    }

    private boolean isClosed(IDocument document, int offset, BlockCompletionInfo completion) {
        int token = completion.headTokenId;
        if (token == SQLIndentSymbols.TokenBEGIN || 
            token == SQLIndentSymbols.TokenCASE || 
            token == SQLIndentSymbols.TokenLOOP || 
            token == SQLIndentSymbols.TokenIF
        ) {
            return getBlockBalance(document, offset, completion) <= 0;
        }
        return false;
    }

    /**
     * Returns the block balance, i.e. zero if the blocks are balanced at <code>offset</code>, a negative number if
     * there are more closing than opening peers, and a positive number if there are more opening than closing peers.
     */
    private int getBlockBalance(IDocument document, int offset, BlockCompletionInfo completion) {
        if (offset < 1) {
            return -1;
        }
        if (offset >= document.getLength()) {
            return 1;
        }

        int begin = offset;
        int end = offset;

        SQLHeuristicScanner scanner = new SQLHeuristicScanner(document, syntaxManager);

        while (true) {
            begin = scanner.findOpeningPeer(begin, completion.headTokenId, completion.tailTokenId, completion.tailEndTokenId, completion.headCancelTokenId);
            end = scanner.findClosingPeer(end, completion.headTokenId, completion.tailTokenId, completion.tailEndTokenId, completion.headCancelTokenId);

            if (begin == -1 && end == -1) {
                return 0;
            }
            if (begin == -1) {
                return -1;
            }
            if (end == -1) {
                return 1;
            }
        }
    }

}

