package guiICTools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import java.io.Serializable;
import cytoscape.Cytoscape;

import plugin.Gene;
import plugin.HistogramGui;
import plugin.RootNetwork;
import plugin.Init;

import cern.jet.random.Normal;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import cytoscape.view.cytopanels.CytoPanelImp;
import java.awt.Container;
import java.io.LineNumberReader;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/*
 * 
 * Yanqi Hao
 * 
 * My action listener specific for my own wizard panels. Also stores data.
 * 
 * Updated: Jan 7, 2010
 * Anna Merkoulovitch
 * 
 * Added bin sorting, pvalue gaussian cutoff calculations.
 * 
 */

public class MyWizardPanelDataStorage implements Serializable{
	
    transient private RootNetwork rootNetwork;

    //Cutoff Panel 1 Data
    private String relationalFilePath, complexFilePath, nameMapFilePath, physicalInteractionPath;

    //Enrichment Panel 2 Data
    private int leftTailPercentile, rightTailPercentile; //the number of trials to run in order to generate a distribution

    // should the distribution generating function be repeated for each complex (or use a previously generated example) 
    private boolean usePvalCutoff, usePercentileCutoff, relationPValuesIncluded, filteringRelations, 
            useFisher, useSimulations; // useFisher is true when using Fisher, and false if using simulations
    private double fdr, pvalLeftTail, pvalRightTail;
    
    private int totalNumInteractions, numberOfTrials;
    private boolean trialForEachComplex;

    //Computational Data
    private static double positiveCutoff, negativeCutoff;
    private static Double gaussianMean, gaussianIQR;

    List<Double> valuesWithin, valuesBetween;

    private static Double[] relationScoreArray;
    private double minScore, maxScore;
    
    final double INITITAL_GAUSSIAN_PVAL = 0.05;
    final double COMPLEX_CUTOFF = 0.5;
    transient private JTextArea console;
    transient public File inputInfoDirectory;
    
    private boolean isInterrupted = false;

    // The scores in the genetic relations file that also have an acceptable pValue (<=0.05), if p-value is specified
    private List<Double> filteredListOfScores;
    // All the scores in the relations file
    private static List<Double> fullListOfScores;

    // maps number of draws (this is number of interactions or pairs) to stored distributions already generated.
    private HashMap<Integer, int[][]> storedTrials;

    // set of all genes in the relational file
    private HashSet<String> genesInRelationList;

    // Lists all pairs of genes in the relational file
    private HashMap<String, Double> genePairsInRelationalFile;
    
    private static Normal standardNormalDistribution;
    
    private TaskMonitor loadFilesMonitor; 
        private int[] loadFilesMethodPercent = {80, 20};
    private TaskMonitor computationsMonitor;
        private int[] computationsMethodPercent = {5,15,25,40,50,55,58,65, 70};
    
    public void setRootNetwork(RootNetwork rn){
            this.rootNetwork = rn;
    }
    public RootNetwork getRootNetwork(){
        return this.rootNetwork;
    }
    
    /**
     * Constructor for MyWizardPanelDataStorage
     */
    public MyWizardPanelDataStorage(){
        
        //Initiate directory for temp file containing saved directories depending on OS
        String currentOs = System.getProperty("os.name").toLowerCase();
        //inputInfoDirectory = new File(System.getProperty("user.home")+File.separator+"ICTPlugin.temp");
        
        inputInfoDirectory = new File("GIProPlugin.temp");
        
        genesInRelationList = new HashSet<String>();//Set of strings of genes in GIs
        genePairsInRelationalFile = new HashMap<String, Double>();//Pairs of genes mapped to score genea//geneb->score
        storedTrials = new HashMap<Integer, int[][]>();
        filteredListOfScores = new ArrayList<Double>();
        fullListOfScores = new ArrayList<Double>();
        relationScoreArray = new Double[0];
        minScore = 0;
        maxScore = 0;
        relationPValuesIncluded = false;
        //poke the gc to hopefully clear memory from previous read in stuff
        System.gc();
    }
    
    public void setMwpdsInitVariables(HashSet<String> genesInRelationList, 
            List<Double> fullListOfRelations, List<Double> filteredListOfScores,
            Double[] relationScoreArray, Double positiveCutoff, Double negativeCutoff ){
        this.genesInRelationList = genesInRelationList;
        this.fullListOfScores = fullListOfRelations;
        this.filteredListOfScores = filteredListOfScores;
        this.relationScoreArray = relationScoreArray;
        this.positiveCutoff = positiveCutoff;
        this.negativeCutoff = negativeCutoff;
        
        totalNumInteractions = fullListOfRelations.size();
    }
    
    public void setGenePairsInRelationalFile(HashMap<String, Double> genePairsInRelationalFile){
        this.genePairsInRelationalFile = genePairsInRelationalFile;
    }
    
    /**
     * Instantiate cutoff panel variables of MyWizardPanelDataStorage class with 
     * values from Input.java.
     * Also saves default file paths to temp file
     * @param rfp Relational file path 
     * @param cfp Complex file path
     * @param nmfp Name-map file path
     * @param pif Physical interaction file path
     * @param filterRelations 
     */
    public void populatePanelOneData(String rfp, String cfp, String nmfp, 
        String pif, boolean filterRelations){

        this.relationalFilePath = rfp;
        this.complexFilePath = cfp;
        this.nameMapFilePath = nmfp;
        this.physicalInteractionPath = pif;
        this.filteringRelations = filterRelations;
        saveFileDirs();
    }
    
