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
/*
 * Created on Aug 12, 2004
 */
package org.jkiss.dbeaver.ui.editors.erd.editor;

import org.eclipse.gef.EditDomain;
import org.eclipse.gef.GEFColorProvider;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.ui.palette.PaletteColorProvider;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.XMLMemento;
import org.jkiss.code.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * PaletteViewerProvider subclass used for initialising drag and drop
 *
 * @author Serge Rider
 */
public class ERDPaletteViewerProvider extends PaletteViewerProvider {
    @NotNull
    private final List<PaletteViewer> viewers = new ArrayList<>();

    /**
     * implicit constructor
     */
    public ERDPaletteViewerProvider(@NotNull EditDomain graphicalViewerDomain) {
        super(graphicalViewerDomain);
    }

    @Override
    protected void configurePaletteViewer(@NotNull PaletteViewer viewer) {
        super.configurePaletteViewer(viewer);
        viewer.setColorProvider(new ThemePaletteColorProvider());
        viewers.add(viewer);
        viewer.getControl().addDisposeListener(event -> viewers.remove(viewer));
        viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));
    }

    public void refreshTheme() {
        for (PaletteViewer viewer : viewers) {
            if (viewer.getControl().isDisposed()) {
                continue;
            }
            viewer.setColorProvider(new ThemePaletteColorProvider());
            if (viewer.getPaletteRoot() != null) {
                XMLMemento state = XMLMemento.createWriteRoot("palette");
                viewer.saveState(state);
                var root = viewer.getPaletteRoot();
                viewer.setPaletteRoot(null);
                viewer.setPaletteRoot(root);
                viewer.restoreState(state);
            }
            viewer.getControl().redraw();
        }
    }

    private static class ThemePaletteColorProvider extends PaletteColorProvider {
        private final GEFColorProvider themeColors = new GEFColorProvider();

        @Override
        @NotNull
        public Color getButton() {
            return themeColors.getButton();
        }

        @Override
        @NotNull
        public Color getButtonDarker() {
            return themeColors.getButtonDarker();
        }

        @Override
        @NotNull
        public Color getButtonDarkest() {
            return themeColors.getButtonDarkest();
        }

        @Override
        @NotNull
        public Color getListBackground() {
            return themeColors.getListBackground();
        }

        @Override
        @NotNull
        public Color getListForeground() {
            return themeColors.getListForeground();
        }

        @Override
        @NotNull
        public Color getListSelectedBackgroundColor() {
            return themeColors.getListSelectedBackgroundColor();
        }

        @Override
        @NotNull
        public Color getListHoverBackgroundColor() {
            return themeColors.getListHoverBackgroundColor();
        }
    }

}
