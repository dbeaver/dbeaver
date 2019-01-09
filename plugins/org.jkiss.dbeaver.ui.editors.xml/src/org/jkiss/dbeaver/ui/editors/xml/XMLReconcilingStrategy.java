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

package org.jkiss.dbeaver.ui.editors.xml;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;
import org.eclipse.swt.widgets.Display;

public class XMLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

    private static final int START_TAG = 1;
    private static final int LEAF_TAG = 2;
    private static final int END_TAG = 3;
    private static final int EOR_TAG = 4;
    private static final int COMMENT_TAG = 5;
    private static final int PI_TAG = 6;

    private XMLEditor editor;
    private IDocument document;

    private final List<Position> positions = new ArrayList<>();

    /**
     * next character position - used locally and only valid while
     * {@link #calculatePositions()} is in progress.
     */
    private int cNextPos = 0;

    /**
     * number of newLines found by {@link #classifyTag()}
     */
    private int cNewLines = 0;

    private char cLastNLChar = ' ';

    /**
     * The offset of the next character to be read
     */
    private int fOffset;

    /**
     * The end offset of the range to be scanned
     */
    private int fRangeEnd;

    /**
     * @return Returns the editor.
     */
    public XMLEditor getEditor() {
        return editor;
    }

    public void setEditor(XMLEditor editor) {
        this.editor = editor;
    }

    public void setDocument(IDocument document) {
        this.document = document;

    }

    public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
        initialReconcile();
    }

    public void reconcile(IRegion partition) {
        initialReconcile();
    }

    public void setProgressMonitor(IProgressMonitor monitor) {

    }

    public void initialReconcile() {
        fOffset = 0;
        fRangeEnd = document.getLength();
        calculatePositions();

    }

    /**
     * uses {@link #document}, {@link #fOffset} and {@link #fRangeEnd} to
     * calculate {@link #positions}. About syntax errors: this method is not a
     * validator, it is useful.
     */
    private void calculatePositions() {
        positions.clear();
        cNextPos = fOffset;

        try {
            recursiveTokens(0);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        // Collections.sort(positions, new RangeTokenComparator());

        Display.getDefault().asyncExec(new Runnable() {
            public void run() {
                editor.updateFoldingStructure(positions);
            }

        });
    }

    /**
     * emits tokens to {@link #positions}.
     *
     * @return number of newLines
     */
    private int recursiveTokens(int depth) throws BadLocationException {
        int newLines = 0;
        while (cNextPos < fRangeEnd) {
            while (cNextPos < fRangeEnd) {
                char ch = document.getChar(cNextPos++);
                switch (ch) {
                    case '<':
                        int startOffset = cNextPos - 1;
                        int startNewLines = newLines;
                        int classification = classifyTag();
                        //String tagString = fDocument.get(startOffset, Math.min(cNextPos - startOffset, fRangeEnd - startOffset)); // this is to see where we are in the debugger
                        newLines += cNewLines; // cNewLines is written by
                        // classifyTag()

                        switch (classification) {
                            case START_TAG:
                                newLines += recursiveTokens(depth + 1);
                                if (newLines > startNewLines + 1) {
                                    emitPosition(startOffset, cNextPos - startOffset);
                                }
                                break;
                            case LEAF_TAG:
                                if (newLines > startNewLines + 1) {
                                    emitPosition(startOffset, cNextPos - startOffset);
                                }
                                break;
                            case COMMENT_TAG:
                                if (newLines > startNewLines + 1) {
                                    emitPosition(startOffset, cNextPos - startOffset);
                                }
                                break;
                            case PI_TAG:
                                break;
                            case END_TAG:
                            case EOR_TAG:
                                return newLines;
                            default:
                                break;
                        }
                        break;
                    case '\n':
                    case '\r':
                        if ((ch == cLastNLChar) || (' ' == cLastNLChar)) {
                            newLines++;
                            cLastNLChar = ch;
                        }
                        break;
                    default:
                        break;
                }
            }

        }
        return newLines;
    }

    private void emitPosition(int startOffset, int length) {
        positions.add(new Position(startOffset, length));
    }

    /**
     * classsifies a tag: <br />
     * &lt;?...?&gt;: {@link #PI_TAG} <br />
     * &lt;!...--&gt;: {@link #COMMENT_TAG} <br />
     * &lt;...&gt;: {@link #START_TAG} <br />
     * &lt;.../&gt;: {@link #LEAF_TAG} <br />
     * &lt;/...&gt;: {@link #END_TAG} <br />
     * &lt;...: {@link #EOR_TAG} (end of range reached before closing &gt; is
     * found). <br />
     * when this method is called, {@link #cNextPos} must point to the character
     * after &lt;, when it returns, it points to the character after &gt; or
     * after the range. About syntax errors: this method is not a validator, it
     * is useful. Side effect: writes number of found newLines to
     * {@link #cNewLines}.
     *
     * @return the tag classification
     */
    private int classifyTag() {
        try {
            char ch = document.getChar(cNextPos++);
            cNewLines = 0;

            // processing instruction?
            if ('?' == ch) {
                boolean piFlag = false;
                while (cNextPos < fRangeEnd) {
                    ch = document.getChar(cNextPos++);
                    if (('>' == ch) && piFlag)
                        return PI_TAG;
                    piFlag = ('?' == ch);
                }
                return EOR_TAG;
            }

            // comment?
            if ('!' == ch) {
                cNextPos++; // must be '-' but we don't care if not
                cNextPos++; // must be '-' but we don't care if not
                int commEnd = 0;
                while (cNextPos < fRangeEnd) {
                    ch = document.getChar(cNextPos++);
                    if (('>' == ch) && (commEnd >= 2))
                        return COMMENT_TAG;
                    if (('\n' == ch) || ('\r' == ch)) {
                        if ((ch == cLastNLChar) || (' ' == cLastNLChar)) {
                            cNewLines++;
                            cLastNLChar = ch;
                        }
                    }
                    if ('-' == ch) {
                        commEnd++;
                    } else {
                        commEnd = 0;
                    }
                }
                return EOR_TAG;
            }

            // consume whitespaces
            while ((' ' == ch) || ('\t' == ch) || ('\n' == ch) || ('\r' == ch)) {
                ch = document.getChar(cNextPos++);
                if (cNextPos > fRangeEnd)
                    return EOR_TAG;
            }

            // end tag?
            if ('/' == ch) {
                while (cNextPos < fRangeEnd) {
                    ch = document.getChar(cNextPos++);
                    if ('>' == ch) {
                        cNewLines += eatToEndOfLine();
                        return END_TAG;
                    }
                    if ('"' == ch) {
                        ch = document.getChar(cNextPos++);
                        while ((cNextPos < fRangeEnd) && ('"' != ch)) {
                            ch = document.getChar(cNextPos++);
                        }
                    } else if ('\'' == ch) {
                        ch = document.getChar(cNextPos++);
                        while ((cNextPos < fRangeEnd) && ('\'' != ch)) {
                            ch = document.getChar(cNextPos++);
                        }
                    }
                }
                return EOR_TAG;
            }

            // start tag or leaf tag?
            while (cNextPos < fRangeEnd) {
                ch = document.getChar(cNextPos++);
                // end tag?
                s:
                switch (ch) {
                    case '/':
                        while (cNextPos < fRangeEnd) {
                            ch = document.getChar(cNextPos++);
                            if ('>' == ch) {
                                cNewLines += eatToEndOfLine();
                                return LEAF_TAG;
                            }
                        }
                        return EOR_TAG;
                    case '"':
                        while (cNextPos < fRangeEnd) {
                            ch = document.getChar(cNextPos++);
                            if ('"' == ch)
                                break s;
                        }
                        return EOR_TAG;
                    case '\'':
                        while (cNextPos < fRangeEnd) {
                            ch = document.getChar(cNextPos++);
                            if ('\'' == ch)
                                break s;
                        }
                        return EOR_TAG;
                    case '>':
                        cNewLines += eatToEndOfLine();
                        return START_TAG;
                    default:
                        break;
                }

            }
            return EOR_TAG;

        } catch (BadLocationException e) {
            // should not happen, but we treat it as end of range
            return EOR_TAG;
        }
    }

    private int eatToEndOfLine() throws BadLocationException {
        if (cNextPos >= fRangeEnd) {
            return 0;
        }
        char ch = document.getChar(cNextPos++);
        // 1. eat all spaces and tabs
        while ((cNextPos < fRangeEnd) && ((' ' == ch) || ('\t' == ch))) {
            ch = document.getChar(cNextPos++);
        }
        if (cNextPos >= fRangeEnd) {
            cNextPos--;
            return 0;
        }

        // now ch is a new line or a non-whitespace
        if ('\n' == ch) {
            if (cNextPos < fRangeEnd) {
                ch = document.getChar(cNextPos++);
                if ('\r' != ch) {
                    cNextPos--;
                }
            } else {
                cNextPos--;
            }
            return 1;
        }

        if ('\r' == ch) {
            if (cNextPos < fRangeEnd) {
                ch = document.getChar(cNextPos++);
                if ('\n' != ch) {
                    cNextPos--;
                }
            } else {
                cNextPos--;
            }
            return 1;
        }

        return 0;
    }

}


