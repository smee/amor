package de.modelrepository.test.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Iterator;

public class FileIterator implements Iterable<File>{
	private FileFilter filter;
	private Iterator<File> files;
	
	/**
	 * Construct a new FileIterator at a given entry point. 
	 * The Iterator will deliver files contained by the entry point and matching the filter criteria.
	 * @param entryPoint the container
	 * @param filter filter which defines criteria for the files
	 */
	public FileIterator(File entryPoint, FileFilter filter) {
		this.filter = filter;
		files = getFiles(entryPoint).iterator();
	}
	
	/*
	 * get all children files (recursively) from the parent which match the filefilter. 
	 */
	private ArrayList<File> getFiles(File parent) {
		ArrayList<File> temp = new ArrayList<File>();
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
}
