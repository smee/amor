package de.modelrepository.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
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
	
	public static boolean isEmpty(File f) throws IOException {
		if(f.isDirectory() && f.listFiles().length > 0)
			return false;
		else {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line = br.readLine();
			if(line != null && line.length() != 0) {
				char[] chars = line.toCharArray();
				for (char c : chars) {
					if(c!=' ' && c!='\t')
						return false;
				}
			}
		}
		return true;
	}
}
