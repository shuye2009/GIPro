package plugin;

import ch.rakudave.suggest.JSuggestField;
import cytoscape.task.TaskMonitor;
import guiICTools.MyWizardPanelDataStorage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;


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
import guiICTools.UpdateParamsPanel;
import guiICTools.UserManualPanel;
import java.awt.Cursor;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * @author YH
 * 
 * Contains all information regarding complexes, genes and complex edges etc.
 * Also contains functions for feeding in data.
 */

public class RootNetwork{
    List<Double> fullListOfScores, filteredListOfScores; // SHOULD BE SAVED AS ITS UNFILTERED
    //parameters for cutoff/enrichments (must be saved for on the fly updates)
    private int leftTailPercentile, rightTailPercentile;
    private boolean usePvalCutoffs, usePercentileCutoffs, useFisher, useSimulations; // useFisher is true when using Fisher, and false if using simulations
    private int numberOfTrials;
    private boolean trialForEachComplex;
    private Double pvalLeftTail, pvalRightTail;
    
    //variables that are instantiated and mutated throughout all the classes
    private Map<String, Complex> complexes;
    private Map<String, Gene> genes;
    private Map<String, ComplexEdge> complexEdges;//concatenated sourcetarget with source being less than the target
    private Map<String, String> geneToOrf;
    private ActionListener sgal;
    private JEditorPane nodeInfoPane, edgeInfoPane;
    private ComplexMutableTreeNode rootNode;
    private ComplexTreeListener treeListener;
    private JTree tree;
    private JScrollPane nodeInfoScroll, edgeInfoScroll;
    private Double withinPVal, betweenPVal, posCutoff, negCutoff, fdr;
    public Map<String, Set<Complex>> networkComplexMap;

    private FisherExact fisherExact;
    // Keep track of the total number of positive or negative interactions within or between complexes.
    private Integer totalNeg, totalPos, total;
    
    //For side panel
    JPanel mainSidebarPanel, actionsAndParamPanel;
    private static JButton createSubNet, createGeneHeatMap, createComplexHeatMap, createHistogram, exportResults;
    private static JButton subnetRawHeatmap, subnetPatterns, subnetCorrTables, subnetCorrEdges; //subnetwork btns

    List<Double> valuesWithin, valuesBetween;
    private MyWizardPanelDataStorage mwpds;
    
    boolean ctrlDown = false;
    
    Double[] relationScoreArray;
    UpdateParamsPanel upp;
    
    private static java.awt.Font boldFont = new java.awt.Font("Lucida Grande", 1, 11);
    private static java.awt.Font plainFont = new java.awt.Font("Lucida Grande", 0, 11);
    
    public Double minNodeEnrichment = 0.0;
    public Double maxNodeEnrichment = 0.0;
    
    private Double upperPerc = 0.80;
    private Double lowerPerc = 0.20;
    
    public Double maxPosGI,maxNegGI, upperPercPos, upperPercNeg, lowerPercPos, lowerPercNeg;
    
    
    /**
     * Initial RootNetwork constructor
     * @param mwpds 
     */
    public RootNetwork(MyWizardPanelDataStorage mwpds){
        complexes = new TreeMap<String, Complex>();
        genes = new TreeMap<String, Gene>();
        complexEdges = new TreeMap<String, ComplexEdge>();
        geneToOrf = new HashMap<String, String>();
        withinPVal = 0.05;
        betweenPVal = 0.05;
        totalPos = 0;
        totalNeg = 0;
        total = 0;
        this.mwpds = mwpds;
        posCutoff = mwpds.getPosCutoff();
        negCutoff = mwpds.getNegCutoff();
        fdr = mwpds.getFDR();
        networkComplexMap = new HashMap();
        fullListOfScores = mwpds.getFullListOfScores();
        filteredListOfScores = mwpds.getFilteredListOfScores();
        
        //for on the fly
        usePvalCutoffs = mwpds.getUsePvalCutoffs();
        pvalLeftTail = mwpds.getPvalLeftTail();
        pvalRightTail = mwpds.getPvalRightTail();
        
        usePercentileCutoffs = mwpds.getUsePercentileCutoff();
        leftTailPercentile = mwpds.getLeftTailPercentile();
        rightTailPercentile = mwpds.getRightTailPercentile();
        
        useFisher = mwpds.getUseFisher();
        useSimulations = mwpds.getUseSimulations();
        numberOfTrials = mwpds.getNumberOfTrials();
        trialForEachComplex = mwpds.getTrialForEachComplex();
        
        mwpds.setRootNetwork(this);
        System.out.println("RootNetwork constructed");
    }

    /**
     * RootNetwork constructor used for restoring
     * @param complexes
     * @param genes
     * @param complexEdges
     * @param geneToOrf
     * @param withinPVal
     * @param betweenPVal
     * @param totalPos
     * @param totalNeg
     * @param total
     * @param posCutoff
     * @param negCutoff
     * @param fdr
     * @param networkComplexMap 
     */
    public RootNetwork(Map<String, Complex> complexes, Map<String, Gene> genes, 
            Map<String, ComplexEdge> complexEdges, Map<String,String> geneToOrf, 
            double withinPVal, double betweenPVal, int totalPos, int totalNeg, int total,
            double posCutoff, double negCutoff, double fdr, 
            Map<String, Set<Complex>> networkComplexMap, 
            List<Double> valuesWithin, List<Double> valuesBetween, 
            List<Double> fullListOfScores, List<Double> filteredListOfScores,
            Boolean usePvalCutoffs, Double pvalLeftTail , Double pvalRightTail,
            Boolean usePercentileCutoffs, Integer leftTailPercentile, Integer rightTailPercentile,
            Boolean useFisher,Boolean useSimulations, Integer numberOfTrials,
            Boolean trialForEachComplex){

        System.err.println("Restoring GUI");
        this.complexes = complexes;
        this.genes = genes;
        this.complexEdges = complexEdges;
        this.geneToOrf = geneToOrf;
        this.withinPVal = withinPVal;
        this.betweenPVal = betweenPVal;
        this.totalPos = totalPos;
        this.totalNeg = totalNeg;
        this.total = total;
        this.posCutoff = posCutoff;
        this.negCutoff = negCutoff;
        this.fdr = fdr;
        this.networkComplexMap = networkComplexMap;
        this.valuesWithin = valuesWithin;
        this.valuesBetween = valuesBetween;
        this.fullListOfScores = fullListOfScores;
        this.filteredListOfScores = filteredListOfScores; 
        
        //on the fly
        this.usePvalCutoffs = usePvalCutoffs;
        this.pvalLeftTail = pvalLeftTail;
        this.pvalRightTail = pvalRightTail;
        
        this.usePercentileCutoffs = usePercentileCutoffs;
        this.leftTailPercentile = leftTailPercentile;
        this.rightTailPercentile = rightTailPercentile;
        
        this.useFisher = useFisher;
        this.useSimulations = useSimulations;
        this.numberOfTrials = numberOfTrials;
        this.trialForEachComplex = trialForEachComplex;

        createGui(posCutoff, negCutoff, fdr);	
        System.err.println("Restored!");
        
    }
    
    public void refreshMaxMin(){
        
        if(maxNegGI == null || maxPosGI == null){
            List<Double> pos = new ArrayList();
            List<Double> neg = new ArrayList();
            
            maxNegGI = 0.0;
            maxPosGI = 0.0;
            for(Double d: filteredListOfScores){
                if(d>0){
                    if(d > maxPosGI){
                        maxPosGI = d;
                    }
                    pos.add(d);
                }if(d<0){
                    if(d < maxNegGI){
                        maxNegGI = d;
                    }
                    neg.add(d * -1);
                }
            }
            
            if(pos.size()>0){
                Collections.sort(pos);
                Double d = (pos.size() + 0.5) * upperPerc;
                upperPercPos = pos.get(d.intValue());
                d = (pos.size() + 0.5) * lowerPerc;
                lowerPercPos = pos.get(d.intValue());
            }if(neg.size()>0){
                Collections.sort(neg);
                Double d = (neg.size() + 0.5) * upperPerc;
                upperPercNeg = neg.get(d.intValue());
                d = (neg.size() + 0.5) * lowerPerc;
                lowerPercNeg = neg.get(d.intValue());
            }
        }
    }
    
    public Double getMaxPos(){
        return maxPosGI;
    }
    
    public Double getMaxNeg(){
        return maxNegGI;
    }
    
    public Double getLowerPercPos(){
        if(lowerPercPos == null) return 0.0;
        return lowerPercPos;
    }
    public Double getUpperPercPos(){
        if(upperPercPos == null) return 0.0;
        return upperPercPos;
    }
    public Double getLowerPercNeg(){
        if(lowerPercNeg == null) return 0.0;
        return lowerPercNeg;
    }
    public Double getUpperPercNeg(){
        if(upperPercNeg == null) return 0.0;
        return upperPercNeg;
    }
    
    public void clearAll(){
        complexes = null;
        genes = null;
        complexEdges = null;
        geneToOrf = null;
        networkComplexMap = null;
        valuesWithin = null;
        valuesBetween = null;
        totalPos = null;
        totalNeg = null;
        total = null;
        posCutoff = null;
        negCutoff = null;
        withinPVal = null;
        betweenPVal = null;
        
        mwpds = null;
        
        
    }
    
    public List<Double> getFullListOfScores(){return fullListOfScores;}
    public List<Double> getFilteredListOfScores(){return filteredListOfScores;}
    
