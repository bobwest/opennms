'use strict';

function doLogin(casper, url, username, password) {
	if (url === undefined) {
		url = 'http://localhost:8980/opennms';
	}
	if (username === undefined) {
		username = 'admin';
	}
	if (password === undefined) {
		password = 'admin';
	}

	casper.log('Filling in OpenNMS form.');
	casper.start(url, function() {
		this.fill('form', {
			j_username: username,
			j_password: password
		}, true);
	});
	casper.then(function() {
		casper.log('Finished filling out the form.');
	});
}

module.exports = {
	go: doLogin
};