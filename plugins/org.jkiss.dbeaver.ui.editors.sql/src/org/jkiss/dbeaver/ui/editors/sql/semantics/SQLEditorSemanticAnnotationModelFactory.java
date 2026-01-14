/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.IAnnotationModelFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.texteditor.BasicMarkerUpdater;
import org.eclipse.ui.texteditor.IMarkerUpdater;
import org.eclipse.ui.texteditor.ResourceMarkerAnnotationModel;

// see org.eclipse.ui.texteditor.ResourceMarkerAnnotationModelFactory
public class SQLEditorSemanticAnnotationModelFactory implements IAnnotationModelFactory {

    @Override
    public IAnnotationModel createAnnotationModel(IPath location) {
        IFile file = FileBuffers.getWorkspaceFileAtLocation(location);
        return file != null ? new ResourceMarkerSemanticAnnotationModel(file) : new AnnotationModel();
    }

    private static class ResourceMarkerSemanticAnnotationModel extends ResourceMarkerAnnotationModel {

        public ResourceMarkerSemanticAnnotationModel(IResource file) {
            super(file);
        }

        @Override
        protected void addMarkerUpdater(IMarkerUpdater markerUpdater) {
            if (markerUpdater instanceof BasicMarkerUpdater defaultMarkerUpdater) {
                // BasicMarkerUpdater fucks up the position object associated with semantic marker annotation, so intercept it when needed.
                // Apparently it is due to the error in the condition in the BasicMarkerUpdater::updateMarker(..) method
                // where the MarkerUtilities.setLineNumber(..) is being forced when not needed:
                //     line number is being inferred from the position object, then leading to the incorrect offsets being derived
                //     and assigned back to the position object somewhere upper on the callstack with the fruitless help of
                //     AbstractMarkerAnnotationModel::createPositionFromMarker(..), which consumes only the line number and
                //     also incorrectly infers `end = start`, which is obviously wrong even if the line number usage is intended.
                markerUpdater = new IMarkerUpdater() {
                    @Override
                    public String getMarkerType() {
                        return defaultMarkerUpdater.getMarkerType();
                    }

                    @Override
                    public String[] getAttribute() {
                        return defaultMarkerUpdater.getAttribute();
                    }

                    @Override
                    public boolean updateMarker(IMarker iMarker, IDocument iDocument, Position position) {
                        if (isSemanticErrorMarker(iMarker)) {
                            if (position == null) {
                                return true;
                            } else if (position.isDeleted()) {
                                return false;
                            } else {
                                return true;
                            }
                        } else {
                            return defaultMarkerUpdater.updateMarker(iMarker, iDocument, position);
                        }
                    }

                    private static boolean isSemanticErrorMarker(IMarker marker) {
                        try {
                            return marker.getType().equalsIgnoreCase(SQLSemanticErrorAnnotation.MARKER_TYPE);
                        } catch (CoreException e) {
                            return false;
                        }
                    }
                };
            }
            super.addMarkerUpdater(markerUpdater);
        }
    }
}
