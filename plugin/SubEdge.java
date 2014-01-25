package plugin;

import java.io.Serializable;

/**
 * 
 * @author YH
 * An edge representing an interaction to a gene.  This edge is typically stored in the source gene and
 * the gene field points to the target gene.
 *
 */

public class SubEdge implements Serializable {

    private boolean physical;
    private double score;
    private Gene gene;
    
    /**
     * SubEdge constructor
     * @param p Boolean: physical. True if subedge is physical interaction, false otherwise
     * @param s Interaction score
     * @param g Gene
     */
    public SubEdge(boolean p, double s, Gene g){
        gene = g;
        score = s;
        physical = p;
    }
    
    /**
     * @return Gene in the SubEdge
     */
    public Gene getGene(){
        return gene;
    }
    
    /**
     * @return Gene Identifier
     */
    public String getGeneIdentifier(){
        return gene.getGeneIdentifier();
    }
    
    /**
     * @return Gene Name 
     */
    public String getGeneName(){
        return gene.getGeneName();
    }
    
    /**
     * @return SubEdge score
     */
    public double getScore(){
        return score;
    }
    
    /**
     * @return True if SubEdge is physical, false otherwise
     */
    public boolean isPhysical(){
        return physical;
    }
    
    /**
     * @return True if SubEdge is genetic, false otherwise
     */
    public boolean isGenetic(){
        return !physical;
    }
}
