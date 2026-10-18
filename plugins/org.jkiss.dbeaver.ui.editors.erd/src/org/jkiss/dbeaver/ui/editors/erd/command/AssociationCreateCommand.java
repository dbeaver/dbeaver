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
package org.jkiss.dbeaver.ui.editors.erd.command;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.erd.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityForeignKey;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.erd.editor.ERDEditorPart;
import org.jkiss.dbeaver.ui.editors.object.struct.EditForeignKeyPage;
import org.jkiss.utils.CommonUtils;

import java.util.Collections;
import java.util.List;

/**
 * Command to create association
 */
public class AssociationCreateCommand extends Command {

    private static final Log log = Log.getLog(AssociationCreateCommand.class);

    protected ERDAssociation association;
    protected ERDElement<?> sourceEntity;
    protected ERDElement<?> targetEntity;

    protected List<ERDEntityAttribute> sourceAttributes;
    protected List<ERDEntityAttribute> targetAttributes;
    private ERDEditorPart editor;
    private AbstractJob directionLookupJob;
    private int directionLookupGeneration;
    private boolean undone;

    public AssociationCreateCommand() {
    }

    @Nullable
    public ERDEntityAttribute getSourceAttribute() {
        return CommonUtils.isEmpty(sourceAttributes) ? null : sourceAttributes.getFirst();
    }

    @Nullable
    public ERDEntityAttribute getTargetAttribute() {
        return CommonUtils.isEmpty(targetAttributes) ? null : targetAttributes.getFirst();
    }


    public void setAttributes(@NotNull ERDEntityAttribute sourceAttribute, @NotNull ERDEntityAttribute targetAttribute) {
        this.sourceAttributes = Collections.singletonList(sourceAttribute);
        this.targetAttributes = Collections.singletonList(targetAttribute);
    }

    @Override
    public boolean canExecute() {
        if (sourceEntity.equals(targetEntity)) {
            return false;
        } else {

            if (targetEntity == null) {
                return false;
            } else {
                return !isAssociationExists();
            }
        }
    }

    protected boolean isAssociationExists() {
        if (sourceEntity instanceof ERDEntity && targetEntity instanceof ERDEntity &&
            !CommonUtils.isEmpty(sourceAttributes) && !CommonUtils.isEmpty(targetAttributes)) {
            // GEF calls canExecute() on the UI thread. Determining FK direction
            // may load database metadata and must not happen here with a
            // non-cancellable VoidProgressMonitor.
            // If only one direction already exists, the other may still be
            // valid. Check the final direction during execute() instead.
            return isAssociationExists(sourceEntity, targetEntity) &&
                isAssociationExists(targetEntity, sourceEntity);
        }

        return isAssociationExists(sourceEntity, targetEntity);
    }

