    alter table TBASKET_ITEM 
        drop 
        foreign key FK47D06D1862C3498C;

    alter table TBASKET_ITEM 
        drop 
        foreign key FK47D06D18BF1587A8;

    alter table TCAR 
        drop 
        foreign key FK2732E0CEA9F9A4;

    alter table TCAT_FOLDER 
        drop 
        foreign key FKA2E6386B7AA7FB45;

    alter table TCAT_ITEM 
        drop 
        foreign key FKD98C76501EB22101;

    alter table TCAT_ITEM 
        drop 
        foreign key FKD98C76502F376E8;

    alter table TCUSTOMER_COMPANY 
        drop 
        foreign key FK42FFF9503B5043AC;

    alter table TCUSTOMER_TXN 
        drop 
        foreign key FK2DAE8BDCC2BD108;

    alter table TCUSTOMER_TXN 
        drop 
        foreign key FK2DAE8BD3B5043AC;

    alter table TCUSTOMER_TXN 
        drop 
        foreign key FK2DAE8BD4EC00D88;

    alter table TDELIVERY_ADDRESS 
        drop 
        foreign key FKA1697EDD62C3498C;

    alter table TDELIVERY_ADDRESS 
        drop 
        foreign key FKA1697EDD3B5043AC;

    alter table TDISCOUNT_ITEM 
        drop 
        foreign key FKD0570F7D2F376E8;

    alter table TDISCOUNT_ITEM 
        drop 
        foreign key FKD0570F7D7DD4EC;

    alter table TDISCOUNT_ITEM 
        drop 
        foreign key FKD0570F7D3B5043AC;

    alter table TDOWNLOAD_FILE 
        drop 
        foreign key FKDA272AFFCBAD6D4E;

    alter table TMESSAGE 
        drop 
        foreign key FK799945138E154E94;

    alter table TMESSAGE 
        drop 
        foreign key FK79994513E8DDB22B;

    alter table TMESSAGE 
        drop 
        foreign key FK7999451334D33202;

    alter table TMESSAGE 
        drop 
        foreign key FK799945133B5043AC;

    alter table TORDER 
        drop 
        foreign key FK93D6A35ADE9D71C5;

    alter table TORDER 
        drop 
        foreign key FK93D6A35A62C3498C;

    alter table TORDER 
        drop 
        foreign key FK93D6A35A789231E7;

    alter table TORDER 
        drop 
        foreign key FK93D6A35A77DDB155;

    alter table TORDER 
        drop 
        foreign key FK93D6A35A3B5043AC;

    alter table TORDER 
        drop 
        foreign key FK93D6A35ADDC92831;

    alter table TORDER_ITEM 
        drop 
        foreign key FK26C890D8DE9D71C5;

    alter table TORDER_ITEM 
        drop 
        foreign key FK26C890D8CC2BD108;

    alter table TORDER_ITEM 
        drop 
        foreign key FK26C890D8BF1587A8;

    alter table TORDER_ITEM 
        drop 
        foreign key FK26C890D889CBA1DE;

    alter table TORDER_ITEM 
        drop 
        foreign key FK26C890D84EC00D88;

    alter table TPRICE 
        drop 
        foreign key FK93E4CD557DD4EC;

    alter table TPRICE 
        drop 
        foreign key FK93E4CD55BBCB403F;

    alter table TPRICE 
        drop 
        foreign key FK93E4CD55FDFB268;

    alter table TPRODUCT 
        drop 
        foreign key FK2E3C11FB2F376E8;

    alter table TPRODUCT_ANALOG 
        drop 
        foreign key FK78CAA7B49915CD57;

    alter table TPRODUCT_REPLACE 
        drop 
        foreign key FK154BCC502F376E8;

    alter table TPRODUCT_REPLACE_2 
        drop 
        foreign key FKF18A0403CAC638E;

    alter table TPRODUCT_REPLACE_2 
        drop 
        foreign key FKF18A0403CAC638F;

    alter table TPRODUCT_REQUEST 
        drop 
        foreign key FK155E0C2B62C3498C;

    alter table TPRODUCT_REQUEST 
        drop 
        foreign key FK155E0C2B3216338E;

    alter table TPRODUCT_REQUEST 
        drop 
        foreign key FK155E0C2BFC464348;

    alter table TPRODUCT_REQUEST_ITEM 
        drop 
        foreign key FK5E4702A726225C9D;

    alter table TSPECIAL_OFFER 
        drop 
        foreign key FKEC41E3422F376E8;

    alter table TUPDATE_BATCH 
        drop 
        foreign key FKDF22167841B732D5;

    alter table TUPDATE_FILE 
        drop 
        foreign key FKE62C525ECBAD6D4E;

    alter table TUPDATE_FILE 
        drop 
        foreign key FKE62C525EF9C9CEA9;

    alter table TUPDATE_FILE 
        drop 
        foreign key FKE62C525E69DF1E4B;

    alter table TUSER 
        drop 
        foreign key FK4C79A1F3B5043AC;

    alter table TUSER 
        drop 
        foreign key FK4C79A1FBD2C048B;

    alter table TUSERSESSION 
        drop 
        foreign key FK7B7A37739B38DA3;

    alter table TUSER_SERVICE 
        drop 
        foreign key FK9A7604756B886898;

    alter table TUSER_TO_ROLE 
        drop 
        foreign key FKE1222D9A62C3498C;

    alter table TUSER_TO_ROLE 
        drop 
        foreign key FKE1222D9ABD9885AC;

    alter table TWAYBILL 
        drop 
        foreign key FK841124823B5043AC;

    drop table if exists TBASKET_ITEM;

    drop table if exists TBRAND;

    drop table if exists TCAR;

    drop table if exists TCAT_FOLDER;

    drop table if exists TCAT_ITEM;

    drop table if exists TCUSTOMER;

    drop table if exists TCUSTOMER_COMPANY;

    drop table if exists TCUSTOMER_TXN;

    drop table if exists TDELIVERY_ADDRESS;

    drop table if exists TDELIVERY_METHOD;

    drop table if exists TDISCOUNT_ITEM;

    drop table if exists TDOWNLOAD_FILE;

    drop table if exists TMESSAGE;

    drop table if exists TNEWS;

    drop table if exists TORDER;

    drop table if exists TORDER_ITEM;

    drop table if exists TORDER_PROCESS;

    drop table if exists TORDER_STATUS;

    drop table if exists TPAYMENT_METHOD;

    drop table if exists TPRICE;

    drop table if exists TPRODUCT;

    drop table if exists TPRODUCT_ANALOG;

    drop table if exists TPRODUCT_REPLACE;

    drop table if exists TPRODUCT_REPLACE_2;

    drop table if exists TPRODUCT_REQUEST;

    drop table if exists TPRODUCT_REQUEST_ITEM;

    drop table if exists TROLE;

    drop table if exists TSETTINGS;

    drop table if exists TSPECIAL_OFFER;

    drop table if exists TSUPPLIER;

    drop table if exists TUPDATE_BATCH;

    drop table if exists TUPDATE_FILE;

    drop table if exists TUSER;

    drop table if exists TUSERSESSION;

    drop table if exists TUSER_SERVICE;

    drop table if exists TUSER_TO_ROLE;

    drop table if exists TWAYBILL;

    create table TBASKET_ITEM (
        Item_Id bigint not null auto_increment,
        Item_Add_Date datetime not null,
        Item_Count integer not null,
        User_Comments varchar(250),
        Price_Id bigint not null,
        User_Id bigint not null,
        primary key (Item_Id)
    );

    create table TBRAND (
        Brand_Id smallint not null auto_increment,
        Brand_Code varchar(32) not null unique,
        Brand_Original bit not null,
        Brand_Removed bit not null,
        Brand_Synonyms varchar(250),
        Brand_Title varchar(250) not null,
        primary key (Brand_Id)
    );

    create table TCAR (
        Car_Id bigint not null auto_increment,
        Is_ABS_Available varchar(32),
        Body_Type varchar(100),
        Car_Brand varchar(64) not null,
        Comments text,
        Is_Conditioner_Available varchar(32),
        Doors_Number integer,
        Engine_Number varchar(64),
        Engine_Power varchar(100),
        Engine_Type varchar(100),
        Gear_Type varchar(100),
        Hood_Table varchar(100),
        Car_Model varchar(64) not null,
        Product_Country varchar(100),
        Product_Month smallint,
        Product_Year integer,
        Is_Steering_Intenser_Available varchar(32),
        Transmission_Type varchar(100),
        VIN varchar(32),
        Owner_Id bigint not null,
        primary key (Car_Id)
    );

    create table TCAT_FOLDER (
        Folder_Id integer not null auto_increment,
        Folder_Comment text,
        Folder_Link varchar(250),
        Is_Removed bit not null,
        Folder_Title varchar(250),
        Total_Children integer not null,
        Parent_Id integer,
        primary key (Folder_Id)
    );

    create table TCAT_ITEM (
        Item_Id integer not null auto_increment,
        Item_Link varchar(250),
        Item_Image LONGBLOB,
        Item_Image_Height integer,
        Item_Image_Size bigint,
        Item_Image_Type varchar(255),
        Item_Image_Width integer,
        Item_Code varchar(250),
        Is_Removed bit not null,
        Item_Text text,
        Item_Title varchar(250),
        Folder_Id integer not null,
        Brand_Id smallint,
        primary key (Item_Id)
    );

    create table TCUSTOMER (
        Customer_Id bigint not null auto_increment,
        Analog_Discount double precision not null,
        Balance double precision not null,
        Customer_Comment text,
        General_Discount double precision not null,
        Customer_Name varchar(250) not null unique,
        Customer_Personal bit not null,
        Registration_Date datetime not null,
        Customer_Suspended bit not null,
        primary key (Customer_Id)
    );

    create table TCUSTOMER_COMPANY (
        Company_Id bigint not null,
        Address1 varchar(200),
        Address2 varchar(200),
        BIK varchar(100),
        Director varchar(100),
        Company_Email varchar(100) unique,
        INN varchar(100),
        KPP varchar(100),
        KS varchar(100),
        Company_Name varchar(100) not null unique,
        OKONH varchar(100),
        OKPO varchar(100),
        Address_Physical varchar(200),
        RS varchar(100),
        Company_Web varchar(200) unique,
        Customer_Id bigint not null,
        primary key (Company_Id)
    );

    create table TCUSTOMER_TXN (
        Customer_Txn_Id bigint not null auto_increment,
        Comments text,
        Txn_Number varchar(100),
        Txn_Time datetime not null,
        Txn_Type varchar(255) not null,
        Txn_Value double precision not null,
        Waybill_Id bigint,
        Customer_Id bigint not null,
        Order_Id bigint,
        primary key (Customer_Txn_Id)
    );

    create table TDELIVERY_ADDRESS (
        Address_Id bigint not null auto_increment,
        Address_Address1 varchar(255),
        Address_Address2 varchar(255),
        Address_Address3 varchar(255),
        Address_City varchar(128),
        Address_Comment text,
        Address_Country varchar(128),
        Address_District varchar(128),
        Person_Name varchar(128),
        Address_Phone1 varchar(64),
        Address_Phone2 varchar(64),
        Address_Phone3 varchar(64),
        Address_Title varchar(64) not null,
        Address_Zip varchar(32),
        Customer_Id bigint not null,
        User_Id bigint,
        primary key (Address_Id)
    );

    create table TDELIVERY_METHOD (
        Delivery_Method_Code varchar(8) not null unique,
        Method_Description varchar(255) not null,
        Delivery_Method_Name varchar(64) not null unique,
        primary key (Delivery_Method_Code)
    );

    create table TDISCOUNT_ITEM (
        Discount_Id bigint not null auto_increment,
        Discount_Value double precision not null,
        Customer_Id bigint not null,
        Brand_Id smallint,
        Supplier_Id smallint not null,
        primary key (Discount_Id)
    );

    create table TDOWNLOAD_FILE (
        File_Id integer not null auto_increment,
        File_Comment varchar(255),
        Content_Type varchar(255),
        File_Name varchar(255) unique,
        File_Size bigint not null,
        Upload_Time datetime not null,
        Upload_User_Id bigint not null,
        primary key (File_Id)
    );

    create table TMESSAGE (
        Message_Id bigint not null auto_increment,
        Create_Date datetime not null,
        Is_Customer_Message bit not null,
        Read_Date datetime,
        Send_Date datetime,
        Message_Subject varchar(100) not null,
        Message_Text text not null,
        Customer_User_Id bigint,
        Customer_Id bigint not null,
        Service_User_Id bigint not null,
        Ref_Message_Id bigint,
        primary key (Message_Id)
    );

    create table TNEWS (
        News_Id integer not null auto_increment,
        News_Body text not null,
        Is_Removed bit not null,
        News_Time datetime not null,
        News_Title varchar(250),
        primary key (News_Id)
    );

    create table TORDER (
        Order_Id bigint not null auto_increment,
        Order_Canceled bit not null,
        Order_Comments text,
        Order_Completed bit not null,
        Order_Confirmed bit not null,
        Order_Number integer not null,
        Order_Time datetime not null,
        Order_Value double precision not null,
        User_Comments varchar(250),
        Payment_Method_Id varchar(8) not null,
        Order_Status_Id varchar(10),
        Customer_Id bigint not null,
        User_Id bigint not null,
        Delivery_Address_Code bigint,
        Delivery_Method_Code varchar(8) not null,
        primary key (Order_Id),
        unique (Customer_Id, Order_Number)
    );

    create table TORDER_ITEM (
        Item_Id bigint not null auto_increment,
        Item_Canceled bit not null,
        Item_Comments varchar(250),
        Item_Count integer not null,
        Price_Base_Value double precision not null,
        Price_Value double precision not null,
        Total_Base_Value double precision not null,
        Total_Value double precision not null,
        Update_Time datetime not null,
        Item_User_Comments varchar(250),
        Order_Status_Id varchar(10) not null,
        Process_Id bigint,
        WayBill_Id bigint,
        Order_Id bigint not null,
        Price_Id bigint not null,
        primary key (Item_Id)
    );

    create table TORDER_PROCESS (
        Process_Id bigint not null auto_increment,
        Process_Comments text,
        Process_Time datetime not null,
        primary key (Process_Id)
    );

    create table TORDER_STATUS (
        Status_Code varchar(10) not null,
        Status_Default bit not null,
        Status_Description varchar(255) not null,
        Status_Name varchar(64) not null unique,
        primary key (Status_Code)
    );

    create table TPAYMENT_METHOD (
        Payment_Method_Code varchar(8) not null unique,
        Method_Description varchar(255) not null,
        Payment_Method_Name varchar(64) not null unique,
        primary key (Payment_Method_Code)
    );

    create table TPRICE (
        Price_Id bigint not null auto_increment,
        Base_Value double precision not null,
        Reserved_Count varchar(10),
        Price_Title varchar(100) not null,
        Price_Value double precision not null,
        Batch_Id integer not null,
        Product_Id bigint not null,
        Supplier_Id smallint not null,
        primary key (Price_Id),
        unique (Product_Id, Supplier_Id)
    );

    create table TPRODUCT (
        Product_Id bigint not null auto_increment,
        Product_Code varchar(32) not null,
        Product_Title varchar(64) not null,
        Brand_Id smallint not null,
        primary key (Product_Id),
        unique (Product_Code, Brand_Id)
    );

    create table TPRODUCT_ANALOG (
        Original_Code varchar(32) not null,
        Analog_Code varchar(32) not null,
        Analog_Brand_Id smallint not null,
        primary key (Original_Code, Analog_Code, Analog_Brand_Id)
    );

    create table TPRODUCT_REPLACE (
        Replace_Code varchar(32) not null,
        Original_Code varchar(32) not null,
        Brand_Id smallint not null,
        primary key (Replace_Code, Brand_Id, Original_Code)
    );

    create table TPRODUCT_REPLACE_2 (
        Code_2 varchar(32) not null,
        Code_1 varchar(32) not null,
        Brand_2 smallint not null,
        Brand_1 smallint not null,
        primary key (Code_2, Code_1, Brand_2, Brand_1)
    );

    create table TPRODUCT_REQUEST (
        Request_Id bigint not null auto_increment,
        Request_Time datetime not null,
        Response_Time datetime,
        Car_Id bigint not null,
        User_Id bigint not null,
        Response_User_Id bigint,
        primary key (Request_Id)
    );

    create table TPRODUCT_REQUEST_ITEM (
        Request_Item_Id bigint not null auto_increment,
        Product_Code varchar(32),
        Product_Comment text,
        Product_Name varchar(200),
        Product_Price double precision,
        Request_Id bigint not null,
        primary key (Request_Item_Id)
    );

    create table TROLE (
        Role_Id bigint not null,
        Description text,
        Name varchar(64) not null,
        primary key (Role_Id)
    );

    create table TSETTINGS (
        Settings_Id bigint not null auto_increment,
        adminEmail varchar(255),
        notifyEmail varchar(255),
        robotEmail varchar(255),
        smtpHost varchar(255),
        smtpName varchar(255),
        smtpPassword varchar(255),
        smtpPort integer,
        primary key (Settings_Id)
    );

    create table TSPECIAL_OFFER (
        Offer_Id integer not null auto_increment,
        Offer_Image LONGBLOB,
        Offer_Image_Height integer,
        Offer_Image_Size bigint,
        Offer_Image_Type varchar(255),
        Offer_Image_Width integer,
        Offer_New_Price double precision,
        Offer_Date datetime,
        Offer_Old_Price double precision,
        Offer_Code varchar(250),
        Is_Removed bit not null,
        Offer_Text text,
        Offer_Title varchar(250),
        Brand_Id smallint,
        primary key (Offer_Id)
    );

    create table TSUPPLIER (
        Supplier_Id smallint not null auto_increment,
        Supplier_Code varchar(4) not null unique,
        Delivery_Days_Max integer,
        Delivery_Days_Max2 integer,
        Delivery_Days_Min integer,
        Delivery_Days_Min2 integer,
        Is_Disabled bit not null,
        Price_Minimum integer not null,
        Supplier_Title varchar(250) not null,
        primary key (Supplier_Id)
    );

    create table TUPDATE_BATCH (
        Batch_Id integer not null,
        Batch_Comment varchar(200),
        Batch_Deleted bit not null,
        Update_Time datetime not null,
        File_Id integer not null,
        primary key (Batch_Id)
    );

    create table TUPDATE_FILE (
        File_Id integer not null auto_increment,
        File_Comment varchar(200),
        Complete_Rows integer not null,
        Content_Type varchar(64),
        Is_Corrupted bit not null,
        Is_Downloadable bit not null,
        End_Time datetime,
        Is_Executed bit not null,
        File_Name varchar(200),
        File_Size bigint not null,
        File_Type varchar(32) not null,
        Imported_Rows integer not null,
        Start_Time datetime,
        Total_Rows integer not null,
        Upload_Time datetime not null,
        File_Brand_Id smallint,
        File_Supplier_Id smallint,
        Upload_User_Id bigint not null,
        primary key (File_Id)
    );

    create table TUSER (
        User_Id bigint not null auto_increment,
        User_Admin bit not null,
        Creation_Time datetime not null,
        User_Deleted bit not null,
        Email varchar(255) not null,
        First_Name varchar(127),
        Last_Login_Time datetime,
        Last_Name varchar(127),
        Middle_Name varchar(127),
        Username varchar(64) not null unique,
        Password_Hash varchar(64) not null,
        Phone varchar(255),
        Remote_Code varchar(120),
        Created_By bigint,
        Customer_Id bigint not null,
        primary key (User_Id)
    );

    create table TUSERSESSION (
        SessionId varchar(255) not null,
        LastLoginTime datetime not null,
        UserId bigint not null,
        primary key (SessionId)
    );

    create table TUSER_SERVICE (
        id bigint not null,
        Department varchar(64),
        Service_Customer_Type varchar(20) not null,
        primary key (id)
    );

    create table TUSER_TO_ROLE (
        User_Id bigint not null,
        Role_Id bigint not null,
        primary key (User_Id, Role_Id)
    );

    create table TWAYBILL (
        WayBill_Id bigint not null auto_increment,
        Bill_Date datetime,
        Is_Canceled bit not null,
        Waybill_Comments varchar(250),
        Is_Completed bit not null,
        Waybill_Value double precision not null,
        Customer_Id bigint not null,
        primary key (WayBill_Id)
    );

    alter table TBASKET_ITEM 
        add index FK47D06D1862C3498C (User_Id), 
        add constraint FK47D06D1862C3498C 
        foreign key (User_Id) 
        references TUSER (User_Id);

    alter table TBASKET_ITEM 
        add index FK47D06D18BF1587A8 (Price_Id), 
        add constraint FK47D06D18BF1587A8 
        foreign key (Price_Id) 
        references TPRICE (Price_Id);

    alter table TCAR 
        add index FK2732E0CEA9F9A4 (Owner_Id), 
        add constraint FK2732E0CEA9F9A4 
        foreign key (Owner_Id) 
        references TUSER (User_Id);

    alter table TCAT_FOLDER 
        add index FKA2E6386B7AA7FB45 (Parent_Id), 
        add constraint FKA2E6386B7AA7FB45 
        foreign key (Parent_Id) 
        references TCAT_FOLDER (Folder_Id);

    alter table TCAT_ITEM 
        add index FKD98C76501EB22101 (Folder_Id), 
        add constraint FKD98C76501EB22101 
        foreign key (Folder_Id) 
        references TCAT_FOLDER (Folder_Id);

    alter table TCAT_ITEM 
        add index FKD98C76502F376E8 (Brand_Id), 
        add constraint FKD98C76502F376E8 
        foreign key (Brand_Id) 
        references TBRAND (Brand_Id);

    alter table TCUSTOMER_COMPANY 
        add index FK42FFF9503B5043AC (Customer_Id), 
        add constraint FK42FFF9503B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);

    alter table TCUSTOMER_TXN 
        add index FK2DAE8BDCC2BD108 (Waybill_Id), 
        add constraint FK2DAE8BDCC2BD108 
        foreign key (Waybill_Id) 
        references TWAYBILL (WayBill_Id);

    alter table TCUSTOMER_TXN 
        add index FK2DAE8BD3B5043AC (Customer_Id), 
        add constraint FK2DAE8BD3B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);

    alter table TCUSTOMER_TXN 
        add index FK2DAE8BD4EC00D88 (Order_Id), 
        add constraint FK2DAE8BD4EC00D88 
        foreign key (Order_Id) 
        references TORDER (Order_Id);

    alter table TDELIVERY_ADDRESS 
        add index FKA1697EDD62C3498C (User_Id), 
        add constraint FKA1697EDD62C3498C 
        foreign key (User_Id) 
        references TUSER (User_Id);

    alter table TDELIVERY_ADDRESS 
        add index FKA1697EDD3B5043AC (Customer_Id), 
        add constraint FKA1697EDD3B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);

    alter table TDISCOUNT_ITEM 
        add index FKD0570F7D2F376E8 (Brand_Id), 
        add constraint FKD0570F7D2F376E8 
        foreign key (Brand_Id) 
        references TBRAND (Brand_Id);

    alter table TDISCOUNT_ITEM 
        add index FKD0570F7D7DD4EC (Supplier_Id), 
        add constraint FKD0570F7D7DD4EC 
        foreign key (Supplier_Id) 
        references TSUPPLIER (Supplier_Id);

    alter table TDISCOUNT_ITEM 
        add index FKD0570F7D3B5043AC (Customer_Id), 
        add constraint FKD0570F7D3B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);

    alter table TDOWNLOAD_FILE 
        add index FKDA272AFFCBAD6D4E (Upload_User_Id), 
        add constraint FKDA272AFFCBAD6D4E 
        foreign key (Upload_User_Id) 
        references TUSER (User_Id);

    alter table TMESSAGE 
        add index FK799945138E154E94 (Ref_Message_Id), 
        add constraint FK799945138E154E94 
        foreign key (Ref_Message_Id) 
        references TMESSAGE (Message_Id);

    alter table TMESSAGE 
        add index FK79994513E8DDB22B (Customer_User_Id), 
        add constraint FK79994513E8DDB22B 
        foreign key (Customer_User_Id) 
        references TUSER (User_Id);

    alter table TMESSAGE 
        add index FK7999451334D33202 (Service_User_Id), 
        add constraint FK7999451334D33202 
        foreign key (Service_User_Id) 
        references TUSER (User_Id);

    alter table TMESSAGE 
        add index FK799945133B5043AC (Customer_Id), 
        add constraint FK799945133B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);

    alter table TORDER 
        add index FK93D6A35ADE9D71C5 (Order_Status_Id), 
        add constraint FK93D6A35ADE9D71C5 
        foreign key (Order_Status_Id) 
        references TORDER_STATUS (Status_Code);

    alter table TORDER 
        add index FK93D6A35A62C3498C (User_Id), 
        add constraint FK93D6A35A62C3498C 
        foreign key (User_Id) 
        references TUSER (User_Id);

    alter table TORDER 
        add index FK93D6A35A789231E7 (Delivery_Method_Code), 
        add constraint FK93D6A35A789231E7 
        foreign key (Delivery_Method_Code) 
        references TDELIVERY_METHOD (Delivery_Method_Code);

    alter table TORDER 
        add index FK93D6A35A77DDB155 (Payment_Method_Id), 
        add constraint FK93D6A35A77DDB155 
        foreign key (Payment_Method_Id) 
        references TPAYMENT_METHOD (Payment_Method_Code);

    alter table TORDER 
        add index FK93D6A35A3B5043AC (Customer_Id), 
        add constraint FK93D6A35A3B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);

    alter table TORDER 
        add index FK93D6A35ADDC92831 (Delivery_Address_Code), 
        add constraint FK93D6A35ADDC92831 
        foreign key (Delivery_Address_Code) 
        references TDELIVERY_ADDRESS (Address_Id);

    alter table TORDER_ITEM 
        add index FK26C890D8DE9D71C5 (Order_Status_Id), 
        add constraint FK26C890D8DE9D71C5 
        foreign key (Order_Status_Id) 
        references TORDER_STATUS (Status_Code);

    alter table TORDER_ITEM 
        add index FK26C890D8CC2BD108 (WayBill_Id), 
        add constraint FK26C890D8CC2BD108 
        foreign key (WayBill_Id) 
        references TWAYBILL (WayBill_Id);

    alter table TORDER_ITEM 
        add index FK26C890D8BF1587A8 (Price_Id), 
        add constraint FK26C890D8BF1587A8 
        foreign key (Price_Id) 
        references TPRICE (Price_Id);

    alter table TORDER_ITEM 
        add index FK26C890D889CBA1DE (Process_Id), 
        add constraint FK26C890D889CBA1DE 
        foreign key (Process_Id) 
        references TORDER_PROCESS (Process_Id);

    alter table TORDER_ITEM 
        add index FK26C890D84EC00D88 (Order_Id), 
        add constraint FK26C890D84EC00D88 
        foreign key (Order_Id) 
        references TORDER (Order_Id);

    alter table TPRICE 
        add index FK93E4CD557DD4EC (Supplier_Id), 
        add constraint FK93E4CD557DD4EC 
        foreign key (Supplier_Id) 
        references TSUPPLIER (Supplier_Id);

    alter table TPRICE 
        add index FK93E4CD55BBCB403F (Batch_Id), 
        add constraint FK93E4CD55BBCB403F 
        foreign key (Batch_Id) 
        references TUPDATE_BATCH (Batch_Id);

    alter table TPRICE 
        add index FK93E4CD55FDFB268 (Product_Id), 
        add constraint FK93E4CD55FDFB268 
        foreign key (Product_Id) 
        references TPRODUCT (Product_Id);

    alter table TPRODUCT 
        add index FK2E3C11FB2F376E8 (Brand_Id), 
        add constraint FK2E3C11FB2F376E8 
        foreign key (Brand_Id) 
        references TBRAND (Brand_Id);

    alter table TPRODUCT_ANALOG 
        add index FK78CAA7B49915CD57 (Analog_Brand_Id), 
        add constraint FK78CAA7B49915CD57 
        foreign key (Analog_Brand_Id) 
        references TBRAND (Brand_Id);

    alter table TPRODUCT_REPLACE 
        add index FK154BCC502F376E8 (Brand_Id), 
        add constraint FK154BCC502F376E8 
        foreign key (Brand_Id) 
        references TBRAND (Brand_Id);

    create index Code2 on TPRODUCT_REPLACE_2 (Code_2);

    alter table TPRODUCT_REPLACE_2 
        add index FKF18A0403CAC638E (Brand_1), 
        add constraint FKF18A0403CAC638E 
        foreign key (Brand_1) 
        references TBRAND (Brand_Id);

    alter table TPRODUCT_REPLACE_2 
        add index FKF18A0403CAC638F (Brand_2), 
        add constraint FKF18A0403CAC638F 
        foreign key (Brand_2) 
        references TBRAND (Brand_Id);

    alter table TPRODUCT_REQUEST 
        add index FK155E0C2B62C3498C (User_Id), 
        add constraint FK155E0C2B62C3498C 
        foreign key (User_Id) 
        references TUSER (User_Id);

    alter table TPRODUCT_REQUEST 
        add index FK155E0C2B3216338E (Response_User_Id), 
        add constraint FK155E0C2B3216338E 
        foreign key (Response_User_Id) 
        references TUSER (User_Id);

    alter table TPRODUCT_REQUEST 
        add index FK155E0C2BFC464348 (Car_Id), 
        add constraint FK155E0C2BFC464348 
        foreign key (Car_Id) 
        references TCAR (Car_Id);

    alter table TPRODUCT_REQUEST_ITEM 
        add index FK5E4702A726225C9D (Request_Id), 
        add constraint FK5E4702A726225C9D 
        foreign key (Request_Id) 
        references TPRODUCT_REQUEST (Request_Id);

    alter table TSPECIAL_OFFER 
        add index FKEC41E3422F376E8 (Brand_Id), 
        add constraint FKEC41E3422F376E8 
        foreign key (Brand_Id) 
        references TBRAND (Brand_Id);

    alter table TUPDATE_BATCH 
        add index FKDF22167841B732D5 (File_Id), 
        add constraint FKDF22167841B732D5 
        foreign key (File_Id) 
        references TUPDATE_FILE (File_Id);

    alter table TUPDATE_FILE 
        add index FKE62C525ECBAD6D4E (Upload_User_Id), 
        add constraint FKE62C525ECBAD6D4E 
        foreign key (Upload_User_Id) 
        references TUSER (User_Id);

    alter table TUPDATE_FILE 
        add index FKE62C525EF9C9CEA9 (File_Supplier_Id), 
        add constraint FKE62C525EF9C9CEA9 
        foreign key (File_Supplier_Id) 
        references TSUPPLIER (Supplier_Id);

    alter table TUPDATE_FILE 
        add index FKE62C525E69DF1E4B (File_Brand_Id), 
        add constraint FKE62C525E69DF1E4B 
        foreign key (File_Brand_Id) 
        references TBRAND (Brand_Id);

    alter table TUSER 
        add index FK4C79A1F3B5043AC (Customer_Id), 
        add constraint FK4C79A1F3B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);

    alter table TUSER 
        add index FK4C79A1FBD2C048B (Created_By), 
        add constraint FK4C79A1FBD2C048B 
        foreign key (Created_By) 
        references TUSER (User_Id);

    alter table TUSERSESSION 
        add index FK7B7A37739B38DA3 (UserId), 
        add constraint FK7B7A37739B38DA3 
        foreign key (UserId) 
        references TUSER (User_Id);

    alter table TUSER_SERVICE 
        add index FK9A7604756B886898 (id), 
        add constraint FK9A7604756B886898 
        foreign key (id) 
        references TUSER (User_Id);

    alter table TUSER_TO_ROLE 
        add index FKE1222D9A62C3498C (User_Id), 
        add constraint FKE1222D9A62C3498C 
        foreign key (User_Id) 
        references TUSER (User_Id);

    alter table TUSER_TO_ROLE 
        add index FKE1222D9ABD9885AC (Role_Id), 
        add constraint FKE1222D9ABD9885AC 
        foreign key (Role_Id) 
        references TROLE (Role_Id);

    alter table TWAYBILL 
        add index FK841124823B5043AC (Customer_Id), 
        add constraint FK841124823B5043AC 
        foreign key (Customer_Id) 
        references TCUSTOMER (Customer_Id);