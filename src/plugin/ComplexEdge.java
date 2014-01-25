package plugin;

import java.text.DecimalFormat;
import java.util.List;
import java.io.Serializable;
import java.util.ArrayList;
/**
 * 
 * @author YH
 * Class holds complex edge information
 * Contains 2 complexes, a source and a target, source MUST be less than target using Complex's natural ordering
 * (see Complex.compareTo)
 */

public class ComplexEdge implements Comparable<ComplexEdge>, Serializable{
        
    //source must be < target (LEXICOGRAPHICALLY not necessarily NUMERICALLY)
    private Complex source, target;
    private int total, actual, zero, pos, neg;
    private Double posPValue, negPValue;
    private double scoreSum, negSum, posSum;
    private String significance;
    
    public ComplexEdge(){
        
    }
    
    /**
     * ComplexEdge constructor
     * @param source Source complex
     * @param target Target complex
     * @param totalPairs Number of possible pairs - redundant edges
     * @param actualPairs Actual number of interactions between two complexes
     */
    public ComplexEdge(Complex source, Complex target, int totalPairs, int actualPairs){
        if(source.compareTo(target) >= 0){
            System.out.println("Source is not smaller than target");
            throw new IllegalArgumentException("Source is not smaller than target");
        }
        total = totalPairs;
        actual = actualPairs;
        zero = actual;
        pos = 0;
        neg = 0;
        // keeps track of the sum of all scores (that meet the cutoff) -> for averaging purposes
        scoreSum = 0;
        negSum = 0;
        posSum = 0;
        this.source = source;
        this.target = target;
    }
    
    /**
     * @return Source of complex edge
     */
    public Complex getSource(){
            return source;
    }
    
    /**
     * @return Target of complex edge
     */
    public Complex getTarget(){
            return target;
    }

    @Override
    public int compareTo(ComplexEdge o) {
        if(source.compareTo(o.getSource()) == 0){
            return target.compareTo(o.getTarget());
        }else{
            return source.compareTo(o.getSource());
        }
    }
    
    /**
     * Update total, negative, positive interactions, positive sum of scores and 
     * negative sum of scores given a score
     * @param score Score of interaction
     * @param positiveCutoff Positive cutoff
     * @param negativeCutoff Negative cutoff
     */
    public void processScore(double score, double positiveCutoff, double negativeCutoff) {
        if(score > positiveCutoff){
            pos++;
            zero--;
            scoreSum += score;
            posSum += score;
        }
        else if(score < negativeCutoff){
            neg++;
            zero--;
            scoreSum += score;
            negSum += score;
        }
    }
    
    /**
     * @return Total number of edges
     */
    public int totalNumberEdges() {
        return total;
    }
    
    /**
     * @return Actual number of edges
     */
    public int actualNumberEdges() {
        return actual;
    }
    
    /**
     * @return Number of positive relations
     */
    public int posRelations(){
        return pos;
    }
    
    /**
     * @return Number of negative relations
     */
    public int negRelations(){
            return neg;
    }
    
    /**
     * @return Number of neutral interactions
     */
    public int zeroRelations(){
            return zero;
    }
    
    /**
     * @return Positive p-value
     */
    public Double posPValue(){
            return posPValue;
    }
    
    /**
     * @return Negative p-value
     */
    public Double negPValue(){
            return negPValue;
    }

    /**
     * Unknown usage...
     * @return the averages the pearson correlations for all the gene interactions
     */
    public double averagePearsonCorrelation(){
        double sum = 0;
        int count = 0;
        List<Gene> c1Genes = new ArrayList(source.getGenes());
        List<Gene> c2Genes = new ArrayList(target.getGenes());
        for (Gene g1 : c1Genes){
            for (Gene g2 : c2Genes){
                if (g1.geneticallyConnectedTo(g2)){
                    double pearson = g1.pearsonCorrelationBetween(g2)[0];
                    if (pearson != 0){
                            sum += g1.pearsonCorrelationBetween(g2)[0];
                            count++;
                    }
                }
            }
        }
        return sum/(count);	
    }
    
    /**
     * @return Average score (sum of all scores / positive and negative scores)
     */
    public double getAverageScore(){
        return scoreSum/(pos + neg);
    }
    
    /**
     * @return Average positive score (sum of positive scores / positive scores)
     */
    public double getPosAverageScore(){
        return posSum/pos;
    }
    
    /**
     * @return Average negative score (sum of negative scores / negative scores)
     */
    public double getNegAverageScore(){
        return negSum/neg;
    }
    
    /**
     * Sets positive p-value
     * @param pos Positive p-value
     */
    public void setPosPValue(double pos){
        posPValue = pos;
    }
    
    /**
     * Sets negative p-value
     * @param pos Positive p-value
     */
    public void setNegPValue(double neg){
        negPValue = neg;
    }
    
    /**
     * Checks if interaction is significant given a p-value cutoff
     * @param pvalueCutoff P-value cutoff
     * @return true if interaction is significant with respect to p-value, false, otherwise
     */
    public boolean significant(double pvalueCutoff){
        return (posPValue != null && posPValue < pvalueCutoff) 
            || (negPValue != null && negPValue < pvalueCutoff);
    }
    
	
    /**
     * Returns information on complex edge used in side panel 
     * @return Returns a string of html-formatted ComplexEdge information 
     * (for display in the side panel)
     */
    public String getInformation(){
        DecimalFormat format = new DecimalFormat("#.########");
        StringBuilder sb = new StringBuilder();
        
        sb.append("<font face=arial>");
        
        sb.append("<b>Complex Edge: " + source.getName() + " cc " + target.getName() + "</b><br><br>");
        sb.append("<b>" + total + "</b> pairs (<b>" + actual + "</b> with interaction scores)<br><br>");
        
        //pos 
        sb.append("<b>" + pos +"</b> positive relations ");
        if (posPValue != null){
            sb.append("(<b>p-value: " + format.format(posPValue.doubleValue()) + "</b>)");
        }
        //neg
        sb.append("<br><b>" + neg + "</b> negative relations");
        if (negPValue != null){
            sb.append(" (<b>p-value: " + format.format(negPValue.doubleValue()) + "</b>)");
        }
        
        sb.append("<br><b>" + zero + "</b> non interacting pair");
        if (zero != 1)
            sb.append("s");
        sb.append("</br><br>");
        
        sb.append("</font>");
        
        return sb.toString();
    }
    
    /**
     * Sets the significance string of this complex (to be used by 
     * VizMapper/Visualization class). See Config.java to see String definitions
     * @param pValueCutoff pValue
     */
    public void setSignificance(double pValueCutoff){
        if(posPValue != null && posPValue < pValueCutoff 
                && negPValue != null && negPValue < pValueCutoff)
            significance = Config.edgeBothSignificanceKey;
        else if(posPValue != null && posPValue < pValueCutoff)
            significance = Config.edgePosSignificanceKey;
        else if(negPValue != null && negPValue < pValueCutoff)
            significance = Config.edgeNegSignificanceKey;
        else
            significance = null;
    }
    
    /**
     * @return Significance of the complex
     */
    public String getSignificance(){
        return significance;
    }
}