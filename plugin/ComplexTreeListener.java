package plugin;


/**
 * @author YH
 * 
 * Class used to listen for selection events in the JTree at the side panel.
 *
 */

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;

public class ComplexTreeListener implements TreeSelectionListener{

    private JTree tree;

    //Set of networks registered to this listener
    private Set<CyNetwork> networks;
    private boolean enabled;
    
    /**
     * ComplexTreeListener constructor
     * @param tree JTree 
     */
    public ComplexTreeListener(JTree tree){
            this.tree = tree;
            networks = new HashSet<CyNetwork>();
            enabled = true;
    }
	
    /**
     * Looks at the nodes selected in the tree, and selects corresponding nodes
     * in networks registered to this listener
     * @param e TreeSelectionEvent
     */
    public void valueChanged(TreeSelectionEvent e) {
        if(!enabled){
            return;
        }

        TreePath[] paths = tree.getSelectionPaths();

        if(paths != null){
            Cytoscape.getCurrentNetwork().unselectAllNodes();

            for(TreePath tp : paths){
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) tp.getLastPathComponent();

                if(node != null){
                    Object o = node.getUserObject();
                    if(o instanceof Gene){
                        Gene g = (Gene)o;
                        String identifier = g.getGeneIdentifier();

                        HashSet<String> identifiers = new HashSet<String>();
                        identifiers.add(identifier);

                        for(Complex c : g.getComplexes()){
                                identifiers.add(c.getName());
                        }
                        selectNodes(identifiers);
                    }
                    else if(o instanceof Complex){
                        Complex c = (Complex)o;
                        String identifier = c.getName();

                        HashSet<String> identifiers = new HashSet<String>();
                        identifiers.add(identifier);

                        for(Gene g : c.getGenes()){
                                identifiers.add(g.getGeneIdentifier());
                        }
                        selectNodes(identifiers);		
                    }
                }
            }
        }
    }
	
    /**
     * Register a CyNetwork to this tree listener
     * @param network network to register
     */
    public void registerCyNetwork(CyNetwork network){
        networks.add(network);
    }
    
    /**
     * Sets value of enabled. If true tree listener listens for changes, otherwise
     * it does not
     * @param bool 
     */
    public void setEnabled(boolean bool){
        enabled = bool;
    }
	
    /**
     * Selects nodes in a network
     * @param identifiers Set of identifiers to select
     */
    private void selectNodes(HashSet<String> identifiers){
        CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
        for(CyNetwork cn : networks){	
            if(cn.equals(currentNetwork)){
                List<CyNode> nodes = cn.nodesList();
                Set<CyNode> nodesToSelect = new HashSet<CyNode>();

                for(CyNode n : nodes){
                        if(identifiers.contains(n.getIdentifier())){
                                nodesToSelect.add(n);
                        }
                }
                cn.setSelectedNodeState(nodesToSelect, true);
                Cytoscape.getNetworkView(cn.getIdentifier()).updateView();
                // TODO: We want to focus on the "nodesToSelect", maybe find out coordinates and zoom there?
            }
        }
    }
	
}//end class
