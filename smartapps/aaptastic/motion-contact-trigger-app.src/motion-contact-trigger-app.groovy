/**
 * Motion & Contact Trigger App - for use with my "Whole House Scene Creation and Control App".
 * Together, they create and control multiple Scenes (via Modes) - for Hues, Dimmers, and/or Switches
 *

 *  Version 1.0.0 (2015-1-7)
 *
 *  The latest version of this file can be found at:
 *  <TBD>
 *
 *  --------------------------------------------------------------------------
 *
 *  Copyright (c) 2015 Anthony Pastor
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
    name: "Motion & Contact Trigger App",
    namespace: "Aaptastic",
    author: "Anthony Pastor",
    description: "If my Selected (Virtual) Switch is True, then this SmartApp turns selected Dimming Lights (including Hues) ON to past level.  If no motion for X minutes, then turns selected Lights/Switches OFF -- Unless, the switch was already on prior to motion, in which case it stays on.  If the switch is toggled while the no-motion timer is running, the timer is canceled.",
    category: "Convenience",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png"
)

preferences {
	section("Select (Virtual) Presence Switch:"){
		input "someoneHome", "capability.switch", multiple: false, required: true
	}
    
    section("Select motion detector(s):"){
		input "motions", "capability.motionSensor", multiple: true, required: false
	}
	section("Select contact sensor(s):"){
		input "contacts", "capability.contactSensor", multiple: true, required: false
	}
	section("Select Lights..."){
		input "switches", "capability.switchLevel", multiple: true, title: "Select Lights", required: true
	}
	section("Turn off after no trigger for ___ Minutes..."){
		input "minutes1", "number", defaultValue: "5"
	}
}


def installed()
{
	state.someoneHome = false
	initialize()
	schedule("0 * * * * ?", "scheduleCheck")
}


def updated()
{
	unsubscribe()
	initialize()
    
    log.debug "state: " + state.myState
}

def initialize()
{

	subscribe(someoneHome, "switch", switchHome)
    if (motions) {
    	subscribe(motions, "motion", motionHandler)
    }
    if (contacts) {
    	subscribe(contacts, "contact", contactHandler)
    }
    subscribe(switches, "level", switchChange)
	state.myState = "ready"
    checkHome()
    subscribe(location, modeChangeHandler)
}


def switchHome(evt) {
	log.debug "switchHome: $evt.name: $evt.value"
    if (evt.value == "on") {
		state.someoneHome = true
    } else {
    	state.someoneHome = false
    }
}


def checkHome() {

   	if (someoneHome.currentValue("switch") == "on") {
   		log.debug "checkHome: state someoneHome is now ON." 
        state.someoneHome = true
    } else {
   		log.debug "checkHome: state someoneHome is now OFF." 
        state.someoneHome = false
    }
}      



def switchChange(evt) {
	log.debug "SwitchChange: $evt.name: $evt.value"
	state.changeValue = evt.value as Integer

	if (state.someoneHome) {
		log.debug "SwitchChange: Someone's home, so run..."	
	    if(state.changeValue > 0 ) {
        // Slight change of Race condition between motion or contact turning the switch on,
        // versus user turning the switch on. Since we can't pass event parameters :-(, we rely
        // on the state and hope the best.
    	    if(state.myState == "activating") {
            // OK, probably an event from Activating something, and not the switch itself. Go to Active mode.
        	    setActiveAndSchedule()
	        } else if(state.myState != "active") {
    			state.myState = "already on"
                unschedule()
				schedule("0 0 * * * ?", "scheduleCheck")
        	}
           
	    } else {
    	// If active and switch is turned off manually, then stop the schedule and go to ready state
    		if(state.myState == "active" || state.myState == "activating") {
    			unschedule()
	        }
  			state.myState = "ready"
	    }
    	log.debug "state: " + state.myState
	} else {
		log.debug "SwitchChange: No one's home, so don't run..."	
	}
}

def contactHandler(evt) {
	log.debug "contactHandler: $evt.name: $evt.value"

	if (state.someoneHome) {
		log.debug "contactHandler: Someone's home, so run..."	
	    if (evt.value == "open") {
    	    if(state.myState == "ready") {
        	    

				def myLevel = null
	            switches.each {
                   	if (it.currentValue("switch") == "off") {
			        	myLevel = it.currentValue("level")

    	    		    if (myLevel > 0) {
	                        log.debug "Turning on lights by CONTACT."
        	    			it.setLevel(myLevel)
            			}
	  				}     
            	}
    	        state.inactiveAt = null
        	    state.myState = "activating"
	        }
    	} else if (evt.value == "closed") {
        	if (!state.inactiveAt && state.myState == "active" || state.myState == "activating") {
			// When contact closes, we reset the timer if not already set
            	setActiveAndSchedule()
	        }
    	}
	    log.debug "state: " + state.myState
	} else {
		log.debug "contactHandler: No one's home, so don't run..."	
	}
}

def motionHandler(evt) {
	log.debug "motionHandler: $evt.name: $evt.value"

	if (state.someoneHome) {
		log.debug "motionHandler: Someone's home, so run..."	

	    if (evt.value == "active") {
    	    if(state.myState == "ready" || state.myState == "active" || state.myState == "activating" ) {
        	    			
            	def myLevel = null
	            switches.each {
                	if (it.currentValue("switch") == "off") {
			        	myLevel = it.currentValue("level")

        			    if (myLevel > 0) {
	                        log.debug "Turning on lights from MOTION."
            				it.setLevel(myLevel)
	            		}
	  				}
        	    }
                
            	state.inactiveAt = null
	            state.myState = "activating"
    	    }
	    } else if (evt.value == "inactive") {
    	    if (!state.inactiveAt && state.myState == "active" || state.myState == "activating") {
			// When Motion ends, we reset the timer if not already set
        	   setActiveAndSchedule()
	        }
    	}
	    log.debug "state: " + state.myState
	} else {
		log.debug "motionHandler: No one's home, so don't run..."	
	}
}


def setActiveAndSchedule() {
    unschedule()
 	state.myState = "active"
    state.inactiveAt = now()
	schedule("0 * * * * ?", "scheduleCheck")
}

def scheduleCheck() {
	log.debug "schedule check, ts = ${state.inactiveAt}"
    if(state.myState != "already on") {
    	if(state.inactiveAt != null) {
	        def elapsed = now() - state.inactiveAt
            log.debug "${elapsed / 1000} sec since motion stopped"
	        def threshold = 1000 * 60 * minutes1
	        if (elapsed >= threshold) {
	            if (state.myState == "active") {
	                log.debug "Turning off lights"
	                switches.each {
                    	it.off()
                    }    
	            }
	            state.inactiveAt = null
	            state.myState = "ready"
	        }
    	}
    }
    log.debug "state: " + state.myState
}