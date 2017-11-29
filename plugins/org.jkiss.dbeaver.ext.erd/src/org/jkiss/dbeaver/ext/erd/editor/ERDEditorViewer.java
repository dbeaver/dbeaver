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
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.gef.GraphicalViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Control;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.part.EntityPart;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * ERD viewer adapter
 */
public class ERDEditorViewer extends Viewer
{
    private static final Log log = Log.getLog(ERDEditorViewer.class);

    private final ERDEditorPart editorPart;

    public ERDEditorViewer(ERDEditorPart editorPart) {
        this.editorPart = editorPart;
    }

    @Override
    public Control getControl() {
        return editorPart.getGraphicalViewer().getControl();
    }

    @Override
    public Object getInput() {
        return editorPart.getGraphicalViewer().getContents();
    }

    @Override
    public ISelection getSelection() {
        final GraphicalViewer graphicalViewer = editorPart.getGraphicalViewer();
        return graphicalViewer == new StructuredSelection() ? null : graphicalViewer.getSelection();
/*
        if (graphicalViewer == null) {
            return new StructuredSelection();
        }
        final ISelection selection = graphicalViewer.getSelection();
        if (selection instanceof IStructuredSelection) {
            return new ERDSelectionAdapter((IStructuredSelection)selection);
        } else {
            return selection;
        }
*/
    }

    @Override
    public void refresh() {
        editorPart.refreshDiagram(true, true);
    }

    @Override
    public void setInput(Object input) {

    }

    @Override
    public void setSelection(ISelection selection, boolean reveal) {
        editorPart.getGraphicalViewer().setSelection(selection);
    }

    private class ERDSelectionAdapter implements IStructuredSelection {
        private final IStructuredSelection selection;

        public ERDSelectionAdapter(IStructuredSelection selection) {
            this.selection = selection;
        }

        @Override
        public Object getFirstElement() {
            final Object firstElement = selection.getFirstElement();
            return firstElement == null ? null : convertObject(firstElement);
        }

        @Override
        public Iterator iterator() {
            final Iterator iterator = selection.iterator();
            return new Iterator() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Object next() {
                    final Object next = iterator.next();
                    return next == null ? null : convertObject(next);
                }
            };
        }

        @Override
        public int size() {
            return selection.size();
        }

        @Override
        public Object[] toArray() {
            final Object[] objects = selection.toArray();
            final Object[] result = new Object[objects.length];
            for (int i = 0; i < objects.length; i++) {
                result[i] = convertObject(objects[i]);
            }
            return result;
        }

        @Override
        public List toList() {
            List list = selection.toList();
            List<Object> result = new ArrayList<>(list.size());
            for (int i = 0; i < list.size(); i++) {
                result.add(convertObject(list.get(i)));
            }
            return result;
        }

        @Override
        public boolean isEmpty() {
            return selection.isEmpty();
        }

        private Object convertObject(Object object) {
            if (object instanceof EntityPart) {
                final DBSEntity entity = ((EntityPart) object).getTable().getObject();
                return entity == null ? null : DBeaverCore.getInstance().getNavigatorModel().getNodeByObject(entity);
            }
            return object;
        }

    }
}
