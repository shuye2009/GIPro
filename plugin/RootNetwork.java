package plugin;

import guiICTools.MyWizardPanelDataStorage;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeSelectionModel;


import cytoscape.CyEdge;
import cytoscape.CyNetwork;
import cytoscape.CyNode;
import cytoscape.Cytoscape;
import cytoscape.data.CyAttributes;
import cytoscape.view.CyNetworkView;
import cytoscape.view.cytopanels.CytoPanelImp;
import edu.stanford.genetics.treeview.BrowserControl;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.MouseListener;

import java.net.URL;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author YH
 * 
 * Contains all information regarding complexes, genes and complex edges etc.
 * Also contains functions for feeding in data.
 */

public class RootNetwork{
	
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
    private static double withinPVal, betweenPVal, posCutoff, negCutoff, fdr;
    public Map<String, Set<Complex>> networkComplexMap;

    private FisherExact fisherExact;
    // Keep track of the total number of positive or negative interactions within or between complexes.
    private int totalNeg, totalPos, total;
    
    //For side panel
    private JButton createSubNet, createGeneHeatMap, createComplexHeatMap, createHistogram, dumpInformation;

    ArrayList<Double> valuesWithin, valuesBetween;
    private MyWizardPanelDataStorage mwpds;
    
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
            ArrayList<Double> valuesWithin, ArrayList<Double> valuesBetween){

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

        createGui(posCutoff, negCutoff, fdr);	
        System.err.println("Restored!");
    }
    
    public void clearAll(){
        
        //Try to clear datasturctures if not null
        Map[] mapsToClear = {complexes, genes, complexEdges, geneToOrf, networkComplexMap};
        for(Map m: mapsToClear){
            try{
                m.clear();
            }catch(Exception e){
                //skip
            }
        }
        ArrayList[] arraysToClear = {valuesWithin, valuesBetween};
        for(ArrayList a: arraysToClear){
            try{
                a.clear();
            }catch(Exception e){
                //skip
            }
        }
        
    }
    
    /*FOR HISTOGRAM SAVE*/
    public void setValuesWithinAndBetween(ArrayList<Double> within, ArrayList<Double> between){
        valuesWithin = within;
        valuesBetween = between;
    }
    
    /**
     * Creates histogram of within/between genetic interaction scores
     * using jfreechart
     */
    public void createHistogram() {
        System.out.println(valuesWithin);
        String sNumBins = JOptionPane.showInputDialog(Cytoscape.getDesktop(), "Enter the number of BINs");
        if (sNumBins != null){
            int numberBins = Integer.parseInt(sNumBins);
            HistogramGui.showHistogram(valuesWithin.toArray(new Double[0]),
                    valuesBetween.toArray(new Double[0]), numberBins);
        }
    }


    public void insertComplexGenePair(String complexName, String geneIdentifier, 
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
                    if (! gene1.getGeneName().equals(gene2.getGeneName()) && mwpds.hasGeneticInteraction(gene1, gene2)){
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
                        
                        int before = 0;
                        int after = 0;
                        // Find the number of actual pairs
                        for (Gene gene1 : c1.getActualGenes()){
                            for (Gene gene2 : c2.getActualGenes()){
                                if(mwpds.hasGeneticInteraction(gene1, gene2)){
                                    before++;
                                }
                                
                                if (mwpds.hasGeneticInteraction(gene1, gene2)
                                        & !sharedGenes.contains(gene1) 
                                        & !sharedGenes.contains(gene2) ){
                                    actualPairs ++;
                                    after++;
                                }
                            }
                        }
                        System.out.println("#BEFORE ACTUAL = "+before);
                        System.out.println("#AFTER ACTUAL = "+after);
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
            totalPos++;
        }
        else if (score < negativeCutoff){
            totalNeg++;
        }
        
        if(g1 == null || g2 == null)
            return;
        if(g1.equals(g2))
            return;
        
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
        System.out.println("Performing within complex trials");
        total = mwpds.getTotalNumGenes();
        int nonPositive = total - totalPos;
        int nonNegative = total - totalNeg;

        Vector<Double> pVals = new Vector<Double>();

        for(Complex c : complexes.values()){
            int numberOfEdgesInCpx = c.getNumInteractions();
            int posRelationsInCpx = c.posRelations();
            int negRelationsInCpx = c.negRelations();
            
            int nonPosRelations = numberOfEdgesInCpx - posRelationsInCpx;
            int nonNegRelations = numberOfEdgesInCpx - negRelationsInCpx;

            if (posRelationsInCpx > 0){
                double posPVal = getFisherExactTestPVal
                        (posRelationsInCpx, totalPos - posRelationsInCpx, 
                        nonPosRelations, nonPositive - nonPosRelations);
                pVals.add(posPVal);
                c.setPosPValue(posPVal);
            }
            if (negRelationsInCpx > 0){
                double negPVal = getFisherExactTestPVal
                        (negRelationsInCpx, totalNeg - negRelationsInCpx, 
                        nonNegRelations, nonNegative - nonNegRelations);
                pVals.add(negPVal);
                c.setNegPValue(negPVal);
            }
        }

        withinPVal = generateFDRPValue(pVals, fdr);
        System.out.println("within " +withinPVal);
        for (Complex c : complexes.values()){
            c.setSignificance(withinPVal);
        }
        System.out.println("Done performing within complex trials");
    }//end withinComplexTrials

    /**
     * Performs complex fisher trials on between genetic interactions
     * @param posCutoff Positive cutoff
     * @param negCutoff Negative cutoff
     * @param fdr False discovery rate
     */
    public void betweenComplexFisherTrials(double posCutoff, double negCutoff, double fdr){
        Set<String> insignificantKeys = new HashSet<String>();
        Vector<Double> pVals = new Vector<Double>();
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
            
            if (posRelationsInEdge > 0){
                double posPVal = getFisherExactTestPVal
                        (posRelationsInEdge, totalPos - posRelationsInEdge,
                        nonPosRelations, totalNonPos - nonPosRelations);
                pVals.add(posPVal);
                ce.setPosPValue(posPVal);
            }
            if (negRelationsInEdge > 0){
                double negPVal = getFisherExactTestPVal
                        (negRelationsInEdge, totalNeg - negRelationsInEdge, 
                        nonNegRelations, totalNonNeg - nonNegRelations);
                pVals.add(negPVal);
                ce.setNegPValue(negPVal);
            }
        }

        betweenPVal = generateFDRPValue(pVals, fdr);
        System.out.println("between " + betweenPVal);

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

        Vector<Double> pVals = new Vector<Double>();

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
            Vector<Double> pVals = new Vector<Double>();

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
            String type = networkAtr.getAttribute(network.getIdentifier(), "ICTools_NetworkType").toString();
            
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

                Visualization.createVisualStyle();
                Visualization.refreshNetworkView(Cytoscape.createNetworkView(network), true, false);
                
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
    }

    /**
     * This method actually creates the graph node/edge visualizations
     */
    public void createRootGraph() {
        CytoPanelImp ctrlPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
        int previousIndex = ctrlPanel.getSelectedIndex();

        //create the complex nodes
        CyAttributes nodeAttrs = Cytoscape.getNodeAttributes();
        CyAttributes edgeAttrs = Cytoscape.getEdgeAttributes();
        
        CyNetwork mainNetwork = Cytoscape.createNetwork("Complex Network", true);
        
        //Add attribute to network to distinguish gene networks from complex networks
        CyAttributes networkAtr = Cytoscape.getNetworkAttributes();
        networkAtr.setAttribute(mainNetwork.getIdentifier(), "ICTools_NetworkType", "Complex");
        
        //FOCUS CHANGES IN NETWORK + OTHER STUFF
        CytoscapeEventListener nsl = new CytoscapeEventListener(this);

        for(String c : complexes.keySet()){
            CyNode n = Cytoscape.getCyNode(c, true);
            mainNetwork.addNode(n);
            Complex cc = complexes.get(c);
            String significance = cc.getSignificance();
            nodeAttrs.setAttribute(n.getIdentifier(), SpecialAttributeNames.NODE_TYPE, significance);
            nodeAttrs.setAttribute(n.getIdentifier(), SpecialAttributeNames.DISPLAY_NAME, n.getIdentifier());
            nodeAttrs.setAttribute(n.getIdentifier(), SpecialAttributeNames.IS_SUBNET, false);

            int numGenes = cc.getGenes().size();
            double nodeSize = asymptoticallyLogFunction(numGenes);
            nodeAttrs.setAttribute(n.getIdentifier(), SpecialAttributeNames.NODE_SIZE, nodeSize);

            GeneMutableTreeNode complexTreeNode = new GeneMutableTreeNode(cc);
            for(Gene g : cc.getGenes()){
                DefaultMutableTreeNode geneNode = new DefaultMutableTreeNode(g);
                complexTreeNode.add(geneNode);
            }
            rootNode.add(complexTreeNode);
        }

        //connect the intercomplex edges
        for(ComplexEdge ce : complexEdges.values()){
            String sourceId = ce.getSource().getName();
            String targetId = ce.getTarget().getName();
            String significance = ce.getSignificance();
            double pearson = ce.averagePearsonCorrelation();
            if (significance == Config.edgeBothSignificanceKey){
                    CyEdge ePos = Cytoscape.getCyEdge(sourceId, sourceId + "//" + targetId, targetId, "cc");
                    CyEdge eNeg = Cytoscape.getCyEdge(targetId, sourceId + "//" + targetId, sourceId, "cc");

                    mainNetwork.addEdge(ePos);
                    mainNetwork.addEdge(eNeg);
                    edgeAttrs.setAttribute(ePos.getIdentifier(), SpecialAttributeNames.EDGE_TYPE, Config.edgePosSignificanceKey);
                    edgeAttrs.setAttribute(eNeg.getIdentifier(), SpecialAttributeNames.EDGE_TYPE, Config.edgeNegSignificanceKey);

                    edgeAttrs.setAttribute(ePos.getIdentifier(), SpecialAttributeNames.WEIGHT, pearson);
                    edgeAttrs.setAttribute(eNeg.getIdentifier(), SpecialAttributeNames.WEIGHT, pearson);
            }
            else{
                    CyEdge e = Cytoscape.getCyEdge(sourceId, sourceId + "//" + targetId, targetId, "cc");
                    mainNetwork.addEdge(e);
                    edgeAttrs.setAttribute(e.getIdentifier(), SpecialAttributeNames.EDGE_TYPE, significance);
                    edgeAttrs.setAttribute(e.getIdentifier(), SpecialAttributeNames.WEIGHT, pearson);
            }
        }

        RootNetworkSelectionListener rnsl = new RootNetworkSelectionListener
                (this, complexes, complexEdges, nodeInfoPane, edgeInfoPane, tree);
        mainNetwork.addSelectEventListener(rnsl);
        treeListener.registerCyNetwork(mainNetwork);

        ((SideGuiActionListener) sgal).setNetwork(mainNetwork);
        ctrlPanel.setSelectedIndex(previousIndex);

        CyNetworkView view = Cytoscape.getNetworkView(mainNetwork.getIdentifier());
        Visualization.createVisualStyle();
        Visualization.refreshNetworkView(view, true, true);

}//end createRootGraph

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
    }
    
    public void enablePanelButtons(){
        createSubNet.setEnabled(true);
        createGeneHeatMap.setEnabled(true);
        createComplexHeatMap.setEnabled(true);
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
    public void createGui(double posCut, double negCut, double fdr){
        final CytoPanelImp ctrlPanel = (CytoPanelImp) Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);

        rootNode = new ComplexMutableTreeNode(Config.ROOT_NODE_NAME);
        tree = new JTree(rootNode);

        treeListener = new ComplexTreeListener(tree);
        tree.setExpandsSelectedPaths(true);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // =============================Gene Search==================================
        GeneComboBox gcb = new GeneComboBox(genes, geneToOrf, tree);
        gcb.setMaximumSize(new Dimension(600, 50));
        final JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));	
        gcb.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));
        searchPanel.add(gcb);
        searchPanel.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.GRAY), "Gene Search"));

        mainPanel.add(searchPanel);		
        mainPanel.add(Box.createRigidArea(new Dimension(0,10)));

        //=======================InformationPanel====================================
        nodeInfoPane = new JEditorPane();
        nodeInfoPane.setEditable(false);
        nodeInfoPane.setContentType("text/html");

        edgeInfoPane = new JEditorPane();
        edgeInfoPane.setEditable(false);
        edgeInfoPane.setContentType("text/html");

        nodeInfoScroll = new JScrollPane(nodeInfoPane);
        nodeInfoScroll.setPreferredSize(new Dimension(300, 200));

        edgeInfoScroll = new JScrollPane(edgeInfoPane);
        edgeInfoScroll.setPreferredSize(new Dimension(300, 200));

        JTabbedPane infoPane = new JTabbedPane();
        infoPane.addTab("Node", nodeInfoScroll);
        infoPane.addTab("Edge", edgeInfoScroll);

        infoPane.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.GRAY), "Information"));

        mainPanel.add(infoPane);
        mainPanel.add(Box.createRigidArea(new Dimension(0,10)));

        //=============================Tree============================================
        tree.addTreeSelectionListener(treeListener);
        tree.setRootVisible(false);
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
        JScrollPane treeScroll = new JScrollPane(tree);
        treeScroll.setBorder(BorderFactory.createLineBorder(Color.black));

        JPanel treePanel = new JPanel();
        treePanel.setLayout(new BoxLayout(treePanel, BoxLayout.PAGE_AXIS));
        treePanel.add(treeScroll);
        treePanel.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.GRAY), "Complex/Gene Tree"));

        treeScroll.setPreferredSize(new Dimension(300, 300));
        mainPanel.add(treePanel);

        //===============================Buttons=======================================
        sgal = new SideGuiActionListener(complexes, genes, complexEdges, nodeInfoPane,
                edgeInfoPane, tree, treeListener, betweenPVal, mwpds, this);
        
        URL iconURL;
        ImageIcon icon;
        
        iconURL = getClass().getClassLoader().getResource("images/subnetwork.png");
        icon = new ImageIcon(iconURL);
        
        createSubNet = new JButton("Subnetwork");
        createSubNet.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Create Subnetwork</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates a subnetwork of the selected complexes showing their underlying genes and interactions</span>"
                + "<br><br>"
                + "<b>Note:</b> to enter/upload complex names customly press <b>Ctrl + 1</b></html>");
        createSubNet.setIcon(icon);
        createSubNet.setHorizontalAlignment(SwingConstants.LEFT);
        createSubNet.addActionListener(sgal);
        createSubNet.setActionCommand("create_subnet");
        
        iconURL = getClass().getClassLoader().getResource("images/heatmap_small.png");
        icon = new ImageIcon(iconURL);
        
        createGeneHeatMap = new JButton("Gene heatmap");
        createGeneHeatMap.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Create gene heatmap</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates a heatmap of the genes in two selected complexes</span>"
                + "<br><br>"
                + "<b>Note:</b> to enter/upload complex names customly press <b>Ctrl + 2</b></html>");
        createGeneHeatMap.setIcon(icon);
        createGeneHeatMap.setHorizontalAlignment(SwingConstants.LEFT);
        createGeneHeatMap.addActionListener(sgal);
        createGeneHeatMap.setActionCommand("create_gene_heatmap");

        createComplexHeatMap = new JButton("Complex heatmap");
        createComplexHeatMap.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Create complex heatmap</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Creates heatmap of the interactions between selected complexes</span>"
                + "<br><br>"
                + "<b>Note:</b> to enter/upload complex names customly press <b>Ctrl + 3</b></html>");
        createComplexHeatMap.setIcon(icon);
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
        
        iconURL = getClass().getClassLoader().getResource("images/export.png");
        icon = new ImageIcon(iconURL);
        
        dumpInformation = new JButton("Export results");
        dumpInformation.setToolTipText("<html><u><strong><span style=\"font-size: 9px;\">Export results</span></strong></u>"
                + "<br><span style=\"font-size: 9px;\">"
                + "Export enrichmnet and complex data to external tab-delimited files</span></html>");
        dumpInformation.setIcon(icon);
        dumpInformation.setHorizontalAlignment(SwingConstants.LEFT);
        dumpInformation.addActionListener(sgal);
        dumpInformation.setActionCommand("output_dialog");

        JPanel stuffOnBottom = new JPanel();
        //stuffOnBottom.setPreferredSize(new Dimension(300, 200));
        //stuffOnBottom.setMaximumSize(new Dimension(400, 250));
        GridLayout glhf = new GridLayout(5, 2);
        //glhf.setHgap(6);
        stuffOnBottom.setLayout(glhf);
        stuffOnBottom.setBorder(BorderFactory.createTitledBorder
                (BorderFactory.createLineBorder(Color.gray), "Actions and Parameters"));
        
        DecimalFormat format = new DecimalFormat("#.#####");

        JLabel posCutLabel = new JLabel("Positive cutoff: " + format.format(posCut));
        JLabel negCutLabel = new JLabel("Negative cutoff: " + format.format(negCut));
        JLabel pValueLabel = new JLabel("False Discovery Rate: " + fdr);
        JLabel withinPValLabel = new JLabel ("Within complex p-val: " + format.format(withinPVal));
        JLabel betweenPValLabel = new JLabel ("Between complex p-val: "+ format.format(betweenPVal));

        //this order works well
        stuffOnBottom.add(createSubNet);
        stuffOnBottom.add(posCutLabel);
        stuffOnBottom.add(createGeneHeatMap);
        stuffOnBottom.add(negCutLabel);
        stuffOnBottom.add(createComplexHeatMap);
        stuffOnBottom.add(pValueLabel);
        stuffOnBottom.add(createHistogram);
        stuffOnBottom.add(withinPValLabel);
        stuffOnBottom.add(dumpInformation);
        stuffOnBottom.add(betweenPValLabel);

        mainPanel.add(stuffOnBottom);
        
        //USER MANUAL STUFF
        mainPanel.add(new UserManualLabel()); 
        
        //Add the key listener for custom create
        addMyKeyListeners();
        
        int index = ctrlPanel.indexOfComponent("ICTools");
        if(index == -1){
            ctrlPanel.add("ICTools", mainPanel);
        }
        index = ctrlPanel.indexOfComponent("ICTools");
        ctrlPanel.setSelectedIndex(index);

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
    private double generateFDRPValue(Vector<Double> pVals, double fdr){
        Collections.sort(pVals);
        double pval = 0.05;
        for (int index = pVals.size(); index > 0; index --){
            pval = pVals.get(index-1); // use -1 to use appropriate vector indices
            if (pval < (index*fdr)/pVals.size()){
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
    
    public ArrayList<Double> getValuesWithin(){
        return valuesWithin;
    }
    public ArrayList<Double> getValuesBetween(){
        return valuesBetween;
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