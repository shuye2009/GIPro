/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package guiICTools;

import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import plugin.Complex;
import plugin.Gene;
import plugin.RootNetwork;
import plugin.SubEdge;

/**
 *
 * @author omarwagih
 */
public class UpdateParamsPanel extends JDialog {
    private static MyWizardPanelDataStorage rebuilt_mwpds;
    private static JButton APPLY;
    
    private InputActionListener ial;
    
    private JPanel visualCutoffBox;
    private JLabel solidPosCutoffLabel, solidNegCutoffLabel;
    private static JLabel solidPositiveCutoffValue, solidNegativeCutoffValue;
    
    //CUTOFF PANEL VARIABLES
    private static JRadioButton pvalCutoffButton, customCutoffButton, percentCutoffButton;
    private static JLabel pvalRightTailCutoffLabel,pvalLeftTailCutoffLabel, customPosCutoffLabel, 
            customNegCutoffLabel, leftTailPercentileLabel, rightTailPercentileLabel;
    private static JTextField pvalRightTailCutoffField, pvalLeftTailCutoffField, 
            leftTailPercentileField,rightTailPercentileField, customPositiveCutoffField, customNegativeCutoffField;
    
    //ENRICHMENT PANEL VARIABLES
    private static JTextField FDRField, numTrialsField;
    private static JLabel fdrLabel, numberOfTrialsLabel;
    private static JRadioButton useFET, useSimulations;
            //, noEnrichmentBtn;
    private static JCheckBox trialForEachComplex;
    
    //Radiobutton and textfield data previous populated (in input)
    Double prevPosCutoff, prevNegCutoff, prevFdr, prevPvalLeftTail, prevPvalRightTail;
    Integer prevLeftTailPercentile, prevRightTailPercentile, prevNumberOfTrials;
    Boolean prevUsePvalCutoffs, prevUsePercentileCutoffs, prevUseFisher,
            prevTrialForEachComplex, prevUseSimulations;
    
    RootNetwork rn;
    
    public UpdateParamsPanel(MyWizardPanelDataStorage rebuilt_mwpds, RootNetwork rn,
            Boolean usePvalCutoffs, Double pvalLeftTail, Double pvalRightTail,
            Boolean usePercentileCutoffs, Integer leftTailPercentile, Integer rightTailPercentile,  
            Double posCutoff, Double negCutoff, Double fdr, 
            Boolean useFisher, Boolean useSimulations, Integer numberOfTrials, 
            Boolean trialForEachComplex){
        
        this.rn = rn;
        this.rebuilt_mwpds = rebuilt_mwpds;
        
        this.prevUsePvalCutoffs = usePvalCutoffs;
        this.prevPvalLeftTail = pvalLeftTail;
        this.prevPvalRightTail = pvalRightTail;
        
        this.prevUsePercentileCutoffs = usePercentileCutoffs;
        this.prevLeftTailPercentile = leftTailPercentile;
        this.prevRightTailPercentile = rightTailPercentile;
        
        this.prevPosCutoff = posCutoff; 
        this.prevNegCutoff = negCutoff;
        
        this.prevFdr = fdr;
        this.prevUseFisher = useFisher;
        this.prevUseSimulations = useSimulations;
        this.prevNumberOfTrials = numberOfTrials;
        this.prevTrialForEachComplex = trialForEachComplex;
        
        //constructor
        //this.setModal(true);
        this.setTitle("Update Parameters");
        initPanel();
        this.pack();
        this.setResizable(false);
        this.setVisible(true);
        this.setLocationRelativeTo(null);
    }
    
