/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.yashandb.model;

import java.math.BigDecimal;
import java.sql.ResultSet;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

public class YashanDBDataFile extends YashanDBObject<YashanDBTablespace> {

	private final YashanDBTablespace tablespace;
	private long id;
	private BigDecimal bytes;
	private BigDecimal blocks;
	private BigDecimal maxBytes;
	private long maxBlocks;
	private BigDecimal userBytes;
	private BigDecimal userBlocks;
	private long nextSize;
	public Status status;
	public AutoExtend autoExtend;

	protected YashanDBDataFile(YashanDBTablespace tablespace, ResultSet dbResult, boolean temporary) {
		super(tablespace, JDBCUtils.safeGetString(dbResult, "FILE_NAME"), true);
		this.tablespace = tablespace;
		this.status = CommonUtils.valueOf(Status.class, JDBCUtils.safeGetString(dbResult, "STATUS"), null, true);
		this.autoExtend = CommonUtils.valueOf(AutoExtend.class, JDBCUtils.safeGetString(dbResult, "AUTO_EXTEND"), null,
				true);
		this.id = JDBCUtils.safeGetLong(dbResult, "FILE_ID");
		this.bytes = JDBCUtils.safeGetBigDecimal(dbResult, "BYTES");
		this.blocks = JDBCUtils.safeGetBigDecimal(dbResult, "BLOCKS");
		this.maxBytes = JDBCUtils.safeGetBigDecimal(dbResult, "MAXBYTES");
		this.maxBlocks = JDBCUtils.safeGetLong(dbResult, "MAXBLOCKS");
		this.userBytes = JDBCUtils.safeGetBigDecimal(dbResult, "USER_BYTES");
		this.userBlocks = JDBCUtils.safeGetBigDecimal(dbResult, "USER_BLOCKS");
		this.nextSize = JDBCUtils.safeGetLong(dbResult, "NEXT_SIZE");
	}

	public YashanDBTablespace getTablespace() {
		return tablespace;
	}

	@NotNull
	@Override
	@Property(viewable = true, editable = true, order = 1)
	public String getName() {
		return name;
	}

	@Property(order = 2)
	public long getId() {
		return id;
	}

	@Property(viewable = true, order = 3)
	public BigDecimal getBytes() {
		return bytes;
	}

	@Property(viewable = true, order = 4)
	public BigDecimal getBlocks() {
		return blocks;
	}

	@Property(viewable = true, order = 5)
	public Status getStatus() {
		return status;
	}

	@Property(viewable = true, order = 6)
	public BigDecimal getMaxBytes() {
		return maxBytes;
	}

	@Property(viewable = true, order = 7)
	public long getMaxBlocks() {
		return maxBlocks;
	}

	@Property(viewable = true, order = 8)
	public AutoExtend getAutoExtend() {
		return autoExtend;
	}

	@Property(viewable = true, order = 9)
	public long getNextSize() {
		return nextSize;
	}

	@Property(viewable = true, order = 10)
	public BigDecimal getUserBytes() {
		return userBytes;
	}

	@Property(viewable = true, order = 11)
	public BigDecimal getUserBlocks() {
		return userBlocks;
	}

	public enum Status {
		ONLINE, OFFLINE
	}

	public enum AutoExtend {
		ON, OFF
	}
}
