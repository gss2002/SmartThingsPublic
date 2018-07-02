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
 
definition(
        name: "HTD Audio",
        namespace: "gss2002",
        author: "Greg Senia",
        description: "HTD Lync6/Lync12 via (W)GW-SL1 Smart Gateway",
        category: "My Apps",
        iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
        iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
        iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
       	singleInstance: true,
)

preferences {
    page(name: "config", title: "HTD GW-SL1 Config", uninstall: true, nextPage: "active_page") {
        section() {
            input("ipAddress", "text", multiple: false, required: true, title: "IP Address:", defaultValue: "172.16.1.133")
            input("tcpPort", "integer", multiple: false, required: true, title: "TCP Port:", defaultValue: 10006)
            input("HTDtype", "enum", multiple: false, required: true, title: "HTD Controller:", options: ['Lync6', 'Lync12'])
            input("theHub", "hub", multiple: false, required: true, title: "Pair with SmartThings Hub:")
        }
    }
    page(name: "active_page", title: "Select Active Zones and Sources", nextPage: "naming_page")
    page(name: "naming_page", title: "Name Zones and Sources", install: true)
}

def active_page() {
	dynamicPage(name: "active_page") {
        section("Which zones are available?") {
            input("active_zones", "enum", multiple: true, title: "Active Zones", options: controllerZonesIn(HTDtype))
        }
        section("Which input sources are available?") {
            input("active_sources", "enum", multiple: true, title: "Active Sources:", options: controllerSourcesIn(HTDtype))
        }
    }
}

def naming_page() {
    dynamicPage(name: "naming_page") {
        section("Name your zones:") {
        	def zones = convertStringList(active_zones)
        	log.debug("My active zones: ${zones.join(',')}")
            for (int i = 1; i <= controllerZones(HTDtype); i++) {
            	if (i in zones) {
                    log.debug("creating active zone: ${i}")
                    input("zone_name.${i}", "text", multiple: false, required: true, title: "Zone ${i}:", defaultValue: "Zone ${i}")
                } else {
                	log.debug("Skipping zone ${i}")
                }
            }
        }
        section("Name your input sources:") {
        	def sources = convertStringList(active_sources)
        	log.debug("My active sources: ${sources.join(',')}")
            for (int i = 1; i <= controllerSources(HTDtype); i++) {
            	if (i in sources) {
                    input "source_name.${i}", "text", multiple: false, required: true, title: "Source ${i}:", defaultValue: "Source ${i}"
                }
            }
        }
    }
}
// Takes a list of Strings, and converts the list to Integers

private convertStringList(list) {
	return list.collect { it.toInteger() }
}

private int controllerZones(controller) {
	switch(controller) {
   		case "Lync6":
        	return 6
        case "Lync12":
        	return 12
        default:
        	return 0
    }
}


private int controllerSources(controller) {
	switch(controller) {
        case "Lync6":
        	return 12
        case "Lync12":
        	return 18
        default:
        	return 0
    }

}


// How many sources does our controller support?

private controllerSourcesIn(controller) {
    def ret = []
    def list = 1..controllerSources(controller)
    list.each { n ->
   		ret.add(n)
    }
    return ret
}

// How many zones does our controller support?
private controllerZonesIn(controller) {
    def ret = []
    def list = 1..controllerZones(controller)
    list.each { n ->
   		ret.add(n)
    }
    return ret
}