    /**
     * Instantiate enrichment panel variables of MyWizardPanelDataStorage class 
     * with values from Input.java.
     * @param percentCutoff
     * @param usePvalCutoff
     * @param posSolidCutoff
     * @param negSolidCutoff
     * @param fdr
     * @param useFisher
     * @param numTrials
     * @param trialForEachComplex 
     */
    public void populatePanelTwoData(
            boolean usePvalCutoff, Double pvalLeftTail, Double pvalRightTail,
            boolean usePercentileCutoff, int leftTailPercentile, int rightTailPercentile, 
            double posSolidCutoff, double negSolidCutoff, double fdr, boolean useFisher,
            boolean useSimulations, int numTrials, boolean trialForEachComplex){
        this.usePvalCutoff = usePvalCutoff;
        this.pvalLeftTail = pvalLeftTail;
        this.pvalRightTail = pvalRightTail;
        
        this.usePercentileCutoff = usePercentileCutoff;
        this.leftTailPercentile = leftTailPercentile;	
        this.rightTailPercentile = rightTailPercentile;
        
        this.positiveCutoff = posSolidCutoff;
        this.negativeCutoff = negSolidCutoff;
        
        this.fdr = fdr;
        this.useFisher = useFisher;
        this.useSimulations = useSimulations;
        this.numberOfTrials = numTrials;
        this.trialForEachComplex = trialForEachComplex;
        
        
    }
    
    public HashMap<String, Double> getGenePairsInRelationalFile(){
        return genePairsInRelationalFile;}
    
    
    public boolean getUsePvalCutoffs(){return usePvalCutoff;}
    public Double getPvalLeftTail(){return pvalLeftTail;}
    public Double getPvalRightTail(){return pvalRightTail;}
    
    public boolean getUsePercentileCutoff(){return usePercentileCutoff;}
    public int getLeftTailPercentile(){return leftTailPercentile;}
    public int getRightTailPercentile(){return rightTailPercentile;}
    
    public boolean getUseFisher(){return useFisher;}
    public boolean getUseSimulations(){return useSimulations;}
    public int getNumberOfTrials(){return numberOfTrials;}
    public boolean getTrialForEachComplex(){return trialForEachComplex;}
    
    /**
     * Input string is System.err.println-ed and written to console on a new line
     * @param toPrint String to print
     */
    public void specialPrintln(String toPrint){
        System.err.println(toPrint);
        if(console != null){
            console.append(toPrint + "\n");
            console.setCaretPosition(console.getText().length());
        }
    }
    
    /**
     * Input string is System.err.print-ed and written to console on the same line
     * @param toPrint 
     */
    private void specialPrint(String toPrint){
        if(console != null){
            console.append(toPrint);
            console.setCaretPosition(console.getText().length());
        }
        System.out.print(toPrint);
    }
    
