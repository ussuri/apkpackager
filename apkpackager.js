
var exec = cordova.require('cordova/exec');

function pkgSuccess( msg ) {
      console.log(msg);
}

function pkgFail(msg) {
    console.log('Error: ' + msg);
}


module.exports.buildAPK =  function(zipPath, assetsPath, outputPath, templatePath) {
    exec(pkgSuccess, pkgFail, 'APKPackager', 'packageAPK', [templatePath, zipPath, assetsPath, outputPath]);
}
/*
module.exports.makeapk = function(name) {
    var workdir = 'file:///storage/sdcard0/Download/test-apk/'; //fs.root.toURL()+'Download/';
    var publicKeyURL = workdir+"pub.x509.pem";
    var privateKeyURL = workdir+"pk8p.pk8";
    var passwd="android";      // password for private key
    console.log(name);
    exec(pkgSuccess, pkgFail, 'APKPackager', 'package', [workdir, publicKeyURL, privateKeyURL, passwd,name]);

}*/

