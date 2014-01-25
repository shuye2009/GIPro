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
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

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
                    ("Genetic interaction scores within complexes", "Score", "Frequency", within,
                    PlotOrientation.VERTICAL, false, true, false);
           
            ChartPanel withinChartPanel = new ChartPanel(withinChart);
            withinPanel.add(withinChartPanel);

            HistogramDataset between = new HistogramDataset();
            between.setType(HistogramType.RELATIVE_FREQUENCY);
            between.addSeries("Histogram",valuesBetween,numberBins);
            final JFreeChart betweenChart = ChartFactory.createHistogram
                    ("Genetic interaction scores between complexes", "Score", "Frequency",
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
                String withinPath = "";
                String betweenPath = "";
                try{
                    int i;
                    boolean done = false;
                    while(!done){
                    File dir = HelperMethods.showExportDialog("Choose save file name");
                    if(dir == null) return;

                    String fileName = dir.getName();
                    String basePath = dir.getParent() + File.separator;
                    if(fileName.contains(".")) 
                        basePath = basePath + fileName.substring(0, fileName.lastIndexOf("."));
                    else 
                        basePath = basePath + fileName;

                    withinPath = basePath +"_histogram_within.png";
                    betweenPath = basePath +"_histogram_between.png";

                    String conf = "<ul>";
                    if(withinButton.isSelected()) conf = conf + "<li>"+withinPath+"</li>";
                    if(betweenButton.isSelected()) conf = conf + "<li>"+betweenPath+"</li>";
                    conf = conf + "</ul>";

                    i = JOptionPane.showConfirmDialog(null, "<html>File(s) will be saved as<br></br>"
                            +conf+"<br></br>Do you agree?");

                    done = (i == JOptionPane.YES_OPTION);
                    }
                    
                    if(withinButton.isSelected()){
                        ChartUtilities.saveChartAsPNG(new File(withinPath), withinChart, 1200, 900);
                        List<String> paths = new ArrayList(); paths.add(withinPath);
                        HelperMethods.showSaveSuccess(paths);
                    }else{
                        ChartUtilities.saveChartAsPNG(new File(betweenPath), betweenChart, 1200, 900);
                        List<String> paths = new ArrayList(); paths.add(betweenPath);
                        HelperMethods.showSaveSuccess(paths);
                    }
                    
                }catch(Exception e){
                    JOptionPane.showMessageDialog(null, "Oops! An error occured while trying to save"
                        + " your files. Please contact the authors", "Error", 
                        JOptionPane.ERROR_MESSAGE);
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