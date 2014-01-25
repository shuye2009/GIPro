/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.data.Semantics;
import cytoscape.task.TaskMonitor;
import cytoscape.view.CyNetworkView;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author omarwagih
 */
public class PearsonEdges extends JDialog{
    TaskMonitor taskMonitor;
    
    JDialog dialog;
    JTextField posValue, negValue;
    JButton done;
    
    List<Gene> pearsonInput;
    Map<String, CyNode> strToNode;
    Double cutoff, negCutoff, posCutoff;
    CyNetwork network;
    
    public PearsonEdges(List<Gene> pearsonInput, Map<String, CyNode> strToNode){
        this.pearsonInput = pearsonInput;
        this.strToNode = strToNode;
        this.network = Cytoscape.getCurrentNetwork();
        showFrame();
    }
    
    public void showFrame(){
        Font titleFont = new Font("Helvetica", Font.BOLD, 16);
        Font subtitleFont = new Font("Helvetica", Font.BOLD, 14);
        Font textFont = new Font("Helvetica", Font.PLAIN, 14);
        
        //Dialog stuff
        setModal(true);
        Container c = getContentPane();
        BoxLayout layout = new BoxLayout(c, BoxLayout.Y_AXIS);
        c.setLayout(layout);
        
        //North area of dialog
        JLabel title = new JLabel("Add correlation edges");
        title.setFont(titleFont);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        //Center area of dialog
        JLabel subtitle = new JLabel("Enter a correlation cutoff or leave blank");
        subtitle.setFont(subtitleFont);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel posLabel = new JLabel("Positive cutoff:");
        posLabel.setFont(textFont);
        posLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel negLabel = new JLabel("Negative cutoff:");
        negLabel.setFont(textFont);
        negLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        posValue = new JTextField(7);
        posValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        posValue.setHorizontalAlignment(JTextField.CENTER);
        negValue = new JTextField(7); 
        negValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        negValue.setHorizontalAlignment(JTextField.CENTER);
        
        posValue.getDocument().addDocumentListener(new CutoffValidator());
        negValue.getDocument().addDocumentListener(new CutoffValidator());
        
        //South area of dialog
        done = new JButton("Add edges");
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.addActionListener(new DoneAction());
        
        JButton removeAll = new JButton("Remove edges");
        removeAll.setToolTipText("Removes all generated correlation edges if any");
        removeAll.setAlignmentX(Component.CENTER_ALIGNMENT);
        removeAll.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                removePearsonEdges();
                dispose();
            }
        });
        
        JPanel southPanel = new JPanel();
        southPanel.add(done); southPanel.add(removeAll);
        southPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        //Key listener for esc and enter
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyDispatcher());
        
        //Add stuff
        c.add(new JSeparator());
        c.add(title);
        c.add(new JSeparator());
        c.add(subtitle);
        c.add(posLabel); c.add(posValue);
        c.add(negLabel); c.add(negValue);
        c.add(new JSeparator());
        c.add(southPanel);
        
        setLocationRelativeTo(null);
        pack();
        setSize(330,200);
        setResizable(false);
        setVisible(true);
    }
    
    public boolean isValidRCutoff(String s){
        if(s.equals("")) return true;
        Double d;
        try{
            d = Double.parseDouble(s);
            if(Math.abs(d) >= 0 && Math.abs(d)<= 1) return true;
            else return false;
        }catch(NumberFormatException e){
            return false;
        }
    }
    
    public void doEdges(){
        CyNetworkView view = Cytoscape.getNetworkView(network.getIdentifier());
        CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
        
        //Remove all edges if any first 
        removePearsonEdges();
        //--> go on
        //Non redundant set of pearson pairs in set
        Set<String> nr = new HashSet();
        //Do pearson correlation for all pairwise
        int numEdgesAdded = 0;
        int pairsNotInGIData = 0;
        for(Gene g1: pearsonInput){
            for(Gene g2: pearsonInput){
                String id1 = g1.getGeneIdentifier();
                String id2 = g2.getGeneIdentifier();
                //skip self gene
                if(id1.equalsIgnoreCase(id2)) continue;
                //Skip redundant
                if(nr.contains(id1+"\t"+id2) || nr.contains(id2+"\t"+id1)) continue;
                
                /*Do pearson*/
                Double[] rSet = g1.unfilteredPearsonCorrelationBetween(g2);
                Double r = rSet[0];
                int N = rSet[1].intValue();
                
                //ignore pairs with insignificant data
                if(r == null || N < 3) continue;
                
                //Skip r's that do not meet cutoff 
                
                //R is positive and does not meet cutofff
                if(r>=0 && r < posCutoff) continue;
                //R is negative and does not meet cutoff 
                if(r<0 && Math.abs(r)<negCutoff) continue;
                
                //All good, do edge stuff
                CyEdge edge;
                //If pos add pos attributes
                if(r>=0){
                    edge = Cytoscape.getCyEdge(strToNode.get(id1), strToNode.get(id2), Semantics.INTERACTION, "rp", true);
                    network.addEdge(edge);
                    edgeAttrs.setAttribute(edge.getIdentifier(),GIProAttributeNames.EDGE_TYPE, "rp");
                    numEdgesAdded++;
                }
                //Negative, add negative attributes
                else{
                    edge = Cytoscape.getCyEdge(strToNode.get(id1), strToNode.get(id2), Semantics.INTERACTION, "rn", true);
                    network.addEdge(edge);
                    edgeAttrs.setAttribute(edge.getIdentifier(),GIProAttributeNames.EDGE_TYPE, "rn");
                    numEdgesAdded++;
                }
                
                //Edge attribute for label
                DecimalFormat df = new DecimalFormat("#.###");
                edgeAttrs.setAttribute(edge.getIdentifier(), 
                                    GIProAttributeNames.EDGE_CORR, df.format(r));
                
                //Non redundant
                nr.add(id1+"\t"+id2);
            }
        }
        Visualization.refreshLabels();
        //Refresh view
        view.redrawGraph(true, false);
        view.updateView();
        
        if(numEdgesAdded == 0){
            JOptionPane.showMessageDialog(null, 
                            "No correlation edges were added. Please ensure that cutoffs are not too stringent or that selected genes have sufficent GI data",
                            "Warning", JOptionPane.WARNING_MESSAGE);
        }
        
    }

    /**
     * Removes all pearson edges from current network
     */
    public void removePearsonEdges(){
        CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
        //Remove all available edges at each selection run
        Iterator itr = network.edgesIterator();
        while(itr.hasNext()){
            CyEdge e = (CyEdge)itr.next();
            String type = edgeAttrs.getAttribute(e.getIdentifier(), Semantics.INTERACTION).toString();
            if(type.equals(Config.edgePosCorrelationKey)|| type.equals(Config.edgeNegCorrelationKey)){
                network.removeEdge(e.getRootGraphIndex(), true);
            }
        }
    }
    
    /**
     * Document listener for cutoff textfields 
     */
    public class CutoffValidator implements DocumentListener{

        @Override
            public void insertUpdate(DocumentEvent de) {
                if(isValidRCutoff(posValue.getText())&& isValidRCutoff(negValue.getText()))
                    done.setEnabled(true);
                else{
                    done.setEnabled(false);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                insertUpdate(de);
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                insertUpdate(de);
            }
        
    }
    
    /**
     * Add edges button action listener
     */
    public class DoneAction implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent ae) {
            Cytoscape.getDesktop().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            
            if(posValue.getText().equals("")) posCutoff = 0.0;
            else posCutoff = Double.parseDouble(posValue.getText());

            if(negValue.getText().equals("")) negCutoff = 0.0;
            else negCutoff = Math.abs(Double.parseDouble(negValue.getText()));

            doEdges();
            dispose();
            
            
            Cytoscape.getDesktop().setCursor(Cursor.getDefaultCursor());
            setCursor(Cursor.getDefaultCursor());
        }
        
    }
    
    /**
     * Listens for ENTER and ESC
     */
    private class KeyDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_ENTER){
                    //done.doClick();
                }
                if (key == KeyEvent.VK_ESCAPE){
                    dispose();
                }

            } else if (e.getID() == KeyEvent.KEY_RELEASED) {
            } else if (e.getID() == KeyEvent.KEY_TYPED) {
            }
            return false;

        }
    }
    
}
