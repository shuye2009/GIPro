package plugin;

/**
 * @author YH
 * Action listener for buttons at side c1.
 */

import cytoscape.task.TaskMonitor;
import guiICTools.MyWizardPanelDataStorage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;


import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.task.Task;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanelImp;
import edu.stanford.genetics.treeview.ViewFrame;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.List;



public class SideGuiActionListener implements ActionListener{
	private Map<String, Gene> genes;
	private Map<String, Complex> complexes;
	private Map<String, ComplexEdge> complexEdges;
	private JEditorPane jepN, jepE;
	private CyNetwork network;
	private ComplexTreeListener ctl;
	private JTree tree;
	private MyWizardPanelDataStorage mwpds;
	private double betweenPValueCutoff;
        private Map<String, Set<Complex>> networkComplexMap;
        private RootNetwork rn;
	
	private final String corrOrInt = "relations";
        
        Toolkit tk = Toolkit.getDefaultToolkit();  
        int screenX = ((int) tk.getScreenSize().getWidth());  
        int screenY = ((int) tk.getScreenSize().getHeight());
        
        TaskMonitor taskMonitor;
        
        Set<CyNode> nodesToUse;
        ArrayList<String> nodesToUseString;
        
        public static ExportResults exportDialog;
	
        /**
         * SideGuiActionListener constructor
         * @param complexes Map<String, Complex> 
         * @param genes Map<String, Gene>
         * @param complexEdges Map<String, ComlexEdges>
         * @param jepN Node JEditorPane
         * @param jepE Edge JEditorPane
         * @param tree JTree
         * @param ctl ComplexTreeListener
         * @param betweenPValueCutoff Between Value cutoff
         * @param mwpds MyWizardPanelDataStorage
         * @param rn RootNetwork
         */
	public SideGuiActionListener(Map<String, Complex> complexes, Map<String, Gene> genes,
                Map<String, ComplexEdge> complexEdges, JEditorPane jepN, JEditorPane jepE, JTree tree, 
                ComplexTreeListener ctl, double betweenPValueCutoff, MyWizardPanelDataStorage mwpds, RootNetwork rn) {
	    this.genes = genes;
	    this.complexes = complexes;
	    this.complexEdges = complexEdges;
	    this.jepN = jepN;
	    this.jepE = jepE;
	    this.ctl = ctl;
	    this.tree = tree;
	    this.betweenPValueCutoff = betweenPValueCutoff;
	    this.mwpds = mwpds;
            this.rn = rn;
            network = Cytoscape.getCurrentNetwork();
            networkComplexMap = rn.getNetworkComplexMap();
        }

        public void actionPerformed(ActionEvent e) {
            network = Cytoscape.getCurrentNetwork();
            setNodesToUse(network.getSelectedNodes());
            
	    String actionCommand = e.getActionCommand();
            
	    if("create_subnet".equals(actionCommand)){
                if(Cytoscape.getCurrentNetwork().getSelectedNodes().isEmpty()){
                    JOptionPane.showMessageDialog(null, 
                            "You must select at least one complex node to create an expanded view!",
                            "Invalid Request", JOptionPane.ERROR_MESSAGE);
                }else{
                    createSubNet();
                }
	    }
                    
	    else if("create_gene_heatmap".equals(actionCommand)){
//	    	if (network.getSelectedNodes().size() != 2){
//                    JOptionPane.showMessageDialog(null, 
//                            "You must select two complex nodes in order to create a gene heatmap!",
//                            "Invalid Request", JOptionPane.ERROR_MESSAGE);
//                }
//	    	else{
                    makeHeatMap("gene");
//	    	}
	    }
                    
	    else if ("create_complex_heatmap".equals(actionCommand)){
	    	if (network.getSelectedNodes().size() < 2){
                    JOptionPane.showMessageDialog(null, 
                            "You must select at least two nodes in order to create a complex heatmap!",
                            "Invalid Request", JOptionPane.ERROR_MESSAGE);
		}
	    	else{
                    makeHeatMap("complex");
	    	}
	    }
                    
	    else if("create_query_heatmap".equals(actionCommand)){
	    	if (network.getSelectedNodes().isEmpty()){
                    JOptionPane.showMessageDialog(null, 
                            "You must select a gene node to create a heatmap!"
                            , "Invalid Request", JOptionPane.ERROR_MESSAGE);
                }
	    	else{
                    makeHeatMap("query");
	    	}
	    }
	    else if("output".equals(actionCommand)){
                exportResults();
	    }
            else if("output_dialog".equals(actionCommand)){
                exportDialog = new ExportResults(this);
            }
	    else if("histogram".equals(actionCommand)){	
	    	rn.createHistogram();
	    }
        }

	public void setNetwork(CyNetwork network){
            this.network = network;
	}
        
