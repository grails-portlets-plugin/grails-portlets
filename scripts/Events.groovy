import grails.util.GrailsUtil
import groovy.xml.MarkupBuilder
import org.mortbay.jetty.Server
import org.mortbay.jetty.security.HashUserRealm
import org.mortbay.jetty.security.UserRealm
import org.mortbay.jetty.webapp.WebAppContext
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

def version = 0.1
def basedir = System.getProperty("base.dir")
def portletXml = new File("${basedir}/web-app/web-inf/portlet.xml")
def pluginLibDir = "${basedir}/plugins/portlets-${version}/lib"
if (!new File(pluginLibDir).exists()) {
    //we must not be installed...
    pluginLibDir = "${basedir}/lib"
    if (!new File(pluginLibDir).exists()) {
        throw new RuntimeException('Unable to find Portlets plugin lib folder')
    }
}

//config.portlet.version = '2'

def portletVersion = '1.0'
def plutoVersion = '1.1.4'

if (config.portlet.version == '2') {
    portletVersion = '2.0'
    plutoVersion = '2.0.0-SNAPSHOT'
}

def underscoredVersion = portletVersion.replaceAll("\\.", "_")

def plutoConfigXml = new File("${pluginLibDir}/pluto-portal-${plutoVersion}/web-inf/pluto-portal-driver-config.xml")
def confClassList = ["org.mortbay.jetty.webapp.WebInfConfiguration",
        "org.mortbay.jetty.plus.webapp.EnvConfiguration",
        "org.mortbay.jetty.plus.webapp.Configuration",
        "org.mortbay.jetty.webapp.JettyWebXmlConfiguration",
        "org.mortbay.jetty.webapp.TagLibConfiguration"]

eventConfigureJetty = {Server server ->
    def grailsContext = server.getHandler()
    //grailsContext.systemClasses = ["org.apache.pluto.", "org.castor.", "javax.portlet.", "org.springframework."]

    def webContext = new WebAppContext("${pluginLibDir}/pluto-portal-${plutoVersion}", "pluto")
   // webContext.serverClasses = ["org.springframework."]
    webContext.systemClasses = ["-org.apache.pluto.driver.","org.apache.pluto.", "javax.portlet.", "javax.servlet.","org.springframework."]
    webContext.contextPath = "/pluto"
    server.addHandler(webContext)

    HashUserRealm myrealm = new HashUserRealm("default", "${pluginLibDir}/realm.properties");
    server.setUserRealms([myrealm] as UserRealm[]);
}

eventSetClasspath = {rootLoader ->
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
    try {
        def sw = new StringWriter();
        def xmlWriter = new MarkupBuilder(sw);
        def searchPath = "file:${basedir}/grails-app/controllers/**/*Portlet.groovy"
        event("StatusUpdate", ["Searching for portlets: ${searchPath}"])
        portletFiles = resolveResources(searchPath).toList()
        if (portletFiles.size() > 0) {
            event("StatusUpdate", ["Generating portlet.xml - ${portletFiles.size()} portlets found"])

            if (portletXml.exists()) portletXml.delete()
            xmlWriter.'portlet-app'(version: portletVersion,
                    xmlns: "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd",
                    'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
                    'xsi:schemaLocation': "http://java.sun.com/xml/ns/portlet/portlet-app_${underscoredVersion}.xsd") {
                portletFiles.each {portletClassFile ->
                    def className = portletClassFile.filename - '.groovy'
                    Class portletClass = classLoader.loadClass(className)
                    def portletName = className - 'Portlet'
                    def instance = portletClass.newInstance()
                    checkRequiredProperties(['supports', 'title'], instance)
                    //TODO security role refs, security constraints, user attributes, il8n
                    xmlWriter.portlet {
                        'portlet-name'(portletName)
                        if (hasProperty('displayName', instance))
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
                        'supports'
                        {
                            'mime-type'('*/*')
                            instance.supports.each {mode ->
                                'portlet-mode'(mode)
                            }
                        }
                        'portlet-info'
                        {
                            'title'(instance.title)
                            if (hasProperty('shortTitle', instance)) 'short-title'(instance.shortTitle)
                            if (hasProperty('keywords', instance)) 'keywords'(instance.keywords)
                        }
                        if (hasProperty('preferences', instance) && instance.preferences instanceof Map) {
                            'portlet-preferences'
                            {
                                instance.preferences.each {prefName, prefValue ->
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
            }
            portletXml.write(sw.toString())
            if (GrailsUtil.environment == 'development' || GrailsUtil.environment == 'test') {
                sw = new StringWriter()
                xmlWriter = new MarkupBuilder(sw)
                if (plutoConfigXml.exists()) plutoConfigXml.delete()
                xmlWriter.'pluto-portal-driver'(
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
                plutoConfigXml.write(sw.toString())
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
    //try {
    def resolver = new PathMatchingResourcePatternResolver()
    return resolver.getResources(pattern)
    /*}
    catch (Throwable e) {
        e.print
        return []
    } */
}
