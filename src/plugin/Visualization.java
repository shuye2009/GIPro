package plugin;

/**
 * @author YH
 * Sets up the vizmapper and provides a refreshing function
 * 
 */

import giny.view.EdgeView;

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;

import cytoscape.Cytoscape;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import cytoscape.view.CyDesktopManager;
import cytoscape.view.CyNetworkView;
import cytoscape.visual.Appearance;
import cytoscape.visual.CalculatorCatalog;
import cytoscape.visual.EdgeAppearance;
import cytoscape.visual.EdgeAppearanceCalculator;
import cytoscape.visual.GlobalAppearanceCalculator;
import cytoscape.visual.LineStyle;
import cytoscape.visual.NodeAppearance;
import cytoscape.visual.NodeAppearanceCalculator;
import cytoscape.visual.NodeShape;
import cytoscape.visual.VisualMappingManager;
import cytoscape.visual.VisualPropertyType;
import cytoscape.visual.VisualStyle;
import cytoscape.visual.calculators.BasicCalculator;
import cytoscape.visual.calculators.Calculator;
import cytoscape.visual.mappings.BoundaryRangeValues;
import cytoscape.visual.mappings.ContinuousMapping;
import cytoscape.visual.mappings.DiscreteMapping;
import cytoscape.visual.mappings.Interpolator;
import cytoscape.visual.mappings.LinearNumberToNumberInterpolator;
import cytoscape.visual.mappings.ObjectMapping;
import cytoscape.visual.mappings.PassThroughMapping;
import javax.swing.JOptionPane;

public class Visualization {
	
	public static VisualStyle visualStyle;
	
	public Visualization(){
		createVisualStyle();
	}
	
