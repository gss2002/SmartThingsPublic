/**
 *  HtdAudio
 *
 *  Copyright 2018 Gregory Senia
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
preferences {
	input("destIp", "text", title: "IP", description: "The device IP")
    input("destPort", "number", title: "Port", description: "The port you wish to connect", defaultValue: 80)
}
 

metadata {
	definition (name: "HtdAudio", namespace: "gss2002", 
    	author: "gsenia") {
        capability "Music Player"
        capability "Actuator"
        capability "Switch" 
        capability "Polling"
        capability "Switch Level"
        
        attribute "mute", "string"
        attribute "input", "string"
        attribute "inputChan", "enum"
        
        command "mute"
        command "unmute"
        command "inputSelect", ["string"] //define that inputSelect takes a string of the input name as a parameter
        command "inputNext"
        command "toggleMute"
   		 command "source0"
   		 command "source1"
   		 command "source2"
  		  command "source3"
   		 command "source4"
   		 command "source5"
	    command "partyModeOn"
    	command "partyModeOff"
   		 command "zone"        
        
      	}

	simulator {
		// TODO-: define status and reply messages here
	}

	tiles {
		standardTile("switch", "device.switch", width: 2, height: 2, canChangeIcon: false, canChangeBackground: true) {
            state "on", label: '${name}', action:"switch.off", backgroundColor: "#79b821", icon:"st.Electronics.electronics16"
            state "off", label: '${name}', action:"switch.on", backgroundColor: "#ffffff", icon:"st.Electronics.electronics16"
        }
		standardTile("poll", "device.poll", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "poll", label: "", action: "polling.poll", icon: "st.secondary.refresh", backgroundColor: "#FFFFFF"
		}
        standardTile("input", "device.input", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
			state "input", label: '${currentValue}', action: "inputNext", icon: "", backgroundColor: "#FFFFFF"
		}
        standardTile("mute", "device.mute", width: 1, height: 1, canChangeIcon: false, inactiveLabel: true, canChangeBackground: false) {
            state "muted", label: '${name}', action:"unmute", backgroundColor: "#79b821", icon:"st.Electronics.electronics13"
            state "unmuted", label: '${name}', action:"mute", backgroundColor: "#ffffff", icon:"st.Electronics.electronics13"
		}
        controlTile("level", "device.level", "slider", height: 1, width: 2, inactiveLabel: false, range: "(0..100)") {
			state "level", label: '${name}', action:"setLevel"
		}
        
		main "switch"
        details(["switch","input","mute","level","poll"])
	}
}

// parse events into attributes
def parse(String description) {
	log.debug "Parsing '${description}'"
	// TODO: handle 'activities' attribute
	// TODO: handle 'currentActivity' attribute

}

// handle commands
def startActivity() {
	log.debug "Executing 'startActivity'"
	// TODO: handle 'startActivity' command
}

def on() {
	sendEvent(name: "switch", value: 'on')
    log.info "Turning on HTD Zone"
	request('/htd/ZonePower?zone=2&state=on')
}

def off() { 
	sendEvent(name: "switch", value: 'off')
    log.info "Turning off HTD Zone"
	request('/htd/ZonePower?zone=2&state=off')
}


def request(path) { 
    log.debug "Sending Request to HTD Zone"
    def hosthex = convertIPtoHex(parent.getIpAddress())
    def porthex = convertPortToHex(parent.getTcpPort())
    device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
   	 		'method': 'GET',
    		'path': path,
        	'headers': [ HOST: "$destIp:$destPort" ]
		) 
              
    hubAction
}


private String convertIPtoHex(ipAddress) { 
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02X', it.toInteger() ) }.join()
    return hex
}

private String convertPortToHex(port) {
	String hexport = port.toString().format( '%04X', port.toInteger() )
    return hexport
}