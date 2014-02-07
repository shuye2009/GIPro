    package plugin;

/**
 * @author YH
 * Holds special attribute names this plugin uses
 */

public class GIProAttributeNames {
        public static final String NETWORK_TYPE = "GIPro_NetworkType";//subnet or complex
	public static final String DISPLAY_NAME = "GIPro_Display";//node labels
        public static final String IS_WITHIN = "GIPro_isWithin";
	public static final String EDGE_TYPE = "GIPro_EdgeType";//p, gp, gn
        public static final String EDGE_CORR = "GIPro_EdgeCorrelation";//person corr
        public static final String EDGE_SCORE = "GIPro_Score";//person corr
        public static final String EDGE_PPISCORE = "GIPro_PPIScore";//person corr
	public static final String NODE_TYPE = "GIPro_NodeType";//p, n, b, 0
	public static final String COMPLEX_COLOR = "GIPro_ComplexColor";
	public static final String COMPLEX_FROM = "GIPro_Complex";
	public static final String IS_SUBNET = "GIPro_IsSubNet";//true means the node borders are thicker
	public static final String NODE_COLOUR = "GIPro_NodeColour";//colour of node
	public static final String NODE_LABEL_COLOUR = "GIPro_InteractingGene";//colour of node
	public static final String NODE_SIZE = "GIPro_NodeSize";//node size
	public static final String NUM_PHYS_INT = "Physical Interaction Count";//node size
	public static final String WEIGHT = "Interaction Weight";
        public static final String EDGE_WIDTH = "GIPro_EdgeWidth";
        public static final String COMPLEX_LAYOUTNONLY = "GIPro_ComplexsForLayoutOnly";
        public static final String IS_MULTICOMPLEX = "GIPro_IsMultiComplexGene";
        public static final String ENRICHMENT_SCORE = "GIPro_EnrichmentScore";
}
