package de.modelrepository.test.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class FileUtility {
	public static String getRelativePath(File file, File relativeTo) {
	    String path = "";
	    
	    List<String> fileList = getDelimitedStringAsList(file.getAbsolutePath(),File.separator);
	    List<String> relativeList = getDelimitedStringAsList(relativeTo.getAbsolutePath(),File.separator);
	    
	    int size = fileList.size();
	    int relativeSize = relativeList.size();
	    int count = 0;
	    
	    while(count<size && count<relativeSize) {
	    	if(fileList.get(count).equals(relativeList.get(count)))
	    		count++;
	    	else
	    		break;
	    }
	    
	    for (int i = count; i < relativeSize; i++) {
	    	path += ".." + File.separator;
	    }
	    
	    for (int i = count; i < size; i++) {
	    	path += fileList.get(i) + File.separator;
	    }
	    
	    if(path.indexOf(File.separator)>-1)
	    	path = path.substring(0,path.lastIndexOf(File.separator));
	    
	    return path;    
	}
	  
	private static List getDelimitedStringAsList(String str, String delimiter) {
	  List resultList = new ArrayList();
    	StringTokenizer st = new StringTokenizer(str, delimiter);
    	while (st.hasMoreTokens())
    		resultList.add(st.nextToken());
    	return resultList;
	}
	
	public static void delete(final File d) {
		if (d.isDirectory()) {
			final File[] items = d.listFiles();
			if (items != null) {
				for (final File c : items)
					delete(c);
			}
		}
		d.delete();
	}
}
