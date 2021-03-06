/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.limes.core.measures.mapper.atomic.topology;

import org.aksw.limes.core.io.mapping.Mapping;


import java.util.*;

import org.aksw.limes.core.measures.mapper.atomic.hausdorff.Polygon;

/**
 * Mapper that checks for the topological relation equals.
 * @author psmeros
 */
public class EqualsMapper implements TopologicRelationMapper {

    float theta = 10;

    /**
     *
     * @param sourceData Set of Polygons
     * @param targetData Set of Polygons
     * @return Mapping
     */
    @Override
    public Mapping getMapping(Set<Polygon> sourceData, Set<Polygon> targetData) {
        TopologicalRelationUtils.theta = this.theta;
        return TopologicalRelationUtils.getMapping(sourceData, targetData, TopologicalRelationUtils.EQUALS);
    }
}
