## libertyDev Task

Start a Liberty instance in dev mode. This task also invokes the `libertyCreate`, `installFeature`, and `deploy` tasks before starting the runtime.
**Note:** This task is designed to be executed directly from the Gradle command line.

Starting in version 3.4.1, dev mode invokes the `generateFeatures` task when the `generateFeatures` configuration parameter is set to `true`. **This task modifies the source configuration directory of your application.** See [generateFeatures](generateFeatures.md) for details. The default value for the `generateFeatures` parameter is `false`. When auto-generation of features is turned on, dev mode has a runtime dependency on IBM WebSphere Application Server Migration Toolkit for Application Binaries, which is separately licensed under IBM License Agreement for Non-Warranted Programs. For more information, see the [license](https://public.dhe.ibm.com/ibmdl/export/pub/software/websphere/wasdev/license/wamt).

**Limitations:** This task is not supported with Spring Boot applications.

This task requires applications to be installed as loose applications. Information on configuring loose applications can be found in the [deploy task parameter documentation](deploy.md#Parameters) and the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration).

To start the server in a container, see the [libertyDevc](#libertydevc-task-container-mode) section below. 

### Console Actions

While dev mode is running, perform the following in the command terminal to run the corresponding actions.

* <kbd>g</kbd> - To toggle the automatic generation of features, type <kbd>g</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>o</kbd> - To optimize the list of generated features, type <kbd>o</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>t</kbd> or <kbd>Enter</kbd> - If `changeOnDemandTestAction` is enabled, type <kbd>t</kbd> and press <kbd>Enter</kbd> to run tests on demand. Otherwise, press <kbd>Enter</kbd>. 
* <kbd>r</kbd> - To restart the server, type <kbd>r</kbd> and press <kbd>Enter</kbd>.
* <kbd>h</kbd> - To see the help menu for available actions, type <kbd>h</kbd> and press <kbd>Enter</kbd>.
* <kbd>q</kbd> - stop the server and quit dev mode, press <kbd>Ctrl</kbd>-<kbd>C</kbd>, or type <kbd>q</kbd> and press <kbd>Enter</kbd>.

### Features

Dev mode provides three key features. Code changes are detected, recompiled, and picked up by your running server. Tests are run on demand when you press <kbd>Enter</kbd> in the command terminal where dev mode is running, or optionally on every code change to give you instant feedback on the status of your code. Finally, it allows you to attach a debugger to the running server at any time to step through your code.

The following are dev mode supported code changes. Changes to your server such as changes to the port, server name, hostname, etc. will require restarting dev mode to be detected.  Changes other than those listed below may also require restarting dev mode to be detected.

* Java source file changes and Java test file changes are detected, recompiled, and picked up by your running server.  
* Added dependencies to your `build.gradle` are detected and added to your classpath.  Dependencies that are Liberty features will be installed via the `installFeature` task.  Any other changes to your `build.gradle` will require restarting dev mode to be detected.
* Resource file changes are detected and copied into your `target` directory. 
* Configuration directory and configuration file changes are detected and copied into your `target` directory.  Added features to your `server.xml` will be installed and picked up by your running server.  Adding a configuration directory or configuration file that did not previously exist while dev mode is running will require restarting dev mode to be detected.

### Multi-Project Builds

Dev mode can be run on a single Gradle project or on a multi-project build (a project consisting of multiple projects specified as include(<module_name_one>,<module_name_two>,....) section of its settings.gradle). When run on a single Gradle project, only changes within that project are detected and hot deployed. When run on a multi-project build, changes in all projects are detected and hot deployed according to the Gradle build order. Note that any projects that other projects rely on as a compile dependency must have a non-empty Java source folder with Java file(s) before starting dev mode, otherwise the other projects may fail to compile.

To start dev mode on a multi-project build by using the short-form `libertyDev` task for the Liberty Gradle plugin:
1. Do one of the following:
* Define the Liberty Gradle plugin in the multi-project build.gradle,
* or define the Liberty Gradle plugin in the build.gradle of every subproject.

2. If the Liberty Gradle plugin is defined in your `build.gradle` file(s), ensure it is at version `3.9.2` or later.
3. From the directory containing the multi-project `build.gradle`, run:
```
$ gradle libertyDev
```

Liberty server configuration files (such as `server.xml`) will be used from the project that does not have any other projects depending on it.  If there is more than one project without other project depending on it, specify which project with Liberty configuration that you want to use by including the project while running libertyDev `ear:libertyDev`.  
For example, to use Liberty configuration from a project with artifact id `ear`, run:
```
$ gradle ear:libertyDev
```

More details on Gradle multi-project builds can be found at https://docs.gradle.org/current/userguide/intro_multi_project_builds.html

### Examples

The examples below apply regardless of whether you are using a single project or multi-project build.

Start dev mode.
```
$ gradle libertyDev
```

Start dev mode and run tests automatically after every code change.
```
$ gradle libertyDev --hotTests
```

Start dev mode and listen on a specific port for attaching a debugger (default is 7777).
```
$ gradle libertyDev --libertyDebugPort=8787
```

Start dev mode without allowing to attach a debugger.
```
$ gradle libertyDev --libertyDebug=false
```

### Command Line Parameters

The following are optional command line parameters supported by this task.

| Parameter | Description | Required |
| --------  | ----------- | -------  |
| compileWait | Time in seconds to wait before processing Java changes. If you encounter compile errors while refactoring, increase this value to allow all files to be saved before compilation occurs. The default value is `0.5` seconds. | No |
| generateFeatures | If set to `true`, when a Java file, server configuration file, or build file is changed, generate features required by the application in the source configuration directory. The default value is `false`. | No |
| hotTests | If this option is enabled, run tests automatically after every change. The default value is `false`. | No |
| libertyDebug | Whether to allow attaching a debugger to the running server. The default value is `true`. | No |
| libertyDebugPort | The debug port that you can attach a debugger to. The default value is `7777`. | No |
| serverStartTimeout | Maximum time to wait (in seconds) to verify that the server has started. The value must be an integer greater than or equal to 0. The default value is `90` seconds. | No |
| skipInstallFeature | If set to `true`, the `installFeature` task will be skipped when `dev` mode is started on an already existing Liberty runtime installation. It will also be skipped when `dev` mode is running and a restart of the server is triggered either directly by the user or by application changes. The `installFeature` task will be invoked though when `dev` mode is running and a change to the configured features is detected. The default value is `false`. | No |
| skipTests | If this option is enabled, do not run any tests in dev mode, even when the on demand test action is entered or when `hotTests` is set to `true`. The default value is `false`. | No |
| verifyAppStartTimeout | Maximum time to wait (in seconds) to verify that the application has started or updated before running tests. The value must be an integer greater than or equal to 0. The default value is `30` seconds. | No |

### Properties

The `dev` extension allows you to configure properties for the `libertyDev` task.

These can also be specified as command line parameters in addition to the ones in the section above.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| changeOnDemandTestsAction | boolean | 3.9.0 | If set to `true`, change the action for running on demand tests from `Enter` to type `t` and press `Enter`. The default value is `false`. | No |
| skipInstallFeature | String | 3.9.4-SNAPSHOT | If set to `true`, the `installFeature` task will be skipped when `dev` mode is started on an already existing Liberty runtime installation. It will also be skipped when `dev` mode is running and a restart of the server is triggered either directly by the user or by application changes. The `installFeature` task will be invoked though when `dev` mode is running and a change to the configured features is detected. The default value is `false`. | No |

See the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration) properties for common server configuration.

### Examples

Start dev mode and change the on demand tests action from `Enter` to type `t` and press `Enter`.
```
$ gradle libertyDev --changeOnDemandTestsAction
```

Customizing the configuration using `dev` extension properties in `build.gradle`.  Note that changing these while dev mode is running is not supported.
```
liberty {
    dev {
        changeOnDemandTestsAction=true
    }
}
```

or

```
ext {
    liberty.dev.changeOnDemandTestsAction=true
}
```

Start dev mode and change the `skipInstallFeature` setting from `false` to `true`.
```
$ gradle libertyDev --skipInstallFeature=true
```

Customizing the configuration using `dev` extension properties in `build.gradle`.  Note that changing these while dev mode is running is not supported.
```
liberty {
    dev {
        skipInstallFeature=true
    }
}
```

or

```
ext {
    liberty.dev.skipInstallFeature=true
}
```

### System Properties for Tests

Tests can read the following system properties to obtain information about the Liberty server.

| Property | Description |
| --------  | ----------- |
| wlp.user.dir | The user directory location that contains server definitions and shared resources. |
| liberty.hostname | The host name of the Liberty server. |
| liberty.http.port | The port used for client HTTP requests. |
| liberty.https.port | The port used for client HTTP requests secured with SSL (HTTPS). |

The Liberty Gradle plugin automatically propagates the system properties in the table above from the Gradle JVM to the JVM(s) running your tests. If you wish to add your own additional system properties you must configure your `build.gradle` file to set the system properties for the test JVM(s).

This can be done by setting specific properties for the test JVM.
```groovy
test {
    systemProperty 'example.property.1', System.getProperty('example.property.1')
    systemProperty 'example.property.2', System.getProperty('example.property.2')
    systemProperty 'example.property.3', System.getProperty('example.property.3')
}
```

Or by propagating all system properties from the Gradle JVM to the test JVM.
```groovy
test {
    systemProperties = System.properties
}
```

----

## libertyDevc Task, Container Mode

Start a Liberty server in a local container using the Containerfile/Dockerfile that you provide. An alternative to the `libertyDevc` task is to specify the `libertyDev` task with the `--container` option. 

When dev mode runs with container support, it builds a container image and runs the container. You can examine the commands that it uses to build and run the container by viewing the console output of dev mode. Additionally, it still provides the same features as the `libertyDev` task. It monitors files for changes and runs tests either automatically or on demand. This mode also allows you to attach a debugger to work on your application. You can review the logs generated by your server in the Liberty directory in your project e.g. build/wlp/usr/servers/defaultServer/logs.

**Limitations:** This task is not supported with Spring Boot applications.

This task requires applications to be installed as loose applications. Information on configuring loose applications can be found in the [deploy task parameter documentation](deploy.md#Parameters) and the [Liberty server configuration](libertyExtensions.md#liberty-server-configuration).

N.B. starting in 3.4.1, dev mode invokes `generate-features` if the `generateFeatures` configuration parameter is set to true. Ensure that the `generated-features.xml` configuration file is copied to your container image via your Containerfile/Dockerfile.
```dockerfile
COPY --chown=1001:0  build/wlp/usr/servers/defaultServer/configDropins/overrides/generated-features.xml /config/configDropins/overrides/
```
If on Linux, it is recommended that you copy the entire `configDropins/overrides` directory to your container image via your Containerfile/Dockerfile.
```dockerfile
COPY --chown=1001:0  build/wlp/usr/servers/defaultServer/configDropins/overrides /config/configDropins/overrides
```

### Prerequisites

You need to install Podman or the Docker runtime (Docker Desktop on macOS or Windows, or Docker on Linux) locally to use this Gradle task. If using Podman, version 4.4.4 or higher is required. If using Docker, the installed Docker Client and Engine versions must be 18.03.0 or higher.

### Containerfile and Dockerfile

Your project must have a Containerfile or Dockerfile to use dev mode in container mode. A sample Dockerfile is shown in [Building an application image](https://github.com/openliberty/ci.docker/#building-an-application-image). The parent image must be one of the [Open Liberty container images](https://github.com/openliberty/ci.docker/#container-images), or an image using Linux with Open Liberty configured with the same paths as the Open Liberty container images. The Containerfile/Dockerfile must copy the application .war file and the server configuration files that the application requires into the container.

Dev mode works with a temporary, modified copy of your Containerfile/Dockerfile to allow for hot deployment during development. When dev mode starts up, it pulls the latest version of the parent image defined in the Containerfile/Dockerfile, builds the container image, then runs the container. Note that the context of the container `build` command used to generate the container image is the directory containing the Containerfile/Dockerfile, unless the `containerBuildContext` parameter is specified. When dev mode exits, the container is stopped and deleted, and the logs are preserved in the directory mentioned above.

Hot deployment is made possible because the application is installed as a loose application WAR. This method uses a file type of `.war.xml` which is functionally equivalent to the `.war` file. Dev mode only supports the application under development in the current project so to avoid application conflicts, dev mode removes all Containerfile/Dockerfile commands that copy or add a `.war` file. 

The `.war.xml` file is generated in the `defaultServer/apps` or the `defaultServer/dropins` directory so these directories are mounted in the container. Therefore any files that the Containerfile/Dockerfile may have copied into these directories in the container image will not be accessible.

There are other features of the Containerfile/Dockerfile which are not supported for hot deployment of changes. See the section on [File Tracking](#File-Tracking) for details.

Finally, if dev mode detects the Liberty command `RUN configure.sh` it will insert the environment variable command `ENV OPENJ9_SCC=false` in order to skip the configuration of the [shared class cache](https://github.com/OpenLiberty/ci.docker/#openj9-shared-class-cache-scc).

### File Tracking

Dev mode offers different levels of file tracking and deployment depending on the way the file is specified in the Dockerfile. 
1. When you use the COPY command on an individual file, dev mode can track file changes and hot deploy them to the container subject to the limitations below. **This is the recommended way to deploy files for dev mode,** so that you can make changes to those files at any time without needing to rebuild the image or restart the container.
   - E.g. `COPY build/wlp/usr/servers/defaultServer/server.xml /config/`
   - Note that the Containerfile/Dockerfile must copy only one `.war` file for the application. See the section on [Dockerfiles](#containerfile-and-dockerfile) for details.
2. You can use the COPY command to deploy an entire directory and its sub-directories. In this case, dev mode will detect file changes and automatically rebuild the image and restart the container upon changes.
3. The ADD command can be used on individual files, including tar files, as well as on directories. Again, dev mode will rebuild the image and restart the container when it detects file changes. 
4. Certain Containerfile/Dockerfile features are not supported by dev mode. In these cases, the files specified are not tracked. If you change these files, you must rebuild the image and restart the container manually. **Type <kbd>r</kbd> and press <kbd>Enter</kbd> to rebuild the image and restart the container.**
   - variable substitution used in the COPY or ADD command e.g. `$PROJECT/config`
   - wildcards used in the COPY or ADD command e.g. `build/wlp/usr/servers/defaultServer/configDropins/*`
   - paths relative to WORKDIR e.g. `WORKDIR /other/project` followed by `COPY test.txt relativeDir/`
   - files copied from a different part of a multistage Podman or Docker build e.g. `COPY --from=<name>`

### Console Actions

While dev mode is running in container mode, perform the following in the command terminal to run the corresponding actions.

* <kbd>g</kbd> - To toggle the automatic generation of features, type <kbd>g</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>o</kbd> - To optimize the list of generated features, type <kbd>o</kbd> and press <kbd>Enter</kbd>. A new server configuration file will be generated in the SOURCE configDropins/overrides configuration directory.
* <kbd>t</kbd> or <kbd>Enter</kbd> - If `changeOnDemandTestAction` is enabled, type <kbd>t</kbd> and press <kbd>Enter</kbd> to run tests on demand. Otherwise, press <kbd>Enter</kbd>. 
* <kbd>r</kbd> - To rebuild the container image and restart the container, type <kbd>r</kbd> and press <kbd>Enter</kbd>.
* <kbd>h</kbd> - To see the help menu for available actions, type <kbd>h</kbd> and press <kbd>Enter</kbd>.
* <kbd>q</kbd> - stop the server and quit dev mode, press <kbd>Ctrl</kbd>-<kbd>C</kbd>, or type <kbd>q</kbd> and press <kbd>Enter</kbd>.

### Linux Limitations

The following limitations apply to Linux:

* In dev mode, the Open Liberty server runs in the container on the UID (user identifier) of the current user. This is so that the server can access the configuration files from your project and you can access the Open Liberty log files. Outside of dev mode, the Open Liberty server will run on the UID specified in the Docker image.
* Use of editors like `vim`: when you edit a configuration file with `vim` it will delete the file and rewrite it when you save. This necessitates a container restart. To avoid the restart edit your .vimrc file and add the line `set backupcopy=yes`

### Podman Troubleshooting
Podman VMs may need to be configured to run in rootful mode depending on how permissions are used in the container. This can be done with the following commands:
```
podman machine stop
podman machine set --rootful=true
podman machine start
```

If permissions issues continue with Podman the machine may need to be reinitialized. This can be done as follows:
```
podman machine stop
podman machine <machine name>
podman machine init --rootful=true
podman machine start
```

### Examples

Start dev mode with the server in a container using the Containerfile or Dockerfile in the project root.
```
$ gradle libertyDevc
```

Customizing the container configuration using `dev` extension properties in `build.gradle`.  Note that changing these while dev mode is running is not supported.
```
liberty {
    dev {
        container = true
        containerRunOpts = '-e key=value'
        containerfile = file('myDockerfile')
    }
}
```

### Port Mappings

By default, container mode publishes the following ports and maps them to the corresponding local ports of the same value:
* HTTP port at 9080
* HTTPS port at 9443
* Debug port at 7777

The container ports and mapped local ports will be displayed when dev mode starts up.

If you use the default ports and you run multiple instances of dev mode in container mode, the containers will use different local port mappings to avoid errors. The first instance will use the local ports 9080 and 9443, the second instance will use 9081 and 9444, and so on.

To publish additional ports, add them to the `containerRunOpts` parameter either in `build.gradle` or on the `gradle` command line.  For example:
```
--containerRunOpts="-p 8000:8000"
```

To map the container ports to specific local ports that are not the default, use the `skipDefaultPorts` parameter and specify container port mappings using the `containerRunOpts` parameter:
```
--skipDefaultPorts --containerRunOpts="-p 10000:9080 -p 10001:9443"
```

Alternatively, you can have Container map random ephemeral local ports to the exposed container ports as follows.
```
--skipDefaultPorts --containerRunOpts="-P"
```

Note that you do not need to specify an alternative for the debug port. Dev mode will automatically find an open local port to map the container debug port to.

### Properties

The `dev` extension allows you to configure properties for the `libertyDevc` task.

These can also be specified as command line parameters in addition to the ones in the `libertyDev` section above.

| Attribute | Type  | Since | Description | Required |
| --------- | ----- | ----- | ----------- | -------- |
| container | boolean | 3.1-M1 | If set to `true`, run the server in the container specified by the `containerfile` parameter. Setting this to `true` and using the `libertyDev` task is equivalent to using the `libertyDevc` task. The default value is `false` when the `libertyDev` task is used, and `true` when the `libertyDevc` task is used. | No |
| containerBuildContext | File | 3.7 | The container build context directory to be used by dev mode for the container `build` command.  The default location is the directory of the Containerfile/Dockerfile. This attribute replaces the deprecated `dockerBuildContext` attribute and will take precedence. | No |
| containerBuildTimeout | integer | 3.7 | Maximum time to wait (in seconds) for the completion of the container operation to build the image. The value must be an integer greater than 0. The default value is `600` seconds. This attribute replaces the deprecated `dockerBuildTimeout` attribute and will take precedence. | No |
| containerfile | File | 3.7 | Location of a Containerfile or Dockerfile to be used by dev mode to build the image for the container that will run your Liberty server. The default location is `Containerfile` or `Dockerfile` in the project root. This attribute replaces the deprecated `dockerfile` attribute and will take precedence. | No |
| containerRunOpts | String | 3.7 | Specifies options to add to the container `run` command when using dev mode to launch your server in a container. For example, `-e key=value` is recognized by `docker run` to define an environment variable with the name `key` and value `value`. This attribute replaces the deprecated `dockerRunOpts` attribute and will take precedence. | No |
| keepTempContainerfile | boolean | 3.7 | If set to `true`, dev mode will not delete the temporary modified copy of your Containerfile/Dockerfile used to build the container image. This file is handy in case you need to debug the process of building the container image. The path of the temporary Containerfile/Dockerfile can be seen when dev mode displays the container `build` command. The default value is `false`. This attribute replaces the deprecated `keepTempDockerfile` attribute and will take precedence. | No |
| skipDefaultPorts | boolean | 3.1-M3 | If set to `true`, dev mode will not publish the default container port mappings of `9080:9080` (HTTP) and `9443:9443` (HTTPS). Use this option if you would like to specify alternative local ports to map to the exposed container ports for HTTP and HTTPS using the `containerRunOpts` parameter. | No |
