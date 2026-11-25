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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverWithLazyIcon;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObjectState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class DBNDataSourceTest {
    @Test
    public void connectionStateDoesNotModifySharedDriverIcon() {
        DBPImage vendorOverlay = Mockito.mock(DBPImage.class);
        DBIconComposite driverIcon = new DBIconComposite(
            DBIcon.DATABASE_DEFAULT, false, null, null, null, vendorOverlay);

        DBIconComposite connectedIcon = (DBIconComposite) DBNModel.getStateOverlayImage(driverIcon, DBSObjectState.ACTIVE);
        Assertions.assertSame(DBIcon.OVER_SUCCESS, connectedIcon.getBottomRight());
        Assertions.assertSame(vendorOverlay, driverIcon.getBottomRight());

        DBIconComposite disconnectedIcon = (DBIconComposite) DBNModel.getStateOverlayImage(driverIcon, DBSObjectState.NORMAL);
        Assertions.assertSame(vendorOverlay, disconnectedIcon.getBottomRight());
        Assertions.assertNotSame(connectedIcon, disconnectedIcon);

        DBIconComposite failedIcon = (DBIconComposite) DBNModel.getStateOverlayImage(driverIcon, DBSObjectState.INVALID);
        Assertions.assertSame(DBIcon.OVER_ERROR, failedIcon.getBottomRight());
        Assertions.assertSame(DBIcon.OVER_SUCCESS, connectedIcon.getBottomRight());
        Assertions.assertSame(vendorOverlay, driverIcon.getBottomRight());
    }

    @Test
    public void connectionStatePreservesOtherDriverDecorations() {
        DBIconComposite driverIcon = new DBIconComposite(
            DBIcon.DATABASE_DEFAULT, true, DBIcon.OVER_UNKNOWN, DBIcon.OVER_EXTERNAL, DBIcon.OVER_LOCK, null);

        DBIconComposite connectedIcon = (DBIconComposite) DBNModel.getStateOverlayImage(driverIcon, DBSObjectState.ACTIVE);

        Assertions.assertSame(driverIcon.getMain(), connectedIcon.getMain());
        Assertions.assertTrue(connectedIcon.isDisabled());
        Assertions.assertSame(DBIcon.OVER_UNKNOWN, connectedIcon.getTopLeft());
        Assertions.assertSame(DBIcon.OVER_EXTERNAL, connectedIcon.getTopRight());
        Assertions.assertSame(DBIcon.OVER_LOCK, connectedIcon.getBottomLeft());
        Assertions.assertNull(driverIcon.getBottomRight());
    }

    @Test
    public void loadDriverIconAndRefreshNode() {
        DBNModel model = Mockito.mock(DBNModel.class);
        DBNNode parent = Mockito.mock(DBNNode.class);
        Mockito.when(parent.getModel()).thenReturn(null, model);

        DBPDriver driver = Mockito.mock(
            DBPDriver.class, Mockito.withSettings().extraInterfaces(DBPDriverWithLazyIcon.class));
        Mockito.when(driver.getNavigatorRoot()).thenReturn(Mockito.mock(DBXTreeNode.class));
        DBPDataSourceContainer dataSource = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(dataSource.getDriver()).thenReturn(driver);

        DBNDataSource node = new DBNDataSource(parent, dataSource);
        node.getNodeIcon();
        node.getNodeIcon();

        ArgumentCaptor<Runnable> callbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        Mockito.verify((DBPDriverWithLazyIcon) driver, Mockito.times(2)).loadIcon(callbackCaptor.capture());
        Assertions.assertSame(callbackCaptor.getAllValues().get(0), callbackCaptor.getAllValues().get(1));

        callbackCaptor.getValue().run();
        Mockito.verify(model).fireNodeUpdate(node, node, DBNEvent.NodeChange.STRUCT_REFRESH);
    }
}