    /**
     * Used to update rootnetworks enrichment/cutoff params if cutoff is changed via on the fly
     */
    public void updateAllParameters(
            boolean usePvalCutoffs, double pvalLeftTail, double pvalRightTail,
            boolean usePercentileCutoffs, int leftTailPercentile, int rightTailPercentile, 
            double customNegCutoff, double customPosCutoff,
            double fdr,
            boolean useFisher, boolean useSimulations, int numberOfTrials, boolean trialForEachComplex){
            
        this.usePvalCutoffs = usePvalCutoffs;
        this.pvalLeftTail = pvalLeftTail;
        this.pvalRightTail = pvalRightTail;
        this.usePercentileCutoffs = usePercentileCutoffs;
        this.leftTailPercentile = leftTailPercentile;
        this.rightTailPercentile = rightTailPercentile;
        this.posCutoff = customPosCutoff;
        this.negCutoff = customNegCutoff;
        
        this.fdr = fdr;
        this.useFisher = useFisher;
        this.useSimulations = useSimulations;
        this.numberOfTrials = numberOfTrials;
        this.trialForEachComplex = trialForEachComplex;
        
        
        
        
    }   
    
    public boolean getUsePvalCutoffs(){return usePvalCutoffs;}
    public Double getPvalLeftTail(){return pvalLeftTail;}
    public Double getPvalRightTail(){return pvalRightTail;}
    
    public boolean getUsePercentileCutoffs(){return usePercentileCutoffs;}
    public int getLeftTailPercentile(){return leftTailPercentile;}
    public int getRightTailPercentile(){return rightTailPercentile;}
    
    public boolean getUseFisher(){return useFisher;}
    public boolean getUseSimulations(){return useSimulations;}
    public int getNumberOfTrials(){return numberOfTrials;}
    public boolean getTrailForEachComplex(){return trialForEachComplex;}
    
    public MyWizardPanelDataStorage rebuildMwpds(){
        Cytoscape.getDesktop().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        System.out.println(">> Rebuilding MWPDS...");
        long start = System.currentTimeMillis();
        
        HashSet<String> genesInRelationList = new HashSet(genes.keySet());
        System.out.println(">> full list of relations size = "+ fullListOfScores.size());
        
        if(relationScoreArray == null){
            relationScoreArray = fullListOfScores.toArray(new Double[fullListOfScores.size()]);
            Arrays.sort(relationScoreArray);
            System.out.println(">> Building relation score array from saved state");
        }else{
            System.out.println(">> Found relation score array already built, using it..");
        }
        
        
        System.out.println(">> relation score array size = "+ relationScoreArray.length);
        
        MyWizardPanelDataStorage rebuilt_mwpds = new MyWizardPanelDataStorage();
        rebuilt_mwpds.setMwpdsInitVariables(
                genesInRelationList,
                fullListOfScores, filteredListOfScores,
                relationScoreArray, posCutoff, negCutoff);
        
        long elapsedTimeMillis = System.currentTimeMillis()-start;
        // Get elapsed time in seconds
        float elapsedTimeSec = elapsedTimeMillis/1000F;
        System.out.println(">> Done rebuild in "+elapsedTimeSec+" seconds");
        
        Cytoscape.getDesktop().setCursor(Cursor.getDefaultCursor());
        return rebuilt_mwpds;
        
    }
    
    /*FOR HISTOGRAM SAVE*/
    public void setValuesWithinAndBetween(List<Double> within, List<Double> between){
        valuesWithin = within;
        valuesBetween = between;
    }
    
    /**
     * Creates histogram of within/between genetic interaction scores
     * using jfreechart
     */
    public void createHistogram() {
        System.out.println(valuesWithin);
        String sNumBins = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Enter the number of bins (150-200 recommended)");
        
        try{
            if (sNumBins != null){
                int numberBins = Integer.parseInt(sNumBins);
                HistogramGui.showHistogram(valuesWithin.toArray(new Double[0]),
                        valuesBetween.toArray(new Double[0]), numberBins);
            }
        }catch(Exception e){
            JOptionPane.showMessageDialog(null,
                            "Unable to plot histogram, not enough data!",
                            "Error",
                            JOptionPane.ERROR_MESSAGE);
        }
    }


    public void insertComplexGenePair(String complexName, String geneIdentifier, String desc, 
            int numGenes, boolean geneInRelationList){
        // get or create the complex
        Complex complex = null;
        if(!complexes.containsKey(complexName)){
            complex = new Complex(complexName);
            complexes.put(complexName, complex);
        }
        else{
            complex = complexes.get(complexName);
        }

        // get or create the gene
        Gene gene = null;
        if(!genes.containsKey(geneIdentifier)){
            gene = new Gene(null, geneIdentifier);
            genes.put(geneIdentifier, gene);
        }
        else{
            gene = genes.get(geneIdentifier);
        }

        complex.addGene(gene);
        
        //Set desc
        gene.setDescription(desc);
        if (geneInRelationList){
            complex.addGeneInRelationList(gene);
        }
        gene.addComplex(complex);
    }

    /**
     * Initiates complex data by executing Complex.java's inititateInfo()
     * Also populates complexEdges
     * @param genesInRelationList Genes in genetic interaction file
     */
    public void initializeComplexData(HashSet<String> genesInRelationList) {
        System.out.println("Initializing complex data for "+complexes.size() + " complexes");
        //initialize single complexes
        for(Complex c : complexes.values()){ 
            int actualInteractions = 0;
            // find the number of "actual interactions" and use this value.
            Set <Gene> genes = c.getActualGenes();
            for (Gene gene1 : genes){
                for (Gene gene2 : genes){
                    if (! gene1.getGeneIdentifier().equals(gene2.getGeneIdentifier()) && mwpds.hasGeneticInteraction(gene1, gene2)){
                        actualInteractions ++;
                    }
                }
            }
            // Divide by two (interactions were counted twice)
        c.initiateInfo(actualInteractions/2);
        }

        //between complexes
        for(String complex1 : complexes.keySet()){
            for(String complex2 : complexes.keySet()){
                Complex c1 = complexes.get(complex1);
                Complex c2 = complexes.get(complex2);
                // switch if necessary
                if(c1.compareTo(c2) > 0){
                    c1 = complexes.get(complex2);
                    c2 = complexes.get(complex1);
                }

                if(!c1.equals(c2)){
                    
                    boolean b = (c1.equals("Rpd3L complex") && c2.equals("Snt2C complex"))||
                            (c1.equals("Snt2C complex") && c2.equals("Rpd3L complex"));
                    //now c1 is smaller
                    String interaction = c1.getName() + "//" + c2.getName();
                    String interaction2 = c2.getName() + "//" + c1.getName();

                    if(!complexEdges.containsKey(interaction) && !complexEdges.containsKey(interaction2)){
                        int numberOfPossiblePairs = c1.size() * c2.size();
                        // genes that are in both complexes
                        int redundantEdges = 0;
                        // pairs of genes that are in the relational file
                        int actualPairs = 0;
                        
                        Set<Gene> sharedGenes = new HashSet();
                        for(Gene gene : c1.getGenes()){
                            if(c2.containsGene(gene)){
                                redundantEdges++;
                                
                                sharedGenes.add(gene);
                                System.out.println("#SHARED GENE = "+gene.getGeneIdentifier());
                            }
                        }
                        
                        //int before = 0;
                        // Find the number of actual pairs
                        for (Gene gene1 : c1.getActualGenes()){
                            for (Gene gene2 : c2.getActualGenes()){
//                               if(mwpds.hasGeneticInteraction(gene1, gene2)){
//                                    System.out.println("#GI="+gene1.getGeneIdentifier()+" - "+gene2.getGeneIdentifier());
//                                    before++;
//                                }
                                
                                if (mwpds.hasGeneticInteraction(gene1, gene2)
                                        && !sharedGenes.contains(gene1) 
                                        && !sharedGenes.contains(gene2) ){
                                    
                                    //System.out.println("#GI2="+gene1.getGeneIdentifier()+" - "+gene2.getGeneIdentifier());
                                    if(b) System.out.println("###GI BETWEEN:"+gene1+"_"+gene2);
                                    actualPairs ++;
                                }
                            }
                        }
                        if(b){
                            System.out.println("#actualPairs="+actualPairs); 
                            System.out.println("#numberOfPossiblePairs="+numberOfPossiblePairs);
                            System.out.println("#redundantEdges="+redundantEdges);
                        }
                        //System.out.println("#BEFORE="+before);
                        //System.out.println("#AFTER="+actualPairs);
                        int totalPairs = numberOfPossiblePairs - redundantEdges;
                        ComplexEdge newEdge = new ComplexEdge(c1, c2, totalPairs, actualPairs);
                        complexEdges.put(interaction, newEdge);
                    }
                }
            }
        }
            System.out.println("Done initializing complex data");
}//end initializeComplexData

    /**
     * Add physical SubEdge to both passed genes 
     * @param gene1Name Name of source gene
     * @param gene2Name Name of target gene
     * @param score Score of interaction
     */
    public void processPhysicalInteraction(String gene1Name, 
            String gene2Name, Double score) {

        Gene g1 = genes.get(gene1Name);
        Gene g2 = genes.get(gene2Name);

        if(g1 != null && g2 != null){
            g1.addEdge(new SubEdge(true, score, g2));
            g2.addEdge(new SubEdge(true, score, g1));
        }
    }