	public static void refreshNetworkView(CyNetworkView view, boolean drawGraph, boolean doLayout, boolean isSubnet){
		if (doLayout){
                    CyLayoutAlgorithm cla = CyLayouts.getLayout(Config.preferredLayout);
                    if(isSubnet){
                        try{
                        
                        CyLayoutAlgorithm lay = CyLayouts.getLayout(Config.GROUP_ATR_LAYOUT);
                        cla = lay;
                        LayoutProperties props = cla.getSettings();
                        Tunable atr = props.get("attributeName");
                        atr.setValue(GIProAttributeNames.COMPLEX_LAYOUTNONLY);
                        
                        Tunable maxwidth = props.get("maxwidth");
                        maxwidth.setValue(2000.0);
                        
                        Tunable scaleRadius = props.get("radmult");
                        scaleRadius.setValue(50.0);
                        cla.updateSettings();
                        
                        }catch(Exception e){
                            JOptionPane.showMessageDialog(null, "Unable to layout by complex, a default layout will be applied");
                            cla = CyLayouts.getDefaultLayout();
                        }
                    }
                    cla.doLayout(view);
		}
	    
		if (drawGraph){
			CyDesktopManager.arrangeFrames(CyDesktopManager.Arrange.CASCADE);
			Cytoscape.setCurrentNetworkView(view.getIdentifier());
			view.fitContent();
			view.setVisualStyle(visualStyle.getName());
			view.redrawGraph(true, true);
			view.updateView();
			Iterator<?> e = view.getEdgeViewsIterator();
			// Make edge selection colour yellow (easier to differentiate from red edges)
			while(e.hasNext()){
				EdgeView ev = (EdgeView) e.next();
				ev.setSelectedPaint(Color.YELLOW);
			}
		}
	}
	
	
	// Creates our vismapper (called "Homolog Networks Style") in cytoscape.
	// This changes a lot of default properties as well as introduce many
	// mappings on different attributes
	public static void createVisualStyle(){
		VisualMappingManager visualMappingManager = Cytoscape.getVisualMappingManager();
		CalculatorCatalog cc = visualMappingManager.getCalculatorCatalog();
		visualStyle = cc.getVisualStyle("GIPro Style");
		
		if (visualStyle == null) {
			visualStyle = new VisualStyle("GIPro Style");
			
			NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
			EdgeAppearanceCalculator eac = visualStyle.getEdgeAppearanceCalculator();
			GlobalAppearanceCalculator gac = visualStyle.getGlobalAppearanceCalculator();
			
			// node tool tip (displays orf names)
			PassThroughMapping nodeToolTipLabel = new PassThroughMapping(String.class, "tooltip");
			nodeToolTipLabel.setControllingAttributeName("tooltip");
			Calculator nodeToolTipCalculator = new BasicCalculator("node tooltip calculator", nodeToolTipLabel, VisualPropertyType.NODE_TOOLTIP);
			nac.setCalculator(nodeToolTipCalculator);
			
			Appearance na = nac.getDefaultAppearance();
			
			//these variables are used later for vizmapper
			Color defaultColour = Color.PINK;
			Color defaultBorderColour = new Color(Integer.parseInt("7e7e7e", 16));
			NodeShape defaultShape = NodeShape.ELLIPSE;
			float defaultBorderWidth = 2;
			
			//default node appearances
			na.set(VisualPropertyType.NODE_LABEL_COLOR, Color.WHITE);
			na.set(VisualPropertyType.NODE_LINE_WIDTH, defaultBorderWidth);
			na.set(VisualPropertyType.NODE_WIDTH, 60.0);
			na.set(VisualPropertyType.NODE_SHAPE, defaultShape);
			na.set(VisualPropertyType.NODE_FILL_COLOR, defaultColour);
			na.set(VisualPropertyType.NODE_BORDER_COLOR, defaultBorderColour);
			na.set(VisualPropertyType.NODE_FONT_FACE, new Font("SansSerif", Font.BOLD, 18));
                        na.set(VisualPropertyType.EDGE_LABEL_COLOR, Color.WHITE);
			nac.setDefaultAppearance((NodeAppearance) na);
			
			// setting default appearances for edges
			Appearance ea = eac.getDefaultAppearance();
			ea.set(VisualPropertyType.EDGE_COLOR, new Color(Integer.parseInt("59b82a", 16)));
			//ea.set(VisualPropertyType.EDGE_OPACITY, 166);
			ea.set(VisualPropertyType.EDGE_LINE_WIDTH, 1.61803);
			eac.setDefaultAppearance((EdgeAppearance) ea);
			
			Color defaultEdgeColour = (Color) ea.get(VisualPropertyType.EDGE_COLOR);
			Double defaultEdgeWidth = (Double)ea.get(VisualPropertyType.EDGE_LINE_WIDTH);
			// setting default appearances for global
			gac.setDefaultBackgroundColor(Color.BLACK);

			//Node label set as the display name
			PassThroughMapping pm = new PassThroughMapping(String.class, GIProAttributeNames.DISPLAY_NAME);
			pm.setControllingAttributeName(GIProAttributeNames.DISPLAY_NAME);
			Calculator nlc = new BasicCalculator("NodeLabelCalculatrice", pm, VisualPropertyType.NODE_LABEL);
			nac.setCalculator(nlc);
			
                        
			// Node Label Colouring based on whether a gene is genetically interacting
			DiscreteMapping nodeLabelColour = new DiscreteMapping(defaultColour, ObjectMapping.NODE_MAPPING);
			nodeLabelColour.setControllingAttributeName(GIProAttributeNames.NODE_LABEL_COLOUR);

			nodeLabelColour.putMapValue(Config.interactingGeneKey, Config.interactingGeneColour);
			nodeLabelColour.putMapValue(Config.nonInteractingGeneKey, Config.noninteractingGeneColour);
			Calculator nodeLabelCalculator = new BasicCalculator("Label Colour", nodeLabelColour, VisualPropertyType.NODE_LABEL_COLOR);
			nac.setCalculator(nodeLabelCalculator);
                        
                        // Node Label FONT based on whether a gene is genetically interacting
                        DiscreteMapping nodeLabelFont = new DiscreteMapping(new Font("SansSerif", Font.BOLD,12), ObjectMapping.NODE_MAPPING);
			nodeLabelFont.setControllingAttributeName(GIProAttributeNames.NODE_LABEL_COLOUR);

			nodeLabelFont.putMapValue(Config.interactingGeneKey, new Font("SansSerif", Font.BOLD,12));
			nodeLabelFont.putMapValue(Config.nonInteractingGeneKey, new Font("SansSerif", Font.ITALIC,10));
			Calculator nodeLabelFontCalculator = new BasicCalculator("Label FONT", nodeLabelFont, VisualPropertyType.NODE_FONT_FACE);
			nac.setCalculator(nodeLabelFontCalculator);
                        
                        
                        
                        
                        //NEW EDGE WIDTH
			PassThroughMapping edgeWidthMap = new PassThroughMapping(1.0, ObjectMapping.EDGE_MAPPING);
			edgeWidthMap.setControllingAttributeName(GIProAttributeNames.EDGE_WIDTH);
			
			Calculator edgeWidthCalculator = new BasicCalculator("Edge Width", edgeWidthMap, VisualPropertyType.EDGE_LINE_WIDTH);
			eac.setCalculator(edgeWidthCalculator);
                        
                        
                        
                        
			//Node border width
			DiscreteMapping borderWidthMap = new DiscreteMapping(defaultBorderWidth, ObjectMapping.NODE_MAPPING);
			borderWidthMap.setControllingAttributeName(GIProAttributeNames.IS_SUBNET);
			
			borderWidthMap.putMapValue(true, 3.14159);
			borderWidthMap.putMapValue(false, defaultBorderWidth);
			
			Calculator nodeBorderWidthCalculator = new BasicCalculator("Node Border Width", borderWidthMap, VisualPropertyType.NODE_LINE_WIDTH);
			nac.setCalculator(nodeBorderWidthCalculator);
                        
                        //NODE BORDER WIDTH FOR MULTICOMPLEX
                        DiscreteMapping borderWidthMap2 = new DiscreteMapping(defaultBorderWidth, ObjectMapping.NODE_MAPPING);
			borderWidthMap2.setControllingAttributeName(GIProAttributeNames.IS_MULTICOMPLEX);
			
			borderWidthMap2.putMapValue(true, defaultBorderWidth*5);
			
			Calculator nodeBorderWidthCalculator2 = new BasicCalculator("Node Border Width2", borderWidthMap2, VisualPropertyType.NODE_LINE_WIDTH);
			nac.setCalculator(nodeBorderWidthCalculator2);
                        
                        
                        
                        //NODE BORDER COLOR FOR MULTICOMPLEX
                        DiscreteMapping nodeColorMapMulti = new DiscreteMapping(defaultBorderColour, ObjectMapping.NODE_MAPPING);
			nodeColorMapMulti.setControllingAttributeName(GIProAttributeNames.COMPLEX_COLOR);
			
                        nodeColorMapMulti.putMapValue(true, 
                                new Color(Integer.parseInt(Config.MULTICOMPLEX_COLOUR, 16)));
                        
                        Calculator nodeBorderColourMultiCalculator = new 
                                BasicCalculator("Node Border Colour multicomplex", nodeColorMapMulti, VisualPropertyType.NODE_BORDER_COLOR);
			nac.setCalculator(nodeBorderColourMultiCalculator);
                        
                        
			
			//Node colour
			DiscreteMapping nodeColorMap = new DiscreteMapping(defaultBorderColour, ObjectMapping.NODE_MAPPING);
			nodeColorMap.setControllingAttributeName(GIProAttributeNames.COMPLEX_COLOR);
			
                        //nodeColorMap.putMapValue(Config.MULTICOMPLEX_COLOUR, new Color(Integer.parseInt(Config.MULTICOMPLEX_COLOUR, 16)));
			
			for(String c : Config.DISTINGUISHABLE_COLOURS){
				nodeColorMap.putMapValue(c, new Color(Integer.parseInt(c, 16)));
			}
                        
			
			nodeColorMap.putMapValue(Config.nodePosSignificanceKey, Config.posNodeColour);
			nodeColorMap.putMapValue(Config.nodeNegSignificanceKey, Config.negNodeColour);
			nodeColorMap.putMapValue(Config.nodeBothSignificanceKey, Config.bothNodeColour);
			nodeColorMap.putMapValue(Config.nodeNoSignificanceKey, Config.noneNodeColour);
			
			Calculator nodeColourCalculator = new BasicCalculator("Node Border Colour", nodeColorMap, VisualPropertyType.NODE_FILL_COLOR);
			nac.setCalculator(nodeColourCalculator);
                        
                        
                        
			//Node size
			PassThroughMapping nodeSizeMap = new PassThroughMapping(60.0, ObjectMapping.NODE_MAPPING);
			nodeSizeMap.setControllingAttributeName(GIProAttributeNames.NODE_SIZE);
			
			Calculator nodeSizeCalculator = new BasicCalculator("Node Size", nodeSizeMap, VisualPropertyType.NODE_SIZE);
			nac.setCalculator(nodeSizeCalculator);
			
			// edge tool tip
			PassThroughMapping edgeToolTipLabel = new PassThroughMapping(new String(), "ID");
			edgeToolTipLabel.setControllingAttributeName("interaction");
			
			Calculator edgeToolTipCalculator = new BasicCalculator("edge tooltip calculator", edgeToolTipLabel, VisualPropertyType.EDGE_TOOLTIP);
			eac.setCalculator(edgeToolTipCalculator);
			
			//new edge colour algorithm
			DiscreteMapping edgeColour = new DiscreteMapping(defaultEdgeColour, ObjectMapping.EDGE_MAPPING);
			edgeColour.setControllingAttributeName(GIProAttributeNames.EDGE_TYPE);

			edgeColour.putMapValue(Config.edgePhysicalKey, Config.physEdgeColour);
			edgeColour.putMapValue(Config.edgePosSignificanceKey, Config.posEdgeColour);
			edgeColour.putMapValue(Config.edgeNegSignificanceKey, Config.negEdgeColour);
                        edgeColour.putMapValue(Config.edgePosCorrelationKey, Config.posEdgeColour);
                        edgeColour.putMapValue(Config.edgeNegCorrelationKey, Config.negEdgeColour);
                        
			
			Calculator edgeColourCalculator = new BasicCalculator("edge colour calculator", edgeColour, VisualPropertyType.EDGE_COLOR);
			eac.setCalculator(edgeColourCalculator);

			// Edge Width
			DiscreteMapping edgeWidth = new DiscreteMapping(defaultEdgeWidth, ObjectMapping.EDGE_MAPPING);
			edgeWidth.setControllingAttributeName(GIProAttributeNames.EDGE_TYPE);

			edgeWidth.putMapValue(Config.edgePhysicalKey, Config.physEdgeWidth);
			edgeWidth.putMapValue(Config.edgePosSignificanceKey, Config.posEdgeWidth);
			edgeWidth.putMapValue(Config.edgeNegSignificanceKey, Config.negEdgeWidth);
                        edgeWidth.putMapValue(Config.edgePosCorrelationKey, Config.correlationEdgeWidth);
                        edgeWidth.putMapValue(Config.edgeNegCorrelationKey, Config.correlationEdgeWidth);
                        
                        
			
//			Calculator edgeWidthCalculator = new BasicCalculator("edge width calculator", edgeWidth, VisualPropertyType.EDGE_LINE_WIDTH);
//			eac.setCalculator(edgeWidthCalculator);
                        
                        /*PEARSON CORRELATION STUFF*/
                        
                        // Edge Label color
			DiscreteMapping edgeLabelColor = new DiscreteMapping(Color.WHITE, ObjectMapping.EDGE_MAPPING);
			edgeLabelColor.setControllingAttributeName(GIProAttributeNames.EDGE_TYPE);
                        edgeLabelColor.putMapValue(Config.edgePosCorrelationKey, Color.WHITE);
                        edgeLabelColor.putMapValue(Config.edgeNegCorrelationKey, Color.WHITE);
			Calculator edgeLabelColorCalculator = new BasicCalculator("edge label color calculator", edgeLabelColor, 
                                VisualPropertyType.EDGE_LABEL_COLOR);
			eac.setCalculator(edgeLabelColorCalculator);
                        
                        // Edge Label size
			DiscreteMapping edgeLabelSize = new DiscreteMapping(8.0, ObjectMapping.EDGE_MAPPING);
			edgeLabelSize.setControllingAttributeName(GIProAttributeNames.EDGE_TYPE);
                        edgeLabelSize.putMapValue(Config.edgePosCorrelationKey, 8.0);
                        edgeLabelSize.putMapValue(Config.edgeNegCorrelationKey, 8.0);
			Calculator edgeLabelSizeCalculator = new BasicCalculator("edge label size calculator", edgeLabelSize, 
                                VisualPropertyType.EDGE_FONT_SIZE);
			eac.setCalculator(edgeLabelSizeCalculator);
                        
                        // Edge line style
			DiscreteMapping edgeLineStyle = new DiscreteMapping(LineStyle.EQUAL_DASH, ObjectMapping.EDGE_MAPPING);
			edgeLineStyle.setControllingAttributeName(GIProAttributeNames.EDGE_TYPE);
                        edgeLineStyle.putMapValue(Config.edgePosCorrelationKey, LineStyle.EQUAL_DASH);
                        edgeLineStyle.putMapValue(Config.edgeNegCorrelationKey, LineStyle.EQUAL_DASH);
			Calculator edgeLineStyleCalculator = new BasicCalculator("edge label size calculator", edgeLineStyle, 
                                VisualPropertyType.EDGE_LINE_STYLE);
			eac.setCalculator(edgeLineStyleCalculator);
                        
                        // edge label
			PassThroughMapping edgeLable = new PassThroughMapping(String.class, "interaction");
			edgeLable.setControllingAttributeName(GIProAttributeNames.EDGE_CORR);
			
			Calculator edgeLabelCalculator = new BasicCalculator("edge label calculator", edgeLable, VisualPropertyType.EDGE_LABEL);
			eac.setCalculator(edgeLabelCalculator);
                        
                        
			ContinuousMapping scoreStrength = new ContinuousMapping(4, ObjectMapping.EDGE_MAPPING);
                        scoreStrength.setControllingAttributeName(GIProAttributeNames.EDGE_SCORE);
                        Interpolator scoreToWidth = new LinearNumberToNumberInterpolator();
                        scoreStrength.setInterpolator(scoreToWidth);
                        BoundaryRangeValues bv0 = new BoundaryRangeValues(3, 3, 3);
			BoundaryRangeValues bv1 = new BoundaryRangeValues(11,11,11);
                        
                        
                        scoreStrength.addPoint(-1, bv1); 
                        scoreStrength.addPoint(0, bv0); 
			scoreStrength.addPoint(1, bv1);
                        
                        Calculator geneEdgeWidthCalculator = new BasicCalculator("gene edge width calculator", scoreStrength, VisualPropertyType.EDGE_LINE_WIDTH);
			eac.setCalculator(geneEdgeWidthCalculator);
                        
			/*
			ContinuousMapping edgeWidth = new ContinuousMapping(1.61803, ObjectMapping.EDGE_MAPPING);
			edgeWidth.setControllingAttributeName(GIProAttributeNames.WEIGHT);
			
			Interpolator scoreToWidth = new LinearNumberToNumberInterpolator();
			edgeWidth.setInterpolator(scoreToWidth);
	
			BoundaryRangeValues bv0 = new BoundaryRangeValues(1, 1, 1);
			BoundaryRangeValues bv1 = new BoundaryRangeValues(10,10,12);
			
			// Set the attribute point values associated with the boundary values
			edgeWidth.addPoint(0, bv0); 
			edgeWidth.addPoint(2.5, bv1);
			   
			Calculator edgeWidthCalculator = new BasicCalculator("edge width calculator", edgeWidth, VisualPropertyType.EDGE_LINE_WIDTH);
			eac.setCalculator(edgeWidthCalculator);
			*/
			// Create the visual style
			visualStyle.setNodeAppearanceCalculator(nac);
			visualStyle.setEdgeAppearanceCalculator(eac);
			visualStyle.setGlobalAppearanceCalculator(gac);
			
			// The visual style must be added to the Global Catalog
			// in order for it to be written out to vizmap.props upon user exit
			cc.addVisualStyle(visualStyle);
		}
		// actually apply the visual style
		visualMappingManager.setVisualStyle(visualStyle);
		visualMappingManager.applyAppearances();
	}//end createVisualStyle
        