        public void setNodesToUse(Set<CyNode> nodesToUse){
            this.nodesToUse = nodesToUse;
            
            nodesToUseString = new ArrayList();
            for(CyNode node: nodesToUse){
                nodesToUseString.add(node.getIdentifier());
            }
            
        }
	
        /**
         * Shows JFileChooser and exports all info by executing
         * dumpBetweenInformation() , dumpWithinInformation() and
         * dumpComplexEnrichmentInformation()
         */
	private void exportResults(){
            try{
                System.out.println("Initiating memory transfer protocal...");
                boolean selectedOnly = ExportResults.getExportExportSelectedOnly();
                boolean withinSelected = ExportResults.getExportWithin();
                boolean betweenSelected = ExportResults.getExportBetween();
                boolean matrixSelected = ExportResults.getExportComplexMatrix();
                
                if(!withinSelected && !betweenSelected && !matrixSelected)
                    return;
                String withinPath = "";
                String betweenPath = "";
                String matrixPath = "";
                
                int i;
                boolean done = false;
                while(!done){
                File dir = HelperMethods.showExportDialog("Choose save file name");
                if(dir == null) return;
                
                String fileName = dir.getName();
                String basePath = dir.getParent() + File.separator;
                if(fileName.contains(".")) 
                    basePath = basePath + fileName.substring(0, fileName.lastIndexOf("."));
                else 
                    basePath = basePath + fileName;
                
                withinPath = basePath +"_within.txt";
                betweenPath = basePath +"_between.txt";
                matrixPath = basePath +"_matrix.txt";
                
                String conf = "<ul>";
                if(withinSelected) conf = conf + "<li>"+withinPath+"</li>";
                if(betweenSelected) conf = conf + "<li>"+betweenPath+"</li>";
                if(matrixSelected) conf = conf + "<li>"+matrixPath+"</li>";
                conf = conf + "</ul>";
                
                i = JOptionPane.showConfirmDialog(null, "<html>File(s) will be saved as<br></br>"
                        +conf+"<br></br>Do you agree?");
                
                done = (i == JOptionPane.YES_OPTION);
                }
                
                
                
                
                List<String> paths = new ArrayList();
                //String dirPath = dir.getAbsolutePath();
                
                if(withinSelected){
                    dumpWithinInformation(withinPath, selectedOnly);
                    paths.add(withinPath);
                }
                if(betweenSelected){
                    dumpBetweenInformation(betweenPath, selectedOnly);
                    paths.add(betweenPath);
                }
                if(matrixSelected){
                    dumpComplexEnrichmentInformation(matrixPath, selectedOnly);
                    paths.add(matrixPath);
                }
                
                HelperMethods.showSaveSuccess(paths);
                System.out.println("Memory transfer completed.");
            }
            catch (IOException e){
                JOptionPane.showMessageDialog(null, "Oops! An error occured while trying to save"
                        + " your files. Please contact the authors", "Error", 
                        JOptionPane.ERROR_MESSAGE);
            }
	}
	
        /**
         * Add between complex information to file for export button
         * @param dir Exported file 
         * @throws IOException 
         */
	private void dumpBetweenInformation(String path, boolean onlySelected) throws IOException{

            FileWriter fw = new FileWriter(path);
            BufferedWriter bw = new BufferedWriter(fw);

            String headers = "Complex1\tComplex2\tNumber of Pairs\tActual Number of Pairs\tpos " + corrOrInt + 
                                            "\tpos pvalue\tneg " + corrOrInt + "\tneg pvalue\tzero " + corrOrInt +"\tSignificance\n";
            bw.write(headers);
            bw.flush();

            List<String> orderInteractions = new ArrayList<String>();
            orderInteractions.addAll(complexEdges.keySet());
            //HelperMethods.InteractionSort(orderInteractions);
            Collections.sort(orderInteractions);
            for(String interaction : orderInteractions){
                ComplexEdge ce = complexEdges.get(interaction);
                
                //Only needed complexes
                if(onlySelected){
                    if(! (nodesToUseString.contains(ce.getSource().getName()) 
                            && nodesToUseString.contains(ce.getTarget().getName()))){
                        continue;
                    }
                }
                String twoComplexes = interaction.replace("//", "\t");
                int numberPairs = ce.totalNumberEdges();
                int actualNumberOfPairs = ce.actualNumberEdges();
                int posRelations = ce.posRelations();
                int negRelations = ce.negRelations();
                String posPValue = "";
                String negPValue = "";

                if (ce.posPValue() != null){
                        posPValue = ce.posPValue().toString();
                }
                if (ce.negPValue() != null){
                        negPValue = ce.negPValue().toString();
                }
                int zeroRelations = ce.zeroRelations();

                String significance = HelperMethods.getSignificanceString(ce.getSignificance());

                String line = twoComplexes + "\t" +  numberPairs + "\t" +
                        actualNumberOfPairs + "\t" + posRelations + "\t" + 
                        posPValue + "\t" + negRelations + "\t" + negPValue + "\t" +
                        zeroRelations + "\t" + significance + "\n";
                bw.write(line);
                bw.flush();
            }
		
		bw.close();
		fw.close();
	}
	
