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
package org.jkiss.dbeaver.ui.controls.findandreplace;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.fieldassist.*;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.*;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.internal.SearchDecoration;
import org.eclipse.ui.internal.findandreplace.FindReplaceLogic;
import org.eclipse.ui.internal.findandreplace.FindReplaceMessages;
import org.eclipse.ui.internal.findandreplace.HistoryStore;
import org.eclipse.ui.internal.findandreplace.SearchOptions;
import org.eclipse.ui.part.MultiPageEditorSite;
import org.eclipse.ui.texteditor.*;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIStyles;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.CommonUtils;
import org.osgi.framework.FrameworkUtil;

import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

/**
 * Derived from <a href="https://github.com/eclipse-platform/eclipse.platform.ui/blob/master/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/FindReplaceOverlay.java">eclipse.platform.ui</a>
 */
public class FindReplaceOverlay {

    public static final String ID_DATA_KEY = "org.eclipse.ui.internal.findreplace.overlay.FindReplaceOverlay.id"; //$NON-NLS-1$

    private static final String REPLACE_BAR_OPEN_DIALOG_SETTING = "replaceBarOpen"; //$NON-NLS-1$
    private static final double WORST_CASE_RATIO_EDITOR_TO_OVERLAY = 0.95;
    private static final double BIG_WIDTH_RATIO_EDITOR_TO_OVERLAY = 0.7;
    private static final String MINIMAL_WIDTH_TEXT = "THIS TEXT IS SHORT "; //$NON-NLS-1$
    private static final String IDEAL_WIDTH_TEXT = "THIS TEXT HAS A REASONABLE LENGTH FOR SEARCHING"; //$NON-NLS-1$
    private static final int HISTORY_SIZE = 15;

    @NotNull
    private final List<EventListener> eventListeners = Collections.synchronizedList(new ArrayList<>());

    @Nullable
    private final ISelectionProvider selectionProvider;
    private final IWorkbenchPart targetPart;

    protected FindReplaceLogic findReplaceLogic;
    private boolean isOpened = false;
    private boolean replaceBarOpen;

    protected final Composite targetControl;

    protected Composite containerControl;
    protected Composite realContainerControl;
    private AccessibleToolBar replaceToggleTools;
    private ToolItem replaceToggle;

    private Composite contentGroup;

    private Composite searchContainer;
    private Composite searchBarContainer;
    protected HistoryTextWrapper searchBar;
    private AccessibleToolBar searchTools;
    private ToolItem searchInSelectionButton;
    protected ToolItem wholeWordSearchButton;
    protected ToolItem caseSensitiveSearchButton;
    protected ToolItem regexSearchButton;
    private ToolItem searchBackwardButton;
    private ToolItem searchForwardButton;
    protected ToolItem selectAllButton;
    private AccessibleToolBar closeTools;

    private Composite replaceContainer;
    private Composite replaceBarContainer;
    private HistoryTextWrapper replaceBar;
    private AccessibleToolBar replaceTools;
    private ToolItem replaceButton;
    private ToolItem replaceAllButton;

    protected Color widgetBackgroundColor;
    protected Color overlayBackgroundColor;
    protected Color normalTextForegroundColor;
    protected Color errorTextForegroundColor;

    private ControlDecoration searchBarDecoration;
    private ContentAssistCommandAdapter contentAssistSearchField;
    private ContentAssistCommandAdapter contentAssistReplaceField;

    private final FocusListener targetActionActivationHandling = new FocusListener() {
        @Nullable
        private DeactivateGlobalActionHandlers globalActionHandlerDeaction;

        @Override
        public void focusGained(@NotNull FocusEvent e) {
            this.setTextEditorActionsActivated(false);
        }

        @Override
        public void focusLost(@NotNull FocusEvent e) {
            this.setTextEditorActionsActivated(true);
        }

        /*
         * Adapted from org.eclipse.jdt.internal.ui.javaeditor.JavaEditor#setActionsActivated(boolean)
         */
        private void setTextEditorActionsActivated(boolean state) {
            if (!(targetPart instanceof AbstractTextEditor) || targetPart.getSite().getWorkbenchWindow().isClosing()) {
                return;
            }
            if (targetPart.getSite() instanceof MultiPageEditorSite multiEditorSite) {
                if (!state && this.globalActionHandlerDeaction == null) {
                    this.globalActionHandlerDeaction = new DeactivateGlobalActionHandlers(multiEditorSite.getActionBars());
                } else if (state && this.globalActionHandlerDeaction != null) {
                    this.globalActionHandlerDeaction.reactivate();
                    this.globalActionHandlerDeaction = null;
                }
            }

            // looks like we are already doing this for all our text editors, where we are able to introduce this find&replace overlay
            // see org.jkiss.dbeaver.ui.editors.TextEditorUtils.enableHostEditorKeyBindingsSupport(..)
            // try {
            //     Method method = AbstractTextEditor.class.getDeclaredMethod("setActionActivation", boolean.class); //$NON-NLS-1$
            //     method.setAccessible(true);
            //     method.invoke(targetPart, state);
            // } catch (IllegalArgumentException | ReflectiveOperationException ex) {
            // ILog.of(FindReplaceOverlay.class).error("cannot (de-)activate actions for text editor", ex); //$NON-NLS-1$
            // }
        }

        static final class DeactivateGlobalActionHandlers {
            @NotNull
            private static final List<String> ACTIONS = List.of(
                ITextEditorActionConstants.CUT,
                ITextEditorActionConstants.COPY,
                ITextEditorActionConstants.PASTE,
                ITextEditorActionConstants.DELETE,
                ITextEditorActionConstants.SELECT_ALL,
                ITextEditorActionConstants.FIND
            );

            @NotNull
            private final Map<String, IAction> deactivatedActions = new HashMap<>();
            @NotNull
            private final IActionBars actionBars;

            public DeactivateGlobalActionHandlers(@NotNull IActionBars actionBars) {
                this.actionBars = actionBars;
                for (String actionID : ACTIONS) {
                    this.deactivatedActions.putIfAbsent(actionID, actionBars.getGlobalActionHandler(actionID));
                    actionBars.setGlobalActionHandler(actionID, null);
                }
            }

            public void reactivate() {
                for (String actionID : this.deactivatedActions.keySet()) {
                    this.actionBars.setGlobalActionHandler(actionID, this.deactivatedActions.get(actionID));
                }
            }
        }

    };

    private final CustomFocusOrder customFocusOrder = new CustomFocusOrder();

    @NotNull
    private static final PreferredLocation[] locationsApplicationOrder = new PreferredLocation[] {
        PreferredLocation.TOP_RIGHT, PreferredLocation.BOTTOM_RIGHT, PreferredLocation.BOTTOM_LEFT
    };

