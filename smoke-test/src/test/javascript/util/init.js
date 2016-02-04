'use strict';

var configureCasper = function(casper) {
	casper.on('remote.message', function(message) {
		casper.log(message, 'debug');
	});
	casper.options.viewportSize = {
		width: 1920,
		height: 1024
	};
	casper.options.onWaitTimeout = function() {
		this.capture('timeout.png');
		casper.exit(1);
	};
	casper.test.on('fail', function(failure) {
		if (failure && typeof failure.message === 'string' || failure.message instanceof String) {
			var message = failure.message.replace(/[^A-Za-z0-9]+/gm, '-').replace(/^\-/, '').replace(/\-$/, '').toLowerCase();
			casper.capture('target/screenshots/' + message + '.png');
		} else {
			console.log('Unsure how to handle failure: ' + JSON.stringify(failure));
		}
		casper.exit(1);
	});
};

module.exports = {
	configure: configureCasper
};