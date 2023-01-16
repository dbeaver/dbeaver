/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.sql.completion.hippie;

import org.eclipse.jface.text.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.text.parser.TPWordDetector;


import java.util.ArrayList;
import java.util.List;

/**
 * A completion proposal computer for hippie word completions.
 * <p>
 * Clients may instantiate.
 * </p>
 *
 * @since 3.2
 */
public final class HippieProposalProcessor {

    private static final String[] NO_PROPOSALS = new String[0];
    private final HippieCompletionEngine fEngine = new HippieCompletionEngine();
    private TPWordDetector wordDetector;

    /**
     * Creates a new hippie completion proposal computer.
     */
    public HippieProposalProcessor() {
    }

    public HippieProposalProcessor(@NotNull TPWordDetector wordDetector) {
        this.wordDetector = wordDetector;
    }

    /**
     * Get completion proposals for the specified offset in the given document
     * @param document
     * @param offset of the symbol to the left of the cursor
     * @return proposals
     */
    public String[] computeCompletionStrings(IDocument document, int offset) {
        try {
            String prefix = getPrefix(document, offset);
            if (prefix == null || prefix.isEmpty() || prefix.charAt(prefix.length() - 1) == '.') {
                return NO_PROPOSALS;
            }

            List<String> result = new ArrayList<>();
            for (String string : getSuggestions(document, offset, prefix)) {
                if (!string.isEmpty()) {
                    result.add(prefix + string);
                }
            }

            return result.toArray(new String[0]);

        } catch (BadLocationException x) {
            // ignore and return no proposals
            return NO_PROPOSALS;
        }
    }

    private String getPrefix(IDocument document, int offset) throws BadLocationException {
        if (document == null || offset > document.getLength()) {
            return null;
        }

        int length = 0;
        int localOffset = offset;
        char nextChar = document.getChar(localOffset);
        while (Character.isJavaIdentifierPart(nextChar) || (wordDetector != null && wordDetector.isWordPart(nextChar))) {
            length++;
            localOffset--;
            if (localOffset < 0) {
                break;
            }
            nextChar = document.getChar(localOffset);
        }

        return document.get(localOffset + 1, length);
    }

    /**
     * Return the list of suggestions from the current document. First the document is searched
     * backwards from the caret position and then forwards.
     *
     * @param offset   the offset
     * @param document the viewer
     * @param prefix   the completion prefix
     * @return all possible completions that were found in the current document
     */
    private ArrayList<String> createSuggestionsFromOpenDocument(IDocument document, int offset, String prefix) {
        ArrayList<String> completions = new ArrayList<>();
        completions.addAll(fEngine.getCompletionsBackwards(document, prefix, offset));
        completions.addAll(fEngine.getCompletionsForward(document, prefix, offset - prefix.length(), true));

        return completions;
    }

    /**
     * Create the array of suggestions. It scans for other documents or editors,
     * and prefers suggestions from the currently open editor. It also adds the
     * empty suggestion at the end.
     *
     * @param document document to check
     * @param offset   the offset
     * @param prefix   the prefix to search for
     * @return the list of all possible suggestions in the currently open
     * editors
     * @throws BadLocationException if accessing the current document fails
     */
    private List<String> getSuggestions(IDocument document, int offset, String prefix) throws BadLocationException {
        ArrayList<String> suggestions = createSuggestionsFromOpenDocument(document, offset, prefix);
        if (document != null) {
            suggestions.addAll(fEngine.getCompletionsForward(document, prefix, 0, false));
        }
        // add the empty suggestion
        suggestions.add(""); //$NON-NLS-1$
        return fEngine.makeUnique(suggestions);
    }


    public String getErrorMessage() {
        return null; // no custom error message
    }
}
