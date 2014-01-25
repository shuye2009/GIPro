    package plugin;

/**
 * @author YH
 * Holds special attribute names this plugin uses
 */

public class SpecialAttributeNames {
	public static final String DISPLAY_NAME = "ICTools_Display";//node labels
	public static final String EDGE_TYPE = "ICTools_EdgeType";//p, gp, gn
        public static final String EDGE_CORR = "ICTools_EdgeCorrelation";//person corr
	public static final String NODE_TYPE = "ICTools_NodeType";//p, n, b, 0
	public static final String COMPLEX_FROM = "ICTools_Complex";
	public static final String IS_SUBNET = "ICTools_IsSubNet";//true means the node borders are thicker
	public static final String NODE_COLOUR = "ICTools_NodeColour";//colour of node
	public static final String NODE_LABEL_COLOUR = "ICTools_InteractingGene";//colour of node
	public static final String NODE_SIZE = "ICTools_NodeSize";//node size
	public static final String NUM_PHYS_INT = "Physical Interaction Count";//node size
	public static final String WEIGHT = "Interaction Weight";
}
