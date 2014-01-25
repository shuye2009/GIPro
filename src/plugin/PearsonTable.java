/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package plugin;

import cytoscape.CyNetwork;
import cytoscape.Cytoscape;
import cytoscape.view.CyNetworkView;
import giny.view.NodeView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import probdist.StudentDist;


/**
 *
 * @author omarwagih
 */
public class PearsonTable{
    RootNetwork rn;
    SideGuiActionListener sgal;
    
    Map<String, NodeView> stringToNodeView;
    
    List<Gene> pearsonInput;
    List<String> pearsonInputStr;
    Vector<Vector<String>> rRowData;
    Vector<Vector<String>> tRowData;
    Vector<Vector<String>> nRowData;
    Vector<String> colNames;
    private int maxLength;
    
    //Display items globally needed
    JFrame mainFrame;
    JTable rankTable;
    Vector<Vector<String>> unsortedVectorList;
    Vector<Vector<String>> sortedVectorList;
    Vector<String> rankColNames;
    
    TableModel currentTableModel;
    JTextField filterTextField;
    
    boolean ctrlDown;
    
    public PearsonTable(List<Gene> pearsonInput){
        ctrlDown = false;
        this.pearsonInput = pearsonInput;
        
        if(pearsonInput.size() < 2){
            JOptionPane.showMessageDialog(null, "Select two or more genes to show correlation tables", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if(pearsonInput.size() > 300){
            int size = pearsonInput.size();
            int comb = (Integer) (((size*size)/2) - (size/2));
            int ret = JOptionPane.showConfirmDialog(null, "<html>You are attempting to calculate pairwise pearson correlation and"
                    + " run statistical significance tests on "+size+" genes ("+comb+" pairs) against all interactions.<br> "
                    + "This may take a while and is very memory consuming. Proceed? <html>", "Memory notice", JOptionPane.YES_NO_OPTION);
            if(ret == JOptionPane.NO_OPTION) return;
        }
        
        stringToNodeView = new HashMap();
        rRowData = new Vector();
        tRowData = new Vector();
        nRowData = new Vector();
        colNames = new Vector();
        
        maxLength = 7; // length of p-value
        Cytoscape.getDesktop().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        showPanel();
        Cytoscape.getDesktop().setCursor(Cursor.getDefaultCursor());
    }
    
    public void setRootNetwork(RootNetwork rn){
        this.rn = rn;
    }
    
    public void setSideGuiActionListener(SideGuiActionListener sgal){
        this.sgal = sgal;
    }
    
    public void showPanel(){
        //For pvalue calculation
        StudentDist tdist = new StudentDist();
        HelperMethods.GeneSort(pearsonInput);
        
        for(Gene g1: pearsonInput){
            Vector<String> row = new Vector();
            Vector<String> tTestRow = new Vector();
            Vector<String> nRow = new Vector();
            
            //nRow.add(g1.getGeneName());
            colNames.add(g1.getGeneName());
            
            for(Gene g2: pearsonInput){
                //Longest gene name length for jlist
                if(g1.toString().length()>maxLength)
                    maxLength = g1.toString().length();
                if(g2.toString().length()>maxLength)
                    maxLength = g2.toString().length();
                
                //ignore self interacting genes
                if(g1 == g2){
                    row.add("-");
                    tTestRow.add("-");
                    nRow.add("-");
                    continue;
                }
                DecimalFormat df = new DecimalFormat("#.###");
                
                //calculate stuff
                Double[] rSet = g1.unfilteredPearsonCorrelationBetween(g2);
                
                Double r = rSet[0];
                int N = rSet[1].intValue();
                
                //Insufficent N
                if(r == null){
                    row.add("-");
                    tTestRow.add("-");
                    nRow.add(N+"");
                    continue;
                }
                if(N < 3){
                    row.add("*");
                    tTestRow.add("*");
                    nRow.add(N+"");
                    continue;
                }
                double tvalue = r / (Math.sqrt((1-(r*r))/(N-2)));
                
                //add to row
                row.add(df.format(r) +"");
                double pvalue = 1 - tdist.CDFvalue(tvalue, N-2);
                tTestRow.add(df.format(pvalue) +"");
                nRow.add(N+"");
            }
            rRowData.add(row);
            tRowData.add(tTestRow);
            nRowData.add(nRow);
        }
        
        mainFrame = new JFrame("GI correlation for "+pearsonInput.toString());
        
        Font headerFont = new Font("Sans Serif", Font.BOLD, 13);
        int rowHeaderWidth = colNames.get(1).length() +3; // For row headers width with extra space
        
        //# Correlation table
        JTable rTable = new JTable(rRowData, colNames);
        rTable.getTableHeader().setFont(headerFont);
        
        JScrollPane scrollPane = new JScrollPane(rTable);
        JTable rRowHeader = new RowHeaderRenderer(rTable, colNames, headerFont, rowHeaderWidth, "Correlation");
        scrollPane.setRowHeaderView(rRowHeader);
        scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, rRowHeader.getTableHeader());
        
        //# P-value table
        JTable tTestTable = new JTable(tRowData, colNames);
        tTestTable.getTableHeader().setFont(headerFont);
        
        JScrollPane scrollPane2 = new JScrollPane(tTestTable);
        JTable tRowHeader = new RowHeaderRenderer(tTestTable, colNames, headerFont, rowHeaderWidth, "P-value");
        scrollPane2.setRowHeaderView(tRowHeader);
        scrollPane2.setCorner(JScrollPane.UPPER_LEFT_CORNER, tRowHeader.getTableHeader());
        
        //Add both scroll panes to a grid layout panel
        JPanel centerPanel = new JPanel(new GridLayout(2,1)); 
        centerPanel.add(scrollPane); centerPanel.add(scrollPane2);
        
        //Rest of the stuff, (north and south panel)
        JLabel title = new JLabel("GI correlation / p-value tables");
        title.setFont(new Font("Helvetica", Font.BOLD, 18));
        title.setToolTipText
                ("<html>"
                + "<b>TOP:</b>Pairwise correlation matrices for selected genes<br> "+
                  "<b>BOTTOM:</b>Pairwise p-values from student's t-test for selected genes"
                + "</html>");
        JPanel northPanel = new JPanel();
        BoxLayout layout = new BoxLayout(northPanel, BoxLayout.Y_AXIS);
        northPanel.setLayout(layout);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        northPanel.add(title);
        
        JPanel p = new JPanel();
        p.add(Box.createHorizontalStrut(50));
        
        JPanel legend = new JPanel();
        TitledBorder legendBorder = BorderFactory.createTitledBorder("Legend");
        legend.setBorder(legendBorder);
        JLabel lab = new JLabel("<html><b>*</b> = Insufficent data</html>");
        JLabel lab2 = new JLabel("<html><b>–</b> = No data</html>");
        legend.add(lab);
        legend.add(Box.createHorizontalStrut(7));
        legend.add(lab2);
        legend.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        p.add(legend);
        
        p.add(Box.createHorizontalStrut(50));
        
        northPanel.add(p);
        
        URL rankURL = getClass().getClassLoader().getResource("images/rank.png");
        ImageIcon rankIcon = new ImageIcon(rankURL);
        
        JButton rank = new JButton("List pairwise genes");
        rank.setIcon(rankIcon);
        rank.addActionListener(new RankActionListener());
        
        URL exportURL = getClass().getClassLoader().getResource("images/export.png");
        ImageIcon exportIcon = new ImageIcon(exportURL);
        
        JButton exportTables = new JButton("Export tables");
        exportTables.setIcon(exportIcon);
        exportTables.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                String rFilePath = "";
                String tFilePath = "";
                int i;
                boolean done = false;
                
                try{
                while(!done){
                File dir = HelperMethods.showExportDialog("Choose save file name");
                if(dir == null) return;

                String fileName = dir.getName();
                String basePath = dir.getParent() + File.separator;
                if(fileName.contains(".")) 
                    basePath = basePath + fileName.substring(0, fileName.lastIndexOf("."));
                else 
                    basePath = basePath + fileName;

                rFilePath = basePath +"_correlation_matrix.txt";
                tFilePath = basePath +"_pvalue_matrix.txt";

                String conf = "<ul>";
                conf = conf + "<li>"+rFilePath+"</li>";
                conf = conf + "<li>"+tFilePath+"</li>";
                conf = conf + "</ul>";

                i = JOptionPane.showConfirmDialog(null, "<html>File(s) will be saved as<br></br>"
                        +conf+"<br></br>Do you agree?");

                done = (i == JOptionPane.YES_OPTION);
                }
                
                
                
                List<String> rTableList = new ArrayList();
                List<String> tTableList = new ArrayList();
                
                StringBuilder line0 = new StringBuilder("gene\t");
                for(String s: colNames){
                    line0.append(s+"\t");
                }
                //Remove tab
                line0.replace(line0.length()-1, line0.length(), "");
                //add to lists which will be written
                rTableList.add(line0.toString());
                tTableList.add(line0.toString());
                
                for(Vector vector: rRowData){
                    StringBuilder rLine = new StringBuilder();
                    rLine.append(colNames.get(rRowData.indexOf(vector))+"\t");
                    for(Object str: vector){
                        rLine.append(str.toString()+"\t");
                    }
                    rLine.replace(rLine.length()-1, rLine.length(), "");
                    rTableList.add(rLine.toString());
                }
                
                for(Vector vector: tRowData){
                    StringBuilder tLine = new StringBuilder();
                    tLine.append(colNames.get(tRowData.indexOf(vector))+"\t");
                    for(Object str: vector){
                        tLine.append(str.toString()+"\t");
                    }
                    tLine.replace(tLine.length()-1, tLine.length(), "");
                    tTableList.add(tLine.toString());
                }
                
                
                int r1 = HelperMethods.writeListToFile(rTableList, rFilePath);
                int r2 = HelperMethods.writeListToFile(tTableList, tFilePath);
                
                List<String> filePaths = new ArrayList();
                if(r1 != -1) filePaths.add(rFilePath);
                if(r2 != -1) filePaths.add(tFilePath);
                
                HelperMethods.showSaveSuccess(filePaths);
                }catch(Exception e){
                    JOptionPane.showMessageDialog(null, "Oops! An error occured while trying to save"
                        + " your files. Please contact the authors", "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        
        JPanel southPanel = new JPanel(); 
        southPanel.add(rank);
        southPanel.add(exportTables);
        
        //Legend
        //southPanel.add(legend);
        
        Container c = mainFrame.getContentPane();
        c.add(centerPanel);
        c.add(northPanel, BorderLayout.PAGE_START);
        c.add(southPanel, BorderLayout.PAGE_END);
        mainFrame.setVisible(true);
        mainFrame.setSize(800, 600);
        mainFrame.setLocationRelativeTo(null);
        
        // Done making panel. Populate stringToNodeView for heatmap use
       
    }
    

    
    /**
     * Calculates the Pearson un-centered correlation value between the two vectors
     * @param vectorx
     * @param vectory
     * @return Pearson correlation between two vectors, null otherwise
     */
    public static Double pearson (Vector<Double> xVect, Vector<Double> yVect){
        try{
            double meanX = 0.0, meanY = 0.0;
            for(int i = 0; i < xVect.size(); i++)
            {
                meanX += xVect.elementAt(i);
                meanY += yVect.elementAt(i);
            }

            meanX /= xVect.size();
            meanY /= yVect.size();

            double sumXY = 0.0, sumX2 = 0.0, sumY2 = 0.0;
            for(int i = 0; i < xVect.size(); i++)
            {
              sumXY += ((xVect.elementAt(i) - meanX) * (yVect.elementAt(i) - meanY));
              sumX2 += Math.pow(xVect.elementAt(i) - meanX, 2.0);
              sumY2 += Math.pow(yVect.elementAt(i) - meanY, 2.0);
            }
            
            //Divide by 0 case
            if(sumX2 == 0.0 || sumY2 == 0.0){
                return 0.0;
            }
            return (sumXY / (Math.sqrt(sumX2) * Math.sqrt(sumY2)));
        }catch(Exception e){
            System.out.println("NULL PEARSON");
            e.printStackTrace();
            return null;
        }
    }
    
    static List sortByValue(Map map) {
         List list = new LinkedList(map.entrySet());
         Collections.sort(list, new Comparator() {
              public int compare(Object o1, Object o2) {
                   return ((Comparable) ((Map.Entry) (o1)).getValue())
                  .compareTo(((Map.Entry) (o2)).getValue());
              }
         });

        return list;
    } 
    public DefaultTableModel rankAndSort(int col, boolean descending){
        //WAIT CURSOR
        Cytoscape.getDesktop().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        unsortedVectorList = new Vector();
        Set<String> unique = new HashSet();
        
        for(int i=0; i<rRowData.size(); i++){
            for(int j = 0; j<rRowData.size(); j++){
                //same gene
                if(i == j){
                    continue;
                }

                Vector rowVector = new Vector();
                //Account for empty value entered start of colNames
                String geneA = colNames.get(i);
                String geneB = colNames.get(j);

                if(unique.contains(geneA+"\t"+geneB) || unique.contains(geneB+"\t"+geneA)){
                    continue;
                } 
                
                
                String rowInfo = rRowData.get(i).get(j);
                String tTestInfo = tRowData.get(i).get(j);
                String nInfo = nRowData.get(i).get(j);

                rowVector.add(geneA); 
                rowVector.add(geneB);
                rowVector.add(rowInfo);
                rowVector.add(tTestInfo);
                rowVector.add(nInfo);

                unsortedVectorList.add(rowVector);
                unique.add(geneA+"\t"+geneB);
            }
        }
        
        //Header
        rankColNames = new Vector();
        rankColNames.add("GeneA"); 
        rankColNames.add("GeneB");
        rankColNames.add("Correlation (r)"); 
        rankColNames.add("P-value"); 
        rankColNames.add("Size (N)");
        
        //Sorting
        Map<Integer, Double> indexToValue = new HashMap();
        for(int index = 0; index<unsortedVectorList.size(); index++){
            String str = unsortedVectorList.get(index).get(col);
            Double val;
            try{ val = Double.parseDouble(str);}
            catch(NumberFormatException e){
                if(descending)
                    val = Double.NEGATIVE_INFINITY;
                else
                    val = Double.POSITIVE_INFINITY;
            }
            indexToValue.put(index, val);
        }
        
        List<Map.Entry<Integer, Double>> sortedList = sortByValue(indexToValue);
        sortedVectorList = new Vector();
        
        if(descending){
            Collections.reverse(sortedList);
        }
        for(Map.Entry<Integer, Double> i: sortedList){
                sortedVectorList.add(unsortedVectorList.get(i.getKey()));
        }
        
        //Do model and return    
        DefaultTableModel model = new DefaultTableModel(){
            @Override
            public boolean isCellEditable(int row, int col){
                return false;
            }
        }; 
        model.setDataVector(sortedVectorList, rankColNames);
        
        //Reset cursor and return
        Cytoscape.getDesktop().setCursor(Cursor.getDefaultCursor());
        return model;
    }
    
    public class RankActionListener implements ActionListener{
        final private int CORRELATION_COL = 2;
        final private int PVALUE_COL = 3;
        final private int N_COL = 4;
        
        @Override
        public void actionPerformed(ActionEvent ae) {
            final JFrame rankFrame = new JFrame("Pairwise genes");
            rankFrame.setMaximumSize(new Dimension(800, 600));
            rankFrame.setLocation(HelperMethods.getScreenWidth()/4, HelperMethods.getScreenHeight()/4);
            Container container = rankFrame.getContentPane();
            
            //Key listener for esc and enter
            KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
            manager.addKeyEventDispatcher(new KeyDispatcher());
            
            rankTable = new JTable();
            rankTable.setModel(rankAndSort(CORRELATION_COL, true));
            rankTable.getTableHeader().setFont(new Font("Sans Serif", Font.BOLD, 13));
            rankTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            //init
            currentTableModel = rankTable.getModel();
            
            
            JToolBar toolbar = new JToolBar("Toolbar", JToolBar.HORIZONTAL);
            
            //RAW BUTTON -  LATER?
            URL rawURL = getClass().getClassLoader().getResource("images/heatmap_small.png");
            ImageIcon rawIcon = new ImageIcon(rawURL);
            JButton rawButton = new JButton(rawIcon);
            rawButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    //do nothing yet
                }
            });
            
            //HEATMAP BUTTON
            URL heatmapURL = getClass().getClassLoader().getResource("images/heatmap_small.png");
            ImageIcon heatmapIcon = new ImageIcon(heatmapURL);
            JButton heatmap = new JButton(heatmapIcon);
            heatmap.setText("Display sign patterns");
            heatmap.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    //If nothing selected 
                    if(rankTable.getSelectedRow() == -1){
                        JOptionPane.showMessageDialog(null, "Select an item from the list to generate a heatmap", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    // Else
                    TableModel model = rankTable.getModel();
                    int rowIndex = rankTable.getSelectedRow();
                    try{
                        String sGene1 = model.getValueAt(rowIndex, 0).toString();
                        String sGene2 = model.getValueAt(rowIndex, 1).toString();
                        NodeView gene1 = null;
                        NodeView gene2 = null;
                        
                        CyNetwork network = Cytoscape.getCurrentNetwork();
                        CyNetworkView view = Cytoscape.getNetworkView(network.getIdentifier());
                        
                        //Find and set gene views for heatmap
                        for(Iterator i = view.getNodeViewsIterator(); i.hasNext(); ){
                            NodeView nView = (NodeView)i.next();
                            String id = nView.getLabel().getText();
                            
                            if(id.equals(sGene1))
                                gene1 = nView;
                            if(id.equals(sGene2))
                                gene2 = nView;
                            
                            //Deselect all node views
                            nView.setSelected(false);
                        }
                        gene1.setSelected(true);
                        gene2.setSelected(true);
                        
                        if(ctrlDown){
                            new CustomPatterns(sgal);
                            return;
                        }
                        sgal.makeHeatMap("query-patterns");
                        
                    }catch(Exception e){
                        System.out.println("ERROR HEATMAP COR");
                        e.printStackTrace();
                    }
                }
            });
            
            //EXPORT BUTTON
            URL exportURL = getClass().getClassLoader().getResource("images/export.png");
            ImageIcon exportIcon = new ImageIcon(exportURL);
            
            JButton export = new JButton(exportIcon);
            export.setText("Export list");
            export.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    File file = HelperMethods.showExportDialog("Choose save directory");
                    if(file == null) return;
                    
                    Calendar currentDate = Calendar.getInstance();
                    SimpleDateFormat formatter= new SimpleDateFormat("yyyy-MMM-dd_HH-mm");
                    
                    //formatter.format(currentDate.getTime()
                    
                    String path = file.getAbsolutePath() +File.separator+ "/GIPro_pairwise_list.txt";
                    
                    List<String> toWrite = new ArrayList();
                    
                    StringBuilder line = new StringBuilder();
                    for(String s: rankColNames){
                        line.append(s+"\t");
                    }
                    //remove last tab
                    line.replace(line.length()-1, line.length(), "");
                    toWrite.add(line.toString());
                    
                    TableModel model = rankTable.getModel();
                    int numRows = model.getRowCount();
                    int numCols = model.getColumnCount();
                    
                    for(int row = 0; row<numRows; row++){
                        line = new StringBuilder();
                        for(int col = 0; col<numCols; col++){
                            line.append(model.getValueAt(row, col).toString()+"\t");
                        }
                        line.replace(line.length()-1, line.length(), "");
                        //remove last tab
                        toWrite.add(line.toString());
                    }
                    List<String> paths = new ArrayList();
                    
                    int i = HelperMethods.writeListToFile(toWrite, path);
                    if(i != -1){ 
                        paths.add(path);
                        HelperMethods.showSaveSuccess(paths);
                    }
                }
            });
            //toolbar.add(rawButton);
            toolbar.add(heatmap); 
            toolbar.add(export);
            toolbar.addSeparator();
            
            final String[] sort = new String[]{"r", "p-value","N"};
            
            final JComboBox jcb = new JComboBox(sort);
            
            jcb.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent ae) {
                    //Clear filter text field upon trying to sort
                    
                    if(jcb.getSelectedItem().equals(sort[0])){
                        rankTable.setModel(rankAndSort(CORRELATION_COL, true));
                        rankTable.revalidate();
                    }
                    else if(jcb.getSelectedItem().equals(sort[1])){
                        rankTable.setModel(rankAndSort(PVALUE_COL, true));
                        rankTable.revalidate();
                    }
                    else if(jcb.getSelectedItem().equals(sort[2])){
                        rankTable.setModel(rankAndSort(N_COL, true));
                        rankTable.revalidate();
                    }
                    
                    currentTableModel = rankTable.getModel();
                }
            });
            
            //Filter genes 
            JPanel filterPanel = new JPanel();
            JLabel filterLabel = new JLabel("Filter genes: ");
            filterLabel.setToolTipText("<html>"
                    + "Enter gene names space seperated to filter ranked table<br>"
                    + "<b>Example: COG2 HFD1 STP3</b>"
                    + "<html>");
            filterLabel.setFont(new Font("Helvetica", Font.BOLD, 12));
            
            //FILTERING
            filterTextField = new JTextField(25);
            
            filterTextField.getDocument().addDocumentListener(new DocumentListener() {
                
                @Override
                public void insertUpdate(DocumentEvent de) {
                    String[] sp = filterTextField.getText().split("\\s");
                    List<String> filteredList = Arrays.asList(sp);
                    Vector<Vector<String>> filteredVectorList = new Vector();
                    
                    if(filterTextField.getText().equals("")){
                        rankTable.setModel(currentTableModel);
                        rankTable.revalidate();
                        return;
                    }
                    for(Vector vector: sortedVectorList){
                        String geneA = vector.get(0).toString();
                        String geneB = vector.get(1).toString();
                        Double N;
                        for(String str: filteredList){
                            if(geneA.startsWith(str)
                                    ||geneA.toLowerCase().startsWith(str)
                                    ||geneB.startsWith(str)
                                    ||geneB.toLowerCase().startsWith(str)){
                                
                                //Add vector
                                filteredVectorList.add(vector);
                            }
                        }
                    }
                    DefaultTableModel model = new DefaultTableModel();
                    model.setDataVector(filteredVectorList, rankColNames);
                    rankTable.setModel(model);
                    rankTable.revalidate();
                }

                @Override
                public void removeUpdate(DocumentEvent de) {
                    insertUpdate(de);
                    
                }

                @Override
                public void changedUpdate(DocumentEvent de) {
                    throw new UnsupportedOperationException("Not supported yet.");
                }
            });
            
            filterPanel.add(filterLabel);
            filterPanel.add(filterTextField);
            
            toolbar.add(new JLabel("Sort by:"));
            toolbar.add(jcb);
            
            container.add(toolbar, BorderLayout.NORTH);
            container.add(new JScrollPane(rankTable));
            container.add(filterPanel, BorderLayout.SOUTH);
            
            rankFrame.pack();
            rankFrame.setVisible(true);
        }
        
        
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
    
}
