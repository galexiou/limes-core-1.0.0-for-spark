package org.aksw.limes.core.execution.engine;

import java.util.ArrayList;
import java.util.List;

import org.aksw.limes.core.execution.engine.filter.LinearFilter;
import org.aksw.limes.core.execution.planning.plan.NestedPlan;
import org.aksw.limes.core.execution.planning.plan.Instruction;
import org.aksw.limes.core.execution.planning.plan.Instruction.Command;
import org.aksw.limes.core.execution.planning.plan.Plan;
import org.aksw.limes.core.io.cache.Cache;
import org.aksw.limes.core.io.mapping.Mapping;
import org.aksw.limes.core.io.mapping.MemoryMapping;
import org.aksw.limes.core.measures.mapper.IMapper;
import org.aksw.limes.core.measures.mapper.MappingOperations;
import org.aksw.limes.core.measures.mapper.atomic.EDJoin;
import org.aksw.limes.core.measures.mapper.atomic.ExactMatchMapper;
import org.aksw.limes.core.measures.mapper.atomic.JaroMapper;
import org.aksw.limes.core.measures.mapper.atomic.MongeElkanMapper;
import org.aksw.limes.core.measures.mapper.atomic.OrchidMapper;
import org.aksw.limes.core.measures.mapper.atomic.PPJoinPlusPlus;
import org.aksw.limes.core.measures.mapper.atomic.SoundexMapper;
import org.aksw.limes.core.measures.mapper.atomic.TotalOrderBlockingMapper;
import org.aksw.limes.core.measures.mapper.atomic.fastngram.FastNGram;
import org.aksw.limes.core.measures.measure.MeasureFactory;

/**
 * Implements the default execution engine class. The idea is that the engine
 * gets a series of instructions in the form of an execution plan and runs these
 * instructions sequentially and returns a MemoryMemoryMapping.
 *
 * @author ngonga
 * @author kleanthi
 */
public class SimpleExecutionEngine extends ExecutionEngine {
   
	private Mapping executionMapping = new MemoryMapping();
	private static final long serialVersionUID = 1L;

	/**
     * Implements running the run operator. Assume atomic measures
     *
     * @param inst
     *            Instruction
     * @param source
     *            Source cache
     * @param target
     *            Target cache
     * @return MemoryMapping
     */
    public SimpleExecutionEngine(Cache source, Cache target, String sourceVar, String targetVar) {
	super(source, target, sourceVar, targetVar);
    }

    
    public SimpleExecutionEngine(){
    	
    }
    
    
    public void configure(Cache source, Cache target, String sourceVar, String targetVar){
    	this.source = source;
    	this.target = target;
    	this.sourceVariable = sourceVar;
    	this.targetVariable = targetVar;
    	this.buffer = null;
    	this.executionMapping.clear();
    }
    
    /**
     * Implementation of the execution of a plan. Be aware that this doesn't
     * executes complex Link Specifications.
     *
     * @param plan
     *            An execution plan
     * @return The mapping obtained from executing the plan
     */
    public Mapping execute(Plan plan) {
    	
	buffer = new ArrayList<MemoryMapping>();
	if (plan.isEmpty()) {
	    logger.info("Plan is empty. Done.");
	    return new MemoryMapping();
	}
	List<Instruction> instructions = plan.getInstructionList();
	Mapping m = new MemoryMapping();
	
	for (int i = 0; i < instructions.size(); i++) {
	    Instruction inst = instructions.get(i);
	    // get the index for writing the results
	    int index = inst.getResultIndex();
	    // first process the RUN operator
	    if (inst.getCommand().equals(Command.RUN)) {
	    m = executeRun(inst);
	    } // runs the filter operator
	    else if (inst.getCommand().equals(Command.FILTER)) {
	    m = executeFilter(inst, buffer.get(inst.getSourceMapping()));
	    } // runs set operations such as intersection,
	    else if (inst.getCommand().equals(Command.INTERSECTION)) {
	    m = executeIntersection(buffer.get(inst.getSourceMapping()), buffer.get(inst.getTargetMapping()));
	    } // union
	    else if (inst.getCommand().equals(Command.UNION)) {
	    m = executeUnion(buffer.get(inst.getSourceMapping()), buffer.get(inst.getTargetMapping()));
	    } // diff
	    else if (inst.getCommand().equals(Command.DIFF)) {
	    m = executeDifference(buffer.get(inst.getSourceMapping()), buffer.get(inst.getTargetMapping()));
	    } // xor
	    else if (inst.getCommand().equals(Command.XOR)) {
	    m = executeExclusiveOr(buffer.get(inst.getSourceMapping()), buffer.get(inst.getTargetMapping()));
	    } // end of processing. Return the indicated mapping
	    else if (inst.getCommand().equals(Command.RETURN)) {
		logger.info("Reached return command. Returning results.");
		if (buffer.isEmpty()) {
		    return m;
		}
		if (index < 0) {// return last element of buffer
		    return buffer.get(buffer.size() - 1);
		} else {
		    return buffer.get(index);
		}
	    }
	    // place resulting mapping in the buffer
	    if (index < 0) {// add the new mapping at the end of the list
		buffer.add((MemoryMapping) m);
	    } else {
		// avoid overriding places in buffer
		// by adding the result at the end
		if (index < buffer.size()) {
		    buffer.add((MemoryMapping) m);
		} else {
		    // add placeholders to ensure that the mapping can be placed
		    // where the user wanted to have it
		    // new mappings are added at the end
		    while ((index + 1) > buffer.size()) {
			buffer.add(new MemoryMapping());
		    }
		    buffer.set(index, (MemoryMapping) m);
		}

	    }
	}

	// just in case the return operator was forgotten.
	// then we return the last mapping computed
	if (buffer.isEmpty()) {
	    return new MemoryMapping();
	} else {
	    return buffer.get(buffer.size() - 1);
	}
    }