        /**
         * Add Within complex information to file for export button
         * @param dir
         * @throws IOException 
         */
	private void dumpWithinInformation(String path, boolean onlySelected) throws IOException{
            FileWriter fw = new FileWriter(path);
            BufferedWriter bw = new BufferedWriter(fw);

            String headers = "name\tnumber of genes\tactual number of genes\tfull list\tinteracting list\tinteractions\t" +
                            "pos" +	corrOrInt + "\tpos pvalue\t" + "neg" +	corrOrInt + "\tneg pvalue\t" +
                            "zero" + corrOrInt + "\tSignificance\n";
            bw.write(headers);
            bw.flush();

            ArrayList<Complex> orderedComplexes = new ArrayList<Complex>();
            orderedComplexes.addAll(complexes.values());
            HelperMethods.ComplexSort(orderedComplexes);
            for(Complex c : orderedComplexes){
                String complexName = c.getName();
                
                //must be in selected nodes if we want only selected
                if(onlySelected){
                    if(! nodesToUseString.contains(complexName)){
                        continue;
                    }
                }
                String numberOfGenes = Integer.toString(c.getGenes().size());
                String actualNumberOfGenes = Integer.toString(c.getActualGenes().size());
                String fullList = setToString(c.getGenes());
                String interactingList = setToString(c.getActualGenes());
                int posRelations = c.posRelations();
                int negRelations = c.negRelations();
                String posPValue = "";
                String negPValue = "";

                if (c.posPValue() != null){
                        posPValue = c.posPValue().toString();
                }
                if (c.negPValue() != null){
                        negPValue = c.negPValue().toString();
                }
                int zeroRelations = c.zeroRelations();
                String significance = HelperMethods.getSignificanceString(c.getSignificance());

                String line = complexName + "\t" +  numberOfGenes + "\t" + 
                        actualNumberOfGenes + "\t" + fullList + "\t" + 
                        interactingList + "\t" + c.getNumInteractions() + "\t" +
                        posRelations + "\t" + posPValue + "\t" + negRelations + "\t" 
                        + negPValue + "\t" + zeroRelations + "\t" + significance + "\n";
                bw.write(line);
                bw.flush();
            }
            bw.close();
            fw.close();
	}
	
        /**
         * Adds complex enrichment information to file for export button
         * @param dir Exported file 
         * @throws IOException 
         */
	private void dumpComplexEnrichmentInformation(String path, boolean onlySelected) throws IOException{

            FileWriter fw = new FileWriter(path);
            BufferedWriter bw = new BufferedWriter(fw);

            ArrayList<Complex> comps = new ArrayList<Complex>();
            // Select all the complexes
            for (Entry<String, Complex> entry : complexes.entrySet()){
                    Complex c = entry.getValue();
                    comps.add(c);
            }

            HelperMethods.ComplexSort(comps);
		
            try{
                bw.write("complex_name\t");
                for (Complex c : comps){
                        bw.write(c.getName() + "\t");
                }
                bw.write("\n");
                bw.flush();
                Random rand = new Random();
                
                for (Complex comp1 : comps){
                    
                    //Skip if custom
                    if(onlySelected){
                        if(!nodesToUseString.contains(comp1.toString()))
                            continue;
                    }
                    bw.write(comp1.getName() + "\t");
                    for (Complex comp2 : comps){
                        ComplexEdge ce;
                        if (comp1.compareTo(comp2) < 0)
                            ce = complexEdges.get(comp1.getName() + "//" + comp2.getName());
                        else
                            ce = complexEdges.get(comp2.getName() + "//" + comp1.getName());
                        if (ce == null){
                            bw.write("-\t");
                        }
                        else{
                            Double posPValue = ce.posPValue();
                            Double negPValue = ce.negPValue();
                            if(posPValue != null 
                                    && posPValue < betweenPValueCutoff 
                                    && negPValue != null && negPValue < betweenPValueCutoff){
                                    
                                // Randomly assign both as either positive or negative
                                if (rand.nextDouble() > 0.5){
                                    bw.write(ComplexHeatMap.POS_SIGNIFICANCE+"\t");
                                }
                                else{
                                    bw.write(ComplexHeatMap.NEG_SIGNIFICANCE+"\t");
                                }
                            }
                            else if (posPValue != null && posPValue < betweenPValueCutoff){
                                bw.write(ComplexHeatMap.POS_SIGNIFICANCE+"\t");
                            }
                            else if (negPValue != null && negPValue < betweenPValueCutoff){
                                bw.write(ComplexHeatMap.NEG_SIGNIFICANCE+"\t");
                            }
                        }	
                    }	
                    bw.write("\n");
                    bw.flush();
                }//end for
            }
            catch(IOException e){
                System.err.println("Error: dumpComplexEnrichmentInformation()");
            }
            bw.close();
            fw.close();
	}
	
