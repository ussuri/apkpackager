# Android Package to build an apk on device

This plugin provides the packaging (zip, zipalign) and signing methods

## Status

Unstable/in development on Android. 
Not suitable for other platforms

## Notes

This demo functionality requires that the following are available
* a directory containing the custom template apk
* a zip file containing public key (pub.x509.pem), private key (pk8p.pk8) and a JSON file(app.json)
* a directory containing the new assets directory

API requirements:
* [templatePath, zipPath, assetsPath, outputPath]

JSON file example in the zip:
*   "appName":"HelloWorld",
*   "packageName":"com.example.helloworld",
*   "versionName":"0.2.1.1",
*   "keyPassword":"android",
*   "publicKeyName":"pub.x509.pem",
*   "privateKeyName":"pk8p.pk8"