    /**
     * Implements the execution of the run operator. Assume atomic measures.
     *
     * @param inst
     *            atomic run Instruction
     * @return The mapping obtained from executing the atomic run Instruction
     */
    public Mapping executeRun(Instruction inst) {
	double threshold = Double.parseDouble(inst.getThreshold());
	// generate correct mapper
	IMapper mapper = MeasureFactory.getMapper(inst.getMeasureExpression());
	if (mapper != null)
	    return mapper.getMapping(source, target, sourceVariable, targetVariable, inst.getMeasureExpression(),
		    threshold);
	return new MemoryMapping();
    }

    /**
     * Runs the filtering operator
     *
     * @param inst
     *            filter Instruction
     * @param input
     *            Mapping that is to be filtered
     * @return filtered Mapping
     */
    public Mapping executeFilter(Instruction inst, Mapping input) {
	LinearFilter filter = new LinearFilter();
	Mapping m = new MemoryMapping();
	if (inst.getMeasureExpression() == null)
	    m = filter.filter(input, Double.parseDouble(inst.getThreshold()));
	else {
	    if (inst.getMainThreshold() != null)
		m = filter.filter(input, inst.getMeasureExpression(), Double.parseDouble(inst.getThreshold()),
			Double.parseDouble(inst.getMainThreshold()), source, target, sourceVariable, targetVariable);
	    else// original filtering
		m = filter.filter(input, inst.getMeasureExpression(), Double.parseDouble(inst.getThreshold()), source,
			target, sourceVariable, targetVariable);
	}

	return m;
    }

    /**
     * Implements the difference of Mappings
     *
     * @param m1
     *            First Mapping
     * @param m2
     *            Second Mapping
     * @return Intersection of m1 and m2
     */
    public Mapping executeDifference(Mapping m1, Mapping m2) {
	return MappingOperations.difference(m1, m2);
    }

    /**
     * Implements the exclusive or of Mappings
     *
     * @param m1
     *            First Mapping
     * @param m2
     *            Second Mapping
     * @return Intersection of m1 and m2
     */
    public Mapping executeExclusiveOr(Mapping m1, Mapping m2) {
	return MappingOperations.xor(m1, m2);
    }

    /**
     * Implements the intersection of Mappings
     *
     * @param m1
     *            First Mapping
     * @param m2
     *            Second Mapping
     * @return Intersection of m1 and m2
     */
    public Mapping executeIntersection(Mapping m1, Mapping m2) {
	return MappingOperations.intersection(m1, m2);
    }

    /**
     * Implements the union of Mappings
     *
     * @param m1
     *            First Mapping
     * @param m2
     *            Second Mapping
     * @return Intersection of m1 and m2
     */
    public Mapping executeUnion(Mapping m1, Mapping m2) {
	return MappingOperations.union(m1, m2);
    }

    /**
     * Implementation of the execution of a nested plan.
     *
     * @param plan,
     *            A nested plan
     * 
     * @return The mapping obtained from executing the plan
     */
    public Mapping execute(NestedPlan plan) {
	// empty nested plan contains nothing
	Mapping m = new MemoryMapping();
	
	if (plan.isEmpty()) {
	} // atomic nested plan just contain simple list of instructions
	else if (plan.isAtomic()) {
		m = execute((Plan) plan);
	} // nested plans contain subplans, an operator for merging the results
	  // of the subplans and a filter for filtering the results of the
	  // subplan
	else {
	    // run all the subplans
		m = execute(plan.getSubPlans().get(0));
	    Mapping m2, result = m;
	    for (int i = 1; i < plan.getSubPlans().size(); i++) {
		m2 = execute(plan.getSubPlans().get(i));
		
		if (plan.getOperator().equals(Command.INTERSECTION)) {
		    result = executeIntersection(m, m2);
		} // union
		else if (plan.getOperator().equals(Command.UNION)) {
		    result = executeUnion(m, m2);
		} // diff
		else if (plan.getOperator().equals(Command.DIFF)) {
		    result = executeDifference(m, m2);
		    // exclusive or
		} else if (plan.getOperator().equals(Command.XOR)) {
		    result = executeExclusiveOr(m, m2);
		}
		m = result;
	    }
	    // only run filtering if there is a filter indeed, else simply
	    // return MemoryMapping
	    if (plan.getFilteringInstruction() != null) {
	    	m = executeFilter(plan.getFilteringInstruction(),m);
	    }
	}
	return m;
    }

}
