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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.*;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.internal.SearchDecoration;
import org.eclipse.ui.internal.findandreplace.FindReplaceMessages;
import org.eclipse.ui.internal.findandreplace.HistoryStore;
import org.eclipse.ui.internal.findandreplace.overlay.HistoryTextWrapper;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.Pair;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;
import java.util.stream.StreamSupport;

/**
 * derived from org.eclipse.ui.internal.findandreplace.overlay.FindReplaceOverlay
 */
public class SpreadsheetQuickFilterOverlay {

    @NotNull
    private static final Map<KeyStroke, LocalCommandInfo> SHORTCUTS = new HashMap<>();
    private static final double WORST_CASE_RATIO_EDITOR_TO_OVERLAY = 0.95;
    private static final double BIG_WIDTH_RATIO_EDITOR_TO_OVERLAY = 0.7;
    private static final String MINIMAL_WIDTH_TEXT = "THIS TEXT IS SHORT"; //$NON-NLS-1$
    private static final String IDEAL_WIDTH_TEXT = "THIS TEXT HAS A REASONABLE LENGTH FOR SEARCHING"; //$NON-NLS-1$
    private static final int HISTORY_SIZE = 15;
    private static final String HISTORY_SETTINGS_SECTION_NAME
        = "org.jkiss.dbeaver.ui.controls.resultset.spreadsheetQuickFilterHistory"; //$NON-NLS-1$

    @NotNull
    private final SpreadsheetPresentation spreadsheetPresentation;
    @NotNull
    private final KeyListener spreadsheetKeyListener;
    @NotNull
    private final ControlListener spreadsheetControlListener;
    @NotNull
    private final ISelectionChangedListener spreadsheetSelectionListener;
    @NotNull
    private final Composite overlayContainer;
    @NotNull
    private final HistoryTextWrapper searchBar;
    @NotNull
    private final ToolBar tbarSearchTools;
    @NotNull
    private final ToolItem chkWholeWord;
    @NotNull
    private final ToolItem chkCaseSensitive;
    @NotNull
    private final ToolItem chkRegex;
    @NotNull
    private final ToolBar tbarCloseButton;

    @NotNull
    private final ColorInfo colors;

    @NotNull
    private final ControlDecoration searchBarDecoration;

    private boolean positionAtTop = true;

    private record LocalCommandInfo(
        int toolItemStyle,
        @NotNull String imageKey,
        @NotNull String buttonLabel,
        @NotNull List<KeyStroke> shortcuts,
        @NotNull Consumer<SpreadsheetQuickFilterOverlay> overlayAction
    ) {
        @NotNull
        public ToolItem createToolItem(@NotNull SpreadsheetQuickFilterOverlay overlay, @NotNull ToolBar toolBar) {
            ToolItem item = new ToolItem(toolBar, this.toolItemStyle);
            item.setImage(FindReplaceOverlayImages.get(this.imageKey));
            item.setToolTipText(this.buttonLabel);

            if (this.toolItemStyle != SWT.CHECK) {
                item.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> this.overlayAction.accept(overlay)));
            }

