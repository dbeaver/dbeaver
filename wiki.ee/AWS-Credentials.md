<!-- META products: [LE, EE, UE, TE] -->
<!-- START doctoc -->
#### Table of contents
- [Overview](#overview)
- [Settings](#settings)
- [Credentials](#credentials)
  - [Default credentials](#default-credentials)
  - [Access keys](#access-keys)
  - [AWS Profiles](#aws-profiles)
  - [SSO](#sso)
- [AWS Secrets Manager](#aws-secrets-manager)
<!-- END doctoc -->

**Note**: This feature is available in [Lite](Lite-Edition), [Enterprise](Enterprise-Edition), [Ultimate](Ultimate-Edition) and <a href="https://dbeaver.com/dbeaver-team-edition">Team</a> editions only.
## Overview

DBeaver is integrated with AWS RDS IAM authentication, providing you with the ability to authenticate in AWS to access
your cloud databases. There are numerous ways to authorize and authenticate users in DBeaver AWS IAM, and DBeaver
supports all the basic ones.

The instructions provided here are intended for the client machine where DBeaver is installed. It is assumed that the
necessary configuration on the server side, including the setup of AWS RDS IAM, has already been completed.

![](images/auth_methods/AWS_RDS_IAM/aws-rds-authentication-configuration.png)
## Settings

To use IAM authentication in DBeaver, you need to select **AWS RDS IAM** as your authentication method in the connection
settings.

| Setting                      | Description                                                                                                                                                                                                                      |
|------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Credentials**              | Choose between [Access/secret keys](#access-keys), [AWS profile](#aws-profiles), or [Default credentials](#default-credentials) for the type of IAM credentials configuration.                                                   |
| **User**                     | Input your username.                                                                                                                                                                                                             |
| **Region**                   | Optional field to specify the AWS region. If not specified, the global region will be used.                                                                                                                                      |
| **Access key**               | AWS Access key.                                                                                                                                                                                                                  |
| **Secret key**               | AWS Secret key.                                                                                                                                                                                                                  |
| **Role name**                | Input the name of the role you want to use.                                                                                                                                                                                      |
| **Save credentials locally** | Check this box if you want to save your password locally.                                                                                                                                                                        |
| **3rd party account**        | Check this box if you want to access using a 3rd party role-based account.                                                                                                                                                       |
| **Use AWS Secrets Manager**  | Check this box if you want to connect to [AWS Secrets Manager](#aws-secrets-manager) to access the database credentials. When selected, a field **Secret Name** will appear where you can input the name of your **AWS Secret**. |

> **Tip**: DBeaver also supports AWS Systems Manager (SSM) for accessing databases. For detailed instructions on setting
> up AWS SSM, refer to the [AWS SSM setup guide](AWS-SSM-Configuration).

## Credentials

Choose the type of credentials by selecting the appropriate option from the **Credentials** dropdown menu.

### Default credentials

When you use Default Credentials, AWS will then try to determine credentials by using the standard credential providers
chain:

1. Java system properties
2. Environment variables
3. Web identity token from AWS STS
4. The shared credentials and config files
5. Amazon ECS container credentials
6. Amazon EC2 instance profile credentials
7. Amazon SSO credentials

Using default credentials is the easiest way to integrate with various Single Sign-On (SSO) and web identity providers,
as these providers typically supply credentials through configuration files.

For a more detailed explanation, please refer to the [AWS credentials documentation](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/credentials.html).

To use Default credentials, simply enter your username in the **User** field and select your AWS region.

### Access keys

Using the IAM user access key and secret key is the simplest way to authenticate. You just need to input these two keys.
You have the option to save them locally for convenience, or, for better security, you can choose to enter them each
time you connect to a database.

As with the Default configuration, you need to enter your username and select the AWS region. If you've checked the **Save
credentials locally** box, you'll need to fill in the **Access key** and **Secret key** fields. If you haven't checked this box,
you'll be prompted to fill in these fields each time you connect to the database.

For more detailed instructions on managing access keys for IAM users, you can refer to the [official AWS guide](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_credentials_access-keys.html).

### AWS Profiles

Just like with default credentials, you also have the option to select a specific credentials profile.

To do this, first select the profile you've already configured. If you need information on how to configure a profile,
you can find it below. After selecting a profile, fill in the **User** field and select your AWS region, just like you
would with the default credentials.

For more detailed instructions, you can refer to the official AWS guide
on [credentials config files](https://docs.aws.amazon.com/sdkref/latest/guide/creds-config-files.html).

### SSO

If you've set up an [SSO](AWS-SSO) portal on your AWS account, you can use it for web-based SSO authorization. This
SSO support can be activated for both Default and Profile-based AWS authorization types. To use this feature, you need
to enable the **Enable SSO** option.

## AWS Secrets Manager

If you've set up an **AWS Secret**, you can use it to access your database. This method can be used for both RDS databases
and Redshift. You can find instructions on how to create an **AWS Secret** in [official guide](https://docs.aws.amazon.com/secretsmanager/latest/userguide/). 
Remember, you'll need to fill in the **Password** field.

To use this feature, check the **Use AWS Secrets Manager** box and then fill in the **Secret Name** field.

**Note**: Make sure that the secret in the same region as the database.