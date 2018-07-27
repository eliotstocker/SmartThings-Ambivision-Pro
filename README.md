# SmartThings-Ambivision-Pro
Smartthings Control for Ambivision Pro (Via NodeJS service)

## Instructions:
Download this repo and run ```npm install``` then start the service with ```npm start``` (it is most liley best to use something like PM2 to keep the server running in the backgrounds and after reboots)

the service with automagically find your ambivision pro.

currently the device will have to be manually installed, (I'll create a smart app to auto discover etc at a later date)

install the device handler, then create a new device with the handler, it is very important that you set the devcie network id to match the device the service is running on.

### the network ID must be set the following way:
hexidecimal value of each port of the device IP, followed by the hexidecial value of the port the service is running on (49873)
Example:
192.168.1.25:49873 would be C0A80119:C2D1 where 192 converted to hex is C0 and so on

## TODO:
1. add service auto discovery
2. better way to choose capture, audio and mood sub modes
3. add a way to decide on on state
4. try and work out how to workout what mode the device is in when "off" like in the wizard app
5. think about better integration for other apps etc, code side its very easy to start differtent modes, but would be nice to have easy ways to enable capture mode for instance.
