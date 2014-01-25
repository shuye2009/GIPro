// This class creates a combobox used for selecting genes. It includes filtering capabilities.

package plugin;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataListener;
import javax.swing.tree.TreePath;

@SuppressWarnings("serial")
public class GeneComboBox extends JComboBox implements ActionListener{
    Map<String, Gene> geneLowerMap;
    Map<String, String> geneToOrfLowerMap;
    /**
     * GeneComboBox constructor
     * @param genes map of gene names to gene
     * @param geneToOrf map of gene name to orf
     * @param tree JTree
     */
    public GeneComboBox(final Map<String, Gene> genes, 
            final Map<String, String> geneToOrf, final JTree tree){
            // Put genes into a vector
            List<String> geneNames = new ArrayList<String>();
            geneLowerMap = new HashMap();
            geneToOrfLowerMap = new HashMap();
            
            for (Entry<String, Gene> entry : genes.entrySet()){
                    Gene g = entry.getValue();
                    geneNames.add(g.getGeneName());
                    geneLowerMap.put(g.getGeneName().toLowerCase(), g);
            }
            for (Entry<String, String> entry : geneToOrf.entrySet()){
                geneToOrfLowerMap.put(entry.getKey().toLowerCase(), entry.getValue());
            }
            
            Collections.sort(geneNames);
            final MyComboBoxModel comboModel = new MyComboBoxModel(geneNames);

            this.setModel(comboModel);
            this.setEditable(true);

            JCBKeyListener jcbListener = new JCBKeyListener(this);
            this.getEditor().getEditorComponent().addKeyListener(jcbListener);
            
            final JTextField tf = (JTextField)this.getEditor().getEditorComponent();
            tf.getDocument().addDocumentListener(new DocumentListener(){

                public void insertUpdate(DocumentEvent e) {
                        tf.setForeground(Color.black);
                        updateList(e, "insert");
                }

                public void removeUpdate(DocumentEvent e) {
                        tf.setForeground(Color.black);
                        updateList(e, "remove");
                }

                public void changedUpdate(DocumentEvent e) {
                        tf.setForeground(Color.black);
                        // should not happen
                }

                private void updateList(DocumentEvent e, final String type){
                    final String text;
                    try{
                        text = e.getDocument().getText(0, e.getDocument().getLength());
                        hidePopup();
                        
                        if (text != null){
                            System.out.println("updateList-text not null");
                            if (type.equals("insert")){
                                comboModel.insertFilter(text);
                            }
                            else if (type.equals("remove")){
                                if (text.equals("")){
                                    return;
                                    //comboModel.addAllElements();
                                }
                                comboModel.removeFilter(text);
                            }
                            
                            repaint();
                            firePopupMenuWillBecomeVisible();
                            showPopup();
                            repaint();
                        }else{
                            System.out.println("text is null");
                        }
                    }
                    catch(Exception m){
                        System.out.println("Exception in updateList(): GeneComboBox.java");
                        m.printStackTrace();
                    }
//                    SwingUtilities.invokeLater(new Runnable() {
//                        public void run() {
//                            setModel(comboModel);
//                            revalidate();
//                            hidePopup();
//                            firePopupMenuWillBecomeVisible();
//                            showPopup();
//                        }
//                    });

                }
            });//End document listener
	
            addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                hidePopup();
                final int keyPressed = comboModel.getKey();
                System.out.println("#"+keyPressed);
                boolean b = e.getSource() instanceof JComboBox;
                if(!b) return;
                JComboBox cb = (JComboBox)e.getSource();
                
                JTextField tf = (JTextField)cb.getEditor().getEditorComponent();
                String geneName = (String)cb.getSelectedItem();
                    geneName = geneName.toLowerCase();
                String textEntered = (String) tf.getText().toLowerCase();
                System.out.println("TEXT ENTERED="+textEntered);
                System.out.println("GENE SELECTED="+geneName);
                
                boolean found = false;
                if (keyPressed != 65535 || keyPressed == 10){
                    System.out.println("<KEYPRESSED ENETERED>");
                    cb.hidePopup();
                    if (geneLowerMap.containsKey(geneName) || geneLowerMap.containsKey(textEntered) ||
                        geneToOrfLowerMap.containsKey(geneName) || geneToOrfLowerMap.containsKey(textEntered)){
                        System.out.println("<IF ENETERED>");

                        String key = null;
                        if (geneToOrfLowerMap.containsKey(geneName)){
                                key = geneToOrfLowerMap.get(geneName);
                        }else if(geneToOrfLowerMap.containsKey(textEntered)){
                            key = geneToOrfLowerMap.get(geneName);
                        }
                        
                        //Stiff havent found it
                        if(key == null){
                            if(geneLowerMap.containsKey(geneName))
                                key = geneName;
                            else if(geneLowerMap.containsKey(textEntered)){
                                key = textEntered;
                            }
                        }
                        
                        Gene gene = geneLowerMap.get(key);
                        if(gene == null){
                            gene = geneLowerMap.get(geneName);
                            
                        }
                        
                        if (gene == null){
                            System.out.println("<GENE IS NULL>");
                            geneName = "";
                            return;
                        }
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
                        found = true;
                        //((JTextField)cb.getEditor().getEditorComponent()).setText(geneName);
                        
                    }
                    else{
                        System.out.println("<ENTERED ELSE>");
                        //((JTextField)cb.getEditor().getEditorComponent()).setText("");
                        hidePopup();
                        tf.setForeground(Color.red);
                        return;
                    }
                    geneName = "";
                    textEntered = "";
                }
                comboModel.resetKey();
//                revalidate();
//                hidePopup();
//                if(keyPressed == 65535 && !tf.getText().isEmpty() && !found){
//                    System.out.println("Showing popup after");
//                    showPopup();
//                }
//                SwingUtilities.invokeLater(new Runnable() {
//                    public void run() {
//                        comboModel.resetKey();
//                        revalidate();
//                        hidePopup();
//                        if(keyPressed == 65535){
//                            showPopup();
//                        }
//                    }
//                });

            }

        });//End add action listener
    }//End constructor
}

