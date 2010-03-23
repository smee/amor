package de.modelrepository.test.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class FileUtility {
	/**
	 * Method returns the path of a file relative to another file.
	 * @param file the file whose path shall be relative.
	 * @param relativeTo the anchor-file.
	 * @return the relative path as a String.
	 */
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
	 
	/*
	 * Splits the string into tokens with the given delimiter.
	 */
	private static ArrayList<String> getDelimitedStringAsList(String str, String delimiter) {
	  ArrayList<String> resultList = new ArrayList<String>();
    	StringTokenizer st = new StringTokenizer(str, delimiter);
    	while (st.hasMoreTokens())
    		resultList.add(st.nextToken());
    	return resultList;
	}
	
	/**
	 * Deletes a file or directory.<br>
	 * If the directory is not empty its content will be erased first.
	 * @param d the file to delete.
	 */
	public static void delete(File d) {
		if (d.isDirectory()) {
			File[] items = d.listFiles();
			if (items != null) {
				for (File c : items)
					delete(c);
			}
		}
		d.delete();
	}
	
	/**
	 * Checks if a file is empty.<br>
	 * If the file is a directory the method checks if it contains other files.<br>
	 * If the file is a simple file the method checks if the file contains anything except whitespaces.
	 * @param f the file to check
	 * @return true if the file or directory is empty.
	 */
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
	
	public static File searchForFile(File directory, File f) {
		List<File> files = searchRecoursiveForFiles(directory);
		for (File file : files) {
			if(file.getName().equals(f.getName())) {
				return file;
			}
		}
		return null;
	}
	
	private static List<File> searchRecoursiveForFiles(File dir) {
		List<File> fileList = new LinkedList<File>();
		if(dir.isFile()) {
			fileList.add(dir);
		}else {
			File[] entries = dir.listFiles();
			for (File file : entries) {
				fileList.addAll(searchRecoursiveForFiles(file));
			}
		}
		return fileList;
	}
//	
//	public static void main(String[] args) {
//		File workdir = new File("D:/git/commons-lang/");
//		File f = new File("src/java/org/apache/commons/lang/exception/Nestable.java");
//		File newFile = searchForFile(workdir, f);
//		System.out.println(newFile);
//	}
}
