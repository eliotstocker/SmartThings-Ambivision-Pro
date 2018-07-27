module.exports = {
  ping: {
    commandId: "Ping"
  },
  status: {
    commandId: 5
  },
  mode: {
    commandId: 2,
    values: {
      capture: 1,
      mood: 2,
      audio: 3,
      off: 4
    }
  },
  subMode: {
    commandId: 3,
    values: function(mode) {
      var _values = {
        1: {
          intelligent: 1,
          smooth: 2,
          fast: 3,
          average: 4,
          user: 5
        },
        2: {
          manual: 2,
          relax: 1,
          rainbow: 4,
          disco: 3,
          nature: 5
        },
        3: {
          level: 1,
          mixed: 2,
          lamp: 3,
          strobe: 4,
          frequency: 5
        }
      };
      return _values[mode];
    },
  },
  brightness: {
    commandId: 4,
    value: function(value) {
      if(typeof value != 'number') throw new Error('value must be number');
      return 'OVERALL_BRIGHTNESS={' + parseInt(value) + '}';
    }
  },
  color: {
    commandId: 1,
    value: function(r, g, b) {
      if(typeof r != 'number' ||
      typeof g != 'number' ||
      typeof b != 'number') throw new Error('value must be number');
      return 'R{' + parseInt(r) + '} G{' + parseInt(g) + '} B{' + parseInt(b) + '}';
    }
  }
};