def initialize() {
    // nothing is muted by default
   /*
    state.zone_mute = [:]
    for (int i = 0; i <= controllerZones(HTDtype); i++) {
    	state.zone_mute[i] = false
    }
    */
    // all zones default to source = 1.  Hopefully the user enabled it :)
   /*
    state.zone_source = [:]
   	for (int i = 0; i <= controllerSources(HTDtype); i++) {
    	state.zone_source[i] = 1
    }
    */
    // remember our active available zones & sources
    state.available_zones = convertStringList(active_zones)
    state.available_sources = convertStringList(active_sources)
   	state.zone_names = []
    state.source_names = []
    state.htd_controller = HTDtype

    def porthex = convertPortToHex(tcpPort)
    def iphex = convertIPtoHex(ipAddress)
    def dni_base = "${iphex}:${porthex}"

    // remember the zone & source names
    for (entry in settings) {
    	log.debug("setting entry: ${entry.getKey()} = ${entry.getValue()}")
    	if (entry.getKey().startsWith("zone_name.")) {
       		def kvpair = entry.getKey().split(/\./)
            log.debug("${HTDtype} zone ${kvpair[1]}: ${entry.getValue()}")
            state.zone_names.putAt(kvpair[1].toInteger(), entry.getValue())
        } else if (entry.getKey().startsWith("source_name.")) {
       		def kvpair = entry.getKey().split(/\./)
            log.debug("${HTDtype} source ${kvpair[1]}: ${entry.getValue()}")
            state.source_names.putAt(kvpair[1].toInteger(), entry.getValue())
        }
    }

    // create the zones
    for (int i = 1; i <= controllerZones(HTDtype); i ++) {
    	if (i in state.available_zones) {
        	def dni = "${dni_base}:${i}"
            def existingDevice = getChildDevice(dni)
            if (!existingDevice) {
            	// Add any enabled zones
                def dev = addChildDevice("gss2002", "Htd Zone", dni, theHub.id,
                	[label: "${state.zone_names[i]}", name: "Htd Zone.${dni}", zoneNo: "${i}"])
                log.debug "created ${dev.displayName} with ${dev.deviceNetworkId}"

            } else {
            	// delete zone if disabled
            	log.debug "Deleting disabled zone: ${dni}"
                log.debug "${existingDevice.label} label"
                existingDevice.updateLabel("${state.zone_names[i]}")

                //deleteChildDevice(dni)
			}
		}
	}
/*
	// remove zones which were disabled
    	def delete = getChildDevices().findAll { !settings.devices.contains(it.deviceNetworkId) }
    	delete.each {
    	   log.debug "{it.deviceNetworkId}"
     	   deleteChildDevice(it.deviceNetworkId)
   	 }
     */
}

def unsubscribe() {
}

def uninstalled() {
    getAllChildDevices().each {
        log.debug "deleting child device: ${it.displayName} = ${it.id}"
        deleteChildDevice(it.deviceNetworkId)
    }
}

private Integer convertHexToInt(hex) {
    Integer.parseInt(hex,16)
}

private String convertHexToIP(hex) {
    [convertHexToInt(hex[0..1]),convertHexToInt(hex[2..3]),convertHexToInt(hex[4..5]),convertHexToInt(hex[6..7])].join(".")
}



private String convertIPtoHex(ipAddress) {
    String hex = ipAddress.tokenize( '.' ).collect {  String.format( '%02x', it.toInteger() ) }.join()
    log.debug "IP address ${ipAddress} is converted to ${hex}"
    return hex
}

private String convertPortToHex(port) {
    String hexport = port.toString().format('%04x', port.toInteger())
    log.debug "Port ${port} is converted to $hexport"
    return hexport
}


// get the zone_id for a child device
private int get_zone_id(child) {
    def values = child.id.split(':')
    return values[2]
}

def installed() {
    log.debug "Installed with settings: ${settings}"
    initialize()
}

def updated() {
    log.debug "Updated with settings: ${settings}"
  //  unsubscribe()
    initialize()
}

private request(path) { 
    log.debug "Sending Request to HTD Zone"
    log.debug "IP Address: ${ipAddress}"
    log.debug "TCP Port: ${tcpPort}"
    log.debug "Request Path: ${path}"

    def hosthex = convertIPtoHex(ipAddress)
    def porthex = convertPortToHex(tcpPort)
    //device.deviceNetworkId = "$hosthex:$porthex" 

    def hubAction = new physicalgraph.device.HubAction(
   	 		'method': 'GET',
    		'path': path,
        	'headers': [ HOST: "$ipAddress:$tcpPort" ]
		) 
              
    hubAction
}


