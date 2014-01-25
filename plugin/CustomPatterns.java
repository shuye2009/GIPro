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
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
/**
 *
 * @author omarwagih
 */
public class CustomPatterns extends JDialog{
    public static Double posCutoff, negCutoff;
    SideGuiActionListener sgal;
    
    JTextField posValue, negValue;
    JButton done;
    
    public CustomPatterns(SideGuiActionListener sgal){
        this.sgal = sgal;
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
        JLabel title = new JLabel("Display patterns");
        title.setFont(titleFont);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        //Center area of dialog
        JLabel subtitle = new JLabel("Enter a score cutoff or leave blank");
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
        done = new JButton("Display");
        done.setAlignmentX(Component.CENTER_ALIGNMENT);
        done.addActionListener(new DisplayAction());
        
        
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
        c.add(done);
        
        setLocationRelativeTo(null);
        pack();
        setSize(330,200);
        setResizable(false);
        setVisible(true);
    }
    
    private class KeyDispatcher implements KeyEventDispatcher {
        @Override
        public boolean dispatchKeyEvent(KeyEvent e) {
            if (e.getID() == KeyEvent.KEY_PRESSED) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_ENTER){
                    //done.doClick(); messes up :(
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
    public class DisplayAction implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent ae) {
            if(posValue.getText().equals("")) posCutoff = 0.0;
            else posCutoff = Double.parseDouble(posValue.getText());
            
            if(negValue.getText().equals("")) negCutoff = 0.0;
            else negCutoff = Math.abs(Double.parseDouble(negValue.getText()));
            
            sgal.makeHeatMap("query-patterns");
            
            //Reset cutoffs for normal view
            CustomPatterns.posCutoff = null;
            CustomPatterns.negCutoff = null;
            
            dispose();
        }
        
    }
    
    /**
     * Document listener for cutoff text fields 
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
    
    public boolean isValidRCutoff(String s){
        if(s.equals("")) return true;
        Double d;
        try{
            d = Double.parseDouble(s);
            return true;
        }catch(NumberFormatException e){
            return false;
        }
    }
}