        /**
         * Create necessary heat-maps depending on which button is clicked
         * @param type 
         */
	@SuppressWarnings("unchecked")
	public void makeHeatMap(String type){
            //Set<CyNode> nodesToUse;
            CyNetwork currentNetwork = Cytoscape.getCurrentNetwork();
            Set selNodes = currentNetwork.getSelectedNodes();
            
            
            setNodesToUse(selNodes);

            String title = "";
            
            System.out.println("finished printing");
            //GENE HEATMAP
            if (type.equals("gene")){
                if(nodesToUseString.isEmpty()){
                        JOptionPane.showMessageDialog(null, "You must select at least one complex to generate a gene heatmap", 
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                }
                
                List<Complex> listOfComplexes = new ArrayList();
                for(String cpxName: nodesToUseString){
                    listOfComplexes.add(complexes.get(cpxName));
                }

                //GeneHeatMap tvheatmap = new GeneHeatMap(complex1, complex2);
                GeneHeatMapMulti tvheatmap = new GeneHeatMapMulti(listOfComplexes);
                if(tvheatmap.hasInsufficentData()){
                    JOptionPane.showMessageDialog(null, "Insufficent data available: there are no GIs within/between the complex(es) you selected", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                ViewFrame tvFrame = tvheatmap.getViewFrame();
                
                //title = "Gene heat-map for " + complex1Name + "(black) and " + complex2Name+"(gray)";
                title = "Gene heatmap for "+listOfComplexes.toString();
                tvFrame.setTitle(title);
                tvFrame.setVisible(true);
                return;
            }
            //QUERY HEATMAP types are "query" for raw interaction data or "query-patterns" for patterns
            else if (type.startsWith("query")){
                //Need to update nodes to use here as its apart of right click menu and 
                //right click menu does not instaniate sgal
                ArrayList<Gene> queryGenes = new ArrayList<Gene>();
                ArrayList<String> geneNames = new ArrayList<String>();
                
                //Create query gene objects from selected nodes
                for (String n : nodesToUseString){
                    if (genes.containsKey(n)){
                        Gene g = genes.get(n);
                        queryGenes.add(g);
                        geneNames.add(g.getGeneName());
                    }
                }
                Collections.sort(geneNames);
                
                //Do pattern stuff
                if(type.endsWith("-patterns")){
                    
                    title = "Correlations/Anti-correlations " + geneNames;
                    
                    //Must have atleast 2 nodes to display patterns
                    if(nodesToUseString.size()<2){
                        JOptionPane.showMessageDialog(null, "Please select at least two genes to see patterns", 
                                "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    
                    //create heatmaps
                    GeneQueryHeatMap hm  = new GeneQueryHeatMap(queryGenes, genes.values(), "query-same");
                    GeneQueryHeatMap hm2 = new GeneQueryHeatMap(queryGenes, genes.values(), "query-alternating");
                    
                    //Display jdialog if both patterns have insufficnet data
                    if(hm.hasInsufficentData() && hm2.hasInsufficentData()){
                        JOptionPane.showMessageDialog(null, "Insufficent data available: there are no shared GIs between the genes you selected ", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    
                    //Get containers of treeview heatmaps
                    ViewFrame tvFrame1 = hm.getViewFrame();
                    
                    if(!hm.hasInsufficentData()){
                        tvFrame1.setTitle("Same sign patterns for :"+geneNames.toString());
                        //Only split screen when other heatmap has sufficent data
                        if(!hm2.hasInsufficentData()){
                            tvFrame1.setSize(screenX/2, screenY);
                            tvFrame1.setLocation(0, 0);
                        }
                        tvFrame1.setVisible(true);
                    }else{
                        JOptionPane.showMessageDialog(null, "Insufficent shared interactions with same signs available", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    
                    ViewFrame tvFrame2 = hm2.getViewFrame();
                    
                    if(!hm2.hasInsufficentData()){
                        tvFrame2.setTitle("Alternating sign patterns for :"+geneNames.toString());
                        //Only split screen when other heatmap has sufficent data
                        if(!hm.hasInsufficentData()){
                            tvFrame2.setSize(screenX/2, screenY);
                            tvFrame2.setLocation(screenX/2, 0);
                        }
                        tvFrame2.setVisible(true);
                    }else{
                        JOptionPane.showMessageDialog(null, "Insufficent shared interactions with alternating signs available", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                    
                }else{
                    //Reguar case: display raw interaction data
                    if(selNodes.isEmpty()){
                        JOptionPane.showMessageDialog(null, "You must select at least one node to display raw interaction data", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    title = "Gene heatmap for Query Genes: " + geneNames;
                    
                    //Do heatmap and get treeview frame's container
                    GeneQueryHeatMap hm = new GeneQueryHeatMap(queryGenes, genes.values(), type);
                    if(hm.hasInsufficentData()){
                        JOptionPane.showMessageDialog(null, "Insufficent raw interaction data available", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                        
                    ViewFrame tvFrame = hm.getViewFrame();
                    
                    tvFrame.setTitle(title);
                    tvFrame.setVisible(true);
                    
                }
                return;
            }
            else{ 
                //DO COMPLEX HEATMAP
                
                //Get name of selected complexes / selected complexes
                ArrayList <Complex> complexesToMap = new ArrayList<Complex>();
                ArrayList<String> complexNames = new ArrayList<String>();
                Collections.sort(nodesToUseString);
                for (String s : nodesToUseString){
                    complexesToMap.add(complexes.get(s));
                    complexNames.add(complexes.get(s).getName());
                }
                    
                //Must have atleast 2 complexes to display heatmap
                if(nodesToUseString.size()<2){
                    JOptionPane.showMessageDialog(null, "Please select at least two complexes", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                //Create heatmaps
//                ComplexHeatMap hm  = new ComplexHeatMap(complexesToMap, complexEdges, true);
//                ComplexHeatMap hm2  = new ComplexHeatMap(complexesToMap, complexEdges, false);

                ComplexHeatMapEnrichment hm  = new ComplexHeatMapEnrichment(complexesToMap, complexEdges, true);
                ComplexHeatMapEnrichment hm2  = new ComplexHeatMapEnrichment(complexesToMap, complexEdges, false);
                //Display jdialog if both complex heatmaps have insufficient data
                if(hm.hasInsufficentData() && hm2.hasInsufficentData()){
                    JOptionPane.showMessageDialog(null, "Insufficent data available", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                ViewFrame tvFrame2 = hm2.getViewFrame();
                
                if(!hm2.hasInsufficentData()){
                    tvFrame2.setTitle("Negative enrichment between complexes: "+ complexNames.toString());
                    //Only split screen when other heatmap has insufficent data
                    if(!hm.hasInsufficentData()){
                        tvFrame2.setSize(screenX/2, screenY);
                        tvFrame2.setLocation(screenX/2, 0);
                    }
                    tvFrame2.setVisible(true);
                }else{
                    JOptionPane.showMessageDialog(null, "Insufficent negative interactions available", "Error", JOptionPane.ERROR_MESSAGE);
                }
                
                //Get containers of treeview heatmaps
                ViewFrame tvFrame1 = hm.getViewFrame();

                if(!hm.hasInsufficentData()){
                    tvFrame1.setTitle("Positive enrichment between complexes: "+ complexNames.toString());
                    if(!hm2.hasInsufficentData()){
                        tvFrame1.setSize(screenX/2, screenY);
                        tvFrame1.setLocation(0, 0);
                    }
                    tvFrame1.setVisible(true);
                }else{
                    JOptionPane.showMessageDialog(null, "Insufficent positive interactions available", "Error", JOptionPane.ERROR_MESSAGE);
                }

                
                
                
                return;
            }
            
	}
        
        public void createPearsonTables(){
            //selected array of genes for correlation/pvalue stuff
            List<Gene> selectedGenes = new ArrayList();
            for(Object o: Cytoscape.getCurrentNetwork().getSelectedNodes()){
                CyNode node = (CyNode)o;
                if(genes.keySet().contains(node.getIdentifier())){
                    selectedGenes.add(genes.get(node.getIdentifier()));
                }
            }
            PearsonTable pt = new PearsonTable(selectedGenes);
            pt.setRootNetwork(rn);
            pt.setSideGuiActionListener(this);
        }
        
        public void addCorrelationEdges(){
            //All genes in network selected array of genes for correlation/pvalue stuff
            List<Gene> selectedGenes = new ArrayList();
            Map<String, CyNode> strToNode = new HashMap();
            int genesNotInGI = 0;
            int genesInGI = 0;
            for(Object o: Cytoscape.getCurrentNetwork().getSelectedNodes()){
                CyNode node = (CyNode)o;
                if(genes.keySet().contains(node.getIdentifier())){
                    Gene g = genes.get(node.getIdentifier());
                    if(g.getGeneNeighbours().isEmpty()){
                        genesNotInGI++;
                    }else{
                        genesInGI++;
                    }
                    selectedGenes.add(g);
                    strToNode.put(node.getIdentifier(), node);
                }
            }
            if(genesNotInGI > 0 && genesInGI == 0){
                JOptionPane.showMessageDialog(null, "Please ensure selected genes are in the GI data and try again",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            PearsonEdges pe = new PearsonEdges(selectedGenes, strToNode);
        }
        
        public boolean isValidRCutoff(String s){
            Double d;
            try{
                d = Double.parseDouble(s);
                if(d >= 0 && d <= 1) return true;
                else return false;
            }catch(NumberFormatException e){
                return false;
            }
        }
        
        /**
         * Creates subnetwork of complexes 
         */
	@SuppressWarnings("unchecked")
        public void createSubNet(){
            //No nodes. do nothing
            if(nodesToUse.isEmpty()){return;}
            SubNetworkTask snt = new SubNetworkTask();
            // Configure JTask Dialog Pop-Up Box
            JTaskConfig jTaskConfig = new JTaskConfig();
            jTaskConfig.setOwner(Cytoscape.getDesktop());
            jTaskConfig.displayCloseButton(true);

            jTaskConfig.displayCancelButton(false);

            jTaskConfig.displayStatus(true);
            jTaskConfig.setAutoDispose(true);

            // Execute Task in New Thread; pops open JTask Dialog Box.
            TaskManager.executeTask(snt, jTaskConfig);
	}//end createSubNet
	
        private void doSubNetwork(){
            CytoPanelImp ctrlPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
            int previousIndex = ctrlPanel.getSelectedIndex();

            //Set<CyNode> nodes = network.getSelectedNodes();
            String name = "Expanded view of nodes: ";
            
            taskMonitor.setStatus("Getting selected complex nodes...");
            taskMonitor.setPercentCompleted(10);
            
            ArrayList<String> complexNames = new ArrayList<String>();
            for(CyNode n : nodesToUse){
                    complexNames.add(n.getIdentifier());
            }

            HelperMethods.NumericalSort(complexNames);
            name += complexNames;
            
            //create the complex nodes
            CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();
            CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
            
            taskMonitor.setStatus("Creating network...");
            taskMonitor.setPercentCompleted(30);
            
	    CyNetwork subnet = Cytoscape.createNetwork(name, true);
            //Cytoscape.getNetworkView(subnet.getIdentifier()).addGraphViewChangeListener(new NetworkSelectionListener(subnet, rn));
            
            
	    Set<Gene> totalSetOfGenes = new HashSet<Gene>();
	    Set<Complex> complexesSelected = new HashSet<Complex>();
	    
            //Add attribute to network to distinguish gene networks from complex networks
            CyAttributes networkAtr = Cytoscape.getNetworkAttributes();
            networkAtr.setAttribute(subnet.getIdentifier(), GIProAttributeNames.NETWORK_TYPE, "Subnetwork");
            
	    for(CyNode n : nodesToUse){
	    	Complex c = complexes.get(n.getIdentifier());
	    	complexesSelected.add(c);
	    }
	    
	    int colourIndex = 0;
	    
            taskMonitor.setStatus("Adding nodes to network...");
            taskMonitor.setPercentCompleted(40);
            
            List<Complex> tooManyMulti = new ArrayList();
            for(Complex c: complexesSelected){
                Integer numMulti = 0;
                for(Gene gene: c.getGenes()){
                    if(gene.getComplexes().size()>1)
                        numMulti++;
                }
                Double d = (double) numMulti/(double)c.getGenes().size();
                if(d>30){//Greater than 30 percent multicomplex
                    tooManyMulti.add(c);
                }
            }
            
            
            HashMap<String,String> falggedForMultiComplex = new HashMap();
	    //Adds nodess
	    for(Complex c : complexesSelected){	    	
	    	Set<Gene> genes = c.getGenes();	    	
	    	for(Gene g : genes){
                    totalSetOfGenes.add(g);
                    String identifier = g.getGeneIdentifier();
                    CyNode newNode = Cytoscape.getCyNode(identifier, true);
                    
                    
                    
                    if(!subnet.containsNode(newNode)){
                        subnet.addNode(newNode);
                        nodeAttrs.setAttribute(identifier, 
                                GIProAttributeNames.DISPLAY_NAME, g.getGeneName());
                        nodeAttrs.setAttribute(identifier, 
                                GIProAttributeNames.IS_SUBNET, true);
                        nodeAttrs.setAttribute(identifier, 
                                GIProAttributeNames.COMPLEX_COLOR, Config.DISTINGUISHABLE_COLOURS[colourIndex]);
                        nodeAttrs.setAttribute(identifier, 
                                GIProAttributeNames.NODE_LABEL_COLOUR, g.getGIKey());
                        nodeAttrs.setAttribute(identifier, 
                                GIProAttributeNames.COMPLEX_FROM, g.getComplexes().toString());
                        
                        //Multiple complexes
                        Set<Complex> temp = new HashSet();
                        if(g.getComplexes().size() > 1){
                            temp.add(c);
                            nodeAttrs.setAttribute(identifier, 
                                GIProAttributeNames.IS_MULTICOMPLEX, true);
                        }
                        else temp = g.getComplexes();
                        
                        //Only update if hasnt been updated before
                        if(!falggedForMultiComplex.containsKey(identifier) && !tooManyMulti.contains(c)){
                            nodeAttrs.setAttribute(identifier, 
                            GIProAttributeNames.COMPLEX_LAYOUTNONLY, temp.toString());
                        }
                        
                    }
                    else{
                        //node exists already so we change its colour to multicomplex colour
//                        nodeAttrs.setAttribute(identifier, 
//                                GIProAttributeNames.COMPLEX_COLOR, Config.MULTICOMPLEX_COLOUR);
                    }
	    	}
	    	colourIndex++;
                
                if(colourIndex == Config.DISTINGUISHABLE_COLOURS.length){
                    colourIndex = 0;
                }
	    }
	    
            taskMonitor.setStatus("Adding physical edges...");
            taskMonitor.setPercentCompleted(50);
	    //Do subnet of physical edges
	    for(Gene gs : totalSetOfGenes){
	    	for(Gene gt : totalSetOfGenes){ 
                    if(!gs.equals(gt)){
                        //physical neighbours
                        if(gt.physicallyConnectedTo(gs)){

                            String n1 = gt.getGeneIdentifier();
                            String n2 = gs.getGeneIdentifier();

                            if(n1.compareTo(n2) > 0){
                                String temp = n1;
                                n1 = n2;
                                n2 = temp;
                            }
                            //n1 is smaller now
                            CyEdge edge = Cytoscape.getCyEdge(n1, n1 + "//" + n2, n2, "pp");
                            if(!subnet.containsEdge(edge)){
                                subnet.addEdge(edge);
                                edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.EDGE_TYPE, "p");
                                edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.EDGE_SCORE, 
                                        gs.getPhysicalEdge(gt).getScore());
                                Set<String> sharedComplexes = new HashSet(gt.getComplexes());
                                sharedComplexes.retainAll(gs.getComplexes());
                                if(sharedComplexes.isEmpty()){
                                    edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.IS_WITHIN, 
                                        "Between complex");
                                }else{
                                    edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.IS_WITHIN, 
                                        "Within complex");
                                }
                            }
                        }
                    }
	    	}
	    }
	    
	    // Layout based on physical interactions
	    CyNetworkView view = Cytoscape.getNetworkView(subnet.getIdentifier());
	    Visualization.refreshNetworkView(view, false, true, true);
	    
            taskMonitor.setStatus("Adding genetic edges...");
            taskMonitor.setPercentCompleted(70);
	    
            
            Map<CyEdge, Double> posMap = new HashMap();
            Map<CyEdge, Double> negMap = new HashMap();
            
            // Do gene edges (after layout!)
	    for(Gene gs : totalSetOfGenes){
	    	for(Gene gt : totalSetOfGenes){ 
                    if(!gs.equals(gt)){
                        //genetic neighbours
                        if(gt.geneticallyConnectedTo(gs)){
                            Double score = gt.getGeneticEdge(gs).getScore();
                            
                            
                            String n1 = gt.getGeneIdentifier();
                            String n2 = gs.getGeneIdentifier();

                            if(n1.compareTo(n2) > 0){
                                    String temp = n1;
                                    n1 = n2;
                                    n2 = temp;
                            }

                            //n1 is smaller now
                            CyEdge edge = Cytoscape.getCyEdge(n1, n1 + "//" + n2, n2, "gg");
                            if(!subnet.containsEdge(edge)){
                                subnet.addEdge(edge);

                                String type = "";

                                if(score > 0){
                                    posMap.put(edge, score);
                                    type = "gp";
                                }
                                else{
                                    negMap.put(edge, score * -1);
                                    type = "gn";
                                }
                                edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.EDGE_TYPE, type);
                                edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.EDGE_SCORE, 
                                        gs.getUnfilteredGeneticEdge(gt).getScore());
                                
                                Set<String> sharedComplexes = new HashSet(gt.getComplexes());
                                sharedComplexes.retainAll(gs.getComplexes());
                                if(sharedComplexes.isEmpty()){
                                    edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.IS_WITHIN, 
                                        "Between complex");
                                }else{
                                    edgeAttrs.setAttribute(edge.getIdentifier(), 
                                        GIProAttributeNames.IS_WITHIN, 
                                        "Within complex");
                                }
                            }
                        }
                    }
	    	}
	    }
            
            rn.refreshMaxMin();
            Double maxPos = rn.getMaxPos();
            Double maxNeg = rn.getMaxNeg();
//            
//            System.out.println("maxPos="+maxPos);
//            System.out.println("maxNeg="+maxNeg);
            
            Double upper = 0.90; 
            Double lower = 0.10;
            
            List<Double> posValues = new ArrayList(posMap.values());
            Collections.sort(posValues);
            
            List<Double> negValues = new ArrayList(negMap.values());
            Collections.sort(negValues);
            
//            Double pos75 = rn.getUpperPercPos(); 
//            Double neg75 = rn.getUpperPercNeg();
//            Double pos25 = rn.getLowerPercPos(); 
//            Double neg25 = rn.getLowerPercNeg();
            
//            if(!posValues.isEmpty()){
//                Double d = (posValues.size()+0.5)*upper;
//                pos75 = posValues.get(d.intValue());
//                
//                d = (posValues.size()+0.5)*lower;
//                pos25 = posValues.get(d.intValue());
//            }if(!negValues.isEmpty()){
//                Double d = (negValues.size()+0.5)*upper;
//                neg75 = negValues.get(d.intValue());
//                
//                d = (negValues.size()+0.5)*lower;
//                neg25 = negValues.get(d.intValue());
//            }
            
//            System.out.println("90th percpos="+pos75);
//            System.out.println("10th percpos="+pos25);
//            System.out.println("90th percneg="+neg75);
//            System.out.println("10th percneg="+neg25);
            
            Double FACTOR = 11.0;
            Double minWidth = 3.0;
            Double maxWidth = 10.0;
            //Do edge widths
            for(CyEdge e: posMap.keySet()){
                Double score = posMap.get(e);
                Double width;
                
//                if(score > pos75)
//                    width = maxWidth;
//                else if(score < pos25)
//                    width = minWidth;
//                else
                    width = (score/maxPos) * FACTOR;
                
                    if(width<minWidth) width = 5.0;
                edgeAttrs.setAttribute(e.getIdentifier(), 
                                        GIProAttributeNames.EDGE_WIDTH, 
                                        width);
            }
            
            for(CyEdge e: negMap.keySet()){
                Double score = negMap.get(e);
                Double width;
                
//                if(score > neg75)
//                    width = maxWidth;
//                else if(score < neg25)
//                    width = minWidth;
//                else
                    width = ((score*-1)/maxNeg) * FACTOR;
                    if(width< minWidth) width = minWidth;
                edgeAttrs.setAttribute(e.getIdentifier(), 
                                        GIProAttributeNames.EDGE_WIDTH, 
                                        width);
            }
            
            taskMonitor.setStatus("Adding selection listeners...");
            taskMonitor.setPercentCompleted(90);
	    
	    SubNetSelectionListener snsl = new SubNetSelectionListener(genes, jepN, jepE, tree, complexesSelected);
	    subnet.addSelectEventListener(snsl);
	    ctl.registerCyNetwork(subnet);
            
	    networkComplexMap.put(name, complexesSelected);
            rn.setNetworkComplexMap(networkComplexMap);
            
            //set side panel actions for subnetwork
            rn.populateSubnetworkActionsAndParams();
            
	    GeneNodeContextMenu menu = new GeneNodeContextMenu(subnet, this);
	    
	    //apply layout algorithm, rearrange windows, and refit content
	    view.addNodeContextMenuListener(menu);
	    Visualization.refreshNetworkView(view, true, false, true);
	    ctrlPanel.setSelectedIndex(previousIndex);
	    
	    jepN.setText("<html><font color=gray size=5 face=arial><center><i>No nodes selected</i></center></font></html>");
	    jepE.setText("<html><font color=gray size=5 face=arial><center><i>No edges selected</i></center></font></html>");
	    tree.clearSelection();
	}//end createSubNet
        /**
         * String representation of a set of Genes
         * @param set Set of genes
         * @return String separating genes in the set by a comma 
         */
        private static String setToString(Set<Gene> set){
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for(Object o : set){
                sb.append(o.toString() + ",");
            }
            if(sb.length() > 1){
                sb.deleteCharAt(sb.length()-1);
            }
            sb.append("]");

            return sb.toString();
	}
    
    //PROGRESS BAR FOR SUBNETWORK TASK
    public class SubNetworkTask implements Task{

        @Override
        public void run() {
            taskMonitor.setStatus("Initializing...");
            taskMonitor.setPercentCompleted(-1);
            doSubNetwork();
            taskMonitor.setStatus("Done");
            taskMonitor.setPercentCompleted(100);
        }

        @Override
        public void halt() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
            taskMonitor = tm;
        }

        @Override
        public String getTitle() {
            return "Generating subnetworks..."; 
        }
        
    }
}




