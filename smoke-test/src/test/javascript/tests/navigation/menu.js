'use strict';

var expected = {
	'Search': 'http://localhost:8980/opennms/element/index.jsp',
	'Info': {
		children: {
			'Nodes': 'http://localhost:8980/opennms/element/nodeList.htm',
			'Assets': 'http://localhost:8980/opennms/asset/index.jsp',
			'Path Outages': 'http://localhost:8980/opennms/pathOutage/index.jsp'
		}
	},
	'Status': {
		children: {
			'Events': 'http://localhost:8980/opennms/event/index',
			'Alarms': 'http://localhost:8980/opennms/alarm/index.htm',
			'Notifications': 'http://localhost:8980/opennms/notification/index.jsp',
			'Outages': 'http://localhost:8980/opennms/outage/index.jsp',
			'Surveillance': 'http://localhost:8980/opennms/surveillance-view.jsp',
			'Heatmap': 'http://localhost:8980/opennms/heatmap/index.jsp',
			'Distributed Status': 'http://localhost:8980/opennms/distributedStatusSummary.htm'
		}
	},
	'Reports': {
		href: 'http://localhost:8980/opennms/report/index.jsp',
		children: {
			'Charts': 'http://localhost:8980/opennms/charts/index.jsp',
			'Resource Graphs': 'http://localhost:8980/opennms/graph/index.jsp',
			'KSC Reports': 'http://localhost:8980/opennms/KSC/index.htm',
			'Database Reports': 'http://localhost:8980/opennms/report/database/index.htm',
			'Statistics': 'http://localhost:8980/opennms/statisticsReports/index.htm'
		}
	},
	'Dashboards': {
		href: 'http://localhost:8980/opennms/dashboards.htm',
		children: {
			'Dashboard': 'http://localhost:8980/opennms/dashboard.jsp',
			'Ops Board': 'http://localhost:8980/opennms/vaadin-wallboard'
		}
	},
	'Maps': {
		href: 'http://localhost:8980/opennms/maps.htm',
		children: {
			'Distributed': 'http://localhost:8980/opennms/RemotePollerMap/index.jsp',
			'Topology': 'http://localhost:8980/opennms/topology',
			'Geographical': 'http://localhost:8980/opennms/node-maps'
		}
	},
	'admin': {
		href: 'http://localhost:8980/opennms/account/selfService/index.jsp',
		children: {
			'Notices: Off': {
				name: 'nav-admin-notice-status'
			},
			'Configure OpenNMS': {
				name: 'nav-admin-admin',
				href: 'http://localhost:8980/opennms/admin/index.jsp'
			},
			'Quick-Add Node': {
				name: 'nav-admin-quick-add',
				href: 'http://localhost:8980/opennms/admin/node/add.htm'
			},
			'Help/Support': {
				name: 'nav-admin-support',
				href: 'http://localhost:8980/opennms/support/index.htm'
			},
			'Change Password': {
				name: 'nav-admin-self-service',
				href: 'http://localhost:8980/opennms/account/selfService/index.jsp'
			},
			'Log Out': {
				name: 'nav-admin-logout',
				href: 'http://localhost:8980/opennms/j_spring_security_logout'
			}
		}
	}
};

casper.test.begin('OpenNMS Nav Bar Menu', 33, function suite(test) {
	require('../../util/login').go(casper);
	var utils = require('utils');
	var getElement = function(selector) {
		var elements = rootElement.querySelectorAll(selector);
		if (elements) {
			return elements.length;
		} else {
			return 0;
		}
	};

	var getEntry = function(text, obj, parent) {
		var ret = {
			selector: 'nav-' + text + '-top',
			href: undefined,
			text: text
		};

		if (parent) {
			ret.selector = 'nav-' + parent + '-' + text;
		}

		var entry = obj[text];
		if (typeof entry === 'string') {
			ret.href = entry;
		} else {
			if (entry.name) {
				ret.selector = entry.name;
			}
			if (entry.href) {
				ret.href = entry.href;
			}
		}
		if (!ret.href) {
			ret.href = '#';
		}

		return ret;
	};

	var testSelectorExists = function(selector) {
		casper.then(function() {
			test.assertExists(selector);
		});
	};

	for (var text in expected) {
		if (expected.hasOwnProperty(text)) {
			var entry = getEntry(text, expected);
			//var selector = 'ul > li > a[name=\"' + entry.selector.replace(/\"/, '\\\"') + '\"]';
			testSelectorExists('ul > li > a[name=\"' + entry.selector.replace(/\"/, '\\\"') + '\"]');
			if (expected[text].children) {
				var children = expected[text].children;
				for (var child in children) {
					if (children.hasOwnProperty(child)) {
						var childEntry = getEntry(child, children, text);
						testSelectorExists('ul > li > ul > li > a[name=\"' + childEntry.selector.replace(/\"/, '\\\"') + '\"]');
					}
				}
			}
		}
	}

	casper.run(function() {
		setTimeout(function() {
			test.done();
			phantom.exit();
		},0);
	});
});