package plugin;

/**
 * 
 * @author YH
 * Holds configuration parameters
 *
 */


import java.awt.Color;
import java.text.DecimalFormat;

import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;

public class Config {
    public static Color correlationEdgeColor, physEdgeColour, posEdgeColour, negEdgeColour, 
            posNodeColour, negNodeColour, bothNodeColour, noneNodeColour, 
            interactingGeneColour, noninteractingGeneColour;
    public static String preferredLayout, nodePosSignificanceKey, 
            nodeNegSignificanceKey, nodeBothSignificanceKey, 
            nodeNoSignificanceKey, edgePosSignificanceKey, 
            edgeNegSignificanceKey, edgePhysicalKey, edgeBothSignificanceKey,
            edgePosCorrelationKey,edgeNegCorrelationKey,
            interactingGeneKey, nonInteractingGeneKey;

    public static DecimalFormat format = new DecimalFormat("#.#####");
    
    public static final String FIRST_LAYOUT = "kamada-kawai";
    public static final String SECOND_LAYOUT = "Kamada-Kawai";
    //version August 27/2010
    public static final String ROOT_NODE_NAME = "Version 2.71828182845904523536";

    public static final String[] DISTINGUISHABLE_COLOURS = new String[]
            {"FF0000", "0000FF", "00FFFF", "FF00FF", "FFFF00", "00FF00"};
    public static final String MULTICOMPLEX_COLOUR = "FFFFFF";

    public static double physEdgeWidth, posEdgeWidth, negEdgeWidth, correlationEdgeWidth;
    
    /**
     * Config constructor
     */
    public Config(){
        for(CyLayoutAlgorithm o : CyLayouts.getAllLayouts()){
            if(o.getName().equals(FIRST_LAYOUT)){
                preferredLayout = FIRST_LAYOUT;
                break;
            }
            if(o.getName().equals(SECOND_LAYOUT)){
                preferredLayout = SECOND_LAYOUT;
                break;
            }
        }
        physEdgeColour = new Color(0,81,193);	
        posEdgeColour = new Color(170,0,0);
        negEdgeColour = new Color(0,153,0);
        correlationEdgeColor = new Color(250,250,250);

        physEdgeWidth = 1.0;
        posEdgeWidth = 3.0;
        negEdgeWidth = 3.0;
        correlationEdgeWidth = 1.0;

        posNodeColour = Color.MAGENTA;
        negNodeColour = Color.CYAN;
        bothNodeColour = Color.GREEN;
        noneNodeColour = Color.white;

        interactingGeneColour = Color.YELLOW;
        noninteractingGeneColour = Color.GRAY;

        nodePosSignificanceKey = "p";
        nodeNegSignificanceKey = "n";
        nodeBothSignificanceKey = "b";
        nodeNoSignificanceKey = "0";

        edgePosSignificanceKey = "gp";
        edgeNegSignificanceKey = "gn";
        edgePhysicalKey = "p";
        edgePosCorrelationKey = "rp";
        edgeNegCorrelationKey = "rn";
        edgeBothSignificanceKey = "b";

        interactingGeneKey = "i";
        nonInteractingGeneKey = "ni";

        new Visualization();
    }
}
