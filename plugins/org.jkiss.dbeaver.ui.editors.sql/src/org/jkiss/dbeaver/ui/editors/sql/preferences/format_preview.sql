Select * From TABLE1 t Where a > 100 AND b between 12 AND 45;

SELECT t.*,j1.x,j2.y FROM TABLE1 t JOIN JT1 j1 ON j1.a = t.a
LEFT OUTER JOIN JT2 j2 ON j2.a=t.a AND j2.b=j1.b
WHERE t.xxx NOT NULL;

DELETE FROM TABLE1 WHERE a=1;

UPDATE TABLE1 SET a=2 WHERE a=1

SELECT table1.id, table2.number, SUM(table1.amount) FROM table1 INNER JOIN table2 ON table.id = table2.table1_id
WHERE table1.id IN (SELECT table1_id FROM table3 WHERE table3.name = 'Foo Bar' and table3.type = 'unknown_type')
GROUP BY table1.id, table2.number ORDER BY table1.id;
