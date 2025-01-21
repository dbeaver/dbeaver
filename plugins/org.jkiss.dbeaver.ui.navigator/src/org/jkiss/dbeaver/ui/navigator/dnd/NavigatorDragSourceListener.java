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
package org.jkiss.dbeaver.ui.navigator.dnd;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.ui.part.EditorInputTransfer;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNStreamData;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dnd.DatabaseObjectTransfer;
import org.jkiss.dbeaver.ui.dnd.TreeNodeTransfer;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.StandardConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class NavigatorDragSourceListener implements DragSourceListener {
    private static final Log log = Log.getLog(NavigatorDragSourceListener.class);

    private final Viewer viewer;
    private IStructuredSelection selection;
    private Path tempFolder;

    public NavigatorDragSourceListener(Viewer viewer) {
        this.viewer = viewer;
    }

    private synchronized Path getTempFolder() {
        if (tempFolder == null) {
            try {
                tempFolder = DBWorkbench.getPlatform().getTempFolder(new VoidProgressMonitor(), "dnd-files");
            } catch (IOException e) {
                log.error(e);
                tempFolder = Path.of(System.getProperty(StandardConstants.ENV_TMP_DIR));
            }
        }
        return tempFolder;
    }

    @Override
    public void dragStart(DragSourceEvent event) {
        selection = (IStructuredSelection) viewer.getSelection();
    }

    @Override
    public void dragSetData(DragSourceEvent event) {
        if (!selection.isEmpty()) {
            final Map<DBNNode, NavigatorTransferInfo> info = collectNodesInfo(event);
            if (!info.isEmpty()) {
                if (TreeNodeTransfer.getInstance().isSupportedType(event.dataType)) {
                    event.data = info.keySet();
                } else if (DatabaseObjectTransfer.getInstance().isSupportedType(event.dataType)) {
                    event.data = info.values().stream()
                        .map(NavigatorTransferInfo::getObject)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
                    event.data = info.values().stream()
                        .map(NavigatorTransferInfo::getName)
                        .collect(Collectors.joining(CommonUtils.getLineSeparator()));
                } else if (EditorInputTransfer.getInstance().isSupportedType(event.dataType)) {
                    event.data = info.values().stream()
                        .map(NavigatorTransferInfo::createEditorInputData)
                        .filter(Objects::nonNull)
                        .toArray(EditorInputTransfer.EditorInputData[]::new);
                } else if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
                    event.data = info.values().stream()
                        .map(NavigatorTransferInfo::getName)
                        .filter(name -> Files.exists(Path.of(name)))
                        .toArray(String[]::new);
                }
                return;
            }
        }
        setEmptyData(event);
    }

    @NotNull
    private Map<DBNNode, NavigatorTransferInfo> collectNodesInfo(DragSourceEvent event) {
        final Map<DBNNode, NavigatorTransferInfo> info = new LinkedHashMap<>();

        for (Object nextSelected : selection) {
            if (!(nextSelected instanceof DBNNode node)) {
                continue;
            }

            String nodeName;
            Object nodeObject = null;

            if (nextSelected instanceof DBNDatabaseNode && !(nextSelected instanceof DBNDataSource)) {
                DBSObject object = ((DBNDatabaseNode) nextSelected).getObject();
                if (object == null) {
                    continue;
                }
                nodeName = DBUtils.getObjectFullName(object, DBPEvaluationContext.UI);
                nodeObject = object;
            } else if (nextSelected instanceof DBNDataSource) {
                DBPDataSourceContainer object = ((DBNDataSource) nextSelected).getDataSourceContainer();
                nodeName = object.getName();
                nodeObject = object;
            } else if (nextSelected instanceof DBNStreamData streamData && streamData.supportsStreamData()
                && (EditorInputTransfer.getInstance().isSupportedType(event.dataType)
                    || FileTransfer.getInstance().isSupportedType(event.dataType))) {
                String fileName = node.getNodeDisplayName();
                try {
                    Path tmpFile = copyStreamToTempFile(streamData, fileName);
                    if (tmpFile == null) {
                        continue;
                    }
                    nodeObject = tmpFile;
                    nodeName = tmpFile.toAbsolutePath().toString();
                } catch (Exception e) {
                    log.error(e.getMessage());
                    continue;
                }
            } else {
                nodeName = node.getNodeTargetName();
            }

            info.put(node, new NavigatorTransferInfo(nodeName, node, nodeObject));
        }
        return info;
    }

    @Nullable
    private Path copyStreamToTempFile(DBNStreamData streamData, String fileName) throws InvocationTargetException, InterruptedException {
        Path tmpFile = getTempFolder().resolve(CommonUtils.escapeFileName(fileName));
        if (!Files.exists(tmpFile)) {
            try {
                Files.createFile(tmpFile);
            } catch (IOException e) {
                log.error("Can't create new file" + tmpFile.toAbsolutePath(), e);
                return null;
            }
            // Start writing to stream and lock it
            UIUtils.runInProgressService(monitor -> {
                try {
                    long streamSize = streamData.getStreamSize();
                    try (InputStream is = streamData.openInputStream()) {
                        try (OutputStream out = Files.newOutputStream(tmpFile)) {
                            ContentUtils.copyStreams(is, streamSize, out, monitor);
                        }
                    }
                } catch (Exception e) {
                    try {
                        Files.delete(tmpFile);
                    } catch (IOException ex) {
                        log.error("Error deleting temp file " + tmpFile.toAbsolutePath(), e);
                    }
                    throw new InvocationTargetException(e);
                }
            });
        }
        return tmpFile;
    }

    private void setEmptyData(DragSourceEvent event) {
        if (TreeNodeTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = Collections.emptyList();
        } else if (DatabaseObjectTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = Collections.emptyList();
        } else if (TextTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = "";
        } else if (EditorInputTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = new EditorInputTransfer.EditorInputData[0];
        } else if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
            event.data = new String[0];
        }
    }

    @Override
    public void dragFinished(DragSourceEvent event) {
        // Delete temporary files if needed
        selection = null;
    }

}
