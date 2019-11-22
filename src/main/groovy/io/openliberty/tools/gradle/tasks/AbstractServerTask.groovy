/**
 * (C) Copyright IBM Corporation 2017, 2019.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.openliberty.tools.gradle.tasks

import groovy.xml.StreamingMarkupBuilder
import io.openliberty.tools.common.plugins.config.ApplicationXmlDocument
import io.openliberty.tools.common.plugins.config.ServerConfigDocument
import io.openliberty.tools.gradle.utils.CommonLogger

import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War
import org.gradle.plugins.ear.Ear

import java.nio.file.Files
import java.nio.file.StandardCopyOption

import org.apache.commons.io.FilenameUtils

import io.openliberty.tools.ant.ServerTask
import io.openliberty.tools.common.plugins.config.ServerConfigDropinXmlDocument;

import java.util.ArrayList
import java.util.List
import java.util.HashMap
import java.util.Map
import java.util.Map.Entry
import java.util.Properties
import java.util.HashSet
import java.util.Set
import java.util.EnumSet
import java.util.regex.Pattern
import java.util.regex.Matcher

import javax.xml.transform.TransformerException
import javax.xml.parsers.ParserConfigurationException

abstract class AbstractServerTask extends AbstractTask {

    private final String HEADER = "# Generated by liberty-gradle-plugin"

    private static final String LIBERTY_CONFIG_GRADLE_PROPS = "(^liberty\\.server\\.(env|jvmOptions|bootstrapProperties|var|defaultVar))\\.(.+)"
    private static final Pattern pattern = Pattern.compile(LIBERTY_CONFIG_GRADLE_PROPS)

    protected final String PLUGIN_VARIABLE_CONFIG_XML = "configDropins/overrides/liberty-plugin-variable-config.xml"

    protected Properties bootstrapProjectProps = new Properties()
    protected Properties envProjectProps = new Properties()
    protected List<String> jvmProjectProps = new ArrayList<String>()
    protected Properties varProjectProps = new Properties()
    protected Properties defaultVarProjectProps = new Properties()

    protected Map<String,String> combinedBootstrapProperties = null
    protected List<String> combinedJvmOptions = null
    protected Map<String,String> combinedEnvProperties = null

    def server
    def springBootBuildTask

    private enum PropertyType {
        BOOTSTRAP("liberty.server.bootstrapProperties"),
        ENV("liberty.server.env"),
        JVM("liberty.server.jvmOptions"),
        VAR("liberty.server.var"),
        DEFAULTVAR("liberty.server.defaultVar");

        private final String name

        private PropertyType(final String propName) {
            this.name = propName
        }

        private static final Map<String, PropertyType> lookup = new HashMap<String, PropertyType>()

        static {
            for (PropertyType s : EnumSet.allOf(PropertyType.class)) {
               lookup.put(s.name, s)
            }
        }

        public static PropertyType getPropertyType(String propertyName) {
            PropertyType pt = lookup.get(propertyName)
            if (pt == null) {
                // get a matcher object from pattern 
                Matcher matcher = pattern.matcher(propertyName)
  
                // check whether Regex string is found in propertyName or not 
                if (matcher.find()) {
                    // strip off the end of the property name to get the prefix
                    String prefix = matcher.group(1);
                    pt = lookup.get(prefix);
                }
            }
            return pt
        } 

        public static String getSuffix(propertyName) {
            // get a matcher object from pattern 
            Matcher matcher = pattern.matcher(propertyName)
 
            // check whether Regex string is found in propertyName or not 
            if (matcher.find()) {
                // strip off the beginning of the property name 
                String suffix = matcher.group(3)
                // strip off surrounding quotation marks
                if (suffix.startsWith("\"") && suffix.endsWith("\"")) {
                    suffix = suffix.substring(1, suffix.length() -1)
                }
                return suffix
            }
            return null
        }

    }

    protected determineSpringBootBuildTask() {
        if (springBootVersion ?. startsWith('2')    ) {
            return project.bootJar
        }
        else if ( springBootVersion ?. startsWith('1') ) {
            return project.bootRepackage
        }
    }

    protected void executeServerCommand(Project project, String command, Map<String, String> params) {
        project.ant.taskdef(name: 'server',
                            classname: 'io.openliberty.tools.ant.ServerTask',
                            classpath: project.buildscript.configurations.classpath.asPath)
        params.put('operation', command)
        project.ant.server(params)
    }

    protected Map<String, String> buildLibertyMap(Project project) {
        Map<String, String> result = new HashMap();
        result.put('serverName', server.name)

        def installDir = getInstallDir(project)
        result.put('installDir', installDir)

        def userDir = getUserDir(project, installDir)
        result.put('userDir', userDir)

        result.put('outputDir', getOutputDir(project))

        if (server.timeout != null && !server.timeout.isEmpty()) {
            result.put('timeout', server.timeout)
        }

        return result;
    }

    protected List<String> buildCommand (String operation) {
        List<String> command = new ArrayList<String>()
        String installDir = getInstallDir(project).toString()

        if (isWindows) {
            command.add(installDir + "\\bin\\server.bat")
        } else {
            command.add(installDir + "/bin/server")
        }
        command.add(operation)
        command.add(server.name)

        return command
    }

    protected File getServerDir(Project project){
        return new File(getUserDir(project).toString() + "/servers/" + server.name)
    }

    protected String getOutputDir(Project project) {
        if (server.outputDir != null) {
            return server.outputDir
        } else if (project.liberty.outputDir != null) {
            return project.liberty.outputDir
        } else {
            return getUserDir(project).toString() + "/servers"
        }
    }

    /**
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void copyConfigFiles() throws IOException {

        String serverDirectory = getServerDir(project).toString()
        String serverXMLPath = null
        String jvmOptionsPath = null
        String bootStrapPropertiesPath = null
        String serverEnvPath = null

        // First check for Liberty configuration specified by Gradle project properties.
        loadLibertyConfigFromProperties();

        if (server.configDirectory == null) {
            server.configDirectory = new File(project.projectDir, "src/main/liberty/config")
        }

        if(server.configDirectory.exists()) {
            // copy configuration files from configuration directory to server directory if end-user set it
            FileUtils.copyDirectory(server.configDirectory, getServerDir(project))

            File configDirServerXML = new File(server.configDirectory, "server.xml")
            if (configDirServerXML.exists()) {
                serverXMLPath = configDirServerXML.getCanonicalPath()
            }

            File configDirJvmOptionsFile = new File(server.configDirectory, "jvm.options")
            if (configDirJvmOptionsFile.exists()) {
                jvmOptionsPath = configDirJvmOptionsFile.getCanonicalPath()
            }

            File configDirBootstrapFile = new File(server.configDirectory, "bootstrap.properties")
            if (configDirBootstrapFile.exists()) {
                bootStrapPropertiesPath = configDirBootstrapFile.getCanonicalPath()
            }

            File configDirServerEnv = new File(server.configDirectory, "server.env")
            if (configDirServerEnv.exists()) {
                serverEnvPath = configDirServerEnv.getCanonicalPath()
            }
        }

        // serverXmlFile takes precedence over server.xml from configDirectory
        // copy configuration file to server directory if end-user set it.
        if (server.serverXmlFile != null && server.serverXmlFile.exists()) {
            Files.copy(server.serverXmlFile.toPath(), new File(serverDirectory, "server.xml").toPath(), StandardCopyOption.REPLACE_EXISTING)
            serverXMLPath = server.serverXmlFile.getCanonicalPath()
        }

        // jvmOptions, jvmOptionsFile and jvmProjectProps take precedence over jvm.options from configDirectory
        File optionsFile = new File(serverDirectory, "jvm.options")
        if((server.jvmOptions != null && !server.jvmOptions.isEmpty()) || !jvmProjectProps.isEmpty()){
            if (jvmOptionsPath != null) {
                logger.warn("The " + jvmOptionsPath + " file is overwritten by inlined configuration.")
            }
            writeJvmOptions(optionsFile, server.jvmOptions, jvmProjectProps)
            jvmOptionsPath = "inlined configuration"
        } else if (server.jvmOptionsFile != null && server.jvmOptionsFile.exists()) {
            if (jvmOptionsPath != null) {
                logger.warn("The " + jvmOptionsPath + " file is overwritten by the " + jvmOptionsFile.getCanonicalPath() + " file.");
            }
            Files.copy(server.jvmOptionsFile.toPath(), optionsFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            jvmOptionsPath = server.jvmOptionsFile.getCanonicalPath()
        }

        // bootstrapProperties, bootstrapPropertiesFile and bootstrapProjectProps take precedence over 
        // bootstrap.properties from configDirectory
        File bootstrapFile = new File(serverDirectory, "bootstrap.properties")
        if((server.bootstrapProperties != null && !server.bootstrapProperties.isEmpty()) || !bootstrapProjectProps.isEmpty()){
            if (bootStrapPropertiesPath != null) {
                logger.warn("The " + bootStrapPropertiesPath + " file is overwritten by inlined configuration.")
            }
            writeBootstrapProperties(bootstrapFile, server.bootstrapProperties, bootstrapProjectProps)
            bootStrapPropertiesPath = "inlined configuration"
        } else if (server.bootstrapPropertiesFile != null && server.bootstrapPropertiesFile.exists()) {
            if (bootStrapPropertiesPath != null) {
                logger.warn("The " + bootStrapPropertiesPath + " file is overwritten by the " + server.bootstrapPropertiesFile.getCanonicalPath() + " file.")
            }
            Files.copy(server.bootstrapPropertiesFile.toPath(), bootstrapFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            bootStrapPropertiesPath = server.bootstrapPropertiesFile.getCanonicalPath()
        }

        // envProjectProps and serverEnvFile take precedence over server.env from configDirectory
        File envFile = new File(serverDirectory, "server.env")
        if ((server.env != null && !server.env.isEmpty()) || !envProjectProps.isEmpty()) {
            if (serverEnvPath != null) {
                logger.warn("The " + serverEnvPath + " file is overwritten by inlined configuration.")
            }
            writeServerEnvProperties(envFile, server.env, envProjectProps)
            serverEnvPath = "inlined configuration"
        } else if (server.serverEnvFile != null && server.serverEnvFile.exists()) {
             if (serverEnvPath != null) {
                logger.warn("The " + serverEnvPath + " file is overwritten by the " + server.serverEnvFile.getCanonicalPath() + " file.")
            }
            Files.copy(server.serverEnvFile.toPath(), envFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            serverEnvPath = server.serverEnvFile.getCanonicalPath()
        }

        // generate a config file on the server with any Liberty configuration variables specified via project properties
        if ((server.var != null && !server.var.isEmpty()) || (server.defaultVar != null && !server.defaultVar.isEmpty()) || 
             !varProjectProps.isEmpty() || !defaultVarProjectProps.isEmpty()) {
            File pluginVariableConfig = new File(serverDirectory, PLUGIN_VARIABLE_CONFIG_XML)
            writeConfigDropinsServerVariables(pluginVariableConfig, server.var, server.defaultVar, varProjectProps, defaultVarProjectProps)
            logger.info("Generate server configuration file " + pluginVariableConfig.getCanonicalPath())
        }

        // log info on the configuration files that get used
        if (serverXMLPath != null && !serverXMLPath.isEmpty()) {
            logger.info("Update server configuration file server.xml from " + serverXMLPath)
        }
        if (jvmOptionsPath != null && !jvmOptionsPath.isEmpty()) {
            logger.info("Update server configuration file jvm.options from " + jvmOptionsPath)
        }
        if (bootStrapPropertiesPath != null && !bootStrapPropertiesPath.isEmpty()) {
            logger.info("Update server configuration file bootstrap.properties from " + bootStrapPropertiesPath)
        }
        if (serverEnvPath != null && !serverEnvPath.isEmpty()) {
            logger.info("Update server configuration file server.env from " + serverEnvPath)
        }
    }

    private void loadLibertyConfigFromProperties() {
        Set<Entry<Object, Object>> entries = project.getProperties().entrySet()
        for (Entry<Object, Object> entry : entries) {
            String key = (String) entry.getKey()
            PropertyType propType = PropertyType.getPropertyType(key)

            if (propType != null) {
                String suffix = PropertyType.getSuffix(key)
                if (suffix != null) {
                    // dealing with single property
                    Object value = entry.getValue()
                    String propValue = value == null ? null : value.toString()
                    if (propValue != null && propValue.startsWith("\"") && propValue.endsWith("\"")) {
                        propValue = propValue.substring(1, propValue.length() -1)
                    }

                    addProjectProperty(suffix, propValue, propType)
                } else {
                    // dealing with array of properties
                    Object value = entry.getValue()
                    String propValue = value == null ? null : value.toString()
                    if ( (propValue != null) && ( (propValue.startsWith("{") && propValue.endsWith("}")) || (propValue.startsWith("[") && propValue.endsWith("]")) ) ) {
                        propValue = propValue.substring(1, propValue.length() -1)
                    }

                    // parse the array where properties are delimited by commas and the name/value are separated with a colon
                    String[] values = propValue.split(",")
                    for (String nextNameValuePair : values) {
                        String trimmedNameValuePair = nextNameValuePair.trim()
                        String[] splitNameValue = trimmedNameValuePair.split(":")
                        String nextPropName = splitNameValue[0].trim()

                        // remove surrounding quotes from property names and property values
                        if (nextPropName.startsWith("\"") && nextPropName.endsWith("\"")) {
                            nextPropName = nextPropName.substring(1, nextPropName.length() -1)
                        }

                        String nextPropValue = null
                        if (splitNameValue.length == 2) {
                            nextPropValue = splitNameValue[1].trim()
                            if (nextPropValue.startsWith("\"") && nextPropValue.endsWith("\"")) {
                                nextPropValue = nextPropValue.substring(1, nextPropValue.length() -1)
                            }
                        }

                        addProjectProperty(nextPropName, nextPropValue, propType)
                    }
                }
            }
        }
    }

    private void addProjectProperty(String propName, String propValue, PropertyType propType) {
        if (propValue != null) {
            logger.debug("Processing Liberty configuration from property with type "+ propType +" and name "+ propName +" and value "+ propValue)
        } else {
            logger.debug("Processing Liberty configuration from property with type "+ propType +" and value " + propName)
        }
        switch (propType) {
            case PropertyType.ENV:        envProjectProps.setProperty(propName, propValue)
                                          break
            case PropertyType.BOOTSTRAP:  bootstrapProjectProps.setProperty(propName, propValue)
                                          break
            case PropertyType.JVM:        jvmProjectProps.add(propName)
                                          break
            case PropertyType.VAR:        varProjectProps.setProperty(propName, propValue)
                                          break
            case PropertyType.DEFAULTVAR: defaultVarProjectProps.setProperty(propName, propValue)
                                          break
        }
    }

    protected void setServerDirectoryNodes(Project project, Node serverNode) {
        serverNode.appendNode('userDirectory', getUserDir(project).toString())
        serverNode.appendNode('serverDirectory', getServerDir(project).toString())
        serverNode.appendNode('serverOutputDirectory', new File(getOutputDir(project), server.name))
    }

    protected void setServerPropertyNodes(Project project, Node serverNode) {
        serverNode.appendNode('serverName', server.name)
        if (server.configDirectory != null && server.configDirectory.exists()) {
            serverNode.appendNode('configDirectory', server.configDirectory.toString())
        }

        if (server.serverXmlFile != null && server.serverXmlFile.exists()) {
            serverNode.appendNode('configFile', server.serverXmlFile.toString())
        }

        if (combinedBootstrapProperties != null) {
            Node bootstrapProperties = new Node(null, 'bootstrapProperties')
            combinedBootstrapProperties.each { k, v ->
                bootstrapProperties.appendNode(k, v.toString())
            }
            serverNode.append(bootstrapProperties)
        } else if (server.bootstrapProperties != null && !server.bootstrapProperties.isEmpty()) {
            Node bootstrapProperties = new Node(null, 'bootstrapProperties')
            server.bootstrapProperties.each { k, v ->
                bootstrapProperties.appendNode(k, v.toString())
            }
            serverNode.append(bootstrapProperties)
        } else if (server.bootstrapPropertiesFile != null && server.bootstrapPropertiesFile.exists()) {
            serverNode.appendNode('bootstrapPropertiesFile', server.bootstrapPropertiesFile.toString())
        }

        if (combinedJvmOptions != null) {
            Node jvmOptions = new Node(null, 'jvmOptions')
            combinedJvmOptions.each { v ->
                jvmOptions.appendNode('params', v.toString())
            }
            serverNode.append(jvmOptions)
        } else if (server.jvmOptions != null && !server.jvmOptions.isEmpty()) {
            Node jvmOptions = new Node(null, 'jvmOptions')
            server.jvmOptions.each { v ->
                jvmOptions.appendNode('params', v.toString())
            }
            serverNode.append(jvmOptions)
        } else if (server.jvmOptionsFile != null && server.jvmOptionsFile.exists()) {
            serverNode.appendNode('jvmOptionsFile', server.jvmOptionsFile.toString())
        }

        // Only write the serverEnvFile path if it was not overridden by liberty.env.{var} project properties.
        if (envProjectProps.isEmpty() && server.serverEnvFile != null && server.serverEnvFile.exists()) {
            serverNode.appendNode('serverEnv', server.serverEnvFile.toString())
        }

        serverNode.appendNode('looseApplication', server.looseApplication)
        serverNode.appendNode('stripVersion', server.stripVersion)

        configureMultipleAppsConfigDropins(serverNode)
    }

    protected boolean isAppConfiguredInSourceServerXml(String fileName) {
        boolean configured = false;
        File serverConfigFile = new File(getServerDir(project), 'server.xml')
        if (serverConfigFile != null && serverConfigFile.exists()) {
            try {
                ServerConfigDocument scd = ServerConfigDocument.getInstance(CommonLogger.getInstance(), serverConfigFile, server.configDirectory, server.bootstrapPropertiesFile, convertPropertiesToMap(server.bootstrapProperties), server.serverEnvFile, false);
                if (scd != null && scd.getLocations().contains(fileName)) {
                    logger.debug("Application configuration is found in server.xml : " + fileName)
                    configured = true
                }
            }
            catch (Exception e) {
                logger.warn(e.getLocalizedMessage())
            }
        }
        return configured
    }

    // Gradle passes the properties from the configuration as Strings and Integers and maybe Booleans.
    // Need to convert to the String values for those Objects before passing along to ServerConfigDocument.
    protected Map<String,String> convertPropertiesToMap(Properties props) {
        if (props == null) {
            return null
        }

        Map<String,String> returnProps = new HashMap<String,String> ()

        Set<Entry<Object, Object>> entries = props.entrySet()
        for (Entry<Object, Object> entry : entries) {
            String key = (String) entry.getKey()
            Object value = entry.getValue()
            if (value != null) {
                returnProps.put(key,value.toString())
            }
        }
        return returnProps
    }
    
    protected String getArchiveName(Task task){
        if (springBootVersion?.startsWith('1')) {
            task = project.jar
        }
        if (server.stripVersion){
            return task.baseName + "." + task.extension
        }
        return task.archiveName;
    }

    protected void configureApps(Project project) {
        if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
            if (!project.configurations.libertyApp.isEmpty()) {
                server.apps = getApplicationFilesFromConfiguration().toArray()
            } else if (project.plugins.hasPlugin('war')) {
                server.apps = [project.war]
            } else if (project.plugins.hasPlugin('ear')) {
                server.apps = [project.ear]
            } else if (project.plugins.hasPlugin('org.springframework.boot')) {
                server.apps = [springBootBuildTask]
            }
        }
    }
    
    protected void configureMultipleAppsConfigDropins(Node serverNode) {
        if (server.apps != null && !server.apps.isEmpty()) {
            Tuple applications = splitAppList(server.apps)
            applications[0].each{ Task task ->
              isConfigDropinsRequired(task, 'apps', serverNode)
            }
        }
    }
    
    protected Tuple splitAppList(List<Object> allApps) {
        List<File> appFiles = new ArrayList<File>()
        List<Task> appTasks = new ArrayList<Task>()

        allApps.each { Object appObj ->
            if (appObj instanceof Task) {
                appTasks.add((Task)appObj)
            } else if (appObj instanceof File) {
                appFiles.add((File)appObj)
            } else {
                logger.warn('Application ' + appObj.getClass.name + ' is expressed as ' + appObj.toString() + ' which is not a supported input type. Define applications using Task or File objects.')
            }
        }

        return new Tuple(appTasks, appFiles)
    }
    
    private boolean isSupportedType(){
      switch (getPackagingType()) {
        case "ear":
        case "war":
            return true;
        default:
            return false;
        }
    }
    private String getLooseConfigFileName(Task task){
      return getArchiveName(task) + ".xml"
    }
    
    protected void isConfigDropinsRequired(Task task, String appsDir, Node serverNode) {
        File installAppsConfigDropinsFile = ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project))
        if (isSupportedType()) {
          if (server.looseApplication){
            String looseConfigFileName = getLooseConfigFileName(task)
            String application = looseConfigFileName.substring(0, looseConfigFileName.length()-4)
            if (!isAppConfiguredInSourceServerXml(application)) {
                serverNode.appendNode('installAppsConfigDropins', installAppsConfigDropinsFile.toString())
            }
          } else {
                if (!isAppConfiguredInSourceServerXml(getArchiveName(task)) || hasConfiguredApp(ApplicationXmlDocument.getApplicationXmlFile(getServerDir(project)))) {
                    serverNode.appendNode('installAppsConfigDropins', installAppsConfigDropinsFile.toString())
                }
            }
        }
    }

    protected void createApplicationElements(Node applicationsNode, List<Objects> appList, String appDir) {
        springBootVersion=findSpringBootVersion(project)
        appList.each { Object appObj ->
            Node application = new Node(null, 'application')
            if (appObj instanceof Task) {
                if (springBootVersion?.startsWith('1')) {
                    appObj = project.jar
                }
                application.appendNode('appsDirectory', appDir)
                if (server.looseApplication) {
                    application.appendNode('applicationFilename', appObj.archiveName + '.xml')
                } else {
                    application.appendNode('applicationFilename', appObj.archiveName)
                }
                if (appObj instanceof War) {
                    application.appendNode('warSourceDirectory', project.webAppDirName)
                }
            } else if (appObj instanceof File) {
                application.appendNode('appsDirectory', appDir)
                if (server.looseApplication) {
                    application.appendNode('applicationFilename', appObj.name + '.xml')
                } else {
                    application.appendNode('applicationFilename', appObj.name)
                }
            }

            if(!application.children().isEmpty()) {
                if (project.plugins.hasPlugin("war")) {
                    application.appendNode('projectType', 'war')
                } else if (project.plugins.hasPlugin("ear")) {
                    application.appendNode('projectType', 'ear')
                }
                applicationsNode.append(application)
            }
        }
    }

    protected void setApplicationPropertyNodes(Project project, Node serverNode) {
        Node applicationsNode;
        if ((server.apps == null || server.apps.isEmpty()) && (server.dropins == null || server.dropins.isEmpty())) {
            if (project.plugins.hasPlugin('war')) {
                applicationsNode = new Node(null, 'applications')
                createApplicationElements(applicationsNode, [project.tasks.war], 'apps')
                serverNode.append(applicationsNode)
            }
        } else {
            applicationsNode = new Node(null, 'applications')
            if (server.apps != null && !server.apps.isEmpty()) {
                createApplicationElements(applicationsNode, server.apps, 'apps')
            }
            if (server.dropins != null && !server.dropins.isEmpty()) {
                createApplicationElements(applicationsNode, server.dropins, 'dropins')
            }
            serverNode.append(applicationsNode)
        }
    }

    protected void setDependencyNodes(Project project, Node serverNode) {
        Project parent = project.getParent()
        if (parent != null) {
            serverNode.appendNode('aggregatorParentId', parent.getName())
            serverNode.appendNode('aggregatorParentBasedir', parent.getProjectDir())
        }

        if (project.configurations.findByName('compile') && !project.configurations.compile.dependencies.isEmpty()) {
            project.configurations.compile.dependencies.each { dependency ->
                serverNode.appendNode('projectCompileDependency', dependency.group + ':' + dependency.name + ':' + dependency.version)
            }
        }
    }

    protected void writeServerPropertiesToXml(Project project) {
        XmlParser pluginXmlParser = new XmlParser()
        Node libertyPluginConfig = pluginXmlParser.parse(new File(project.buildDir, 'liberty-plugin-config.xml'))
        if (libertyPluginConfig.getAt('servers').isEmpty()) {
            libertyPluginConfig.appendNode('servers')
        } else {
            //removes the server nodes from the servers element
            libertyPluginConfig.getAt('servers')[0].value = ""
        }
        Node serverNode = new Node(null, 'server')

        setServerDirectoryNodes(project, serverNode)
        setServerPropertyNodes(project, serverNode)
        setApplicationPropertyNodes(project, serverNode)
        setDependencyNodes(project, serverNode)

        libertyPluginConfig.getAt('servers')[0].append(serverNode)

        new File( project.buildDir, 'liberty-plugin-config.xml' ).withWriter('UTF-8') { output ->
            output << new StreamingMarkupBuilder().bind { mkp.xmlDeclaration(encoding: 'UTF-8', version: '1.0' ) }
            XmlNodePrinter printer = new XmlNodePrinter( new PrintWriter(output) )
            printer.preserveWhitespace = true
            printer.print( libertyPluginConfig )
        }

        logger.info ("Adding Liberty plugin config info to ${project.buildDir}/liberty-plugin-config.xml.")
    }

    private void writeBootstrapProperties(File file, Properties properties, Map<String, String> projectProperties) throws IOException {
        Map<String,String> convertedProps = convertPropertiesToMap(properties)
        if (! projectProperties.isEmpty()) {
            if (properties == null) {
                combinedBootstrapProperties = projectProperties;
            } else {
                combinedBootstrapProperties = new HashMap<String,String> ()
                // add the project properties (which come from the command line) last so that they take precedence over the properties specified in build.gradle
                combinedBootstrapProperties.putAll(convertedProps)
                combinedBootstrapProperties.putAll(projectProperties)
            }
        } else {
            combinedBootstrapProperties = convertedProps
        }

        makeParentDirectory(file)
        PrintWriter writer = null
        try {
            writer = new PrintWriter(file, "UTF-8")
            writer.println(HEADER)
            for (Map.Entry<String, String> entry : combinedBootstrapProperties.entrySet()) {
                writer.print(entry.getKey())
                writer.print("=")
                writer.println((entry.getValue() != null) ? entry.getValue().toString().replace("\\", "/") : "")
            }
        } finally {
            if (writer != null) {
                writer.close()
            }
        }
    }

    private void writeJvmOptions(File file, List<String> options, List<String> projectProperties) throws IOException {
        if (! projectProperties.isEmpty()) {
            if (options == null) {
                combinedJvmOptions = projectProperties;
            } else {
                combinedJvmOptions = new ArrayList<String> ()
                // add the project properties (which come from the command line) last so that they take precedence over the options specified in build.gradle
                combinedJvmOptions.addAll(options)
                combinedJvmOptions.addAll(projectProperties)
            }
        } else {
            combinedJvmOptions = options
        }

        makeParentDirectory(file)
        PrintWriter writer = null
        try {
            writer = new PrintWriter(file, "UTF-8")
            writer.println(HEADER)
            for (String option : combinedJvmOptions) {
                writer.println(option)
            }
        } finally {
            if (writer != null) {
                writer.close()
            }
        }
    }

    private void writeServerEnvProperties(File file, Properties properties, Properties projectProperties) throws IOException {
        Properties combinedEnvProperties = null
        if (! projectProperties.isEmpty()) {
            if (properties.isEmpty()) {
                combinedEnvProperties = projectProperties
            } else {
                combinedEnvProperties = new Properties()
                // add the project properties (which come from the command line) last so that they take precedence over the properties specified in build.gradle
                combinedEnvProperties.putAll(properties)
                combinedEnvProperties.putAll(projectProperties)
            }
        } else {
            combinedEnvProperties = properties
        }

        makeParentDirectory(file)
        PrintWriter writer = null
        try {
            writer = new PrintWriter(file, "UTF-8")
            writer.println(HEADER)
            for (Map.Entry<String, String> entry : combinedEnvProperties.entrySet()) {
                writer.print(entry.getKey())
                writer.print("=")
                writer.println((entry.getValue() != null) ? entry.getValue().toString().replace("\\", "/") : "")
            }
        } finally {
            if (writer != null) {
                writer.close()
            }
        }
    }


    private void writeConfigDropinsServerVariables(File file, Properties varProps, Properties defaultVarProps, Properties varProjectProps, Properties defaultVarProjectProps) throws IOException, TransformerException, ParserConfigurationException {

        ServerConfigDropinXmlDocument configDocument = ServerConfigDropinXmlDocument.newInstance()

        configDocument.createComment(HEADER)
        Set<String> existingVarNames = new HashSet<String>()

        for (Map.Entry<String, String> entry : varProjectProps.entrySet()) {
            String key = entry.getKey()
            existingVarNames.add(key)
            configDocument.createVariableWithValue(entry.getKey(), entry.getValue(), false)
        }

        for (Map.Entry<String, String> entry : varProps.entrySet()) {
            String key = entry.getKey()
            existingVarNames.add(key)
            configDocument.createVariableWithValue(entry.getKey(), entry.getValue(), false)
        }

        for (Map.Entry<String, String> entry : defaultVarProjectProps.entrySet()) {
            // check to see if a variable with a value already exists with the same name and log it
            String key = entry.getKey()
            if (existingVarNames.contains(key)) {
                // since the defaultValue will only be used if no other value exists for the variable, 
                // it does not make sense to generate the variable with a defaultValue when we know a value already exists.
                logger.warn("The variable with name "+key+" and defaultValue "+entry.getValue()+" is skipped since a variable with that name already exists with a value.")
            } else {
                // set boolean to true so the variable is created with a defaultValue instead of a value
                configDocument.createVariableWithValue(entry.getKey(), entry.getValue(), true)
            }
        }

        for (Map.Entry<String, String> entry : defaultVarProps.entrySet()) {
            // check to see if a variable with a value already exists with the same name and log it
            String key = entry.getKey()
            if (existingVarNames.contains(key)) {
                // since the defaultValue will only be used if no other value exists for the variable, 
                // it does not make sense to generate the variable with a defaultValue when we know a value already exists.
                logger.warn("The variable with name "+key+" and defaultValue "+entry.getValue()+" is skipped since a variable with that name already exists with a value.")
            } else {
                // set boolean to true so the variable is created with a defaultValue instead of a value
                configDocument.createVariableWithValue(entry.getKey(), entry.getValue(), true)
            }
        }

        // write XML document to file
        makeParentDirectory(file)
        configDocument.writeXMLDocument(file)

    }

    private void makeParentDirectory(File file) {
        File parentDir = file.getParentFile()
        if (parentDir != null) {
            parentDir.mkdirs()
        }
    }

    protected String getPackagingType() throws Exception{
      if (project.plugins.hasPlugin("war") || !project.tasks.withType(War).isEmpty()) {
          if (project.plugins.hasPlugin("org.springframework.boot")) {
              return "springboot"
          }
          return "war"
      }
      else if (project.plugins.hasPlugin("ear") || !project.tasks.withType(Ear).isEmpty()) {
          return "ear"
      }
      else if (project.plugins.hasPlugin("org.springframework.boot") ) {
          return "springboot"
      }
      else {
          throw new GradleException("Archive path not found. Supported formats are jar, war, ear, and springboot jar.")
      }
    }

    //Checks if there is an app configured in an existing configDropins application xml file
    protected boolean hasConfiguredApp(File applicationXmlFile) {
      if (applicationXmlFile.exists()) {
          ApplicationXmlDocument appXml = new ApplicationXmlDocument()
          appXml.createDocument(applicationXmlFile)
          return appXml.hasChildElements()
      }
      return false
    }

    protected List<File> getApplicationFilesFromConfiguration() {
        List<File> appFiles = new ArrayList<File>()

        //This loops all the Dependency objects that get created by the configuration treating them as File objects
        //Should also include transitive dependencies
        //Can't use the resolved configuration unless we do a check separate from this one, not sure if there is an advantage since we want the applicaitons
        project.configurations.libertyApp.each {
            if (FilenameUtils.getExtension(it.name).equals('war') || FilenameUtils.getExtension(it.name).equals('ear')) {
                appFiles.add(it)
            }
        }

        return appFiles
    }

    protected ServerTask createServerTask(Project project, String operation) {
        ServerTask serverTask =  new ServerTask()
        serverTask.setOperation(operation)
        serverTask.setServerName(server.name)

        def installDir = getInstallDir(project)

        serverTask.setInstallDir(installDir)
        serverTask.setUserDir(getUserDir(project, installDir))

        serverTask.setOutputDir(new File(getOutputDir(project)))

        if (server.timeout != null && !server.timeout.isEmpty()) {
            serverTask.setTimeout(server.timeout)
        }

        return serverTask
    }


}
