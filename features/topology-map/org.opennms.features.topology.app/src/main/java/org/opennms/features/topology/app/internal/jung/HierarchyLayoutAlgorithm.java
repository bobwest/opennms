/*******************************************************************************
 * This file is part of OpenNMS(R).
 * <p>
 * Copyright (C) 2015 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2015 The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 * <p>
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 * <p>
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 * http://www.gnu.org/licenses/
 * <p>
 * For more information contact:
 * OpenNMS(R) Licensing <license@opennms.org>
 * http://www.opennms.org/
 * http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.topology.app.internal.jung;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.opennms.features.topology.api.Graph;
import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.Layout;
import org.opennms.features.topology.api.Point;
import org.opennms.features.topology.api.topo.AbstractEdge;
import org.opennms.features.topology.api.topo.AbstractVertex;
import org.opennms.features.topology.api.topo.Edge;
import org.opennms.features.topology.api.topo.Vertex;
import org.opennms.features.topology.api.topo.VertexRef;

import edu.uci.ics.jung.algorithms.layout.TreeLayout;
import edu.uci.ics.jung.algorithms.shortestpath.MinimumSpanningForest;
import edu.uci.ics.jung.graph.DelegateForest;
import edu.uci.ics.jung.graph.SparseGraph;

/**
 * <p>
 * Algorithm to transform a graph of vertices and edges into a hierarchical structure of a 2D plane.
 * It is a geometric transformation which does not change the graph but the x an y position of its vertices.
 * </p>
 * <p>
 * The algorithm generates the layout correctly if only one root element exists or if there are no vertices at all
 * If no root element exists, a random vertex from the graph is picked and defined as root.
 * If multiple root elements exist, the first one returned by getDisplayedVertices() which has no parent (= root candidate) is picked as the root element
 * </p>
 * <p>
 * The current graph to be transformed is extracted from the {@link org.opennms.features.topology.api.GraphContainer}.
 * Basically all its vertices are transformed and then the new layout is set back to the GraphContainer by executing
 * the {@link #updateLayout(GraphContainer) updateLayout} method.
 * </p>
 *
 * The Algorithm uses the JUNG library.
 * @see <a href="http://jung.sourceforge.net">JUNG Framework (Java Universal Network/Graph Framework)</a>
 *
 *
 */
public class HierarchyLayoutAlgorithm extends AbstractLayoutAlgorithm {

    /**
     * Updates the current layout by extracting the containers graph and then perform a (x,y) tranformation
     * of all vertices.
     *
     * @param graphContainer The container of the current graph. Contains all relevant informations to perform the transformation
     *                       of the {@link org.opennms.features.topology.api.Graph} by changing its {@link org.opennms.features.topology.api.Layout}
     */
    @Override
    public void updateLayout(final GraphContainer graphContainer) {
        final Graph graph = graphContainer.getGraph();
        final Layout graphLayout = graph.getLayout();
        final edu.uci.ics.jung.algorithms.layout.Layout<VertexRef, Edge> treeLayout = createTreeLayout(graph);

        applyLayoutPositions(graph.getDisplayVertices(), treeLayout, graphLayout);
    }

    private edu.uci.ics.jung.graph.Graph<VertexRef, Edge> convert(final Graph g) {
        final SparseGraph<VertexRef, Edge> sparseGraph = new SparseGraph<VertexRef, Edge>();
        for(VertexRef v : g.getDisplayVertices()) {
            sparseGraph.addVertex(v);
        }
        for(Edge e : g.getDisplayEdges()) {
            sparseGraph.addEdge(e, e.getSource().getVertex(), e.getTarget().getVertex());
        }
        return sparseGraph;
    }

    private void applyLayoutPositions(final Collection<? extends Vertex> vertices, final edu.uci.ics.jung.algorithms.layout.Layout<VertexRef, Edge> layout, final Layout graphLayout) {
        for(VertexRef v : vertices) {
            Point2D p = layout.transform(v);
            graphLayout.setLocation(v, new Point(p.getX(), p.getY()));
        }
    }

    // we may have 1 to n root vertices
    private List<Vertex> getRoots(Graph g) {
        List<Vertex> rootList = new ArrayList<Vertex>();
        Collection<Vertex> displayableVertices = g.getDisplayVertices();
        for (Vertex eachVertex : displayableVertices) {
            if (eachVertex.getParent() == null // no parent
                    || !displayableVertices.contains(eachVertex.getParent())) { // parent is not visible, include it as possible root
                rootList.add(eachVertex);
            }
        }
        return rootList;
    }

    private edu.uci.ics.jung.algorithms.layout.Layout<VertexRef, Edge> createTreeLayout(final Graph g) {
        final edu.uci.ics.jung.graph.Graph<VertexRef, Edge> jungGraph = convert(g);
        final List<Vertex> rootVertices = getRoots(g);

        // Vertex to be used as a dummy root element, if more than 1 root element exists in the provided graph
        final Vertex dummyRootVertex = new AbstractVertex(getClass().getName(), "$ROOT", "$ROOT");

        // If no rootVertices exist in the graph, null is used.
        // If only one root element exists, that vertex is used.
        // If more than 1 root element exist, the dummyRootVertex is used
        final Vertex rootVertex = rootVertices.isEmpty() ? null : rootVertices.size() > 1 ? dummyRootVertex : rootVertices.iterator().next();

        // If more than 1 root vertices exist, we add a dummy root to have
        // the tree layout set the positions correctly. However the dummy root and its vertices do not show up in the ui.
        // They are only used for the positioning

        if (rootVertices.size() > 1) {
            List<Edge> dummyRootEdges = createRootDummyEdges(rootVertices, dummyRootVertex);
            jungGraph.addVertex(dummyRootVertex);
            for (Edge e : dummyRootEdges) {
                jungGraph.addEdge(e, e.getSource().getVertex(), e.getTarget().getVertex());
            }
        }

        final MinimumSpanningForest minimumSpanningForest = new MinimumSpanningForest(jungGraph, new DelegateForest(), rootVertex);
        final TreeLayout<VertexRef, Edge> treeLayout =  new TreeLayout<>(minimumSpanningForest.getForest(), ELBOW_ROOM * 2, ELBOW_ROOM * 2);
        treeLayout.setInitializer(initializer(g.getLayout()));
        return treeLayout;
    }

    private List<Edge> createRootDummyEdges(List<Vertex> realRootVertices, Vertex dummyRootVertex) {
        List<Edge> edges = new ArrayList<>();
        for (Vertex eachRealRootVertex : realRootVertices) {
            Edge edge = new AbstractEdge(
                    getClass().getSimpleName(),
                    dummyRootVertex.getId() + ":" + eachRealRootVertex.getId(),
                    dummyRootVertex, eachRealRootVertex);
            edges.add(edge);
        }
        return edges;
    }
}