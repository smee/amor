package de.asv.graph;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map.Entry;

public abstract class GraphItem {
	private int id;
	private Hashtable<String,Object> data = new Hashtable<String, Object>();
	
	protected GraphItem(int id) {
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
	
	public boolean addData(String key, Object value) {
		if(data.containsKey(key))
			return false;
		data.put(key, value);
		return true;
	}
	
	public Object getData(String key) {
		return data.get(key);
	}
	
	public Iterator<Entry<String,Object>> getDataObjects() {
		return data.entrySet().iterator();
	}
}
