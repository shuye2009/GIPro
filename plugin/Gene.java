package plugin;

import java.util.HashMap;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;


/**
 * Gene class
 * 
 * contains all neighbours of this gene as well as the score.
 * contains physical interactions too as well as complexes it belongs in
 * @author YH
 *
 */
public class Gene implements Comparable<Gene>, Serializable{

    private Map<Gene, SubEdge> geneticNeighbours;
    private Map<Gene, SubEdge> unfilteredGeneticNeighbours;
    private Map<Gene, SubEdge> physicalNeighbours;
    private Map<Gene, Double[]> unfilteredCorrelations;
    private Map<Gene, Double[]> correlations;

    private Set<Complex> inTheseComplexes;

    private String name;
    private String identifier;
    
    /**
     * Gene constructor
     * @param n name of gene
     * @param i identifier
     */
    public Gene(String n, String i){
        name = n;
        identifier = i;
        geneticNeighbours = new TreeMap<Gene, SubEdge>();
        unfilteredGeneticNeighbours = new TreeMap<Gene, SubEdge>();
        physicalNeighbours = new TreeMap<Gene, SubEdge>();
        correlations = new HashMap<Gene, Double[]>();
        unfilteredCorrelations = new HashMap<Gene, Double[]>();
        inTheseComplexes = new TreeSet<Complex>();
    }
    
    /**
     * Set gene name
     * @param s new gene name
     */
    public void setGeneName(String s){
        name = s;
    }
    
    /**
     * @return If current gene name is not null, return it. Else return current 
     * gene identifier
     */
    public String getGeneName() {
        if(name == null){
            return getGeneIdentifier();
        }
        return name;
    }
    
    /**
     * Set gene identifier
     * @param s new gene identifier
     */
    public void setGeneIdentifier(String s){
        identifier = s;
    }
    
    /**
     * @return current gene identifier
     */
    public String getGeneIdentifier() {
        return identifier;
    }
    
    /**
     * @return Set of genes affiliated with this gene
     */
    public Set<Gene> getGeneNeighbours(){
        return geneticNeighbours.keySet();
    }
    
    public Set<Gene> getUnfilteredGeneNeighbours(){
        return unfilteredGeneticNeighbours.keySet();
    }
    
    /**
     * Add edge (SubEdge) to this gene
     * @param s SubEdge to add
     */
    public void addEdge(SubEdge s){
        if(s.isPhysical()){
            physicalNeighbours.put(s.getGene(), s);
        }
        else if(s.isGenetic()){
            geneticNeighbours.put(s.getGene(), s);
        }
    }
    
    public void addUnfilteredGeneticEdge(SubEdge e){
        unfilteredGeneticNeighbours.put(e.getGene(), e);
    }
    
    /**
     * Add passed complex to a list of complexes which contain the gene 
     * @param c passed complex
     */
    public void addComplex(Complex c){
        inTheseComplexes.add(c);
    }
    
    /**
     * Check if passed gene is physically connected to target gene
     * @param g Target gene
     * @return true if physically connected to target gene, else otherwise
     */
    public boolean physicallyConnectedTo(Gene g){
            return physicalNeighbours.containsKey(g);
    }

    /**
     * Check if passed gene is genetically connected to target gene
     * @param g Target gene
     * @return true if genetically connected to target gene, else otherwise
     */
    public boolean geneticallyConnectedTo(Gene g){
            return geneticNeighbours.containsKey(g);
    }
    
    public boolean unfilteredGeneticallyConnectedTo(Gene g){
            return unfilteredGeneticNeighbours.containsKey(g);
    }

    /**
     * @return Returns the configuration key relating to whether or not the gene
     * is interacting
     */
    public String getGIKey(){
        if (geneticNeighbours.isEmpty()){
            return Config.nonInteractingGeneKey;
        }
        else{
            return Config.interactingGeneKey;
        }

    }
    
    /**
     * Check if this gene is physically connected to target gene and return 
     * SubEdge if possible
     * @param g Target gene
     * @return SubEdge connecting both genes physically if it exists, null otherwise
     */
    public SubEdge getPhysicalEdge(Gene g){
        if(!physicallyConnectedTo(g)){
            return null;
        }else{
            return physicalNeighbours.get(g);
        }
    }
    /**
     * Check if this gene is genetically connected to target gene and return 
     * SubEdge if possible
     * @param g Target gene
     * @return SubEdge connecting both genes genetically if it exists, null otherwise
     */
    public SubEdge getGeneticEdge(Gene g){
        if(!geneticallyConnectedTo(g)){
            return null;
        }else{
            return geneticNeighbours.get(g);
        }
    }
    