    /**
     * Update totalPos/totalNeg count given that score meets cutoff 
     * Also, add genetic SubEdge to both passed genes
     * @param gene1 Source gene
     * @param gene2 Target gene
     * @param score Genetic interaction score
     * @param positiveCutoff Positive cutoff
     * @param negativeCutoff Negative cutoff
     */
    public void processGeneticInteraction(String gene1, String gene2, 
            Double score, double positiveCutoff, double negativeCutoff) {
        
        Gene g1 = genes.get(gene1);
        Gene g2 = genes.get(gene2);
        
        if (score > positiveCutoff){
            //System.out.println("Add positive -> "+gene1+"\t"+gene2+"\t"+score);
            totalPos++;
        }
        else if (score < negativeCutoff){
            //System.out.println("Add negative -> "+gene1+"\t"+gene2+"\t"+score);
            totalNeg++;
        }
        
        //THIS SHOULD COME BEFORE? OR WHAT, causes issues when rebuilding cpx network
        if(g1 == null || g2 == null || g1.equals(g2)){
            return;
        }
        
        //Add an edge if the score meets the cutoff
        if(score > positiveCutoff || score < negativeCutoff){
            g1.addEdge(new SubEdge(false, score, g2));
            g2.addEdge(new SubEdge(false, score, g1));
        }
        
        //Add unfiltered score for pearson correlations
        g1.addUnfilteredGeneticEdge(new SubEdge(false, score, g2));
        g2.addUnfilteredGeneticEdge(new SubEdge(false, score, g1));
        
        Set<Complex> c1Set = g1.getComplexes();
        Set<Complex> c2Set = g2.getComplexes();

        // If both genes are EACH contained in at least one complex
        if(c1Set.size() > 0 && c2Set.size() > 0){
            for(Complex complex1 : c1Set){
                for(Complex complex2 : c2Set){
                    Complex c1 = null;
                    Complex c2 = null;

                    if(complex1.compareTo(complex2) > 0){
                        c1 = complex2;
                        c2 = complex1;
                    }
                    else{
                        c1 = complex1;
                        c2 = complex2;
                    }
                    //now c1 is smaller

                    // Complexes are different -> between complex interaction (add score to complex edge)
                    if(c1 != c2 && !c1.equals(c2)){
                        ComplexEdge ce = complexEdges.get(c1.getName() + "//" + c2.getName());
                        if (ce != null){
                            ce.processScore(score, positiveCutoff, negativeCutoff);
                        }
                        else{
                            System.err.println("ERROR BUG: processGeneticInteraction");
                        }
                    }
                    // Complexes are same -> within complex interaction (add score to complex)
                    else{
                        c1.processScore(score, positiveCutoff, negativeCutoff);
                    }	
                }
            }
        }
    }
    //end processGeneticInteraction

    /**
     * Performs complex fisher trials on within genetic interactions
     * @param posCutoff Positive cutoff
     * @param negCutoff Negative cutoff
     * @param fdr False discovery rate
     */
    public void withinComplexFisherTrials(double posCutoff, double negCutoff, double fdr){
        //System.out.println("Performing within complex trials");
        total = mwpds.getTotalNumGenes();
        System.out.println("=======WITHIN COMPLEX TRIALS===========");
        System.out.println("fdr = "+fdr);
        System.out.println("total = "+total);
        System.out.println("totalPos = "+totalPos);
        System.out.println("totalNeg = "+totalNeg);
        System.out.println("=======================================");
        
        int nonPositive = total - totalPos;
        int nonNegative = total - totalNeg;

        List<Double> pVals = new ArrayList<Double>();

        for(Complex c : complexes.values()){
            int numberOfEdgesInCpx = c.getNumInteractions();
            int posRelationsInCpx = c.posRelations();
            int negRelationsInCpx = c.negRelations();
            
            int nonPosRelations = numberOfEdgesInCpx - posRelationsInCpx;
            int nonNegRelations = numberOfEdgesInCpx - negRelationsInCpx;
            
//            System.out.println("##### COMPLEX="+c.getName());
//            System.out.println("#numberOfEdgesInCpx="+numberOfEdgesInCpx);
//            System.out.println("#posRelationsInCpx="+posRelationsInCpx);
//            System.out.println("#negRelationsInCpx="+negRelationsInCpx);
//            System.out.println("#nonPosRelations="+nonPosRelations);
//            System.out.println("#nonNegRelations="+nonNegRelations);
//            System.out.println("#totalPos="+totalPos);
//            System.out.println("#totalNeg="+totalNeg);
            
            
            
            if (posRelationsInCpx > 0){
//                System.out.println(posRelationsInCpx + " $ " +(totalPos - posRelationsInCpx)  +
//                        " $ " + nonPosRelations + " $ " + (nonPositive - nonPosRelations));
                double posPVal = getFisherExactTestPVal
                        (posRelationsInCpx, totalPos - posRelationsInCpx, 
                        nonPosRelations, nonPositive - nonPosRelations);
                
                pVals.add(posPVal);
                c.setPosPValue(posPVal);
            }
            if (negRelationsInCpx > 0){
//                 System.out.println(posRelationsInCpx + " $ " +(totalPos - posRelationsInCpx)  +
//                        " $ " + nonPosRelations + " $ " + (nonPositive - nonPosRelations));
                double negPVal = getFisherExactTestPVal
                        (negRelationsInCpx, totalNeg - negRelationsInCpx, 
                        nonNegRelations, nonNegative - nonNegRelations);
                pVals.add(negPVal);
                c.setNegPValue(negPVal);
            }
            
        }

        //withinPVal = generateFDRPValue(pVals, fdr);
        withinPVal = 1.1;   // disable entichment analysis for within complex
        //System.out.println("within " +withinPVal);
        for (Complex c : complexes.values()){
            c.setSignificance(withinPVal);
        }
        //System.out.println("Done performing within complex trials");
    }//end withinComplexTrials

    /**
     * Performs complex fisher trials on between genetic interactions
     * @param posCutoff Positive cutoff
     * @param negCutoff Negative cutoff
     * @param fdr False discovery rate
     */
    public void betweenComplexFisherTrials(double posCutoff, double negCutoff, double fdr){
//        System.out.println("=======BETWEEN COMPLEX TRIALS===========");
//        System.out.println("fdr = "+fdr);
//        System.out.println("total = "+total);
//        System.out.println("totalPos = "+totalPos);
//        System.out.println("totalNeg = "+totalNeg);
//        System.out.println("=======================================");
        
        Set<String> insignificantKeys = new HashSet<String>();
        List<Double> pVals = new ArrayList<Double>();
        total = mwpds.getTotalNumGenes();
        int totalNonPos = total - totalPos;
        int totalNonNeg = total - totalNeg;

        for(String complexPair : complexEdges.keySet()){	
            ComplexEdge ce = complexEdges.get(complexPair);
            int allRelationsInEdge = ce.actualNumberEdges();
            int posRelationsInEdge = ce.posRelations();
            int negRelationsInEdge = ce.negRelations();
            
            int nonPosRelations = allRelationsInEdge - posRelationsInEdge;
            int nonNegRelations = allRelationsInEdge - negRelationsInEdge;
            //System.out.println("PAIR:"+complexPair+"\t");
                //System.out.println("\t#All relations:"+allRelationsInEdge);
            if (posRelationsInEdge > 0){
                //System.out.println("\t#"+posRelationsInEdge+"\t"+(totalPos - posRelationsInEdge)+"\t"+nonPosRelations+"\t"+(totalNonPos - nonPosRelations));
                double posPVal = getFisherExactTestPVal
                        (posRelationsInEdge, totalPos - posRelationsInEdge,
                        nonPosRelations, totalNonPos - nonPosRelations);
                pVals.add(posPVal);
                ce.setPosPValue(posPVal);
            }
            if (negRelationsInEdge > 0){
                //System.out.println("\t$"+negRelationsInEdge+"\t"+(totalNeg - negRelationsInEdge)+"\t"+nonNegRelations+"\t"+(totalNonNeg - nonNegRelations));
                
                double negPVal = getFisherExactTestPVal
                        (negRelationsInEdge, totalNeg - negRelationsInEdge, 
                        nonNegRelations, totalNonNeg - nonNegRelations);
                pVals.add(negPVal);
                ce.setNegPValue(negPVal);
            }
        }

        betweenPVal = generateFDRPValue(pVals, fdr);
        //System.out.println("between " + betweenPVal);

        for (String complexPair : complexEdges.keySet()){	
            ComplexEdge ce = complexEdges.get(complexPair);
            ce.setSignificance(betweenPVal);

            if(ce.getSignificance() == null){
                    insignificantKeys.add(complexPair);
            }			
        }
        for(String s : insignificantKeys){
            complexEdges.remove(s);
        }
    }

    /**
     * Perform simulation trails on within complex interactions
     * @param numberOfTrials Number of trials 
     * @param trialForEachComplex Number of trials for each complex
     * @param storedTrials Stored Trials
     * @param listOfRelations List of scores in the relational file
     * @param posCutoff Positive cutoff
     * @param negCutoff Negative cutoff
     * @param fdr False discovery rate
     */
    public void withinComplexSimulationTrials(int numberOfTrials, boolean trialForEachComplex, 
        HashMap<Integer, int[][]> storedTrials, List<Double> listOfRelations, 
        double posCutoff, double negCutoff, double fdr){

        System.out.println("Performing within complex trials");

        List<Double> pVals = new ArrayList<Double>();

        for(Complex c : complexes.values()){
            int numberOfEdges = c.getNumInteractions();
            int posRelations = c.posRelations();
            int negRelations = c.negRelations();

            //create bothTrials by generating a new distribution for both positive and negative. 
            // 0 is an array of positive trials, 1 is an array of negative trials
            int[][] bothTrials = new int[2][];

            // if a trial has already been performed for the given number of edges, assume
            // the same kind of behaviour for this complex
            if(!trialForEachComplex && storedTrials.containsKey(new Integer(numberOfEdges))){
                bothTrials = storedTrials.get(new Integer(numberOfEdges));
            }
            // Otherwise do the calculation
            else{
                int[] posTrials = new int[numberOfTrials];
                int[] negTrials = new int[numberOfTrials];

                //populate posTrials and negTrials
                getDistribution(posTrials, negTrials, numberOfEdges, numberOfTrials, listOfRelations, posCutoff, negCutoff);

                bothTrials[0] = posTrials;
                bothTrials[1] = negTrials;

                // Store the results of this distribution if they might be reused (we are not trailing for each complex)
                if(!trialForEachComplex){
                    storedTrials.put(new Integer(numberOfEdges), bothTrials);
                }
            }
            if (posRelations > 0){
                double posPValue = getPVal(bothTrials[0], posRelations, numberOfTrials);
                pVals.add(posPValue);
                c.setPosPValue(posPValue);
            }
            if (negRelations > 0){
                double negPValue = getPVal(bothTrials[1], negRelations, numberOfTrials);
                pVals.add(negPValue);
                c.setNegPValue(negPValue);
            }
        }

        withinPVal = generateFDRPValue(pVals, fdr);
        for (Complex c : complexes.values()){
                c.setSignificance(withinPVal);
        }
        System.out.println("Done performing within complex trials");
    }

