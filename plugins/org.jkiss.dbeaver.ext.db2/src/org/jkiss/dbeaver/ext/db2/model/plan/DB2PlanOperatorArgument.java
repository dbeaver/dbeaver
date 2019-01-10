/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model.plan;

import java.util.HashMap;
import java.util.Map;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPNamedValueObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;

/**
 * DB2 EXPLAIN_ARGUMENT table
 * 
 * @author Denis Forveille
 */
public class DB2PlanOperatorArgument implements DBPNamedValueObject {

    private static final Map<String, String> ARGUMENT_TYPES = new HashMap<>(256); // See init below

    private String argumentType;
    private String argumentValue;

    // ------------
    // Constructors
    // ------------

    public DB2PlanOperatorArgument(JDBCResultSet dbResult)
    {

        this.argumentType = JDBCUtils.safeGetStringTrimmed(dbResult, "ARGUMENT_TYPE");
        this.argumentValue = JDBCUtils.safeGetStringTrimmed(dbResult, "ARGUMENT_VALUE");
        if (argumentValue == null) {
            this.argumentValue = JDBCUtils.safeGetString(dbResult, "LONG_ARGUMENT_VALUE");
        }
    }

    // -----------------
    // Business contract
    // -----------------
    @NotNull
    @Override
    public String getName()
    {
        String desc = ARGUMENT_TYPES.get(argumentType);
        if (desc == null) {
            return argumentType;
        } else {
            return desc;
        }
    }

    @NotNull
    @Override
    public Object getObjectValue()
    {
        return argumentValue;
    }

    @Override
    public String toString()
    {
        return argumentValue;
    }

    // ----------------
    // Standard Getters
    // ----------------

    public String getArgumentValue()
    {
        return argumentValue;
    }

    public String getArgumentType()
    {
        return argumentType;
    }