    private boolean isAssociationExists(@NotNull ERDElement<?> source, @NotNull ERDElement<?> target) {
        // Check for an existing relationship in the final FK -> PK direction.
        for (ERDAssociation currentRelationship : target.getReferences()) {
            if (currentRelationship.getSourceEntity().equals(source)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void execute() {
        undone = false;
        if (sourceEntity instanceof ERDEntity && targetEntity instanceof ERDEntity
            && !CommonUtils.isEmpty(sourceAttributes) && !CommonUtils.isEmpty(targetAttributes)) {
            resolveAssociationDirectionInBackground();
        } else {
            association = createAssociation(sourceEntity, targetEntity, true);
        }
    }

    private void resolveAssociationDirectionInBackground() {
        if (editor == null || editor.getDiagram() == null) {
            log.error("Cannot create ERD association without an active diagram");
            return;
        }

        // Capture the drag endpoints before scheduling; GEF models must only
        // be updated on the UI thread, never from the background job.
        final ERDElement<?> originalSource = sourceEntity;
        final ERDElement<?> originalTarget = targetEntity;
        final List<DBSEntityAttribute> srcAttrs = ERDUtils.getObjectsFromERD(sourceAttributes);
        final List<DBSEntityAttribute> refAttrs = ERDUtils.getObjectsFromERD(targetAttributes);
        final int generation = ++directionLookupGeneration;

        directionLookupJob = new AbstractJob("Determine ERD foreign key direction") {
            @NotNull
            @Override
            protected IStatus run(@NotNull DBRProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                boolean swap = false;
                try {
                    swap = needToSwapForeignKeyDirection(monitor, srcAttrs, refAttrs);
                } catch (DBException e) {
                    // Metadata lookup may fail even when the FK editor can work.
                    // Preserve the original drag direction as the fallback.
                    log.error("Cannot determine ERD foreign key direction; using drag direction", e);
                }
                if (monitor.isCanceled()) {
                    return Status.CANCEL_STATUS;
                }

                final boolean shouldSwap = swap;
                UIUtils.asyncExec(() -> {
                    // Only apply a completed lookup while this command (possibly
                    // wrapped in a GEF compound drag command) is still at the
                    // head of the active undo history. Otherwise it could update
                    // a diagram after a newer operation or an editor reset.
                    if (undone || generation != directionLookupGeneration || editor == null
                        || editor.getDiagram() == null || !containsCommand(
                            editor.getCommandStack().getUndoCommand(), AssociationCreateCommand.this)) {
                        return;
                    }
                    directionLookupJob = null;
                    association = createAssociation(originalSource, originalTarget, true, shouldSwap);
                });
                return Status.OK_STATUS;
            }
        };
        directionLookupJob.setUser(true);
        directionLookupJob.schedule();
    }

    private static boolean containsCommand(@Nullable Command candidate, @NotNull Command expected) {
        if (candidate == expected) {
            return true;
        }
        if (candidate instanceof CompoundCommand compound) {
            for (Command child : compound.getCommands()) {
                if (containsCommand(child, expected)) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    public ERDElement<?> getSourceEntity() {
        return sourceEntity;
    }

    public void setSourceEntity(@NotNull ERDElement<?> sourceEntity) {
        this.sourceEntity = sourceEntity;
    }

    @NotNull
    public ERDElement<?> getTargetEntity() {
        return targetEntity;
    }

    public void setTargetEntity(@NotNull ERDElement<?> targetEntity) {
        this.targetEntity = targetEntity;
    }

    @Nullable
    public ERDAssociation getAssociation() {
        return association;
    }

    public void setAssociation(@Nullable ERDAssociation association) {
        this.association = association;
    }

    @Override
    public void redo() {
        undone = false;
        if (association != null) {
            sourceEntity.addAssociation(association, true);
            targetEntity.addReferenceAssociation(association, true);
        } else {
            execute();
        }
    }

    @Override
    public void undo() {
        undone = true;
        ++directionLookupGeneration;
        if (directionLookupJob != null) {
            directionLookupJob.cancel();
            directionLookupJob = null;
        }
        if (association != null) {
            sourceEntity.removeAssociation(association, true);
            targetEntity.removeReferenceAssociation(association, true);
        }
    }

    @Nullable
    protected ERDAssociation createAssociation(
        @Nullable ERDElement<?> sourceEntity,
        @Nullable ERDElement<?> targetEntity,
        boolean reflect
    ) {
        return createAssociation(sourceEntity, targetEntity, reflect, false);
    }

    @Nullable
    private ERDAssociation createAssociation(
        @Nullable ERDElement<?> sourceEntity,
        @Nullable ERDElement<?> targetEntity,
        boolean reflect,
        boolean shouldSwap
    ) {
        ERDAssociation association = null;
        if (sourceEntity instanceof ERDEntity sourceERDEntity && targetEntity instanceof ERDEntity targetERDEntity) {
            // Metadata was resolved in a cancellable background job before
            // reaching this UI-only model/dialog creation path.
            if (editor == null || editor.getDiagram() == null) {
                log.error("Cannot create ERD association without an active diagram");
                return null;
            }

            DBSEntity srcEntityObject = sourceERDEntity.getObject();
            DBSEntity targetEntityObject = targetERDEntity.getObject();
            ERDEntity associationSourceERDEntity = sourceERDEntity;
            ERDEntity associationTargetERDEntity = targetERDEntity;
            boolean directionSwapped = false;

            List<DBSEntityAttribute> srcAttrs = ERDUtils.getObjectsFromERD(sourceAttributes);
            List<DBSEntityAttribute> refAttrs = ERDUtils.getObjectsFromERD(targetAttributes);

            // Swap only after the background lookup has determined the final direction.
            if (shouldSwap) {
                directionSwapped = true;
                ERDEntity tmpERDEntity = associationSourceERDEntity;
                associationSourceERDEntity = associationTargetERDEntity;
                associationTargetERDEntity = tmpERDEntity;

                DBSEntity tmpEntity = targetEntityObject;
                targetEntityObject = srcEntityObject;
                srcEntityObject = tmpEntity;

                List<DBSEntityAttribute> tmpAttrs = refAttrs;
                refAttrs = srcAttrs;
                srcAttrs = tmpAttrs;
            }

            if (isAssociationExists(associationSourceERDEntity, associationTargetERDEntity)) {
                // The final direction is known only after reading the attributes.
                // Reject duplicates even if canExecute() allowed the opposite drag.
                return null;
            }

            DBVEntity vEntity = DBVUtils.getVirtualEntity(srcEntityObject, true);
            assert vEntity != null;

            DBVEntityForeignKey vfk = EditForeignKeyPage.createVirtualForeignKey(
                vEntity,
                targetEntityObject,
                new EditForeignKeyPage.FKType[] {
                    EditForeignKeyPage.FK_TYPE_LOGICAL
                },
                srcAttrs,
                refAttrs);
            if (vfk == null) {
                return null;
            }
            vEntity.persistConfiguration();
            association = new ERDAssociation(vfk, associationSourceERDEntity, associationTargetERDEntity, true);
            if (directionSwapped) {
                // Keep undo/redo consistent with the direction of the created association.
                this.sourceEntity = associationSourceERDEntity;
                this.targetEntity = associationTargetERDEntity;
            }
        } else {
            if (sourceEntity != null && targetEntity != null) {
                association = new ERDAssociation(sourceEntity, targetEntity, true);
            }
        }
        if (association != null) {
            association.resolveAttributes();
        }
        return association;
    }

    @Nullable
    public ERDEditorPart getEditor() {
        return editor;
    }

    public void setEditor(@Nullable ERDEditorPart editor) {
        this.editor = editor;
    }

    protected boolean needToSwapForeignKeyDirection(DBRProgressMonitor monitor, List<DBSEntityAttribute> srcAttrs, List<DBSEntityAttribute> targetAttrs) throws DBException {
        // The reference must point to an identifying key, not originate from one.
        // Reverse the drag only when its source is unique and its target is not.
        return !CommonUtils.isEmpty(srcAttrs) &&
            !CommonUtils.isEmpty(targetAttrs) &&
            DBVUtils.isIdentifyingAttributes(monitor, srcAttrs) &&
            !DBVUtils.isIdentifyingAttributes(monitor, targetAttrs);
    }
}

