'use strict';

var require = patchRequire(require);

var casper = require('casper').create({
	verbose: true,
	logLevel: 'debug'
});
casper.on('remote.message', function(message) {
	casper.log(message, 'debug');
});

require('./login').go(casper);

module.exports = casper;