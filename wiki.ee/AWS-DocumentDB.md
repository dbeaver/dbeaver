<!-- META products: [LE, EE, UE, TE] -->
<!-- START doctoc -->
#### Table of contents
- [Connections](#connections)
- [Queries](#queries)
<!-- END doctoc -->

**Note**: This driver is available in [Lite](Lite-Edition), [Enterprise](Enterprise-Edition), [Ultimate](Ultimate-Edition) and <a href="https://dbeaver.com/dbeaver-team-edition">Team</a> editions only.
AWS DocumentDB is based on the [[MongoDB]] engine.  
It has several minor differences in the query processing and network configuration.  
However, most features which work for MongoDB will work for DocumentDB as well. Please refer to the [[MongoDB]] article. 

### Connections

AWS restricts direct access to DocumentDB clusters from outside of the cloud (region). So you can connect to it directly (using a cluster host name) only when DBeaver is deployed on the EC2 instance.  

In other cases you will need to use the SSH tunnel through a proxy machine to access DocumentDB instance. Please read the AWS Documentation about proxy configurations: https://docs.aws.amazon.com/documentdb/latest/developerguide/connect-from-outside-a-vpc.html

In DBeaver you can use the SSH tab on the connection settings page. Just enter proxy host, user name and specify a private key file (it is provided by AWS as a keypair).

### Queries

DBeaver processes DocDB SQL queries exactly like in [[MongoDB]]. It supports SELECT, UPDATE, INSERT and DELETE queries.  
SELECT queries support WHERE, ORDER BY, GROUP BY and HAVING clauses.

DocumentDB restricts the `eval` function so all JavaScript queries will be parsed on the client's side and then evaluated at a DocDB cluster one by one.
Most JS functions work exactly like in Mongo Shell.
