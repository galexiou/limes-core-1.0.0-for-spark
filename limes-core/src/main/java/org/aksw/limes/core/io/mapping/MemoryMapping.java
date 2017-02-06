package org.aksw.limes.core.io.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.aksw.limes.core.util.RandomStringGenerator;

/**
 * This class contains the mappings computed by an organizer. Each URI from the
 * second knowledge base is mapped to the URI of instances from the first
 * knowledge base and the corresponding similarity value. This is a help class
 * for further processing that simply stores the mapping results in memory. It
 * is important to notice that if (s, t, sim1) are already in the mapping and
 * (s, t, sim2) is added then the mapping will contain (s, t, max(sim1, sim2))
 * 
 * @author ngonga
 * @author Mohamed Sherif <sherif@informatik.uni-leipzig.de>
 * @version Nov 24, 2015
 */
public class MemoryMapping extends Mapping implements Serializable {

    private static final long serialVersionUID = 1L;
   
    public MemoryMapping() {
	super();
	
    }

    public static Mapping generateRandomMapping(int mappingSize, int minSize, int maxSize) {
	Mapping m = new MemoryMapping();
	RandomStringGenerator generator = new RandomStringGenerator(minSize, maxSize);
	while (m.getNumberofMappings() < mappingSize) {
	    m.add(generator.generateString(), generator.generateString(), Math.random());
	}
	return m;
    }

    /**
     * Fills the whole content of the current map into the reversed map, which
     * uses the similarity scores as key.
     */
    public void initReversedMap() {
	reversedMap = new HashMap<Double, HashMap<String, TreeSet<String>>>();
	for (String s : map.keySet()) {
	    for (String t : map.get(s).keySet()) {
		double sim = map.get(s).get(t);
		if (!reversedMap.containsKey(sim)) {
		    reversedMap.put(sim, new HashMap<String, TreeSet<String>>());
		}
		if (!reversedMap.get(sim).containsKey(s)) {
		    reversedMap.get(sim).put(s, new TreeSet<String>());
		}
		reversedMap.get(sim).get(s).add(t);
	    }
	}
    }

    /**
     * Returns a mapping that contains all elements of the current mapping that
     * have similarity above the threshold. Basically the same as
     * filter(mapping, threshold) but should be faster
     *
     * @param threshold
     *            Similarity threshold for filtering
     * @return Mapping that contains all elements (s,t) with sim(s,t)>=threshold
     */
    public MemoryMapping getSubMap(double threshold) {
    	//System.out.println("HERE!!!!!!!!!!" + threshold);
    MemoryMapping m = new MemoryMapping();
	HashMap<String, TreeSet<String>> pairs;
	/*if (reversedMap == null) {
	    initReversedMap();
	}*/
	initReversedMap();
	//System.out.println("reversed map is "+reversedMap);
	for (Double d : reversedMap.keySet()) {
		//System.out.println("d = "+d.doubleValue());
	    if (d.doubleValue() >= threshold) {
		pairs = reversedMap.get(d);
		for (String s : pairs.keySet()) {
		    for (String t : pairs.get(s)) {
			m.add(s, t, d);
		    }
		}
	    }
	}
	return m;
    }

    /**
     * Add a batch of similarities to the mapping
     *
     * @param uri
     *            A resource from the source knowledge base
     * @param instances
     *            Map containing uris from the target knowledge base and their
     *            similarity to uri
     */
    public void add(String uri, HashMap<String, Double> instances) {
	if (!map.containsKey(uri)) {
	    map.put(uri, instances);
	    size += instances.size();
	} else {
	    Iterator<String> keyIter = instances.keySet().iterator();
	    String mappingUri;
	    while (keyIter.hasNext()) {
		mappingUri = keyIter.next();
		add(uri, mappingUri, instances.get(mappingUri));
		// size++;
	    }
	}
    }

    @Override
    public int size() {
	return size;
    }

    /**
     * Add one entry to the mapping
     *
     * @param source
     *            Uri in the source knowledge bases
     * @param target
     *            Mapping uri in the target knowledge base
     * @param similarity
     *            Similarity of uri and mappingUri
     */
    @Override
    public void add(String source, String target, double similarity) {
	if (map.containsKey(source)) {
	    // System.out.print("Found duplicate key " + uri);
	    if (map.get(source).containsKey(target)) {
		// System.out.println(" and value " + mappingUri);
		if (similarity > map.get(source).get(target)) {
		    map.get(source).put(target, similarity);
		}
	    } else {
		map.get(source).put(target, similarity);
		size++;
	    }
	} else {
	    HashMap<String, Double> help = new HashMap<String, Double>();
	    help.put(target, similarity);
	    map.put(source, help);
	    size++;
	}
    }