    /**
     * Perform simulation trails on between complex interactions
     * @param numberOfTrials Number of trials 
     * @param trialForEachComplex Number of trials for each complex
     * @param storedTrials Stored Trials
     * @param listOfRelations List of scores in the relational file
     * @param posCutoff Positive cutoff
     * @param negCutoff Negative cutoff
     * @param fdr False discovery rate
     */
    public void betweenComplexSimulationTrials(int numberOfTrials, boolean trialForEachComplex,
            HashMap<Integer, int[][]> storedTrials, List<Double> listOfRelations,
            double posCutoff, double negCutoff, double fdr){
            Set<String> insignificantKeys = new HashSet<String>();
            List<Double> pVals = new ArrayList<Double>();

            for(String complexPair : complexEdges.keySet()){	
                    ComplexEdge ce = complexEdges.get(complexPair);
                    int actualNumberOfPairs = ce.actualNumberEdges();
                    int posRelations = ce.posRelations();
                    int negRelations = ce.negRelations();

                    /* 
                     * Create the bothTrials array by generating a new distribution for both 
                     * positive and negative relations. bothTrials[0] is an array of positive 
                     * trials, bothTrials[1] is an array of negative trials.
                     */
                    int[][] bothTrials = new int[2][];

                    if(!trialForEachComplex && storedTrials.containsKey(new Integer(actualNumberOfPairs))){
                            bothTrials = storedTrials.get(new Integer(actualNumberOfPairs));
                    }
                    else{
                            int[] posTrials = new int[numberOfTrials];
                            int[] negTrials = new int[numberOfTrials];

                            //populate posTrials and negTrials
                            getDistribution(posTrials, negTrials, actualNumberOfPairs, numberOfTrials, listOfRelations, posCutoff, negCutoff);

                            bothTrials[0] = posTrials;
                            bothTrials[1] = negTrials;

                            if(!trialForEachComplex){
                                    storedTrials.put(new Integer(actualNumberOfPairs), bothTrials);
                            }
                    }

                    if (posRelations > 0){
                            double posPValue = getPVal(bothTrials[0], posRelations, numberOfTrials);
                            ce.setPosPValue(posPValue);
                            pVals.add(posPValue);
                    }
                    if (negRelations > 0){
                            double negPValue = getPVal(bothTrials[1], negRelations, numberOfTrials);
                            ce.setNegPValue(negPValue);
                            pVals.add(negPValue);
                    }
            }//end while

            betweenPVal = generateFDRPValue(pVals, fdr);

            for (String complexPair : complexEdges.keySet()){	
                    ComplexEdge ce = complexEdges.get(complexPair);
                    ce.setSignificance(betweenPVal);
                    if(ce.getSignificance() == null){
                            insignificantKeys.add(complexPair);
                    }
            }

            for(String s : insignificantKeys){
                    complexEdges.remove(s);
            }
    }

    /**
     * Populate geneToOrf with passed gene / orf name
     * @param orfName Orf name
     * @param geneName Gene name
     */
    public void orfMapPair(String orfName, String geneName) {
        Gene g = genes.get(orfName);
        if(g != null){
            genes.get(orfName).setGeneName(geneName);
            geneToOrf.put(geneName, orfName);
        }
    }

