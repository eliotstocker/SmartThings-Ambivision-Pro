var AmbiAPI = require('./ambivisionApi');
var express = require('express');

var api = new AmbiAPI();
//api.debug = true;
var app = express();

app.get('/off', function(req, res) {
  api.turnOff();
  res.send(JSON.stringify({
    status: 'ok'
  }));
});

app.get('/mode/:mode/:submode', function(req, res) {
  try {
    api.setMode(req.params['mode'], req.params['submode']);
    res.send(JSON.stringify({
      status: 'ok'
    }));
  } catch(e) {
    res.send(JSON.stringify({
      status: 'error',
      message: e.message
    }));
  }
});

app.get('/color/:color', function(req, res) {
  api.setColor(req.params['color']);
  res.send(JSON.stringify({
    status: 'ok'
  }));
});

app.get('/brightness/:brightness', function(req, res) {
  api.setBrightness(parseInt(req.params['brightness']));
  res.send(JSON.stringify({
    status: 'ok'
  }));
});

app.get('/status', function(req, res) {
  api.getStatus(function(data) {
    res.send(JSON.stringify({
      status: 'ok',
      data: data
    }));
  })
});

app.listen(49873, function() { console.log('WebServer listening on port: 49873') });