/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import ch.rakudave.suggest.JSuggestField;
import cytoscape.Cytoscape;
import cytoscape.ding.DingNetworkView;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.TreePath;

/**
 *
 * @author omarwagih
 */
public class GeneSearchBox {
    private Vector<String> vector;
    private JSuggestField suggest;
    private String selectedItem;
    private Map<String, String> vectorLower;
    private JTree tree;
    private JCheckBox zoomCheckBox;
    private boolean doZoom;
    
    Map<String, Gene> genes;
    Map<String, String> geneToOrf;
    
    public GeneSearchBox(JPanel frame, Map<String, Gene> genes, Map<String, String> geneToOrf, JTree tree){
        this.tree = tree;
        this.genes = genes;
        this.geneToOrf = geneToOrf;
        
        zoomCheckBox = new JCheckBox("Zoom into result");
        zoomCheckBox.setSelected(true);
        
        vector = new Vector();
        vectorLower = new HashMap();
        
        //Add all genes to suggestion vector
        for(String s: genes.keySet()){
            vector.add(s);
            vectorLower.put(s.toLowerCase(), null);
        }
        //Add all synonyms to suggestion vector
        for(String s: geneToOrf.keySet()){
            vector.add(s);
            vectorLower.put(s.toLowerCase(), null);
        }
        
        suggest = new JSuggestField(frame, vector);
        
        addListeners();
    }
    
    private void addListeners(){
        
        //List listeners
        JList jlist = suggest.getSuggestionList();
        jlist.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent ke) {
                int key = (int) ke.getKeyChar();
                if(key == 10){
                    JList list = suggest.getSuggestionList();
                    if(!list.isShowing()){
                        //Do nothing
                    }else{
                        suggest.setForeground(Color.BLACK);
                        selectedItem = suggest.getSuggestionList().getSelectedValue().toString();
                        search(selectedItem);
                    }
                }
            }
            @Override
            public void keyPressed(KeyEvent ke) {}
            @Override
            public void keyReleased(KeyEvent ke) {}
        });
        
        jlist.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent me) {
               
                
            }

            @Override
            public void mousePressed(MouseEvent me) {
             boolean doubleClick = me.getClickCount() == 1;
                if(!doubleClick) return;
                
                JList list = suggest.getSuggestionList();
                if(!list.isShowing()){
                    //Do nothing
                    return;
                }else{
                    suggest.setForeground(Color.BLACK);
                    selectedItem = suggest.getSuggestionList().getSelectedValue().toString();
                    suggest.setText(selectedItem);
                    search(selectedItem);
                }
            
            }

            @Override
            public void mouseReleased(MouseEvent me) {}

            @Override
            public void mouseEntered(MouseEvent me) {}

            @Override
            public void mouseExited(MouseEvent me) {}
        });
        
        suggest.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent ke) {
                int key = (int) ke.getKeyChar();
                if(key == 10){//Enter key
                    JList list = suggest.getSuggestionList();
                    if(!list.isShowing()){
                        //Do nothing
                    }else{
                        suggest.setForeground(Color.BLACK);
                        selectedItem = suggest.getSuggestionList().getSelectedValue().toString();
                        search(selectedItem);
                    }
                }
                
                
            }
            @Override
            public void keyPressed(KeyEvent ke) {}
            @Override
            public void keyReleased(KeyEvent ke) {}
        });
        
        suggest.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent de) {
                String textEntered = suggest.getText().toLowerCase();
                if(!vectorLower.containsKey(textEntered)){
                    suggest.setForeground(Color.RED);
                }else{
                    suggest.setForeground(Color.BLACK);
                }
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                if(suggest.getText().isEmpty()){
                    suggest.hideSuggest();
                }else{
                    String textEntered = suggest.getText();
                    if(!vectorLower.containsKey(textEntered)){
                        suggest.setForeground(Color.RED);
                    }else{
                        suggest.setForeground(Color.BLACK);
                    }
                }
                
            }

            @Override
            public void changedUpdate(DocumentEvent de) {}
        });
    }
    
    public void search(String geneName){
        suggest.hideSuggest();
        System.out.println("Search:"+geneName);
        if(geneToOrf.containsKey(geneName)){
            geneName = geneToOrf.get(geneName);
        }
        Gene gene = genes.get(geneName);
        Set<TreePath> paths = new HashSet<TreePath>();

        for (Complex c : gene.getComplexes()){
            TreePath path = External.findByName(tree, 
                    new String[]{Config.ROOT_NODE_NAME,c.getName()});
            paths.add(path);
        }

        //This part hilights the gene
        List<Complex> tmp = new ArrayList(gene.getComplexes());
        TreePath genePath = External.findByName(tree, new String[]{Config.ROOT_NODE_NAME,tmp.get(0).getName(),gene.getGeneName()});

        paths.add(genePath);

        tree.clearSelection();
        tree.setSelectionPaths(paths.toArray(new TreePath[0]));
        
        if(Cytoscape.getCurrentNetwork().getSelectedNodes().isEmpty()){
            JOptionPane.showMessageDialog(null, "Gene: "+geneName+" is not in this network","Message", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if(zoomCheckBox.isSelected()){
            ((DingNetworkView) Cytoscape.getCurrentNetworkView()).fitSelected();
            Cytoscape.getCurrentNetworkView().setZoom(4.0);
            Cytoscape.getCurrentNetworkView().updateView();
        }
    }
    
    public JSuggestField getSuggestField(){
        return suggest;
    }
    
    public JCheckBox getZoomCheckbox(){
        return zoomCheckBox;
    }
    
}
