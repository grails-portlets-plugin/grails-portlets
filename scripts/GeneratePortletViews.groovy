includeTargets << grailsScript("Init")
includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << grailsScript("_GrailsGenerate")

target ('main': "Generates the portal views for a specified portal class") {
	depends( checkVersion, parseArguments, packageApp )
    promptForName(type: "Portlet Class")
    portletClass = argsMap["params"][0].toLowerCase()
    def viewsDir = "${basedir}/grails-app/views/${portletClass}"
    ant.mkdir(dir:viewsDir)
    ant.copy(todir:viewsDir, overwrite:false) {
        fileset(dir:"${portletsPluginDir}/src/templates/portlet-views")
    }
}

setDefaultTarget(main)



