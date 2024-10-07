<!-- META products: [EE, UE, TE] -->
<!-- START doctoc -->
#### Table of contents
  - [Supported features](#supported-features)
- [DynamoDB connection](#dynamodb-connection)
  - [Database navigation](#database-navigation)
  - [Viewing table data](#viewing-table-data)
  - [Viewing data in JSON document format](#viewing-data-in-json-document-format)
  - [Executing queries](#executing-queries)
  - [Exporting and importing data](#exporting-and-importing-data)
<!-- END doctoc -->

**Note**: This driver is available in [Enterprise](Enterprise-Edition), [Ultimate](Ultimate-Edition) and <a href="https://dbeaver.com/dbeaver-team-edition">Team</a> editions only.
### Supported features
- Table data view
- Table data edit in document (json) mode
- Data filters
- SQL queries execution
- JSON queries execution
- Data export and import

## DynamoDB connection

![](images/database/dynamodb/connection-page.png)

DBeaver supports AWS Cloud and Standalone versions of DynamoDB.  
For standalone server you need to enter endpoint (http or https URL).  
For cloud server you must enter the AWS region. DynamoDB exists in all available regions in your AWS account but the tables are different.

AWS Access Key and Secret Key are used for authentication.  
For 3rd-party account access you must specify the 3rd party account ID (12-digits number) and the 3rd party role name. This role will be used for permission management. You account must be added to the whitelist in the 3rd party account.  

Press "Test Connection" to validate your connection settings.

### Database navigation

DynamoDB has a simple metadata structure. Basically, you can only access Table and Global tables.  
Table has primary attributes (a kind of primary key) and indexes.  
DynamoDB is a document-oriented database. Each table may have its own set of attributes and sub-attributes.  

![](images/database/dynamodb/database-structure.png)

### Viewing table data

You can open table editor and see the table data.  
You may need to switch to the "Data" tab.
DBeaver converts DynamoDB documents into a table format by default, but you can switch to another data representation.  
You can use data filters in order to find documents.  
![](images/database/dynamodb/data-view.png)

### Viewing data in JSON document format

You view, search and edit JSON documents. Double-click on a document to activate the editor.  
![](images/database/dynamodb/data-view-json.png)

### Executing queries

DBeaver supports simple SQL dialect for DynamoDB.  
You can use the WHERE clause in the same fashion as in regular SQL in order to find or filter documents.  

You can also use JSON requests syntax to query documents. See [Amazon DynamoDB query reference](https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html).

![](images/database/dynamodb/sql-query-simple.png)

### Exporting and importing data

You can export data from a DynamoDB table in different file formats (CSV, XLSX, XML, JSON, etc,) or export data directly to another table.  