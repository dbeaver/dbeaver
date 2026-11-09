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
package org.jkiss.dbeaver.ext.postgresql.model.impls.redshift;

/**
 * RedshiftConstants
 */
public class RedshiftQueries {

    private static final String LOCK_QUERY_TEMPLATE = """
        WITH locks AS (
            SELECT * FROM svv_transactions WHERE lockable_object_type = 'relation'
        ), conflicts AS (
            SELECT waiting.pid AS blocked_pid, waiting.txn_owner AS blocked_user,
                   waiting.xid AS blocked_xid, holding.pid AS blocking_pid,
                   holding.txn_owner AS blocking_user, holding.xid AS blocking_xid,
                   ROW_NUMBER() OVER (
                       PARTITION BY waiting.pid
                       ORDER BY CASE WHEN holding.granted THEN 1 ELSE 0 END, holding.txn_start, holding.pid
                   ) AS lock_rank
            FROM locks waiting
            JOIN locks holding ON waiting.txn_db = holding.txn_db AND waiting.relation = holding.relation
                AND waiting.pid <> holding.pid AND waiting.xid <> holding.xid
                AND (holding.granted OR (
                    holding.lock_mode = 'AccessExclusiveLock'
                    AND (holding.txn_start < waiting.txn_start
                        OR (holding.txn_start = waiting.txn_start AND holding.pid < waiting.pid))
                ))
                AND CASE waiting.lock_mode
                    WHEN 'AccessShareLock' THEN holding.lock_mode IN ('AccessExclusiveLock')
                    WHEN 'RowShareLock' THEN holding.lock_mode IN ('ExclusiveLock', 'AccessExclusiveLock')
                    WHEN 'RowExclusiveLock' THEN holding.lock_mode IN (
                        'ShareLock', 'ShareRowExclusiveLock', 'ExclusiveLock', 'AccessExclusiveLock')
                    WHEN 'ShareUpdateExclusiveLock' THEN holding.lock_mode IN (
                        'ShareUpdateExclusiveLock', 'ShareLock', 'ShareRowExclusiveLock', 'ExclusiveLock', 'AccessExclusiveLock')
                    WHEN 'ShareLock' THEN holding.lock_mode IN (
                        'RowExclusiveLock', 'ShareUpdateExclusiveLock', 'ShareRowExclusiveLock', 'ExclusiveLock', 'AccessExclusiveLock')
                    WHEN 'ShareRowExclusiveLock' THEN holding.lock_mode IN (
                        'RowExclusiveLock', 'ShareUpdateExclusiveLock', 'ShareLock',
                        'ShareRowExclusiveLock', 'ExclusiveLock', 'AccessExclusiveLock')
                    WHEN 'ExclusiveLock' THEN holding.lock_mode IN (
                        'RowShareLock', 'RowExclusiveLock', 'ShareUpdateExclusiveLock', 'ShareLock',
                        'ShareRowExclusiveLock', 'ExclusiveLock', 'AccessExclusiveLock')
                    WHEN 'AccessExclusiveLock' THEN TRUE
                    ELSE FALSE
                END
            WHERE NOT waiting.granted
        ), queries AS (
            SELECT history.session_id, history.transaction_id, TRIM(history.query_text) AS query_text,
                   ROW_NUMBER() OVER (
                       PARTITION BY history.session_id, history.transaction_id
                       ORDER BY history.start_time DESC, history.query_id DESC
                   ) AS query_rank
            FROM sys_query_history history
            JOIN (SELECT DISTINCT pid, xid FROM locks) transactions
                ON history.session_id = transactions.pid AND history.transaction_id = transactions.xid
        ), current_queries AS (
            %s
        ), real_locks AS (
            SELECT conflicts.blocked_pid, conflicts.blocked_user, conflicts.blocking_pid, conflicts.blocking_user,
                   COALESCE(current_waiting.query_text, waiting_query.query_text) AS blocked_statement,
                   COALESCE(current_holding.query_text, holding_query.query_text) AS statement_in
            FROM conflicts
            LEFT JOIN queries waiting_query ON waiting_query.session_id = conflicts.blocked_pid
                AND waiting_query.transaction_id = conflicts.blocked_xid AND waiting_query.query_rank = 1
            LEFT JOIN queries holding_query ON holding_query.session_id = conflicts.blocking_pid
                AND holding_query.transaction_id = conflicts.blocking_xid AND holding_query.query_rank = 1
            LEFT JOIN current_queries current_waiting ON current_waiting.pid = conflicts.blocked_pid
                AND current_waiting.query_rank = 1
            LEFT JOIN current_queries current_holding ON current_holding.pid = conflicts.blocking_pid
                AND current_holding.query_rank = 1
            WHERE conflicts.lock_rank = 1
        )
        SELECT blocked_pid, blocked_user, blocking_pid, blocking_user, blocked_statement, statement_in
        FROM real_locks
        UNION
        SELECT blocking_pid, blocking_user, NULL::integer, NULL::text, statement_in, NULL::text
        FROM real_locks
        WHERE blocking_pid NOT IN (SELECT blocked_pid FROM real_locks)
        """;

