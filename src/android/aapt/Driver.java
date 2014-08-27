package org.chromium.aapt;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Collections;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Xml;

public class Driver {

	private File srcDir;
	private File destDir;
	
	public Driver(File srcResDir, File destResDir) {
		this.srcDir = srcResDir;
		this.destDir = destResDir;
	}
	
    private void parseStringXml(StringPool sp, File file) throws IOException{
	try {
   	        if(file.isFile()==false)return;
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new FileReader(file));
		int eventType = -1;
		Boolean canAdd=false;
		Boolean canAddArray=false;
		eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				if ("string".equals(parser.getName())) {
				    canAdd=true;
				    //sp.addString(parser.getText());
				} else if ("string-array".equals(parser.getName())) {
				    canAddArray=true;
				} else if ("item".equals(parser.getName()) && canAddArray) {
				    canAdd=true;
				}
			} else if ((eventType == XmlPullParser.TEXT) && (canAdd)) {
			    //debug+="@"+parser.getText()+"\n";
			    sp.addString(parser.getText());
			    canAdd=false;
			} else if (eventType == XmlPullParser.END_TAG) {
			    if ("string-array".equals(parser.getName())) {
				    canAddArray=false;
			    }
			}
			eventType = parser.next();			
		}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
    }

    private void addToVector(Vector<String> v, String toAdd) {
	if(!v.contains(toAdd))
	    v.add(toAdd);
    }

    private void parseXML(File file) throws IOException{
	try {
   	        if(file.isFile()==false)return;

		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new FileReader(file));
		int eventType = -1;
		Boolean canAdd=false;
		Boolean canAddArray=false;
		eventType = parser.getEventType();
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
			    String name=parser.getName();
			    if(tsp.contains(name)) {
				String value=parser.getAttributeValue(null,"name");
				if(value!=null)addToVector(ksp, value);
			    }
			    if ("string".equals(name)) {
				    canAdd=true;
				} else if ("string-array".equals(name)) {
				    canAddArray=true;
			    } else if ("item".equals(name)){
				if (canAddArray) {
				    canAdd=true;
				}else {
				    String type=parser.getAttributeValue(null,"type");
				    if((type!=null) && (type.equals("id"))) {
					addToVector(ksp, parser.getAttributeValue(null,"name"));
				    }
				}
			    }
			} else if ((eventType == XmlPullParser.TEXT) && (canAdd)) {
			    canAdd=false;
			    addToVector(sp,parser.getText());
			} else if (eventType == XmlPullParser.END_TAG) {
			    if ("string-array".equals(parser.getName())) {
				    canAddArray=false;
			    }
			}
			eventType = parser.next();			
		}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
    }
    
    Vector<String> sp = new Vector<String>();
    Vector<String> tsp = new Vector<String>();
    Vector<String> ksp = new Vector<String>();

    public void doTheMagic() throws IOException{
        tsp.add("attr");
        tsp.add("drawable");
        tsp.add("mipmap");
        tsp.add("layout");
        tsp.add("xml");
        tsp.add("raw");
        tsp.add("string");
        tsp.add("color");
        tsp.add("dimen");
        tsp.add("style");
        tsp.add("id");
        tsp.add("array");
        tsp.add("menu");

	String pkgAbsolutePath = srcDir.getAbsolutePath();
	debug="-Magic-\n";
	File resDir = new File(srcDir, "res");
	String[] resDirs = resDir.list();
	if (resDirs != null)
	  for (String dir : resDirs) {
		File rd = new File(resDir, dir);
		if (rd.isDirectory()) {
			String[] dirNameParts = dir.split("-");
			Config cfg = Config.fromDirNameParts(dirNameParts);
			String[] resFiles = rd.list();
			for (String resFileName : resFiles) {
			    File rf = new File(rd, resFileName);
			    if (rf.isFile()) {
				String relativePath = rf.getAbsolutePath();
				if (relativePath.startsWith(pkgAbsolutePath)) {
				    relativePath = relativePath.substring(pkgAbsolutePath.length());
				}

			      if("values".equals(dirNameParts[0])==false) {
			        addToVector(tsp,dirNameParts[0]);
				addToVector(sp,relativePath);
				String[] fnParts = resFileName.split("\\.");
			        addToVector(ksp,fnParts[0]);
			      }
			      parseXML(rf);
			    }
			}
		}
	}
    }

	public void createResourceTable(OutputStream output) throws IOException {
		Package pkg = new Package();
		String pkgAbsolutePath = srcDir.getAbsolutePath();
		StringPool topLevelStringPool = new StringPool();
		topLevelStringPool.setUseUTF8(true);
		/*
		debug="";
		debug+="Driver\n";
		debug+="Dir list\n";
		// mapping of resource type names ("xml", "drawable", etc.) to their file collections.	
		HashMap<String, AaptResourceGroup> resourceGroups = new HashMap<String, AaptResourceGroup>();

		//TODO: This, in a loop
		File resDir = new File(srcDir, "res");
		String[] resDirs = resDir.list();
		if (resDirs != null)
		  for (String dir : resDirs) {
		      debug+=dir+"\n";
			File rd = new File(resDir, dir);
			if (rd.isDirectory()) {
				String[] dirNameParts = dir.split("-");
				AaptResourceGroup resourceGroup = resourceGroups.get(dirNameParts[0]);
				if (resourceGroup == null) {
					resourceGroup = new AaptResourceGroup();
					resourceGroups.put(dirNameParts[0], resourceGroup);
					pkg.addTypeString(dirNameParts[0]);
				}
				if (dirNameParts[0].equals("values")) {
				  Config cfg = Config.fromDirNameParts(dirNameParts);
				  String[] resFiles = rd.list();
				  for (String resFileName : resFiles) {
				      File rf = new File(rd, resFileName);
				      debug+="-> "+resFileName+"\n";
				      parseStringXml(topLevelStringPool, rf);
				  }
				} else {
				  Config cfg = Config.fromDirNameParts(dirNameParts);
				  String[] resFiles = rd.list();
				  for (String resFileName : resFiles) {
					File rf = new File(rd, resFileName);
					if (rf.isFile()) {
						String relativePath = rf.getAbsolutePath();
						if (relativePath.startsWith(pkgAbsolutePath)) {
						    relativePath = relativePath.substring(pkgAbsolutePath.length());}
			
						topLevelStringPool.addString(relativePath);
						resourceGroup.addFileWithConfig(cfg, rf);
						debug+="---"+relativePath+"\n";
					}
				}
			      }
			}
		}
		*/
		/* "First time through: only add base packages (id is not 0);
		 *  second time through add the other packages.
		 */
		doTheMagic();
		pkg.setId(127);
		pkg.setName(getPackageName());

		debug+="-StringPool="+sp.size()+"\n";
		for(int i=0;i<sp.size();i++) {
		    debug+=sp.get(i)+"\n";
		    topLevelStringPool.addString(sp.get(i));
		}

		debug+="-Type StringPool="+tsp.size()+"\n";
		for(int i=0;i<tsp.size();i++) {
		    debug+=tsp.get(i)+"\n";
		    pkg.addTypeString(tsp.get(i));
		}

		Collections.sort(ksp);
		debug+="-Key StringPool="+ksp.size()+"\n";
		for(int i=0;i<ksp.size();i++) {
		    debug+=ksp.get(i)+"\n";
		    pkg.addKeyString(ksp.get(i));
		}


		Table resourceTable = new Table();
		resourceTable.setStringPool(topLevelStringPool);
		resourceTable.addPackage(pkg);
		
		ResourceArchive resourceArchive = new ResourceArchive();
		resourceArchive.addComponent(resourceTable);
		debug+=resourceArchive.computeSize();
		resourceArchive.write(output);

	}

    String debug="";

    private String getPackageName() throws IOException {
		String packageName = null;
		try {
		File manifestFile = new File(srcDir, "AndroidManifest.xml");
		XmlPullParser parser = Xml.newPullParser();
		parser.setInput(new FileReader(manifestFile));
		// Get package name
		int eventType = -1;
		while (packageName == null && eventType != XmlPullParser.END_DOCUMENT) {
			eventType = parser.getEventType();
			if (eventType == XmlPullParser.START_TAG) {
				if ("manifest".equals(parser.getName())) {
					packageName = parser.getAttributeValue(null, "package");
				}
			}
			eventType = parser.next();			
		}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}
		return packageName;
    }

    public String getDebug() {
	return debug;
    }
	
	public static void main(String[] args) {
		Driver d = new Driver(new File("/Users/iclelland/MCA4/kktest/platforms/android/bin/temp"),new File("/Users/iclelland/MCA4/kktest/platforms/android/bin/temp"));
		try {
			File outFile = new File("resources.arsc__");
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));
			d.createResourceTable(os);
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		//test();
	}
	
	public void test() {
		ResourceArchive argc = new ResourceArchive();
		Table theOnlyTable = new Table();
		StringPool topLevelStringPool = new StringPool();
		topLevelStringPool.addString("This is the first String");
		topLevelStringPool.addString("This is the second String");
		topLevelStringPool.setUseUTF8(false);
		theOnlyTable.setStringPool(topLevelStringPool);
		
		Package pkg = new Package();
		/* "First time through: only add base packages (id is not 0);
		 *  second time through add the other packages.
		 */
		pkg.setId(1);
        pkg.setName("com.google.aapttest");
        
        pkg.addTypeString("attr");
        pkg.addTypeString("drawable");
        pkg.addTypeString("mipmap");
        pkg.addTypeString("layout");
        pkg.addTypeString("xml");
        pkg.addTypeString("raw");
        pkg.addTypeString("string");
        pkg.addTypeString("color");
        pkg.addTypeString("dimen");
        pkg.addTypeString("style");
        pkg.addTypeString("id");
        pkg.addTypeString("array");
        pkg.addTypeString("menu");

        TypeBundle attrs = new TypeBundle(1);
        
        Type type = new Type();
        type.typeId = 1;
        Config config = new Config();
        config.screenHeight = 320;
        config.screenWidth = 480;
        config.language = "en";
        type.config = config;
        
        MapEntry me = new MapEntry();
        Value v = new Value();
        v.dataType = Value.TYPE_INT_DEC;
        v.data = 1;
        Map m = new Map();
        m.value = v;
        TableRef tr = new TableRef();
        tr.packageIndex = 1;
        tr.typeIndex = 2;
        tr.entryIndex = 3;
        m.name = tr;
        m.value = v;
        me.addMap(m);
        me.isPublic = false;
        me.stringPoolRef = 0x1234;
        type.addEntry(me);

        attrs.addType(type);
        
        pkg.addTypeBundle(attrs);
        
        theOnlyTable.addPackage(pkg);
        
		argc.addComponent(theOnlyTable);
		try {
			File outFile = new File("resources.arsc_");
			BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(outFile));
			argc.write(os);
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