    /**
     * Checks if two genes belong in the same complex
     * @param gene1 Source gene
     * @param gene2 Target gene
     * @return true if both genes exists in the same complex, false otherwise
     */
    public boolean inSameComplex(String gene1, String gene2) {
        Gene g1 = genes.get(gene1);
        Gene g2 = genes.get(gene2);

        if(g1 == null || g2 == null){
            return false;
        }

        Set<Complex> g1Complexes = g1.getComplexes();
        Set<Complex> g2Complexes = g2.getComplexes();

        for(Complex c1 : g1Complexes){
            for(Complex c2 : g2Complexes){
                if(c1 == c2){//In this case, c1.equals(c2) is true iff c1 == c2
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if both genes passed exist in the inputted data 
     * @param gene1 Source gene
     * @param gene2 Target gene
     * @return true if both genes exist, false otherwise
     */
    public boolean genesExist(String gene1, String gene2) {
        Gene g1 = genes.get(gene1);
        Gene g2 = genes.get(gene2);
        return g1 != null && g2 != null;
    }
    
    /**********************
     *END OF COMPUTATIONS *
     **********************
     */

    /**
     * Restores JTree, Listeners etc.. to initial state from saved objects
     * Re-instantiates objects
     */
    public void restoreRootNetwork(){
        System.out.println("Restoring...");
        rootNode.removeAllChildren();
        //restore tree
        for(String c : complexes.keySet()){
            Complex cc = complexes.get(c);
            GeneMutableTreeNode complexTreeNode = new GeneMutableTreeNode(cc);
            
            for(Gene g : cc.getGenes()){
                GeneMutableTreeNode geneNode = new GeneMutableTreeNode(g);
                complexTreeNode.add(geneNode);
            }
            rootNode.add(complexTreeNode);
        }

        System.out.println("Saved networks:");
        Set<CyNetwork> networks = Cytoscape.getNetworkSet();

        for(CyNetwork network: networks){
            String name = network.getIdentifier();
            CyAttributes networkAtr = Cytoscape.getNetworkAttributes();
            String type = networkAtr.getAttribute(network.getIdentifier(), GIProAttributeNames.NETWORK_TYPE).toString();
            
            if(type.equalsIgnoreCase("Subnetwork")){
                System.out.println(name + "\tSub-Network");
                SubNetSelectionListener snsl = new SubNetSelectionListener
                        (genes, nodeInfoPane, edgeInfoPane, tree, networkComplexMap.get(name));
                network.addSelectEventListener(snsl);
                GeneNodeContextMenu menu = new GeneNodeContextMenu(network, 
                        (SideGuiActionListener) sgal);
                Cytoscape.getNetworkView(network.getIdentifier())
                        .addNodeContextMenuListener(menu);
            }else{
                System.out.println(name + "\tComplex-Network");
                RootNetworkSelectionListener rnsl = new RootNetworkSelectionListener
                        (this, complexes, complexEdges, nodeInfoPane, edgeInfoPane, tree);
                network.addSelectEventListener(rnsl);
                ComplexNodeContextMenu menu = new ComplexNodeContextMenu(network, 
                        (SideGuiActionListener) sgal);
                Cytoscape.getNetworkView(network.getIdentifier())
                        .addNodeContextMenuListener(menu);

                Visualization.createVisualStyle();
                Visualization.refreshNetworkView(Cytoscape.createNetworkView(network), true, false, false);
                
                //listens for focus changes
                CytoscapeEventListener cel = new CytoscapeEventListener(this);
            }
            treeListener.registerCyNetwork(network);
            ((SideGuiActionListener) sgal).setNetwork(network);
        }

        System.out.println("Register tree");
        CyNetwork currNetwork = Cytoscape.getCurrentNetwork();
        treeListener.registerCyNetwork(currNetwork); 
        ((SideGuiActionListener) sgal).setNetwork(currNetwork);
        
        //for ctrldown
        
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyDispatcher());
    }

    /**
     * This method actually creates the graph node/edge visualizations
     */
    public void createRootGraph() {
        CytoPanelImp ctrlPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
        int previousIndex = ctrlPanel.getSelectedIndex();
        
        //Network atributes
        CyAttributes networkAtr = Cytoscape.getNetworkAttributes();
        
        //create the complex nodes
        CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();
        CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
        
        CyNetwork mainNetwork = null;
        
        for(CyNetwork net: Cytoscape.getNetworkSet()){
            String t = networkAtr.getAttribute(net.getIdentifier(), "GIPro_NetworkType").toString();
            if(t.equalsIgnoreCase("Complex")){
                mainNetwork = net;
            }
        }
        
        //If no complex network found, create a new one
        if(mainNetwork == null){
            mainNetwork = Cytoscape.createNetwork("Complex Network", true);
        }
        else{//Network already exists, destroy it and create a new one
            for(CyNetwork n: Cytoscape.getNetworkSet()){
                Cytoscape.destroyNetworkView(n);
                Cytoscape.destroyNetwork(n);
            }
            mainNetwork = Cytoscape.createNetwork("Complex Network", true);
            this.populateComplexActionsAndParams();
        }
        //Add attribute to network to distinguish gene networks from complex networks
        networkAtr.setAttribute(mainNetwork.getIdentifier(), "GIPro_NetworkType", "Complex");
        
        //FOCUS CHANGES IN NETWORK + OTHER STUFF
        CytoscapeEventListener nsl = new CytoscapeEventListener(this);
        
        boolean isRebuild = (rootNode.getChildCount() > 0);
        System.out.println("isRebuild = "+isRebuild);
        for(String c : complexes.keySet()){
            CyNode n = Cytoscape.getCyNode(c, true);
            mainNetwork.addNode(n);
            Complex cc = complexes.get(c);
            String significance = cc.getSignificance();
            nodeAttrs.setAttribute(n.getIdentifier(), GIProAttributeNames.COMPLEX_COLOR, significance);
            nodeAttrs.setAttribute(n.getIdentifier(), GIProAttributeNames.DISPLAY_NAME, n.getIdentifier());
            nodeAttrs.setAttribute(n.getIdentifier(), GIProAttributeNames.IS_SUBNET, false);

            int numGenes = cc.getGenes().size();
            double nodeSize = asymptoticallyLogFunction(numGenes);
            nodeAttrs.setAttribute(n.getIdentifier(), GIProAttributeNames.NODE_SIZE, nodeSize);
            
            if(cc.getSignificance().equals(Config.nodePosSignificanceKey)){
                Double logScore = - Math.log10(cc.posPValue());
                if(logScore > maxNodeEnrichment){
                    maxNodeEnrichment = logScore;
                }
                nodeAttrs.setAttribute(n.getIdentifier(), 
                        GIProAttributeNames.ENRICHMENT_SCORE, - Math.log10(cc.posPValue()));
            }
            
            if(cc.getSignificance().equals(Config.nodeNegSignificanceKey)){
                Double logScore = Math.log10(cc.negPValue());
                if(logScore < minNodeEnrichment){
                    minNodeEnrichment = logScore;
                }
                
                nodeAttrs.setAttribute(n.getIdentifier(), 
                        GIProAttributeNames.ENRICHMENT_SCORE, logScore);
            }
            
            if(isRebuild) continue;// Dont need to add complex nodes if its rebuild
            GeneMutableTreeNode complexTreeNode = new GeneMutableTreeNode(cc);
                for(Gene g : cc.getGenes()){
                    DefaultMutableTreeNode geneNode = new DefaultMutableTreeNode(g);
                    complexTreeNode.add(geneNode);
                }
                rootNode.add(complexTreeNode);
        }
        
        
        Double maxPosLog = 0.0;
        Double maxNegLog = 0.0;
        
        List<Double> allLogs = new ArrayList();
        
        Map<CyEdge, Double> cyEdgesPos = new HashMap();
        Map<CyEdge, Double> cyEdgesNeg = new HashMap();
        
        //connect the intercomplex edges
        for(ComplexEdge ce : complexEdges.values()){
            String sourceId = ce.getSource().getName();
            String targetId = ce.getTarget().getName();
            String significance = ce.getSignificance();
            double pearson = ce.averagePearsonCorrelation();
            
            if (significance.equals(Config.edgeBothSignificanceKey)){
                    CyEdge ePos = Cytoscape.getCyEdge(sourceId, sourceId + "//" + targetId, targetId, "cc");
                    CyEdge eNeg = Cytoscape.getCyEdge(targetId, sourceId + "//" + targetId, sourceId, "cc");

                    mainNetwork.addEdge(ePos);
                    mainNetwork.addEdge(eNeg);
                    edgeAttrs.setAttribute(ePos.getIdentifier(), GIProAttributeNames.EDGE_TYPE, Config.edgePosSignificanceKey);
                    edgeAttrs.setAttribute(eNeg.getIdentifier(), GIProAttributeNames.EDGE_TYPE, Config.edgeNegSignificanceKey);

                    edgeAttrs.setAttribute(ePos.getIdentifier(), GIProAttributeNames.WEIGHT, pearson);
                    edgeAttrs.setAttribute(eNeg.getIdentifier(), GIProAttributeNames.WEIGHT, pearson);
                    
                    
                    Double posPval = Math.log10( - Math.log10(ce.posPValue()));
                    Double negPval = Math.log10( - Math.log10(ce.negPValue()));
                    
                    allLogs.add(posPval);
                    allLogs.add(negPval);
                    
                    //Pos stuff;
                    if(posPval>maxPosLog) maxPosLog = posPval;
                    cyEdgesPos.put(ePos, posPval);
                    
                    //Neg stuff
                    if(negPval>maxNegLog) maxNegLog = negPval;
                    cyEdgesNeg.put(eNeg, negPval);
                    
                    
//                    edgeAttrs.setAttribute(ePos.getIdentifier(), GIProAttributeNames.EDGE_WIDTH, 3.5);
//                    edgeAttrs.setAttribute(eNeg.getIdentifier(), GIProAttributeNames.EDGE_WIDTH, 3.5);
            }
            else{
                    CyEdge e = Cytoscape.getCyEdge(sourceId, sourceId + "//" + targetId, targetId, "cc");
                    mainNetwork.addEdge(e);
                    edgeAttrs.setAttribute(e.getIdentifier(), GIProAttributeNames.EDGE_TYPE, significance);
                    edgeAttrs.setAttribute(e.getIdentifier(), GIProAttributeNames.WEIGHT, pearson);
                    
                    //Check signifiance and update maps containing number of interactions accordingly to be used 
                    //For edge width later
                    if(significance.equals(Config.edgePosSignificanceKey)){
                        Double posPval = Math.log10(- Math.log10(ce.posPValue()));
                        allLogs.add(posPval);
                        if(posPval>maxPosLog) maxPosLog = posPval;
                        cyEdgesPos.put(e, posPval);
                    }else{
                        Double negPval = Math.log10(- Math.log10(ce.negPValue()));
                        allLogs.add(negPval);
                        if(negPval>maxNegLog) maxNegLog = negPval;
                        cyEdgesNeg.put(e, negPval);
                    }
                    
                    
            }
        }
        System.out.println("$maxPosLog="+maxPosLog);
        System.out.println("$maxNegLog="+maxNegLog);
        
        if(maxPosLog == 0.0  || String.valueOf(maxPosLog).contains("Infinity")){
            maxPosLog = 0.01;
        }
        if(maxNegLog == 0.0  || String.valueOf(maxNegLog).contains("Infinity")) maxNegLog = 0.01;
        
        System.out.println("$maxPosLog="+maxPosLog);
        System.out.println("$maxNegLog="+maxNegLog);
        Collections.sort(allLogs);
        
        Double perc90 = 0.0;
        Double perc10 = 0.0;
        
        if(allLogs.size()>0){
            //Get percentiles 
            Double a = allLogs.size() * 0.95;
            if(a == 0.0) perc90 = 1.0;
            else perc90 = allLogs.get(a.intValue());

            Double b = allLogs.size() * 0.05;
            if(b == 0.0) perc10 = 0.01;
            perc10 = allLogs.get(b.intValue());
        }
        System.out.println("$Perc90"+perc90+"\tPerc10"+perc10);
        
        //Min and max widths for values that lie outside 75th and 25th perc.
        Double minWidth = 3.0;
        Double maxWidth = 12.0;
        
        
        Double FACTOR = 10.0;
        //Do edge weights
        for(CyEdge e: cyEdgesPos.keySet()){
            Double i = cyEdgesPos.get(e);
            
            Double frac;
            if(i > perc90) frac = maxWidth;
            else if(i < perc10) frac = minWidth;
            else frac = ((double) i/ (double) maxPosLog) * FACTOR; //Normalize w.r.t max pos
            if(frac==0.0||String.valueOf(frac).contains("Infinity")) frac = maxWidth-2;
            
            if(frac>maxWidth) frac = maxWidth;
            edgeAttrs.setAttribute(e.getIdentifier(), GIProAttributeNames.EDGE_WIDTH, frac);
        }
        for(CyEdge e: cyEdgesNeg.keySet()){
            Double i = cyEdgesNeg.get(e);
            
            Double frac;
            if (i> perc90) frac = maxWidth;
            else if (i<perc10) frac = minWidth;
            else frac = ((double) i/ (double) maxNegLog)* FACTOR; //Normalize w.r.t max pos
            
            //System.out.println("fracNeg="+frac+"\t"+i+"\t"+maxNegLog);
            if(frac==0.0||String.valueOf(frac).contains("Infinity")) frac = maxWidth-2;
            if(frac>maxWidth) frac = maxWidth;
            edgeAttrs.setAttribute(e.getIdentifier(), GIProAttributeNames.EDGE_WIDTH, frac);
            //System.out.println(e.getIdentifier() + "\t"+i+"\t"+frac);
        }
        
        System.out.println("$"+cyEdgesNeg.size()+"\t"+cyEdgesPos.size());
        
        RootNetworkSelectionListener rnsl = new RootNetworkSelectionListener
                (this, complexes, complexEdges, nodeInfoPane, edgeInfoPane, tree);
        mainNetwork.addSelectEventListener(rnsl);
        treeListener.registerCyNetwork(mainNetwork);
        
        //menu 
        ComplexNodeContextMenu menu = new ComplexNodeContextMenu(mainNetwork, 
                        (SideGuiActionListener) sgal);
                Cytoscape.getNetworkView(mainNetwork.getIdentifier())
                        .addNodeContextMenuListener(menu);

        ((SideGuiActionListener) sgal).setNetwork(mainNetwork);
        ctrlPanel.setSelectedIndex(previousIndex);

        //for ctrlDown
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(new KeyDispatcher());
        
        CyNetworkView view = Cytoscape.getNetworkView(mainNetwork.getIdentifier());
        Visualization.createVisualStyle();
        Visualization.refreshNetworkView(view, true, true, false);
        
        
        //Hack to show tree initially
        TreeNode root = (TreeNode)tree.getModel().getRoot();
        expandAll(tree, new TreePath(root), true);
}//end createRootGraph

    private void expandAll(JTree tree, TreePath parent, boolean expand) {
    // Traverse children
    TreeNode node = (TreeNode)parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e=node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode)e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }
    /**
     * Asymptotically Log Function: e^2 * ln(x) + e^pi - pi
     * @param x Double value
     * @return Return f(x)
     */
    private double asymptoticallyLogFunction(double x){
        return Math.E*Math.E*Math.log(x) + Math.exp(Math.PI) - Math.PI;
    }
    
    public void disablePanelButtons(){
        createSubNet.setEnabled(false);
        createGeneHeatMap.setEnabled(false);
        createComplexHeatMap.setEnabled(false);
        exportResults.setEnabled(false);
        createHistogram.setEnabled(false);
    }
    
    public void enablePanelButtons(){
        createSubNet.setEnabled(true);
        createGeneHeatMap.setEnabled(true);
        createComplexHeatMap.setEnabled(true);
        exportResults.setEnabled(true);
        createHistogram.setEnabled(true);
    }
    
    public void addMyKeyListeners(){
        final RootNetwork rn = this;
        CytoPanelImp ctrlPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
        
        //Key listener for custom subnetwork
        ((JComponent) ctrlPanel.getParent()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 1"), "doSubnetwork");
        ((JComponent) ctrlPanel.getParent()).getActionMap().put("doSubnetwork", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                new CustomCreate((SideGuiActionListener)sgal, rn,"subnetwork");
            }
        });
        
        //Key listener for custom complex heatmap
        ((JComponent) ctrlPanel.getParent()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 2"), "doComplexheatmap");
        ((JComponent) ctrlPanel.getParent()).getActionMap().put("doComplexheatmap", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                new CustomCreate((SideGuiActionListener)sgal, rn,"complex_heatmap");
            }
        });
        
        //Key listener for custom gene heatmap
        ((JComponent) ctrlPanel.getParent()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 3"), "doGeneheatmap");
        ((JComponent) ctrlPanel.getParent()).getActionMap().put("doGeneheatmap", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                new CustomCreate((SideGuiActionListener)sgal, rn,"gene_heatmap");
            }
        });
        