    @NotNull
    private PreferredLocation defaultLocation = PreferredLocation.TOP_RIGHT;
    @NotNull
    private PreferredLocation preferredLocation = defaultLocation;

    private HistoryStore searchHistory;

    public FindReplaceOverlay(
        @NotNull IWorkbenchPart part,
        @NotNull Composite targetControl,
        @NotNull IFindReplaceTarget target,
        @Nullable ISelectionProvider selectionProvider
    ) {
        this.targetPart = part;
        this.targetControl = targetControl;
        this.selectionProvider = selectionProvider;

        this.createFindReplaceLogic(target);
        this.createContainerAndSearchControls(targetControl);
        this.containerControl.setVisible(false);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this.containerControl, IAbstractTextEditorHelpContextIds.FIND_REPLACE_OVERLAY);
    }

    public void setFilterState(@Nullable SearchQuickFilterInfo quickFilter) {
        if (quickFilter != null) {
            this.regexSearchButton.setSelection(quickFilter.isUseRegex());
            this.wholeWordSearchButton.setSelection(quickFilter.isWholeWord());
            this.caseSensitiveSearchButton.setSelection(quickFilter.isCaseSensitive());
            this.searchBar.setText(quickFilter.getText());
        }
    }

    public void setDefaultPreferredLocation(@NotNull PreferredLocation preferredLocation) {
        this.defaultLocation = preferredLocation;
    }

    private boolean insertedInTargetParent() {
        return targetControl instanceof StyledText;
    }

    private void createFindReplaceLogic(@NotNull IFindReplaceTarget target) {
        this.findReplaceLogic = new FindReplaceLogic();
        this.findReplaceLogic.updateTarget(target, target.isEditable());
        this.findReplaceLogic.activate(SearchOptions.INCREMENTAL);
        this.findReplaceLogic.activate(SearchOptions.GLOBAL);
        this.findReplaceLogic.activate(SearchOptions.WRAP);
        this.findReplaceLogic.activate(SearchOptions.FORWARD);
    }

    @NotNull
    public Composite getContainerControl() {
        return this.containerControl;
    }

    private void performReplaceAll() {
        BusyIndicator.showWhile(
            this.containerControl.getShell() != null ? this.containerControl.getShell().getDisplay() : Display.getCurrent(),
            this.findReplaceLogic::performReplaceAll
        );
        this.evaluateStatusAfterReplace();
        this.replaceBar.storeHistory();
        this.searchBar.storeHistory();
    }

    protected void performSelectAll() {
        BusyIndicator.showWhile(
            this.containerControl.getShell() != null ? this.containerControl.getShell().getDisplay() : Display.getCurrent(),
            () -> this.findReplaceLogic.performSelectAll()
        );
        this.searchBar.storeHistory();
    }

    @NotNull
    private final ControlListener targetMovementListener = ControlListener.controlResizedAdapter(e -> asyncExecIfOpen(
        () -> FindReplaceOverlay.this.updatePlacementAndVisibility(true)
    ));

    @NotNull
    private final ISelectionChangedListener selectionListener = e -> asyncExecIfOpen(
        () -> FindReplaceOverlay.this.updatePlacementAndVisibility(true)
    );

    private void asyncExecIfOpen(@NotNull Runnable operation) {
        if (!this.containerControl.isDisposed()) {
            this.containerControl.getDisplay().asyncExec(() -> {
                if (this.containerControl != null && !this.containerControl.isDisposed()) {
                    operation.run();
                }
            });
        }
    }

    @NotNull
    private final FocusListener targetFocusListener = FocusListener.focusGainedAdapter(e ->  {
        this.removeSearchScope();
        this.searchBar.storeHistory();
    });

    @NotNull
    private final KeyListener closeOnTargetEscapeListener = KeyListener.keyPressedAdapter(c -> {
        if (c.keyCode == SWT.ESC) {
            this.close();
        }
    });

    /**
     * Returns the dialog settings object used to share state between several
     * find/replace overlays.
     *
     * @return the dialog settings to be used
     */
    @NotNull
    private IDialogSettings getDialogSettings() {
        // '.class.getClass()' wtf, but SWT does it like this, see https://github.com/eclipse-platform/eclipse.platform.ui/blob/e314e028bde8b21b0005ca6b671f59c1e00dba3a/bundles/org.eclipse.ui.workbench.texteditor/src/org/eclipse/ui/internal/findandreplace/overlay/FindReplaceOverlay.java#L382
        return this.getDialogSettings(FindReplaceAction.class, FindReplaceAction.class.getClass().getName());
    }

    @NotNull
    protected IDialogSettings getDialogSettings(@NotNull Class<?> dialogContextClass) {
        return this.getDialogSettings(dialogContextClass, dialogContextClass.getName());
    }

    @NotNull
    private IDialogSettings getDialogSettings(@NotNull Class<?> dialogContextClass, @NotNull String sectionName) {
        IDialogSettings settings = PlatformUI.getDialogSettingsProvider(FrameworkUtil.getBundle(dialogContextClass)).getDialogSettings();
        IDialogSettings dialogSettings = settings.getSection(sectionName);
        if (dialogSettings == null) {
            dialogSettings = settings.addNewSection(sectionName);
        }
        return dialogSettings;
    }

    public void addEventListener(@NotNull EventListener listener) {
        this.eventListeners.add(listener);
    }

    public void removeEventListener(@NotNull EventListener listener) {
        this.eventListeners.remove(listener);
    }

    public void raizeEvent(@NotNull Consumer<EventListener> operation) {
        for (EventListener listener : this.eventListeners.toArray(EventListener[]::new)) {
            operation.accept(listener);
        }
    }

    public void close() {
        if (this.containerControl.isDisposed() || !this.containerControl.isVisible()) {
            return;
        }
        if (!this.targetControl.setFocus()) {
            if (this.targetPart != null) {
                this.targetPart.setFocus();
            }
        }
        this.storeOverlaySettings();

        this.findReplaceLogic.activate(SearchOptions.GLOBAL);
        this.unbindListeners();

        UIUtils.asyncExec(() -> this.containerControl.setVisible(false));

        this.onClose();

        if (this.findReplaceLogic.getTarget() instanceof IFindReplaceTargetExtension e) {
            e.endSession();
        }

        this.isOpened = false;
        this.raizeEvent(l -> l.closed(this));
    }

    protected void onClose() {
        // do nothing by default
    }

    public void open() {
        boolean alreadyOpened = this.containerControl.isVisible();
        if (!alreadyOpened) {
            this.containerControl.setVisible(true);
            this.bindListeners();
            this.restoreOverlaySettings();
        }
        this.assignIDs();
        this.containerControl.layout();
        this.containerControl.moveAbove(null);
        this.updatePlacementAndVisibility(alreadyOpened);
        this.updateContentAssistAvailability();

        this.searchBar.setFocus();
        this.updateFromTargetSelection();

        IFindReplaceTarget target = this.findReplaceLogic.getTarget();
        this.findReplaceLogic.updateTarget(target, target.isEditable());
        if (target instanceof IFindReplaceTargetExtension e) {
            e.beginSession();
        }

        this.isOpened = true;
        this.raizeEvent(l -> l.opened(this));
    }

    public boolean isOpened() {
        return this.isOpened;
    }

    protected void storeOverlaySettings() {
        this.getDialogSettings().put(REPLACE_BAR_OPEN_DIALOG_SETTING, this.replaceBarOpen);
    }

    protected void restoreOverlaySettings() {
        boolean shouldOpenReplaceBar = this.getDialogSettings().getBoolean(REPLACE_BAR_OPEN_DIALOG_SETTING);
        this.setReplaceVisible(shouldOpenReplaceBar);
    }

    @SuppressWarnings("nls")
    private void assignIDs() {
        this.replaceToggle.setData(ID_DATA_KEY, "replaceToggle");
        this.searchBar.setData(ID_DATA_KEY, "searchInput");
        this.searchBackwardButton.setData(ID_DATA_KEY, "searchBackward");
        this.searchForwardButton.setData(ID_DATA_KEY, "searchForward");
        this.selectAllButton.setData(ID_DATA_KEY, "selectAll");
        this.searchInSelectionButton.setData(ID_DATA_KEY, "searchInSelection");
        this.wholeWordSearchButton.setData(ID_DATA_KEY, "wholeWordSearch");
        this.regexSearchButton.setData(ID_DATA_KEY, "regExSearch");
        this.caseSensitiveSearchButton.setData(ID_DATA_KEY, "caseSensitiveSearch");

        if (this.replaceBarOpen) {
            this.replaceBar.setData(ID_DATA_KEY, "replaceInput");
            this.replaceButton.setData(ID_DATA_KEY, "replaceOne");
            this.replaceAllButton.setData(ID_DATA_KEY, "replaceAll");
        }
    }

    private void unbindListeners() {
        this.targetControl.removeFocusListener(this.targetFocusListener);
        this.targetControl.removeControlListener(this.targetMovementListener);
        this.targetControl.removeKeyListener(this.closeOnTargetEscapeListener);
        if (this.selectionProvider != null) {
            this.selectionProvider.removeSelectionChangedListener(this.selectionListener);
        }
    }

    private void bindListeners() {
        this.targetControl.addFocusListener(this.targetFocusListener);
        this.targetControl.addControlListener(this.targetMovementListener);
        this.targetControl.addKeyListener(this.closeOnTargetEscapeListener);
        if (this.selectionProvider != null) {
            this.selectionProvider.addSelectionChangedListener(this.selectionListener);
        }
    }

    private void createContainerAndSearchControls(@NotNull Composite parent) {
        if (this.insertedInTargetParent()) {
            parent = parent.getParent();
        }
        this.retrieveColors();
        this.createMainContainer(parent);
        this.initializeSearchShortcutHandlers();

        this.containerControl.layout();
    }

    private void initializeSearchShortcutHandlers() {
        this.searchTools.registerActionShortcutsAtControl(this.searchBar);
        this.closeTools.registerActionShortcutsAtControl(this.searchBar);
        this.replaceToggleTools.registerActionShortcutsAtControl(this.searchBar);
    }

    /**
     * HACK: In order to not introduce a hard-coded color, we need to retrieve the
     * background color of text widgets and composite to color those widgets that
     * would otherwise inherit non-fitting custom colors from the containing
     * StyledText.
     */
    private void retrieveColors() {
        if (this.targetPart instanceof StatusTextEditor textEditor) {
            Control targetWidget = textEditor.getAdapter(ITextViewer.class).getTextWidget();
            this.widgetBackgroundColor = targetWidget.getBackground();
            this.normalTextForegroundColor = targetWidget.getForeground();
        } else if (this.targetControl instanceof StyledText targetWidget) {
            this.widgetBackgroundColor = targetWidget.getBackground();
            this.normalTextForegroundColor = targetWidget.getForeground();
        } else {
            Text textBarForRetrievingTheRightColor = new Text(this.targetControl.getShell(), SWT.SINGLE | SWT.SEARCH);
            this.targetControl.getShell().layout();
            this.widgetBackgroundColor = textBarForRetrievingTheRightColor.getBackground();
            this.normalTextForegroundColor = textBarForRetrievingTheRightColor.getForeground();
            textBarForRetrievingTheRightColor.dispose();
        }
        this.overlayBackgroundColor = this.retrieveDefaultCompositeBackground();
        this.errorTextForegroundColor = JFaceColors.getErrorText(this.targetControl.getShell().getDisplay());

        this.widgetBackgroundColor = UIStyles.getDefaultWidgetBackground();
    }

    @NotNull
    private Color retrieveDefaultCompositeBackground() {
        AtomicReference<Color> colorReference = new AtomicReference<>();
        Dialog dummyDialogForColorRetrieval = new Dialog(this.targetControl.getShell()) {
            @Override
            public void create() {
                super.create();
                colorReference.set(getContents().getBackground());
            }

        };
        dummyDialogForColorRetrieval.create();
        dummyDialogForColorRetrieval.close();
        return colorReference.get();
    }

    /**
     * A composite with a fixed background color, not adapting to theming.
     */
    protected static class FixedColorComposite extends Composite {
        @NotNull
        private final Color fixColor;

        public FixedColorComposite(@NotNull Composite parent, int style, @NotNull Color backgroundColor) {
            super(parent, style);
            this.fixColor = backgroundColor;
            this.setBackground(backgroundColor);
        }

        @Override
        public void setBackground(@Nullable Color unusedColor) {
            super.setBackground(fixColor);
        }
    }

    private void createMainContainer(@NotNull final Composite parent) {
        Color borderColor = Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
        this.containerControl = new FixedColorComposite(parent, SWT.NONE, borderColor);
        GridDataFactory.fillDefaults().exclude(true).applyTo(this.containerControl);
        GridLayoutFactory.fillDefaults().margins(1, 1).spacing(0, 0).applyTo(this.containerControl);
        this.realContainerControl = new FixedColorComposite(this.containerControl, SWT.NONE, this.overlayBackgroundColor);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(this.realContainerControl);
        GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(2, 2).spacing(2, 0).applyTo(this.realContainerControl);

        this.createReplaceToggle();
        this.createContentsContainer();
        this.createExtraContent(this.realContainerControl);

        this.containerControl.addDisposeListener(e -> {
            this.raizeEvent(l -> {
                this.isOpened = false;
                l.disposed(this);
            });
            this.eventListeners.clear();
        });
    }

    protected void createExtraContent(@NotNull Composite realContainerControl) {
    }

    private void createReplaceToggle() {
        this.replaceToggleTools = new AccessibleToolBar(this.realContainerControl);
        GridDataFactory.fillDefaults().grab(false, true).align(GridData.FILL, GridData.FILL).applyTo(this.replaceToggleTools);
        this.replaceToggleTools.addMouseListener(MouseListener.mouseDownAdapter(e -> this.setReplaceVisible(!this.replaceBarOpen)));

        this.replaceToggle = new AccessibleToolItemBuilder(this.replaceToggleTools)
            .withShortcuts(KeyboardShortcuts.TOGGLE_REPLACE)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_OPEN_REPLACE))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_replaceToggle_toolTip)
            .withOperation(() -> this.setReplaceVisible(!this.replaceBarOpen)).withShortcuts(KeyboardShortcuts.TOGGLE_REPLACE)
            .build();

        this.replaceToggleTools.setBackground(this.realContainerControl.getBackground());
    }

    private void createContentsContainer() {
        this.contentGroup = new FixedColorComposite(this.realContainerControl, SWT.NONE, this.overlayBackgroundColor);
        GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false).spacing(0, 2).applyTo(this.contentGroup);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(this.contentGroup);

        this.createSearchContainer();
    }

    private void createSearchTools() {
        this.searchTools = new AccessibleToolBar(this.searchContainer);
        GridDataFactory.fillDefaults().grab(false, true).align(GridData.END, GridData.END)
            .applyTo(this.searchTools);

        this.searchTools.createToolItem(SWT.SEPARATOR);

        this.createCaseSensitiveButton();
        this.createRegexSearchButton();
        this.createWholeWordsButton();
        this.createAreaSearchButton();

        this.searchTools.createToolItem(SWT.SEPARATOR);

        this.searchBackwardButton = new AccessibleToolItemBuilder(this.searchTools).withStyleBits(SWT.PUSH)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_FIND_PREV))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_upSearchButton_toolTip)
            .withOperation(() -> this.performSearch(false))
            .withShortcuts(KeyboardShortcuts.SEARCH_BACKWARD).build();

        this.searchForwardButton = new AccessibleToolItemBuilder(this.searchTools).withStyleBits(SWT.PUSH)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_FIND_NEXT))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_downSearchButton_toolTip)
            .withOperation(() -> this.performSearch(true))
            .withShortcuts(KeyboardShortcuts.SEARCH_FORWARD).build();
        this.searchForwardButton.setSelection(true); // by default, search down

        AccessibleToolItemBuilder selectAllButtonBuilder = new AccessibleToolItemBuilder(this.searchTools);
        this.configureSelectAllButton(selectAllButtonBuilder);
        this.selectAllButton = selectAllButtonBuilder.build();

        this.searchTools.setBackground(this.searchContainer.getBackground());
    }

    protected void configureSelectAllButton(@NotNull AccessibleToolItemBuilder buttonBuilder) {
        buttonBuilder.withStyleBits(SWT.PUSH)
                     .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_FIND_ALL))
                     .withToolTipText(FindReplaceMessages.FindReplaceOverlay_searchAllButton_toolTip)
                     .withOperation(this::performSelectAll).withShortcuts(KeyboardShortcuts.SEARCH_ALL);
    }

    private void createCloseTools() {
        this.closeTools = new AccessibleToolBar(this.searchContainer);
        GridDataFactory.fillDefaults().grab(false, true).align(GridData.END, GridData.END)
            .applyTo(this.closeTools);

        // Close button
        new AccessibleToolItemBuilder(this.closeTools).withStyleBits(SWT.PUSH)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_CLOSE))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_closeButton_toolTip) //
            .withOperation(this::close)
            .withShortcuts(KeyboardShortcuts.CLOSE).build();

        this.closeTools.setBackground(this.searchContainer.getBackground());
    }

    private void createAreaSearchButton() {
        this.searchInSelectionButton = new AccessibleToolItemBuilder(this.searchTools).withStyleBits(SWT.CHECK)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_MATCH_IN_SELECTION))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_searchInSelectionButton_toolTip)
            .withOperation(() -> {
                this.activateInFindReplacerIf(SearchOptions.GLOBAL, !this.searchInSelectionButton.getSelection());
                this.updateIncrementalSearch();
            })
            .withShortcuts(KeyboardShortcuts.OPTION_SEARCH_IN_SELECTION).build();
        this.searchInSelectionButton.setSelection(this.findReplaceLogic.isActive(SearchOptions.WHOLE_WORD));
    }

    private void createRegexSearchButton() {
        this.regexSearchButton = new AccessibleToolItemBuilder(this.searchTools).withStyleBits(SWT.CHECK)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_MATCH_REGEX))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_regexSearchButton_toolTip)
            .withOperation(() -> {
                this.activateInFindReplacerIf(SearchOptions.REGEX, this.regexSearchButton.getSelection());
                this.wholeWordSearchButton.setEnabled(this.findReplaceLogic.isAvailable(SearchOptions.WHOLE_WORD));
                this.updateIncrementalSearch();
                this.updateContentAssistAvailability();
                this.decorate();
            }).withShortcuts(KeyboardShortcuts.OPTION_REGEX).build();
        this.regexSearchButton.setSelection(this.findReplaceLogic.isActive(SearchOptions.REGEX));
    }

    private void createCaseSensitiveButton() {
        this.caseSensitiveSearchButton = new AccessibleToolItemBuilder(this.searchTools).withStyleBits(SWT.CHECK)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_MATCH_CASE))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_caseSensitiveButton_toolTip)
            .withOperation(() -> {
                this.activateInFindReplacerIf(SearchOptions.CASE_SENSITIVE, this.caseSensitiveSearchButton.getSelection());
                this.updateIncrementalSearch();
            }).withShortcuts(KeyboardShortcuts.OPTION_CASE_SENSITIVE).build();
        this.caseSensitiveSearchButton.setSelection(this.findReplaceLogic.isActive(SearchOptions.CASE_SENSITIVE));
    }

    private void createWholeWordsButton() {
        this.wholeWordSearchButton = new AccessibleToolItemBuilder(this.searchTools).withStyleBits(SWT.CHECK)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_MATCH_WORD))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_wholeWordsButton_toolTip)
            .withOperation(() -> {
                this.activateInFindReplacerIf(SearchOptions.WHOLE_WORD, this.wholeWordSearchButton.getSelection());
                this.updateIncrementalSearch();
            }).withShortcuts(KeyboardShortcuts.OPTION_WHOLE_WORD).build();
        this.wholeWordSearchButton.setSelection(this.findReplaceLogic.isActive(SearchOptions.WHOLE_WORD));
    }

    private void createReplaceTools() {
        this.replaceTools = new AccessibleToolBar(this.replaceContainer);
        this.replaceTools.createToolItem(SWT.SEPARATOR);
        GridDataFactory.fillDefaults().grab(false, true).align(GridData.CENTER, GridData.END).applyTo(this.replaceTools);
        this.replaceButton = new AccessibleToolItemBuilder(this.replaceTools).withStyleBits(SWT.PUSH)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_REPLACE))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_replaceButton_toolTip)
            .withOperation(() -> {
                if (this.getFindString().isEmpty()) {
                    this.applyErrorColor(this.replaceBar);
                    return;
                }
                this.performSingleReplace();
            }).withShortcuts(KeyboardShortcuts.SEARCH_FORWARD).build();

        this.replaceAllButton = new AccessibleToolItemBuilder(this.replaceTools).withStyleBits(SWT.PUSH)
            .withImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_REPLACE_ALL))
            .withToolTipText(FindReplaceMessages.FindReplaceOverlay_replaceAllButton_toolTip)
            .withOperation(() -> {
                if (this.getFindString().isEmpty()) {
                    this.applyErrorColor(this.replaceBar);
                    return;
                }
                this.performReplaceAll();
            }).withShortcuts(KeyboardShortcuts.SEARCH_ALL).build();

        this.replaceTools.setBackground(this.replaceContainer.getBackground());
    }

    @NotNull
    private ContentAssistCommandAdapter createContentAssistField(@NotNull HistoryTextWrapper control, boolean isFind) {
        TextContentAdapter contentAdapter = new TextContentAdapter();
        FindReplaceDocumentAdapterContentProposalProvider findProposer = new FindReplaceDocumentAdapterContentProposalProvider(isFind);
        ContentAssistCommandAdapter commandAdapter;
        if (isFind) {
            IContentProposalProvider historyProposer = (f, pos) -> StreamSupport.stream(this.searchHistory.get().spliterator(), false)
                .filter(s -> s.toLowerCase().startsWith(f.toLowerCase()))
                .map(s -> new ContentProposal(s.substring(pos), s, null))
                .toArray(IContentProposal[]::new);
            commandAdapter = new ContentAssistCommandAdapter(
                this.searchBar.getTextBar(), contentAdapter,
                (f, pos) -> this.regexSearchButton.getSelection()
                    ? findProposer.getProposals(f, pos)
                    : historyProposer.getProposals(f, pos),
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true
            );
        } else {
            commandAdapter = new ContentAssistCommandAdapter(
                control.getTextBar(), contentAdapter, findProposer,
                ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, new char[0], true
            );
        }
        commandAdapter.setEnabled(true);
        return commandAdapter;
    }

    private void createSearchBar() {
        this.searchBarContainer = new FixedColorComposite(this.searchContainer, SWT.NONE, this.widgetBackgroundColor);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL)
            .applyTo(this.searchBarContainer);
        GridLayoutFactory.fillDefaults().numColumns(1)
            .applyTo(this.searchBarContainer);

        this.searchHistory = new HistoryStore(this.getDialogSettings(), "findhistory", HISTORY_SIZE); //$NON-NLS-1$
        this.searchBar = new HistoryTextWrapper(this.searchHistory, this.searchBarContainer, SWT.SINGLE);
        this.searchBarDecoration = new ControlDecoration(this.searchBar, SWT.BOTTOM | SWT.LEFT);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(this.searchBar);
        this.searchBar.forceFocus();
        this.searchBar.selectAll();
        this.searchBar.addModifyListener(this::onSearchFieldModified);
        this.searchBar.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(@NotNull FocusEvent e) {
                findReplaceLogic.resetIncrementalBaseLocation();
            }

            @Override
            public void focusLost(@NotNull FocusEvent e) {
                resetErrorColoring();
            }
        });
        this.searchBar.addFocusListener(this.targetActionActivationHandling);
        this.searchBar.setMessage(FindReplaceMessages.FindReplaceOverlay_searchBar_message);
        this.contentAssistSearchField = createContentAssistField(this.searchBar, true);
        this.searchBar.setTabList(null);
        this.searchBar.setWidgetBackground(this.widgetBackgroundColor);
    }

    protected void onSearchFieldModified(@NotNull ModifyEvent event) {
        this.wholeWordSearchButton.setEnabled(this.findReplaceLogic.isAvailable(SearchOptions.WHOLE_WORD));
        this.updateIncrementalSearch();
        this.decorate();
    }

    protected void updateIncrementalSearch() {
        String text = this.searchBar.getText();
        if (CommonUtils.isNotEmpty(text)) { // don't try to find <empty string>; it works but selection behaves really weird, so don't
            this.findReplaceLogic.setFindString(text);
            this.evaluateStatusAfterFind();
        } else {
            if (this.findReplaceLogic.getTarget() instanceof IFindReplaceTargetExtension e) {
                e.endSession();
                e.beginSession();
            }
        }
    }

    private void createReplaceBar() {
        this.replaceBarContainer = new FixedColorComposite(this.replaceContainer, SWT.NONE, this.widgetBackgroundColor);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.END)
            .applyTo(this.replaceBarContainer);
        GridLayoutFactory.fillDefaults().numColumns(1).equalWidth(false)
            .applyTo(this.replaceBarContainer);

        HistoryStore replaceHistory = new HistoryStore(this.getDialogSettings(), "replacehistory", HISTORY_SIZE); //$NON-NLS-1$
        this.replaceBar = new HistoryTextWrapper(replaceHistory, this.replaceBarContainer, SWT.SINGLE);
        GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.END).applyTo(this.replaceBar);
        this.replaceBar.setMessage(FindReplaceMessages.FindReplaceOverlay_replaceBar_message);
        this.replaceBar.addModifyListener(e -> {
            this.findReplaceLogic.setReplaceString(this.replaceBar.getText());
            this.resetErrorColoring();
        });
        this.replaceBar.addFocusListener(this.targetActionActivationHandling);
        this.replaceBar.addFocusListener(FocusListener.focusLostAdapter(e -> this.resetErrorColoring()));
        this.contentAssistReplaceField = createContentAssistField(this.replaceBar, false);
        this.replaceBar.setWidgetBackground(this.widgetBackgroundColor);
    }

    private void createSearchContainer() {
        this.searchContainer = new FixedColorComposite(this.contentGroup, SWT.NONE, this.widgetBackgroundColor);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL)
            .applyTo(this.searchContainer);
        GridLayoutFactory.fillDefaults().numColumns(3).extendedMargins(7, 4, 3, 5).equalWidth(false)
            .applyTo(this.searchContainer);

        this.createSearchBar();
        this.createSearchTools();
        this.createCloseTools();
    }

    private void createReplaceContainer() {
        this.replaceContainer = new FixedColorComposite(this.contentGroup, SWT.NONE, this.widgetBackgroundColor);
        GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL)
            .applyTo(this.replaceContainer);
        GridLayoutFactory.fillDefaults().margins(0, 0).numColumns(2).extendedMargins(7, 4, 3, 5).equalWidth(false)
            .applyTo(this.replaceContainer);

        this.createReplaceBar();
        this.createReplaceTools();
    }

    private void setReplaceVisible(boolean visible) {
        if (this.findReplaceLogic.getTarget().isEditable() && visible) {
            this.createReplaceDialog();
            this.replaceToggle.setImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_CLOSE_REPLACE));
        } else {
            this.hideReplace();
            this.replaceToggle.setImage(DBeaverIcons.getImage(UIIcon.FIND_REPLACE_OPEN_REPLACE));
        }
        this.updateContentAssistAvailability();
    }

    private void hideReplace() {
        if (!this.replaceBarOpen) {
            return;
        }
        this.customFocusOrder.dispose();
        this.searchBar.forceFocus();
        this.contentAssistReplaceField = null;
        this.replaceBarOpen = false;
        this.replaceContainer.dispose();
        this.updatePlacementAndVisibility(true);
    }

    private void createReplaceDialog() {
        if (this.replaceBarOpen) {
            return;
        }
        this.replaceBarOpen = true;
        this.createReplaceContainer();
        this.initializeReplaceShortcutHandlers();

        this.updatePlacementAndVisibility(true);
        this.assignIDs();
        this.replaceBar.forceFocus();
        this.customFocusOrder.apply();
    }

    private void initializeReplaceShortcutHandlers() {
        this.replaceTools.registerActionShortcutsAtControl(this.replaceBar);
        this.closeTools.registerActionShortcutsAtControl(this.replaceBar);
        this.replaceToggleTools.registerActionShortcutsAtControl(this.replaceBar);
    }

    private void enableSearchTools(boolean enable) {
        ((GridData) this.searchTools.getLayoutData()).exclude = !enable;
        this.searchTools.setVisible(enable);
    }

    private void enableReplaceToggle(boolean enable) {
        if (!okayToUse(this.replaceToggle)) {
            return;
        }
        boolean shouldBeVisible = enable && this.findReplaceLogic.getTarget().isEditable();
        ((GridLayout) this.containerControl.getLayout()).numColumns = shouldBeVisible ? 2 : 1;
        ((GridData) this.replaceToggleTools.getLayoutData()).exclude = !shouldBeVisible;
        this.replaceToggleTools.setVisible(shouldBeVisible);
    }

    private void enableReplaceTools(boolean enable) {
        if (!okayToUse(this.replaceTools)) {
            return;
        }
        ((GridData) this.replaceTools.getLayoutData()).exclude = !enable;
        this.replaceTools.setVisible(enable);
    }

    private int getIdealOverlayWidth(@NotNull Rectangle targetBounds) {
        int idealOverlayWidth = this.calculateOverlayWidthWithToolbars(IDEAL_WIDTH_TEXT);
        int minimumOverlayWidth = Math.min(
            this.calculateOverlayWidthWithoutToolbars(MINIMAL_WIDTH_TEXT),
            (int) (targetBounds.width * WORST_CASE_RATIO_EDITOR_TO_OVERLAY)
        );
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

    private void configureDisplayedWidgetsForWidth(int overlayWidth) {
        int minimumWidthWithToolbars = this.calculateOverlayWidthWithoutToolbars(IDEAL_WIDTH_TEXT);
        int minimumWidthWithReplaceToggle = this.calculateOverlayWidthWithoutToolbars(MINIMAL_WIDTH_TEXT);
        this.enableSearchTools(overlayWidth >= minimumWidthWithToolbars);
        this.enableReplaceTools(overlayWidth >= minimumWidthWithToolbars);
        this. enableReplaceToggle(overlayWidth >= minimumWidthWithReplaceToggle);
    }

    private int calculateOverlayWidthWithToolbars(@NotNull String searchInput) {
        int toolbarWidth = this.searchTools.getSize().x;
        return this.calculateOverlayWidthWithoutToolbars(searchInput) + toolbarWidth;
    }

    private int calculateOverlayWidthWithoutToolbars(@NotNull String searchInput) {
        int replaceToggleWidth = 0;
        if (okayToUse(this.replaceToggle)) {
            replaceToggleWidth = this.replaceToggle.getBounds().width;
        }
        int closeButtonWidth = this.closeTools.getSize().x;
        int searchInputWidth = this.getTextWidthInSearchBar(searchInput);
        return replaceToggleWidth + closeButtonWidth + searchInputWidth;
    }

    private int getTextWidthInSearchBar(@NotNull String input) {
        GC gc = new GC(this.searchBar);
        gc.setFont(this.searchBar.getFont());
        int textWidth = gc.stringExtent(input).x; // $NON-NLS-1$
        gc.dispose();
        return textWidth;
    }

    /**
     * When making the text-bar 100% small and then regrowing it, we want the text
     * to start at the first character again.
     */
    private void repositionTextSelection() {
        if (okayToUse(this.searchBar) && !this.searchBar.isFocusControl()) {
            this.searchBar.setSelection(0, 0);
        }
        if (okayToUse(this.replaceBar) && !this.replaceBar.isFocusControl()) {
            this.replaceBar.setSelection(0, 0);
        }
    }

    protected void updatePlacementAndVisibility(boolean keepLocation) {
        if (!okayToUse(targetControl)) {
            this.close();
            return;
        }

        this.containerControl.requestLayout();
        Rectangle targetControlBounds = this.calculateControlBounds(this.targetControl);
        Rectangle overlayBounds = this.calculateDesiredOverlayBounds(targetControlBounds, keepLocation);
        this.updatePosition(overlayBounds);
        this.configureDisplayedWidgetsForWidth(overlayBounds.width);
        this.updateVisibility(targetControlBounds, overlayBounds);

        this.repositionTextSelection();
    }

    @NotNull
    private Rectangle calculateControlBounds(@NotNull Control control) {
        Rectangle controlBounds = control.getBounds();
        final int workAreaPadding = 2;

        Point topLeft = this.obtainControlTopLeftPadding(control);
        Point rightBottom = this.obtainControlRightBottomPadding(control);

        int x = workAreaPadding + topLeft.x;
        int y = workAreaPadding + topLeft.y;
        int width = controlBounds.width - x - rightBottom.x - 1;
        int height = controlBounds.height - y - rightBottom.y - 1;

        return new Rectangle(x, y, width, height);
    }

    @NotNull
    protected Point obtainControlTopLeftPadding(@NotNull Control control) {
        int x;
        int y;
        if (this.insertedInTargetParent()) {
            Rectangle controlBounds = control.getBounds();
            x = controlBounds.x;
            y = controlBounds.y;
        } else {
            x = 0;
            y = 0;
        }
        return new Point(x, y);
    }

    @NotNull
    protected Point obtainControlRightBottomPadding(@NotNull Control control) {
        int x = 0;
        int y = 0;
        if (control instanceof Scrollable scrollable) {
            ScrollBar verticalBar = scrollable.getVerticalBar();
            ScrollBar horizontalBar = scrollable.getHorizontalBar();
            if (verticalBar != null && verticalBar.isVisible()) {
                x += verticalBar.getSize().x;
            }
            if (horizontalBar != null && horizontalBar.isVisible()) {
                y += horizontalBar.getSize().y;
            }
        }
        if (control instanceof StyledText styledText) {
            x += styledText.getRightMargin();
        }
        return new Point(x, y);
    }

    @NotNull
    private Rectangle calculateDesiredOverlayBounds(@NotNull Rectangle targetControlBounds, boolean keepLocation) {
        int width = this.getIdealOverlayWidth(targetControlBounds);
        int height = containerControl.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;

        int left = targetControlBounds.x;
        int top = targetControlBounds.y;
        int right = targetControlBounds.x + targetControlBounds.width;
        int bottom = targetControlBounds.y + targetControlBounds.height;

        PreferredLocation currentLocation = keepLocation ? this.preferredLocation : this.defaultLocation;
        Rectangle overlayBounds = currentLocation.prepareBounds(left, top, right, bottom, width, height);

        if (selectionProvider != null) {
            ISelection selection = this.selectionProvider.getSelection();
            if (!this.hasSelectionBoundsConflict(selection, overlayBounds)) {
                return overlayBounds;
            }

            for (PreferredLocation l : locationsApplicationOrder) {
                overlayBounds = l.prepareBounds(left, top, right, bottom, width, height);
                if (!this.hasSelectionBoundsConflict(selection, overlayBounds)) {
                    this.preferredLocation = l;
                    return overlayBounds;
                }
            }

            this.preferredLocation = this.defaultLocation;
            overlayBounds = this.defaultLocation.prepareBounds(left, top, right, bottom, width, height);
        }

        return overlayBounds;
    }

    protected boolean hasSelectionBoundsConflict(@Nullable ISelection selection, @NotNull Rectangle bounds) {
        return false;
    }

    private void updatePosition(@NotNull Rectangle overlayBounds) {
        this.containerControl.setSize(new Point(overlayBounds.width, overlayBounds.height));
        this.containerControl.setLocation(new Point(overlayBounds.x, overlayBounds.y));
        this.containerControl.layout(true);
    }

    private void updateVisibility(@NotNull Rectangle targetControlBounds, @NotNull Rectangle overlayBounds) {
        this.containerControl.setVisible(targetControlBounds.union(overlayBounds).equals(targetControlBounds));
    }

    @NotNull
    private String getFindString() {
        return this.searchBar.getText();
    }

    private void performSingleReplace() {
        if (this.findReplaceLogic.performSelectAndReplace()) {
            this.findReplaceLogic.performSearch();
            this.evaluateStatusAfterFind();
        } else {
            this.evaluateStatusAfterReplace();
        }

        this.replaceBar.storeHistory();
        this.searchBar.storeHistory();
    }

    private void performSearch(boolean forward) {
        boolean oldForwardSearchSetting = findReplaceLogic.isActive(SearchOptions.FORWARD);
        this.activateInFindReplacerIf(SearchOptions.FORWARD, forward);
        this.findReplaceLogic.performSearch();
        this.activateInFindReplacerIf(SearchOptions.FORWARD, oldForwardSearchSetting);
        this.evaluateStatusAfterFind();
        this.searchBar.storeHistory();
    }

    private void updateFromTargetSelection() {
        String selectionText = this.findReplaceLogic.getTarget().getSelectionText();
        if (selectionText.contains("\n")) { //$NON-NLS-1$
            this.findReplaceLogic.deactivate(SearchOptions.GLOBAL);
            this.searchInSelectionButton.setSelection(true);
        } else if (!selectionText.isEmpty()) {
            if (this.findReplaceLogic.isAvailableAndActive(SearchOptions.REGEX)) {
                selectionText = FindReplaceDocumentAdapter.escapeForRegExPattern(selectionText);
            }
            this.searchBar.setText(selectionText);
            this.findReplaceLogic.findAndSelect(this.findReplaceLogic.getTarget().getSelection().x);
        }
        this.searchBar.setSelection(0, this.searchBar.getText().length());
    }

    private void evaluateStatusAfterFind() {
        this.resetErrorColoring();
        if (!this.findReplaceLogic.getStatus().wasSuccessful()) {
            this.applyErrorColor(this.searchBar);
        }
    }

    private void evaluateStatusAfterReplace() {
        this.resetErrorColoring();
        if (!this.findReplaceLogic.getStatus().wasSuccessful()) {
            this.applyErrorColor(this.replaceBar);
        }
    }

    private void applyErrorColor(@NotNull HistoryTextWrapper inputField) {
        inputField.setForeground(this.errorTextForegroundColor);
    }

    private void resetErrorColoring() {
        this.searchBar.setForeground(this.normalTextForegroundColor);
        if (okayToUse(this.replaceBar)) {
            this.replaceBar.setForeground(this.normalTextForegroundColor);
        }
    }

    private void activateInFindReplacerIf(@NotNull SearchOptions option, boolean shouldActivate) {
        if (shouldActivate) {
            this.findReplaceLogic.activate(option);
        } else {
            this.findReplaceLogic.deactivate(option);
        }
    }

    private static boolean okayToUse(@Nullable Widget widget) {
        return widget != null && !widget.isDisposed();
    }

    private void removeSearchScope() {
        this.findReplaceLogic.activate(SearchOptions.GLOBAL);
        this. searchInSelectionButton.setSelection(false);
    }

    private void setContentAssistsEnablement(boolean enable) {
        this.contentAssistSearchField.setEnabled(enable);
        if (okayToUse(this.replaceBar)) {
            this.contentAssistReplaceField.setEnabled(enable);
        }
    }

    private void updateContentAssistAvailability() {
        // setContentAssistsEnablement(findReplaceLogic.isAvailableAndActive(SearchOptions.REGEX));
        this.setContentAssistsEnablement(true); // findReplaceLogic.isAvailableAndActive(SearchOptions.REGEX));
    }

    protected void decorate() {
        if (this.regexSearchButton.getSelection()) {
            SearchDecoration.validateRegex(getFindString(), this.searchBarDecoration);
        } else {
            this.searchBarDecoration.hide();
        }
    }


    private class CustomFocusOrder {
        private final Listener searchBarToReplaceBar = e -> {
            if (e.detail == SWT.TRAVERSE_TAB_NEXT) {
                e.doit = false;
                replaceBar.forceFocus();
            }
        };

        private final Listener replaceBarToSearchBarAndTools = e -> {
            switch (e.detail) {
                case SWT.TRAVERSE_TAB_NEXT:
                    e.doit = false;
                    searchBar.getDropDownTool().forceFirstControlFocus();
                    break;
                case SWT.TRAVERSE_TAB_PREVIOUS:
                    e.doit = false;
                    searchBar.getTextBar().forceFocus();
                    break;
                default:
                    // Proceed as normal
            }
        };

        private final Listener searchToolsToReplaceBar = e -> {
            switch (e.detail) {
                case SWT.TRAVERSE_TAB_PREVIOUS:
                    e.doit = false;
                    replaceBar.forceFocus();
                    break;
                default:
                    // Proceed as normal
            }
        };

        private final Listener closeToolsToReplaceTools = e -> {
            switch (e.detail) {
                case SWT.TRAVERSE_TAB_NEXT:
                    e.doit = false;
                    replaceBar.getDropDownTool().forceFirstControlFocus();
                    break;
                default:
                    // Proceed as normal
            }
        };

        private final Listener replaceToolsToCloseTools = e -> {
            switch (e.detail) {
                case SWT.TRAVERSE_TAB_PREVIOUS:
                    e.doit = false;
                    closeTools.forceFirstControlFocus();
                    break;
                default:
                    // Proceed as normal
            }
        };

        void apply() {
            searchBar.getTextBar().addListener(SWT.Traverse, searchBarToReplaceBar);
            replaceBar.getTextBar().addListener(SWT.Traverse, replaceBarToSearchBarAndTools);
            searchBar.getDropDownTool().getFirstControl().addListener(SWT.Traverse, searchToolsToReplaceBar);
            closeTools.getFirstControl().addListener(SWT.Traverse, closeToolsToReplaceTools);
            replaceBar.getDropDownTool().getFirstControl().addListener(SWT.Traverse, replaceToolsToCloseTools);
        }

        void dispose() {
            searchBar.getTextBar().removeListener(SWT.Traverse, searchBarToReplaceBar);
            replaceBar.getTextBar().removeListener(SWT.Traverse, replaceBarToSearchBarAndTools);
            searchBar.getDropDownTool().getFirstControl().removeListener(SWT.Traverse, searchToolsToReplaceBar);
            closeTools.getFirstControl().removeListener(SWT.Traverse, closeToolsToReplaceTools);
            replaceBar.getDropDownTool().getFirstControl().removeListener(SWT.Traverse, replaceToolsToCloseTools);
        }
    }

    public enum PreferredLocation {
        TOP_RIGHT {
            @NotNull
            @Override
            public Rectangle prepareBounds(int left, int top, int right, int bottom, int width, int height) {
                return new Rectangle(right - width, top, width, height);
            }
        },
        BOTTOM_RIGHT {
            @NotNull
            @Override
            public Rectangle prepareBounds(int left, int top, int right, int bottom, int width, int height) {
                return new Rectangle(right - width, bottom - height, width, height);
            }
        },
        BOTTOM_LEFT {
            @NotNull
            @Override
            public Rectangle prepareBounds(int left, int top, int right, int bottom, int width, int height) {
                return new Rectangle(left, bottom - height, width, height);
            }
        };

        @NotNull
        public abstract Rectangle prepareBounds(int left, int top, int right, int bottom, int width, int height);
    }

    /**
     * Find&Replace overlay lifecycle events listener
     */
    public interface EventListener {

        /**
         * Triggered on overlay open
         */
        default void opened(@NotNull FindReplaceOverlay overlay) {
        }

        /**
         * Triggered on overlay close
         */
        default void closed(@NotNull FindReplaceOverlay overlay) {
        }

        /**
         * Triggered on overlay dispose
         */
        default void disposed(@NotNull FindReplaceOverlay overlay) {
        }
    }


    protected static final class KeyboardShortcuts {
        public static final List<KeyStroke> SEARCH_FORWARD = List.of(
            KeyStroke.getInstance(SWT.CR),
            KeyStroke.getInstance(SWT.KEYPAD_CR)
        );
        public static final List<KeyStroke> SEARCH_BACKWARD = List.of(
            KeyStroke.getInstance(SWT.SHIFT, SWT.CR),
            KeyStroke.getInstance(SWT.SHIFT, SWT.KEYPAD_CR)
        );
        public static final List<KeyStroke> SEARCH_ALL = List.of(
            KeyStroke.getInstance(SWT.MOD1, SWT.CR),
            KeyStroke.getInstance(SWT.MOD1, SWT.KEYPAD_CR)
        );
        public static final List<KeyStroke> OPTION_CASE_SENSITIVE = List.of(
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'C'),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'c')
        );
        public static final List<KeyStroke> OPTION_WHOLE_WORD = List.of(
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'D'),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'd')
        );
        public static final List<KeyStroke> OPTION_REGEX = List.of(
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'P'),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'p')
        );
        public static final List<KeyStroke> OPTION_SEARCH_IN_SELECTION = List.of(
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'I'),
            KeyStroke.getInstance(SWT.MOD1 | SWT.SHIFT, 'i')
        );
        public static final List<KeyStroke> CLOSE = List.of(
            KeyStroke.getInstance(SWT.ESC),
            KeyStroke.getInstance(SWT.MOD1, 'F'),
            KeyStroke.getInstance(SWT.MOD1, 'f')
        );
        public static final List<KeyStroke> TOGGLE_REPLACE = List.of(
            KeyStroke.getInstance(SWT.MOD1, 'R'),
            KeyStroke.getInstance(SWT.MOD1, 'r')
        );
    }
}