cordova.define("org.chromium.system.cpu.system.cpu", function(require, exports, module) { // Copyright (c) 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var exec = require('cordova/exec');

exports.getInfo = function(callback) {
  exec(callback, callback, 'ChromeSystemCpu', 'getInfo', []);
};

});
