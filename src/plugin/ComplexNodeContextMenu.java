package plugin;


import giny.view.NodeView;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cytoscape.CyNetwork;
import ding.view.NodeContextMenuListener;

public class ComplexNodeContextMenu implements NodeContextMenuListener{
    SideGuiActionListener sgal;
    CyNetwork net;
    
    /**
     * GeneNodeContextMenu constructor
     * @param net CyNetwork
     * @param sgal SideGuiActionListener
     */
    public ComplexNodeContextMenu(CyNetwork net, SideGuiActionListener sgal) {
        super();
        this.sgal = sgal;
        this.net = net;
    }
	
    /**
     * Adds menu items to the context menu with correct enabled states
     * @param nodeView NodeView
     * @param menu JPopupMenu
     */
    @Override
    public void addNodeContextMenuItems(NodeView nodeView, JPopupMenu menu) {
        

        final JMenuItem e = new JMenuItem("Expand complex(es)");
        final JMenuItem g = new JMenuItem("Gene heatmap");
        final JMenuItem c = new JMenuItem("Complex heatmap");
        
        
        e.addActionListener(sgal);
        e.setActionCommand("create_subnet");
        
        g.addActionListener(sgal);
        g.setActionCommand("create_gene_heatmap");
        
        c.addActionListener(sgal);
        c.setActionCommand("create_complex_heatmap");
        menu.add(e);
        menu.add(g);
        menu.add(c);
        
    }
}// end node context menu

