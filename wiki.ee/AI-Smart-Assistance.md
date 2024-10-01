<!-- META products: [CE, LE, EE, UE, TE] -->

**Note**: This feature is available in [Lite](Lite-Edition), [Enterprise](Enterprise-Edition), [Ultimate](Ultimate-Edition) and <a href="https://dbeaver.com/dbeaver-team-edition">Team</a> editions only. For users of the Community edition to access **AI smart completion** feature,
first [install the required extension](#installing-the-gpt-extension).

<!-- START doctoc -->
#### Table of contents
- [Understanding the AI integration in DBeaver](#understanding-the-ai-integration-in-dbeaver)
  - [Initial setup](#initial-setup)
  - [Data privacy](#data-privacy)
- [AI settings and customization](#ai-settings-and-customization)
  - [Basic configuration](#basic-configuration)
    - [Credentials for OpenAI](#credentials-for-openai)
    - [Credentials for Azure AI](#credentials-for-azure-ai)
    - [Credentials for Google Gemini](#credentials-for-google-gemini)
    - [Credentials for Ollama](#credentials-for-ollama)
  - [Preferences](#preferences)
  - [PRO version exclusive settings](#pro-version-exclusive-settings)
- [Utilizing AI capabilities in DBeaver](#utilizing-ai-capabilities-in-dbeaver)
  - [AI Chat usage](#ai-chat-usage)
    - [Resetting query context](#resetting-query-context)
    - [Defining the scope in AI Chat](#defining-the-scope-in-ai-chat)
  - [AI smart completion usage](#ai-smart-completion-usage)
    - [Defining the scope](#defining-the-scope)
    - [Accessing query history](#accessing-query-history)
  - [Disabling AI features](#disabling-ai-features)
- [Best practices for question formulation](#best-practices-for-question-formulation)
- [Installing the GPT extension](#installing-the-gpt-extension)
<!-- END doctoc -->

DBeaver offers the ability to construct SQL queries using natural language through our **AI smart completion** and **AI
Chat** features. This capability is achieved through integrations with
OpenAI's [GPT language model](https://platform.openai.com/docs/models/), [Azure OpenAI](https://azure.microsoft.com/en-us/products/ai-services/openai-service), [Google Gemini](https://gemini.google.com/)
and [Ollama](https://ollama.com/).

> **Note**: DBeaver is not affiliated with OpenAI, Microsoft Azure, or Google. Integration with AI features is achieved through the public APIs of OpenAI's GPT, Azure OpenAI Service, and Google Gemini.
> - To utilize these features, register with the respective service provider and [obtain a secret key](#basic-configuration).
> - Users of the DBeaver Community version need to [install the GPT extension](#installing-the-gpt-extension) to enable this functionality.

### Understanding the AI integration in DBeaver

With the **AI smart completion** feature, you can type queries in natural language and DBeaver will convert them into SQL
statements. This tool simplifies writing complex queries by interpreting your input and automatically generating the
correct SQL code.

![](images/ai/ai-smart-completion-demo.png)

The **AI Chat** feature provides a conversational interface, allowing you to communicate with the database in plain language.
Type your queries conversationally, and **AI Chat** translates them into executable SQL, streamlining your database
interactions.

![](images/ai/ai-chat-demo.png)

#### Initial setup

To activate the AI features in DBeaver, configure the API token:

1. Navigate to **Window** -> **Preferences** -> **General** -> **AI**.

2. Ensure the **Enable smart completion** option is activated. This option is typically enabled by default
in the PRO versions.

3. In the **API token** field, input your AI secret key.

4. Save the changes. See the [Basic configuration](#basic-configuration) for secret key details.
   ![](images/ai/ai_smart_assistance_5.png)

For instructions on utilizing the AI features, visit the [AI Smart completion usage](#ai-smart-completion-usage) and [AI Chat usage](#ai-chat-usage) sections.

#### Data privacy

We prioritize data safety and user privacy. In this section, we outline how data is managed and the measures taken to
protect user privacy when using the AI features.

To enable the AI features capabilities, table and column names from the current database schema are transmitted
to OpenAI. This step is crucial for accurately translating user requests into SQL queries. Key considerations regarding
data privacy are as follows:

- **No Table Data**: Only metadata like table and column names are shared with OpenAI. Actual table data is not
  transmitted.
- **User Consent**: On the first use of AI completion for a specific connection, DBeaver will prompt for your
  confirmation to send metadata. This confirmation is mandatory to use the feature.

  ![](images/ai/ai_smart_assistance_9.png)

- **Log Transparency**: The entire request can be logged for your review. To enable this, navigate to **Preferences**
  and check the **Write GPT queries to debug log** option.
- **Selective Metadata Sharing**: If you prefer not to share information about certain tables, adjust the tables in
  scope using the **Scope** field.
- **Disabling AI**: PRO version users can [turn off the AI feature](#disabling-ai-features), while Community Edition
  users can avoid it by not installing the AI plugin. <img src="images/commercial.png" style="width:12px; height:12px; vertical-align:top; margin-top:4px;" title="This feature is available only in PRO products."/>
- **Azure OpenAI privacy**: If you use Azure OpenAI, be aware that it operates under its own [privacy policy](https://learn.microsoft.com/en-us/legal/cognitive-services/openai/data-privacy). It's recommended to review their terms before using. <img src="images/commercial.png" style="width:12px; height:12px; vertical-align:top; margin-top:4px;" title="This feature is available only in PRO products."/>
- **Google Gemini privacy**: When utilizing Google Gemini, it is important to understand the specific data [privacy measures](https://www.gemini.com/legal/privacy-policy). <img src="images/commercial.png" style="width:12px; height:12px; vertical-align:top; margin-top:4px;" title="This feature is available only in PRO products."/>

### AI settings and customization

To utilize the AI-enhanced functionalities within DBeaver, certain configurations and setup processes are required. This
section offers a comprehensive guide on initial setup, advanced configurations for PRO users, and customization options
to tailor the AI integration according to specific preferences.

#### Basic configuration

By default, AI features are ready to use. To start, you need to specify the service credentials based on the AI
service you choose: OpenAI, Azure AI, Ollama or Google Gemini.

##### Credentials for OpenAI

1. Sign up on the [OpenAI platform](https://openai.com/api/).

2. Navigate to the [API Keys section](https://platform.openai.com/account/api-keys) and generate a new secret key.

3. Insert this key into DBeaver's **API token** setting.

Here is a list of the currently supported models:

- **gpt-3.5-turbo** (recommended for SQL).
- **gpt-3.5-turbo-instruct**.
- **gpt-4**.
- **gpt-4-turbo**.
- **gpt-4o**.
- **gpt-4o-mini**.

> **Note**: OpenAI services are available in specific countries. Consult
> the [supported countries](https://platform.openai.com/docs/supported-countries) list to verify availability in your
> location.

<!-- PRO versions: [LE, EE, UE, TE] start -->
##### Credentials for Azure AI

1. Sign up on the [Azure platform](https://azure.microsoft.com/en-us/free/).

2. Navigate to the [Azure Portal](https://portal.azure.com/) and create a new AI service under the AI + Machine Learning
   section.

3. Generate and copy the credentials for the newly created service.

4. Insert these credentials into DBeaver's **Engine Settings**.

##### Credentials for Google Gemini

1. Sign up on the [Google Cloud Platform](https://cloud.google.com/).

2. Navigate to the [Google Cloud Console](https://console.cloud.google.com/) and create a new project.

3. Enable the Gemini API for your project by searching for the Gemini API in the marketplace and clicking **Enable**.

4. Create credentials for your project by navigating to the **Credentials** page under **APIs & Services**. Choose 
   **Create credentials** and select the appropriate type for your Gemini integration.

5. Copy the generated credentials.

6. Insert these credentials into DBeaver's **Engine Settings**.

> **Note**: Google Gemini services are subject to regional availability. Check the list
> of [available regions](https://ai.google.dev/available_regions) to ensure access in your area.

##### Credentials for Ollama

Ensure that Ollama is already installed and running on a server. You will need the host address where Ollama is
installed to proceed.

1. Specify the host address of your Ollama server in the **Instance host** field, ensuring it follows the
   format `http://host:port`.
2. Click **Load Models**. If the host address is correct, DBeaver will display the available models from your Ollama
   server in the **Model** dropdown menu.
3. Select the model you need for your integration.

<!-- PRO versions: [LE, EE, UE, TE] end -->
#### Preferences

For specific requirements or troubleshooting, you might want to adjust some of the following settings:

- Navigate to **Window** -> **Preferences** -> **General** -> **AI** to access these settings.

  ![](images/ai/ai_smart_assistance_5.png)

| Setting                                   | Description                                                                                                                                        |
|-------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| **Enable smart completion**               | Displays the AI features in the SQL Editor.                                                                                                        |
| **Include source in query comment**       | Shows your original request above the AI-generated query in the SQL Editor.                                                                        |
| **Execute SQL immediately**               | Runs the translated SQL query immediately after generation.                                                                                        |
| **Send attribute type information**       | Send attribute type information to the AI vendor. It makes better completion, but consumes more tokens.                                            |
| **Send object description**               | Send object description to the AI vendor. Improves completion, but may consume significant amount of tokens.                                       |
| **API token**                             | Input your secret key from the OpenAI platform.                                                                                                    |
| **Model**                                 | Choose the AI model.                                                                                                                               |
| **Temperature**                           | Control AI's creativity from `0.0` (more precise) to `0.9` (more diverse).</br> Note that higher temperature can lead to less predictable results. |
| **Write GPT/Ollama queries to debug log** | Logs your AI requests.                                                                                                                             |

#### PRO version exclusive settings
**Note**: This feature is available in [Lite](Lite-Edition), [Enterprise](Enterprise-Edition), [Ultimate](Ultimate-Edition) and <a href="https://dbeaver.com/dbeaver-team-edition">Team</a> editions only.

For users of the [Lite](Lite-Edition), [Enterprise](Enterprise-Edition), [Ultimate](Ultimate-Edition) and <a href="https://dbeaver.com/dbeaver-team-edition">Team</a> editions, additional
configurations are available:

![](images/ai/ai_smart_assistance_7.png)

| Setting                                      | Description                                            |
|----------------------------------------------|--------------------------------------------------------|
| **Send foreign keys information**            | Helps AI understand table relationships.               |
| **Send unique keys and indexes information** | Assists AI in crafting complex queries.                |
| **Format SQL query**                         | Adds formatting to the generated SQL.                  |
| **Table join rule**                          | Choose between explicit JOIN or JOIN with sub-queries. |

There is also an option to switch the **Service** between **OpenAI**, **Azure OpenAI**, **Google Gemini** and **Ollama**. 

Azure provides a set of distinct settings:

![](images/ai/ai_smart_assistance_11.png)

| Setting          | Description                                                                                                                                                                                                |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Endpoint**     | Configure a custom endpoint URL for Azure OpenAPI interactions.                                                                                                                                            |
| **API version**  | Select the version of the API you wish to use.                                                                                                                                                             |
| **Deployment**   | Specify the deployment name chosen during model deployment.                                                                                                                                                |
| **Context size** | Choose the context size between `2048` and `32768`. A larger number allows the AI to use more data for better answers but may slow down response time. Choose based on your balance of accuracy and speed. |

### Utilizing AI capabilities in DBeaver

#### AI Chat usage
**Note**: This feature is available in [Lite](Lite-Edition), [Enterprise](Enterprise-Edition), [Ultimate](Ultimate-Edition) and <a href="https://dbeaver.com/dbeaver-team-edition">Team</a> editions only.

To utilize the **AI Chat** feature in DBeaver:

![](images/ai/ai-chat-window.png)

1. Launch the **SQL Editor**.

2. Click on the **AI Chat** tab ![](images/ai/ai-chat-button.png), located on the right toolbar of the **SQL Editor**.

3. In the chat window that opens, input your request in natural language.

4. Press the **Send** button to get the SQL translation.

5. To execute the SQL query generated by AI, click the **Execute SQL query** button.

> **Note**: The AI Chat logs your query history, enabling you to reference and expand on prior inputs. Each new or edited
> entry prompts the AI to generate a revised SQL query.

##### Resetting query context

To start a new conversation or to change the query context in the **AI Chat**, you can use one of the two reset options
available:

* To clear the entire history, use the reset button located next to the **Save** button.
* To clear a portion of the conversation history up to a chosen point, use the reset option within the **AI Chat**
  itself, located at the end of each user's input prompt.

![](images/ai/resetting-query-context.png)

##### Defining the scope in AI Chat

For enhanced precision, especially in databases with extensive schemas, you can specify database objects in the
**Scope** field to narrow down the context.

1. In the **AI Chat** interface, click on the arrow near the **Change scope**
   button ![](images/ai/change-scope-button.png) to open the context menu.

2. Specify the area of your database that the AI should concentrate on.

3. You can choose from:
   - **Current Schema**: Focuses the AI on the schema you are currently using.
   - **Current Database**: Limits the AI to the database currently selected.
   - **Connection**: Sets the AI to consider all schemas within the current database connection.
   - **Custom**: Allows you to define a more specific scope, such as a particular table or schema.

#### AI smart completion usage

To interact with databases using the **AI Smart completion** feature:

![](images/ai/ai_smart_assistance_2.png)

1. Launch the **SQL Editor**.

2.  Click on the **GPT Chat smart completion** icon ![](images/ai/ai_smart_assistance_10.png) located in the left toolbar of the **SQL Editor**. 
  > **Note** The toolbar is customizable. For further information, refer
  to [Toolbar Customization](Toolbar-Customization) article.
  
3.  Input your natural language request in the **AI smart completion** window.

4.  Click **Translate** to obtain the SQL query.

For enhanced precision, especially in databases with extensive schemas, you can specify database objects in the
**Scope** field to narrow down the context.

##### Defining the scope

To enhance the precision of AI-assisted SQL queries, especially in extensive database schemas, setting the scope is key.
This action narrows down the AI's focus, ensuring it generates more relevant queries.

1. In the **AI Smart completion** interface, navigate to the **Scope** field.

   ![](images/ai/ai_smart_assistance_3.png)

2. You can choose from:
   - **Current Schema**: Focuses the AI on the schema you are currently using.
   - **Current Database**: Limits the AI to the database currently selected.
   - **Connection**: Sets the AI to consider all schemas within the current database connection.
   - **Custom**: Allows you to define a more specific scope, such as a particular table or schema.

##### Accessing query history

Query history allows you to review previous requests. In the PRO version, this includes history from past sessions,
offering a track record of your SQL queries and AI interactions.

#### Disabling AI features

To hide the **GPT smart completion** and **AI Chat** icons in the SQL Editor:

- Navigate to **Window** -> **Preferences** -> **General** -> **AI**.
- Deselect **Enable smart completion**.

For users with PRO versions wanting to permanently disable this feature:

- Use the system variable by setting `DBEAVER_AI_DISABLED` to `true`.
- Or, add `Dai.disabled=true` to the `dbeaver.ini` file.

Detailed instructions on finding `dbeaver.ini` are available
in [our article](Configuration-files-in-DBeaver#how-to-locate-the-dbeaver-ini).

> **Note**: Once this feature is permanently disabled in PRO versions, it cannot be re-enabled via Preferences.

### Best practices for question formulation

When using AI to generate SQL queries, it's essential to provide clear and specific input. Here's how to optimize
your questions:

- **Language**: While AI supports multiple languages, it's recommended to use English for best results.
- **Database knowledge**: Familiarity with your database structure enhances the accuracy of generated queries.
- **Iterative refinement**: Start with a general request and refine iteratively. As you provide more specific details,
  the AI's output will become more relevant.
- **Explicit details**: If you know certain tables or columns that should be part of the query, include them in your
  request for better accuracy.

For instance, if you're using the [DBeaver sample SQLite database](Sample-Database), you might phrase your request
as:

Example 1: "List all customers from Italy"

Resulting SQL:

```SQL
SELECT *
FROM customers
WHERE country = 'Italy';
```

Example 2: "montre les clients de France"

```SQL
SELECT *
FROM customer
WHERE country = 'France';
```

Example 3: "show customers who purchased blues tracks, use joins"

```SQL
SELECT c.FirstName, c.LastName, t.Name, g.Name
FROM Customer c
         JOIN Invoice i ON c.CustomerId = i.CustomerId
         JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
         JOIN Track t ON il.TrackId = t.TrackId
         JOIN Genre g ON t.GenreId = g.GenreId
WHERE g.Name = 'Blues'
ORDER BY c.LastName, c.FirstName;
```

Example 4: "get names of customers who purchased blues tracks, use joins"

```SQL
SELECT DISTINCT c.FirstName, c.LastName
FROM Customer c
         JOIN Invoice i ON c.CustomerId = i.CustomerId
         JOIN InvoiceLine il ON i.InvoiceId = il.InvoiceId
         JOIN Track t ON il.TrackId = t.TrackId
         JOIN Genre g ON t.GenreId = g.GenreId
WHERE g.Name = 'Blues';
```

### Installing the GPT extension

For users of the DBeaver Community version, the GPT extension requires manual installation.

Follow these steps to install the GPT extension in the Community version:

1. Navigate to **Help** from the main menu and select **Install New Software**.

2. Within the installation dialog, locate the **Work with** dropdown. Choose **DBeaver AI integration** from the
   list.

   ![](images/ai/ai_smart_assistance_8.png)

3. Check the box next to **AI Support**. Click **Next** and proceed through the installation prompts.

4. A **Trust Artifacts** window will appear. Ensure you select all the artifacts listed and click on **Trust Selected**.

5. Once the installation is complete, restart DBeaver.

After restarting, launch the SQL editor. On the left toolbar, you should now see the **GPT Chat**
icon ![](images/ai/ai_smart_assistance_10.png).