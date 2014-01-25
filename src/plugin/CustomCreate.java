/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import cytoscape.CyNode;
import cytoscape.Cytoscape;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author omarwagih
 */
public class CustomCreate extends JDialog {
    private SideGuiActionListener sgal;
    private RootNetwork rn;
    private String type;
    
    private JLabel title, subtitle;
    private JScrollPane scrollPane;
    private JTextArea textArea;
    private JButton ok, browse, clear;
    private JTextField filePath;
    
    private Set<String> inputFileSet;
    
    public CustomCreate(SideGuiActionListener sgal, RootNetwork rn, String type){
        this.sgal = sgal;
        this.type = type;
        this.rn = rn;
        
        title = new JLabel();
        subtitle = new JLabel();
        textArea = new JTextArea(10,30);
        scrollPane = new JScrollPane(textArea);
        ok = new JButton("Create");
        browse = new JButton("Browse file");
        clear = new JButton("Clear");
        
        filePath = new JTextField(20);
        
        //Init JDialog
        init();
        
    }
    
    public void init(){
        if(type.equals("subnetwork"))
            title.setText("Create subnetwork");
        if(type.equals("gene_heatmap"))
            title.setText("Create gene heatmap");
        if(type.equals("complex_heatmap"))
            title.setText("Create complex heatmap");
        
        setLocationRelativeTo(null);
        setModal(true);
        setResizable(false);
        
        Container c = getContentPane();
        BoxLayout layout = new BoxLayout(c, BoxLayout.Y_AXIS);
        c.setLayout(layout);
        
        title.setFont(new Font("Helvetica", Font.BOLD, 16));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        subtitle.setFont(new Font("Helvetica", Font.PLAIN, 14));
        subtitle.setText("<html>Paste new-line seperated complexes below <b>or</b> </html>");
        
        filePath.setEditable(false);
        browse.addActionListener(new MyActionListener());
        clear.addActionListener(new MyActionListener());
        
        ok.setAlignmentX(Component.CENTER_ALIGNMENT);
        ok.addActionListener(new MyActionListener());
        
        //Add to jdialog's container
        c.add(title);
        c.add(new JSeparator());
        
        JPanel instructions = new JPanel();
        instructions.add(subtitle);
        instructions.add(browse);
        instructions.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(instructions);
        
        JPanel pathPanel = new JPanel();
        pathPanel.add(filePath);
        pathPanel.add(clear);
        pathPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.add(pathPanel);
        
        c.add(scrollPane);
        c.add(new JSeparator());
        
        c.add(ok);
        
        pack();
        setLocation(HelperMethods.getScreenWidth()/4, HelperMethods.getScreenHeight()/4);
        setVisible(true);
    }
    
    /**
     * Gets set of CyNodes from current network given a set of identifiers
     * @param toFind Set of identifiers
     * @return Set of CyNodes
     */
    public Set<CyNode> getCyNodesFromStringList(Set<String> toFind){
        
        List cyNodeList = Cytoscape.getCyNodesList();
        Set<CyNode> toReturn = new HashSet();
        for(Object o: cyNodeList){
            CyNode node = (CyNode) o;
            if(toFind.contains(node.getIdentifier().toLowerCase())){
                System.out.println(node.getIdentifier());
                toReturn.add(node);
            }
        }
        return toReturn;
    }
    
    public void disposeDialog(){
        this.dispose();
    }
    
    public class MyActionListener implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            Set<CyNode> nodesToUse;
            
            if(e.getSource() == clear){
                filePath.setText("");
                inputFileSet = null;
            }
            if(e.getSource() == browse){
                //Start file chooser
                JFileChooser jfc = new JFileChooser();
                jfc.setMultiSelectionEnabled(false);
                int input = jfc.showOpenDialog(null);
                if(input == JFileChooser.CANCEL_OPTION) return;
                
                //Read in file
                File inputFile = jfc.getSelectedFile();
                filePath.setText(inputFile.getAbsolutePath());
                
                inputFileSet = HelperMethods.fileToSet(inputFile, true, true);
                if(inputFileSet == null)
                    JOptionPane.showMessageDialog(null, "Failed to read file", "Error", JOptionPane.ERROR_MESSAGE);
            }
            
            
            if(e.getSource() == ok){
                //If we have an input file
                if(inputFileSet != null){
                    nodesToUse = getCyNodesFromStringList(inputFileSet);
                }
                //No input file use textarea's text
                else{
                    Set<String> textAreaLines = new HashSet();
                    String[] textAreaInput = textArea.getText().split("\n");
                    for(String s: textAreaInput)
                        textAreaLines.add(s.toLowerCase().trim());
                    nodesToUse = getCyNodesFromStringList(textAreaLines);
                }
                //No matches to our search of nodes
                if(nodesToUse.isEmpty()){
                    JOptionPane.showMessageDialog(null, "No nodes found, try again", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if(type.equals("subnetwork")){
                    //Do subnetwork from sgal
                    sgal.setNodesToUse(nodesToUse);
                    sgal.createSubNet();
                }
                
                if(type.equals("complex_heatmap")){
                        sgal.setNodesToUse(nodesToUse);
                        sgal.makeHeatMap("complex");
                }
                
                if(type.equals("gene_heatmap")){
                    if(nodesToUse.size() == 1){
                        JOptionPane.showMessageDialog(null, "Only one complex found. Try again", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    sgal.setNodesToUse(nodesToUse);
                    sgal.makeHeatMap("gene");
                }
                
                //Dispose main dialog once something is generated
                if((nodesToUse.size()>1 && type.equals("complex_heatmap"))
                        ||(nodesToUse.size()==2 && type.equals("gene_heatmap"))
                        || type.equals("subnetwork")){
                    disposeDialog();
                }
            }
        }
        
    }
}