    public class ApplyTask implements Task{
        TaskMonitor tm;
        @Override
        public void run() {
            rn.setRootNetworksMWPDS(rebuilt_mwpds);
            rebuilt_mwpds.setRootNetwork(rn);

            Double pos = Double.parseDouble(customPositiveCutoffField.getText());
            Double neg = Double.parseDouble(customNegativeCutoffField.getText());
            int trials = 0;
            boolean trialForEach = false;
            if (useSimulations.isSelected()){
                    trials = Integer.parseInt(numTrialsField.getText());
                    trialForEach = trialForEachComplex.isSelected();
            }

            rebuilt_mwpds.populatePanelTwoData(
                pvalCutoffButton.isSelected(), 
                Double.parseDouble(pvalLeftTailCutoffField.getText()), 
                Double.parseDouble(pvalRightTailCutoffField.getText()),
                percentCutoffButton.isSelected(),
                Integer.parseInt(leftTailPercentileField.getText()),
                Integer.parseInt(rightTailPercentileField.getText()), 
                pos, neg, 
                Double.parseDouble(FDRField.getText()), 
                useFET.isSelected(), useSimulations.isSelected(), 
                trials, trialForEach);


            //Rebuild UNFILTERED (filtered for pval 0.05) genetic interaction from rootnetwork genes
            HashMap<String, Double> genePairsInRelationalFile = new HashMap();
            for(Gene g: rn.getGenes().values()){
                for(SubEdge se: g.getUnfilteredGeneNeighboursMap().values()){
                    String neighbourId = se.getGene().getGeneIdentifier();
                    String n1 = g.getGeneIdentifier()+"//"+neighbourId;
                    String n2 = neighbourId+"//"+g.getGeneIdentifier();
                    if(genePairsInRelationalFile.containsKey(n1) || genePairsInRelationalFile.containsKey(n2))
                        continue;
                    genePairsInRelationalFile.put(g.getGeneIdentifier()+"//"+neighbourId, se.getScore());
                }
            }
            rebuilt_mwpds.setGenePairsInRelationalFile(genePairsInRelationalFile);
            //Done
            //Set complex positive/negative pvalues to null so the old ones are not used 
            for(Complex c: rn.getComplexes().values()){
                c.setNegPValue(null);
                c.setPosPValue(null);
            }

            rebuilt_mwpds.rebuildComputations();

            /*
             * SET UPDATED CUTOFFS IN ROOTNETWORK 
             * SO WHEN PARAMETERS IS CLICKED AGAIN, THE NEW VALUES SHOW UP, 
             * NOT THE INITIAL BEGNING VALUES
             */
            rn.updateAllParameters( pvalCutoffButton.isSelected(), Double.parseDouble(pvalLeftTailCutoffField.getText()),
                        Double.parseDouble(pvalRightTailCutoffField.getText()),
                    percentCutoffButton.isSelected(), Integer.parseInt(leftTailPercentileField.getText()),
                        Integer.parseInt(rightTailPercentileField.getText()), 
                        Double.parseDouble(customNegativeCutoffField.getText()), 
                        Double.parseDouble(customPositiveCutoffField.getText()),
                        Double.parseDouble(FDRField.getText()), 
                    useFET.isSelected(), useSimulations.isSelected(), trials, trialForEach);

            //Close the on the fly panel
            UpdateParamsPanel.this.dispose();
        }

        @Override
        public void halt() {
            //Do nothing
        }

        @Override
        public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
            this.tm = tm;
        }

