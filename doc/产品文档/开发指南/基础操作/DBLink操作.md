本文档介绍如何在DBeaver中创建，查看YashanDB的数据库连接。

## 权限需求
使用DBeaver for YashanDB管理DBLink，请确保当前用户具有以下权限。

| 权限                          | 说明        |
|-----------------------------|-----------|
| CREATE PUBLIC DATABASE LINK | 创建公有数据库链接 |
| CREATE DATABASE LINK        | 创建数据库连接   |
| ALTER DATABASE LINK         | 修改私有数据库链接 |
| ALTER PUBLIC DATABASE LINK  | 修改公有数据库链接 |
| DROP DATABASE LINK          | 删除私有数据库链接 |
| DROP PUBLIC DATABASE LINK   | 删除公有数据库链接 |


## 创建数据库连接
创建示例SQL语句
```sql
CREATE DATABASE LINK dbeaver CONNECT TO TEST01 IDENTIFIED BY ****** USING '***.***.**.***:****'
```

在左侧点击**模式**，点击**schema**，点击**数据库连接**，将展示创建的连接。

![查看数据库连接](./image/查看数据库连接.png)


点击**查看数据库连接**，或**双击连接**查看数据库连接详情。

![连接详情](./image/连接详情.png)

在全局元数据中**数据库连接**查看public数据库连接。

![全局元数据下连接](./image/全局元数据下连接.png)