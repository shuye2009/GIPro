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

public class Visualization {
	
	public static VisualStyle visualStyle;
	
	public Visualization(){
		createVisualStyle();
	}
	
	public static void refreshNetworkView(CyNetworkView view, boolean drawGraph, boolean doLayout){
		if (doLayout){
			CyLayoutAlgorithm cla = CyLayouts.getLayout(Config.preferredLayout);
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
		visualStyle = cc.getVisualStyle("ICTools Style");
		
		if (visualStyle == null) {
			visualStyle = new VisualStyle("ICTools Style");
			
			NodeAppearanceCalculator nac = visualStyle.getNodeAppearanceCalculator();
			EdgeAppearanceCalculator eac = visualStyle.getEdgeAppearanceCalculator();
			GlobalAppearanceCalculator gac = visualStyle.getGlobalAppearanceCalculator();
			
			// node tool tip (displays orf names)
			PassThroughMapping nodeToolTipLabel = new PassThroughMapping(new String(), "tooltip");
			nodeToolTipLabel.setControllingAttributeName("tooltip", null, false);
			Calculator nodeToolTipCalculator = new BasicCalculator("node tooltip calculator", nodeToolTipLabel, VisualPropertyType.NODE_TOOLTIP);
			nac.setCalculator(nodeToolTipCalculator);
			
			Appearance na = nac.getDefaultAppearance();
			
			//these variables are used later for vizmapper
			Color defaultColour = Color.PINK;
			Color defaultBorderColour = Color.BLACK;
			NodeShape defaultShape = NodeShape.ELLIPSE;
			float defaultBorderWidth = 2;
			
			//default node appearances
			na.set(VisualPropertyType.NODE_LABEL_COLOR, Color.DARK_GRAY);
			na.set(VisualPropertyType.NODE_LINE_WIDTH, defaultBorderWidth);
			//na.set(VisualPropertyType.NODE_OPACITY, 160);
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
			PassThroughMapping pm = new PassThroughMapping(new String(), SpecialAttributeNames.DISPLAY_NAME);
			pm.setControllingAttributeName(SpecialAttributeNames.DISPLAY_NAME, null, false);
			Calculator nlc = new BasicCalculator("NodeLabelCalculatrice", pm, VisualPropertyType.NODE_LABEL);
			nac.setCalculator(nlc);
			
			// Node Label Colouring based on whether a gene is genetically interacting
			DiscreteMapping nodeLabelColour = new DiscreteMapping(defaultColour, ObjectMapping.NODE_MAPPING);
			nodeLabelColour.setControllingAttributeName(SpecialAttributeNames.NODE_LABEL_COLOUR, null, false);

			nodeLabelColour.putMapValue(Config.interactingGeneKey, Config.interactingGeneColour);
			nodeLabelColour.putMapValue(Config.nonInteractingGeneKey, Config.noninteractingGeneColour);
			Calculator nodeLabelCalculator = new BasicCalculator("Label Colour", nodeLabelColour, VisualPropertyType.NODE_LABEL_COLOR);
			nac.setCalculator(nodeLabelCalculator);
			
			// Node Colour
			DiscreteMapping disMapping = new DiscreteMapping(defaultColour, ObjectMapping.NODE_MAPPING);
			disMapping.setControllingAttributeName(SpecialAttributeNames.NODE_TYPE, null, false);
			
			disMapping.putMapValue(Config.nodePosSignificanceKey, Config.posNodeColour);
			disMapping.putMapValue(Config.nodeNegSignificanceKey, Config.negNodeColour);
			disMapping.putMapValue(Config.nodeBothSignificanceKey, Config.bothNodeColour);
			disMapping.putMapValue(Config.nodeNoSignificanceKey, Config.noneNodeColour);

			Calculator nodeColourCalculator = new BasicCalculator("Node Colour", disMapping, VisualPropertyType.NODE_FILL_COLOR);
			
			nac.setCalculator(nodeColourCalculator);
			
			//Node border width
			DiscreteMapping borderWidthMap = new DiscreteMapping(defaultBorderWidth, ObjectMapping.NODE_MAPPING);
			borderWidthMap.setControllingAttributeName(SpecialAttributeNames.IS_SUBNET, null, false);
			
			borderWidthMap.putMapValue(true, 3.14159);
			borderWidthMap.putMapValue(false, defaultBorderWidth);
			
			Calculator nodeBorderWidthCalculator = new BasicCalculator("Node Border Width", borderWidthMap, VisualPropertyType.NODE_LINE_WIDTH);
			nac.setCalculator(nodeBorderWidthCalculator);
			
			//Node border colour
			DiscreteMapping borderColourMap = new DiscreteMapping(defaultBorderColour, ObjectMapping.NODE_MAPPING);
			borderColourMap.setControllingAttributeName(SpecialAttributeNames.COMPLEX_FROM, null, false);
			borderColourMap.putMapValue(Config.MULTICOMPLEX_COLOUR, new Color(Integer.parseInt(Config.MULTICOMPLEX_COLOUR, 16)));
			
			for(String c : Config.DISTINGUISHABLE_COLOURS){
				borderColourMap.putMapValue(c, new Color(Integer.parseInt(c, 16)));
			}
			
			Calculator nodeBorderColourCalculator = new BasicCalculator("Node Border Colour", borderColourMap, VisualPropertyType.NODE_BORDER_COLOR);
			nac.setCalculator(nodeBorderColourCalculator);
			
			//Node size
			PassThroughMapping nodeSizeMap = new PassThroughMapping(60.0, ObjectMapping.NODE_MAPPING);
			nodeSizeMap.setControllingAttributeName(SpecialAttributeNames.NODE_SIZE, null, false);
			
			Calculator nodeSizeCalculator = new BasicCalculator("Node Size", nodeSizeMap, VisualPropertyType.NODE_SIZE);
			nac.setCalculator(nodeSizeCalculator);
			
			// edge tool tip
			PassThroughMapping edgeToolTipLabel = new PassThroughMapping(new String(), "ID");
			edgeToolTipLabel.setControllingAttributeName("interaction", null, false);
			
			Calculator edgeToolTipCalculator = new BasicCalculator("edge tooltip calculator", edgeToolTipLabel, VisualPropertyType.EDGE_TOOLTIP);
			eac.setCalculator(edgeToolTipCalculator);
			
			//new edge colour algorithm
			DiscreteMapping edgeColour = new DiscreteMapping(defaultEdgeColour, ObjectMapping.EDGE_MAPPING);
			edgeColour.setControllingAttributeName(SpecialAttributeNames.EDGE_TYPE, null, false);

			edgeColour.putMapValue(Config.edgePhysicalKey, Config.physEdgeColour);
			edgeColour.putMapValue(Config.edgePosSignificanceKey, Config.posEdgeColour);
			edgeColour.putMapValue(Config.edgeNegSignificanceKey, Config.negEdgeColour);
                        edgeColour.putMapValue(Config.edgePosCorrelationKey, Config.posEdgeColour);
                        edgeColour.putMapValue(Config.edgeNegCorrelationKey, Config.negEdgeColour);
                        
			
			Calculator edgeColourCalculator = new BasicCalculator("edge colour calculator", edgeColour, VisualPropertyType.EDGE_COLOR);
			eac.setCalculator(edgeColourCalculator);

			// Edge Width
			DiscreteMapping edgeWidth = new DiscreteMapping(defaultEdgeWidth, ObjectMapping.EDGE_MAPPING);
			edgeWidth.setControllingAttributeName(SpecialAttributeNames.EDGE_TYPE, null, false);

			edgeWidth.putMapValue(Config.edgePhysicalKey, Config.physEdgeWidth);
			edgeWidth.putMapValue(Config.edgePosSignificanceKey, Config.posEdgeWidth);
			edgeWidth.putMapValue(Config.edgeNegSignificanceKey, Config.negEdgeWidth);
                        edgeWidth.putMapValue(Config.edgePosCorrelationKey, Config.correlationEdgeWidth);
                        edgeWidth.putMapValue(Config.edgeNegCorrelationKey, Config.correlationEdgeWidth);
			
			Calculator edgeWidthCalculator = new BasicCalculator("edge width calculator", edgeWidth, VisualPropertyType.EDGE_LINE_WIDTH);
			eac.setCalculator(edgeWidthCalculator);
                        
                        /*PEARSON CORRELATION STUFF*/
                        
                        // Edge Label color
			DiscreteMapping edgeLabelColor = new DiscreteMapping(Color.WHITE, ObjectMapping.EDGE_MAPPING);
			edgeLabelColor.setControllingAttributeName(SpecialAttributeNames.EDGE_TYPE, null, false);
                        edgeLabelColor.putMapValue(Config.edgePosCorrelationKey, Color.WHITE);
                        edgeLabelColor.putMapValue(Config.edgeNegCorrelationKey, Color.WHITE);
			Calculator edgeLabelColorCalculator = new BasicCalculator("edge label color calculator", edgeLabelColor, 
                                VisualPropertyType.EDGE_LABEL_COLOR);
			eac.setCalculator(edgeLabelColorCalculator);
                        
                        // Edge Label size
			DiscreteMapping edgeLabelSize = new DiscreteMapping(8.0, ObjectMapping.EDGE_MAPPING);
			edgeLabelSize.setControllingAttributeName(SpecialAttributeNames.EDGE_TYPE, null, false);
                        edgeLabelSize.putMapValue(Config.edgePosCorrelationKey, 8.0);
                        edgeLabelSize.putMapValue(Config.edgeNegCorrelationKey, 8.0);
			Calculator edgeLabelSizeCalculator = new BasicCalculator("edge label size calculator", edgeLabelSize, 
                                VisualPropertyType.EDGE_FONT_SIZE);
			eac.setCalculator(edgeLabelSizeCalculator);
                        
                        // Edge line style
			DiscreteMapping edgeLineStyle = new DiscreteMapping(LineStyle.EQUAL_DASH, ObjectMapping.EDGE_MAPPING);
			edgeLineStyle.setControllingAttributeName(SpecialAttributeNames.EDGE_TYPE, null, false);
                        edgeLineStyle.putMapValue(Config.edgePosCorrelationKey, LineStyle.EQUAL_DASH);
                        edgeLineStyle.putMapValue(Config.edgeNegCorrelationKey, LineStyle.EQUAL_DASH);
			Calculator edgeLineStyleCalculator = new BasicCalculator("edge label size calculator", edgeLineStyle, 
                                VisualPropertyType.EDGE_LINE_STYLE);
			eac.setCalculator(edgeLineStyleCalculator);
                        
                        // edge label
			PassThroughMapping edgeLable = new PassThroughMapping(new String(), "interaction");
			edgeLable.setControllingAttributeName(SpecialAttributeNames.EDGE_CORR, null, false);
			
			Calculator edgeLabelCalculator = new BasicCalculator("edge label calculator", edgeLable, VisualPropertyType.EDGE_LABEL);
			eac.setCalculator(edgeLabelCalculator);
                        
                        
			
			/*
			ContinuousMapping edgeWidth = new ContinuousMapping(1.61803, ObjectMapping.EDGE_MAPPING);
			edgeWidth.setControllingAttributeName(SpecialAttributeNames.WEIGHT, null, false);
			
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
            PassThroughMapping edgeLable = new PassThroughMapping(new String(), "interaction");
            edgeLable.setControllingAttributeName(SpecialAttributeNames.EDGE_TYPE, null, false);
            Calculator edgeLabelCalculator = new BasicCalculator("edge label calculator", edgeLable, VisualPropertyType.EDGE_LABEL);
            eac.setCalculator(edgeLabelCalculator);
            
            visualStyle.setEdgeAppearanceCalculator(eac);
            visualMappingManager.applyAppearances();
            
            // Change back
            edgeLable = new PassThroughMapping(new String(), "interaction");
            edgeLable.setControllingAttributeName(SpecialAttributeNames.EDGE_CORR, null, false);
            edgeLabelCalculator = new BasicCalculator("edge label calculator", edgeLable, VisualPropertyType.EDGE_LABEL);
            eac.setCalculator(edgeLabelCalculator);
            
            visualStyle.setEdgeAppearanceCalculator(eac);
            visualMappingManager.applyAppearances();
        }
}
