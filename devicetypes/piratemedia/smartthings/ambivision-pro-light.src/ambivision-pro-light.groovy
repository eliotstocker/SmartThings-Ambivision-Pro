/**
 *  AmbiVision Pro Light
 *
 *  Copyright 2018 Eliot Stocker
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
metadata {
	definition (name: "AmbiVision Pro Light", namespace: "piratemedia/smartthings", author: "Eliot Stocker") {
    // Hard Capabilities
      capability "Light"
      capability "Switch"
      capability "Switch Level"
      capability "Color Control"

      // Soft Capabilities
      capability "Actuator"
      capability "Configuration"
      capability "Refresh"
      capability "Polling"
      capability "Health Check"
      
      command "setCaptureMode"
      command "setAudioMode"
      command "setMoodMode"
      
      attribute "mode", "string"
      attribute "submode", "string"
      attribute "LightMode", "string"
	}
    
    preferences {
		input("DeviceIP", "string", title:"Device IP Address", description: "Please enter your device's IP Address", required: true, displayDuringSetup: true)
	}
    
    // UI tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name:"switch", type: "lighting", width: 6, height: 4, canChangeIcon: true){
            tileAttribute ("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "on", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "off", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
                attributeState "turningOn", label:'${name}', action:"switch.off", icon:"st.lights.philips.hue-single", backgroundColor:"#00A0DC", nextState:"turningOff"
                attributeState "turningOff", label:'${name}', action:"switch.on", icon:"st.lights.philips.hue-single", backgroundColor:"#ffffff", nextState:"turningOn"
            }
            tileAttribute ("device.level", key: "SLIDER_CONTROL") {
                attributeState "level", action:"switch level.setLevel"
            }
            tileAttribute ("device.color", key: "COLOR_CONTROL") {
                attributeState "color", action:"color control.setColor"
            }
            tileAttribute ("LightMode", key: "SECONDARY_CONTROL") {
            	attributeState "mode", label:'Mode: ${currentValue}', icon: "st.Appliances.appliances17"
            }
        }
        standardTile("refresh", "device.refresh", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"", action:"refresh.refresh", icon:"st.secondary.refresh"
        }
        
        standardTile("capture", "device.capture", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Capture", action:"setCaptureMode", icon:"st.Entertainment.entertainment13"
        }
        
        standardTile("audio", "device.audio", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Audio", action:"setAudioMode", icon:"st.Entertainment.entertainment15"
        }
        
        standardTile("mood", "device.party", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label:"Party", action:"setMoodMode", icon:"st.Food & Dining.dining6"
        }

        main(["switch"])
        details(["switch", "capture", "audio", "mood", "refresh"])
    }
}

def logDebug(msg) {
	log.debug msg
}

def logTrace(msg) {
	log.trace msg
}

def updateColor(rgb) {
  rgb.red = Math.round(rgb.red * 255).intValue()
  rgb.green = Math.round(rgb.green * 255).intValue()
  rgb.blue = Math.round(rgb.blue * 255).intValue()
  
  def color = colorUtil.rgbToHex(rgb.red, rgb.green, rgb.blue)
  apiCall('/color/' + color.substring(1))
  
  sendEvent(name: "mode", value: "mood", displayed: false)
  sendEvent(name: "submode", value: "manual", displayed: false)
  sendEvent(name: "LightMode", value: "Manual", descriptionText: "Mode set to: Manual")
  setColorValues(rgb, color)
}

def setColorValues(rgb, color) {
  def events = []
  
  def hsv = colorRgb2Hsv(rgb.red, rgb.green, rgb.blue)
  hsv.hue = Math.round(hsv.hue * 100).intValue()
  hsv.saturation = Math.round(hsv.saturation * 100).intValue()
  hsv.level = Math.round(hsv.level * 100).intValue()
  
  events += createEvent(name: "color", value: color, data: [ hue: hsv.hue, saturation: hsv.saturation, red: rgb.red, green: rgb.green, blue: rgb.blue, hex: color], displayed: false)
  events += createEvent(name: "hue", value: hsv.hue, displayed: false)
  events += createEvent(name: "saturation", value: hsv.saturation, displayed: false)
  events
}

def off() {
  apiCall("/off")
  sendEvent(name: "mode", value: "off", displayed: false)
  sendEvent(name: "submode", value: null, displayed: false)
  sendEvent(name: "LightMode", value: "Off", descriptionText: "Mode set to: Off")
  sendEvent(name: "switch", value: "off")
}

def on() {
  apiCall("/color/ffffff")
  sendEvent(name: "mode", value: "mood", displayed: false)
  sendEvent(name: "submode", value: "manual", displayed: false)
  sendEvent(name: "LightMode", value: "Manual", descriptionText: "Mode set to: Manual")
  sendEvent(name: "switch", value: "on")
}

def setLevel(value) {
  apiCall("/brightness/" + value)
  sendEvent(name: "level", value: value)
}

def setColor(value) {
  def rgb = colorHsv2Rgb(value.hue / 100, value.saturation / 100)

  updateColor(rgb)
  sendEvent(name: "switch", value: "on")
}

def setHue(hue) {
  setColor([ hue: hue, saturation: device.currentValue("saturation") ])
}

def setSaturation(saturation) {
  setColor([ hue: device.currentValue("hue"), saturation: saturation ])
}

def setAudioMode() {
  if(device.currentValue("mode") == 'audio') {
    switch(device.currentValue("submode")) {
        case 'level':
            sendEvent(name: "submode", value: "mixed", displayed: false)
            sendEvent(name: "LightMode", value: "Audio | Mixed Bins", descriptionText: "Mode set to: Audio | Mixed Bins")
            break
        case 'mixed':
            sendEvent(name: "submode", value: "lamp", displayed: false)
            sendEvent(name: "LightMode", value: "Audio | Lamp", descriptionText: "Mode set to: Audio | Lamp")
            break
        case 'lamp':
            sendEvent(name: "submode", value: "strobe", displayed: false)
            sendEvent(name: "LightMode", value: "Audio | Strobe", descriptionText: "Mode set to: Audio | Strobe")
            break
        case 'strobe':
            sendEvent(name: "submode", value: "frequency", displayed: false)
            sendEvent(name: "LightMode", value: "Audio | Frequency Bins", descriptionText: "Mode set to: Audio | Frequency Bins")
            break
        default:
            sendEvent(name: "submode", value: "level", displayed: false)
    	    sendEvent(name: "LightMode", value: "Audio | Level Bins", descriptionText: "Mode set to: Audio | Level Bins")
            break
        }
  } else {
    sendEvent(name: "submode", value: "level", displayed: false)
    sendEvent(name: "LightMode", value: "Audio | Level Bins", descriptionText: "Mode set to: Audio | Level Bins")
  }
  sendEvent(name: "mode", value: "audio", displayed: false)
  apiCall('/mode/' + device.currentValue("mode") + '/' + device.currentValue("submode"))
  sendEvent(name: "switch", value: "on")
}

def setCaptureMode() {
  if(device.currentValue("mode") == 'capture') {
    switch(device.currentValue("submode")) {
        case 'intelligent':
            sendEvent(name: "submode", value: "smooth", displayed: false)
            sendEvent(name: "LightMode", value: "Smooth Capture", descriptionText: "Mode set to: Smooth Capture")
            break
        case 'smooth':
            sendEvent(name: "submode", value: "fast", displayed: false)
            sendEvent(name: "LightMode", value: "Fast Capture", descriptionText: "Mode set to: Fast Capture")
            break
        case 'fast':
            sendEvent(name: "submode", value: "average", displayed: false)
            sendEvent(name: "LightMode", value: "Average Capture", descriptionText: "Mode set to: Average Capture")
            break
        case 'average':
            sendEvent(name: "submode", value: "user", displayed: false)
            sendEvent(name: "LightMode", value: "User Capture", descriptionText: "Mode set to: User Capture")
            break
        default:
            sendEvent(name: "submode", value: "intelligent", displayed: false)
            sendEvent(name: "LightMode", value: "Intelligent Capture", descriptionText: "Mode set to: Intelligent Capture")
            break
        }
  } else {
      sendEvent(name: "submode", value: "intelligent", displayed: false)
      sendEvent(name: "LightMode", value: "Intelligent Capture", descriptionText: "Mode set to: Intelligent Capture")
  }
  sendEvent(name: "mode", value: "capture", displayed: false)
  apiCall('/mode/' + device.currentValue("mode") + '/' + device.currentValue("submode"))
  sendEvent(name: "switch", value: "on")
}

def setMoodMode() {
	if(device.currentValue("mode") == 'mood') {
    	switch(device.currentValue("submode")) {
        	case 'disco':
            	sendEvent(name: "submode", value: "rainbow", displayed: false)
                sendEvent(name: "LightMode", value: "Rainbow", descriptionText: "Mode set to: Rainbow")
            	break
            case 'rainbow':
            	sendEvent(name: "submode", value: "nature", displayed: false)
                sendEvent(name: "LightMode", value: "Nature", descriptionText: "Mode set to: Nature")
            	break
            case 'nature':
            	sendEvent(name: "submode", value: "relax", displayed: false)
                sendEvent(name: "LightMode", value: "Relax", descriptionText: "Mode set to: Relax")
            	break
            default:
            	sendEvent(name: "submode", value: "disco", displayed: false)
                sendEvent(name: "LightMode", value: "Disco", descriptionText: "Mode set to: Disco")
            	break
        }
    } else {
        sendEvent(name: "submode", value: "disco", displayed: false)
        sendEvent(name: "LightMode", value: "Disco", descriptionText: "Mode set to: Disco")
    }
    sendEvent(name: "mode", value: "mood", displayed: false)
    apiCall('/mode/' + device.currentValue("mode") + '/' + device.currentValue("submode"))
    sendEvent(name: "switch", value: "on")
}

def apiCall(path) {
    def host = DeviceIP
    def LocalDevicePort = "49873"
    def method = "GET"
    def body = ""     
    
    def headers = [:] 
	headers.put("HOST", "$host:$LocalDevicePort")
    
	try {
		def hubAction = new physicalgraph.device.HubAction(
			method: method,
			path: path,
			body: body,
			headers: headers
		)
        sendHubCommand(hubAction)
        return hubAction
	}
	catch (Exception e) {
		log.debug "Hit Exception $e on $hubAction"
	}
}

def parse(String description) {
	def msg = parseLanMessage(description)
    def json = new groovy.json.JsonSlurper().parseText(msg.body)
    
    if(json.data) {
    	log.debug(json)
        if(json.data.brightness) {
        	sendEvent(name: "level", value: json.data.brightness)
        }
        
        if(json.data.modes) {
        	if(json.data.modes.mode == 'off') {
        		sendEvent(name: "switch", value: "off")
            } else {
            	sendEvent(name: "switch", value: "on")
            }
            
            sendEvent(name: "mode", value: json.data.modes.mode, displayed: false)
            sendEvent(name: "submode", value: json.data.modes.submode, displayed: false)
            switch(json.data.modes.mode) {
              case "mood":
                switch(json.data.modes.submode) {
                  case "disco":
                  	sendEvent(name: "LightMode", value: "Disco", descriptionText: "Mode set to: Disco")
                    break
                  case "rainbow":
                    sendEvent(name: "LightMode", value: "Rainbow", descriptionText: "Mode set to: Rainbow")
                    break
                  case "nature":
                    sendEvent(name: "LightMode", value: "Nature", descriptionText: "Mode set to: Nature")
                    break
                  case "relax":
                    sendEvent(name: "LightMode", value: "Relax", descriptionText: "Mode set to:  Relax")
                    break
                  case "manual":
                    sendEvent(name: "LightMode", value: "Manual", descriptionText: "Mode set to: Manual")
                    break
                }
                break;
              case "audio":
                switch(json.data.modes.submode) {
                  case 'mixed':
                    sendEvent(name: "LightMode", value: "Audio | Mixed Bins", descriptionText: "Mode set to: Audio | Mixed Bins")
                    break
                  case 'lamp':
                    sendEvent(name: "LightMode", value: "Audio | Lamp", descriptionText: "Mode set to: Audio | Lamp")
                    break
                  case 'strobe':
                    sendEvent(name: "LightMode", value: "Audio | Strobe", descriptionText: "Mode set to: Audio | Strobe")
                    break
                  case 'frequency':
                    sendEvent(name: "LightMode", value: "Audio | Frequency", descriptionText: "Mode set to: Audio | Frequency Bins")
                    break
                  case 'level':
    	            sendEvent(name: "LightMode", value: "Audio | Level Bins", descriptionText: "Mode set to: Audio | Level Bins")
                    break
                }
                break;
              case "capture":
                switch(json.data.modes.submode) {
                  case 'intelligent':
                    sendEvent(name: "LightMode", value: "Intelligent Capture", descriptionText: "Mode set to: Intelligent Capture")
                    break
                  case 'smooth':
                    sendEvent(name: "LightMode", value: "Smooth Capture", descriptionText: "Mode set to: Smooth Capture")
                    break
                  case 'fast':
                    sendEvent(name: "LightMode", value: "Fast Capture", descriptionText: "Mode set to: Fast Capture")
                    break
                  case 'fast':
                    sendEvent(name: "LightMode", value: "Average Capture", descriptionText: "Mode set to: Average Capture")
                    break
                  case 'average':
                    sendEvent(name: "LightMode", value: "User Capture", descriptionText: "Mode set to: User Capture")
                    break
                }
                break;
              case "off":
                sendEvent(name: "LightMode", value: "Off", descriptionText: "Mode set to: Off")
              	break;
            }
        }
        
        if(json.data.color) {
        	def arr = colorUtil.hexToRgb(json.data.color)
            def rgb = [
            	red: arr[0],
            	green: arr[1],
            	blue: arr[2]
            ]
            log.debug(rgb)
            setColorValues(rgb, json.data.color)
        }
    }
}

def updated() {
   unschedule(poll)
   runEvery5Minutes(poll)
}

def ping() {
  refresh()
}

def refresh() {
  apiCall('/status')
}

def poll() {
    refresh()
}

def configure() {
  refresh()
}

def installed() {
  if ((device.currentState("level")?.value == null) || (device.currentState("level")?.value == 0)) {
    sendEvent(name: "level", value: 100)
  }
}

// Color Management functions
def min(first, ... rest) {
  def min = first;
  for(next in rest) {
    if(next < min) min = next
  }
  
  min
}

def max(first, ... rest) {
  def max = first;
  for(next in rest) {
    if(next > max) max = next
  }
  
  max
}

def colorHsv2Rgb(h, s) {
	logTrace "< Color HSV: ($h, $s, 1)"
    
	def r
    def g
    def b
    
    if (s == 0) {
        r = 1
        g = 1
        b = 1
    }
    else {
        def region = (6 * h).intValue()
        def remainder = 6 * h - region

        def p = 1 - s
        def q = 1 - s * remainder
        def t = 1 - s * (1 - remainder)

		if(region == 0) {
            r = 1
            g = t
            b = p
        }
        else if(region == 1) {
            r = q
            g = 1
            b = p
        }
        else if(region == 2) {
            r = p
            g = 1
            b = t
        }
        else if(region == 3) {
            r = p
            g = q
            b = 1
        }
        else if(region == 4) {
            r = t
            g = p
            b = 1
        }
        else {
            r = 1
            g = p
            b = q
        }
	}
    
	logTrace "< Color RGB: ($r, $g, $b)"
  
	[red: r, green: g, blue: b]
}

def colorRgb2Hsv(r, g, b)
{
	logTrace "> Color RGB: ($r, $g, $b)"
  
	def min = min(r, g, b)
	def max = max(r, g, b)
	def delta = max - min
    
    def h
    def s
    def v = max

    if (delta == 0) {
    	h = 0
        s = 0
    }
    else {
		s = delta / max
        if (r == max) h = ( g - b ) / delta			// between yellow & magenta
		else if(g == max) h = 2 + ( b - r ) / delta	// between cyan & yellow
		else h = 4 + ( r - g ) / delta				// between magenta & cyan
        h /= 6

		if(h < 0) h += 1
    }

    logTrace "> Color HSV: ($h, $s, $v)"
    
    return [ hue: h, saturation: s, level: v ]
}