        //For some reason, the mapping attribute has to be changed then changed back for labels to update properly 
        public static void refreshLabels()  {
            VisualMappingManager visualMappingManager = Cytoscape.getVisualMappingManager();
            CalculatorCatalog cc = visualMappingManager.getCalculatorCatalog();
            EdgeAppearanceCalculator eac = visualStyle.getEdgeAppearanceCalculator();
            // Change mapping label to edge type
            PassThroughMapping edgeLable = new PassThroughMapping(String.class, "interaction");
            edgeLable.setControllingAttributeName(GIProAttributeNames.EDGE_TYPE);
            Calculator edgeLabelCalculator = new BasicCalculator("edge label calculator", edgeLable, VisualPropertyType.EDGE_LABEL);
            eac.setCalculator(edgeLabelCalculator);
            
            visualStyle.setEdgeAppearanceCalculator(eac);
            visualMappingManager.applyAppearances();
            
            // Change back
            edgeLable = new PassThroughMapping(String.class, "interaction");
            edgeLable.setControllingAttributeName(GIProAttributeNames.EDGE_CORR);
            edgeLabelCalculator = new BasicCalculator("edge label calculator", edgeLable, VisualPropertyType.EDGE_LABEL);
            eac.setCalculator(edgeLabelCalculator);
            
            visualStyle.setEdgeAppearanceCalculator(eac);
            visualMappingManager.applyAppearances();
        }
}