    public SubEdge getUnfilteredGeneticEdge(Gene g){
        if(!unfilteredGeneticallyConnectedTo(g)){
            return null;
        }else{
            return unfilteredGeneticNeighbours.get(g);
        }
    }
    
    /**
     * Compare two genes
     * @param o Target gene
     * @return 
     */
    @Override
    public int compareTo(Gene o) {
        return getGeneIdentifier().compareTo(o.getGeneIdentifier());
    }   
    
    /**
     * @return Set of complexes which contain this gene
     */
    public Set<Complex> getComplexes(){
        return inTheseComplexes;
    }

    /**
     * String representation of gene
     * @return gene name if not null. If gene name null, returns gene identifier
     */
    @Override
    public String toString(){
        if(getGeneName() == null){
            return getGeneIdentifier();
        }else{
            return getGeneName();
        }
    }

    /**
     * Html format of information for this gene, used in side-panel
     * @param complexesInvolved Set of complexes involved
     * @return Html format of information for this gene, used in side-panel
     */
    public String getInformation(Set<Complex> complexesInvolved) {
        String msg = 
                
        "<b>Name:</b> " + getGeneName() + "<br>"
        + "<b>Identifier:</b> " + getGeneIdentifier() + "<br>"
        + "Belongs in:<ul>";
        
        for(Complex c : inTheseComplexes){
            if(complexesInvolved.contains(c)){
                    msg += "<li><b>Complex " + c.getName() + "</b></li>";
            }
            else{
                    msg += "<li>Complex " + c.getName() + "</li>";
            }
        }
        
        msg += "</ul>";
        
        return msg;
    }
    
    /**
     * Unknown usage...
     * @param g2
     * @return 
     */
    public Double[] unfilteredPearsonCorrelationBetween (Gene g2){
        if (unfilteredCorrelations.containsKey(g2)){
            return unfilteredCorrelations.get(g2);
        }
        
        Vector<Double> g1Vector = new Vector();
        Vector<Double> g2Vector = new Vector();
        Set<Gene> allGenes = new HashSet<Gene>(getUnfilteredGeneNeighbours());
        allGenes.addAll(g2.getUnfilteredGeneNeighbours());
        
        for (Gene g : allGenes){
            if (unfilteredGeneticallyConnectedTo(g) && g2.unfilteredGeneticallyConnectedTo(g)){
                try{
                    g2Vector.add(g.getUnfilteredGeneticEdge(g2).getScore());
                    g1Vector.add(getUnfilteredGeneticEdge(g).getScore());
                }catch(Exception e){
                    System.out.println("error in Gene.java");
                    e.printStackTrace();
                }
            }
        }
        
        
        if (g1Vector.isEmpty()){
            Double[] toReturn = new Double[]{null, (double)g1Vector.size()};
            unfilteredCorrelations.put(g2, toReturn);
            return toReturn;
        }
        else{
            Double r = PearsonTable.pearson(g1Vector, g2Vector);
            Double[] toReturn = new Double[]{r, (double)g1Vector.size()};
            unfilteredCorrelations.put(g2, toReturn);
            return toReturn;
        }
    }

    public Double[] pearsonCorrelationBetween (Gene g2){
        if (correlations.containsKey(g2)){
            return correlations.get(g2);
        }
        
        Vector<Double> g1Vector = new Vector();
        Vector<Double> g2Vector = new Vector();
        Set<Gene> allGenes = new HashSet<Gene>(geneticNeighbours.keySet());
        allGenes.addAll(g2.getGeneNeighbours());
        
        for (Gene g : allGenes){
            if (geneticallyConnectedTo(g) && g2.geneticallyConnectedTo(g)){
                    g2Vector.add(g.getGeneticEdge(g2).getScore());
                    g1Vector.add(getGeneticEdge(g).getScore());
            }
        }
        
        Double r = PearsonTable.pearson(g1Vector, g2Vector);
        if (r == null){
            correlations.put(g2, null);
            return null;
        }
        else{
            Double[] toReturn = new Double[]{r, (double)g1Vector.size()};
            correlations.put(g2, toReturn);
            return toReturn;
        }
    }
}
