package plugin;

/**
 * @author YH
 * Listener for gene networks (subnets)
 * 
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import cytoscape.CyEdge;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;
import java.text.DecimalFormat;

public class SubNetSelectionListener implements SelectEventListener {

    private Map<String, Gene> genes;
    private JEditorPane jepN, jepE;
    private Set<Complex> complexesSelected;
    private JTree tree;
    
    /**
     * SubNetSelectionListener constructor
     * @param genes Map of gene names to Gene objects
     * @param jepN Node info pane
     * @param jepE Edge info pane
     * @param tree JTree
     * @param complexesSelected Selected complexes in the network
     */
    public SubNetSelectionListener(Map<String, Gene> genes, JEditorPane jepN, JEditorPane jepE, JTree tree, Set<Complex> complexesSelected) {
        this.genes = genes;
        this.jepN = jepN;
        this.jepE = jepE;
        this.complexesSelected = complexesSelected;
        this.tree = tree;
    }
    
    /**
     * Handles a node selection event
     * @param e 
     */
    @SuppressWarnings("unchecked")
    public void onSelectEvent(SelectEvent e) {
        //Update subnetwork buttons LOL
        RootNetwork.setSubnetworkNetworkButtonsBold();
        if (e.getTargetType() == SelectEvent.NODE_SET) {
            ((ComplexTreeListener)tree.getTreeSelectionListeners()[0]).setEnabled(false);
            Set<CyNode> setOfNodes = Cytoscape.getCurrentNetwork().getSelectedNodes();
            Set<TreePath> paths = new HashSet<TreePath>();

            if(setOfNodes.size() > 0){
                StringBuilder sb = new StringBuilder();

                ArrayList<Gene> selectedGenes = new ArrayList<Gene>();
                for (CyNode n : setOfNodes){
                    selectedGenes.add(genes.get(n.getIdentifier()));
                }
                
                HelperMethods.GeneSort(selectedGenes);

                for (Gene g: selectedGenes){
                    if(selectedGenes.size() < 30){
                        for(Complex c : g.getComplexes()){
                            TreePath toAdd = External.findByName(tree, new String[]{Config.ROOT_NODE_NAME,c.getName(), g.getGeneName()});
                            paths.add(toAdd);
                            tree.scrollPathToVisible(toAdd);
                        }
                    }
                    String info = g.getInformation(complexesSelected);
                    sb.append(info + "<br/><HR/><br/>");
                }

                tree.setSelectionPaths(paths.toArray(new TreePath[0]));
                jepN.setText("<html>"+sb.toString()+"</html>");
            }
            else{
                tree.clearSelection();
                jepN.setText("<html><font color=gray size=5 face=arial><center><i>No nodes selected</i></center></font></html>");
            }
            ((ComplexTreeListener)tree.getTreeSelectionListeners()[0]).setEnabled(true);
        }//End if
        else if(e.getTargetType() == SelectEvent.EDGE_SET){
            CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
            Set<CyEdge> setOfEdges = Cytoscape.getCurrentNetwork().getSelectedEdges();
            
            if(setOfEdges.size() > 0){
                ArrayList<String> edgeInfoStrings = new ArrayList<String>();

                for(CyEdge edge : setOfEdges){
                    StringBuilder sb = new StringBuilder();
                    String type = (String)Cytoscape.getEdgeAttributes().getAttribute(edge.getIdentifier(), GIProAttributeNames.EDGE_TYPE);
                    String source = edge.getSource().getIdentifier();
                    String target = edge.getTarget().getIdentifier();
                    
                    if(source.compareTo(target) > 0){
                        String temp = source;
                        source = target;
                        target = temp;
                    }

                    Gene g1 = genes.get(source);
                    Gene g2 = genes.get(target);

                    double score = 0;
                    boolean phys = false;
                    
                    if(type.equals(Config.edgePosCorrelationKey)|| type.equals(Config.edgeNegCorrelationKey)){
                        Double r = Double.parseDouble(edgeAttrs.getAttribute
                                (edge.getIdentifier(), GIProAttributeNames.EDGE_CORR).toString());	
                        DecimalFormat df = new DecimalFormat("#.####");
                        
//                        edgeInfoStrings.add
//                            ("<font face=arial>"
//                            + "<b>Edge:</b> " + g1.getGeneName() + " gg " + g2.getGeneName() + "<br>  " + 
//                             "<b>Type:</b> Correlation<br>"
//                            +"<b>Score:</b> "+ df.format(r)
//                            + "</font><br/><HR/><br/>");
                        
                        sb.append("<font face=arial>");

                        sb.append("<b>Edge: " + g1.getGeneName() + " gg " + g2.getGeneName() + "</b><br>");
                        sb.append("<b>Type:</b> Correlation" + "<br>");
                        sb.append("<b>Score:</b> "+ df.format(r)+ "<br>");

                        sb.append("</font>");
                        sb.append("<br/><HR/><br/>");

                        edgeInfoStrings.add(sb.toString());
                        continue;
                    }
                    
                    if(type.equals(Config.edgePhysicalKey)){
                        SubEdge pEdge = g1.getPhysicalEdge(g2);
                        score = pEdge.getScore();
                        phys = true;
                    }
                    else{
                        SubEdge gEdge = g1.getGeneticEdge(g2);
                        score = gEdge.getScore();
                        phys = false;
                    }		
                    sb.append("<font face=arial>");
                    sb.append("<b>Edge: " + g1.getGeneName() + " gg " + g2.getGeneName() + "</b><br>");
                    sb.append("<b>Type:,</b> " + (phys ? "Physical" : "Genetic")+ "<br>");
                    sb.append("<b>Score:</b> "+ Config.format.format(score)+ "<br>");
                    sb.append("</font>");
                    sb.append("<br/><HR/><br/>");

                    edgeInfoStrings.add(sb.toString());
                }

                // Sort the edges in alphabetical order, since they each begin 
                // with the "edge name", we can just sort alphabetically
                StringBuilder sb2 = new StringBuilder();
                Collections.sort(edgeInfoStrings);
                for (String s : edgeInfoStrings){
                        sb2.append(s);
                }
                jepE.setText("<html>" + sb2.toString() + "</html>");
            }
            else{
                jepE.setText("<html><font color=gray size=5 face=arial><center><i>No edges selected</i></center></font></html>");
            }
       }
    }//end onSelectEvent
}//end class
