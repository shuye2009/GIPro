package plugin;

/**
 * @author YH
 * 
 * Creates a histogram using JFreeChart
 * 
 */

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.statistics.HistogramType;

public class HistogramGui{
	
	private static JPanel mainPanel;
	private static CardLayout graphPanel;
	/**
         * Creates a histogram using JFreeChart
         * @param valueWithinD Double array of scores for within genetic interactions
         * @param valueBetweenD Double array of scores for between genetic interactions
         * @param numberBins Number of bins
         */
	public static void showHistogram(Double[] valueWithinD, Double[] valueBetweenD, int numberBins){		
            double[] valuesWithin = new double[valueWithinD.length];
            for(int x = 0; x < valuesWithin.length; x++){
                valuesWithin[x] = valueWithinD[x].doubleValue();
            }
            
            double[] valuesBetween = new double[valueBetweenD.length];
            for(int y = 0; y < valuesBetween.length; y++){
                valuesBetween[y] = valueBetweenD[y].doubleValue();
            }

            JFrame histogram = new JFrame("Histogram");
            JPanel withinPanel = new JPanel();
            JPanel betweenPanel = new JPanel();

            HistogramDataset within = new HistogramDataset();
            within.setType(HistogramType.RELATIVE_FREQUENCY);
            within.addSeries("Histogram",valuesWithin,numberBins);
            final JFreeChart withinChart = ChartFactory.createHistogram
                    ("Within Complexes", "Score", "Count", within,
                    PlotOrientation.VERTICAL, false, true, false);
            
            
            ChartPanel withinChartPanel = new ChartPanel(withinChart);
            withinPanel.add(withinChartPanel);

            HistogramDataset between = new HistogramDataset();
            between.setType(HistogramType.RELATIVE_FREQUENCY);
            between.addSeries("Histogram",valuesBetween,numberBins);
            final JFreeChart betweenChart = ChartFactory.createHistogram
                    ("Between Complexes", "Score", "Count",
                    between, PlotOrientation.VERTICAL, false, true, false);
            
            ChartPanel betweenChartPanel = new ChartPanel(betweenChart);
            betweenPanel.add(betweenChartPanel);		

            mainPanel = new JPanel();
            graphPanel = new CardLayout();
            mainPanel.setLayout(graphPanel);

            mainPanel.add(withinPanel, "within");
            mainPanel.add(betweenPanel, "between");
            histogram.add(mainPanel, BorderLayout.CENTER);

            final JRadioButton withinButton = new JRadioButton("Within Complex");
            withinButton.setBackground(Color.white);
            withinButton.setSelected(true);
            final JRadioButton betweenButton = new JRadioButton("Between Complex");
            betweenButton.setBackground(Color.white);

            ButtonGroup histogramSelectionButtons = new ButtonGroup();

            histogramSelectionButtons.add(withinButton);
            histogramSelectionButtons.add(betweenButton);

            JPanel selectionButtonPanel = new JPanel();
            selectionButtonPanel.setLayout(new BoxLayout(selectionButtonPanel, BoxLayout.Y_AXIS));
            selectionButtonPanel.setBackground(Color.white);
            selectionButtonPanel.add(Box.createRigidArea(new Dimension(0,100)));
            selectionButtonPanel.add(withinButton);
            selectionButtonPanel.add(betweenButton);
            
            //sep
            //selectionButtonPanel.add(new JSeparator());
            selectionButtonPanel.add(Box.createRigidArea(new Dimension(0,10)));
            
            JButton export = new JButton("Export to image");
            export.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                File f = HelperMethods.showExportDialog("Choose save directory");
                String path = "";
                try{
                    if(withinButton.isSelected()){
                        path = f.getAbsoluteFile()+File.separator+"Within_Histogram_ICTools.png";
                        ChartUtilities.saveChartAsPNG(new File(path), withinChart, 1600, 1000);
                        List<String> paths = new ArrayList(); paths.add(path);
                        HelperMethods.showSaveSuccess(paths);
                    }else{
                        path = f.getAbsoluteFile()+File.separator+"Between_Histogram_ICTools.png";
                        ChartUtilities.saveChartAsPNG(new File(path), betweenChart, 1600, 1000);
                        List<String> paths = new ArrayList(); paths.add(path);
                        HelperMethods.showSaveSuccess(paths);
                    }
                    
                }catch(Exception e){
                    JOptionPane.showMessageDialog(null, "Unable to save file", "Error", JOptionPane.ERROR_MESSAGE);
                }
                
            }
            });
            selectionButtonPanel.add(export);
            

            withinButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    graphPanel.first(mainPanel);
                }
            });

            betweenButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    graphPanel.last(mainPanel);
                }
            });

            histogram.setLocation(HelperMethods.getScreenWidth()/4, HelperMethods.getScreenHeight()/4);
            histogram.setBackground(Color.white);
            histogram.add(selectionButtonPanel, BorderLayout.EAST);
            histogram.pack();
            histogram.setVisible(true);
            histogram.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            histogram.setResizable(false);
	}
}