    public static final String LOCK_QUERY = LOCK_QUERY_TEMPLATE.formatted(
        "SELECT NULL::integer AS pid, NULL::text AS query_text, 1 AS query_rank WHERE FALSE");

    // lock-waiting queries may not appear in sys_query_history until the lock is acquired
    public static final String LOCK_QUERY_WITH_RECENTS = LOCK_QUERY_TEMPLATE.formatted("""
        SELECT pid, TRIM(query) AS query_text,
               ROW_NUMBER() OVER (PARTITION BY pid ORDER BY starttime DESC) AS query_rank
        FROM stv_recents WHERE status = 'Running'
        """);

    public static final String LOCK_ITEM_QUERY = """
        SELECT txn_db AS datname, lockable_object_type AS locktype, relation::varchar AS relation,
               lock_mode AS mode, xid::varchar AS tid, NULL::integer AS page, NULL::integer AS tuple, pid, granted
        FROM svv_transactions
        WHERE pid = ?
        """;

    /**
     * Slightly modified official AWS script for DDL:
     * https://github.com/awslabs/amazon-redshift-utils/blob/master/src/AdminViews/v_generate_tbl_ddl.sql
     */
    public static final String DDL_EXTRACT_VIEW =
        "SELECT\n" +
            " table_id\n" +
            " ,REGEXP_REPLACE (schemaname, '^zzzzzzzz', '') AS schemaname\n" +
            " ,REGEXP_REPLACE (tablename, '^zzzzzzzz', '') AS tablename\n" +
            " ,seq\n" +
            " ,ddl\n" +
            "FROM\n" +
            " (\n" +
            " SELECT\n" +
            "  table_id\n" +
            "  ,schemaname\n" +
            "  ,tablename\n" +
            "  ,seq\n" +
            "  ,ddl\n" +
            " FROM\n" +
            "  (\n" +
            "  --DROP TABLE\n" +
            "  SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,0 AS seq\n" +
            "   ,'--DROP TABLE ' + QUOTE_IDENT(n.nspname) + '.' + QUOTE_IDENT(c.relname) + ';' AS ddl\n" +
            "  FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  --CREATE TABLE\n" +
            "  UNION SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,2 AS seq\n" +
            "   ,'CREATE TABLE IF NOT EXISTS ' + QUOTE_IDENT(n.nspname) + '.' + QUOTE_IDENT(c.relname) + '' AS ddl\n" +
            "  FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  --OPEN PAREN COLUMN LIST\n" +
            "  UNION SELECT c.oid::bigint as table_id,n.nspname AS schemaname, c.relname AS tablename, 5 AS seq, '(' AS ddl\n" +
            "  FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  --COLUMN LIST\n" +
            "  UNION SELECT\n" +
            "   table_id\n" +
            "   ,schemaname\n" +
            "   ,tablename\n" +
            "   ,seq\n" +
            "   ,'\\t' + col_delim + col_name + ' ' + col_datatype + ' ' + col_nullable + ' ' + col_default + ' ' + col_encoding AS ddl\n" +
            "  FROM\n" +
            "   (\n" +
            "   SELECT\n" +
            "    c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "    ,c.relname AS tablename\n" +
            "    ,100000000 + a.attnum AS seq\n" +
            "    ,CASE WHEN a.attnum > 1 THEN ',' ELSE '' END AS col_delim\n" +
            "    ,QUOTE_IDENT(a.attname) AS col_name\n" +
            "    ,CASE WHEN STRPOS(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER VARYING') > 0\n" +
            "      THEN REPLACE(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER VARYING', 'VARCHAR')\n" +
            "     WHEN STRPOS(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER') > 0\n" +
            "      THEN REPLACE(UPPER(format_type(a.atttypid, a.atttypmod)), 'CHARACTER', 'CHAR')\n" +
            "     ELSE UPPER(format_type(a.atttypid, a.atttypmod))\n" +
            "     END AS col_datatype\n" +
            "    ,CASE WHEN format_encoding((a.attencodingtype)::integer) = 'none'\n" +
            "     THEN 'ENCODE RAW'\n" +
            "     ELSE 'ENCODE ' + format_encoding((a.attencodingtype)::integer)\n" +
            "     END AS col_encoding\n" +
            "    ,CASE WHEN a.atthasdef IS TRUE THEN 'DEFAULT ' + adef.adsrc ELSE '' END AS col_default\n" +
            "    ,CASE WHEN a.attnotnull IS TRUE THEN 'NOT NULL' ELSE '' END AS col_nullable\n" +
            "   FROM pg_namespace AS n\n" +
            "   INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "   INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
            "   LEFT OUTER JOIN pg_attrdef AS adef ON a.attrelid = adef.adrelid AND a.attnum = adef.adnum\n" +
            "   WHERE c.relkind = 'r'\n" +
            "     AND a.attnum > 0\n" +
            "   ORDER BY a.attnum\n" +
            "   )\n" +
            "  --CONSTRAINT LIST\n" +
            "  UNION (SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,200000000 + CAST(con.oid AS INT) AS seq\n" +
            "   ,'\\t,' + pg_get_constraintdef(con.oid) AS ddl\n" +
            "  FROM pg_constraint AS con\n" +
            "  INNER JOIN pg_class AS c ON c.relnamespace = con.connamespace AND c.oid = con.conrelid\n" +
            "  INNER JOIN pg_namespace AS n ON n.oid = c.relnamespace\n" +
            "  WHERE c.relkind = 'r' AND pg_get_constraintdef(con.oid) NOT LIKE 'FOREIGN KEY%'\n" +
            "  ORDER BY seq)\n" +
            "  --CLOSE PAREN COLUMN LIST\n" +
            "  UNION SELECT c.oid::bigint as table_id,n.nspname AS schemaname, c.relname AS tablename, 299999999 AS seq, ')' AS ddl\n" +
            "  FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  --BACKUP\n" +
            "  UNION SELECT\n" +
            "  c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,300000000 AS seq\n" +
            "   ,'BACKUP NO' as ddl\n" +
            "FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  INNER JOIN (SELECT\n" +
            "    SPLIT_PART(key,'_',5) id\n" +
            "    FROM pg_conf\n" +
            "    WHERE key LIKE 'pg_class_backup_%'\n" +
            "    AND SPLIT_PART(key,'_',4) = (SELECT\n" +
            "      oid\n" +
            "      FROM pg_database\n" +
            "      WHERE datname = current_database())) t ON t.id=c.oid\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  --BACKUP WARNING\n" +
            "  UNION SELECT\n" +
            "  c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,1 AS seq\n" +
            "   ,'--WARNING: This DDL inherited the BACKUP NO property from the source table' as ddl\n" +
            "FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  INNER JOIN (SELECT\n" +
            "    SPLIT_PART(key,'_',5) id\n" +
            "    FROM pg_conf\n" +
            "    WHERE key LIKE 'pg_class_backup_%'\n" +
            "    AND SPLIT_PART(key,'_',4) = (SELECT\n" +
            "      oid\n" +
            "      FROM pg_database\n" +
            "      WHERE datname = current_database())) t ON t.id=c.oid\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  --DISTSTYLE\n" +
            "  UNION SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,300000001 AS seq\n" +
            "   ,CASE WHEN c.reldiststyle = 0 THEN 'DISTSTYLE EVEN'\n" +
            "    WHEN c.reldiststyle = 1 THEN 'DISTSTYLE KEY'\n" +
            "    WHEN c.reldiststyle = 8 THEN 'DISTSTYLE ALL'\n" +
            "    WHEN c.reldiststyle = 9 THEN 'DISTSTYLE AUTO'\n" +
            "    ELSE '<<Error - UNKNOWN DISTSTYLE>>'\n" +
            "    END AS ddl\n" +
            "  FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  --DISTKEY COLUMNS\n" +
            "  UNION SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,400000000 + a.attnum AS seq\n" +
            "   ,' DISTKEY (' + QUOTE_IDENT(a.attname) + ')' AS ddl\n" +
            "  FROM pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
            "  WHERE c.relkind = 'r'\n" +
            "    AND a.attisdistkey IS TRUE\n" +
            "    AND a.attnum > 0\n" +
            "  --SORTKEY COLUMNS\n" +
            "  UNION select table_id,schemaname, tablename, seq,\n" +
            "       case when min_sort <0 then 'INTERLEAVED SORTKEY (' else ' SORTKEY (' end as ddl\n" +
            "from (SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,499999999 AS seq\n" +
            "   ,min(attsortkeyord) min_sort FROM pg_namespace AS n\n" +
            "  INNER JOIN  pg_class AS c ON n.oid = c.relnamespace\n" +
            "  INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  AND abs(a.attsortkeyord) > 0\n" +
            "  AND a.attnum > 0\n" +
            "  group by 1,2,3,4 )\n" +
            "  UNION (SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,500000000 + abs(a.attsortkeyord) AS seq\n" +
            "   ,CASE WHEN abs(a.attsortkeyord) = 1\n" +
            "    THEN '\\t' + QUOTE_IDENT(a.attname)\n" +
            "    ELSE '\\t, ' + QUOTE_IDENT(a.attname)\n" +
            "    END AS ddl\n" +
            "  FROM  pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  INNER JOIN pg_attribute AS a ON c.oid = a.attrelid\n" +
            "  WHERE c.relkind = 'r'\n" +
            "    AND abs(a.attsortkeyord) > 0\n" +
            "    AND a.attnum > 0\n" +
            "  ORDER BY abs(a.attsortkeyord))\n" +
            "  UNION SELECT\n" +
            "   c.oid::bigint as table_id\n" +
            "   ,n.nspname AS schemaname\n" +
            "   ,c.relname AS tablename\n" +
            "   ,599999999 AS seq\n" +
            "   ,'\\t)' AS ddl\n" +
            "  FROM pg_namespace AS n\n" +
            "  INNER JOIN  pg_class AS c ON n.oid = c.relnamespace\n" +
            "  INNER JOIN  pg_attribute AS a ON c.oid = a.attrelid\n" +
            "  WHERE c.relkind = 'r'\n" +
            "    AND abs(a.attsortkeyord) > 0\n" +
            "    AND a.attnum > 0\n" +
            "  --END SEMICOLON\n" +
            "  UNION SELECT c.oid::bigint as table_id ,n.nspname AS schemaname, c.relname AS tablename, 600000000 AS seq, ';' AS ddl\n" +
            "  FROM  pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  WHERE c.relkind = 'r' \n" +
            "  \n" +
            "  UNION\n" +
            "  --TABLE OWNERSHIP AS AN ALTER TABLE STATMENT\n" +
            "  SELECT c.oid::bigint as table_id ,n.nspname AS schemaname, c.relname AS tablename, 600500000 AS seq, \n" +
            "  'ALTER TABLE ' + QUOTE_IDENT(n.nspname) + '.' + QUOTE_IDENT(c.relname) + ' owner to '+  QUOTE_IDENT(u.usename) +';' AS ddl\n" +
            "  FROM  pg_namespace AS n\n" +
            "  INNER JOIN pg_class AS c ON n.oid = c.relnamespace\n" +
            "  INNER JOIN pg_user AS u ON c.relowner = u.usesysid\n" +
            "  WHERE c.relkind = 'r'\n" +
            "  \n" +
            "  )\n" +
            "  UNION (\n" +
            "    SELECT c.oid::bigint as table_id,'zzzzzzzz' || n.nspname AS schemaname,\n" +
            "       'zzzzzzzz' || c.relname AS tablename,\n" +
            "       700000000 + CAST(con.oid AS INT) AS seq,\n" +
            "       'ALTER TABLE ' + QUOTE_IDENT(n.nspname) + '.' + QUOTE_IDENT(c.relname) + ' ADD ' + pg_get_constraintdef(con.oid)::VARCHAR(1024) + ';' AS ddl\n" +
            "    FROM pg_constraint AS con\n" +
            "      INNER JOIN pg_class AS c\n" +
            "             ON c.relnamespace = con.connamespace\n" +
            "             AND c.oid = con.conrelid\n" +
            "      INNER JOIN pg_namespace AS n ON n.oid = c.relnamespace\n" +
            "    WHERE c.relkind = 'r'\n" +
            "    AND con.contype = 'f'\n" +
            "    ORDER BY seq\n" +
            "  )\n" +
            " ORDER BY table_id,schemaname, tablename, seq\n" +
            ")";

}

