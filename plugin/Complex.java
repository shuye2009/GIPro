package plugin;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * @author YH
 *
 * Instances of this class represent a complex and information contained within
 * The natural ordering of this class is by its name (you should not instantiate more than one instance with
 * the same name, that doesn't make sense)
 */
public class Complex implements Comparable<Complex>, Serializable{

    //Information regarding this complex including the genes, genes in relation list pvalues etc.
    private String complexName;
    private Set<Gene> genesWithin, genesInRelationList;
    private String significance;//p, n, b, 0
    private Double posPValue, negPValue;
	
    // Counts number of within complex interactions of each type
    private int pos, neg, zero,  actual;

    /**
     * Complex constructor
     * @param name Name of the complex 
     */
    public Complex(String name){
            genesWithin = new TreeSet<Gene>();
            genesInRelationList = new TreeSet<Gene>();
            complexName = name;
            pos = 0;
            neg = 0;
            zero = 0;
            actual = 0;
    }
	
    /**
     * Checks how many physical interactions exist between this complex and
     * a target complex
     * @param c2 Target complex
     * @return Number of gene pairs that are physically interacting between both complexes
     */
    public int numPhysInteractionsWith(Complex c2){
        int count = 0;
        Set<Gene> genesInComp2 = c2.getGenes();
        for (Gene g: genesWithin){
            for (Gene g2 : genesInComp2){
                if (g.physicallyConnectedTo(g2)){
                        count++;
                }
            }
        }
        return count;
    }
	
    /**
     * Unknown usage... 
     * @param c2
     * @return 
     */
    public int pearsonCorrelationWith(Complex c2){
        ArrayList<Gene> genes = new ArrayList<Gene>();
        genes.addAll(genesInRelationList);
        genes.addAll(c2.getActualGenes());
        int count = 0;
        Set<Gene> genesInComp2 = c2.getGenes();
        for (Gene g: genesWithin){
            for (Gene g2 : genesInComp2){
                if (g.physicallyConnectedTo(g2)){
                        count++;
                }
            }
        }
        return count;
    }
	
    /**
     * Proceses complex information and returns html string used in side-bar
     * @return String that summarizes information in this complex 
     * (to be printed in a GUI found at the side)
     */
    public String getInformation() {
        String listGenesWithin = "<br/>";
        Set<String> geneNames = new TreeSet<String>();
        for (Gene g : genesWithin){
                geneNames.add(g.getGeneName());
        }
        for (String name : geneNames){
                listGenesWithin += "--" + name + "<br/>";
        }
        listGenesWithin += "<br/>";

        geneNames.clear();
        String listActualGenes = "<br/>";

        for (Gene g : genesInRelationList){
                geneNames.add(g.getGeneName());
        }
        for (String name : geneNames){
                listActualGenes += "--" + name + "<br/>";
        }
        listActualGenes += "<br/>";
        DecimalFormat format = new DecimalFormat("#.########");
        
        String msg = 
            "<b>Complex: " + getName() + "</b><br><br>" +
            "<b>Genes</b> (" + genesWithin.size() + " in gene list):" + listGenesWithin +
            "<b>Actual Genes</b> (" + genesInRelationList.size() + " in relation list):" + listActualGenes + 
            "<b>" + pos + "</b> positive relations ";
            
            if (posPValue != null){
                msg += "(<b>p-value: " + format.format(posPValue.doubleValue()) + "</b>)";
            }
            
            msg += "<br><b>" + neg + "</b> negative relations ";
            
            if (negPValue != null){
                msg += "(<b>p-value: " + format.format(negPValue.doubleValue()) + "</b>)";
            }
            
            msg += "<br><b>" + zero + "</b> non-interacting pair";
        if (zero != 1)
            msg += "s";
        
        msg+= ".<br/><br>";
        return msg;
}
	
    //
    /**
     * Sets the significance string of this complex (to be used by 
     * VizMapper/Visualization class). See Config.java to see String definitions
     * @param pValueCutoff pValue
     */
    public void setSignificance(double pValueCutoff){
        if(posPValue != null && posPValue < pValueCutoff 
                && negPValue != null && negPValue < pValueCutoff)
            significance = Config.nodeBothSignificanceKey;
        else if(posPValue != null && posPValue < pValueCutoff)
            significance = Config.nodePosSignificanceKey;
        else if(negPValue != null && negPValue < pValueCutoff)
            significance = Config.nodeNegSignificanceKey;
        else
            significance = Config.nodeNoSignificanceKey;
    }
    
    /**
     * @return Significance of the complex
     */
    public String getSignificance(){
        return significance;
    }
	
    /**
     * @return Name of complex
     */
    public String getName(){
        return complexName;
    }
    
    /**
     * Checks if gene is contained in complex
     * @param g Gene
     * @return true if gene is contained in the complex, false otherwise
     */
    public boolean containsGene(Gene g){
        return genesWithin.contains(g);
    }
    
    /**
     * Add gene to the complex
     * @param g Gene
     */
    public void addGene(Gene g){
        genesWithin.add(g);
    }

    /**
     * @return Number of genes in relational list
     */
    public int getNumGenesInRelationalList(){
        return genesInRelationList.size();
    }
    
    /**
     * Compares sizes of two complexes
     * @param c2 Target complex
     * @return DOCUMENT ME
     */
    public int compareTo(Complex c2) {
        return getName().compareTo(c2.getName());
    }

    /**
     * Checks if complex name equals a string
     * @param s Compared string
     * @return true if complex name equals passed string, false otherwise
     */
    public boolean equals(String s){
        return complexName.equals(s);
    }
    
    /**
     * @return Size of complex
     */
    public int size(){
        return genesWithin.size();
    }
    
    /**
     * @return Set of genes contained in the complex (from complex file)
     */
    public Set<Gene> getGenes(){
        return genesWithin;
    }
    
    /**
     * @return Set of genes interacting (i.e. exists in the relational file)
     */
    public Set<Gene> getActualGenes(){
        return genesInRelationList;
    }
    
    /**
     * Add gene to the set of genes interacting (i.e. exists in the relational file)
     * @param g Gene to add
     */
    public void addGeneInRelationList(Gene g){
        genesInRelationList.add(g);
    }

    /**
     * Update positive, negative and neutral counts given a score meets cutoff
     * @param score Score of interaction
     * @param positiveCutoff Positive cutoff
     * @param negativeCutoff Negative cutoff
     */
    public void processScore(double score, double positiveCutoff, double negativeCutoff) {
        if(score > positiveCutoff){
            pos++;
            zero--;
        }
        else if(score < negativeCutoff){
            neg++;
            zero--;
        }
    }

    /**
     * Initiate counts for positive, negative, neutral and total number of interactions 
     * @param actualInteractions Total number of interactions
     */
    public void initiateInfo(int actualInteractions) {
        actual = actualInteractions;
        pos = 0;
        neg = 0;
        zero = actual;
    }

    /**
     * @return Total number of interactions involving the complex
     */
    public int getNumInteractions(){
        return actual;
    }
    
    /**
     * @return Number of positive interactions involving the complex
     */
    public int posRelations() {
        return pos;
    }
    
    /**
     * @return Number of negative interactions involving the complex
     */
    public int negRelations() {
        return neg;
    }
    
    /**
     * @return Number of neutral interactions involving the complex
     */
    public int zeroRelations() {
            return zero;
    }
    
    /**
     * @return Positive p-value for the complex
     */
    public Double posPValue(){
            return posPValue;
    }
    
    /**
     * @return Negative p-value for the complex
     */
    public Double negPValue(){
            return negPValue;
    }
    
    /**
     * Set positive p-value of a complex
     * @param pos Positive p-value
     */
    public void setPosPValue(double pos){
            posPValue = pos;
    }
    
    /**
     * Set negative p-value of a complex
     * @param neg Negative p-value
     */
    public void setNegPValue(double neg){
            negPValue = neg;
    }
    
    /**
     * @return String representation of a complex (name of complex)
     */
    @Override
    public String toString(){
        return getName();
    }
}
