# Docker

1. Build the docker image:
    ```shell
    docker build -t davmcp -f docker/Dockerfile .
    ```
2. Start the container:
    ```shell
    docker run -p 3000 -v davmcp-data:/app/data davmcp
    ```

# Docker Compose

1. Build the docker image:
    ```shell
    docker compose -f docker/compose.yml build
    ```
2. Start the container:
    ```shell
    docker compose -f docker/compose.yml up
    ```