    /**
     * Checks whether the map contains a certain pair. If yes, its similarity is
     * returned. Else 0 is returned
     *
     * @param sourceInstance
     *            Instance from the source knowledge base
     * @param targetInstance
     *            Instance from the target knowledge base
     * @return Similarity of the two instances according to the mapping
     */
    @Override
    public double getConfidence(String sourceInstance, String targetInstance) {
	if (map.containsKey(sourceInstance)) {
	    if (map.get(sourceInstance).containsKey(targetInstance)) {
		return map.get(sourceInstance).get(targetInstance);
	    }
	}
	return 0;
    }
    
    /**
     * Checks whether a mapping contains a particular entry
     *
     * @param sourceInstance
     *            Key URI
     * @param targetInstance
     *            Value URI
     * @return True if mapping contains (key, value), else false.
     */
    @Override
    public boolean contains(String sourceInstance, String targetInstance) {
	if (map.containsKey(sourceInstance)) {
	    if (map.get(sourceInstance).containsKey(targetInstance)) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public String toString() {
	String s = "";
	for (String key : map.keySet()) {
	    for (String value : map.get(key).keySet()) {
		s = s + "[" + key + " -> (" + value + "|" + map.get(key).get(value) + ")]\n";
	    }
	}
	return s;
    }

    /**
     * Computes the total number of mappings contained in the object
     *
     * @return Total number of mappings
     */
    @Override
    public int getNumberofMappings() {
	int size = 0;
	for (String s : map.keySet()) {
	    HashMap<String, Double> m = map.get(s);
	    size = size + m.size();
	}
	return size;
    }

    /**
     * Computes the best one to n mapping for the current mapping, i.e., for
     * each element of the source, it gets the best t from target. This does not
     * mean an 1 to 1 mapping, as a t can be mapped to several s.
     *
     * @return Best one to one mapping
     */
    @Override
    public Mapping getBestOneToNMapping() {
	Mapping result = new MemoryMapping();
	for (String s : map.keySet()) {
	    double maxSim = 0;
	    Set<String> target = new HashSet<String>();
	    ;
	    for (String t : map.get(s).keySet()) {
		if (getConfidence(s, t) == maxSim) {
		    target.add(t);
		}
		if (getConfidence(s, t) > maxSim) {
		    maxSim = getConfidence(s, t);
		    target = new HashSet<String>();
		    target.add(t);
		}
	    }
	    for (String t : target) {
		result.add(s, t, maxSim);
	    }
	}
	return result;
    }

    /**
     * Reverses source and target
     *
     * @return Reversed map
     */
    public Mapping reverseSourceTarget() {
	Mapping m = new MemoryMapping();
	for (String s : map.keySet()) {
	    for (String t : map.get(s).keySet()) {
		m.add(t, s, map.get(s).get(t));
	    }
	}
	return m;
    }

    public Mapping scale(double d) {
	if (d != 0) {
	    Mapping m = new MemoryMapping();
	    for (String s : map.keySet()) {
		for (String t : map.get(s).keySet()) {
		    m.add(s, t, map.get(s).get(t) / d);
		}
	    }
	    return m;
	} else {
	    return this;
	}
    }

    public Mapping trim() {
	Mapping m = new MemoryMapping();
	for (String s : map.keySet()) {
	    for (String t : map.get(s).keySet()) {
		if (map.get(s).get(t) > 1d) {
		    m.add(s, t, 1d);
		} else {
		    m.add(s, t, map.get(s).get(t));
		}
	    }
	}
	return m;
    }

    /**
     * Returns the best one to one mapping with a bias towards the source Should
     * actually be solved with Hospital residents
     *
     * @param m
     * @return
     */
    public static Mapping getBestOneToOneMappings(Mapping m) {
	Mapping m2 = m.getBestOneToNMapping();
	m2 = m2.reverseSourceTarget();
	m2 = m2.getBestOneToNMapping();
	m2 = m2.reverseSourceTarget();
	return m2;
    }

    public String pairsOutput() {
	String s = "";
	for (String key : map.keySet()) {
	    for (String value : map.get(key).keySet()) {
		s = s + key + "," + value + "\n";
	    }
	}
	return s;
    }

    /**
     * Union of two maps: returns all pairs of sources s and targets t of this
     * map and the other. The scores will be the maximum score of either this or
     * the other.
     * 
     * @param other
     * @return
     */
    public Mapping union(Mapping other) {
	Mapping result = new MemoryMapping();
	result.map.putAll(this.map);
	result.size = size();
	for (String s : other.map.keySet()) {
	    result.add(s, other.map.get(s));
	    // for(Entry<String, Double> t : other.map.get(s).entrySet()) {
	    // if(result.contains(s, t.getKey())) {
	    // double val = Math.max(result.getSimilarity(s,
	    // t.getKey()),t.getValue());
	    // result.map.get(s).put(t.getKey(), val);
	    // } else {
	    // result.add(s, t.getKey(), t.getValue());
	    // }
	    // }
	}
	return result;
    }

}
