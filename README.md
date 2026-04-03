# DAVx5 MCP Server

An experimental [Model Context Protocol (MCP)](https://modelcontextprotocol.org/) server for calendar management.

## ⚠️ Experimental Status

>[!WARNING]
>This project is **experimental** and under active development. The API and functionality may change without notice.
>There are **no automatic database migrations**.

>[!CAUTION]
>Not intended for production use. Only use in test environments.

## Overview

DAVx5 MCP Server provides calendar management capabilities through the Model Context Protocol. It acts as a bridge between MCP clients and CalDAV servers, enabling AI agents and other MCP-compatible tools to manage calendar events.

## Features

- **Events CRUD Operations**: Create, read, update, and delete calendar events
- **Time Tools**: Query current time and time-based information
- **CalDAV Integration**: Connects to standard CalDAV calendar servers

## Technical Stack

- **Language**: Kotlin
- **Framework**: Ktor (HTTP server)
- **MCP SDK**: Model Context Protocol Kotlin SDK
- **CalDAV**: DAV4jvm library for CalDAV integration
- **Serialization**: Kotlinx Serialization

## Quick Start

1. **Build the project**:
   ```bash
   ./gradlew build
   ```

2. **Configure CalDAV**:
   Set up your CalDAV server URL and credentials in the configuration.

3. **Run the server**:
   ```bash
   ./gradlew run --args="3000"
   ```
   (Replace `3000` with your desired port)

   Alternatively, you can build a fat JAR and run it with `java -jar <fat.jar>`.

   The server will start and listen for MCP connections on the specified port.

4. **Add the MCP connection to your AI model.**

## Configuration

The server requires configuration for your CalDAV server. Refer to the code for available configuration options.

## Development

This project uses Gradle for dependency management and building:

```bash
# Build a fat JAR with all dependencies
./gradlew fatJar

# Run tests
./gradlew test
```

## About MCP

The [Model Context Protocol](https://modelcontextprotocol.org/) is a protocol for AI agents to interact with external tools and services in a structured way.