    static {
        ARGUMENT_TYPES.put("BLDLEVEL", "Build Level");
        ARGUMENT_TYPES.put("HEAPUSE", "Heap Use");
        ARGUMENT_TYPES.put("PREPTIME", "Preparation Time");

        ARGUMENT_TYPES.put("AGGMODE", "Aggregate Mode");
        ARGUMENT_TYPES.put("APREUSE", "Reuse Bind Option?");
        ARGUMENT_TYPES.put("BACKJOIN", "Back join?");
        ARGUMENT_TYPES.put("BITFLTR", "Size of Hash Join Bit Filter");
        ARGUMENT_TYPES.put("BLD_LEVEL", "Build Level");
        ARGUMENT_TYPES.put("BLKLOCK", "Block level lock intent");
        ARGUMENT_TYPES.put("BUFFSORT", "SORT used as a buffering operation?");
        ARGUMENT_TYPES.put("BUSTSENS", "BUSTIMESENSITIVE bind option?");
        ARGUMENT_TYPES.put("BY DPART", "ZZJN is performed?");
        ARGUMENT_TYPES.put("CONCACCR", "Concurrent access resolution");
        ARGUMENT_TYPES.put("CSERQY", "Common subexpression?");
        ARGUMENT_TYPES.put("CSETEMP", "Temp.Table over Common Subexpression");
        ARGUMENT_TYPES.put("CUR_COMM", "Access currently committed rows?");
        ARGUMENT_TYPES.put("DEGREE", "Nb of column-organized processing subagents that are used");
        ARGUMENT_TYPES.put("DIRECT", "Direct fetch?");
        ARGUMENT_TYPES.put("DPESTFLG", "DPNUMPRT value based on an estimate?");
        ARGUMENT_TYPES.put("DPFXMLMV", "XML col data moved between DPF part.");
        ARGUMENT_TYPES.put("DPLSTPRT", "Accessed data partitions");
        ARGUMENT_TYPES.put("DPNUMPRT", "Actual or est. nb of data partitions accessed");
        ARGUMENT_TYPES.put("DSTSEVER", "Destination (ship from) server");
        ARGUMENT_TYPES.put("DUPLWARN", "Duplicates Warning?");
        ARGUMENT_TYPES.put("EARLYOUT", "Early out");
        ARGUMENT_TYPES.put("ENVVAR", "Environment variable");
        ARGUMENT_TYPES.put("ERRTOL", "Errors to be tolerated");
        ARGUMENT_TYPES.put("EXTROWS", "Max row size of the syst. temp. tb might be too large?");
        ARGUMENT_TYPES.put("EVALUNCO", "Evaluate uncommitted data?");
        ARGUMENT_TYPES.put("EXECUTID", "Executable ID of the section");
        ARGUMENT_TYPES.put("FETCHMAX", "Override value for MAXPAGES");
        ARGUMENT_TYPES.put("GREEDY", "Optimizer used a greedy algorithm?");
        ARGUMENT_TYPES.put("GLOBLOCK", "Global lock intent");
        ARGUMENT_TYPES.put("GROUPBYC", "Group By columns provided?");
        ARGUMENT_TYPES.put("GROUPBYN", "Group By Nb comparison columns");
        ARGUMENT_TYPES.put("GROUPBYR", "Group By requirement");
        ARGUMENT_TYPES.put("GROUPS", "Nb of times the operator will repeat");
        ARGUMENT_TYPES.put("HASHCODE", "Size (in bits) of hash join hash code");
        ARGUMENT_TYPES.put("HASHTBSZ", "Nb of expected entries in the hash table");
        ARGUMENT_TYPES.put("IDXMSTLY", "FETCH is performed over block identifiers?");
        ARGUMENT_TYPES.put("IDXOVTMP", "Scan builds an ix or a fast integer sort structure for random access of the temp tbs");
        ARGUMENT_TYPES.put("INNERCOL", "Inner order columns");
        ARGUMENT_TYPES.put("INPUTXID", "Input context node");
        ARGUMENT_TYPES.put("ISCANMAX", "Override value for MAXPAGES argument on ISCAN operator");
        ARGUMENT_TYPES.put("JN INPUT", "Feeding Join");
        ARGUMENT_TYPES.put("JUMPSCAN", "Jump Scan?");
        ARGUMENT_TYPES.put("LCKAVOID", "Lock avoidance?");
        ARGUMENT_TYPES.put("LISTENER", "Listener Table Queue indicator?");
        ARGUMENT_TYPES.put("MAXPAGES", "Max pages expected for Prefetch");
        ARGUMENT_TYPES.put("MAXRIDS", "Max RIDs to be included in each list prefetch");
        ARGUMENT_TYPES.put("MXPPSCAN", "How MAXPAGES is calculated?");
        ARGUMENT_TYPES.put("NUMROWS", "Nb of rows expected to be sorted");
        ARGUMENT_TYPES.put("ONEFETCH", "One Fetch?");
        ARGUMENT_TYPES.put("OPROFERR", "One or more errors occurred while parsing or applying the optimization profile");
        ARGUMENT_TYPES.put("OUTERCOL", "Outer order columns");
        ARGUMENT_TYPES.put("OUTERJN", "Outer Join");
        ARGUMENT_TYPES.put("OVERHEAD", "Optimizer used OVERHEAD value");
        ARGUMENT_TYPES.put("PARTCOLS", "Partitioning columns for operator");
        ARGUMENT_TYPES.put("PBLKLOCK", "Positioning scan table lock intent");
        ARGUMENT_TYPES.put("PFTCHSZ", "Prefetch Size");
        ARGUMENT_TYPES.put("PGLOLOCK", "Positioning scan global table lock intent");
        ARGUMENT_TYPES.put("PLANID", "Id uniquely representing a query plan configuration for a given statement");
        ARGUMENT_TYPES.put("PREFETCH", "Prefetch");
        ARGUMENT_TYPES.put("PREFETCHSIZE", "Prefetch Size");
        ARGUMENT_TYPES.put("PROWLOCK", "Positioning scan row lock intent");
        ARGUMENT_TYPES.put("PTABLOCK", "Positioning scan table lock intent");
        ARGUMENT_TYPES.put("RAND ACC", "Regular TEMP tbl allows random access?");
        ARGUMENT_TYPES.put("REOPT", "Statement is optimized using bind-in values");
        ARGUMENT_TYPES.put("RMTQTXT", "Remote Query Text");
        ARGUMENT_TYPES.put("RNG_PROD", "Range producing function");
        ARGUMENT_TYPES.put("ROWLOCK", "Row Lock Intent");
        ARGUMENT_TYPES.put("ROWWIDTH", "Width of row to be sorted");
        ARGUMENT_TYPES.put("RSUFFIX", "Remote SQL suffix");
        ARGUMENT_TYPES.put("SCANDIR", "Scan Direction");
        ARGUMENT_TYPES.put("SCANGRAN", "Intrapartition parallelism, granularity");
        ARGUMENT_TYPES.put("SCANSPEED", "Scan Speed");
        ARGUMENT_TYPES.put("SCANTYPE", "Intrapartition parallelism data scan");
        ARGUMENT_TYPES.put("SCANUNIT", "Intrapartition parallelism, scan granularity unit");
        ARGUMENT_TYPES.put("SEMEVID", "Identifier for semantic env. at the time the statement was compiled");
        ARGUMENT_TYPES.put("SEMIJOIN", "Semi-join optimization?");
        ARGUMENT_TYPES.put("SHARED", "Intrapartition parallelism, shared TEMP?");
        ARGUMENT_TYPES.put("SHRCSE", "Temp table over common subexp.n shared between subsection?");
        ARGUMENT_TYPES.put("SKIP_INS", "Skip inserted?");
        ARGUMENT_TYPES.put("SKIPDKEY", "Skip deleted keys?");
        ARGUMENT_TYPES.put("SKIPDROW", "Skip deleted rows?");
        ARGUMENT_TYPES.put("SKIPLOCK", "Skip locked data?");
        ARGUMENT_TYPES.put("SLOWMAT", "Slow Materialization?");
        ARGUMENT_TYPES.put("SNGLPROD", "Intrapartition parallelism sort or temp produced by a single agent");
        ARGUMENT_TYPES.put("SORTKEY", "Sort key columns");
        ARGUMENT_TYPES.put("SORTTYPE", "Intrapartition parallelism, sort type");
        ARGUMENT_TYPES.put("SRCSEVER", "Source (ship to) server");
        ARGUMENT_TYPES.put("SPEED", "Scan Speed");
        ARGUMENT_TYPES.put("SPILLED", "Est. nb of pages in SORT spill");
        ARGUMENT_TYPES.put("SQLCA", "Warnings and reason codes issued during Explain");
        ARGUMENT_TYPES.put("STARJOIN", "Star Join?");
        ARGUMENT_TYPES.put("STMTHEAP", "Stmt Heap");
        ARGUMENT_TYPES.put("STMTID", "Normalized form of the SQL statement");
        ARGUMENT_TYPES.put("STREAM", "Remote source is streaming?");
        ARGUMENT_TYPES.put("SYSTSENS", "SYSTIMESENSITIVE bind option is in effect?");
        ARGUMENT_TYPES.put("TABLOCK", "Table Lock Intent");
        ARGUMENT_TYPES.put("TBISOLVL", "Table access Isolation Level)");
        ARGUMENT_TYPES.put("TEMPSIZE", "Temporary table page size");
        ARGUMENT_TYPES.put("THROTTLE", "Throttle?");
        ARGUMENT_TYPES.put("TMPCMPRS", "Compression is applied?");
        ARGUMENT_TYPES.put("TQDEGREE", "Intrapartition parallelism, number of subagents accessing Table Queue");
        ARGUMENT_TYPES.put("TQMERGE", "Merging (sorted) Table Queue?");
        ARGUMENT_TYPES.put("TQNUMBER", "able queue identification number");
        ARGUMENT_TYPES.put("TQREAD", "Table Queue reading");
        ARGUMENT_TYPES.put("TQSECNFM", "Subection number at the sending end of the table queue");
        ARGUMENT_TYPES.put("TQSECNTO", "Subsection number at the receiving end of the table queue");
        ARGUMENT_TYPES.put("TQSEND", "Table Queue send");
        ARGUMENT_TYPES.put("TQ TYPE", "Intrapartition parallelism, Table Queue");
        ARGUMENT_TYPES.put("TQORIGIN", "Reason that TQ was introduced into the access plan");
        ARGUMENT_TYPES.put("TRANSFERRATE", "Transfer rate");
        ARGUMENT_TYPES.put("TQORIGIN", "Truncated Table Queue");
        ARGUMENT_TYPES.put("TRUNCTQ", "Truncated Table Queue");
        ARGUMENT_TYPES.put("TRUNCSRT", "Truncated sort");
        ARGUMENT_TYPES.put("TUPBLKSZ", "Nb of bytes that a tuple will be stored in");
        ARGUMENT_TYPES.put("UNIQUE", "Eliminates rows having dup values");
        ARGUMENT_TYPES.put("UNIQKEY", "Unique key columns");
        ARGUMENT_TYPES.put("UR_EXTRA", "UR isolation with extra processing");
        ARGUMENT_TYPES.put("USAGE", "NLJOIN  useage");
        ARGUMENT_TYPES.put("VISIBLE", "Shared scans are visible to other shared scans?");
        ARGUMENT_TYPES.put("VOLATILE", "Volatile table?");
        ARGUMENT_TYPES.put("WRAPPING", "Scan wrapping allowed?");
        ARGUMENT_TYPES.put("XFERRATE", "Optimizer used TRANSFERRATE value");
        ARGUMENT_TYPES.put("XDFOUT", "Expected number of documents to be returned ");
        ARGUMENT_TYPES.put("XLOGID", "Index over XML data chosen by the optimizer");
        ARGUMENT_TYPES.put("XPATH", "Evaluation of an XPATH expression");
        ARGUMENT_TYPES.put("XPHYID", "Physical ix associated with an index over XML data");
    }

}
