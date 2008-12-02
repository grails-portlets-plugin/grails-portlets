//
// This script is executed by Grails after plugin was installed to project.
// This script is a Gant script so you can use all special variables provided
// by Gant (such as 'baseDir' which points on project base dir). You can
// use 'Ant' to access a global instance of AntBuilder
//
// For example you can create directory under project tree:
// Ant.mkdir(dir:"D:\work\portlets\grails-plugins/portlets/grails-app/jobs")
//

ant.property(environment: "env")
grailsHome = ant.antProject.properties."env.GRAILS_HOME"

