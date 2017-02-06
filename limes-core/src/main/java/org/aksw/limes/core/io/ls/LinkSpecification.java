package org.aksw.limes.core.io.ls;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.aksw.limes.core.io.parser.Parser;
import org.aksw.limes.core.measures.mapper.MappingOperations.Operator;
import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * @author Mohamed Sherif <sherif@informatik.uni-leipzig.de>
 * @author Klaus Lyko
 * @version Nov 12, 2015
 */
public class LinkSpecification implements ILinkSpecification,Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(LinkSpecification.class.getName());

    // Constants
    protected static final String MAX = "MAX";
    protected static final String OR = "OR";
    protected static final String ADD = "ADD";
    protected static final String MINUS = "MINUS";
    protected static final String XOR = "XOR";
    protected static final String MIN = "MIN";
    protected static final String AND = "AND";

    private double threshold;
    private Operator operator;
    private List<LinkSpecification> children; // children must be a list because
					      // not all operators are
					      // commutative
    private List<LinkSpecification> dependencies;// dependencies are the list of
						 // specs whose result set is
						 // included in the result set
						 // of this node
    protected String filterExpression;
    protected LinkSpecification parent;
    // just a quick hack to have lower borders for advanced threshold searches
    public double lowThreshold = 0d;
    private double quality = 0d;
    // If the LinkSpecification is atomic the measure and properties are this.
    // filterexpression: e.g. trigrams(s.label,t.label).
    protected String atomicMeasure = ""; // eg. trigrams...
    protected String prop1 = "";
    protected String prop2 = "";
    protected String treePath = "";
    protected String fullExpression = "";

    public void setAtomicFilterExpression(String atomicMeasure, String prop1, String prop2) {
	this.setAtomicMeasure(atomicMeasure);
	this.prop1 = prop1;
	this.prop2 = prop2;
	this.filterExpression = atomicMeasure + "(" + prop1 + "," + prop2 + ")";
    }

    public LinkSpecification() {
	setOperator(null);
	setChildren(null);
	setThreshold(-1);
	parent = null;
	setDependencies(null);
    }

    /**
     * Creates a spec with a measure read inside
     * 
     * @param measure
     *            String representation of the spec
     */
    public LinkSpecification(String measure, double threshold) {
	setOperator(null);
	setChildren(null);
	parent = null;
	setDependencies(null);
	readSpec(measure, threshold);
    }

    

    /**
     * Adds a child to the current node of the spec
     * 
     * @param spec
     */
    public void addChild(LinkSpecification spec) {
	if (getChildren() == null)
	    setChildren(new ArrayList<LinkSpecification>());
	getChildren().add(spec);
    }

    /**
     * Adds a child to the current node of the spec
     * 
     * @param spec
     */
    public void addDependency(LinkSpecification spec) {
	if (getDependencies() == null)
	    setDependencies(new ArrayList<LinkSpecification>());
	getDependencies().add(spec);
    }

    /**
     * Removes a dependency from the list of dependencies
     * 
     * @param spec
     *            Input spec
     */
    public void removeDependency(LinkSpecification spec) {
	if (getDependencies().contains(spec)) {
	    getDependencies().remove(spec);
	}
	if (getDependencies().isEmpty())
	    setDependencies(null);
    }

    /**
     * Checks whether a spec has dependencies
     * 
     */
    public boolean hasDependencies() {
	if (getDependencies() == null)
	    return false;
	return (!getDependencies().isEmpty());
    }

    /**
     *
     * @return True if the spec is empty, all false
     */
    public boolean isEmpty() {
	if (getThreshold() <= 0)
	    return (getThreshold() <= 0);
	if (filterExpression == null && (getChildren() == null || getChildren().isEmpty()))
	    return true;
	return false;
    }

    /**
     *
     * @return True if the spec is a leaf (has no children), else false
     */
    public boolean isAtomic() {
	if (getChildren() == null)
	    return true;
	return getChildren().isEmpty();
    }

    /**
     * Returns all leaves of the link spec
     * 
     * @return List of atomic spec, i.e., all leaves of the link spec
     */
    public void getAllChildren() {
	for (LinkSpecification child : getChildren()) {
	    if (this.getOperator() == Operator.OR) {
		logger.info(this.parent);
		this.parent = child;
		logger.info(this.parent);
	    }
	    if (!child.isAtomic())
		child.getAllChildren();
	}

    }

    /**
     *
     * Create the path of operators for each leaf spec
     */
    public void pathOfAtomic() {
	if (this.isAtomic())
	    treePath += "";
	else {
	    if (getChildren() != null) {
		for (LinkSpecification child : getChildren()) {
		    String parentPath = this.treePath;
		    if (child == getChildren().get(0)) {
			child.treePath = parentPath + ": " + getOperator() + "->left";
		    } else {
			child.treePath = parentPath + ": " + getOperator() + "->right";
		    }
		    child.pathOfAtomic();
		}
	    }
	}

    }

    

    /**
     * Reads a spec expression into its canonical form Don't forget to optimize
     * the filters by checking (if threshold_left and threshold_right >= theta,
     * then theta = 0)
     *
     * @param spec
     *            Spec expression to read
     * @param theta
     *            Global threshold
     */
    public void readSpec(String spec, double theta) {

	Parser p = new Parser(spec, getThreshold());
	if (p.isAtomic()) {
	    filterExpression = spec;
	    setThreshold(theta);
	    fullExpression = spec;
	} else {
	    LinkSpecification leftSpec = new LinkSpecification();
	    LinkSpecification rightSpec = new LinkSpecification();
	    leftSpec.parent = this;
	    rightSpec.parent = this;
	    setChildren(new ArrayList<LinkSpecification>());
	    getChildren().add(leftSpec);
	    getChildren().add(rightSpec);

	    if (p.getOperator().equalsIgnoreCase(AND)) {
		setOperator(Operator.AND);
		leftSpec.readSpec(p.getTerm1(), p.getThreshold1());
		rightSpec.readSpec(p.getTerm2(), p.getThreshold2());
		filterExpression = null;
		setThreshold(theta);
		fullExpression = "AND(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
			+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
	    } else if (p.getOperator().equalsIgnoreCase(MIN)) {
		setOperator(Operator.AND);
		leftSpec.readSpec(p.getTerm1(), theta);
		rightSpec.readSpec(p.getTerm2(), theta);
		filterExpression = null;
		setThreshold(theta);
		fullExpression = "MIN(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
			+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
	    } else if (p.getOperator().equalsIgnoreCase(OR)) {
		setOperator(Operator.OR);
		leftSpec.readSpec(p.getTerm1(), p.getThreshold1());
		rightSpec.readSpec(p.getTerm2(), p.getThreshold2());
		filterExpression = null;
		setThreshold(theta);
		fullExpression = "OR(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
			+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
	    } else if (p.getOperator().equalsIgnoreCase(MAX)) {
		setOperator(Operator.OR);
		leftSpec.readSpec(p.getTerm1(), theta);
		rightSpec.readSpec(p.getTerm2(), theta);
		filterExpression = null;
		setThreshold(theta);
		fullExpression = "MAX(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
			+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
	    } else if (p.getOperator().equalsIgnoreCase(XOR)) {
		setOperator(Operator.XOR);
		leftSpec.readSpec(p.getTerm1(), p.getThreshold1());
		rightSpec.readSpec(p.getTerm2(), p.getThreshold2());
		filterExpression = null;
		setThreshold(theta);
		fullExpression = "XOR(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
			+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
	    } else if (p.getOperator().equalsIgnoreCase(MINUS)) {
		setOperator(Operator.MINUS);
		leftSpec.readSpec(p.getTerm1(), p.getThreshold1());
		rightSpec.readSpec(p.getTerm2(), p.getThreshold2());
		filterExpression = null;
		setThreshold(theta);
		fullExpression = "MINUS(" + leftSpec.fullExpression + "|" + p.getThreshold1() + ","
			+ rightSpec.fullExpression + "|" + p.getThreshold2() + ")";
	    } else if (p.getOperator().equalsIgnoreCase(ADD)) {
		setOperator(Operator.AND);
		leftSpec.readSpec(p.getTerm1(), (theta - p.getCoef2()) / p.getCoef1());
		rightSpec.readSpec(p.getTerm2(), (theta - p.getCoef1()) / p.getCoef2());
		filterExpression = spec;
		setThreshold(theta);
		fullExpression = "ADD(" + leftSpec.fullExpression + "|" + ((theta - p.getCoef2()) / p.getCoef1()) + ","
			+ rightSpec.fullExpression + "|" + ((theta - p.getCoef1()) / p.getCoef2()) + ")";

	    }
	}
    }

    /**
     * Returns all leaves of the link spec
     * 
     * @return List of atomic spec, i.e., all leaves of the link spec
     */
    public List<LinkSpecification> getAllLeaves() {
	List<LinkSpecification> allLeaves = new ArrayList<LinkSpecification>();
	if (isAtomic()) {
	    allLeaves.add(this);
	} else {
	    for (LinkSpecification child : getChildren()) {
		allLeaves.addAll(child.getAllLeaves());
	    }
	}
	return allLeaves;
    }

    /**
     * Returns size of the spec, i.e., 1 for atomic spec, 0 for empty spec and
     * else 1 + sum of size of all children
     * 
     * @return Size of the current spec
     */
    public int size() {
	int size = 1;
	if (isEmpty()) {
	    return 0;
	}
	if (isAtomic()) {
	    return 1;
	} else {
	    for (LinkSpecification c : getChildren()) {
		size = size + c.size();
	    }
	}
	return size;
    }

    /**
     * Computes a hashCode for the current spec
     * 
     * @return Hash code
     */
    public int hashCode() {
	int res = new Random().nextInt();
	// if(this.isEmpty())
	// return 0;
	// if(this.isAtomic())
	// res =
	// filterExpression.hashCode()+Long.valueOf(Double.doubleToLongBits(threshold)).hashCode();
	return res;
	//
	//
	//
	// long bits = doubleToLongBits(thu);
	// return (int)(bits ^ (bits >>> 32));
	// return toString().hashCode();
	// return (int) System.currentTimeMillis();
    }

    /**
     * Generates a clone of the current spec
     * 
     * @return Clone of current spec
     */
    public LinkSpecification clone() {
	LinkSpecification clone = new LinkSpecification();
	clone.setThreshold(threshold);
	clone.lowThreshold = lowThreshold;
	clone.setOperator(operator);
	clone.filterExpression = filterExpression;
	clone.prop1 = prop1;
	clone.prop2 = prop2;
	clone.atomicMeasure = atomicMeasure;
	List<LinkSpecification> l = new ArrayList<LinkSpecification>();
	LinkSpecification childCopy;
	if (getChildren() != null)
	    for (LinkSpecification c : getChildren()) {
		childCopy = c.clone();
		clone.addChild(childCopy);
		childCopy.parent = clone;
		l.add(childCopy);
	    }

	return clone;
    }

    /**
     *
     * @return A string representation of the spec
     */
    @Override
    public String toString() {
	// if (parent != null) {
	// if(children != null) {
	// String str = "(" + filterExpression + ", " + threshold + ", " +
	// operator + ", "+ parent.hashCode()+")";
	// for(LinkSpecification child:children)
	// str +="\n ->"+child;
	// return str;
	// }
	//
	// else
	// return "(" + filterExpression + ", " + threshold + ", " + operator +
	// ", "+ parent.hashCode()+")";
	//// return "(" + filterExpression + ", " + threshold + ", " + operator
	// + ", " + parent.hashCode() +") -> " + children;
	// } else {
	if (getChildren() != null) {
	    String str = "(" + filterExpression + ", " + getThreshold() + ", " + getOperator() + ", null,)";
	    for (LinkSpecification child : getChildren()) {

		str += "\n  ->" + child;
	    }
	    return str;
	}

	else
	    return "(" + filterExpression + ", " + getThreshold() + ", " + getOperator() + ", null)";
	// }
    }

    /**
     *
     * @return A string representation of the spec in a single line
     */
    public String toStringOneLine() {
	if (getChildren() != null) {
	    String str = "(" + getShortendFilterExpression() + ", " + getThreshold() + ", " + getOperator()
		    + ", null,)";
	    str += "{";
	    for (LinkSpecification child : getChildren())
		str += child.toStringOneLine() + ",";
	    str += "}";
	    return str;
	}

	else
	    return "(" + getShortendFilterExpression() + ", " + getThreshold() + ", " + getOperator() + ", null)";
	// }
    }

    /**
     * Checks whether the current node is the root of the whole spec
     * 
     * @return True if root, else false
     */
    public boolean isRoot() {
	return (parent == null);
    }

    /**
     * Returns the filter expression implemented in the spec
     * 
     */
    public String getMeasure() {
	if (isAtomic())
	    return filterExpression;
	else {
	    return getOperator() + "(" + ")";
	}
    }

    

    @Override
    public boolean equals(Object other) {
	LinkSpecification o = (LinkSpecification) other;

	if (this.isAtomic() && o.isAtomic()) {
	    if (this.filterExpression == null && o.filterExpression == null)
		return true;
	    if (this.filterExpression != null && o.filterExpression == null)
		return false;
	    if (this.filterExpression == null && o.filterExpression != null)
		return false;
	    if (this.filterExpression.equalsIgnoreCase(o.filterExpression))
		return Math.abs(this.getThreshold() - o.getThreshold()) < 0.001d;
	    
	} else if(!this.isAtomic() && !o.isAtomic()){
	    if(this.getOperator() == null && o.getOperator() != null)
		return false;
	    if(this.getOperator() != null && o.getOperator() == null)
		return false;
	    if (this.getOperator() == null && o.getOperator() == null)
		return true;
	    if (this.getOperator().equals(o.getOperator())) {
		if(this.getChildren() == null && o.getChildren() == null)
		    return true;
		if(this.getChildren() != null && o.getChildren() == null)
		    return false;
		if(this.getChildren() == null && o.getChildren() != null)
		    return false;
		HashSet<LinkSpecification> hs = new HashSet<LinkSpecification>();
		if (this.getChildren() != null)
		    hs.addAll(getChildren());
		return (!hs.addAll(o.getChildren()));
	    }//not equal operators
	    return false;

	}else//one is atomic the other one is not
	    return false;
	return false;

    }

    @Override
    public int compareTo(Object o) {

	LinkSpecification other = (LinkSpecification) o;

	// logger.info("LinkSpecification.compareTo: this="+this+"
	// -other="+other);
	if (other.size() > size())
	    return -1;
	if (other.size() < size())
	    return 1;
	if (this.isEmpty() && other.isEmpty())
	    return 0;
	// size is equal
	// if(!this.isAtomic() && !other.isAtomic()) {
	// return 0;
	// }
	if (this.isAtomic() && other.isAtomic()) {
	    if (this.getThreshold() > other.getThreshold())
		return 1;
	    if (this.getThreshold() < other.getThreshold())
		return -1;
	    if (this.filterExpression == null && other.filterExpression != null)
		return -1;
	    if (this.filterExpression != null && other.filterExpression == null)
		return 1;
	    if (this.filterExpression == null && other.filterExpression == null)
		return 0;
	    return this.filterExpression.compareToIgnoreCase(other.filterExpression);
	} else { // even size, non atomic
	    // System.out.println("Comparing operators returned
	    // "+(this.operator==other.operator));
	    if (this.getOperator() == other.getOperator()) {

		// same operators

		if (getAllLeaves().size() == other.getAllLeaves().size()) {
		    List<LinkSpecification> leaves = getAllLeaves();
		    List<LinkSpecification> otherLeaves = other.getAllLeaves();
		    for (int i = 0; i < leaves.size(); i++) {
			if (leaves.get(i).compareTo(otherLeaves.get(i)) != 0)
			    return leaves.get(i).compareTo(otherLeaves.get(i));
		    }
		    return 0;
		} else
		    return getAllLeaves().size() - other.getAllLeaves().size();
	    } else {
		// non-atomic, different operators
		// logger.info("compare"+this+" \n with \n"+other);
		return this.getOperator().compareTo(other.getOperator());
	    }
	}
	// logger.info("LinkSpecification.compareTo returns 0");
	// return 0;
    }

    public String getShortendFilterExpression() {
	if (filterExpression == null)
	    return null;
	if (filterExpression.length() <= 0)
	    return "";
	if (!isAtomic())
	    return filterExpression;
	// "trigrams(x.prop1,y.prop2)" expect something like this...
	int beginProp1 = filterExpression.indexOf("(");
	int brakeProp = filterExpression.indexOf(",");
	int endProp2 = filterExpression.indexOf(")");
	String measure = filterExpression.substring(0, beginProp1);
	String prop1 = filterExpression.substring(beginProp1 + 1, brakeProp);
	String prop2 = filterExpression.substring(brakeProp + 1, endProp2);
	if (prop1.lastIndexOf("#") != -1)
	    prop1 = prop1.substring(prop1.lastIndexOf("#") + 1);
	else if (prop1.lastIndexOf("/") != -1)
	    prop1 = prop1.substring(prop1.lastIndexOf("/") + 1);
	if (prop2.lastIndexOf("#") != -1)
	    prop2 = prop2.substring(prop2.lastIndexOf("#") + 1);
	else if (prop2.lastIndexOf("/") != -1)
	    prop2 = prop2.substring(prop2.lastIndexOf("/") + 1);
	DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(Locale.ENGLISH);
	df.applyPattern("#,###,######0.00");
	return measure + "(" + prop1 + "," + prop2 + ")|" + df.format(getThreshold());
    }

    public String getFilterExpression() {
	return filterExpression;
    }

    // @Test
    // public void testEquals() {
    // LinkSpecification ls1 = new LinkSpecification();
    // LinkSpecification ls2 = new LinkSpecification();
    // ls1.filterExpression="trigrams(x.prop1,y.prop2)";
    // ls1.threshold = 0.9d;
    // ls2.filterExpression="trigrams(x.prop1,y.prop2)";
    // ls2.threshold = 0.7d;
    //// assertFalse(ls1.equals(ls2));
    //
    // LinkSpecification c1 = new LinkSpecification();
    // c1.operator=Operator.AND;
    // c1.addChild(ls1);
    // c1.addChild(ls2);
    // c1.threshold=0.9d;
    // c1.addChild(ls2);
    //
    // LinkSpecification c2 = new LinkSpecification();
    // c2.operator=Operator.AND;
    // c2.addChild(ls2);
    // c2.addChild(ls1);
    // c2.threshold=0.9d;
    // c2.addChild(ls2);
    //
    //// assertTrue(c2.equals(c1));
    //
    // TreeSet<LinkSpecification> treeset = new TreeSet<LinkSpecification>();
    // treeset.add(ls1);
    // assertFalse(treeset.add(ls2));
    //// c2.operator=Opera
    //
    // }

    @Test
    public void testEqualsComplex() {
	LinkSpecification p1 = new LinkSpecification();
	p1.setOperator(Operator.AND);
	LinkSpecification p2 = new LinkSpecification();
	p2.setOperator(Operator.AND);
	LinkSpecification c11 = new LinkSpecification();
	c11.setAtomicFilterExpression("trigrams", "p1", "p2");
	c11.setThreshold(1.0);
	LinkSpecification c12 = new LinkSpecification();
	c12.setAtomicFilterExpression("cosine", "p1", "p2");
	c12.setThreshold(0.8);

	LinkSpecification c21 = new LinkSpecification();
	c21.setAtomicFilterExpression("trigrams", "p1", "p2");
	c21.setThreshold(1.0);
	LinkSpecification c22 = new LinkSpecification();
	c22.setAtomicFilterExpression("cosine", "p1", "p2");
	c22.setThreshold(0.8);

	p1.addChild(c11);
	p1.addChild(c12);
	p2.addChild(c21);
	p1.addChild(c22);

	assertFalse(p1.equals(p2));
	assertFalse(p2.equals(p1));

	Set<LinkSpecification> set = new HashSet<LinkSpecification>();
	assertTrue(set.add(p1));
	assertTrue(set.add(p2));
	// false added (null, 0.7400000000000001, AND, null,)
	// ->(trigrams(x.prop1,y.prop1), 1.0, null, null)
	// ->(cosine(x.prop2,y.prop2), 0.7400000000000001, null, null)
	// list.size()=1
	// (null, 0.8, AND, null,)
	// ->(cosine(x.prop2,y.prop2), 0.8, null, null)
	// ->(trigrams(x.prop1,y.prop1), 0.925, null, null)
    }

    /**
     * @return the atomicMeasure
     */
    public String getAtomicMeasure() {
	if (isAtomic()) {
	    if (atomicMeasure.length() > 0)
		return atomicMeasure;
	    else
		return filterExpression.substring(0, filterExpression.indexOf("("));
	}

	else
	    return null;
    }

    /**
     * @param atomicMeasure
     *            the atomicMeasure to set
     */
    public void setAtomicMeasure(String atomicMeasure) {
	this.atomicMeasure = atomicMeasure;
    }

    /**
     * Checks of at least two leaves compare the same properties, possibly with
     * different measures though.
     * 
     * @return true if two leaves compare the same properties, possibly with
     *         different measures
     */
    public boolean containsRedundantProperties() {
	List<LinkSpecification> leaves = getAllLeaves();
	HashSet<String> props = new HashSet<String>();
	for (LinkSpecification leave : leaves) {
	    String propStr = leave.prop1 + "_" + leave.prop2;
	    if (!props.add(propStr))
		return true;
	}
	return false;
    }

    public double getThreshold() {
	return threshold;
    }

    public void setThreshold(double threshold) {
	this.threshold = threshold;
    }

    public Operator getOperator() {
	return operator;
    }

    public void setOperator(Operator operator) {
	this.operator = operator;
    }

    public List<LinkSpecification> getChildren() {
	return children;
    }

    public void setChildren(List<LinkSpecification> children) {
	this.children = children;
    }

    public List<LinkSpecification> getDependencies() {
	return dependencies;
    }

    public void setDependencies(List<LinkSpecification> dependencies) {
	this.dependencies = dependencies;
    }

    public String getFullExpression() {
	return fullExpression;
    }

    public void setFullExpression(String fullExpression) {
	this.fullExpression = fullExpression;
    }
    
    public LinkSpecification getParent() {
    	return parent;
    }
    public void setParent(LinkSpecification parent) {
    	this.parent = parent;
    }
    public String getProperty1() {
    	return this.prop1;
    }
    public String getProperty2() {
    	return this.prop2;
    }

	public double getQuality() {
		return quality;
	}

	public void setQuality(double quality) {
		this.quality = quality;
	}

}