class MyComboBoxModel implements MutableComboBoxModel {
	 
    private List<String> items;
    private List<String> hiddenItems;
    private String selectedItem = "";
    private int key = 0;
    
    /**
     * MyComboBoxModel constructor
     * @param items 
     */
    public MyComboBoxModel(List<String> items) {
        this.items = items;
        hiddenItems = new ArrayList<String>();
    }
    
    @Override
    public void removeListDataListener(ListDataListener l) {
    }
 
    @Override
    public void addListDataListener(ListDataListener l) {
    }
 
    @Override
    public Object getElementAt(int index) {
    	if (0 <= index && index < items.size())
            return items.get(index);
    	else{
            return null;
    	}
    }
 
    @Override
    public int getSize() {
        return items.size();
    }
 
    @Override
    public Object getSelectedItem() {
    	return selectedItem;
    }
 
    /**
     * If the item was selected using enter key, DONT update the selected item
     * @param anItem 
     */
    @Override
    public void setSelectedItem(Object anItem) {
    	if (key != 10){
            selectedItem = anItem.toString();
    	}
    }
 
    /**
     * Methods from MutableComboBoxModel interface
     * @param obj 
     */
    @Override
    public void addElement(Object obj) {
    	String sObj = obj.toString();
    	if (hiddenItems.contains(sObj)){
            hiddenItems.remove(sObj);
            items.add(sObj);     
    	}
    }
    
    private boolean startsWithIgnoreCase(String str1, String str2) {
        return str1.toUpperCase().startsWith(str2.toUpperCase());
    }
    
    public void insertFilter (String s){
        final String filter = s;    	
    	if (items.contains(filter) || hiddenItems.contains(filter)){
    		setSelectedItem(filter);	
    	}
        SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){
            @Override
            protected Void doInBackground() throws Exception {
                List<String> filteredItems = new ArrayList<String>();
                for (String item : items){
                    if (!startsWithIgnoreCase(item, filter)){
                        filteredItems.add(item);
                    }
                }
                for (String str : filteredItems){
                    removeElement(str);
                }
                Collections.sort(items);
                return null;
            }
				
            @Override
            protected void done(){
            }
        };
        task.execute();

    }
    
    public void removeFilter (String s){
    	final String filter = s;		    	
    	if (items.contains(filter) || hiddenItems.contains(filter))
        setSelectedItem(filter);	
        SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){
        
            @Override
            protected Void doInBackground() throws Exception {
                List<String> filteredItems = new ArrayList<String>();
                for (String item : hiddenItems){
                    if (startsWithIgnoreCase(item, filter)){
                        filteredItems.add(item);
                    }
                }
                for (String str : filteredItems){
                    addElement(str);
                }
                Collections.sort(items);
                return null;
            }
            
            @Override
            protected void done(){
            }
            
        };
        task.execute();
    }
    
 
    @Override
    public void removeElement(Object obj) {
    	String sObj = obj.toString();
    	if (items.contains(sObj)){
            items.remove(sObj);
            hiddenItems.add(sObj);
    	}
    }
    
    public void removeAllElements() {
    	hiddenItems.addAll(items);
        items.clear();
    }
    
    public void addAllElements() {
    	items.addAll(hiddenItems);
        hiddenItems.clear();
    }

    @Override
    public void removeElementAt(int index) {
    	String sObj = items.get(index);
        removeElement(sObj);
    }    
    
    public void setKey(int key){
    	this.key = key;
    }
    
    public int getKey(){
    	return key;
    }
    
    public void resetKey(){
    	key = 0;
    }
	
    @Override
    public void insertElementAt(Object obj, int index) {
        String sObj = obj.toString();
        if (hiddenItems.contains(sObj)){
            items.add(index, sObj);
            hiddenItems.remove(sObj);
        }
    }
    
}//End MyComboBoxModel class

class JCBKeyListener implements KeyListener{
    MyComboBoxModel model;
    JComboBox jcb;
    
    /**
     * JKeyListener constructor
     * @param jcb JComboBox
     */
    public JCBKeyListener (JComboBox jcb){
            this.jcb = jcb;
            model = (MyComboBoxModel) jcb.getModel();
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int key = (int) e.getKeyChar();
        model.setKey(key);
    }

    @Override
    public void keyReleased(KeyEvent e) {}
}
