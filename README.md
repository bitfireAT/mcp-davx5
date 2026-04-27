# davmcp – "DAVx⁵ for AI"

An experimental [Model Context Protocol (MCP)](https://modelcontextprotocol.org/) server for CalDAV access.

It allows AI models to connect to CalDAV (in the future maybe also CardDAV and WebDAV) servers and run various queries
like "list events" or "create event". For you as a user of such a model, it means that you can **use natural
language to manage your calendars**, for instance: "Add a two-hour event tomorrow from 1 pm with title: 'Some
Appointment'",
and the AI model can directly add the event to the calendar over CalDAV.

The CalDAV server credentials stay in davmcp all the time and are not exposed to the AI model.

The [Model Context Protocol](https://modelcontextprotocol.org/) is a protocol for AI agents to interact with external
tools and services in a
structured way.

## ⚠️ Experimental Status

> [!WARNING]
> This project is **experimental** and under active development. The API and functionality may change without notice.
> There are **no automatic database migrations**.

> [!CAUTION]
> Not intended for production use. Only use in test environments.

There's no UI for configuration yet. You'll have to add database entries manually to make it work.

## What it does

davmcp provides calendar management capabilities through the Model Context Protocol. It acts as a bridge between MCP
clients and CalDAV servers, enabling AI agents and other MCP-compatible tools to manage calendar events.

- **CalDAV Integration**: Connects to standard CalDAV calendar servers
- TODO: CardDAV integration
- TODO: WebDAV integration
- **Events CRUD Operations**: Create, read, update, and delete calendar events
- TODO: CRUD for contacts, tasks, journal entries
- TODO: provide access to WebDAV resources
- **Auxiliary tools**: Query current time and time-based information (needed because LLMs don't have a concept of time)

## Quick Start

These are the steps to manually compile and run davmcp. You can also [use Docker](DOCKER.md) instead.

1. Prepare the required environment: Currently only a JDK is needed. `sqlite3` or a similar tool is required to edit
   the database since there's no configuration UI (yet). `npx` (NodeJS package manager) is needed if you want to run
   the MCP Inspector.
2. Check out or download davmcp.
3. Change the directory to the `server` subproject and keep using that directory for all remaining commands in this
   section and in [Configuration](#configuration):
    ```bash
    cd server
    ```
4. **Build the server**:
    ```bash
    ../gradlew :server:build
    ```
5. **Create a database directory and run the server**:
    ```bash
    mkdir -p data && ../gradlew :server:run --args="3000"
    ```
   (Replace `3000` with your desired port)

   Alternatively, you can build a fat JAR (see [Development](#development)) and run it from `server` with
   `java -jar build/libs/server-*-all.jar 3000`.

   The server will start and listen for MCP connections on the specified port.

   The MCP path is `/mcp`, so you can access it at `http://localhost:3000/mcp` (or any other of your IP addresses
   because the server listens on 0.0.0.0).

   If that works, hit Ctrl+C to shut the server down. It should have created a database file named `data/users.db`.

## Configuration

All commands in this section assume your current working directory is still `server`.

1. **Add user and access token**:

   The AI that acts on your behalf has to authenticate against davmcp. You have to create a token for that
   in the database:
   ```bash
   sqlite3 data/users.db "INSERT INTO user (name, email) VALUES ('Your Name', 'your-email@example.com');"
   # first user has now user id=1

   # use a random string for <your-access-token>   
   sqlite3 data/users.db "INSERT INTO accessToken (userId, token) VALUES (1, '<your-access-token>');"
   ```

   That token is later required when you connect the AI to davmcp.

2. **Add CalDAV service and calendars**:

   ```bash
   # Only use test accounts because davmcp is experimental.
   # The service credentials are stored in davmcp and won't be exposed to the AI model.
   sqlite3 data/users.db "INSERT INTO service (userId, username, password, baseUrl) VALUES (1, 'your-caldav-username', 'your-caldav-password', 'https://caldav.example.com');"
   # first service now has service id=1
   
   sqlite3 data/users.db "INSERT INTO collection (serviceId, url, displayName) VALUES (1, 'https://caldav.example.com/calendars/user/calendar1', 'My Calendar');"
   sqlite3 data/users.db "INSERT INTO collection (serviceId, url, displayName) VALUES (1, 'https://caldav.example.com/calendars/user/calendar2', 'My Second Calendar');"
   ```

3. Optional: **Verify MCP is working.**

   - Access `http://localhost:3000/mcp` with your browser. It should show 401 Unauthorized because no access token is
     sent.
   - You can verify that the MCP is working using
     the [MCP inspector](https://modelcontextprotocol.io/docs/tools/inspector): from another terminal, change to
     `server` as well, run `npx @modelcontextprotocol/inspector`, and then connect to `http://localhost:3000/mcp`,
     using a Custom Header for authentication (`Authorization: Bearer <your-access-token>`). Once you're connected,
     you can _List Tools_ and try out whatever tool you like.

4. **Add the MCP connection to your AI model.**

   How this step is done depends on the used environment. Look in the documentation of your AI environment for how to
   _add an MCP server_.

   - If you use a command-line interface (CLI) to the model, you can usually add an MCP server in the configuration
     file. Use `http://localhost:3000/mcp` as MCP URL in that case. Configure authentication with token so that the CLI
     sends `Authorization: Bearer <your-access-token>`.
   - When you run a model locally, you can usually also add MCP servers somehow.
   - If you use a cloud model (usually over a Web interface or an API), you have to run the MCP server on a public URL
     because the remote model needs access to it. You can run the davmcp server behind a reverse proxy like nginx (
     configure for SSE!) and then configure the public URL, usually something like `https://your-public-server.com/mcp`
     in the model configuration. Again, set the authentication method to _token_ and provide your access token.
     Alternatively, add `Authorization: Bearer <your-access-token>` to the headers.

5. **Enable the MCP/tools for specific requests.**

   You may have to enable the MCP or its tools when you send a request to the model. For instance, if you're using a
   remote model over a Web interface, you may need to click some "+" button and select the davmcp server to allow its
   usage in the request.

   Example request you can try: "Which events do I have in my calendar this week?"


## Development

This project uses Gradle for dependency management and building:

```bash
# Build a fat JAR with all dependencies
cd server
../gradlew :server:fatJar

# Run tests
../gradlew :server:test
```
