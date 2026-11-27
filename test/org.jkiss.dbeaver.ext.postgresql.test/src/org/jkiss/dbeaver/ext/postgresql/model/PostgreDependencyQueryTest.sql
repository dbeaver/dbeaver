-- Original query retained as an integration-test oracle.
SELECT DISTINCT dep.deptype, dep.classid, dep.{queryObjectId}, cl.relkind, attr.attname,pg_get_expr(ad.adbin, ad.adrelid) adefval,
    CASE WHEN cl.relkind IS NOT NULL THEN cl.relkind::text || COALESCE(dep.objsubid::text, '')::text
        WHEN tg.oid IS NOT NULL THEN 'T'::text
        WHEN ty.oid IS NOT NULL THEN 'y'::text
        WHEN ns.oid IS NOT NULL THEN 'n'::text
        WHEN pr.oid IS NOT NULL THEN 'p'::text
        WHEN la.oid IS NOT NULL THEN 'l'::text
        WHEN rw.oid IS NOT NULL THEN 'R'::text
        WHEN co.oid IS NOT NULL THEN 'C'::text || contype::text
        WHEN ad.oid IS NOT NULL THEN 'A'::text
        ELSE ''
    END AS type,
    COALESCE(coc.relname, clrw.relname, tgr.relname) AS ownertable,
    CASE WHEN cl.relname IS NOT NULL AND att.attname IS NOT NULL THEN cl.relname || '.' || att.attname
    ELSE COALESCE(cl.relname, co.conname, pr.proname, tg.tgname, ty.typname, la.lanname, rw.rulename, ns.nspname)
    END AS refname,
    COALESCE(nsc.nspname, nso.nspname, nsp.nspname, nst.nspname, nsrw.nspname, tgrn.nspname) AS nspname
FROM pg_depend dep
LEFT JOIN pg_class cl ON dep.{queryObjectId}=cl.oid
LEFT JOIN pg_attribute att ON dep.{queryObjectId}=att.attrelid AND dep.objsubid=att.attnum
LEFT JOIN pg_namespace nsc ON cl.relnamespace=nsc.oid
LEFT JOIN pg_proc pr ON dep.{queryObjectId}=pr.oid
LEFT JOIN pg_namespace nsp ON pr.pronamespace=nsp.oid
LEFT JOIN pg_trigger tg ON dep.{queryObjectId}=tg.oid
LEFT JOIN pg_class tgr ON tg.tgrelid=tgr.oid
LEFT JOIN pg_namespace tgrn ON tgr.relnamespace=tgrn.oid
LEFT JOIN pg_type ty ON dep.{queryObjectId}=ty.oid
LEFT JOIN pg_namespace nst ON ty.typnamespace=nst.oid
LEFT JOIN pg_constraint co ON dep.{queryObjectId}=co.oid
LEFT JOIN pg_class coc ON co.conrelid=coc.oid
LEFT JOIN pg_namespace nso ON co.connamespace=nso.oid
LEFT JOIN pg_rewrite rw ON dep.{queryObjectId}=rw.oid
LEFT JOIN pg_class clrw ON clrw.oid=rw.ev_class
LEFT JOIN pg_namespace nsrw ON clrw.relnamespace=nsrw.oid
LEFT JOIN pg_language la ON dep.{queryObjectId}=la.oid
LEFT JOIN pg_namespace ns ON dep.{queryObjectId}=ns.oid
LEFT JOIN pg_attrdef ad ON ad.oid=dep.{queryObjectId}
LEFT JOIN pg_attribute attr ON attr.attrelid=ad.adrelid and attr.attnum=ad.adnum
WHERE dep.{conditionObjectId}=?
ORDER BY type;
