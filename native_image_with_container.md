# Quarkus Linux app from another OS using a container

Quarkus qualifies itself as **SUPERSONIC/SUBATOMIC/JAVA** partly thanks to its support for GraalVM. This post shows how to build a GraalVM **native image** for Linux from using a container, allowing to perform this task from Windows or macOS.

## Motivation

GraalVM is a high performance JDK distribution which brings many interesting features such as supporting non JVM languages (such as Python). Among the features of this JDK, the one that we're interested in here is called **native image**, which compiles Java code into a native binary. This means that the resulting binary will contain machine code instead of the traditional bytecode. Theoretically, we would gain in performance and startup speed.

Quarkus allows to generate a **native image** for the current platform quite easily. However, since most servers run on Linux, we would like to find the simplest way to generate a native Linux binary. Unfortunately, GraalVM does not seem to support cross-compilation yet. Thus, we need to find other alternatives. Hopefully, Quarkus provide a very accessible technique, [described here](https://quarkus.io/guides/building-native-image#container-runtime), which requires to have a container engine installed and does not require to install GraalVM.

In this post, we'll try out to use a container to build the native image and we'll use [podman](https://podman.io/) as the container engine.

## Prerequisites

Please read [my previous post](https://dev.to/yostane/using-podman-to-run-testcontainers-in-a-quarkus-project-11me) which describes how to install the prerequisites which are summarized as follows:

- Java JDK 17
- podman
- On Windows, WSL needs to be installed and enabled

You can verity that you're good to go by running these commands.

```shell
java --version
podman run hello-world
```

We don't need to install GraalVM nor any native compilation toolchain as they will be handled by the container which will build the native image.
That's what makes this technique so easy !.

### Sample project

We're going to use the [sample project developed](https://github.com/yostane/quarkus-rest-api-demo) during a previous post. It consists of a Quarkus Rest API that uses a PostgreSQL database for development and a H2 in-memory database for production. For the sake of simplification, H2 is chosen in because it makes the production app autonomous and easier to test. The **application.yaml** file is shown below:

```yaml
greeting:
  message: "hello"

quarkus:
  datasource:
    db-kind: PostgreSQL

"%prod":
  quarkus:
    datasource:
      db-kind: H2
      jdbc:
        url: jdbc:h2:mem:default
```

After cloning the project, please run the following commands to verify that we're ready to go:

- Run tests: `.\gradlew test` or in Windows `.\gradlew.bat test`
- Run the server in production mode: `.\gradlew quarkusDev -Dquarkus.profile=prod`

The **backtick** character is required on PowerShell for arguments that have a dot **.**. So, `-Dquarkus.profile=prod` becomes `(backtick)-Dquarkus.profile=prod` in PowerShell, and so on.

Checking the production profile locally is recommended because the native image will use that profile.
Once you're ready, let's create the native image.

## Creating the native image using a container

Open a terminal on the project folder and run this command:

```shell
.\gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true -Dquarkus.native.container-runtime=podman
```

This argument `-Dquarkus.native.container-runtime=podman` allows to use podman as a container tool.

The command should finish with an output similar to this one:

```shell
------------------------------------------------------------------------------------------------------------------------
Produced artifacts:
 /project/testcontainers-demo-1.0.0-SNAPSHOT-runner (executable)
 /project/testcontainers-demo-1.0.0-SNAPSHOT-runner-build-output-stats.json (json)
 /project/testcontainers-demo-1.0.0-SNAPSHOT-runner-timing-stats.json (raw)
 /project/testcontainers-demo-1.0.0-SNAPSHOT-runner.build_artifacts.txt (txt)
========================================================================================================================
Finished generating 'testcontainers-demo-1.0.0-SNAPSHOT-runner' in 3m 34s.
```

The native app (also called native image) is **/project/testcontainers-demo-1.0.0-SNAPSHOT-runner**

## Running the Linux app from other OSes

We'll be exploring two techniques even if there may be more, such using a virtual machine.

### Using WSL on Windows

On windows, it's quite easy to test thanks to WSL:

```shell
wsl # open a WSL session
./build/testcontainers-demo-1.0.0-SNAPSHOT-runner
```

The output should display this:

```shell
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2022-12-06 22:42:05,809 INFO  [io.quarkus] (main) testcontainers-demo 1.0.0-SNAPSHOT native (powered by Quarkus 2.14.2.Final) started in 0.942s. Listening on: http://0.0.0.0:8
080
2022-12-06 22:42:05,828 INFO  [io.quarkus] (main) Profile prod activated.
2022-12-06 22:42:05,828 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, config-yaml, hibernate-orm, hibernate-orm-panache, hibernate-orm-panache-kotlin, jdbc-h2, j
dbc-postgresql, kotlin, narayana-jta, resteasy-reactive, resteasy-reactive-jackson, resteasy-reactive-kotlin-serialization, smallrye-context-propagation, vertx]
^C2022-12-06 22:42:12,136 INFO  [io.quarkus] (Shutdown thread) testcontainers-demo stopped in 0.012s
```

This means that the server is up and running.

### Using a container

We can also test the native Linux app by running it from a container. This allows to run the app from any OS as long as there is a tool that runs containers. But, we need to first create the image.

Hopefully, projects generated using Quarkus starter already provides the configuration file, located in **src/main/docker/Dockerfile.native-micro** that is used by the container tool in order to generate the image that hosts and runs the native Linux app. It just requires that the native Linux app is already generated, which we already did. So, let's run this command:

```shell
podman build -f src/main/docker/Dockerfile.native-micro -t IMAGE_NAME .
```

Please replace IMAGE_NAME with the desired name of the image, such as: `myapp/native-server`.

You can test the image by running `podman run IMAGE_NAME_OR_ID` and upload it to a container registry by running `podman push IMAGE_NAME_OR_ID REGISTRY_NAME`. **IMAGE_NAME_OR_ID REGISTRY_NAME** corresponds to either the name of the image that you used earlier, or its ID that you can get by running `podman images`.

If running the image fails, it may be due to a missing execution permission for the native app. Please add a `RUN chmod +x /work/application` in the dockerfile **src/main/docker/Dockerfile.native-micro** to fix this. The file that I used is shown below:

```dockerfile
FROM quay.io/quarkus/quarkus-micro-image:1.0
WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --chown=1001:root build/*-runner /work/application
RUN chmod +x /work/application

EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
```

You can run my generated image hosted on [quay.io](https://quay.io) as an example: `podman run quay.io/yostane/quarkus-jvm-demo/quarkus-jvm-demo-micro`.

```shell
podman run quay.io/yostane/quarkus-jvm-demo/quarkus-jvm-demo-micro
__  ____  __  _____   ___  __ ____  ______
 --/ __ \/ / / / _ | / _ \/ //_/ / / / __/
 -/ /_/ / /_/ / __ |/ , _/ ,< / /_/ /\ \
--\___\_\____/_/ |_/_/|_/_/|_|\____/___/
2022-12-07 20:19:51,660 INFO  [io.quarkus] (main) testcontainers-demo 1.0.0-SNAPSHOT native (powered by Quarkus 2.14.2.Final) started in 0.357s. Listening on: http://0.0.0.0:8080
2022-12-07 20:19:51,669 INFO  [io.quarkus] (main) Profile prod activated.
2022-12-07 20:19:51,669 INFO  [io.quarkus] (main) Installed features: [agroal, cdi, config-yaml, hibernate-orm, hibernate-orm-panache, hibernate-orm-panache-kotlin, jdbc-h2, jdbc-postgresql, kotlin, narayana-jta, resteasy-reactive, resteasy-reactive-jackson, resteasy-reactive-kotlin-serialization, smallrye-context-propagation, vertx]
```

## Conclusion

This post showed how to generate a native Linux image of a Quarkus application in a very simple and straightforward way. This native app does not a require a JRE and is **"theoretically"** faster and more optimized than a JVM counterpart.

We used a container to generated the image which handled all the native complication toolchain. It also allowed to cross-compile to Linux. I was really surprised to see how it's easy to generate a native image using this technique. So, why not try it out on your side ?