            return item;
        }
    }

    private static final class LocalCommands {
        public static final LocalCommandInfo APPLY_FILTER = registerHotkey(
            SWT.PUSH,
            FindReplaceOverlayImages.KEY_SEARCH_ALL,
            FindReplaceMessages.FindReplaceOverlay_searchAllButton_toolTip,
            "Enter",
            SpreadsheetQuickFilterOverlay::applyFilter,
            KeyStroke.getInstance(SWT.CR), 
            KeyStroke.getInstance(SWT.KEYPAD_CR)
        );
        public static final LocalCommandInfo OPTION_CASE_SENSITIVE = registerHotkey(
            SWT.CHECK,
            FindReplaceOverlayImages.KEY_CASE_SENSITIVE,
            FindReplaceMessages.FindReplaceOverlay_caseSensitiveButton_toolTip,
            "Ctrl+Shift+C",
            o -> o.chkCaseSensitive.setSelection(!o.chkCaseSensitive.getSelection()),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'C'),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'c')
        );
        public static final LocalCommandInfo OPTION_WHOLE_WORD = registerHotkey(
            SWT.CHECK,
            FindReplaceOverlayImages.KEY_WHOLE_WORD,
            FindReplaceMessages.FindReplaceOverlay_wholeWordsButton_toolTip,
            "Ctrl+Shift+D",
            o -> o.chkWholeWord.setSelection(!o.chkWholeWord.getSelection()),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'D'),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'd')
        );
        public static final LocalCommandInfo OPTION_REGEX = registerHotkey(
            SWT.CHECK,
            FindReplaceOverlayImages.KEY_FIND_REGEX,
            FindReplaceMessages.FindReplaceOverlay_regexSearchButton_toolTip,
            "Ctrl+Shift+P",
            o -> o.chkRegex.setSelection(!o.chkRegex.getSelection()),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'P'),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'p')
        );
        public static final LocalCommandInfo CLOSE = registerHotkey(
            SWT.PUSH,
            FindReplaceOverlayImages.KEY_CLOSE,
            FindReplaceMessages.FindReplaceOverlay_closeButton_toolTip,
            "Esc",
            SpreadsheetQuickFilterOverlay::close,
            KeyStroke.getInstance(SWT.ESC),
            KeyStroke.getInstance(SWT.MOD1, 'F'),
            KeyStroke.getInstance(SWT.MOD1, 'f')
        );
    }

    public static class CommandHandler extends AbstractHandler {
        @Nullable
        @Override
        public Object execute(@NotNull ExecutionEvent event) throws ExecutionException {
            if (event.getTrigger() instanceof Event ev && ev.widget instanceof Spreadsheet s) {
                s.getPresentation().getQuickFilterOverlay().open();
            }
            return null;
        }
    }

    private record ColorInfo(
        @NotNull
        Color widgetBackground,
        @NotNull
        Color overlayBackground,
        @NotNull
        Color normalTextForeground,
        @NotNull
        Color errorTextForeground
    ) {
    }

    public SpreadsheetQuickFilterOverlay(@NotNull SpreadsheetPresentation presentation) {
        this.spreadsheetPresentation = presentation;

        this.colors = this.obtainContainerColors(presentation.getSpreadsheet());

        this.overlayContainer = new FixedColorComposite(presentation.getSpreadsheet(), SWT.NONE, this.colors.overlayBackground);
        GridDataFactory.fillDefaults().exclude(true).applyTo(this.overlayContainer);
        GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(2, 2).spacing(2, 0).applyTo(this.overlayContainer);

        Composite contentGroup = new FixedColorComposite(this.overlayContainer, SWT.NONE, this.colors.overlayBackground);
        GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).spacing(0, 2).applyTo(contentGroup);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(contentGroup);

        Composite searchContainer = new FixedColorComposite(contentGroup, SWT.NONE, this.colors.widgetBackground);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(searchContainer);
        GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(7, 4, 3, 5).equalWidth(false).applyTo(searchContainer);

        Composite searchBarContainer = new Composite(searchContainer, SWT.NONE);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(searchBarContainer);
        GridLayoutFactory.fillDefaults().numColumns(1).applyTo(searchBarContainer);
        HistoryStore searchHistory = new HistoryStore(getDialogSettings(), HISTORY_SETTINGS_SECTION_NAME, HISTORY_SIZE);
        {
            this.searchBar = new HistoryTextWrapper(searchHistory, searchBarContainer, SWT.SINGLE);
            this.searchBarDecoration = new ControlDecoration(this.searchBar, SWT.BOTTOM | SWT.LEFT);
            GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(this.searchBar);
            this.searchBar.forceFocus();
            this.searchBar.selectAll();
            this.searchBar.addModifyListener(e -> {
                this.resetErrorColoring();
                this.decorate();
            });
            this.searchBar.addFocusListener(FocusListener.focusLostAdapter(e -> this.resetErrorColoring()));
            this.searchBar.setMessage(FindReplaceMessages.FindReplaceOverlay_searchBar_message);
            this.searchBar.setTabList(null);
        }
        this.setupSearchboxContentAssist(searchHistory);

        this.tbarSearchTools = new ToolBar(searchContainer, SWT.FLAT);
        this.tbarSearchTools.setBackground(searchContainer.getBackground());
        {
            new ToolItem(this.tbarSearchTools, SWT.SEPARATOR);
            this.chkCaseSensitive = LocalCommands.OPTION_CASE_SENSITIVE.createToolItem(this, this.tbarSearchTools);
            this.chkRegex = LocalCommands.OPTION_REGEX.createToolItem(this, this.tbarSearchTools);
            this.chkWholeWord = LocalCommands.OPTION_WHOLE_WORD.createToolItem(this, this.tbarSearchTools);
            new ToolItem(tbarSearchTools, SWT.SEPARATOR);
            LocalCommands.APPLY_FILTER.createToolItem(this, this.tbarSearchTools);
            GridDataFactory.fillDefaults().grab(false, true).align(GridData.END, GridData.END).applyTo(this.tbarSearchTools);
        }

        this.tbarCloseButton = new ToolBar(searchContainer, SWT.FLAT);
        this.tbarCloseButton.setBackground(searchContainer.getBackground());
        {
            LocalCommands.CLOSE.createToolItem(this, this.tbarCloseButton);
            GridDataFactory.fillDefaults().grab(false, true).align(GridData.END, GridData.END).applyTo(this.tbarCloseButton);
        }

        this.overlayContainer.layout();
        this.overlayContainer.setVisible(false);

        this.searchBar.getTextBar().addKeyListener(KeyListener.keyPressedAdapter(e -> {
            KeyStroke keyStroke = extractKeyStroke(e);
            LocalCommandInfo command = SHORTCUTS.get(keyStroke);
            if (command != null) {
                command.overlayAction.accept(this);
            }
        }));

        this.spreadsheetKeyListener = KeyListener.keyPressedAdapter(e -> {
            if (e.keyCode == SWT.ESC) {
                this.close();
            }
        });
        this.spreadsheetControlListener = ControlListener.controlResizedAdapter(e -> {
            this.updatePlacementAndVisibility(true);
        });
        this.spreadsheetSelectionListener = e -> {
            this.updatePlacementAndVisibility(true);
        };
    }

    private void setupSearchboxContentAssist(@NotNull HistoryStore searchHistory) {
        TextContentAdapter contentAdapter = new TextContentAdapter();
        IContentProposalProvider regexProposer = new FindReplaceDocumentAdapterContentProposalProvider(true);
        IContentProposalProvider historyProposer = (f, pos) -> StreamSupport.stream(searchHistory.get().spliterator(), false)
            .filter(s -> s.toLowerCase().startsWith(f.toLowerCase()))
            .map(s -> new ContentProposal(s.substring(pos), s, null))
            .toArray(IContentProposal[]::new);
        ContentAssistCommandAdapter commandAdapter = new ContentAssistCommandAdapter(
            this.searchBar.getTextBar(), contentAdapter,
            (f, pos) ->  this.chkRegex.getSelection() ? regexProposer.getProposals(f, pos) : historyProposer.getProposals(f, pos),
            ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true
        );
        commandAdapter.setEnabled(true);
    }

    @NotNull
    private static LocalCommandInfo registerHotkey(
        int toolItemStyle,
        @NotNull String imageKey,
        @NotNull String title,
        @NotNull String shortcut,
        @NotNull Consumer<SpreadsheetQuickFilterOverlay> action,
        @NotNull KeyStroke ... hotkeys
    ) {
        LocalCommandInfo info = new LocalCommandInfo(
            toolItemStyle,
            imageKey,
            title  + " (" + shortcut + ")",
            List.of(hotkeys),
            action
        );

        for (KeyStroke hotkey : hotkeys) {
            SHORTCUTS.put(hotkey, info);
        }

        return info;
    }

    @NotNull
    private static KeyStroke extractKeyStroke(@NotNull KeyEvent e) {
        char character = e.character;
        boolean ctrlDown = (e.stateMask & SWT.CTRL) != 0;
        if (ctrlDown && e.character != e.keyCode && e.character < 0x20 && (e.keyCode & SWT.KEYCODE_BIT) == 0) {
            character += 0x40;
        }
        return KeyStroke.getInstance(e.stateMask & (SWT.MOD1 | SWT.SHIFT), character == 0 ? e.keyCode : character);
    }

    private void applyFilter() {
        BusyIndicator.showWhile(this.overlayContainer.getShell().getDisplay(), () -> {
            SpreadsheetQuickFilter filter;
            try {
                filter = new SpreadsheetQuickFilter(
                    this.searchBar.getText(),
                    this.chkCaseSensitive.getSelection(),
                    this.chkRegex.getSelection(),
                    this.chkWholeWord.getSelection()
                );
            } catch (PatternSyntaxException ex) {
                this.searchBar.setForeground(this.colors.errorTextForeground);
                filter = null;
            }
            if (filter != null) {
                this.spreadsheetPresentation.setFilterMask(filter);
                this.spreadsheetPresentation.refreshData(false, false, false);
                this.searchBar.storeHistory();
                this.updatePlacementAndVisibility(true);
            }
        });
    }


    /**
     * Returns the dialog settings object used to share state between several
     * find/replace overlays.
     *
     * @return the dialog settings to be used
     */
    @NotNull
    private IDialogSettings getDialogSettings() {
        IDialogSettings settings = PlatformUI.getDialogSettingsProvider(
            FrameworkUtil.getBundle(SpreadsheetQuickFilterOverlay.class)
        ).getDialogSettings();
        IDialogSettings dialogSettings = settings.getSection(SpreadsheetQuickFilterOverlay.class.getName());
        if (dialogSettings == null) {
            dialogSettings = settings.addNewSection(SpreadsheetQuickFilterOverlay.class.getName());
        }
        return dialogSettings;
    }

    public void close() {
        if (this.overlayContainer.isDisposed() || !this.overlayContainer.isVisible()) {
            return;
        }

        this.spreadsheetPresentation.setFilterMask(null);
        this.spreadsheetPresentation.refreshData(false, false, false);

        this.spreadsheetPresentation.getSpreadsheet().setFocus();
        this.spreadsheetPresentation.getSpreadsheet().removeKeyListener(this.spreadsheetKeyListener);
        this.spreadsheetPresentation.getSpreadsheet().removeControlListener(this.spreadsheetControlListener);
        this.spreadsheetPresentation.removeSelectionChangedListener(this.spreadsheetSelectionListener);

        this.overlayContainer.setVisible(false);
    }

    public void open() {
        boolean alreadyOpened = this.overlayContainer.isVisible();
        if (!alreadyOpened) {
            this.overlayContainer.setVisible(true);

            this.spreadsheetPresentation.getSpreadsheet().addKeyListener(this.spreadsheetKeyListener);
            this.spreadsheetPresentation.getSpreadsheet().addControlListener(this.spreadsheetControlListener);
            this.spreadsheetPresentation.addSelectionChangedListener(this.spreadsheetSelectionListener);
        }

        this.overlayContainer.layout();
        this.overlayContainer.moveAbove(null);
        this.updatePlacementAndVisibility(alreadyOpened);

        this.searchBar.setFocus();
        this.updateFromTargetSelection();
    }

    @NotNull
    private ColorInfo obtainContainerColors(@NotNull Composite targetControl) {
        Text textBarForRetrievingTheRightColor = new Text(targetControl.getShell(), SWT.SINGLE | SWT.SEARCH);
        targetControl.getShell().layout();

        ColorInfo result = new ColorInfo(
            textBarForRetrievingTheRightColor.getBackground(),
            targetControl.getBackground(),
            textBarForRetrievingTheRightColor.getForeground(),
            JFaceColors.getErrorText(targetControl.getShell().getDisplay())
        );
        textBarForRetrievingTheRightColor.dispose();

        return result;
    }

    /**
     * A composite with a fixed background color, not adapting to theming.
     */
    private static class FixedColorComposite extends Composite {
        @NotNull
        private final Color fixColor;

        public FixedColorComposite(@NotNull Composite parent, int style, @NotNull Color backgroundColor) {
            super(parent, style);
            this.fixColor = backgroundColor;
            this.setBackground(backgroundColor);
        }

        @Override
        public void setBackground(@NotNull Color unusedColor) {
            super.setBackground(fixColor);
        }
    }

    private int getIdealOverlayWidth(@NotNull Rectangle targetBounds) {
        int idealOverlayWidth = calculateOverlayWidthWithToolbars(IDEAL_WIDTH_TEXT);
        int minimumOverlayWidth = Math.min(calculateOverlayWidthWithoutToolbars(MINIMAL_WIDTH_TEXT),
            (int) (targetBounds.width * WORST_CASE_RATIO_EDITOR_TO_OVERLAY));
        int maximumOverlayWidth = (int) (targetBounds.width * BIG_WIDTH_RATIO_EDITOR_TO_OVERLAY);

        int overlayWidth = idealOverlayWidth;
        if (overlayWidth > maximumOverlayWidth) {
            overlayWidth = maximumOverlayWidth;
        }
        if (overlayWidth < minimumOverlayWidth) {
            overlayWidth = minimumOverlayWidth;
        }

        return overlayWidth;
    }

    private int calculateOverlayWidthWithToolbars(@NotNull String searchInput) {
        int toolbarWidth = this.tbarSearchTools.getSize().x;
        return this.calculateOverlayWidthWithoutToolbars(searchInput) + toolbarWidth;
    }

    private int calculateOverlayWidthWithoutToolbars(@NotNull String searchInput) {
        int closeButtonWidth = this.tbarCloseButton.getSize().x;
        int searchInputWidth = this.getTextWidthInSearchBar(searchInput);
        return closeButtonWidth + searchInputWidth;
    }

    private int getTextWidthInSearchBar(@NotNull String input) {
        GC gc = new GC(this.searchBar);
        gc.setFont(this.searchBar.getFont());
        int textWidth = gc.stringExtent(input).x;
        gc.dispose();
        return textWidth;
    }

    /**
     * When making the text-bar 100% small and then regrowing it, we want the text
     * to start at the first character again.
     */
    private void repositionTextSelection() {
        if (!this.searchBar.isDisposed() && !this.searchBar.isFocusControl()) {
            this.searchBar.setSelection(0, 0);
        }
    }

    private void updatePlacementAndVisibility(boolean keepLocation) {
        if (this.spreadsheetPresentation.getSpreadsheet().isDisposed()) {
            this.close();
            return;
        }

        this.overlayContainer.requestLayout();
        Rectangle targetControlBounds = this.obtainSpreadsheetDataAreaBounds();
        Rectangle overlayBounds = this.calculateDesiredOverlayBounds(targetControlBounds, keepLocation);
        this.overlayContainer.setSize(new Point(overlayBounds.width, overlayBounds.height));
        this.overlayContainer.setLocation(new Point(overlayBounds.x, overlayBounds.y));
        this.overlayContainer.layout(true);

        int minimumWidthWithToolbars = calculateOverlayWidthWithoutToolbars(IDEAL_WIDTH_TEXT);
        boolean enable = overlayBounds.width >= minimumWidthWithToolbars;
        ((GridData) this.tbarSearchTools.getLayoutData()).exclude = !enable;
        this.tbarSearchTools.setVisible(enable);

        this.overlayContainer.setVisible(targetControlBounds.union(overlayBounds).equals(targetControlBounds));

        repositionTextSelection();
    }

    @NotNull
    private Rectangle obtainSpreadsheetDataAreaBounds() {
        Spreadsheet spreadsheet = this.spreadsheetPresentation.getSpreadsheet();
        Rectangle controlBounds = spreadsheet.getBounds();
        int x = spreadsheet.getRowHeaderWidth() + 2;
        int y = spreadsheet.getHeaderHeight() + 2;
        int width = controlBounds.width - x - 2;
        int height = controlBounds.height - y - 2;

        ScrollBar verticalBar = spreadsheet.getVerticalBar();
        ScrollBar horizontalBar = spreadsheet.getHorizontalBar();
        if (verticalBar != null && verticalBar.isVisible()) {
            width -= verticalBar.getSize().x;
        }
        if (horizontalBar != null && horizontalBar.isVisible()) {
            height -= horizontalBar.getSize().y;
        }
        return new Rectangle(x, y, width, height);
    }

    @NotNull
    private Rectangle calculateDesiredOverlayBounds(@NotNull Rectangle targetControlBounds, boolean keepLocation) {
        int width = this.getIdealOverlayWidth(targetControlBounds);
        int height = this.overlayContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        int x = targetControlBounds.x + targetControlBounds.width - width;
        int y = targetControlBounds.y;

        Rectangle topLocation = new Rectangle(x, y, width, height);
        int y2 = y + targetControlBounds.height - height;
        Rectangle bottomLocation = new Rectangle(x, y2, width, height);

        Pair<Rectangle, Rectangle> possibleLocations = !keepLocation || this.positionAtTop
            ? Pair.of(topLocation, bottomLocation)
            : Pair.of(bottomLocation, topLocation);

        Rectangle result = this.hasSelectionBoundsConflict(possibleLocations.getFirst())
            ? possibleLocations.getSecond()
            : possibleLocations.getFirst();

        this.positionAtTop = result == topLocation;

        return result;
    }

    private boolean hasSelectionBoundsConflict(@NotNull Rectangle bounds) {
        Spreadsheet spreadsheet = this.spreadsheetPresentation.getSpreadsheet();
        for (GridPos cellPos : spreadsheet.getSelection()) {
            Rectangle cellBounds = spreadsheet.getCellBounds(cellPos.col, cellPos.row);
            if (cellBounds.intersects(bounds)) {
                return true;
            }
        }
        return false;
    }

    @NotNull
    private String getSpreadsheetSelectionText() {
        GridPos selection = this.spreadsheetPresentation.getSelection().getFirstElement();
        if (selection == null) {
            return "";
        }
        Spreadsheet spreadsheet = this.spreadsheetPresentation.getSpreadsheet();
        GridCell cell = spreadsheet.posToCell(selection);
        String value = cell == null ? "" : CommonUtils.toString(spreadsheet.getContentProvider().getCellValue(cell.col, cell.row, false));
        return CommonUtils.toString(value);
    }

    private void updateFromTargetSelection() {
        String selectionText = this.getSpreadsheetSelectionText();
        if (!selectionText.isEmpty()) {
            this.searchBar.setText(selectionText);
        }
        this.searchBar.setSelection(0, this.searchBar.getText().length());
    }

    private void resetErrorColoring() {
        this.searchBar.setForeground(this.colors.normalTextForeground);
    }

    private void decorate() {
        if (this.chkRegex.getSelection()) {
            SearchDecoration.validateRegex(this.searchBar.getText(), this.searchBarDecoration);
        } else {
            this.searchBarDecoration.hide();
        }
    }

    /**
     * Provides Icons for the editor overlay used for performing find/replace-operations.
     * see org.eclipse.ui.internal.findandreplace.overlay.FindReplaceOverlayImages
     */
    static class FindReplaceOverlayImages {
        private static final String PREFIX_ELCL = TextEditorPlugin.PLUGIN_ID + ".elcl."; //$NON-NLS-1$
        static final String KEY_CLOSE = PREFIX_ELCL + "close"; //$NON-NLS-1$
        static final String KEY_FIND_REGEX = PREFIX_ELCL + "regex"; //$NON-NLS-1$
        static final String KEY_WHOLE_WORD = PREFIX_ELCL + "whole_word"; //$NON-NLS-1$
        static final String KEY_CASE_SENSITIVE = PREFIX_ELCL + "case_sensitive"; //$NON-NLS-1$
        static final String KEY_SEARCH_ALL = PREFIX_ELCL + "search_all"; //$NON-NLS-1$
        static final String KEY_OPEN_HISTORY = "open_history"; //$NON-NLS-1$

        /**
         * The image registry containing {@link Image images}.
         */
        private static ImageRegistry fgImageRegistry;

        private static final String ICONS_PATH = "$nl$/icons/full/"; //$NON-NLS-1$

        private static final String ELCL = ICONS_PATH + "elcl16/"; //$NON-NLS-1$

        /**
         * Declare all images
         */
        private static void declareImages() {
            declareRegistryImage(KEY_CLOSE, ELCL + "close.svg"); //$NON-NLS-1$
            declareRegistryImage(KEY_FIND_REGEX, ELCL + "regex.svg"); //$NON-NLS-1$
            declareRegistryImage(KEY_WHOLE_WORD, ELCL + "whole_word.svg"); //$NON-NLS-1$
            declareRegistryImage(KEY_CASE_SENSITIVE, ELCL + "case_sensitive.svg"); //$NON-NLS-1$
            declareRegistryImage(KEY_SEARCH_ALL, ELCL + "search_all.svg"); //$NON-NLS-1$
            declareRegistryImage(KEY_OPEN_HISTORY, ELCL + "open_history.svg"); //$NON-NLS-1$
        }

        /**
         * Declare an Image in the registry table.
         *
         * @param key  the key to use when registering the image
         * @param path the path where the image can be found. This path is relative to
         *             where this plugin class is found (i.e. typically the packages
         *             directory)
         */
        private static void declareRegistryImage(@NotNull String key, @NotNull String path) {
            if (fgImageRegistry.get(key) == null) {
                ImageDescriptor desc = ImageDescriptor.getMissingImageDescriptor();
                Bundle bundle = Platform.getBundle(TextEditorPlugin.PLUGIN_ID);
                URL url = null;
                if (bundle != null) {
                    url = FileLocator.find(bundle, IPath.fromOSString(path), null);
                    desc = ImageDescriptor.createFromURL(url);
                }
                fgImageRegistry.put(key, desc);
            }
        }

        /**
         * Returns the ImageRegistry.
         *
         * @return image registry
         */
        @NotNull
        public static ImageRegistry getImageRegistry() {
            if (fgImageRegistry == null) {
                initializeImageRegistry();
            }
            return fgImageRegistry;
        }

        /**
         * Initialize the image registry by declaring all of the required graphics. This
         * involves creating JFace image descriptors describing how to create/find the
         * image should it be needed. The image is not actually allocated until
         * requested.
         *
         * Prefix conventions Wizard Banners WIZBAN_ Preference Banners PREF_BAN_
         * Property Page Banners PROPBAN_ Color toolbar CTOOL_ Enable toolbar ETOOL_
         * Disable toolbar DTOOL_ Local enabled toolbar ELCL_ Local Disable toolbar
         * DLCL_ Object large OBJL_ Object small OBJS_ View VIEW_ Product images PROD_
         * Misc images MISC_
         *
         * Where are the images? The images (typically SVGs) are found in the same
         * location as this plugin class. This may mean the same package directory as
         * the package holding this class. The images are declared using this.getClass()
         * to ensure they are looked up via this plugin class.
         *
         * @return the image registry
         * @see org.eclipse.jface.resource.ImageRegistry
         */
        @NotNull
        public static ImageRegistry initializeImageRegistry() {
            fgImageRegistry = TextEditorPlugin.getDefault().getImageRegistry();
            declareImages();
            return fgImageRegistry;
        }

        /**
         * Returns the image managed under the given key in this registry.
         *
         * @param key the image's key
         * @return the image managed under the given key
         */

        @NotNull
        public static Image get(@NotNull String key) {
            return getImageRegistry().get(key);
        }

        /**
         * Returns the image descriptor for the given key in this registry.
         *
         * @param key the image's key
         * @return the image descriptor for the given key
         */
        @NotNull
        public static ImageDescriptor getDescriptor(@NotNull String key) {
            return getImageRegistry().getDescriptor(key);
        }
    }
}
