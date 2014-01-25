package plugin;

/**
 * @author YH
 * Network listener for the complex network
 * 
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.SelectEvent;
import cytoscape.data.SelectEventListener;
import javax.swing.JOptionPane;

public class RootNetworkSelectionListener implements SelectEventListener{
	
    private Map<String, ComplexEdge> complexEdges;
    private Map<String, Complex> complexes;
    private JEditorPane jepN, jepE;
    private JTree tree;
    private RootNetwork rn;
    
    /**
     * RootNetworkSelectionListener constructor
     * @param rn
     * @param complexes
     * @param complexEdges
     * @param jepN
     * @param jepE
     * @param tree 
     */
    public RootNetworkSelectionListener(RootNetwork rn, Map<String, Complex> complexes,
        Map<String, ComplexEdge> complexEdges, JEditorPane jepN, 
        JEditorPane jepE, JTree tree){

        this.rn = rn;
        this.complexEdges = complexEdges;
        this.complexes = complexes;
        this.jepN = jepN;
        this.jepE = jepE;
        this.tree = tree;
    }
    
    /**
     * Handle selection event in root network
     * @param e 
     */
    @SuppressWarnings("unchecked")
    public void onSelectEvent(SelectEvent e){
        //update BOLDNess lol
        rn.setComplexNetworkButtonsBold();
        
        if (e.getTargetType() == SelectEvent.NODE_SET) {
            CyNetwork network = Cytoscape.getCurrentNetwork();
            ((ComplexTreeListener)tree.getTreeSelectionListeners()[0]).setEnabled(false);
            Set<CyNode> setOfNodes = network.getSelectedNodes();
            

            Set<TreePath> paths = new HashSet<TreePath>();
            if(setOfNodes.size() > 0){
                StringBuilder sb = new StringBuilder();
                List<String> nodeIDs = new ArrayList<String>();
                for (CyNode n : setOfNodes){
                        nodeIDs.add(n.getIdentifier());
                }
                HelperMethods.NumericalSort(nodeIDs);
                for (Object id : nodeIDs){
                    Complex c = complexes.get(id.toString());

                    if(setOfNodes.size() < 30){
                        TreePath toAdd = External.findByName
                                (tree, new String[]{Config.ROOT_NODE_NAME,c.getName()});
                        paths.add(toAdd);
                        tree.scrollPathToVisible(toAdd);
                    }
                    String info = c.getInformation();
                    sb.append(info + "<br/><HR/><br/>");
                }
                tree.setSelectionPaths(paths.toArray(new TreePath[0]));
                jepN.setText("<html>" + sb.toString() + "</html>");
                rn.scrollNodeInfoToTop();
            }
            else{
                tree.clearSelection();
                jepN.setText("<html><font color=gray size=5 face=arial><center><i>No nodes selected</i></center></font></html>");
            }
            ((ComplexTreeListener)tree.getTreeSelectionListeners()[0]).setEnabled(true);			
        }
        else if(e.getTargetType() == SelectEvent.EDGE_SET){
            Set<CyEdge> setOfEdges = Cytoscape.getCurrentNetwork().getSelectedEdges();
            
            if(setOfEdges.size() > 0){
                StringBuilder sb = new StringBuilder();
                for(CyEdge edge : setOfEdges){
                    String source = edge.getSource().getIdentifier();
                    String target = edge.getTarget().getIdentifier();

                    if(source.compareTo(target) > 0){
                        String temp = source;
                        source = target;
                        target = temp;
                    }

                    String cat = source + "//" + target;
                    ComplexEdge ce = complexEdges.get(cat);
                    
                    //if nodes are deleted by user, this avoids null pointer error
                    if(ce == null){return;}
                    String info = ce.getInformation();
                    sb.append(info + "<br/><HR/><br/>");
                }
                jepE.setText("<html>" + sb.toString() + "</html>");
                rn.scrollEdgeInfoToTop();
            }
            else{
                jepE.setText("<html><font color=gray size=5 face=arial><center><i>No edges selected</i></center></font></html>");
            }
        }
    }
}