        //Key listener for sort jtree descending
        ((JComponent) ctrlPanel.getParent()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 4"), "sortTreeDesc");
        ((JComponent) ctrlPanel.getParent()).getActionMap().put("sortTreeDesc", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                sortTree(true, false);
            }
        });
        //Key listener for sort jtree ascending
        ((JComponent) ctrlPanel.getParent()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 5"), "sortTreeAsc");
        ((JComponent) ctrlPanel.getParent()).getActionMap().put("sortTreeAsc", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                sortTree(false, false);
            }
        });
        
        //Key listener for sort jtree ascending
        ((JComponent) ctrlPanel.getParent()).getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("control 6"), "sortTreeAlpha");
        ((JComponent) ctrlPanel.getParent()).getActionMap().put("sortTreeAlpha", new AbstractAction() {
            public void actionPerformed(ActionEvent ae) {
                sortTree(false, true);
            }
        });
    }
    
    public void sortTree(boolean desc, boolean alpha){
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration e = root.children();
        Map<MutableTreeNode, Object> nodeToSize = new HashMap();
        //If Sort alphabetically
        if(alpha){
            while(e.hasMoreElements()){
                MutableTreeNode node = (MutableTreeNode) e.nextElement();
                nodeToSize.put(node, node.toString());
            }
        //Else sort by num subunits    
        }else{
            while(e.hasMoreElements()){
                MutableTreeNode node = (MutableTreeNode) e.nextElement();
                nodeToSize.put(node, node.getChildCount());
            }
        }
        //Throw away old root, no longer need it
        root = null;
        
        DefaultMutableTreeNode newRoot = new DefaultMutableTreeNode(Config.ROOT_NODE_NAME);
        //Sort
        List<Map.Entry<MutableTreeNode, Object>> sortedList = sortByValue(nodeToSize);
        //If desending reverse list
        if(desc) Collections.reverse(sortedList);
        //populate new root
        for(Map.Entry<MutableTreeNode, Object> entry: sortedList){
            newRoot.add(entry.getKey());
        }
        DefaultTreeModel tm = new  DefaultTreeModel(newRoot);
        tree.setModel(tm);
    }
    
    private static List sortByValue(Map map) {
         List list = new LinkedList(map.entrySet());
         Collections.sort(list, new Comparator() {
              public int compare(Object o1, Object o2) {
                   return ((Comparable) ((Map.Entry) (o1)).getValue())
                  .compareTo(((Map.Entry) (o2)).getValue());
              }
         });

        return list;
    } 
    /**
     * Creates side-panel GUI
     * @param posCut Positive cutoff
     * @param negCut Negative cutoff
     * @param fdr False discovery rate
     */
    public void createGui(Double posCut, Double negCut, Double fdr){
        final CytoPanelImp ctrlPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);

        rootNode = new ComplexMutableTreeNode(Config.ROOT_NODE_NAME);
        tree = new JTree(rootNode);

        treeListener = new ComplexTreeListener(tree);
        tree.setExpandsSelectedPaths(true);

        mainSidebarPanel = new JPanel();
        mainSidebarPanel.setLayout(new BoxLayout(mainSidebarPanel, BoxLayout.Y_AXIS));
        
        //Actions panel empty , populated below
        actionsAndParamPanel = new JPanel();
        mainSidebarPanel.add(actionsAndParamPanel);
        
        //=======================InformationPanel====================================
        nodeInfoPane = new JEditorPane();
        nodeInfoPane.setEditable(false);
        nodeInfoPane.setContentType("text/html");
        nodeInfoPane.setText("<html><font color=gray size=5 face=arial><center><i>No nodes selected</i></center></font></html>");

        edgeInfoPane = new JEditorPane();
        edgeInfoPane.setEditable(false);
        edgeInfoPane.setContentType("text/html");
        edgeInfoPane.setText("<html><font color=gray size=5 face=arial><center><i>No edges selected</i></center></font></html>");
        

        nodeInfoScroll = new JScrollPane(nodeInfoPane);
        nodeInfoScroll.setPreferredSize(new Dimension(300, 200));

        edgeInfoScroll = new JScrollPane(edgeInfoPane);
        edgeInfoScroll.setPreferredSize(new Dimension(300, 200));

        JTabbedPane infoPane = new JTabbedPane();
        infoPane.addTab("Node", nodeInfoScroll);
        infoPane.addTab("Edge", edgeInfoScroll);

        infoPane.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.GRAY), "Information"));

        mainSidebarPanel.add(infoPane);
        mainSidebarPanel.add(Box.createRigidArea(new Dimension(0,10)));

        //=============================Tree============================================
        tree.addTreeSelectionListener(treeListener);
        tree.setRootVisible(false);
        
        JScrollPane treeScroll = new JScrollPane(tree);
        //treeScroll.setBorder(BorderFactory.createLineBorder(Color.black));

        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BoxLayout(treePanel, BoxLayout.PAGE_AXIS));
        treePanel.add(treeScroll);
        treePanel.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.GRAY), "Complex/Gene Tree"));

        treeScroll.setPreferredSize(new Dimension(300, 300));
        mainSidebarPanel.add(treePanel);

        //===============================Buttons=======================================
        sgal = new SideGuiActionListener(complexes, genes, complexEdges, nodeInfoPane,
                edgeInfoPane, tree, treeListener, betweenPVal, mwpds, this);
        
        URL iconURL;
        ImageIcon icon;
        
        iconURL = getClass().getClassLoader().getResource("images/subnetwork.png");
        icon = new ImageIcon(iconURL);
        
        createSubNet = new JButton("Expand complex(es)");
        createSubNet.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Expand complex(es)</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates a detailed view of the underlying genes for selected complexes and their physical/genetic interactions</span>"
                + "<br><br>"
                + "<b>Note:</b> to enter/upload complex names customly press <b>Ctrl + 1</b></html>");
        createSubNet.setIcon(icon);
        createSubNet.setHorizontalAlignment(SwingConstants.LEFT);
        createSubNet.addActionListener(sgal);
        createSubNet.setActionCommand("create_subnet");
        
        iconURL = getClass().getClassLoader().getResource("images/heatmap_small.png");
        ImageIcon heatmapIcon = new ImageIcon(iconURL);
        
        createGeneHeatMap = new JButton("Gene heatmap");
        createGeneHeatMap.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Create gene heatmap</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates a heatmap of genetic interactions for the genes in two selected complexes</span>"
                + "<br><br>"
                + "<b>Note:</b> to enter/upload complex names customly press <b>Ctrl + 2</b></html>");
        createGeneHeatMap.setIcon(heatmapIcon);
        createGeneHeatMap.setHorizontalAlignment(SwingConstants.LEFT);
        createGeneHeatMap.addActionListener(sgal);
        createGeneHeatMap.setActionCommand("create_gene_heatmap");

        createComplexHeatMap = new JButton("Complex heatmap");
        createComplexHeatMap.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Create complex heatmap</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates heatmap of the average interaction score between selected complexes</span>"
                + "<br><br>"
                + "<b>Note:</b> to enter/upload complex names customly press <b>Ctrl + 3</b></html>");
        createComplexHeatMap.setIcon(heatmapIcon);
        createComplexHeatMap.setHorizontalAlignment(SwingConstants.LEFT);
        createComplexHeatMap.addActionListener(sgal);
        createComplexHeatMap.setActionCommand("create_complex_heatmap");

        iconURL = getClass().getClassLoader().getResource("images/histogram.png");
        icon = new ImageIcon(iconURL);
        
        createHistogram = new JButton("Histogram");
        createHistogram.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Create histogram</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates histogram of within and between raw pairwise scores</span></html>");
        createHistogram.setIcon(icon);
        createHistogram.setHorizontalAlignment(SwingConstants.LEFT);
        createHistogram.addActionListener(sgal);
        createHistogram.setActionCommand("histogram");
        createHistogram.setFont(boldFont);
        
        iconURL = getClass().getClassLoader().getResource("images/export.png");
        icon = new ImageIcon(iconURL);
        
        exportResults = new JButton("Export results");
        exportResults.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Export results</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Export enrichment and complex data to external tab-delimited files</span></html>");
        exportResults.setIcon(icon);
        exportResults.setHorizontalAlignment(SwingConstants.LEFT);
        exportResults.addActionListener(sgal);
        exportResults.setActionCommand("output_dialog");
        exportResults.setFont(boldFont);
        
        
        //subnetwork buttons
        subnetRawHeatmap = new JButton("Display raw GI data");
        subnetRawHeatmap.setIcon(heatmapIcon);
        subnetRawHeatmap.setHorizontalAlignment(SwingConstants.LEFT);
        subnetRawHeatmap.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                ((SideGuiActionListener) sgal).makeHeatMap("query");
            }
        });
        subnetRawHeatmap.setToolTipText
                ("<html><u><strong><span style=\"font-size: 9px;\">Display raw interaction data</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Generates a heat-map of raw genetic interactions for selected genes</span></html>");
        
        
        subnetPatterns = new JButton("Display sign patterns");
        subnetPatterns.setIcon(heatmapIcon);
        subnetPatterns.setHorizontalAlignment(SwingConstants.LEFT);
        subnetPatterns.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if(ctrlDown){
                        System.out.println("ctrl is down... custom now..");
                        new CustomPatterns((SideGuiActionListener)sgal);
                        return;
                }else{
                    ((SideGuiActionListener) sgal).makeHeatMap("query-patterns");
                }
            }
        });
        subnetPatterns.setToolTipText
                ("<html><u><strong><span style=\"font-size: 9px;\">Display sign patterns</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Generates a heat-map of genetic interactions for 2 or more selected genes "
                + "featuring same or alternating signs</span>"
                + "<br><br>"
                + "<b>Note:</b> to enter cutoffs for positive/negative interactions, hold down <b>Ctrl</b> while clicking</html>"
                + "</html>");
        
        
        subnetCorrTables = new JButton("GI correlation tables");
        iconURL = getClass().getClassLoader().getResource("images/table_icon.png");
        icon = new ImageIcon(iconURL);
        subnetCorrTables.setIcon(icon);
        subnetCorrTables.setHorizontalAlignment(SwingConstants.LEFT);
        subnetCorrTables.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                ((SideGuiActionListener) sgal).createPearsonTables();
            }
        });
        subnetCorrTables.setToolTipText
                ("<html><u><strong><span style=\"font-size: 9px;\">Create pearson tables</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates a table of pairwise pearson correlations and a corresponding table with p-values for the correlations"
                + ", for selected genes</span>"
                + "</html>");
        
        subnetCorrEdges = new JButton("Add correlation edges");
        iconURL = getClass().getClassLoader().getResource("images/edge_icon.png");
        icon = new ImageIcon(iconURL);
        subnetCorrEdges.setIcon(icon);
        subnetCorrEdges.setHorizontalAlignment(SwingConstants.LEFT);
        subnetCorrEdges.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                ((SideGuiActionListener) sgal).addCorrelationEdges();
            }
        });
        subnetCorrEdges.setToolTipText
                ("<html><u><strong><span style=\"font-size: 9px;\">Add correlation edges</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Adds dashed pearson correlation edges for pairwise genes where applicable</span>"
                + "</html>");
        
        //************POPULATE ACTION STUFF*******************
        populateComplexActionsAndParams();
        //****************************************************
        
        //params
        DecimalFormat format = new DecimalFormat("#.#####");

        // =============================Gene Search==================================
        
        
        final JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));	
        
        GeneSearchBox gsb = new GeneSearchBox(searchPanel, genes, geneToOrf, tree);
        JSuggestField jsf = gsb.getSuggestField();
        jsf.setMaximumSize(new Dimension(600, 50));
        
        //searchPanel.add(gcb);
        searchPanel.add(jsf);
        searchPanel.add(gsb.getZoomCheckbox());
        
        searchPanel.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.GRAY), "Gene Search"));
        
        mainSidebarPanel.add(Box.createRigidArea(new Dimension(0,10)));
        mainSidebarPanel.add(searchPanel);		
        
        
        // =============================Params and user manual=========================
        JPanel footerPanel = new JPanel();
        
        JButton showParams = new JButton("Adjust Parameters");
        showParams.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                RebuildMWPDSTask task = new RebuildMWPDSTask();
                // Configure JTask Dialog Pop-Up Box
                JTaskConfig jTaskConfig = new JTaskConfig();
                //jTaskConfig.setOwner(Cytoscape.getDesktop());
                jTaskConfig.displayCloseButton(false);

                jTaskConfig.displayCancelButton(false);
                jTaskConfig.setAutoDispose(true);
                TaskManager.executeTask(task, jTaskConfig);
                
                upp.setVisible(false);
                upp.setModal(true);
                upp.setVisible(true);
            }
        });
        
        JButton userManual = new JButton("User Manual");
        userManual.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                UserManualPanel ump = new UserManualPanel(Cytoscape.getDesktop(), true, null);
                ump.setVisible(true);
            }
        });
        
        footerPanel.add(showParams);
        footerPanel.add(userManual);
        
        JButton exitGIPro = new JButton("Close GIPro");
        exitGIPro.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                int n = JOptionPane.showConfirmDialog(null,
                        "Are you sure you want to close GIPro? All networks will be destroyed", "Message"
                        ,JOptionPane.YES_NO_OPTION);
                if(n == JOptionPane.NO_OPTION) return;
                int index = ctrlPanel.indexOfComponent("GIPro");
                if(index != -1){
                    ctrlPanel.setSelectedIndex(0);
                    ctrlPanel.remove(ctrlPanel.getComponentAt(index));
                }
                Cytoscape.createNewSession();
                
            }
        });
        footerPanel.add(exitGIPro);
        footerPanel.add(Box.createHorizontalStrut(5));
        
        ToolTipManager.sharedInstance().setDismissDelay((int)TimeUnit.MINUTES.toMillis(5));//5 mins
        ToolTipManager.sharedInstance().setInitialDelay(10);//10 millisecs
        //USER MANUAL STUFF
        //footerPanel.add(new UserManualLabel()); 
        
        mainSidebarPanel.add(footerPanel);
        
        //Add the key listener for custom create
        addMyKeyListeners();
        
        int index = ctrlPanel.indexOfComponent("GIPro");
        if(index == -1){
            ctrlPanel.add("GIPro", mainSidebarPanel);
        }
        index = ctrlPanel.indexOfComponent("GIPro");
        ctrlPanel.setSelectedIndex(index);

    }
    
    public void populateComplexActionsAndParams(){
        actionsAndParamPanel.removeAll();
        actionsAndParamPanel.setLayout(new GridLayout(1,1));
        
        JPanel subAction = new JPanel();
        GridLayout glhf = new GridLayout(5, 1);
        subAction.setLayout(glhf);
        actionsAndParamPanel.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.gray), "Complex network actions"));
        //this order works well
        subAction.add(createSubNet);
        subAction.add(createGeneHeatMap);
        subAction.add(createComplexHeatMap);
        //subAction.add(createHistogram); Histogram functionality removed!
        subAction.add(exportResults);
        subAction.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        
        actionsAndParamPanel.add(subAction);
        actionsAndParamPanel.validate();
        actionsAndParamPanel.repaint();
    }
    
    public void populateSubnetworkActionsAndParams(){
        actionsAndParamPanel.removeAll();
        actionsAndParamPanel.setLayout(new GridLayout(1,1));
        
        JPanel subAction = new JPanel();
        GridLayout glhf = new GridLayout(5, 1);
        subAction.setLayout(glhf);
        actionsAndParamPanel.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.gray), "Gene network actions"));
        //this order works well
        subAction.add(subnetRawHeatmap);
        subAction.add(subnetPatterns);
        //subAction.add(subnetCorrTables); Correlation tables removed
        //subAction.add(subnetCorrEdges); Correlation edges removed
        subAction.add(new JLabel(""));
        subAction.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));
        
        actionsAndParamPanel.add(subAction);
        actionsAndParamPanel.validate();
        actionsAndParamPanel.repaint();
        
    }

    
    public void setComplexNetworkButtonsBold(){
        int numSelected = Cytoscape.getCurrentNetwork().getSelectedNodes().size();
        if(numSelected >= 1)createSubNet.setFont(boldFont);
        else createSubNet.setFont(plainFont);
        
        if(numSelected >=2) createComplexHeatMap.setFont(boldFont);
        else createComplexHeatMap.setFont(plainFont);
        
        if(numSelected >=1) createGeneHeatMap.setFont(boldFont);
        else createGeneHeatMap.setFont(plainFont);
        
    }
    
    public static void setSubnetworkNetworkButtonsBold(){
        int numSelected = Cytoscape.getCurrentNetwork().getSelectedNodes().size();
        if(numSelected >= 1)subnetRawHeatmap.setFont(boldFont);
        else subnetRawHeatmap.setFont(plainFont);
        
        if(numSelected >=2) {
            subnetPatterns.setFont(boldFont);
            subnetCorrTables.setFont(boldFont);
            subnetCorrEdges.setFont(boldFont);
        }
        else{
            subnetPatterns.setFont(plainFont);
            subnetCorrTables.setFont(plainFont);
            subnetCorrEdges.setFont(plainFont);
        }
        
        
    }
    /**
     * Creates side-panel GUI and root graph (i.e. runs createGui() and 
     * createRootGraph()
     * @param posCut Positive cutoff
     * @param negCut Negative cutoff
     * @param fdr False discovery rate
     */
    public void initialize(double posCut, double negCut, double fdr) {
        createGui(posCut, negCut, fdr);
        createRootGraph();
    }

    public void scrollNodeInfoToTop(){
        SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                            JScrollBar verticalScrollBar = nodeInfoScroll.getVerticalScrollBar();
                            verticalScrollBar.setValue(verticalScrollBar.getMinimum());
                    }
            });
    }

    public void scrollEdgeInfoToTop(){
        SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                            JScrollBar verticalScrollBar = edgeInfoScroll.getVerticalScrollBar();
                            verticalScrollBar.setValue(verticalScrollBar.getMinimum());
                    }
            });
    }

    /*************************
     *Private functions below*
     *************************
     */

    /**
     * Using the FDR and a list of p-value cutoffs, determine the p-value 
     * cutoff that is appropriate to use
     * @param pVals Vector of P-values
     * @param fdr False discovery rate
     * @return P-value cutoff
     */
     private double generateFDRPValue(List<Double> pVals, double fdr){
        Collections.sort(pVals);
        double pval = 0.0;
        int pSize = pVals.size();
        for (int index = 1; index < pSize+1; index ++){  // note the index start with 1
            pval = pVals.get(index-1); // note it is index-1, instead of index
            if (pval > (index*fdr)/pSize){ // The problem was caused by index == 0, the loop breaks whenever pval > 0
                break;
            }
        }
        return pval;
    }

    private double getFisherExactTestPVal(int num11, int num12, int num21, int num22){
        if (fisherExact == null){
            fisherExact = new FisherExact(total);
        }
        if ((num11 +num12 + num21 + num22) > fisherExact.getMax()){
            fisherExact = new FisherExact(num11 +num12 + num21 + num22);
        }
        return fisherExact.getRightTailedP(num11,num12, num21, num22);
    }

    private void getDistribution(int[] posTrials, int[] negTrials, 
            int numberOfEdges, int numberOfTrials, List<Double> listOfRelations,
            double posCutoff, double negCutoff){
        for(int trialNumber = 0; trialNumber < numberOfTrials; trialNumber++){
            int[] posNeg = populateRandomChoiceArray
                    (numberOfEdges, listOfRelations, posCutoff, negCutoff);

            int numberPositive = posNeg[0];
            int numberNegative = posNeg[1];

            posTrials[trialNumber] = numberPositive;
            negTrials[trialNumber] = numberNegative;					
        }
    }

    /**
     * Does the actual random drawing and counting number of positive and negative
     * @param numEdges
     * @param listOfRelations
     * @param posCutoff
     * @param negCutoff
     * @return 
     */
    private int[] populateRandomChoiceArray(int numEdges, 
        List<Double> listOfRelations, double posCutoff, double negCutoff){
        int size = listOfRelations.size();
        int[] toReturn = new int[2];//0 is positive, 1 is negative

        HashSet<Integer> alreadyAdded = new HashSet<Integer>();

        for(int tryNumber = 0; tryNumber < numEdges; tryNumber++){
            // choose a random score out of the available scores
            int indexToAdd = (int)(Math.random()*size);
            // choose a random score
            double score = listOfRelations.get(indexToAdd);

            // don't add a score more than once
            if(!alreadyAdded.contains(indexToAdd)){
                alreadyAdded.add(indexToAdd);
                if(score > posCutoff){
                    toReturn[0] += 1;
                }
                else if(score < negCutoff){
                    toReturn[1] += 1;						
                }
            }
            // try again to get a non repeated number
            else{
                tryNumber--;
            }
        }
        return toReturn;
    }

    /**
     * Computes the empirical p-value and returns
     * @param trials
     * @param numberToCompare
     * @param numberOfTrials
     * @return Emperical p-value
     */
    private double getPVal(int[] trials, int numberToCompare, int numberOfTrials){
        double count = 0;
        for(int trialNum : trials){
            //Count keeps track of the number of random trials that are 
            //greater than/equal to the ACTUAL value -> to calculate p-value
            if(trialNum >= numberToCompare){	
                count++;
            }
        }
        double pvalue = count/numberOfTrials;		
        return pvalue;
    }

    /*****************
     *GET/SET METHODS*
     *****************
     */

    public MyWizardPanelDataStorage getMyWizardPanelDataStorage(){
        return mwpds;
    }

    public Map<String, Set<Complex>> getNetworkComplexMap(){
        return networkComplexMap;
    }

    public void setNetworkComplexMap(Map<String, Set<Complex>> map){
        networkComplexMap = map;
    }

    /**
     * Sets the total number of genetic interactions
     * @param num Total number of GIs
     */
    public void setTotalInteractions(int num){
        total = num;
    }
    
    public void setTotalPos(int num){
        totalPos = num;
    }
    public void setTotalNeg(int num){
        totalNeg = num;
    }
    public void setRootNetworksMWPDS(MyWizardPanelDataStorage mwpds){
        this.mwpds  = mwpds;
    }

    /**
     * @return Container containing information on the nodes (in side-panel)
     */
    public JEditorPane getNodeInfoPane(){
        return nodeInfoPane;
    }

    /**
     * @return Container containing information on the edges (in side-panel)
     */
    public JEditorPane getEdgeInfoPane(){
        return edgeInfoPane;
    }

    /**
     * @return Current JTree used in the side-panel
     */
    public JTree getTree(){
        return tree;
    }

    /**
     * @return Within p-value 
     */
    public double getWithinPValue (){
        return withinPVal;
    }

    /**
     * @return Between p-value
     */
    public double getBetweenPValue (){
        return betweenPVal;
    }

    /**
     * @return Total number of positive interactions
     */
    public int getTotalPos(){
        return totalPos;
    }

    /**
     * @return Total number of negative interactions
     */
    public int getTotalNeg(){
        return totalNeg;
    }

    /**
     * @return Total number of interactions
     */
    public int getTotal(){
        return total;
    }

    /**
     * @return Map of complex names to Complex object
     */
    public Map<String, Complex> getComplexes(){
        return complexes;
    }

    /**
     * @return Map of gene names to Gene object
     */
    public Map<String, Gene> getGenes(){
        return genes;
    }

    /**
     * @return Map of ComplexEdge names to ComplexEdge object
     */
    public Map<String, ComplexEdge> getComplexEdge(){
        return complexEdges;
    }

    /**
     * @return Map of genes to orf
     */
    public Map<String,String> getGeneToOrf(){
        return geneToOrf;
    }

    /**
     * @return current JTree listener 
     */
    public ComplexTreeListener getTreeListener(){
        return treeListener;
    }

    /**
     * @return Root node (ComplexMutableTreeNode) of current JTree
     */
    public ComplexMutableTreeNode getRootNode(){
        return rootNode;
    }

    /**
     * @return Positive cutoff 
     */
    public Double getPosCutoff(){
        return posCutoff;
    }

    /**
     * @return Negative cutoff 
     */
    public Double getNegCutoff(){
        return negCutoff;
    }

    /**
     * @return False discovery rate 
     */
    public Double getFDR(){
            return fdr;
    }
    
    public RootNetwork getRootNetwork(){
        return this;
    }
    
    public List<Double> getValuesWithin(){
        return valuesWithin;
    }
    public List<Double> getValuesBetween(){
        return valuesBetween;
    }
    
    
    //For ctrl down
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
    
    
    public class RebuildMWPDSTask implements Task{
        TaskMonitor tm;
        @Override
        public void run() {
            upp = new UpdateParamsPanel(RootNetwork.this.rebuildMwpds(), RootNetwork.this,
                    usePvalCutoffs, pvalLeftTail, pvalRightTail,
                    usePercentileCutoffs, leftTailPercentile, rightTailPercentile, 
                    posCutoff, negCutoff,
                    RootNetwork.this.getFDR(), 
                    useFisher,useSimulations, numberOfTrials, trialForEachComplex);
        }

        @Override
        public void halt() {
            //Do nothing
        }

        @Override
        public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
            this.tm = tm;
        }

        @Override
        public String getTitle() {
            return "Please wait...";
        }
    
    }
    
}