        @Override
        public String getTitle() {
            return "Regenerating complex network...";
        }
        
    }
    
    public void initPanel(){
        ial = new InputActionListener();
        APPLY = new JButton("Apply changes");
        APPLY.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                int i = JOptionPane.showConfirmDialog(null, "All current networks will be destroyed. "
                        + "Are you sure?", "", JOptionPane.YES_NO_OPTION);
                
                if(i == JOptionPane.CANCEL_OPTION || i == JOptionPane.NO_OPTION){
                    return;
                }
                
                ApplyTask task = new ApplyTask();
                // Configure JTask Dialog Pop-Up Box
                JTaskConfig jTaskConfig = new JTaskConfig();
                //jTaskConfig.setOwner(Cytoscape.getDesktop());
                jTaskConfig.displayTimeElapsed(true);
                jTaskConfig.displayCloseButton(false);

                jTaskConfig.displayCancelButton(false);

                jTaskConfig.displayStatus(true);
                jTaskConfig.setAutoDispose(true);
                
                //Execute in new thread
                TaskManager.executeTask(task, jTaskConfig);
            }
        });
        
        Font titledBorderFont = new Font("Helvetica", Font.BOLD,  16);
        
        JPanel cutoffParameterPanel = new JPanel();
        cutoffParameterPanel.setLayout(new BoxLayout(cutoffParameterPanel, BoxLayout.Y_AXIS));
        TitledBorder cutoffPanelBorder = BorderFactory.createTitledBorder("Interaction score cutoff parameters");
        cutoffPanelBorder.setTitleFont(titledBorderFont);
        cutoffParameterPanel.setBorder(cutoffPanelBorder);
        ButtonGroup cutoffButtonGroup = new ButtonGroup();
        
        //Visual cutoff box - updated through updateCutoff()
        visualCutoffBox = new JPanel();
        GridBagConstraints gridBagConstraints;

        solidPosCutoffLabel = new JLabel();
        solidNegCutoffLabel = new JLabel();
        solidPositiveCutoffValue = new JLabel();
        solidNegativeCutoffValue = new JLabel();
        
        visualCutoffBox.setLayout(new java.awt.GridBagLayout());

        solidPosCutoffLabel.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        solidPosCutoffLabel.setText("Positive cutoff: ");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        visualCutoffBox.add(solidPosCutoffLabel, gridBagConstraints);

        solidPositiveCutoffValue.setText("0.0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 9;
        visualCutoffBox.add(solidPositiveCutoffValue, gridBagConstraints);
        
        solidNegCutoffLabel.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        solidNegCutoffLabel.setText("Negative cutoff: ");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.LINE_END;
        visualCutoffBox.add(solidNegCutoffLabel, gridBagConstraints);

        solidNegativeCutoffValue.setText("0.0");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 9;
        visualCutoffBox.add(solidNegativeCutoffValue, gridBagConstraints);
        
        //======================================
        //Everything else - REST OF CUTOFF PANEL
        JPanel subCutoffPanel = new JPanel();
        
        java.awt.GridBagConstraints constraintsCutoff;

        pvalCutoffButton = new javax.swing.JRadioButton();
        pvalRightTailCutoffField = new javax.swing.JTextField();
        pvalRightTailCutoffLabel = new javax.swing.JLabel();
        pvalLeftTailCutoffField = new javax.swing.JTextField();
        pvalLeftTailCutoffLabel = new javax.swing.JLabel();
        customCutoffButton = new javax.swing.JRadioButton();
        customPosCutoffLabel = new javax.swing.JLabel();
        customNegCutoffLabel = new javax.swing.JLabel();
        customPositiveCutoffField = new javax.swing.JTextField();
        customNegativeCutoffField = new javax.swing.JTextField();
        percentCutoffButton = new javax.swing.JRadioButton();
        leftTailPercentileLabel = new javax.swing.JLabel();
        leftTailPercentileField = new javax.swing.JTextField();
        rightTailPercentileLabel = new javax.swing.JLabel();
        rightTailPercentileField = new javax.swing.JTextField();

        subCutoffPanel.setLayout(new java.awt.GridBagLayout());
        
        /**
         * PVALUE SECTION
         */
        pvalCutoffButton.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        pvalCutoffButton.setText("P-value based cutoff (recommended)");
        
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 1;
        constraintsCutoff.gridwidth = 3;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_START;
        pvalCutoffButton.addActionListener(ial);
        pvalCutoffButton.setActionCommand("use_solid_cutoff");
        cutoffButtonGroup.add(pvalCutoffButton);
        subCutoffPanel.add(pvalCutoffButton, constraintsCutoff);
        pvalCutoffButton.setSelected(prevUsePvalCutoffs);

        
        pvalLeftTailCutoffLabel.setText("Left tail p-value:");
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 2;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_END;
        subCutoffPanel.add(pvalLeftTailCutoffLabel, constraintsCutoff);
        
        pvalLeftTailCutoffField.setColumns(5);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 1;
        constraintsCutoff.gridy = 2;
        constraintsCutoff.gridwidth = 2;
        constraintsCutoff.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pvalLeftTailCutoffField.setText(prevPvalLeftTail + "");
        pvalLeftTailCutoffField.setEditable(true);
        pvalLeftTailCutoffField.getDocument().addDocumentListener(validation);
        pvalLeftTailCutoffField.getDocument().addDocumentListener(pvalueCutoffUpdateDocumentListener);
        subCutoffPanel.add(pvalLeftTailCutoffField, constraintsCutoff);
        
        pvalRightTailCutoffLabel.setText("Right tail p-value:");
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 3;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_END;
        subCutoffPanel.add(pvalRightTailCutoffLabel, constraintsCutoff);
        
        pvalRightTailCutoffField.setColumns(5);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 1;
        constraintsCutoff.gridy = 3;
        constraintsCutoff.gridwidth = 2;
        constraintsCutoff.fill = java.awt.GridBagConstraints.HORIZONTAL;
        pvalRightTailCutoffField.setText(prevPvalRightTail + "");
        pvalRightTailCutoffField.getDocument().addDocumentListener(validation);
        pvalRightTailCutoffField.getDocument().addDocumentListener(pvalueCutoffUpdateDocumentListener);
        subCutoffPanel.add(pvalRightTailCutoffField, constraintsCutoff);
        
        
        /**
         * PERCENTILE CUTOFF SECTION
         */
        
        percentCutoffButton.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        percentCutoffButton.setText("Percentile-based cutoff");
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 4;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_START;
        percentCutoffButton.addActionListener(ial);
        percentCutoffButton.setActionCommand("use_percentile_cutoff");
        cutoffButtonGroup.add(percentCutoffButton);
        subCutoffPanel.add(percentCutoffButton, constraintsCutoff);
        percentCutoffButton.setSelected(prevUsePercentileCutoffs);
        
        leftTailPercentileLabel.setText("Left tail percentile:");
        leftTailPercentileLabel.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 5;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_END;
        subCutoffPanel.add(leftTailPercentileLabel, constraintsCutoff);

        leftTailPercentileField.setColumns(5);
        leftTailPercentileField.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 1;
        constraintsCutoff.gridy = 5;
        constraintsCutoff.gridwidth = 2;
        constraintsCutoff.fill = java.awt.GridBagConstraints.HORIZONTAL;
        leftTailPercentileField.setText(prevLeftTailPercentile+"");
        leftTailPercentileField.getDocument().addDocumentListener(validation);
        leftTailPercentileField.getDocument().addDocumentListener(percentileCutoffUpdateDocumentListener);
        subCutoffPanel.add(leftTailPercentileField, constraintsCutoff);
        
        
        rightTailPercentileLabel.setText("Right tail percentile:");
        rightTailPercentileLabel.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 6;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_END;
        subCutoffPanel.add(rightTailPercentileLabel, constraintsCutoff);

        rightTailPercentileField.setColumns(5);
        rightTailPercentileField.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 1;
        constraintsCutoff.gridy = 6;
        constraintsCutoff.gridwidth = 2;
        constraintsCutoff.fill = java.awt.GridBagConstraints.HORIZONTAL;
        rightTailPercentileField.setText(prevRightTailPercentile+"");
        rightTailPercentileField.getDocument().addDocumentListener(validation);
        rightTailPercentileField.getDocument().addDocumentListener(percentileCutoffUpdateDocumentListener);
        subCutoffPanel.add(rightTailPercentileField, constraintsCutoff);
        
        /**
         * CUSTOM SCORE CUTOFF SECTION
         */
        customCutoffButton.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        customCutoffButton.setText("Custom score cutoff");
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 7;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_START;
        customCutoffButton.addActionListener(ial);
        customCutoffButton.setActionCommand("use_custom_cutoff");
        cutoffButtonGroup.add(customCutoffButton);
        subCutoffPanel.add(customCutoffButton, constraintsCutoff);
        customCutoffButton.setSelected(!prevUsePercentileCutoffs && !prevUsePvalCutoffs);
        
        customNegCutoffLabel.setText("Negative cutoff:");
        customNegCutoffLabel.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 8;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_END;
        subCutoffPanel.add(customNegCutoffLabel, constraintsCutoff);

        customNegativeCutoffField.setColumns(5);
        customNegativeCutoffField.setText(prevNegCutoff+"");
        customNegativeCutoffField.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 1;
        constraintsCutoff.gridy = 8;
        constraintsCutoff.gridwidth = 2;
        constraintsCutoff.fill = java.awt.GridBagConstraints.HORIZONTAL;
        customNegativeCutoffField.getDocument().addDocumentListener(validation);
        customNegativeCutoffField.getDocument().addDocumentListener(customCutoffUpdateDocumentListener);
        subCutoffPanel.add(customNegativeCutoffField, constraintsCutoff);
        
        customPosCutoffLabel.setText("Positive cutoff:");
        customPosCutoffLabel.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 9;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_END;
        subCutoffPanel.add(customPosCutoffLabel, constraintsCutoff);

        customPositiveCutoffField.setColumns(5);
        customPositiveCutoffField.setText(prevPosCutoff+"");
        customPositiveCutoffField.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 1;
        constraintsCutoff.gridy = 9;
        constraintsCutoff.gridwidth = 2;
        constraintsCutoff.fill = java.awt.GridBagConstraints.HORIZONTAL;
        customPositiveCutoffField.getDocument().addDocumentListener(validation);
        customPositiveCutoffField.getDocument().addDocumentListener(customCutoffUpdateDocumentListener);
        subCutoffPanel.add(customPositiveCutoffField, constraintsCutoff);
        
        if(prevUsePvalCutoffs){
            System.out.println("prev use pval cutoff");
            pvalLeftTailCutoffField.setEnabled(true);
            pvalRightTailCutoffField.setEnabled(true);
            
            customPositiveCutoffField.setEnabled(false);
            customNegativeCutoffField.setEnabled(false);
            
            leftTailPercentileField.setEnabled(false);
            rightTailPercentileField.setEnabled(false);
        }else if(prevUsePercentileCutoffs){
            pvalLeftTailCutoffField.setEnabled(false);
            pvalRightTailCutoffField.setEnabled(false);
            
            customPositiveCutoffField.setEnabled(false);
            customNegativeCutoffField.setEnabled(false);
            
            leftTailPercentileField.setEnabled(true);
            rightTailPercentileField.setEnabled(true);
        }else{
            pvalLeftTailCutoffField.setEnabled(false);
            pvalRightTailCutoffField.setEnabled(false);
            
            customPositiveCutoffField.setEnabled(true);
            customNegativeCutoffField.setEnabled(true);
            
            leftTailPercentileField.setEnabled(false);
            rightTailPercentileField.setEnabled(false);
        }
        
        
        //Need to add this to full frame
        cutoffParameterPanel.add(subCutoffPanel);
        
        // ===================== enrichmentParameterPanel ==================================
        JPanel enrichmentParameterPanel = new JPanel();
        //Titled border
        TitledBorder enrichmentPanelBorder = BorderFactory.createTitledBorder("Enrichment analysis");
        enrichmentPanelBorder.setTitleFont(titledBorderFont);
        enrichmentParameterPanel.setBorder(enrichmentPanelBorder);
        
        ButtonGroup enrichmentButtons = new ButtonGroup();
        
        JPanel subEnrichmentPanel = new JPanel();
        GridBagConstraints constraintsEnrichment;

        useFET = new javax.swing.JRadioButton();
        fdrLabel = new javax.swing.JLabel();
        FDRField = new javax.swing.JTextField();
        useSimulations = new javax.swing.JRadioButton();
        //noEnrichmentBtn = new javax.swing.JRadioButton();
        numberOfTrialsLabel = new javax.swing.JLabel();
        numTrialsField = new javax.swing.JTextField();
        trialForEachComplex = new javax.swing.JCheckBox();

        subEnrichmentPanel.setLayout(new GridBagLayout());
        
        String fdrInfo = "<html>This FDR value is used to determine a p-value to determine if complexes<br>" +
        "and edges are enriched with positive, negative, or<br> " +
        "both types of interactions.";
        
        //Multiple testing panel
        JPanel multiTestPanel = new JPanel(new GridBagLayout());
        
        JLabel multipleTesting = new JLabel("Multiple testing correction");
        multipleTesting.setFont(new java.awt.Font("Lucida Grande", 1, 13));
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 0;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_START;
        subEnrichmentPanel.add(multipleTesting, constraintsEnrichment);
        
        fdrLabel.setText("False discovery rate:");
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 1;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_END;
        fdrLabel.setToolTipText(fdrInfo);
        subEnrichmentPanel.add(fdrLabel, constraintsEnrichment);
        
        
        FDRField.setColumns(5);
        FDRField.setText(prevFdr+"");
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 1;
        constraintsEnrichment.gridy = 1;
        constraintsEnrichment.fill = java.awt.GridBagConstraints.HORIZONTAL;
        FDRField.setToolTipText(fdrInfo);
        FDRField.getDocument().addDocumentListener(validation);
        subEnrichmentPanel.add(FDRField, constraintsEnrichment);
        
        
        //separator
        
        useFET.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        useFET.setText("Fisher exact test (recommended)");
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 2;
        constraintsEnrichment.gridwidth = 2;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_START;
        useFET.addActionListener(ial);
        useFET.setActionCommand("use_fet");
        enrichmentButtons.add(useFET);
        subEnrichmentPanel.add(useFET, constraintsEnrichment);
        useFET.setSelected(prevUseFisher);
        
        useSimulations.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        useSimulations.setText("Random draws");
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 3;
        constraintsEnrichment.gridwidth = 2;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_START;
        useSimulations.addActionListener(ial);
        useSimulations.setActionCommand("use_simulations");
        
        enrichmentButtons.add(useSimulations);
        subEnrichmentPanel.add(useSimulations, constraintsEnrichment);
        useSimulations.setSelected(prevUseSimulations);
        
        String simulationInfo = "<html>The number of simulation trials that will be run<br>" +
        "for each complex/interacting pair of complexes.</html>";
        
        numberOfTrialsLabel.setText("Number of trials:");
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 4;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_END;
        
        numberOfTrialsLabel.setToolTipText(simulationInfo);
        subEnrichmentPanel.add(numberOfTrialsLabel, constraintsEnrichment);
        
        numTrialsField.setColumns(5);
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 1;
        constraintsEnrichment.gridy = 4;
        constraintsEnrichment.fill = java.awt.GridBagConstraints.HORIZONTAL;
        
        numTrialsField.setText(prevNumberOfTrials+"");
        numTrialsField.setToolTipText(simulationInfo);
        numTrialsField.getDocument().addDocumentListener(validation);
        subEnrichmentPanel.add(numTrialsField, constraintsEnrichment);
        
        String eachComplexInfo = "<html>Leave unchecked if you would like complexes with the same<br> " +
            "number of interactions to share a distribution. (Faster) <br><br>Recommendation:<br> " +
            "- <b>UNCHECKED</b> if using many trials (1000 is enough)<br>"+
            "- <b>CHECKED</b> if using small number of trials (<300).";
        
        
        trialForEachComplex.setText("Run trials for each complex");
        
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 5;
        constraintsEnrichment.gridwidth = 7;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.EAST;
        //constraintsEnrichment.fill = GridBagConstraints.HORIZONTAL;
        trialForEachComplex.setActionCommand("trial_for_each_complex");
        trialForEachComplex.addActionListener(ial);
        trialForEachComplex.setToolTipText(eachComplexInfo);
        subEnrichmentPanel.add(trialForEachComplex, constraintsEnrichment);
        
        trialForEachComplex.setSelected(prevTrialForEachComplex);//update from prev
        
//        noEnrichmentBtn.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
//        noEnrichmentBtn.setText("No enrichment");
//        constraintsEnrichment = new java.awt.GridBagConstraints();
//        constraintsEnrichment.gridx = 0;
//        constraintsEnrichment.gridy = 6;
//        constraintsEnrichment.gridwidth = 2;
//        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_START;
//        noEnrichmentBtn.addActionListener(ial);
//        noEnrichmentBtn.setActionCommand("no_enrichment");
//        
//        enrichmentButtons.add(noEnrichmentBtn);
//        subEnrichmentPanel.add(noEnrichmentBtn, constraintsEnrichment);
//        noEnrichmentBtn.setSelected(!prevUseFisher && !prevUseSimulations);
        
        if(prevUseFisher){
            numberOfTrialsLabel.setEnabled(false);
            numTrialsField.setEnabled(false);
            trialForEachComplex.setEnabled(false);
        }else if(prevUseSimulations){
            numberOfTrialsLabel.setEnabled(true);
            numTrialsField.setEnabled(true);
            trialForEachComplex.setEnabled(true);
        }else{//No enrichment
            numberOfTrialsLabel.setEnabled(false);
            numTrialsField.setEnabled(false);
            trialForEachComplex.setEnabled(false);
        }
        
        
        enrichmentParameterPanel.add(subEnrichmentPanel);
        //DONE ENRICHMENT PANEL - START PUTTING ALL TOGETHER
        
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        JPanel parameterGrid = new JPanel();
        parameterGrid.setLayout(new GridLayout(1,2));
        parameterGrid.add(cutoffParameterPanel);
        parameterGrid.add(enrichmentParameterPanel);
        mainPanel.add(parameterGrid);
        
        JButton CANCEL = new JButton("Cancel");
        CANCEL.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                UpdateParamsPanel.this.dispose();
            }
        });
        JPanel bottom = new JPanel();
        bottom.add(CANCEL); bottom.add(APPLY);
        
        mainPanel.add(bottom);
        
        this.getContentPane().add(mainPanel);
        //POPULATE PANEL TWO DATA IN MWPDS
        
    }
    
    DocumentListener validation = new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                APPLY.setEnabled(allNecessaryParameterFieldsValid());
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                APPLY.setEnabled(allNecessaryParameterFieldsValid());
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                APPLY.setEnabled(allNecessaryParameterFieldsValid());
            }
        };

    DocumentListener customCutoffUpdateDocumentListener = new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
            updateCustomCutoffs();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updateCustomCutoffs();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updateCustomCutoffs();	
        }
    };

    DocumentListener pvalueCutoffUpdateDocumentListener = new DocumentListener() {
        @Override
        public void removeUpdate(DocumentEvent e) {
            UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            updatePvalueCutoffs();
            UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getDefaultCursor());
        }
        @Override
        public void insertUpdate(DocumentEvent e) {
            UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            updatePvalueCutoffs();
            UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getDefaultCursor());
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            updatePvalueCutoffs();
            UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getDefaultCursor());
        }
    };

    DocumentListener percentileCutoffUpdateDocumentListener = new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                updatePercentileCutoffs();
                UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getDefaultCursor());
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                updatePercentileCutoffs();
                UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                updatePercentileCutoffs();
                UpdateParamsPanel.this.getContentPane().setCursor(Cursor.getDefaultCursor());
            }
        };
        
    private static void updateCustomCutoffs(){
        if (customCutoffButton.isSelected()){
            if (Input.isDouble(customPositiveCutoffField.getText()))
                setVisualCutoffs(Double.parseDouble(customPositiveCutoffField.getText()), null);
            if (Input.isDouble(customNegativeCutoffField.getText()))
                setVisualCutoffs(null, Double.parseDouble(customNegativeCutoffField.getText()));
        }
    }
    
    private static void updatePvalueCutoffs(){
        
        if (pvalCutoffButton.isSelected()){
            Double leftTailCutoff = null;
            Double rightTailCutoff = null;

            if(Input.isDouble(pvalLeftTailCutoffField.getText())) leftTailCutoff = Double.parseDouble(pvalLeftTailCutoffField.getText());
            if(Input.isDouble(pvalRightTailCutoffField.getText())) rightTailCutoff = Double.parseDouble(pvalRightTailCutoffField.getText());
            rebuilt_mwpds.recalculateGaussianCutoffs(leftTailCutoff, rightTailCutoff, customPositiveCutoffField, customNegativeCutoffField);
            
        }
    }
    
    private static void updatePercentileCutoffs(){
        if (percentCutoffButton.isSelected()){
            //wait cursor for long calculations
            leftTailPercentileField.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            rightTailPercentileField.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Integer leftTailCutoff = null;
            Integer rightTailCutoff = null;

            if(Input.isInteger(leftTailPercentileField.getText())) leftTailCutoff = Integer.parseInt(leftTailPercentileField.getText());
            if(Input.isInteger(rightTailPercentileField.getText())) rightTailCutoff = Integer.parseInt(rightTailPercentileField.getText());

            rebuilt_mwpds.calculateTwoTailPercentileCutoffs(leftTailCutoff,rightTailCutoff, customPositiveCutoffField, customNegativeCutoffField);
            
            //return to default cursor
            leftTailPercentileField.setCursor(Cursor.getDefaultCursor());
            rightTailPercentileField.setCursor(Cursor.getDefaultCursor());
        }
    }    
    
    private static void setVisualCutoffs (Double pos, Double neg){
        if (pos != null){
            rebuilt_mwpds.setCustomCutoffsCutoffs(pos, null);  
        }
        if (neg != null){
            rebuilt_mwpds.setCustomCutoffsCutoffs(null, neg); 
        }
    }
    
    private static boolean allNecessaryParameterFieldsValid(){
        boolean cutoffParamsValid = 
            (
                (
                pvalCutoffButton.isSelected() 
            && Input.isDouble(pvalLeftTailCutoffField.getText()) && Input.isDouble(pvalRightTailCutoffField.getText())  
            && Double.parseDouble(pvalLeftTailCutoffField.getText()) >= new Double(0)
                && Double.parseDouble(pvalLeftTailCutoffField.getText()) <= new Double(1)
            && Double.parseDouble(pvalRightTailCutoffField.getText()) >= new Double(0)
                && Double.parseDouble(pvalRightTailCutoffField.getText()) <= new Double(1)
                )
                
            || 
                (
                percentCutoffButton.isSelected() 
            && Input.isInteger(leftTailPercentileField.getText()) 
            && Input.isInteger(rightTailPercentileField.getText()) 
            && Integer.parseInt(leftTailPercentileField.getText()) >= 0 
            && Integer.parseInt(leftTailPercentileField.getText()) <= 100
            && Integer.parseInt(rightTailPercentileField.getText()) >= 0 
            && Integer.parseInt(rightTailPercentileField.getText()) <= 100
                )

            || 
                (
                (customCutoffButton.isSelected() 
            && Input.isDouble(customPositiveCutoffField.getText()) 
            && Input.isDouble(customNegativeCutoffField.getText()))
            && Double.parseDouble(solidPositiveCutoffValue.getText()) >= Double.parseDouble(solidNegativeCutoffValue.getText())
                )
            );

        boolean enrichmentParamsValid = 
            Input.isDouble(FDRField.getText()) 
            && Double.parseDouble(FDRField.getText()) >=0
            && (useFET.isSelected() || Input.isInteger(numTrialsField.getText()));
        
        return cutoffParamsValid && enrichmentParamsValid;
    }
    
    public static class InputActionListener implements ActionListener{

        public InputActionListener(){
            
        }
		
        @Override
        public void actionPerformed(ActionEvent e) {
            String actionCommand = e.getActionCommand();
            if(actionCommand == null){
                return;
            }
            else if(actionCommand.equals("use_solid_cutoff")){
                
                pvalLeftTailCutoffLabel.setEnabled(true);
                pvalLeftTailCutoffField.setEnabled(true);
                pvalRightTailCutoffLabel.setEnabled(true);
                pvalRightTailCutoffField.setEnabled(true);
                
                pvalRightTailCutoffLabel.setEnabled(true);
                pvalRightTailCutoffField.setEnabled(true);
                
                customPositiveCutoffField.setEnabled(false);
                customPosCutoffLabel.setEnabled(false);
                customNegativeCutoffField.setEnabled(false);
                customNegCutoffLabel.setEnabled(false);
                
                leftTailPercentileField.setEnabled(false);
                leftTailPercentileLabel.setEnabled(false);
                rightTailPercentileField.setEnabled(false);
                rightTailPercentileLabel.setEnabled(false);
                
                updatePvalueCutoffs();
            }
            else if(actionCommand.equals("use_custom_cutoff")){
                pvalLeftTailCutoffLabel.setEnabled(false);
                pvalLeftTailCutoffField.setEnabled(false);
                pvalRightTailCutoffLabel.setEnabled(false);
                pvalRightTailCutoffField.setEnabled(false);
                
                leftTailPercentileField.setEnabled(false);
                leftTailPercentileLabel.setEnabled(false);
                rightTailPercentileField.setEnabled(false);
                rightTailPercentileLabel.setEnabled(false);
                
                customPositiveCutoffField.setEnabled(true);
                customPosCutoffLabel.setEnabled(true);
                customNegativeCutoffField.setEnabled(true);
                customNegCutoffLabel.setEnabled(true);
                
                updateCustomCutoffs();
            }
            else if(actionCommand.equals("use_percentile_cutoff")){
                pvalLeftTailCutoffLabel.setEnabled(false);
                pvalLeftTailCutoffField.setEnabled(false);
                pvalRightTailCutoffLabel.setEnabled(false);
                pvalRightTailCutoffField.setEnabled(false);
                
                customPositiveCutoffField.setEnabled(false);
                customPosCutoffLabel.setEnabled(false);
                customNegativeCutoffField.setEnabled(false);
                customNegCutoffLabel.setEnabled(false);
                
                leftTailPercentileField.setEnabled(true);
                leftTailPercentileLabel.setEnabled(true);
                rightTailPercentileField.setEnabled(true);
                rightTailPercentileLabel.setEnabled(true);
                
                updatePercentileCutoffs();
            }
            else if(actionCommand.equals("use_fet")){
                numTrialsField.setEditable(false);
                trialForEachComplex.setEnabled(false);
                numberOfTrialsLabel.setEnabled(false);
                numTrialsField.setEnabled(false);
            }
            else if(actionCommand.equals("use_simulations")){
                numTrialsField.setEditable(true);
                
                trialForEachComplex.setEnabled(true);
                //FDRField.setEditable(false);
                numberOfTrialsLabel.setEnabled(true);
                numTrialsField.setEnabled(true);
            }
            else if(actionCommand.equals("no_enrichment")){
                trialForEachComplex.setEnabled(false);
                numTrialsField.setEditable(false);
                FDRField.setEditable(false);
                numberOfTrialsLabel.setEnabled(false);
                numTrialsField.setEnabled(false);
            }
            
            APPLY.setEnabled(allNecessaryParameterFieldsValid());
        }
	
    }//end actionListener class
}
