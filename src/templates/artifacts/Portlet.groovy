@artifact.package@

import javax.portlet.*

class @artifact.name@ {

  def title = 'Portlet Title'
  def description = '''
Description about the portlet goes here.
'''
  def displayName = 'Display Name'
  def supports = ['text/html':['view', 'edit', 'help']]

  // Liferay server specific configurations
  def liferay_display_category = 'MyCategory'

  def actionEdit = {
    //TODO Define action phase
    portletResponse.setPortletMode(PortletMode.VIEW) 
  }

  def renderEdit = {
    //TODO Define render phase. Return the map of the variables bound to the view
    ['mykey':'myvalue']
  }

  def actionView = {
    //TODO Define action phase
  }

  def renderView = {
    //TODO Define render phase. Return the map of the variables bound to the view
    ['mykey':'myvalue']
  }

  def actionHelp = {
    //TODO Define action phase
    portletResponse.setPortletMode(PortletMode.VIEW) 
  }

  def renderHelp = {
    //TODO Define render phase. Return the map of the variables bound to the view
    ['mykey':'myvalue']
  }
}