    /**
     * Saves input file directories to a temp file for later default use
     */
    private void saveFileDirs(){
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(inputInfoDirectory));
            bw.write(relationalFilePath + "\n");
            bw.write(nameMapFilePath + "\n");
            bw.write(complexFilePath + "\n");
            bw.write(physicalInteractionPath + "\n");
            bw.flush();
            bw.close();
        }catch (IOException e){
            System.err.println("Failed to write file dirs to temp file");
        }
    }

    /**
     * Stuff that can't be reset unless the user does the primary computation again
     */
    public void reset(){
        genesInRelationList.clear();
        storedTrials.clear();
        minScore = 0;
        maxScore = 0;
    }

    /**
     * All Methods involving computations are below here.
     */
    public void rebuildComputations(){
        //need to reset complex edges first 
        rootNetwork.getComplexEdge().clear();
        
        initializeData();
        
        //clear all previous genetic interactions of genes
        for(Gene g: rootNetwork.getGenes().values()){
            g.removeAllGeneticSubEdges();
        }
        
        //Reset total pos/neg
        rootNetwork.setTotalPos(0);
        rootNetwork.setTotalNeg(0);
        
        //Proceed normally
        processGeneticInteractions();//MUST BE RE RUN WHEN REBUILDING MWPDS
        
        //reset it to what it used to be (FUCKS UP IF YOU CHANGE CUTOFF)
//        rootNetwork.setTotalPos(totalPosOld);
//        rootNetwork.setTotalNeg(totalNegOld);
        
        int totalPos = 0;
        int totalNeg = 0;
        for(Double d: filteredListOfScores){
            if(d > positiveCutoff) totalPos++;
            else if(d < negativeCutoff) totalNeg++;
        }
        rootNetwork.setTotalPos(totalPos);
        rootNetwork.setTotalNeg(totalNeg);
        
        withinComplexTrials();//MUST BE RE RUN WHEN REBUILDING MWPDS
        betweenComplexTrials();//MUST BE RE RUN WHEN REBUILDING MWPDS
        
        //print between and within pvalues
        System.out.println("----REBUILT----");
        System.out.println("FDR = "+fdr);
        System.out.println("Positive cutoff = "+ positiveCutoff);
        System.out.println("Negative cutoff = "+ negativeCutoff);
        System.out.println("Within P-Value = "+rootNetwork.getWithinPValue());
        System.out.println("Between P-Value = "+rootNetwork.getBetweenPValue());
        System.out.println("----REBUILT----");
        
        //Build new network
        Init.setRootNetwork(rootNetwork);

        rootNetwork.setValuesWithinAndBetween(valuesWithin, valuesBetween);
        rootNetwork.createRootGraph();
        System.out.println("Finished rebuild computations !!");
    }
    
    public void executeComputations(){
        final MyWizardPanelDataStorage mwpds = this;
        
        rootNetwork = new RootNetwork(mwpds);
        
        //specialPrintln("Executing computations.");
        computationsMonitor.setStatus("Executing computations...");

        //System.out.println("#executeComputations(): calculateCutoffs()");
        computationsMonitor.setStatus("Calculating cutoffs...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[0]);
        calculateCutoffs();

        if(isInterrupted) return;

        //System.out.println("#executeComputations(): storeComplexData()");
        computationsMonitor.setStatus("Reading and storing complex data...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[1]);
        //read in complex information to populate complexToGenes and geneToComplexes
        storeComplexData();//dosnt need to be rerun on mwpds rebuild

        if(isInterrupted) return;

        //System.out.println("#executeComputations(): initializeData()");
        computationsMonitor.setStatus("Initializing data...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[2]);
        //use complex information to initialize data for each complex and each pair of complexes
        //simply initialize some values in preparation for counting
        initializeData();//dosnt need to be rerun on mwpds rebuild

        if(isInterrupted) return;

        //System.out.println("#executeComputations(): processPhysicalInteractions()");
        computationsMonitor.setStatus("Processing physical interactions...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[3]);
        //processes physical interactions if the user wants cytoscape files.
        //actually begin counting physical interactions and adding them to gene and complex classes
        processPhysicalInteractions();//Does not need to be re run on mwpds rebuild

        if(isInterrupted) return;

        //System.out.println("#executeComputations(): processGeneticInteractions()");
        computationsMonitor.setStatus("Processing genetic interactions...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[4]);
        //read in relations from filteredListOfRelations and populate the number of positive and negative relations 
        // for within and between complex interactions (complex and complexedges)
        processGeneticInteractions();//MUST BE RE RUN WHEN REBUILDING MWPDS

        if(isInterrupted) return;

        //System.out.println("#executeComputations(): processComplexes()");
        computationsMonitor.setStatus("Performing within complex trials...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[5]);
        // Do enrichment analysis for within complex
        withinComplexTrials();//MUST BE RE RUN WHEN REBUILDING MWPDS

        if(isInterrupted) return;

        //System.out.println("#executeComputations(): processIntercomplexes()");
        computationsMonitor.setStatus("Performing between complex trials...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[6]);
        // Do enrichment analysis for between complex
        betweenComplexTrials();//MUST BE RE RUN WHEN REBUILDING MWPDS
        
        
        //print between and within pvalues
        System.out.println("----ORIGINAL----");
        System.out.println("FDR = "+rootNetwork.getFDR());
        System.out.println("Positive cutoff = "+ positiveCutoff);
        System.out.println("Negative cutoff = "+ negativeCutoff);
        System.out.println("Within P-Value = "+rootNetwork.getWithinPValue());
        System.out.println("Between P-Value = "+rootNetwork.getBetweenPValue());
        System.out.println("Total pos = "+rootNetwork.getTotalPos());
        System.out.println("Total neg = "+rootNetwork.getTotalNeg());
        System.out.println("Total = "+rootNetwork.getTotal());
        System.out.println("----ORIGINAL----");
        

        if(isInterrupted) return;

        // Read and store information from the name map file (if one is specified)
        if (!nameMapFilePath.equals("")){
            //System.out.println("#executeComputations(): readNameMap()");
            computationsMonitor.setStatus("Processing name map file...");
            computationsMonitor.setPercentCompleted(computationsMethodPercent[7]);
            readNameMap();
        }

        if(isInterrupted) return;

        //System.out.println("#executeComputations(): rootNetwork.initialize()");
        computationsMonitor.setStatus("Generating network...");
        computationsMonitor.setPercentCompleted(computationsMethodPercent[8]);

        rootNetwork.initialize(positiveCutoff, negativeCutoff, fdr);
        System.out.println("Saving network");
        Init.setRootNetwork(rootNetwork);

        rootNetwork.setValuesWithinAndBetween(valuesWithin, valuesBetween);
        //specialPrintln("Finished Computations");
        
        //System.out.println("@Full list of relations="+fullListOfScores.size());
    }
    
    public void executeComputationsTask(){
        ExecuteComputationsTask task = new ExecuteComputationsTask();
        // Configure JTask Dialog Pop-Up Box
        JTaskConfig jTaskConfig = new JTaskConfig();
        //jTaskConfig.setOwner(Cytoscape.getDesktop());
        jTaskConfig.displayTimeElapsed(true);
        jTaskConfig.displayCloseButton(true);

        jTaskConfig.displayCancelButton(true);

        jTaskConfig.displayStatus(true);
        jTaskConfig.setAutoDispose(true);

        // Execute Task in New Thread; pops open JTask Dialog Box.
        TaskManager.executeTask(task, jTaskConfig);
    }
    /**
     * Reads relation data and calculates the initial gaussian cutoff
     */
    public void executeCutoffCalculations(){
        
        //specialPrintln("Reading relations...");
        loadFilesMonitor.setStatus("Reading relations...");
        readInRelations();
        
        if(isInterrupted) return;
        
        loadFilesMonitor.setStatus("Calculating initial cutoffs...");
        //specialPrintln("Calculating initial cutoffs...");
        calculateGaussianCutoffs(this.INITITAL_GAUSSIAN_PVAL/2, this.INITITAL_GAUSSIAN_PVAL/2);
        loadFilesMonitor.setStatus("Done reading relations. You can now specify cutoff and simulation parameters.");
        //specialPrintln("Done reading relations. You can now specify cutoff and simulation parameters.\n");
    }
    
    public void runTaskLoadFiles(){
        LoadFilesTask task = new LoadFilesTask();
			
        // Configure JTask Dialog Pop-Up Box
        JTaskConfig jTaskConfig = new JTaskConfig();
        //jTaskConfig.setOwner(Cytoscape.getDesktop());
        jTaskConfig.displayTimeElapsed(true);
        jTaskConfig.displayCloseButton(true);

        jTaskConfig.displayCancelButton(true);

        jTaskConfig.displayStatus(true);
        jTaskConfig.setAutoDispose(false);

        // Execute Task in New Thread; pops open JTask Dialog Box.
        TaskManager.executeTask(task, jTaskConfig);
    }
	
    /**
     * Reads in correlations or interactions and populates necessary data
     */
    private void readInRelations(){
    	String gene1, gene2, sScore, spValue = "";
        double score;
        String[]tokens = new String[4];
        // If fullListOfRelations isn't empty, a file must have previously been 
        // loaded in (so we need to replace the data)
        if (!fullListOfScores.isEmpty()){
            relationPValuesIncluded = false;
            fullListOfScores.clear();
            filteredListOfScores.clear();
            genesInRelationList.clear();
            genePairsInRelationalFile.clear();
            minScore = 0;
            maxScore = 0;
            gaussianMean = null;
            gaussianIQR = null;
        }
        try{
            HashSet<String> genesInComplexes = new HashSet<String>();
            if (filteringRelations){
                genesInComplexes = readComplexGenes();
            }
            FileReader fr = new FileReader(relationalFilePath);
            BufferedReader br = new BufferedReader(fr);
            
            //tmp
            FileReader tmpfr = new FileReader(relationalFilePath);
            BufferedReader tmpbr = new BufferedReader(tmpfr);
            
            totalNumInteractions = 0;
            String nextLine = tmpbr.readLine();
            tmpbr.close();
            tmpfr.close();

            int numCols = nextLine.split("\\s").length;
            
            // If there are 4 columns in the first line, assume this file includes p-values for interactions
            if (numCols == 4)
                relationPValuesIncluded = true;
            else
                relationPValuesIncluded = false;
            
            //Get line count for task run
            LineNumberReader lnr = new LineNumberReader(new FileReader(relationalFilePath));
            String line = null;
            while((line = lnr.readLine()) != null){}
            int lineCount = lnr.getLineNumber();
            
            //Used for percent calculation in progress bar
            int count = 0;
            double percent;
            
            
            while((nextLine = br.readLine()) != null){
                if(isInterrupted) return; 
                
                percent = ((double)count/(double)lineCount)
                        * loadFilesMethodPercent[0];
                loadFilesMonitor.setPercentCompleted((int) percent);
                
                tokens = nextLine.split("\\s");
                
                gene1 = tokens[0];
                gene2 = tokens[1];
                /*
                 * Ignore genes that interact with themselves/reciporcal(below)
                 * Without these 2 lines of code FisherExact messes up
                 * as it thinks there is more interactions and so when you 
                 * subtract number of pos/neg interactions from number of 
                 * edges you get a negative number
                 */
                if(gene1.equals(gene2)){
                    continue;
                }
                
                // If at least one of the genes is NOT in a complex, skip this relation
                if (filteringRelations && !(genesInComplexes.contains(gene1) 
                        || genesInComplexes.contains(gene2))){
                        continue;
                }
                
                count++;
                totalNumInteractions++;

                sScore = tokens[2];
                if (relationPValuesIncluded){
                    spValue = tokens[3];
                }

                try{
                    score = Double.parseDouble(sScore);
                    fullListOfScores.add(score);
                    
                    if (relationPValuesIncluded && Double.parseDouble(spValue) > 0.05){
                        if(totalNumInteractions % 200000 == 0){
                            //specialPrintln("Line " + totalNumInteractions + ": did not meet cutoff. " + score);
                            loadFilesMonitor.setStatus("Line " + totalNumInteractions + ": did not meet cutoff. " + score);
                        }
                        continue;
                    }
                    
                    /*
                     * if reciprocal is already added, check if its score is greater
                     * than the already contained score. If so, update the reciprocal 
                     * score and skip this interaction 
                     */
                    if(genePairsInRelationalFile.containsKey(gene2+"//"+gene1)){
                        if(Math.abs(score) > 
                                Math.abs(genePairsInRelationalFile.get(gene2+"//"+gene1))){
                            genePairsInRelationalFile.put(gene2+"//"+gene1, score);
                        }
                        continue;//skip
                    }

                    genesInRelationList.add(gene1);
                    genesInRelationList.add(gene2);
                    genePairsInRelationalFile.put(gene1 + "//" + gene2, score);
                    
                    
                    if(totalNumInteractions % 200000 == 0){
                        //specialPrintln("Line " + totalNumInteractions + ": " + gene1 + " " + gene2 + " " + score);
                        loadFilesMonitor.setStatus("Line " + totalNumInteractions + ": " + gene1 + " " + gene2 + " " + score);
                    }

                    // set new minimum and maximum scores if necessary
                    if (score < minScore){
                            minScore = score;
                    }
                    if (score > maxScore){
                            maxScore = score;
                    }
                    filteredListOfScores.add(score);
                }catch(NumberFormatException e){
                            // skip this line
                }
            }
            System.out.println(">> number of unfiltered interactions = "+filteredListOfScores.size());
            br.close();
            fr.close();
            relationScoreArray = fullListOfScores.toArray(new Double[fullListOfScores.size()]);
            Arrays.sort(relationScoreArray);
            System.out.println("scores size="+relationScoreArray.length);
            
        }
        catch(IOException e){
            JOptionPane.showMessageDialog(null, "Failed to read in relations, Cytoscape will now exit",
                    "Error", JOptionPane.ERROR_MESSAGE);
                //specialPrintln("ERROR READ IN RELATIONS.." + e.toString());
                System.exit(1);
        }
	}//end readInCorrelations
    
    public List<Double> getFullListOfScores(){
        return fullListOfScores;
    }
    
    public List<Double> getFilteredListOfScores(){
        return filteredListOfScores;
    }
    /**
     * Reads in the complex file, and creates a set of genes that are in complexes
     * (this is used to filter the relations to only load relations where at least one gene is in the complex)
     * @return Set of genes that are in complexes
     */
    private HashSet<String> readComplexGenes(){
    	HashSet<String> genesInComplexes = new HashSet<String>();
    	//specialPrintln("Read complex genes...");
        try{
            String pathComplex = complexFilePath;
        
            FileReader readInComplex = new FileReader(pathComplex);
            Scanner inputComplex = new Scanner(readInComplex);
            inputComplex.useDelimiter("\t|\n|\r\n");
            inputComplex.nextLine();
            	            
            while(inputComplex.hasNext()){    
                // skip complex name
                inputComplex.next();

                String geneName = inputComplex.next();
                genesInComplexes.add(geneName);
            }
            inputComplex.close();
            readInComplex.close();
        }
        catch(IOException e){
            JOptionPane.showMessageDialog(null, "Failed to read complex genes, Cytoscape will now exit",
                    "Error", JOptionPane.ERROR_MESSAGE);
        	//specialPrintln("ERROR READING COMPLEXES " + e.toString());
        	System.exit(1);
        }
        //specialPrintln("done reading");
        return genesInComplexes;
    }
    
    /**
     * Calculates the necessary cutoffs for this data (excluding gaussian cutoffs)
     */
    private void calculateCutoffs(){
        //specialPrintln("Calculating cutoffs...");

        String line2 = "Negcutoff: " + negativeCutoff + "\n";
        String line3 = "Poscutoff: " + positiveCutoff + "\n";

        if(!usePvalCutoff){
                Double middle = relationScoreArray[relationScoreArray.length/2];

            String line1 = "Median: " + middle + "\n";
            String line4 = "From cutoff of " + leftTailPercentile + "%\n";

            specialPrint(line1);
            specialPrint(line2);
            specialPrint(line3);
            specialPrint(line4);
        }
        else{			
            String line1 = "Using solid cutoffs:\n";

            specialPrint(line1);
            specialPrint(line2);
            specialPrint(line3);
        }
    }    
   
    /**
     * Read through the complex file path, and return a map of complexes to 
     * integer array containing number of genes and number of interacting genes
     * such that at least half the number of genes are interacting
     * @return HashMap<String, int[]> where String is the complex name,  
     * array[0] contains number of interacting genes and array[1] 
     * contains number of genes in the complex
     */
    private HashMap<String, int[]> filterComplexList(){
    	//specialPrintln("Filtering complexes...");
    	HashMap<String, int[]> complexGIInfo = new HashMap <String, int[]>();
    	HashMap<String, int[]> filteredComplexGIInfo = new HashMap <String, int[]>();
        try{
            String pathComplex = complexFilePath;
        
            FileReader readInComplex = new FileReader(pathComplex);
            Scanner inputComplex = new Scanner(readInComplex);
            inputComplex.useDelimiter("\t|\n|\r\n");
            inputComplex.nextLine();
            	            
            while(inputComplex.hasNext()){    
                String complexName = inputComplex.next();
                String geneName = inputComplex.next();
            	int[] vals = new int[2];
            	int inRelations = 0;
            	if (genesInRelationList.contains(geneName)){
            		inRelations = 1;
            	}
                if (!complexGIInfo.containsKey(complexName)){
                	complexGIInfo.put(complexName, vals);
                }
                else{
                	vals = complexGIInfo.get(complexName);
                }
                vals[0] += inRelations;
                vals[1] ++;
            }
            inputComplex.close();
            readInComplex.close();
        }
        catch(IOException e){
            //specialPrintln("ERROR " + e.toString());
            System.exit(1);
        }
        System.out.println("done reading");
        for (Map.Entry<String, int[]> pair :complexGIInfo.entrySet()){
            String complex = pair.getKey();
            int[] info = pair.getValue();
            int numInteractingGenes = info[0];
            int numGenes = info[1];     	
            if (numInteractingGenes > (COMPLEX_CUTOFF*numGenes)){
                    filteredComplexGIInfo.put(complex,info);
            }
        }
        return filteredComplexGIInfo;
    }
    
    /**
     * Reads in the complex file.
     * Populates the complexToGenes hash (the value "genes" is as an array-list)
     * populates the geneToComplexes hash (the value "complexes" is as an arraylist)
     */
    private void storeComplexData(){
    	// filter complexes so that at least have its genes are interacting.
    	HashMap<String, int[]> filteredComplexesMap = filterComplexList();
    	//specialPrintln("Reading complexes...");
        try{
            String pathComplex = complexFilePath;
            BufferedReader in = new BufferedReader(new FileReader(pathComplex));
            String inputComplex;	            
            while((inputComplex = in.readLine()) != null){ 
                try{
                String[] sp = inputComplex.split("\t");
                String complexName = sp[0];
                String geneName = sp[1];
                String desc = "-";
                
                if(sp.length == 3){
                    desc = sp[2];
                }
                
                //System.out.println(complexName+"\t"+geneName);
            	if (filteredComplexesMap.containsKey(complexName)){
                    int[] data = filteredComplexesMap.get(complexName);
                    int numGenes = data[0];
                    rootNetwork.insertComplexGenePair(complexName, geneName, desc,
                            numGenes, genesInRelationList.contains(geneName));
            	}
                }catch(Exception e){
                    System.out.println("skip cpx");
                }
            }
        }
        catch(IOException e){
            JOptionPane.showMessageDialog(null, "Failed to store complex data, Cytoscape will now exit",
                    "Error", JOptionPane.ERROR_MESSAGE);
            //specialPrintln("ERROR STORING COMPLEX DATA " + e.toString());
            System.exit(1);
        }
        //specialPrintln("Done reading complexes.\n");
    }

    /*
     * Initializes within complex and between complex information
     * (number of genes, actual number of genes, possible edges etc.)
     * Also purges redundant edges when dealing with between complexes. 
     * (ie shared genes in a complex pair shouldn't interact with itself.)
     */
    private void initializeData(){
        //specialPrintln("Initializing data...");
        rootNetwork.initializeComplexData(genesInRelationList);
        //specialPrintln("Done initializing data.\n");
    }
    
    /**
     * Adds physical interaction edges and physical interaction scores to network 
     * and edge attributes
     * Note: does not output files yet, it only stores the new information
     */
    private void processPhysicalInteractions(){
            //specialPrintln("Processing physical interactions...");
		
        try{
//            FileReader readerInteraction = new FileReader(physicalInteractionPath);
//            Scanner inputInteraction = new Scanner(readerInteraction);
            
            BufferedReader in = new BufferedReader(new FileReader(physicalInteractionPath));
//            
//            inputInteraction.useDelimiter("\t|\n|\r\n");
            String inputInteraction;            
            while((inputInteraction = in.readLine()) != null){
                try{
                String[] split = inputInteraction.split("\t");
                String gene1Name = split[0];
                String gene2Name = split[1];
                String sScore = split[2];
                
                //case where gene interacts with itself. messes up fisher exact test
                if(gene1Name.toLowerCase().equals(gene2Name.toLowerCase())) 
                    continue;
                
                Double score = Double.parseDouble(sScore);
                rootNetwork.processPhysicalInteraction(gene1Name, gene2Name, score);  
                }catch(Exception e){
                    //skip line
                    System.out.println("skip");
                }
            }
            
            //specialPrintln("Done processing physical interactions.\n");
        }
        catch(IOException e){
            
            JOptionPane.showMessageDialog(null, "Failed to process physical interactions, Cytoscape will now exit",
                    "Error", JOptionPane.ERROR_MESSAGE);
            //specialPrintln("ERROR PROCESING PHYSICAL.." + e.toString());
            System.exit(1);
        }
    }

    /**
     * Counts number of positive/negative and neutral interactions while 
     * reading in the interactions or correlations.
     */
    private void processGeneticInteractions(){
    	valuesWithin = new ArrayList<Double>();
      	valuesBetween = new ArrayList<Double>();
      	
    	//specialPrintln("Processing Genetic Interactions...");
        //System.out.println("### genePairInRelationFile size="+genePairsInRelationalFile.size());
        for (Map.Entry<String, Double> pair : genePairsInRelationalFile.entrySet()){
            
            String interaction = pair.getKey();
            double score = pair.getValue();
            
            String[] tokens = interaction.split("//");
            String gene1 = tokens[0];
            String gene2 = tokens[1];
            
            //Case where gene interacts with itself. without this line, fisher exact test messes up
            if(gene1.toLowerCase().equals(gene2.toLowerCase())){
                continue;
            }
            
            // Adds the score to either valuesWithin or valuesBetween, depending on whether
            // the two genes are found in the same complex or not
            if(rootNetwork.inSameComplex(gene1, gene2)){
            	valuesWithin.add(score);
            }
            else if(rootNetwork.genesExist(gene1, gene2)){
            	valuesBetween.add(score);
            }
            rootNetwork.processGeneticInteraction(gene1, gene2, score, positiveCutoff, negativeCutoff);
        }
        //specialPrintln("\nDone processing.\n");
    }//end process	
    
    public void updateMwpdsVariables(boolean usePvalCutoffs , double pvalLeftTail, double pvalRightTail,
            boolean usePercentileCutoffs, int leftTailPercentile, int rightTailPercentile, 
            double customNegCutoff, double customPosCutoff,
            double fdr,boolean useFisher, boolean useSimulations, int numberOfTrials, boolean trialForEachComplex){
        
        storedTrials = new HashMap<Integer, int[][]>();
        this.usePvalCutoff = usePvalCutoffs;
        this.pvalLeftTail = pvalLeftTail;
        this.pvalRightTail = pvalRightTail;
        this.usePercentileCutoff = usePercentileCutoffs;
        this.leftTailPercentile = leftTailPercentile;
        this.rightTailPercentile = rightTailPercentile;
        this.useFisher = useFisher;
        this.fdr = fdr;
        this.useSimulations = useSimulations;
        this.numberOfTrials = numberOfTrials;
        this.trialForEachComplex = trialForEachComplex;
        //do more?
        
    }
    
    /**
     * Puts p-values on positive and negative interactions or correlations by 
     * performing fisher/simulation trails on within-complex data
     */
    private void withinComplexTrials(){
        //specialPrintln("Performing trials for within complexes...");
        if (useFisher){
            System.out.println("#dooing fisher on rebuild:"+positiveCutoff+", "+ negativeCutoff+", "+ fdr);
            rootNetwork.withinComplexFisherTrials(positiveCutoff, negativeCutoff, fdr);
            
        }
        else{
            System.out.println("#doing simulation on rebuild:"+numberOfTrials+", "+
                    trialForEachComplex+", "+storedTrials+", "+
                    positiveCutoff+", "+negativeCutoff+", "+fdr);
            rootNetwork.withinComplexSimulationTrials(numberOfTrials, 
                    trialForEachComplex, storedTrials, fullListOfScores, 
                    positiveCutoff, negativeCutoff, fdr);
        }
        //specialPrintln("Done writing within complex analysis.\n");
    }
	
    /**
     * Puts p-values on positive and negative interactions or correlations by 
     * performing fisher/simulation trails on between-complex data
     */
    private void betweenComplexTrials(){
        //specialPrintln("Performing trials for between complexes...");
        if (useFisher){
            rootNetwork.betweenComplexFisherTrials(positiveCutoff, negativeCutoff, fdr);
        }
        else{
            rootNetwork.betweenComplexSimulationTrials(numberOfTrials, 
                    trialForEachComplex, storedTrials,  fullListOfScores, 
                    positiveCutoff, negativeCutoff, fdr);
        }
        //specialPrintln("Done writing between complex analysis.\n");
    }//end processComplexes
	
    /**
     * Read and process name map file
     */
    private void readNameMap() {
        try{	
            Scanner in = new Scanner(new FileReader(nameMapFilePath));
            in.nextLine();//skip header

            while(in.hasNextLine()){
                String nextLine = in.nextLine();
                String orfName = nextLine.split("\t")[0];
                String geneName = nextLine.split("\t")[1];
                rootNetwork.orfMapPair(orfName, geneName);	
            }
            in.close();	
        }
        catch (IOException e){
            System.out.println("ERROR at readNameMap " + e.toString());
        }	    
    }
    
    /**
     * Calculates Gaussian median given the array of relational scores 
     */
    public static void calculateGaussianMedian(){
    	//we have the total list of scores, we want the median as well as the IQR
    	double median = 0;
    	int length = relationScoreArray.length;
        //huh????
        if(length==0 ) return;
    	
    	if(length % 2 == 0){
            median = (relationScoreArray[length/2] + relationScoreArray[length/2 - 1])/2.0;
    	}
    	else{
            median = relationScoreArray[(length-1)/2];
    	}

    	gaussianMean = median;
    	
    	//these quartiles are approximate, their indices may be off by 1 or 2
    	double thirdQuartile = relationScoreArray[(length*75)/100];
    	double firstQuartile = relationScoreArray[(length*25)/100];
       	gaussianIQR = (thirdQuartile - firstQuartile)/1.35;
        
        System.out.println(median+"//"+firstQuartile+"//"+thirdQuartile+"//"+gaussianIQR+"//"+relationScoreArray.length);
    }
    
    /**
     * Calculates the cutoff scores assuming scores follow a Gaussian distribution
     * The cutoffs represent where the pValueCutoff is still satisfied
     * @param leftTailCutoff cutoff used to calculate Gaussian cutoff assuming scores 
     * follow a Gaussian distribution
     */
    public static void calculateGaussianCutoffs(Double leftTailCutoff, Double rightTailCutoff){
    	double sum = 0, sumOfSquares = 0;
    	Double negCut = null;
    	Double posCut = null; 
    	
    	if (gaussianMean == null){
    		calculateGaussianMedian();
    	}
    	
        if(standardNormalDistribution == null){
            standardNormalDistribution = new Normal(0, 1, null);
        }
    	   System.out.println("relationScoreArray size = "+relationScoreArray.length);
    	   System.out.println("gaussianMean = "+gaussianMean);
           System.out.println("gaussianIQR = "+gaussianIQR);
    	for(Double d : relationScoreArray){
            
            sum += d;
            sumOfSquares += (d*d);

            double ziqr = 1.0*(d - gaussianMean)/gaussianIQR;
            double cdf = standardNormalDistribution.cdf(ziqr);
            
            if(leftTailCutoff != null){
                if(cdf <= leftTailCutoff && (negCut == null || d > negCut)){
                        negCut = d;
                }
            }
            if(rightTailCutoff != null){
                if((1 - cdf) <= rightTailCutoff && (posCut == null ||d < posCut)){
                        posCut = d;
                }
            }
    	}    	
    	if (negCut == null){
            //System.out.println("NEG CUTOFF IS NULL");
            negCut = 0.0;
            
        }
    	if (posCut == null)
    		posCut = 0.0;
    	positiveCutoff = posCut;
    	negativeCutoff = negCut;
        
        //System.out.println("#set gaussian cutoff: pos="+positiveCutoff+"\tneg="+negativeCutoff);
    }   
    
    //not used
    public void calculateRightTailGaussianCutoffs(double cutoff){
    	double sum = 0, sumOfSquares = 0;
    	Double negCut = null;
    	Double posCut = null; 
    	
    	if (gaussianMean == null){
            calculateGaussianMedian();
    	}
    	if(standardNormalDistribution == null){
            standardNormalDistribution = new Normal(0, 1, null);
        }
        
    	for(Double d : relationScoreArray){
            
            sum += d;
            sumOfSquares += (d*d);

            double ziqr = 1.0*(d - gaussianMean)/gaussianIQR;
            double cdf = standardNormalDistribution.cdf(ziqr);
            
            //right tail only
            if((1 - cdf) <= cutoff/2.0 && (posCut == null ||d < posCut)){
                posCut = d;
            }
    	}    	
    	if (posCut == null)
    		posCut = 0.0;
    	positiveCutoff = posCut;
        System.out.println("calculated: mwpds gaussian right tail (pos) cutoff:"+positiveCutoff);
        
    }
    //not used
    public void calculateLeftTailGaussianCutoffs(double cutoff){
    	double sum = 0, sumOfSquares = 0;
    	Double negCut = null;
    	Double posCut = null; 
    	
    	if (gaussianMean == null){
            calculateGaussianMedian();
    	}
    	if(standardNormalDistribution == null){
            standardNormalDistribution = new Normal(0, 1, null);
        }
        
    	for(Double d : relationScoreArray){
            
            sum += d;
            sumOfSquares += (d*d);

            double ziqr = 1.0*(d - gaussianMean)/gaussianIQR;
            double cdf = standardNormalDistribution.cdf(ziqr);
            
            //Left tail only
            if(cdf <= cutoff/2.0 && (negCut == null || d > negCut)){
                    negCut = d;
            }
    	}    	
    	if (negCut == null)
    		negCut = 0.0;
    	negativeCutoff = negCut;
        System.out.println("calculated: mwpds gaussian left tail (neg) cutoff:"+negativeCutoff);
        
    }
    
    /**
     * Recalculates Gaussian cutoffs
     * @param cutoff New cutoff used to calculate Gaussian cutoff 
     * @param pos JLabel of positive cutoff (cutoff box)
     * @param neg JLabel of negative cutoff (cutoff box)
     */
    public static void recalculateGaussianCutoffs(Double leftTailCutoff, Double rightTailCutoff, JTextField pos, JTextField neg){
    	calculateGaussianCutoffs(leftTailCutoff, rightTailCutoff);
    	pos.setText(positiveCutoff + "");
    	neg.setText(negativeCutoff + "");
    }

    /**
     * Calculates percentile cutoffs using OLD..NOT USED 
     * @param percent Percentile to cutoff
     * @param pos JLabel of positive cutoff (cutoff box)
     * @param neg JLabel of negative cutoff (cutoff box)
     */
    public void calculatePercentileCutoffs(Integer percent, JLabel pos, JLabel neg){
        if(percent > 100 || percent < 0) return;
        
        int relationListSize = fullListOfScores.size();

        // Make sure percent is less than 50, so the cutoffs match properly
//        if (percent > 50){
//                percent = 100-percent;
//        }
        leftTailPercentile = percent;
        
        int negCut = relationListSize*(leftTailPercentile)/100;
        //int posCut = relationListSize*(100-leftTailPercentile)/100;
        int posCut = relationListSize*(rightTailPercentile)/100;
        
        // To avoid possible error out of bounds exceptions
        if (negCut == relationListSize){
                negativeCutoff = relationScoreArray[relationListSize - 1];
                positiveCutoff = relationScoreArray[posCut];
        }
        else if (posCut == relationListSize){
                negativeCutoff = relationScoreArray[negCut];
                positiveCutoff = relationScoreArray[relationListSize - 1];
        }
        else{
                negativeCutoff = relationScoreArray[negCut];
                positiveCutoff = relationScoreArray[posCut];
        }
    	pos.setText(positiveCutoff + "");
    	neg.setText(negativeCutoff + "");
    }
    
    /**
     * Two tail cutoff (left and right)
     * @param leftTailPercentile
     * @param rightTailPercentile
     * @param pos textfield to update
     * @param neg textfield to update 
     */
    public static void calculateTwoTailPercentileCutoffs(Integer leftTailPercentile,Integer rightTailPercentile, JTextField pos, JTextField neg){
        
        int relationListSize = fullListOfScores.size();
        
        if(leftTailPercentile != null){
            if(leftTailPercentile > 100 || leftTailPercentile < 0) return;
            // Make sure percent is less than 50, so the cutoffs match properly
//            if (leftTailPercentile > 50){
//                    leftTailPercentile = 100-leftTailPercentile;
//            }
            
            int negCut = relationListSize*(leftTailPercentile)/100;
            
            // To avoid possible error out of bounds exceptions
            if (negCut == relationListSize){
                    negativeCutoff = relationScoreArray[relationListSize - 1];
            }else{
                    negativeCutoff = relationScoreArray[negCut];
            }
            neg.setText(negativeCutoff + "");
        }
        if(rightTailPercentile != null){
            if(rightTailPercentile > 100 || rightTailPercentile < 0) return;
            // Make sure percent is less than 50, so the cutoffs match properly
//            if (rightTailPercentile > 50){
//                    rightTailPercentile = 100-rightTailPercentile;
//            }
            
            //int posCut = relationListSize*(100-rightTailPercentile)/100;
            int posCut = relationListSize*(rightTailPercentile)/100;
            
            // To avoid possible error out of bounds exceptions
            if (posCut == relationListSize){
                System.out.println(relationListSize);
                System.out.println(relationScoreArray.length);
                    positiveCutoff = relationScoreArray[relationListSize - 1];
            }else{
                    positiveCutoff = relationScoreArray[posCut];
            }
            pos.setText(positiveCutoff + "");
        }
        System.out.println("#set percentile cutoff: pos="+positiveCutoff+"\tneg="+negativeCutoff);
        
    	
    }
    
    /**
     * Set global cutoffs 
     * @param positiveCutoff
     * @param negativeCutoff 
     */
    public void setCustomCutoffsCutoffs(Double positiveCutoff, Double negativeCutoff){
        if(positiveCutoff != null){
            this.positiveCutoff = positiveCutoff;
        }
        if(negativeCutoff != null) {
            this.negativeCutoff = negativeCutoff;
        }
        //System.out.println("#set custom cutoffs: pos="+positiveCutoff+"\tneg="+negativeCutoff);
    }
    
    public double getPosCutoff(){
    	return positiveCutoff;
    }
    
    public double getFDR(){
    	return fdr;
    }

    public double getNegCutoff(){
    	return negativeCutoff;
    }
    
    public int getTotalNumGenes(){
    	return totalNumInteractions;
    }
    
    public List<Double> getValuesWithin(){
        return valuesWithin;
    }
    
    public List<Double> getValuesBetween(){
        return valuesBetween;
    }
    
    // Returns true if there is an interaction between 2 genes in the relational file
    /**
     * Checks if two genes have a genetic interaction
     * @param gene1 Query gene
     * @param gene2 Target gene
     * @return true if both genes interact, false otherwise
     */
    public boolean hasGeneticInteraction(Gene gene1, Gene gene2){
//    	String g1 = gene1.getGeneName();
//        String g2 = gene2.getGeneName();
        String g1 = gene1.getGeneIdentifier();
        String g2 = gene2.getGeneIdentifier();
        
        Double score = 0.0;
        if(genePairsInRelationalFile.containsKey(g1 + "//" + g2)){
            score = genePairsInRelationalFile.get(g1 + "//" + g2);
        }else if(genePairsInRelationalFile.containsKey(g2 + "//" + g1)){
            score = genePairsInRelationalFile.get(g2 + "//" + g1);
        }else{
            return false;
        }
        
        if(score > positiveCutoff || score < negativeCutoff){
            return true;
        }
        return false;
        //return (genePairsInRelationalFile.containsKey(g1 + "//" + g2) 
         //       || genePairsInRelationalFile.containsKey(g2 + "//" + g1));
    }
    
    /**
     * NOT USED Creates histogram of within/between genetic interaction scores
     * using jfreechart
     */
    public void createHistogram() {
        String sNumBins = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Enter the number of BINs");
        if (sNumBins != null){
            int numberBins = Integer.parseInt(sNumBins);
            HistogramGui.showHistogram(valuesWithin.toArray(new Double[0]),
                    valuesBetween.toArray(new Double[0]), numberBins);
        }
    }
    
    /**
     * Instantiates console / log
     * @param console 
     */
    public void setConsole(JTextArea console){
        this.console = console;
    }
    
    
    public class LoadFilesTask implements Task {

        @Override
        public void run() {
            try{
            isInterrupted = false;
            loadFilesMonitor.setStatus("Please wait...");
            loadFilesMonitor.setPercentCompleted(-1);
            executeCutoffCalculations();
            loadFilesMonitor.setPercentCompleted(100);
            }catch(Exception e){
                halt();
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "<html><pre>"
                        +"<font size=4><b>The dialog has reset</b></font><br>"
                        + "An error occured while trying to load your files."
                        + "<br>Please ensure the correctness of the input files provided"
                        + "<hr><font color=red>Error message: "+e.getStackTrace()[0].toString()+"</font>"
                        + "</pre></html>", 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        @Override
        public void halt() {
            //Interrupt current thread
            isInterrupted = true;
            
            //Clean big datastuctures
            MyWizardPanelDataStorage.this.reset();
            
            //Reconstruct main gui
            MainGui.disposeWizard();
            MainGui.createAndShowGUI();
        }

        @Override
        public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
            loadFilesMonitor = tm;
        }

        @Override
        public String getTitle() {
            return "Please be patient...\nThis may take a while";
        }
        
    }
    
    public class ExecuteComputationsTask implements Task{

        @Override
        public void run() {
            try{
            isInterrupted = false;
            computationsMonitor.setPercentCompleted(-1);
            executeComputations();
            computationsMonitor.setPercentCompleted(100);
            computationsMonitor.setStatus("Done");
            }catch(Exception e){
                halt();
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "<html><pre>"
                        +"<font size=4><b>The dialog has reset</b></font><br>"
                        + "An error occured while trying to process your files."
                        + "<br>Please ensure the correctness of the input files provided"
                        + "<hr><font color=red>Error message: "+e.getStackTrace()[0].toString()+"</font>"
                        + "</pre></html>", 
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
            
        }

        @Override
        public void halt() {
//            System.out.println("Cancelled");
//            isInterrupted = true;
//            
//            //Clean up rootnetwork if any
//            if(rootNetwork != null){
//                rootNetwork.clearAll();
//            }
//            //Clean up panel
//            CytoPanelImp panel = (CytoPanelImp) 
//                    Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
//            if(panel.indexOfComponent("ICTools") != -1){
//                panel.remove(panel.indexOfComponent("ICTools"));
//            }
//            panel.setSelectedIndex(0);
//            
//            //Reconstruct wizard
//            MainGui.disposeWizard();
//            MainGui.createAndShowGUI();
        }

        @Override
        public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
            computationsMonitor = tm;
        }

        @Override
        public String getTitle() {
            return "Please be patient...\nThis may take a while";
        }
        
    }
}