// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.chromium;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.channels.*;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.BufferedInputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import kellinwood.security.zipsigner.ZipSigner;
import android.net.Uri;
import android.util.Log;
import com.android.sdklib.build.*;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.json.JSONObject;

import com.axml.enddec.BinaryXMLParser;
import com.axml.enddec.BinaryResourceParser;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaArgs;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.CordovaWebView;
import org.json.JSONException;


public class APKPackager  extends CordovaPlugin {

    private String LOG_TAG = "APKPackage";
    private String returnMsg = "";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
  
    @Override
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
    }

    @Override
    public boolean execute(String action, final CordovaArgs args, final CallbackContext callbackContext) throws JSONException {
        if ("package".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    //packageApk(args, callbackContext);
                }
            });
            return true;
        } else if ("packageAPK".equals(action)) {
            cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    test(args, callbackContext);
                }
            });
            return true;
        } 
        return false;
    }

    private void test(CordovaArgs args, CallbackContext callbackContext) {
	File playground = null;
	File template = null;
	File assets = null;
        File output = null;
	File zip = null;
	File archive = null;
	File publicKey = null;
	File privateKey = null;
	File appJSON = null;
    	URL publicKeyUrl=null;
    	URL privateKeyUrl=null;
	
	try {
	} catch (Exception e) {
            callbackContext.error("Error on cleaning: "+e.getMessage());
            return;
	}

	try {
  	  CordovaResourceApi cra = webView.getResourceApi();
          playground = cra.mapUriToFile(cra.remapUri(Uri.parse(args.getString(0))));
          assets = cra.mapUriToFile(cra.remapUri(Uri.parse(args.getString(2))));
          output = cra.mapUriToFile(cra.remapUri(Uri.parse(args.getString(3))));
          zip = cra.mapUriToFile(cra.remapUri(Uri.parse(args.getString(1))));
	  archive = new File(playground, "archive");
	  template = new File(playground, "template");
	} catch (Exception e) {
            callbackContext.error("Missing arguments: "+e.getMessage());
            return;
	}
	
	try {
          deleteDir(archive);
	  extractToFolder(zip, archive);	    
	  publicKey = new File(archive, "pub.x509.pem");
	  privateKey = new File(archive, "pk8p.pk8");
	  publicKeyUrl = publicKey.toURI().toURL();
	  privateKeyUrl = privateKey.toURI().toURL();
	  appJSON = new File(archive, "app.json");
	} catch (Exception e) {
            callbackContext.error("Error at unzip: "+e.getMessage());
            return;
	}

	String appName = "";
	String packageName = "";
	String versionName = "";
	String privateKeyPass = "";

	try {
	  String jsonString = loadJSONFromFile(appJSON);
	  JSONObject jObject = new JSONObject(jsonString);	
	  appName = jObject.getString("appName");
	  packageName = jObject.getString("packageName");
	  versionName = jObject.getString("versionName");
	  privateKeyPass = jObject.getString("keyPassword");
	} catch (Exception e) {
            callbackContext.error("Error at parsing the JSON: "+e.getMessage());
            return;
	}

	try {
	  BinaryXMLParser parser = new BinaryXMLParser(template.getAbsolutePath()+"/AndroidManifest.xml");
  	  parser.parseXML();
	  parser.changeString(parser.getAppName(), appName);
	  parser.changeString(parser.getPackageName(), packageName);
	  parser.changeString(parser.getActivityName(), appName);
	  parser.changeString(parser.getVersion(), versionName);
  	  parser.exportXML(template.getAbsolutePath()+"/AndroidManifest.xml");

	  BinaryResourceParser resParser = new BinaryResourceParser(template.getAbsolutePath()+"/resources.arsc");
	  resParser.parseResource();
	  resParser.changePackageName(packageName);
	  //resParser.changePackageName("org.chromium.cadt.template");
	  resParser.exportResource(template.getAbsolutePath()+"/resources.arsc");

	} catch (Exception e) {
	    callbackContext.error("Error at modifing the Android Manifest: "+e.getMessage());
	}

        String generatedApkPath = playground.getAbsolutePath()+"/temp.apk";
        String signedApkPath=output.getAbsolutePath()+"/"+appName+".apk";


	//TODO: to copy the assets directory in the template
	try {
	    File destAssets = new File(template, "assets");
	    deleteDirContent(destAssets);
	    mergeDirectory(assets, destAssets);
	} catch (Exception e) {
            callbackContext.error("Error at assets copy: "+e.getMessage());
            return;
	}

	File fakeResZip;
        // take the completed package and make the unsigned APK
        try{
            // ApkBuilder REALLY wants a resource zip file in the contructor
            // but the composite res is not a zip - so hand it a dummy
            fakeResZip = new File(playground,"FakeResourceZipFile.zip");
            writeZipfile(fakeResZip);

            ApkBuilder b = new ApkBuilder(generatedApkPath,fakeResZip.getPath(),
					  playground.getAbsolutePath()+"/classes.dex",null,null,null);
	    b.addSourceFolder(template);
            b.sealApk();
        } catch (Exception e) {
            callbackContext.error("ApkBuilder Error: "+e.getMessage());
            return;
        }

        // sign the APK with the supplied key/cert
        try {
            ZipSigner zipSigner = new ZipSigner();
            X509Certificate cert = zipSigner.readPublicKey(publicKeyUrl);
            PrivateKey pk = zipSigner.readPrivateKey(privateKeyUrl,  privateKeyPass);
            zipSigner.setKeys("xx", cert, pk, null);
            zipSigner.signZip(generatedApkPath, signedApkPath);
        } catch (Exception e) {
            callbackContext.error("ZipSigner Error: "+e.getMessage());
            return;
	    }

        // After signing apk , delete intermediate stuff
        try {
            new File(generatedApkPath).delete();
	    fakeResZip.delete();
	    deleteDir(archive);
        } catch (Exception e) {
            callbackContext.error("Error cleaning up: "+e.getMessage());
            return;
	}

        callbackContext.success("succes for " + appName);
    }

    /*
    private void packageApk(CordovaArgs args, CallbackContext callbackContext) {
    	File workdir=null;
    	URL publicKeyUrl=null;
    	URL privateKeyUrl=null;
    	String keyPassword="";
	String appName="test";
    
    	try {
    		CordovaResourceApi cra = webView.getResourceApi();
	        workdir= cra.mapUriToFile(cra.remapUri(Uri.parse(args.getString(0))));
	        File pbk = cra.mapUriToFile(cra.remapUri(Uri.parse(args.getString(1))));
	        File pvk = cra.mapUriToFile(cra.remapUri(Uri.parse(args.getString(2))));
	        publicKeyUrl = pbk.toURI().toURL();
	        privateKeyUrl= pvk.toURI().toURL();
	        keyPassword= args.getString(3);
		appName = args.getString(4);
        } catch (Exception e) {
            callbackContext.error("Missing arguments: "+e.getMessage());
            return;
        }
    	String workdirpath=workdir.getAbsolutePath()+File.separator;
        String generatedApkPath = workdirpath+"app.apk";
        String signedApkPath=workdirpath+appName+"-signed.apk";
        String dexname = workdirpath+ "classes.dex";
	String templatedir=workdirpath+"template/";
	File templateDir = new File(workdir,"template");
	try {
          BinaryXMLParser parser = new BinaryXMLParser(templatedir+"AndroidManifest.xml");
  	  parser.parseXML();
	  //parser.changeString(parser.getAppName(), appName);
	  parser.changeString(parser.getPackageName(), "com.example"+appName);
	  parser.changeString(parser.getActivityName(), "A "+appName);
	  parser.changeString(parser.getVersion(), "2.2.1");
  	  parser.exportXML(templatedir+"AndroidManifest.xml");

	} catch (Exception e) {
	    callbackContext.error("Error at parsing: "+e.getMessage());
	}

	//TODO: to copy the assets directory in the template

        // take the completed package and make the unsigned APK
        try{
            // ApkBuilder REALLY wants a resource zip file in the contructor
            // but the composite res is not a zip - so hand it a dummy
            File fakeResZip = new File(workdir,"FakeResourceZipFile.zip");
            writeZipfile(fakeResZip);

            ApkBuilder b = new ApkBuilder(generatedApkPath,fakeResZip.getPath(),dexname,null,null,null);
	    b.addSourceFolder(templateDir);
            b.sealApk();
        } catch (Exception e) {
            callbackContext.error("ApkBuilder Error: "+e.getMessage());
            return;
        }

        // sign the APK with the supplied key/cert
        try {
            ZipSigner zipSigner = new ZipSigner();
            X509Certificate cert = zipSigner.readPublicKey(publicKeyUrl);
            PrivateKey privateKey = zipSigner.readPrivateKey(privateKeyUrl,  keyPassword);
            zipSigner.setKeys("xx", cert, privateKey, null);
            zipSigner.signZip(generatedApkPath, signedApkPath);
        } catch (Exception e) {
            callbackContext.error("ZipSigner Error: "+e.getMessage());
            return;
	    }

        // After signing apk , delete intermediate stuff
        try {
            new File(generatedApkPath).delete();
        } catch (Exception e) {
            callbackContext.error("Error cleaning up: "+e.getMessage());
            return;
	}

        callbackContext.success("succes");
    }*/

    private void deleteDir(File dir){
        if(!dir.exists()) return;
        if(dir.isDirectory()) {
            File [] files = dir.listFiles();
            if(files != null) {
                for( File f : files ) {
    	            if(f.isDirectory()) deleteDir(f);
    	            else f.delete();
                }
            }
        }
        dir.delete();
    }
    private void writeZipfile(File zipFile) throws IOException {
        if(zipFile.exists()) zipFile.delete();
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile));
        ZipEntry e = new ZipEntry("dummydir");
        out.putNextEntry(e);
        out.closeEntry();
        out.close();
    }
    
    private void deleteDirContent(File dir) {
        if(!dir.exists()) return;
        if(dir.isDirectory()) {
            File [] files = dir.listFiles();
            if(files != null) {
                for( File f : files ) {
    	            if(f.isDirectory()) deleteDir(f);
    	            else f.delete();
                }
            }
        }
    }

    private void writeStringToFile(String str, File target) {
    	FileWriter fw=null;
    	try {
    		File dir = target.getParentFile();
    		if(!dir.exists()) dir.mkdirs();
			fw = new FileWriter(target);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try{
			fw.close();
			} catch(Exception e) {}
		}
    	
    }
    
    /* overwrite stuff from a default zip with the things in sourcedir 
    */
    private void mergeDirectory(File srcdir, File workdir) 
            throws FileNotFoundException, IOException {
        File[] files = srcdir.listFiles();
        for(File file : files){
            if(file.isDirectory()) {
                File targetDir = new File(workdir, file.getName());
                targetDir.mkdirs();
                mergeDirectory(file, targetDir);
            } else {
                File targetFile = new File(workdir, file.getName());
                if(targetFile.exists()) targetFile.delete();
                copyFile(file, targetFile);
            }
        }
    }

    private void copyFile(File src, File dest)
            throws FileNotFoundException, IOException {
        FileInputStream istream = new FileInputStream(src);
        FileOutputStream ostream = new FileOutputStream(dest);
        FileChannel input = istream.getChannel();
        FileChannel output = ostream.getChannel();

        try {
            input.transferTo(0, input.size(), output);
        } finally {
            istream.close();
            ostream.close();
            input.close();
            output.close();
        }
    }

  public String loadJSONFromFile(File file) {
    String json = null;
    try {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        json = new String(buffer, "UTF-8");
    } catch (IOException ex) {
        ex.printStackTrace();
        return null;
    }
    return json;
  }

    private void extractToFolder(File zipfile, File tempdir) {
    	InputStream inputStream=null;
    	try {
            FileInputStream zipStream = new FileInputStream(zipfile);
            inputStream = new BufferedInputStream(zipStream);
            ZipInputStream zis = new ZipInputStream(inputStream);
            inputStream = zis;

            ZipEntry ze;
            byte[] buffer = new byte[32 * 1024];

            while ((ze = zis.getNextEntry()) != null)
            {
                String compressedName = ze.getName();

                if (ze.isDirectory()) {
                   File dir = new File(tempdir, compressedName);
                   dir.mkdirs();
                } else {
                    File file = new File(tempdir, compressedName);
                    file.getParentFile().mkdirs();
                    if(file.exists() || file.createNewFile()){
                        FileOutputStream fout = new FileOutputStream(file);
                        int count;
                        while ((count = zis.read(buffer)) != -1)
                        {
                            fout.write(buffer, 0, count);
                        }
                        fout.close();
                    }
                }
                zis.closeEntry();
            }
        } catch (Exception e) {

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
