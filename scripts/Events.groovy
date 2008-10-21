import grails.util.GrailsUtil
import groovy.xml.StreamingMarkupBuilder
import org.mortbay.jetty.Server
import org.mortbay.jetty.security.HashUserRealm
import org.mortbay.jetty.security.UserRealm
import org.mortbay.jetty.servlet.SessionHandler
import org.mortbay.jetty.webapp.WebAppContext
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

def version = 0.1
def basedir = System.getProperty("base.dir")
def portletXml = new File("${basedir}/web-app/WEB-INF/portlet.xml")
def pluginDir = "${pluginsHome}/portlets-${version}"
def pluginLibDir = "${pluginDir}/lib"
if (!new File(pluginDir).exists()) {
    //we must not be installed...
    pluginDir = basedir
    pluginLibDir = "${basedir}/lib"
    if (!new File(pluginLibDir).exists()) {
        throw new RuntimeException('Unable to find Portlets plugin lib folder')
    }
}

//FIX config is not loaded until packaging but we need to know this setting for SetClasspath...
//config.portlet.version = '2'

// default portlet spec to 1.0
def portletVersion = '1.0'
def plutoVersion = '1.1.4'

eventConfigureJetty = {Server server ->
    try {
        SessionHandler sh = new SessionHandler();
        //kindly borrowed from the Maven pluto plugin author Nils-Helge Garli
        def sessionMan = classLoader.loadClass('com.bekk.boss.pluto.embedded.jetty.util.PlutoJettySessionManager')
        sh.setSessionManager(sessionMan.newInstance());
        server.getHandler().setSessionHandler(sh);
    } catch (NoClassDefFoundError e) {
        // This script is compiled before the plugin source in some cases so we need to ignore compile errors as the classs wil be there at runtime when it's needed
    }

    // TODO refactor pluto specific code out to pluggable embedded portal interface
    def webContext = new WebAppContext("${pluginLibDir}/pluto-portal-${plutoVersion}", "pluto")
    webContext.systemClasses = ["-org.apache.pluto.driver.", "org.apache.pluto.", "javax.portlet.", "javax.servlet.", "org.springframework."]
    webContext.contextPath = "/pluto"
    try {
        sh = new SessionHandler();
        def sessionMan = classLoader.loadClass('com.bekk.boss.pluto.embedded.jetty.util.PlutoJettySessionManager')
        sh.setSessionManager(sessionMan.newInstance());
        webContext.setSessionHandler(sh);
    } catch (NoClassDefFoundError e) {
        // This script is compiled before the plugin source in some cases so we need to ignore compile errors as the classs wil be there at runtime when it's needed
    }
    server.addHandler(webContext)

    HashUserRealm myrealm = new HashUserRealm("default", "${pluginLibDir}/realm.properties");
    server.setUserRealms([myrealm] as UserRealm[]);
    println 'jetty'
}

eventSetClasspath = {rootLoader ->
    if (config?.portlet?.version == '2') {
        portletVersion = '2.0'
        plutoVersion = '2.0.0-SNAPSHOT'
    }
    event("StatusUpdate", ["Using Portlet Spec ${portletVersion}"])
    def jars = ["${pluginLibDir}/runtime/castor-1.1.1.jar",
            "${pluginLibDir}/runtime/pluto-container-${plutoVersion}.jar",
            "${pluginLibDir}/runtime/pluto-descriptor-api-${plutoVersion}.jar",
            "${pluginLibDir}/runtime/pluto-descriptor-impl-${plutoVersion}.jar",
            "${pluginLibDir}/runtime/pluto-taglib-${plutoVersion}.jar",
            "${pluginLibDir}/runtime/portlet-api-${portletVersion}.jar",
    ]
    if (portletVersion == '2.0') {
        jars += ["${pluginLibDir}/runtime/ccpp-1.0.jar",
                "${pluginLibDir}/runtime/jaxb-api-2.1.jar",
                "${pluginLibDir}/runtime/activation-1.1.jar",
                "${pluginLibDir}/runtime/stax-api-1.0-2.jar",
                "${pluginLibDir}/runtime/jaxb-impl-2.1.3.jar"]
    }
    jars.each {jar ->
        File file = new File(jar)
        if (!file.exists()) {
            throw new RuntimeException("Unable to find Portlets lib: $jar")
        }
        rootLoader.addURL(file.toURI().toURL());
    }
}