@SuppressWarnings("serial")
class ComplexMutableTreeNode extends DefaultMutableTreeNode{
    public ComplexMutableTreeNode(String name) {
        super(name);
    }
    @SuppressWarnings("unchecked")
	@Override
    public void insert(final MutableTreeNode newChild, final int childIndex) {
        super.insert(newChild, childIndex);
        Collections.sort(this.children, new Comparator<DefaultMutableTreeNode>() {
			public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
				String thisS = o1.toString();
		    	String compareS = o2.toString();
		    	try{
		    		return Integer.parseInt(thisS) - Integer.parseInt(compareS);
		    	} 
		    	catch(NumberFormatException e){
		    		return 0;
		    	}
			}
		});
    }
}

@SuppressWarnings("serial")
class GeneMutableTreeNode extends DefaultMutableTreeNode{
    public GeneMutableTreeNode(Object o){
        super(o);
    }
    @SuppressWarnings("unchecked")
    @Override
    public void insert(final MutableTreeNode newChild, final int childIndex) {
        super.insert(newChild, childIndex);
        Collections.sort(this.children, new Comparator<DefaultMutableTreeNode>() {
			public int compare(DefaultMutableTreeNode o1, DefaultMutableTreeNode o2) {
				String thisS = o1.toString();
		    	String compareS = o2.toString();
		    	return thisS.compareTo(compareS);
			}
		});
    }
    
}



