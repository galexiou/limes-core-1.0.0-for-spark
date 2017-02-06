package org.aksw.limes.core.io.mapping;

import java.util.HashMap;
import java.util.TreeSet;

import com.hp.hpl.jena.vocabulary.OWL;

/**
 * @author ngonga
 * @author Mohamed Sherif <sherif@informatik.uni-leipzig.de>
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 * @author Klaus Lyko <lyko@informatik.uni-leipzig.de>
 * @version 2015-11-24
 *
 */
public abstract class Mapping implements IMapping {

	protected HashMap<String, HashMap<String, Double>> map;
	protected HashMap<Double, HashMap<String, TreeSet<String>>> reversedMap;
	protected int size;
	protected String predicate ;

	

	
	public abstract double getConfidence(String key, String value);

	public abstract void add(String key, String value, double confidence);

	public abstract void add(String key, HashMap<String, Double> hashMap);

	public abstract int size();

	public abstract Mapping reverseSourceTarget();

	public abstract int getNumberofMappings();

	public abstract boolean contains(String key, String value);

	public abstract Mapping getBestOneToNMapping();

	public abstract Mapping getSubMap(double threshold);
	
	/**
	 * Get the predicate URI, which defaults to OWL.sameAs.
	 * 
	 * @return the predicate URI
	 */
	public Mapping() {
		this.map = new HashMap<String, HashMap<String, Double>>();
		this.reversedMap = new HashMap<Double, HashMap<String, TreeSet<String>>>();
		this.size = 0;
		this.predicate = OWL.sameAs.getURI(); //default
	}

	public void clear(){
		this.map.clear();
		this.reversedMap.clear();
		this.size = 0;
		this.predicate = OWL.sameAs.getURI();
	}
	
	public HashMap<Double, HashMap<String, TreeSet<String>>> getReversedMap() {
		return reversedMap;
	}
	
	public String getPredicateURI() {
		return predicate;
	}

	public HashMap<String, HashMap<String, Double>> getMap() {
		return map;
	}

	public void setMap(HashMap<String, HashMap<String, Double>> map) {
		this.map = map;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String mappingPredicate) {
		this.predicate = mappingPredicate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((map == null) ? 0 : map.hashCode());
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result
				+ ((reversedMap == null) ? 0 : reversedMap.hashCode());
		result = prime * result + size;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Mapping other = (Mapping) obj;
		if (map == null) {
			if (other.map != null)
				return false;
		} else if (!map.equals(other.map))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		if (reversedMap == null) {
			if (other.reversedMap != null)
				return false;
		} else if (!reversedMap.equals(other.reversedMap))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	
}