eventPackagingEnd = {
    //TODO refactor out to portletXmlWriter and unit test
    def plutoConfigXml = new File("${pluginLibDir}/pluto-portal-${plutoVersion}/WEB-INF/pluto-portal-driver-config.xml")
    try {
        def xmlWriter = new StreamingMarkupBuilder();
        def searchPath = "file:${basedir}/grails-app/controllers/**/*Portlet.groovy"
        def customModes = [:]
        def userAttributes = [:]
        event("StatusUpdate", ["Searching for portlets: ${searchPath}"])
        portletFiles = resolveResources(searchPath).toList()
        if (portletFiles.size() > 0) {
            event("StatusUpdate", ["Generating portlet.xml - ${portletFiles.size()} portlets found"])

            if (portletXml.exists()) portletXml.delete()
            def underscoredVersion = portletVersion.replaceAll("\\.", "_")
            def xml = xmlWriter.bind {
                'portlet-app'(version: portletVersion,
                        xmlns: "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd",
                        'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                        'xsi:schemaLocation': "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd") {
                    mkp.comment 'GENERATED BY GRAILS PORTLETS PLUGIN - DO NOT EDIT'
                    portletFiles.each {portletClassFile ->
                        def className = portletClassFile.filename - '.groovy'
                        Class portletClass = classLoader.loadClass(className)
                        def portletName = className - 'Portlet'
                        def instance = portletClass.newInstance()
                        checkRequiredProperties(['supports', 'title', 'displayName'], instance)
                        //TODO security constraints
                        portlet {
                            'portlet-name'(portletName)
                            'display-name'(instance.displayName)
                            if (hasProperty('description', instance))
                                'description'(instance.description)
                            'portlet-class'('org.codehaus.grails.portlets.GrailsDispatcherPortlet')
                            'init-param'
                            {
                                'name'('contextClass')
                                'value'('org.codehaus.grails.portlets.GrailsPortletApplicationContext')
                            }
                            'init-param'
                            {
                                'name'('grailsPortletClass')
                                'value'(className)
                            }
                            instance.supports.each {mime, types ->
                                'supports'
                                {
                                    'mime-type'(mime)
                                    types.each {mode ->
                                        'portlet-mode'(mode)
                                    }
                                }
                            }
                            if (hasProperty('customModes', instance) && instance.customModes instanceof Map) {
                                customModes += instance.supportsCustom
                            }
                            'portlet-info'
                            {
                                //TODO support 1l8n via properties files to supply these
                                'title'(instance.title)
                                if (hasProperty('shortTitle', instance)) 'short-title'(instance.shortTitle)
                                if (hasProperty('keywords', instance)) 'keywords'(instance.keywords)
                            }
                            if (hasProperty('roleRefs', instance) && instance.roleRefs instanceof List) {
                                instance.roleRefs.each {roleName ->
                                    'security-role-ref'
                                    {
                                        'role-name'(roleName)
                                    }
                                }
                            }
                            if (hasProperty('userAttributes', instance) && instance.userAttributes instanceof List) {
                                userAttributes += instance.userAttributes

                            }
                            if (hasProperty('supportedPreferences', instance) && instance.supportedPreferences instanceof Map) {
                                'portlet-preferences'
                                {
                                    instance.supportedPreferences.each {prefName, prefValue ->
                                        'preference'
                                        {
                                            'name'(prefName)
                                            if (prefValue instanceof List) {
                                                prefValue.each {multiValue ->
                                                    'value'(multiValue)
                                                }
                                            } else {
                                                'value'(prefValue)
                                            }
                                            /* TODO
                                            if (preference.readOnly) {
                                                'read-only'('true')
                                            }*/
                                        }
                                    }
                                }
                            }
                        }
                    }
                    userAttributes.each {userAttribute ->
                        'user-attribute'
                        {
                            'name'(userAttribute)
                        }
                    }
                    customModes.each {mode, description ->
                        'custom-portlet-mode'
                        {
                            'description'(description)
                            'name'(mode)
                        }
                    }

                }
            }
            portletXml.write(xml.toString())
            if (GrailsUtil.environment == 'development' || GrailsUtil.environment == 'test') {
                // TODO refactor pluto specific code out to pluggable embedded portal interface
                sw = new StringWriter()
                xmlWriter = new StreamingMarkupBuilder()
                if (plutoConfigXml.exists()) plutoConfigXml.delete()
                xml = xmlWriter.bind {
                    'pluto-portal-driver'(
                            'xmlns': "http://portals.apache.org/pluto/xsd/pluto-portal-driver-config.xsd",
                            'xmlns:xsi': "http://www.w3.org/2001/XMLSchema-instance",
                            'xsi:schemaLocation': "http://portals.apache.org/pluto/xsd/pluto-portal-driver-config.xsd",
                            'version': "1.1") {
                        'portal-name'('pluto-portal-driver')
                        'portal-version'(plutoVersion)
                        'container-name'('Pluto Portal Driver')
                        'supports'
                        {
                            'portlet-mode'('view')
                            'portlet-mode'('edit')
                            'portlet-mode'('help')
                            'portlet-mode'('config')

                            'window-state'('normal')
                            'window-state'('maximized')
                            'window-state'('minimized')
                        }
                        'render-config'(default: 'Home') {
                            'page'(name: "Home", uri: "/WEB-INF/themes/pluto-default-theme.jsp") {
                                portletFiles.each {portletClassFile ->
                                    def className = portletClassFile.filename - '.groovy'
                                    def portletName = className - 'Portlet'
                                    'portlet'(context: "/${grailsAppName}", name: portletName)
                                }
                            }
                            'page'(name: "About Apache Pluto", uri: "/WEB-INF/themes/pluto-default-theme.jsp") {
                                'portlet'(context: '/pluto', name: 'AboutPortlet')
                            }
                            'page'(name: "Pluto Admin", uri: "/WEB-INF/themes/pluto-default-theme.jsp") {
                                'portlet'(context: '/pluto', name: 'PlutoPageAdmin')
                            }

                        }
                    }
                }
                plutoConfigXml.write(xml.toString())
            }
        }

    } catch (Exception e) {
        event("StatusError", ["Unable to generate portlet.xml: " + e.message])
        exit(1)
    }

}

def hasProperty(propertyName, instance) {
    try {
        def value = instance."${propertyName}"
        return true;
    } catch (MissingPropertyException mpe) {
        return false;
    }
}

def checkRequiredProperties(propertyNames, instance) {
    propertyNames.each {
        if (!hasProperty(it, instance)) {
            throw new MissingPropertyException("${instance.class.name} does not have the required properties ${propertyNames}",
                    instance.class)
        }
    }
}

def resolveResources(String pattern) {
    def resolver = new PathMatchingResourcePatternResolver()
    return resolver.getResources(pattern)
}
