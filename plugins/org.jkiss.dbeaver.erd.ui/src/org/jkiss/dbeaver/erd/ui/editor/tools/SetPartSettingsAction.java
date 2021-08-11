package org.jkiss.dbeaver.erd.ui.editor.tools;

import org.eclipse.gef3.commands.Command;
import org.eclipse.gef3.ui.actions.SelectionAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.erd.ui.editor.ERDEditorPart;
import org.jkiss.dbeaver.erd.ui.part.EntityPart;
import org.jkiss.dbeaver.erd.ui.part.ICustomizablePart;
import org.jkiss.dbeaver.erd.ui.part.NodePart;
import org.jkiss.dbeaver.erd.ui.part.NotePart;
import org.jkiss.dbeaver.ui.SharedFonts;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseDialog;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SetPartSettingsAction extends SelectionAction {

    private static class ViewSettings {
        private Color background;
        private Color foreground;
        private int borderWidth;
        private boolean transparency;
        private String fontInfo;

    }

    private IStructuredSelection selection;

    public SetPartSettingsAction(ERDEditorPart part, IStructuredSelection selection) {
        super(part);
        this.selection = selection;

        this.setText("Customize ...");
        this.setToolTipText("Figure node view settings");
        this.setId("setPartSettings"); //$NON-NLS-1$
    }

    protected boolean calculateEnabled() {
        for (Object item : selection.toArray()) {
            if (item instanceof NodePart) {
                return true;
            }
        }
        return false;
    }

    protected void init() {
        super.init();
    }

    public void run() {
        this.execute(this.createColorCommand(selection.toArray()));
    }

    private Command createColorCommand(final Object[] objects) {
        return new Command() {
            private ViewSettings newSettings;
            private final Map<ICustomizablePart, ViewSettings> oldSettings = new HashMap<>();
            @Override
            public void execute() {
                final Shell shell = UIUtils.createCenteredShell(getWorkbenchPart().getSite().getShell());
                try {
                    NodePart nodePart = null;
                    boolean hasNotes = false, hasEntities = false;
                    for (Object item : objects) {
                        if (item instanceof NodePart) {
                            if (nodePart == null) {
                                nodePart = (NodePart) item;
                            }
                            if (item instanceof NotePart) {
                                hasNotes = true;
                            } else if (item instanceof EntityPart) {
                                hasEntities = true;
                            }
                        }
                    }

                    PartSettingsDialog settingsDialog = new PartSettingsDialog(shell, nodePart, hasNotes, hasEntities);
                    if (settingsDialog.open() != IDialogConstants.OK_ID) {
                        return;
                    }
                    newSettings = settingsDialog.newSettings;

                    for (Object item : objects) {
                        if (item instanceof ICustomizablePart) {
                            ICustomizablePart part = (ICustomizablePart) item;
                            ViewSettings oldSettings = new ViewSettings();
                            oldSettings.transparency = part.getCustomTransparency();
                            oldSettings.background = part.getCustomBackgroundColor();
                            oldSettings.foreground = part.getCustomForegroundColor();
                            oldSettings.borderWidth = part.getCustomBorderWidth();
                            oldSettings.fontInfo = SharedFonts.toString(part.getCustomFont());
                            this.oldSettings.put(part, oldSettings);

                            setNodeSettings(part, newSettings);
                        }
                    }
                } finally {
                    UIUtils.disposeCenteredShell(shell);
                }
            }

            @Override
            public void undo() {
                for (Object item : objects) {
                    if (item instanceof ICustomizablePart) {
                        ICustomizablePart colorizedPart = (ICustomizablePart) item;
                        ViewSettings viewSettings = oldSettings.get(colorizedPart);
                        if (viewSettings != null) {
                            setNodeSettings(colorizedPart, viewSettings);
                        }
                    }
                }
            }

            @Override
            public void redo() {
                for (Object item : objects) {
                    if (item instanceof ICustomizablePart) {
                        setNodeSettings((ICustomizablePart) item, newSettings);
                    }
                }
            }

            private void setNodeSettings(ICustomizablePart part, ViewSettings settings) {
                if (part instanceof NotePart) {
                    part.setCustomTransparency(settings.transparency);
                }
                part.setCustomBackgroundColor(settings.background);
                if (part instanceof NotePart) {
                    part.setCustomForegroundColor(settings.foreground);
                    part.setCustomBorderWidth(settings.borderWidth);
                    part.setCustomFont(UIUtils.getSharedFonts().getFont(
                        Display.getCurrent(),
                        settings.fontInfo));
                }
            }
        };
    }

    private static class PartSettingsDialog extends BaseDialog {

        private final NodePart node;
        private final boolean noteStyles;
        private final boolean entityStyles;
        private Button transparentCheckbox;
        private ColorSelector backgroundColorPicker;
        private ColorSelector foregroundColorPicker;
        private Text borderWidthText;
        private String fontData;
        private ViewSettings newSettings = new ViewSettings();

        public PartSettingsDialog(Shell parentShell, NodePart node, boolean noteStyles, boolean entityStyles) {
            super(parentShell, "Node view settings", null);
            this.node = node;
            this.noteStyles = noteStyles;
            this.entityStyles = entityStyles;
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            Composite dialogArea = super.createDialogArea(parent);

            Group settingsGroup = UIUtils.createControlGroup(dialogArea, "Settings", 2, GridData.FILL_HORIZONTAL, 0);

            if (noteStyles) {
                transparentCheckbox = UIUtils.createCheckbox(settingsGroup, "Transparent", "Make figure transparent (no background)",
                    node != null && node.getCustomTransparency(), 2);
            }
            UIUtils.createControlLabel(settingsGroup, "Background");
            backgroundColorPicker = new ColorSelector(settingsGroup);
            if (node != null) {
                backgroundColorPicker.setColorValue(node.getCustomBackgroundColor().getRGB());
            }
            if (noteStyles) {
                UIUtils.createControlLabel(settingsGroup, "Foreground");
                foregroundColorPicker = new ColorSelector(settingsGroup);
                if (node != null) {
                    foregroundColorPicker.setColorValue(node.getCustomForegroundColor().getRGB());
                }

                borderWidthText = UIUtils.createLabelText(settingsGroup, "Border width", String.valueOf(node == null ? 1 : node.getCustomBorderWidth()));
                GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
                gd.widthHint = 30;
                borderWidthText.setLayoutData(gd);
                borderWidthText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));

                UIUtils.createControlLabel(settingsGroup, "Font");
                Button changeFontButton = UIUtils.createPushButton(settingsGroup, "Customize...", null, null);
                changeFontButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));

                Text previewText = new Text(settingsGroup, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI);
                previewText.setText("ERD Node Text");
                gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL);
                gd.horizontalSpan = 2;
                previewText.setLayoutData(gd);
                if (node != null) {
                    previewText.setFont(node.getCustomFont());
                    fontData = SharedFonts.toString(node.getCustomFont().getFontData()[0]);
                }

                changeFontButton.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        FontDialog fontDialog = new FontDialog(getShell(), SWT.NONE);
                        fontDialog.setFontList(previewText.getFont().getFontData());
                        FontData result = fontDialog.open();
                        if (result != null) {
                            fontData = SharedFonts.toString(result);
                            previewText.setFont(UIUtils.getSharedFonts().getFont(previewText.getDisplay(), result));
                            settingsGroup.layout(true, true);
                        }
                    }
                });
            }

            return dialogArea;
        }

        @Override
        protected void okPressed() {
            newSettings = new ViewSettings();
            newSettings.background = getBackgroundColor();
            newSettings.foreground = getForegroundColorPicker();
            newSettings.transparency = isTransparent();
            newSettings.borderWidth = getBorderWidth();
            newSettings.fontInfo = getFontData();
            super.okPressed();
        }

        public boolean isTransparent() {
            return transparentCheckbox != null && transparentCheckbox.getSelection();
        }

        public Color getBackgroundColor() {
            return backgroundColorPicker == null ? null : UIUtils.getSharedTextColors().getColor(backgroundColorPicker.getColorValue());
        }

        public Color getForegroundColorPicker() {
            return foregroundColorPicker == null ? null : UIUtils.getSharedTextColors().getColor(foregroundColorPicker.getColorValue());
        }

        public int getBorderWidth() {
            return borderWidthText == null ? 0 : CommonUtils.toInt(borderWidthText.getText());
        }

        public String getFontData() {
            return fontData;
        }
    }

}
