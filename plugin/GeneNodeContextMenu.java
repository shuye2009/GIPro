package plugin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import giny.view.NodeView;
import java.awt.event.KeyEvent;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import cytoscape.CyNetwork;
import cytoscape.CyNode;
import ding.view.NodeContextMenuListener;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;

public class GeneNodeContextMenu implements NodeContextMenuListener{
    boolean ctrlDown;
    SideGuiActionListener sgal;
    CyNetwork net;
    
    /**
     * GeneNodeContextMenu constructor
     * @param net CyNetwork
     * @param sgal SideGuiActionListener
     */
    public GeneNodeContextMenu(CyNetwork net, SideGuiActionListener sgal) {
        super();
        this.sgal = sgal;
        this.net = net;
        ctrlDown = false;
        
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyDispatcher());
    }
	
    /**
     * Adds menu items to the context menu with correct enabled states
     * @param nodeView NodeView
     * @param menu JPopupMenu
     */
    @Override
    public void addNodeContextMenuItems(NodeView nodeView, JPopupMenu menu) {
        final CyNode cn = (CyNode) nodeView.getNode();

        final JMenuItem k = new JMenuItem("Display Raw Interaction Data");
        final JMenuItem p = new JMenuItem("Display sign patterns");
        final JMenuItem t = new JMenuItem("Correlation Tables");
        final JMenuItem r = new JMenuItem("Add correlation edges");
        
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (net.getSelectedNodes().isEmpty()){
                    // No nodes are selected, so select the current node!
                    net.setSelectedNodeState(cn, true);
                    return;
                }
                if(e.getSource() == p){
                    System.out.println("Menu: Display Patterns");
                    if(ctrlDown){
                        System.out.println("ctrl is down... custom now..");
                        new CustomPatterns(sgal);
                        return;
                    }
                    sgal.makeHeatMap("query-patterns");
                }if(e.getSource() == k){
                    System.out.println("Menu: Display Raw Interaction Data");
                    sgal.makeHeatMap("query");
                }
                if(e.getSource() == t){
                    sgal.createPearsonTables();
                }
                if(e.getSource() == r){
                    //Do subnetwork modifications to add pearson
                    sgal.addCorrelationEdges();
                    
                }
                
            }
        }; 
        k.addActionListener(al);
        p.addActionListener(al);
        p.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent ke) {}

            @Override
            public void keyPressed(KeyEvent ke) {
                int key = ke.getID();
                if(key == KeyEvent.VK_CONTROL)
                    ctrlDown = true;
            }

            @Override
            public void keyReleased(KeyEvent ke) {
                int key = ke.getID();
                if(key == KeyEvent.VK_CONTROL)
                    ctrlDown = false;
            }
        });
        
        t.addActionListener(al);
        r.addActionListener(al);
        menu.add(k);
        menu.add(p);
        menu.add(t);
        menu.add(r);
        
    }
    
    private class KeyDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                int key = e.getKeyCode();
                if(key == KeyEvent.VK_CONTROL){
                    ctrlDown = true;
                }
            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
                int key = e.getKeyCode();
                if(key == KeyEvent.VK_CONTROL){
                    ctrlDown = false;   
                }
            } else if (e.getID() == KeyEvent.KEY_TYPED) {
            }
            return false;

        }
    }
}// end node context menu

