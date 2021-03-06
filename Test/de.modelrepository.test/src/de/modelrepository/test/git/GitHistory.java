package de.modelrepository.test.git;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

import org.eclipse.jgit.lib.Repository;

import de.modelrepository.test.util.FileIterator;
import de.modelrepository.test.util.FileUtility;

public class GitHistory {
	private Repository repo;
	private Iterator<File> fileIterator;
	private File indexFile;
	
	/**
	 * Build a history for a whole repository.
	 * @param repo the repository
	 */
	public GitHistory(Repository repo) throws IOException {
		this.repo = repo;
		//create a file iterator which iterates over all java source files of the repository
		fileIterator = new FileIterator(repo.getWorkDir(), new FileFilter() {
			public boolean accept(File pathname) {
				return pathname.isDirectory() || pathname.getAbsolutePath().endsWith(".java");
			}
		}).iterator();
		indexFile = new File(repo.getDirectory(), "javaFileMergeCount.csv");
	}
	
	/*
	 * counts the number of merges for each file.
	 */
	private ArrayList<Entry<File,Integer>> getFileMergeCount() throws IOException {
		Hashtable<File, Integer> ht = new Hashtable<File, Integer>();
		for(Iterator<File> i=fileIterator; i.hasNext(); ) {
			File file = i.next();
			GitFileHistory h = new GitFileHistory(file, repo);
			ht.put(file, h.getParallelBranches().size());
		}
		
		//sort the Hashtable
		ArrayList entryList = new ArrayList(ht.entrySet());
		Collections.sort(entryList, new EntryComparator());
		
		return entryList;
	}
	
	/*
	 * writes the index for the repository.
	 */
	private void indexRepository() throws IOException {
		indexFile.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(indexFile));
		for (Entry<File, Integer> e : getFileMergeCount()) {
			String s = e.getKey() + "," + e.getValue() + "\n";
			bw.append(s);
		}
		bw.close();
	}
	
	/*
	 * A comparator for Entries of the index file
	 */
	private class EntryComparator implements Comparator<Entry>{
		/*
		 * compares two entries by the number of revisions.
		 * if the number equals the files will be compared.
		 */
		public int compare(Entry o1, Entry o2) {
			int result=0;
			Integer value1 = (Integer)o1.getValue();
			Integer value2 = (Integer)o2.getValue();

			if(value1.compareTo(value2) == 0) {
				File file1 = (File)o1.getKey();
				File file2 = (File)o2.getKey();
				result = file1.compareTo(file2);
			} else
				result = value2.compareTo(value1);

			return result;
		}
	}
	
	/**
	 * Get a list of the top (X) files of the repository index.<br>
	 * These files will have the most revisions.
	 * @param x the number of files
	 * @return an {@link ArrayList} containing files with the most revisions.
	 */
	public ArrayList<File> getTopXFiles(int x) throws IOException {
		if(!indexFile.exists() || FileUtility.isEmpty(indexFile))
			indexRepository();
		ArrayList<File> files = new ArrayList<File>();
		BufferedReader indexReader = new BufferedReader(new FileReader(indexFile));
		String line;
		while(x != 0 && (line = indexReader.readLine()) != null) {
			if(!line.equals(""))
				files.add(new File(line.split(",")[0]));
			x--;
		}
		indexReader.close();
		return files;
	}
}
