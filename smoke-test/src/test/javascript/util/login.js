'use strict';

var defaultOptions = {
	url: 'http://localhost:8980/opennms',
	username: 'admin',
	password: 'admin'
};

function doLogin(casper, options) {
	if (!options) { options = {}; }
	for (var key in defaultOptions) {
		if (!options.hasOwnProperty(key)) {
			options[key] = defaultOptions[key];
		}
	}
	casper.log('Filling in OpenNMS form.');
	casper.start(options.url, function() {
		this.fill('form', {
			j_username: options.username,
			j_password: options.password
		}, true);
	});
	casper.then(function() {
		casper.log('Finished filling out the form.');
	});
}

function openLogin(casper, options) {
	if (!options) { options = {}; }
	for (var key in defaultOptions) {
		if (!options.hasOwnProperty(key)) {
			options[key] = defaultOptions[key];
		}
	}
	casper.log('Filling in OpenNMS form.');
	casper.thenOpen(options.url, function() {
		this.fill('form', {
			j_username: options.username,
			j_password: options.password
		}, true);
	});
	casper.then(function() {
		casper.log('Finished filling out the form.');
	});
}

module.exports = {
	go: doLogin,
	login: openLogin
};