/**
 * AAP's Scene Creation - Hues, Dimmers, and/or Switches
 *
 *
 * Most of the logic used in this App was taken from "Dim & Dimmer" by geko@statusbits.com 
 * I just modified it to add Scene Control of Hue lights and, if Hues are selected, to enable selection of their color(s).
 *
 *
 *  Version 1.1.0 (2015-2-11)
 *
 *  The latest version of this file can be found at:
 *  
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2014 Anthony Pastor
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation, either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

definition(
    name: "AAP's Scene Creation - Hues, Dimmers, and/or Switches",
    namespace: "Aaptastic",
    author: "Anthony Pastor",
    description: "Create lighting scenes with Hues, Dimmers, and/or Switches.",
    category: "Convenience",
	iconUrl: "https://dl.dropboxusercontent.com/u/2403292/LR%20Scene.png",
    iconX2Url: "https://dl.dropboxusercontent.com/u/2403292/LR%20Scene%20large.png",
    oauth: true
)

preferences {
    page name:"pageSetup"
    page name:"pageAbout"
    page name:"pageConfigure"
}

// Show "Setup Menu" page
private def pageSetup() {
    TRACE("pageSetup()")

    if (state.installed == null) {
        // First run - initialize state
        state.installed = false
        return pageAbout()
    }

    def inputHues = [
        name        : "hues",
        type        : "capability.colorControl",
        title       : "Select Hues",
        multiple:   true,
        required:   false
    ]

    def inputDimmers = [
        name        : "dimmers",
        type        : "capability.switchLevel",
        title       : "Select Dimmers",
        multiple:   true,
        required:   false
    ]

    def inputSwitches = [
        name        : "switches",
        type        : "capability.switch",
        title       : "Select Switches",
        multiple:   true,
        required:   false
    ]
    
    def inputMonitor = [
        name        : "monitorThese",
        type        : "capability.switchLevel",
        title       : "Select Hues / Dimmers to monitor for physical override.",
        multiple:   true,
        required:   false
    ]

    def pageProperties = [
        name        : "pageSetup",
        title       : "Setup Menu",
        nextPage    : "pageConfigure",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            input inputHues
            input inputDimmers
            input inputSwitches
            input inputMonitor
            
            href "pageAbout", title:"About", description:"Tap to open"
        }
        section([title:"Options", mobileOnly:true]) {
            label title:"Assign a name", required:false
        }
    }
}

// Show "About" page
private def pageAbout() {
    TRACE("pageAbout()")

    def textAbout =
        "This smart app allows you to create lighting scenes by setting " +
        "Hues, Dimmers, and/or Switches to different levels depending on the home mode."

    def pageProperties = [
        name        : "pageAbout",
        title       : "About",
        nextPage    : "pageSetup",
        install     : false,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
            paragraph "${textVersion()}\n${textCopyright()}"
        }
        section("License") {
            paragraph textLicense()
        }
    }
}

// Show "Configure Hues, Dimmers and Switches" setup page
private def pageConfigure() {
    TRACE("pageConfigure()")

    def textAbout =
        "Set desired dimming levels for each home mode. Dimming values " +
        "are between 0 (off) and 98 (almost full brightness). If left blank, the " +
        "dimming level will not change when switching to this mode."

    def pageProperties = [
        name        : "pageConfigure",
        title       : "Configure Hues, Dimmers, and Switches",
        nextPage    : null,
        install     : true,
        uninstall   : state.installed
    ]

    return dynamicPage(pageProperties) {
        section {
            paragraph textAbout
        }

        location.modes?.each() {
            def name = it as String
            section("${name} Mode", hideable:true, hidden:false) {
                name = name.tr(' !+', '___')
                settings.hues?.each() {
                    input "${it.id}_${name}", "number", title: "${it.displayName} Level", required:false
                    input "${it.id}_${name}_color", "enum", title: "${it.displayName} Color?", required: false, multiple:false, metadata: [values:
					["Normal", "Daylight", "Soft", "Warm", "Red", "Green", "Blue", "Yellow", "Orange", "Purple", "Pink"]]
                }
                settings.dimmers?.each() {
                    input "${it.id}_${name}", "number", title: "${it.displayName} Level", required:false
                }
                settings.switches?.each() {
                    input "${it.id}_${name}", "enum", title:it.displayName,
                        metadata:[values: ["on", "off"]], required:false
                }
            }
        }
    }
}

def installed() {
    TRACE("installed()")

    state.installed = true
    initialize()
    
	
}

def updated() {
    TRACE("updated()")

    unsubscribe()
    initialize()
}

// Handle location event.
def onLocation(evt) {
    TRACE("onLocation(${evt.value})")

	state.currentMode = evt.value
    String mode = state.currentMode

	if (state.lastMode != state.currentMode) {
    
	    def allSwitches = []
    	if (settings.hues)
        	allSwitches.addAll(settings.hues)
		if (settings.dimmers)
    	    allSwitches.addAll(settings.dimmers)
	    if (settings.switches)
    	    allSwitches.addAll(settings.switches)

		def counter = 0
	    allSwitches.each() {
        	counter = counter + 1
            if ( counter > 14 ) {
            
            	delay ( 5000 )
            	counter = 0
            
            } else {
            
	    	    def nameColor = "${it.id}_${mode.tr(' !+', '___')}_color"
    	    	TRACE("name: ${nameColor}")
	    	    def valueColor = settings[nameColor]
    	    	TRACE("value: ${valueColor}")
	        	if (valueColor != null) {
					if (valueColor == "Blue") {
        	    		it.setColor([hue: 70, saturation:100])
	        	    } else if (valueColor == "Normal") {
    	        		it.setColor([hue: 52, saturation:19])
	        	    } else if (valueColor == "Daylight") {
    	        		it.setColor([hue: 53, saturation:91])
	    	        } else if (valueColor == "Soft") {
    	    	    	it.setColor([hue: 23, saturation:56])
        	    	} else if (valueColor == "Warm") {
	            		it.setColor([hue: 20, saturation:80])
		            } else if (valueColor == "Red") {
    		        	it.setColor([hue: 100, saturation:100])
        		    } else if (valueColor == "Green") {
            			it.setColor([hue: 39, saturation:100])
		            } else if (valueColor == "Yellow") {
    		        	it.setColor([hue: 25, saturation:100])
        		    } else if (valueColor == "Orange") {
            			it.setColor([hue: 10, saturation:100])
	            	} else if (valueColor == "Pink") {
   	        			it.setColor([hue: 83, saturation:100])
	        	    } else if (valueColor == "Purple") {
    	        		it.setColor([hue: 75, saturation:100])
	    	        }      
            	            
	    	    }    
        
		        def name = "${it.id}_${mode.tr(' !+', '___')}"
    		    TRACE("name: ${name}")
        		def value = settings[name]
		        TRACE("value: ${value}")
    		    if (value != null) {
        		    if (value == 'on') {
            		    TRACE("Turning '${it.displayName}' on")
                		it.on()
		            } else if (value == 'off') {
    		            TRACE("Turning '${it.displayName}' off")
        		        it.off()
   	
    	        	} else {
        	        	value = value.toInteger()
	            	    if (value > 99) value = 98
    	            	TRACE("Setting '${it.displayName}' level to ${value}")
	    	            it.setLevel(value)
    	    	    }
	        	}	
		    }
            
		}
	}
    
    state.lastMode = state.currentMode

}

def newLevelCheck(evt) {
	log.info "newLevel: $evt.name: $evt.value"
	state.changeValue = evt.value as Integer

	if(state.changeValue > 98 ) {
    	log.info "newLevelCheck: Found a level > 98 - must have been a user action at physical light switch, SO...."
        log.info "newLevelCheck: ...calling resetLevel."
	
    unschedule
   	resetLevel()
    }
}

def resetLevel() {

        String mode = state.currentMode
        
	    if (settings.hues) {
	        
            
    	    hues.each() {
       			def nameColor = "${it.id}_${mode.tr(' !+', '___')}_color"
	        	TRACE("name: ${nameColor}")
	    	    def valueColor = settings[nameColor]
    	    	TRACE("value: ${valueColor}")
	    	    if (valueColor != null) {
					if (valueColor == "Blue") {
        	    		it.setColor([hue: 70, saturation:100])
	            	} else if (valueColor == "Normal") {
    	        		it.setColor([hue: 52, saturation:19])
	    	        } else if (valueColor == "Daylight") {
    	    	    	it.setColor([hue: 53, saturation:91])
        	    	} else if (valueColor == "Soft") {
            			it.setColor([hue: 23, saturation:56])
		            } else if (valueColor == "Warm") {
    		        	it.setColor([hue: 20, saturation:80])
        		    } else if (valueColor == "Red") {
            			it.setColor([hue: 100, saturation:100])
	            	} else if (valueColor == "Green") {
    	        		it.setColor([hue: 39, saturation:100])
	        	    } else if (valueColor == "Yellow") {
    	        		it.setColor([hue: 25, saturation:100])
	    	        } else if (valueColor == "Orange") {
    	    	    	it.setColor([hue: 10, saturation:100])
        	    	} else if (valueColor == "Pink") {
            			it.setColor([hue: 83, saturation:100])
		            } else if (valueColor == "Purple") {
    		        	it.setColor([hue: 75, saturation:100])
        		    }      
        		}                
	        }
            
        }
   		
        if (settings.dimmers) {

			dimmers.each() {			    

				def name = "${it.id}_${mode.tr(' !+', '___')}"
	    	    TRACE("name: ${name}")
	        	def value = settings[name]
	    	    TRACE("value: ${value}")
    	    	
                if (value != null) {
	    	        value = value.toInteger()
    	    	    if (value > 99) value = 98 {
	    	    	    TRACE("Setting '${it.displayName}' level to ${value}")
    	    	    	it.setLevel(value)
        			}
        		}
			}
		}    
	runIn (200, "pollMonitored") 	    
}
 
 
def pollMonitored() {

	log.debug "pollMonitored: Polling the Monitored lights"
    unschedule 
    
	monitorThese.each() {
    	def currentLevel = it.currentValue("level") as Integer
        if (currentValue != null) {
    	    if ( currentLevel > 98 ) {    

				log.info "pollMonitored: ${it.label} = ${currentValue} - must have been a user action at physical light switch, SO...."
        		log.info "pollMonitored: ... calling resetLevel."
	            resetLevel()
		
    		}
	    }
	}
}

private def initialize() {
    log.trace "${app.name}. ${textVersion()}. ${textCopyright()}"

    subscribe(location, onLocation)
    subscribe(monitorThese, "level", newLevelCheck)

	runIn (150, "pollMonitored") 
    
    STATE()
}


private def textVersion() {
    def text = "Version 1.1.0"
}

private def textCopyright() {
    def text = "Copyright (c) 2014 AAP"
}

private def textLicense() {
    def text =
        "This program is free software: you can redistribute it and/or " +
        "modify it under the terms of the GNU General Public License as " +
        "published by the Free Software Foundation, either version 3 of " +
        "the License, or (at your option) any later version.\n\n" +
        "This program is distributed in the hope that it will be useful, " +
        "but WITHOUT ANY WARRANTY; without even the implied warranty of " +
        "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU " +
        "General Public License for more details.\n\n" +
        "You should have received a copy of the GNU General Public License " +
        "along with this program. If not, see <http://www.gnu.org/licenses/>."
}

private def TRACE(message) {
    log.debug message
}

private def STATE() {
    log.trace "settings: ${settings}"
    log.trace "state: ${state}"
}