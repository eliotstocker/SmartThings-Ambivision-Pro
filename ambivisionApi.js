var dgram = require('dgram');
var events = require('events');
var commands = require('./commands');

var api = function () {
  this._init();
};

api.prototype = new events.EventEmitter;

api.prototype.port = 45457;
api.prototype.deviceName = 'AmbiVision';
api.prototype._deviceIP = null;
api.prototype._server = null;
api.prototype._connected = false;
api.prototype._discoveryTimeout = 0;
api.prototype._throttleTime = 250;
api.prototype._throttleStart = 0;
api.prototype.currentModes = {
  mode: null,
  submode: null
};
api.prototype.debug = false;
api.prototype._versions = {
  hub: null,
  link: null
};
api.prototype._awaitReturn = false;
api.prototype._returnValue = null;

api.prototype._init = function () {
  this._server = dgram.createSocket('udp4');
  this._server.on('error', function (err) {
    this.destroy();
    this._init();
  });

  this._server.on('message', function (msg, rinfo) {
    if(this.debug) {
      console.log("Received: " + msg);
    }
    var initMessageParse = /AmbiVision\(\d*_V(\d?.\d*)\) MagicLink\(\d*v(\d):/g;
    var data = initMessageParse.exec(msg);
    if (data && data.index !== undefined && data.index === 0) {
      this._versions.hub = parseFloat(data[1]);
      this._versions.link = parseInt(data[2]);
      this._deviceIP = rinfo.address;
      this._connected = true;
      console.log("Connected to AmbiVision Pro on: " + this._deviceIP);
      this.emit('ready');
    } else {
      if(this._awaitReturn) {
        this._returnValue = msg.toString();
        this._awaitReturn = false;
      }
    }
  }.bind(this));

  this._server.on('listening', function () {
    this._server.setBroadcast(true);
    this._sendDiscovery();
    api.prototype._discoveryTimeout = setInterval(this._sendDiscovery.bind(this), 30000);
  }.bind(this));

  this._server.bind(58837);
};

api.prototype._sendDiscovery = function() {
  this._server.send(this.deviceName + commands.ping.commandId + ' \n', this.port, '255.255.255.255', function (err) {
    if (err) console.log(err.message);
  });
};

api.prototype._sendCommand = function(command) {
  if(new Date().getTime() - this._throttleStart < this._throttleTime) {
    if (this.debug) {
      console.log("throttling Send Command: " + command);
    }
    setTimeout(this._sendCommand.bind(this, command), 25);
  } else {
    this._throttleStart = new Date().getTime();
    this._server.send(command + ' \n', this.port, this._deviceIP);
    if (this.debug) {
      console.log("Sent: " + command);
    }
  }
};

api.prototype.setMode = function (mode, submode, callback) {
  if (!this._connected) {
    setTimeout(this.setMode.bind(this, mode, submode), 150);
  }

  if (!commands.mode.values[mode]) throw new Error("Mode: '" + mode + "' not found");

  var modeId = commands.mode.values[mode];

  if (submode !== undefined) {
    var subModes = commands.subMode.values(modeId);
    if (!subModes[submode]) throw new Error("SubMode: '" + submode + "' not found");
  }

  this._sendCommand(this.deviceName + commands.mode.commandId + modeId );
  if (subModes !== undefined) this._sendCommand(this.deviceName + commands.subMode.commandId + subModes[submode]);

  if(callback) {
    this._setAwaitReturn(true);
    this._runCallback(callback);
  }
};

api.prototype.setBrightness = function (percent, callback) {
  if (!this._connected) {
    setTimeout(this.setBrightness.bind(this, percent), 150);
  }

  this._sendCommand(this.deviceName + commands.brightness.commandId + '' + commands.brightness.value(percent));

  if(callback) {
    this._setAwaitReturn(true);
    this._runCallback(callback);
  }
};

api.prototype.setColor = function (value, callback) {
  if (!this._connected) {
    setTimeout(this.setColor.bind(this, value), 150);
  }
  this.setMode('mood', 'manual');
  var rgb = this._htmlColorToRGB(value);
  this._sendCommand(this.deviceName + commands.color.commandId + '' + commands.color.value(rgb.r, rgb.g, rgb.b));

  if(callback) {
    this._setAwaitReturn(true);
    this._runCallback(callback);
  }
};

api.prototype.turnOff = function (callback) {
  if (!this._connected) {
    setTimeout(this.turnOff.bind(this), 150);
  }
  this.setMode('off');

  if(callback) {
    this._setAwaitReturn(true);
    this._runCallback(callback);
  }
};

api.prototype.getStatus = function (callback) {
  this._sendCommand(this.deviceName + commands.status.commandId);

  function parseStatus(data) {
    var parts = data.split('\n');
    var status = {};
    for(var i = 0; i < parts.length; i++) {
      var part = parts[i];
      var kv = part.split('=');
      switch(kv[0]) {
        case "CURRENT_MODES":
          var modeId = parseInt(kv[1][0]);
          var submodeId = parseInt(kv[1][2]);
          var index = Object.values(commands.mode.values).indexOf(modeId);

          var modes = {
            mode: Object.keys(commands.mode.values)[index]
          };

          var values = commands.subMode.values(modeId);

          if(values) {
            index = Object.values(values).indexOf(submodeId);

            if (index > -1) {
              modes.submode = Object.keys(values)[index];
            }
          }
          this.currentModes = modes;
          status.modes = modes;
          break;
        case "MOOD_COLORS":
          var extractColors = /r{(\d*)} g{(\d*)} b{(\d*)}/g;
          var colors = extractColors.exec(kv[1].trim());
          status.color = this._RGBToHtmlColor(parseInt(colors[1]), parseInt(colors[2]), parseInt(colors[3]));
          break;
        case "OVERALL_BRIGHTNESS":
          var val = kv[1].trim();
          status.brightness = val.substr(1, val.length -2);
      }
    }

    callback(status);
  }

  if(callback) {
    this._setAwaitReturn(true);
    this._runCallback(parseStatus.bind(this));
  }
};

api.prototype.destroy = function () {
  clearInterval(api.prototype._discoveryTimeout);
  this._server.close();
};

api.prototype.getHubVersion = function () {
  if (!this._connected) {
    throw new Error("Not yet connected");
  }
  return this._versions.hub;
};

api.prototype.getLinkVersion = function () {
  if (!this._connected) {
    throw new Error("Not yet connected");
  }
  return this._versions.link;
};

api.prototype._runCallback = function(callback) {
  if(this._returnValue != null) {
    callback(this._returnValue);
  } else {
    setTimeout(this._runCallback.bind(this, callback), 100);
  }
};

api.prototype._htmlColorToRGB = function (code) {
  var result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(code);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : null;
};

api.prototype._RGBToHtmlColor = function(r, g, b) {
  return "#" + ((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1);
};

api.prototype._setAwaitReturn = function(on) {
  this._awaitReturn = !!on;
  this._returnValue = null;
};

module.exports = api;