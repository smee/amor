package de.modelrepository.test.util;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.Vector;

public class FileIterator implements Iterable<File>{
	private FileFilter filter;
	private Iterator<File> files;
	
	public FileIterator(File entryPoint, FileFilter filter) {
		this.filter = filter;
		files = getFiles(entryPoint).iterator();
	}
	
	private Vector<File> getFiles(File parent) {
		Vector<File> temp = new Vector<File>();
		for (File file : parent.listFiles(filter)) {
			if(file.isFile()) {
				temp.add(file);
			}
			else
				temp.addAll(getFiles(file));
		}
		return temp;
	}
	
	@Override
	public Iterator<File> iterator() {
		return files;
	}

//	public static void main(String[] args) {
//		File entryPoint = new File("res/out/T0003/01/voldemort");
//		FileFilter filter = new FileFilter() {
//			public boolean accept(File pathname) {
//				return pathname.isDirectory() || pathname.getName().endsWith(".java");
//			}
//		};
//		FileIterator iterator = new FileIterator(entryPoint, filter);
//		while(iterator.hasNext()) {
//			System.out.println(iterator.next());
//		}
//	}
}
