'use strict';

var expected = {
	'Search': {
		href: 'http://localhost:8980/opennms/element/index.jsp',
		linkPageSelector: 'h3[class="panel-title"]:first-of-type',
		linkPageText: 'Search for Nodes'
	},
	'Info': {
		children: {
			'Nodes': {
				href: 'http://localhost:8980/opennms/element/nodeList.htm',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'Nodes'
			},
			'Assets': {
				href: 'http://localhost:8980/opennms/asset/index.jsp',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'Search Asset Information'
			},
			'Path Outages': {
				href: 'http://localhost:8980/opennms/pathOutage/index.jsp',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'All Path Outages'
			}
		}
	},
	'Status': {
		children: {
			'Events': {
				href: 'http://localhost:8980/opennms/event/index',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'Event Queries'
			},
			'Alarms': {
				href: 'http://localhost:8980/opennms/alarm/index.htm',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'Alarm Queries'
			},
			'Notifications': {
				href: 'http://localhost:8980/opennms/notification/index.jsp',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'Notification queries'
			},
			'Outages': {
				href: 'http://localhost:8980/opennms/outage/index.jsp',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'Outage Menu'
			},
			'Surveillance': 'http://localhost:8980/opennms/surveillance-view.jsp',
			'Heatmap': 'http://localhost:8980/opennms/heatmap/index.jsp',
			'Distributed Status': {
				href: 'http://localhost:8980/opennms/distributedStatusSummary.htm',
				linkPageSelector: 'h3[class="panel-title"]',
				linkPageText: 'Distributed Status Summary Error: No Applications Defined'
			}
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
	require('../../util/init').configure(casper);

	var login = require('../../util/login');
	var utils = require('utils');

	login.go(casper);

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
			if (entry.linkPageSelector) {
				ret.linkPageSelector = entry.linkPageSelector;
			}
			if (entry.linkPageText) {
				ret.linkPageText = entry.linkPageText;
			}
		}
		if (!ret.href) {
			ret.href = '#';
		}

		return ret;
	};

	var testSelectorExists = function(selector, name) {
		casper.then(function() {
			test.assertExists(selector, name);
		});
	};

	var testClickable = function(moveto, selector, text, name) {
		if (!utils.isArray(moveto)) {
			moveto = [moveto];
		}
		for (var m=0, len=moveto.length; m < len; m++) {
			var loc = moveto[m];
			casper.then(function() {
				this.mouseEvent('mouseover', loc);
			});
		}
		casper.then(function() {
			//console.log('click: ' + moveto[moveto.length-1]);
			this.click(moveto[moveto.length-1]);
		});

		if (selector) {
			if (text) {
				var desc;
				if (name) {
					desc = name + ' link target page has text "' + text + '"';
				}
				casper.waitForSelector(selector, function() {
					test.assertSelectorHasText(selector, text, desc);
				});
			} else {
				var desc;
				if (name) {
					desc = name + ' link target page selector matches.';
				}
				casper.waitForSelector(selector, function() {
					test.assertExists(selector, desc);
				});
			}
		}

		casper.back();
	};

	var getMenuEntryName = function(entries) {
		if (!utils.isArray(entries)) {
			entries = [entries];
		}
		return '[' + entries.join(' -> ') + ']';
	};

	for (var text in expected) {
		if (expected.hasOwnProperty(text)) {
			var entry = getEntry(text, expected);
			//var selector = 'ul > li > a[name=\"' + entry.selector.replace(/\"/, '\\\"') + '\"]';
			var entrySelector = 'ul > li > a[name=\"' + entry.selector.replace(/\"/, '\\\"') + '\"]';
			testSelectorExists(entrySelector, getMenuEntryName(text) + ' menu entry exists');
			testClickable(entrySelector, entry.linkPageSelector, entry.linkPageText, getMenuEntryName(text));
			if (expected[text].children) {
				var children = expected[text].children;
				for (var child in children) {
					if (children.hasOwnProperty(child)) {
						var childEntry = getEntry(child, children, text);
						var childSelector = 'ul > li > ul > li > a[name=\"' + childEntry.selector.replace(/\"/, '\\\"') + '\"]';
						testSelectorExists(childSelector, getMenuEntryName([text, child]) + ' menu entry exists');
						testClickable([entrySelector, childSelector], childEntry.linkPageSelector, childEntry.linkPageText, getMenuEntryName([text, child]));
					}
				}
			}
		}
	}

	/* Special Cases */
	login.login(casper);

	// surveillance view
	casper.thenOpen('http://localhost:8980/opennms/surveillance-view.jsp');
	casper.waitForSelector('#surveillance-view-ui');
	casper.then(function() {
		this.page.switchToChildFrame(0);
	});
	casper.waitForSelector('span[class="v-captiontext"]', function() {
		test.assertSelectorHasText('span[class="v-captiontext"]', 'Surveillance view: default', 'Surveillance View iframe loads');
	});
	casper.then(function() {
		this.page.switchToParentFrame();
	});

	// heatmap
	casper.thenOpen('http://localhost:8980/opennms/heatmap/index.jsp');
	casper.waitForSelector('#coreweb', function() {
		test.assertSelectorHasText('h3[class="panel-title"] > a', 'Alarm Heatmap  (by Categories)', 'Heatmap iframe loads');
		this.page.switchToParentFrame();
	});
	casper.run(function() {
		setTimeout(function() {
			test.done();
			phantom.exit();
		},0);
	});
});