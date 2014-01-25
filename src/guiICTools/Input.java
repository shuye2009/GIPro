package guiICTools;

import cytoscape.Cytoscape;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import plugin.HelperMethods;
import plugin.UserManualLabel;
import wGUI.AboutDialog;
import wGUI.Wizard;
import wGUI.WizardPanel;

public class Input extends WizardPanel{
    
    private static MyWizardPanelDataStorage mwpds;
    InputActionListener ial;
    
    private JLabel relationalFileLabel, nameMapLabel, complexLabel, 
            physicalInteractionsLabel;
    private static JTextField relationalFilePath,
            complexFilePath, nameMapFilePath, physicalInteractionFilePath;
    
    private JTextArea consoleRedirection;
    private JScrollPane consolePane;
    private JPanel paramInputPanel, fileInput;
    private static JButton loadFiles, clearFiles, chooseNameMapFile, 
            choosePhysicalInteractionFile, chooseComplexFile, chooseRelationalFile;
    
    private JCheckBox relationFiltering;
    
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
    
    private JPanel visualCutoffBox;
    private static JLabel solidPosCutoffLabel, solidNegCutoffLabel;
    private static JLabel solidPositiveCutoffValue, solidNegativeCutoffValue;
    
    //END ENRICHMENT PANEL VARIABLES
    
    private static boolean filesLoaded;
    private final int DIRECTORY_PATH_LENGTH = 30;
    
    JButton exampleData;
    
    JPanel mainPanel;
    
    JFrame wizardFrame;
    /**
     * Constructor for Input
     * @param wizard 
     */
    public Input(final Wizard wizard){
        super(wizard);
        this.wizardFrame = wizard.getWizardFrame();
        
        filesLoaded = false;
        this.mwpds = new MyWizardPanelDataStorage();		
        this.setPanelDescriptorIdentifier(new String("Input"));

        mainPanel = new JPanel();
        this.setPanelComponent(mainPanel);
        
        Font titledBorderFont = new Font("Helvetica", Font.BOLD,  16);
        
        JPanel logoPanel = new JPanel();
        BoxLayout layout = new BoxLayout(logoPanel, BoxLayout.X_AXIS);
        logoPanel.setLayout(layout);
        
        URL url = getClass().getClassLoader().getResource("images/gipro_logo.png"); 
        ImageIcon logoImage = new ImageIcon(url);
        JLabel logoLabel = new JLabel(logoImage);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoPanel.add(logoLabel);
        
        
        
        // ===================================================================
        fileInput = new JPanel();
        fileInput.setLayout(new BoxLayout(fileInput, BoxLayout.Y_AXIS));
        //fileInput.setMaximumSize(new Dimension(700, 130));
        TitledBorder fileInputBorder = BorderFactory.createTitledBorder("1. File input");
        fileInputBorder.setTitleFont(titledBorderFont);
        fileInput.setBorder(fileInputBorder);

        ial = new InputActionListener(this);

        //init stuff
        java.awt.GridBagConstraints fileInputConstraints;
        
        relationalFileLabel = new JLabel();
        nameMapLabel = new JLabel();
        nameMapFilePath = new JTextField();
        chooseRelationalFile = new JButton();
        choosePhysicalInteractionFile = new JButton();
        physicalInteractionsLabel = new JLabel();
        relationalFilePath = new JTextField();
        physicalInteractionFilePath = new JTextField();
        chooseNameMapFile = new JButton();
        complexLabel = new JLabel();
        complexFilePath = new JTextField();
        chooseComplexFile = new JButton();
        loadFiles = new JButton();
        clearFiles = new JButton();
        relationFiltering = new JCheckBox();

        fileInput.setLayout(new GridBagLayout());
        
        clearFiles.setEnabled(true);
        //=======================build relationalFile=========================
        {
        
        String tooltipText = 
            "<html><h2>Functional Relations File</h2><hr>" +
            "Format: <B>orf1Name  orf2Name  score</B> or <B>orf1Name  orf2Name  score  pvalue</B><br><br>" + 
            "File consists of many lines of exactly 3 or 4 tokens <B>delimited by white space with no<br>" +
            "headers on first line.</B>  The score column can contain decimal values as well as negatives.<br>" +
            "It can also contain numbers in scientific notation.<br>" +
            "i.e. 7.654321E-4 which means 7.654321 * 10^-4<br><br>"+

            "The optional fourth column can contain the p-value for that interaction. In this case,<br>" +
            "the simulations are run including only those interactions with p-value less than 0.05 <br><br>"+
            "<B>Make sure all rows have the same number of tokens delimited by white space.</B><br><br>"+

            "Sample File:<br><br>"+
            "YBL075C	YDL133W	0.008711269555458685<br>"+
            "YER054C	YFL020C	-0.02264879462726829<br>"+
            "YER054C	YFL019C	0.0892100128401335<br>"+
            "YBL075C	YDR516C	-9.614055491178168E-4<br>br>"+

            "Sample File with p-values:<br><br>"+

            "YBL075C	YDL133W	0.00871126 0.02<br>"+
            "YER054C	YFL020C	-0.0226487 0.34<br>"+
            "YER054C	YFL019C	0.08921001 0.04<br>"+
            "YBL075C	YDR516C	-9.61405E-4 0.001</html>";
        
        //Relation label
        relationalFileLabel.setText("<html><b>Genetic interactions file:</b></html>");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        fileInputConstraints.gridwidth = 2;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        relationalFileLabel.setToolTipText(tooltipText);
        fileInput.add(relationalFileLabel, fileInputConstraints);
        
        //Relation file path
        relationalFilePath.setColumns(DIRECTORY_PATH_LENGTH);
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 2;
        fileInputConstraints.gridy = 0;
        fileInputConstraints.gridwidth = 5;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        relationalFilePath.setToolTipText(tooltipText);
        relationalFilePath.setEditable(false);
        fileInput.add(relationalFilePath, fileInputConstraints);
        
        //Browse relation file button
        chooseRelationalFile.setText("Browse");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 7;
        fileInputConstraints.gridy = 0;
        chooseRelationalFile.setToolTipText(tooltipText);
        chooseRelationalFile.setActionCommand("choose_file_relational");
        chooseRelationalFile.addActionListener(ial);
        fileInput.add(chooseRelationalFile, fileInputConstraints);
        
        }
        //====================================================================

        

        //========================building complex file=======================
        {
        String tooltipText = "<html><h2>Protein complex file</h2><hr>"+
            "Format: <B>orfName complexName</B><br><br>"+

            "File consists of many lines with exactly 2 tokens <B>delimited by tabs</B>. The file has <B>exactly<br>" +
            "1 line of headers</B>, the contents of the header do not matter, and they contain any<br>" +
            "number of tokens. complexName and orfName tokens can contain spaces since the file is<br>" +
            "tab-delimited.<br><br>"+

            "<B>Make sure all rows have exactly 2 tokens delimited by tabs.</B><br><br>"+

            "Sample File:<br><br>"+
            "CID	ORF<br>"+
            "TRAPP complex	YKR068C<br>"+
            "Ds1lp	YGL098W<br><br>"+

            "That is:<br>"+
            "CID\\tORF$<br>"+
            "TRAPP complex\\tYKR068C$<br>"+
            "Ds1lp\\tYGL098W$</html>";
        
        
        complexLabel.setText("<html><b>Protein complex file:</b></html>");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 0;
        fileInputConstraints.gridy = 1;
        fileInputConstraints.gridwidth = 2;
        fileInputConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        complexLabel.setToolTipText(tooltipText);
        fileInput.add(complexLabel, fileInputConstraints);
        
        complexFilePath.setColumns(DIRECTORY_PATH_LENGTH);
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 2;
        fileInputConstraints.gridy = 1;
        fileInputConstraints.gridwidth = 5;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        complexFilePath.setEditable(false);
        complexFilePath.setToolTipText(tooltipText);
        fileInput.add(complexFilePath, fileInputConstraints);
        
        chooseComplexFile.setText("Browse");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 7;
        fileInputConstraints.gridy = 1;
        chooseComplexFile.setActionCommand("choose_file_complex");
        chooseComplexFile.addActionListener(ial);
        chooseComplexFile.setToolTipText(tooltipText);
        fileInput.add(chooseComplexFile, fileInputConstraints);
        }
        //====================================================================


        //===============building physical interactions file==================
        {
        
        String tooltipText = 
            "<html><h2>Physical Interactions File</h2><hr>" +
            "Format: <B>orf1Name  orf2Name  score</B><br><br>" + 
            "headers on first line.</B>  The score column can contain decimal values as well as negatives.<br>" +
            "File consists of many lines of exactly 3 tokens <B>delimited by white space and containing<br>" +
            "no headers. </B>The score column can contain negatives and/or decimal values, although<br>" +
            "the physical interactions file shouldn't contain negative scores.<br><br>"+

            "<B>Make sure all rows have exactly 3 tokens delimited by white space.</B><br><br>"+

            "Sample File:<br><br>"+
            "YBL075C	YDL133W	0.03141<br>"+
            "YER054C	YFL020C	0.01618<br>"+
            "YER054C	YFL019C	0.01337</html>";
        
        //Physical interaction label for file
        physicalInteractionsLabel.setText("<html><b>Physical interactions file:</b></html>");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 0;
        fileInputConstraints.gridy = 2;
        fileInputConstraints.gridwidth = 2;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        fileInputConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        physicalInteractionsLabel.setToolTipText(tooltipText);
        fileInput.add(physicalInteractionsLabel, fileInputConstraints);
        
        //Text field for file path
        physicalInteractionFilePath.setColumns(DIRECTORY_PATH_LENGTH);
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 2;
        fileInputConstraints.gridy = 2;
        fileInputConstraints.gridwidth = 5;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        physicalInteractionFilePath.setEditable(false);
        physicalInteractionFilePath.setToolTipText(tooltipText);
        fileInput.add(physicalInteractionFilePath, fileInputConstraints);
        
        //Browse physical file button
        choosePhysicalInteractionFile.setText("Browse");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 7;
        fileInputConstraints.gridy = 2;
        choosePhysicalInteractionFile.setActionCommand("choose_file_physical_interaction");
        choosePhysicalInteractionFile.addActionListener(ial);
        choosePhysicalInteractionFile.setToolTipText(tooltipText);
        fileInput.add(choosePhysicalInteractionFile, fileInputConstraints);
        }
        
        //===========================building nameMapFile======================
        {
        String tooltipText = 
            "<html><h2>Name Map File</h2><hr>"+
            "Format: <B>orfName	proteinName</B><br><br>"+
            "File consists of many lines with exactly 2 tokens <B>delimited by tabs</B>.  The file has <B>exactly<br>" +
            "1 line of headers</B>, the headers could contain any number of tokens and the contents of<br>" +
            "the header does not matter.<br>" +
            "orfName or proteinName tokens can contain spaces, since the file is tab-delimited.<br><br>"+

            "<B>Make sure all rows have exactly 2 tokens delimited by tabs.</B><br><br>"+

            "Sample File:<br><br>"+
            "ORF	GENE<br>"+
            "YHR055C	CUP1-2<br>"+
            "YPR161C	SGV1<br>"+
            "YOL138C	YOL138C<br><br>"+

            "That is:<br>"+
            "ORF\\tGENE$<br>"+
            "YHR055C\\tCUP1-2$<br>"+
            "YPR161C\\tSGV1$<br>"+
            "YOL138C\\tYOL138C$</html>";
        
        //Name map label
        nameMapLabel.setText("<html><b>Name map file </b> (optional):</html>");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 0;
        fileInputConstraints.gridy = 3;
        fileInputConstraints.gridwidth = 2;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        fileInputConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
        nameMapLabel.setToolTipText(tooltipText);
        fileInput.add(nameMapLabel, fileInputConstraints);
        
        //Name map path textfield
        nameMapFilePath.setColumns(DIRECTORY_PATH_LENGTH);
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 2;
        fileInputConstraints.gridy = 3;
        fileInputConstraints.gridwidth = 5;
        fileInputConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        nameMapFilePath.setEditable(false);
        nameMapFilePath.setToolTipText(tooltipText);
        fileInput.add(nameMapFilePath, fileInputConstraints);
        
        //Browse name map file
        chooseNameMapFile.setText("Browse");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 7;
        fileInputConstraints.gridy = 3;
        chooseNameMapFile.setActionCommand("choose_file_name_map");
        chooseNameMapFile.addActionListener(ial);
        chooseNameMapFile.setToolTipText(tooltipText);
        fileInput.add(chooseNameMapFile, fileInputConstraints);
        }
        //====================================================================
        //===============SOUTH FILE INPUT STUFF==================================
        JPanel southFileInputButtons = new JPanel();
        
        //Load files button..
        loadFiles.setText("Load files");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 2;
        fileInputConstraints.gridy = 4;
        loadFiles.setEnabled(false);
        loadFiles.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e){
                    //Check validity
//                    loadFiles.setEnabled(false);
//                    clearFiles.setEnabled(false);
//                    exampleData.setEnabled(false);
                    if(loadedFilesValid()){
                        wizard.setCurrentPanel(wizard.returnNextId());
                    }else{
                        loadFiles.setEnabled(true);
                        clearFiles.setEnabled(true);
                        exampleData.setEnabled(true);
                    }
                }
        });
        fileInput.add(loadFiles, fileInputConstraints);
        //southFileInputButtons.add(loadFiles);
        
        //Clear fies button
        clearFiles.setText("Clear all");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 3;
        fileInputConstraints.gridy = 4;
        clearFiles.setActionCommand("clear_files");
        clearFiles.addActionListener(ial);
        fileInput.add(clearFiles, fileInputConstraints);
        clearFiles.setEnabled(true);
        exampleData = new JButton("Load example data");
        exampleData.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                try{
                    String headURL = "http://wodaklab.org/webstarts/gipro/data/small_sample_data/";
                    
                    String urlGenetic = headURL+"sample_genetic_interactions.txt";
                    String urlComplex = headURL+"sample_complex_gene_map.txt";
                    String urlPhysical = headURL+"sample_physical_interactions.txt";
                    String urlNamemap = headURL+"sample_name_map.txt";
                    
                    String saveGenetic = "GIPro_sample_genetic.tab";
                    String saveComplex = "GIPro_sample_complex.tab";
                    String savePhysical = "GIPro_sample_physical.tab";
                    String saveNamemap = "GIPro_sample_name_map.tab";
                    
                    System.out.println("Loading example data...");
                    relationalFilePath.setText(HelperMethods.saveOnlineTextToFile
                            (urlGenetic, saveGenetic));
                    complexFilePath.setText(HelperMethods.saveOnlineTextToFile
                            (urlComplex, saveComplex));
                    physicalInteractionFilePath.setText(HelperMethods.saveOnlineTextToFile
                            (urlPhysical, savePhysical));
                    nameMapFilePath.setText(HelperMethods.saveOnlineTextToFile
                            (urlNamemap, saveNamemap));
                    
                    //Simulate loadFiles button- loadFiles.doClick();
                    loadFiles.setEnabled(false);
                    clearFiles.setEnabled(false);
                    
                    mwpds.populatePanelOneData(relationalFilePath.getText(), 
                            complexFilePath.getText(),nameMapFilePath.getText(),
                            physicalInteractionFilePath.getText(),
                            relationFiltering.isSelected());
                    
                    filesLoaded = true;
                    setNextButtonEnabled(false);
                    mwpds.runTaskLoadFiles();
                    requestFocusInWindow();
                    changeNextButtonText("Begin analysis");
                    setEnabledContainer(fileInput, false);
                    setEnabledContainer(paramInputPanel, true);
                    setNextButtonEnabled(true);
                    
                    //Set cutoff to 0.0
                    customCutoffButton.setSelected(true);
                    customNegativeCutoffField.setText("0.0");
                    customPositiveCutoffField.setText("0.0");
                    //Set no enrichment
                    //noEnrichmentBtn.setSelected(true);
                    FDRField.setText("1.0");
                    
                    
                    //Tell user what happend  
                    JOptionPane.showMessageDialog(null, "<html><b>Note:</b>"
                            + " The loaded example data is too small for cuttoff or enrichment analysis.<br>"
                            + "Positive and negative cutoffs are set to <b>0.0</b> and <b>No enrichment</b> option is selected"
                            + "<br>For more extensive use of the plugin, please download the sample data from http://wodaklab.org/gipro/"
                            + "<br><hr></hr>Click <b>OK</b> to proceed"
                            + "</html>"
                            , "Notice!", JOptionPane.INFORMATION_MESSAGE);
                    
                    //Click begin analysis
                    wizard.getNextButton().doClick();
                    
                    //Delete saved paths for example data if exists
                    File tmpFile = new File("GIProPlugin.temp");
//                    if(tmpFile.exists())
//                        tmpFile.delete();
                    
                    
                }catch(Exception e){
                    JOptionPane.showMessageDialog(null, "Example dataset could not be loaded at this time, "
                            + "please try again later", 
                            "Message", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                    return;
                }
            }
        });
        
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 4;
        fileInputConstraints.gridy = 4;
        fileInputConstraints.gridwidth = 2;
        fileInput.add(exampleData, fileInputConstraints);
        
        //filter relations separate row
        relationFiltering.setText("Filter interactions with at least one member in a complex");
        relationFiltering.setToolTipText("<html>Check this to filter relations so that at least one of the genes <br>" 
                + "belongs to a complex. This should be checked when using large <br>"
                + "relations files (such as those produced from microarray data).</html>");
        fileInputConstraints = new java.awt.GridBagConstraints();
        fileInputConstraints.gridx = 2;
        fileInputConstraints.gridy = 5;
        fileInputConstraints.gridwidth = 5;
        fileInput.add(relationFiltering, fileInputConstraints);
        
        
        //====================================================================
        DocumentListener validation = new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                    setNextButtonEnabled(allNecessaryFieldsValid());
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                    setNextButtonEnabled(allNecessaryFieldsValid());
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                    //setNextButtonEnabled(allNecessaryFieldsValid());	
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
                if(allNecessaryFieldsValid()){
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    updatePvalueCutoffs();
                    mainPanel.setCursor(Cursor.getDefaultCursor());
                }
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                if(allNecessaryFieldsValid()){
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    updatePvalueCutoffs();
                    mainPanel.setCursor(Cursor.getDefaultCursor());
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                updatePvalueCutoffs();
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }
        };
        
        DocumentListener percentileCutoffUpdateDocumentListener = new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                updatePercentileCutoffs();
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }
            @Override
            public void insertUpdate(DocumentEvent e) {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                updatePercentileCutoffs();
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                updatePercentileCutoffs();
                mainPanel.setCursor(Cursor.getDefaultCursor());
            }
        };
        

        // ======================= cutoffParameterPanel ====================================
        JPanel cutoffParameterPanel = new JPanel();
        cutoffParameterPanel.setLayout(new BoxLayout(cutoffParameterPanel, BoxLayout.Y_AXIS));
        TitledBorder cutoffPanelBorder = BorderFactory.createTitledBorder("2. GI score cutoff parameters");
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
        pvalCutoffButton.setSelected(true);
        cutoffButtonGroup.add(pvalCutoffButton);
        subCutoffPanel.add(pvalCutoffButton, constraintsCutoff);

        
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
        Double oneTailDefault = mwpds.INITITAL_GAUSSIAN_PVAL/2;
        pvalLeftTailCutoffField.setText(oneTailDefault + "");
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
        pvalRightTailCutoffField.setText(oneTailDefault + "");
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
        leftTailPercentileField.setText("5");
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
        rightTailPercentileField.setText("95");
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
        customCutoffButton.setSelected(true);
        cutoffButtonGroup.add(customCutoffButton);
        subCutoffPanel.add(customCutoffButton, constraintsCutoff);
        
        customNegCutoffLabel.setText("Negative cutoff:");
        customNegCutoffLabel.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 0;
        constraintsCutoff.gridy = 8;
        constraintsCutoff.anchor = java.awt.GridBagConstraints.LINE_END;
        subCutoffPanel.add(customNegCutoffLabel, constraintsCutoff);

        customNegativeCutoffField.setColumns(5);
        customNegativeCutoffField.setText("0.0");
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
        customPositiveCutoffField.setText("0.0");
        customPositiveCutoffField.setEnabled(false);
        constraintsCutoff = new java.awt.GridBagConstraints();
        constraintsCutoff.gridx = 1;
        constraintsCutoff.gridy = 9;
        constraintsCutoff.gridwidth = 2;
        constraintsCutoff.fill = java.awt.GridBagConstraints.HORIZONTAL;
        customPositiveCutoffField.getDocument().addDocumentListener(validation);
        customPositiveCutoffField.getDocument().addDocumentListener(customCutoffUpdateDocumentListener);
        subCutoffPanel.add(customPositiveCutoffField, constraintsCutoff);


        
        
        
        //End everything else
        
        //cutoffParameterPanel.add(visualCutoffBox);
        //cutoffParameterPanel.add(new JSeparator());
        cutoffParameterPanel.add(subCutoffPanel) ;

        // ===================== enrichmentParameterPanel ==================================
        JPanel enrichmentParameterPanel = new JPanel();
        //Titled border
        TitledBorder enrichmentPanelBorder = BorderFactory.createTitledBorder("3. Enrichment analysis");
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
        FDRField.setText("0.05");
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
        useFET.setSelected(true);
        enrichmentButtons.add(useFET);
        subEnrichmentPanel.add(useFET, constraintsEnrichment);
        
        useSimulations.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        useSimulations.setText("Random draws");
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 3;
        constraintsEnrichment.gridwidth = 2;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_START;
        useSimulations.addActionListener(ial);
        useSimulations.setActionCommand("use_simulations");
        useSimulations.setSelected(true);
        enrichmentButtons.add(useSimulations);
        subEnrichmentPanel.add(useSimulations, constraintsEnrichment);
        
        String simulationInfo = "<html>The number of simulation trials that will be run<br>" +
        "for each complex/interacting pair of complexes.</html>";
        
        numberOfTrialsLabel.setText("Number of trials:");
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 0;
        constraintsEnrichment.gridy = 4;
        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_END;
        numberOfTrialsLabel.setEnabled(false);
        numberOfTrialsLabel.setToolTipText(simulationInfo);
        subEnrichmentPanel.add(numberOfTrialsLabel, constraintsEnrichment);
        
        numTrialsField.setColumns(5);
        constraintsEnrichment = new java.awt.GridBagConstraints();
        constraintsEnrichment.gridx = 1;
        constraintsEnrichment.gridy = 4;
        constraintsEnrichment.fill = java.awt.GridBagConstraints.HORIZONTAL;
        numTrialsField.setEnabled(false);
        numTrialsField.setText(Integer.toString(1000));
        numTrialsField.setToolTipText(simulationInfo);
        numTrialsField.getDocument().addDocumentListener(validation);
        subEnrichmentPanel.add(numTrialsField, constraintsEnrichment);
        
        String eachComplexInfo = "<html>Leave unchecked if you would like complexes with the same<br> " +
            "number of interactions to share a distribution. (Faster) <br><br>Recommendation:<br> " +
            "- <b>UNCHECKED</b> if using many trials (1000 is enough)<br>"+
            "- <b>CHECKED</b> if using small number of trials (<300).";
        
        
        trialForEachComplex.setText("Run trials for each complex");
        trialForEachComplex.setEnabled(false);
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
        
//        noEnrichmentBtn.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
//        noEnrichmentBtn.setText("No enrichment");
//        constraintsEnrichment = new java.awt.GridBagConstraints();
//        constraintsEnrichment.gridx = 0;
//        constraintsEnrichment.gridy = 6;
//        constraintsEnrichment.gridwidth = 2;
//        constraintsEnrichment.anchor = java.awt.GridBagConstraints.LINE_START;
//        noEnrichmentBtn.addActionListener(ial);
//        noEnrichmentBtn.setActionCommand("no_enrichment");
//        noEnrichmentBtn.setSelected(false);
//        enrichmentButtons.add(noEnrichmentBtn);
//        subEnrichmentPanel.add(noEnrichmentBtn, constraintsEnrichment);
        
        enrichmentParameterPanel.add(subEnrichmentPanel);
        
        
        //DONE ENRICHMENT PANEL - START PUTTING ALL TOGETHER
        
        mainPanel.setLayout(new BoxLayout(mainPanel,BoxLayout.Y_AXIS));

        paramInputPanel = new JPanel();
        paramInputPanel.setLayout(new BorderLayout());
        
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        mainPanel.add(logoPanel);
        
        
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        
        
        JButton aboutButton = new JButton("About");
        aboutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                new AboutDialog(Cytoscape.getDesktop(), true, Input.this.wizardFrame);
            }
        });
        JPanel aboutPanel = new JPanel();
        //aboutPanel.add(aboutButton);
        //mainPanel.add(aboutPanel);
        
        
        JPanel helpPanel = new JPanel();
        helpPanel.add(aboutButton);
        URL url2 = getClass().getClassLoader().getResource("images/help_icon.gif"); 
        ImageIcon logoImage2 = new ImageIcon(url2);
        JLabel helpLabel = new JLabel("<html><font size=2><b>Need help? </b>Hover over items for more information or get the </font></html>");
        helpLabel.setIcon(logoImage2);
        helpPanel.add(helpLabel);
        helpPanel.add(new UserManualLabel(wizard.getWizardFrame()));
        mainPanel.add(helpPanel);
        
        
        mainPanel.add(fileInput);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 17)));
        
        cutoffParameterPanel.setPreferredSize(new Dimension(355, 250));
        enrichmentParameterPanel.setPreferredSize(new Dimension(355, 250));
        
        paramInputPanel.add(cutoffParameterPanel, BorderLayout.WEST);
        paramInputPanel.add(Box.createRigidArea(new Dimension(5, 0)), BorderLayout.CENTER);
        paramInputPanel.add(enrichmentParameterPanel, BorderLayout.EAST);
        mainPanel.add(paramInputPanel);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    }
    
    public boolean loadedFilesValid(){
//        JOptionPane.showMessageDialog(null, "<html><b>"
//                + "Your files will be verified<br>"
//                + "Please be patient...<br>"
//                + "Your files will load automatically once verified<br>"
//                + "Click <font color=green>OK</font> to begin</b></html>",
//                "", JOptionPane.INFORMATION_MESSAGE);
        wizard.getWizardFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        
        loadFiles.setEnabled(false);
        clearFiles.setEnabled(false);
        exampleData.setEnabled(false);
        
        boolean isValid = false;
        String giErrorString = "Genetic interaction file: <font color=green><b>OK</b></font>";
        String complexErrorString = "Protein complex file: <font color=green><b>OK</b></font>";
        String physicalErrorString = "Physical interaction file: <font color=green><b>OK</b></font>";
        String nameErrorString = "Name map file: <font color=green><b>OK</b></font>";
        int line = 1;
        String str;
        int tabs;

        List<String> giErrors = new ArrayList();
        List<String> complexErrors = new ArrayList();
        List<String> physicalErr = new ArrayList();
        List<String> nameErr = new ArrayList();
                
        int errorsThrown = 0;
        try{
        FileReader fr1 = new FileReader(relationalFilePath.getText());

        Scanner gi = new Scanner(fr1);

        while(gi.hasNextLine()){
            str = gi.nextLine();
            if(str.isEmpty()){
                line++;
                continue;
            }
            tabs = str.split("\t").length;
            if(tabs != 3 && tabs != 4){
                if(str.length()>100) str = str.substring(0, 99)+"...";
                giErrors.add("Genetic interaction file: "+relationalFilePath.getText() +"<br></br>"
                        + "<font color=red><b>Error</b> on line "+
                        line+". Expected 3 or 4 tab delimited attributes, found: "+tabs+"</font>"
                        +"<br></br>   Line " +line+": " +str+"<hr></hr>");
                break;
            }
            line++;
        }
        }catch(FileNotFoundException e){
            giErrorString = "Genetic interaction file: "+relationalFilePath.getText() +"<br></br>"
                        + "<font color=red><b>Error: </b> This file is not valid </font><hr></hr>";
            errorsThrown++;
        }

        try{
        FileReader fr2 = new FileReader(complexFilePath.getText());
        Scanner complex = new Scanner(fr2);
        line = 1;
        while(complex.hasNextLine()){
            str = complex.nextLine();
            if(str.isEmpty()){
                line++;
                continue;
            }
            tabs = str.split("\t").length;
            if(tabs != 2 && tabs != 3){
                if(str.length()>100) str = str.substring(0, 99)+"...";
                complexErrors.add("Protein complex file: "+complexFilePath.getText()+"<br></br>"
                        + "<font color=red><b>Error</b> on line "+
                        line+". Expected 2 tab delimited attributes, found: "+tabs+"</font>"
                        +"<br></br>   Line " +line+": " +str+"<hr></hr>");
                break;
            }
            line++;
        }
        }catch(FileNotFoundException e){
            complexErrorString = "Protein complex file: "+complexFilePath.getText() +"<br></br>"
                        + "<font color=red><b>Error: </b> This file is not valid </font><hr></hr>";
            errorsThrown++;
        }

        try{
            FileReader fr3 = new FileReader(physicalInteractionFilePath.getText());
            Scanner physical = new Scanner(fr3);
            line = 1;
            while(physical.hasNextLine()){
                str = physical.nextLine();
                if(str.isEmpty()){
                    line++;
                    continue;
                }
                tabs = str.split("\t").length;
                if(tabs != 3){
                    if(str.length()>100) str = str.substring(0, 99)+ "...";
                    physicalErr.add("Physical interaction file: "+physicalInteractionFilePath.getText()+"<br></br>"
                            + "<font color=red><b>Error</b> on line "+
                            line+". Expected 3 tab delimited attributes, found: "+tabs+"</font>"+
                            "<br></br>   Line " +line+": " +str+"<hr></hr>");
                    break;
                }
                line++;
            }
        }catch(FileNotFoundException e){
            physicalErrorString = "Physical interaction file: "+physicalInteractionFilePath.getText() +"<br></br>"
                        + "<font color=red><b>Error: </b> This file is not valid </font><hr></hr>";
            errorsThrown++;
        }

        if(!nameMapFilePath.getText().equals("")){
            try{
                FileReader fr4 = new FileReader(nameMapFilePath.getText());

                Scanner name = new Scanner(fr4);
                line = 1;
                while(name.hasNextLine()){
                    str = name.nextLine();
                    if(str.isEmpty()){
                        line++;
                        continue;
                    }
                    tabs = str.split("\t").length;
                    if(tabs != 2){
                        System.out.println("TAB NOT 2!");
                        if(str.length()>100) str = str.substring(0, 99)+ "...";
                        nameErr.add("Name map file: "+nameMapFilePath.getText()+"<br></br>"
                                + "<font color=red><b>Error</b> on line "+
                                line+". Expected 2 tab delimited attributes, found: "+tabs+"</font>"+
                                "<br></br>   Line " +line+": " +str+"<hr></hr>");
                        break;
                    }
                    line++;
                }
            }catch(FileNotFoundException e){
                nameErrorString = "Name map file: "+nameMapFilePath.getText() +"<br></br>"
                        + "<font color=red><b>Error: </b> This file is not valid </font><hr></hr>";
                errorsThrown++;
            }

            

        }
        //DONE GETTING ERRORS, DISPLAY:
        if(!giErrors.isEmpty()) giErrorString = giErrors.get(0);
        if(!complexErrors.isEmpty()) complexErrorString = complexErrors.get(0);
        if(!physicalErr.isEmpty()) physicalErrorString = physicalErr.get(0);
        if(!nameErr.isEmpty()) nameErrorString = nameErr.get(0);
        isValid = giErrors.isEmpty() && complexErrors.isEmpty() 
                && physicalErr.isEmpty() && nameErr.isEmpty() && (errorsThrown == 0); 

        
        wizard.getWizardFrame().setCursor(Cursor.getDefaultCursor());
                
                if(!isValid){
                    JOptionPane.showMessageDialog(null, "<html><pre>"
                            +"<font size=4><b>File format error</b></font><br>"
                            + "An error occured while trying to load your files."
                            + "<br>Please ensure the correctness of the input files provided<br><br>"
                            + 
                            "<br></br>"+ giErrorString+ 
                            "<br></br>"+complexErrorString +
                            "<br></br>"+   physicalErrorString+
                            "<br></br>"+   nameErrorString
                            +"</pre></html>", 
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
                return isValid;
    }
    
    /**
     * Validates that text-fields for input file paths are not an empty
     * @return true if all 3 required file paths are not empty, false otherwise
     */
    private static boolean allNecessaryFileFieldsValid(){
        return (!relationalFilePath.getText().equals("") 
                && !complexFilePath.getText().equals("") 
                && !physicalInteractionFilePath.getText().equals(""));
    }
    
    /**
     * Validates parameters associated with radio-buttons of cutoff and 
     * enrichment panels.
     * @return true if parameters for both cutoff and enrichment panels are valid, false otherwise
     */
    private static boolean allNecessaryParameterFieldsValid(){
        boolean cutoffParamsValid = 
            (
                (
                pvalCutoffButton.isSelected() 
            && isDouble(pvalLeftTailCutoffField.getText()) && isDouble(pvalRightTailCutoffField.getText())  
            && Double.parseDouble(pvalLeftTailCutoffField.getText()) >= new Double(0)
                && Double.parseDouble(pvalLeftTailCutoffField.getText()) <= new Double(1)
            && Double.parseDouble(pvalRightTailCutoffField.getText()) >= new Double(0)
                && Double.parseDouble(pvalRightTailCutoffField.getText()) <= new Double(1)
                )
                
            || 
                (
                percentCutoffButton.isSelected() 
            && isInteger(leftTailPercentileField.getText()) 
            && isInteger(rightTailPercentileField.getText()) 
            && Integer.parseInt(leftTailPercentileField.getText()) >= 0 
            && Integer.parseInt(leftTailPercentileField.getText()) <= 100
            && Integer.parseInt(rightTailPercentileField.getText()) >= 0 
            && Integer.parseInt(rightTailPercentileField.getText()) <= 100
                )

            || 
                (
                (customCutoffButton.isSelected() 
            && isDouble(customPositiveCutoffField.getText()) 
            && isDouble(customNegativeCutoffField.getText()))
            && Double.parseDouble(solidPositiveCutoffValue.getText()) >= Double.parseDouble(solidNegativeCutoffValue.getText())
                )
            );

        boolean enrichmentParamsValid = 
            isDouble(FDRField.getText()) 
            && Double.parseDouble(FDRField.getText()) >=0
            && (useFET.isSelected() || isInteger(numTrialsField.getText()));

        return cutoffParamsValid && enrichmentParamsValid;
    }
    
    /**
     * Checks if files have been loaded yet. If so, validate parameter fields
     * otherwise, validate file fields
     * @return If files are loaded, returns true given the parameter fields are valid, false otherwise.
     * If files have not been loaded, returns true if the file fields are valid, false otherwise.
     */
    private static boolean allNecessaryFieldsValid(){
        if (filesLoaded)
            return allNecessaryParameterFieldsValid();
        else
            return allNecessaryFileFieldsValid();
    }
    
    /**
     * Updates cutoffs box visual to user accordingly
     */
    private static void updateCustomCutoffs(){
        if (customCutoffButton.isSelected()){
            if (isDouble(customPositiveCutoffField.getText()))
                setVisualCutoffs(Double.parseDouble(customPositiveCutoffField.getText()), null);
            if (isDouble(customNegativeCutoffField.getText()))
                setVisualCutoffs(null, Double.parseDouble(customNegativeCutoffField.getText()));
        }
    }
    
    
    private static void updatePvalueCutoffs(){
        
        if (pvalCutoffButton.isSelected()){
            Double leftTailCutoff = null;
            Double rightTailCutoff = null;

            if(isDouble(pvalLeftTailCutoffField.getText())) leftTailCutoff = Double.parseDouble(pvalLeftTailCutoffField.getText());
            if(isDouble(pvalRightTailCutoffField.getText())) rightTailCutoff = Double.parseDouble(pvalRightTailCutoffField.getText());
            mwpds.recalculateGaussianCutoffs
                (leftTailCutoff, rightTailCutoff, customPositiveCutoffField, customNegativeCutoffField);
            
        }
    }
    
    private static void updatePercentileCutoffs(){
        if (percentCutoffButton.isSelected()){
            //wait cursor for long calculations
            leftTailPercentileField.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            rightTailPercentileField.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Integer leftTailCutoff = null;
            Integer rightTailCutoff = null;

            if(isInteger(leftTailPercentileField.getText())) leftTailCutoff = Integer.parseInt(leftTailPercentileField.getText());
            if(isInteger(rightTailPercentileField.getText())) rightTailCutoff = Integer.parseInt(rightTailPercentileField.getText());

            mwpds.calculateTwoTailPercentileCutoffs(leftTailCutoff,rightTailCutoff, customPositiveCutoffField, customNegativeCutoffField);
            
            //return to default cursor
            leftTailPercentileField.setCursor(Cursor.getDefaultCursor());
            rightTailPercentileField.setCursor(Cursor.getDefaultCursor());
        }
    }
    
    /**
     * Check if a string is an integer
     * @param s String to parse
     * @return true if string is an integer, false otherwise
     */
    public static boolean isInteger(String s){
        if (s == null || s.equals(""))
            return false;
        try{
            Integer.parseInt(s);
            return true;
        }
        catch (NumberFormatException e){
            return false;
        }
    }
    
    /**
     * Check if a string is a Double
     * @param s String to parse
     * @return true if string is a double, false otherwise
     */
    public static boolean isDouble(String s){
        if (s == null || s.equals(""))
            return false;
        try{
            Double.parseDouble(s);
            return true;
        }
        catch (NumberFormatException e){
            return false;
        }
    }
    
    /**
     * Executed before main ICTools window appears
     */
    @Override
    public void aboutToDisplayPanel() {
        //setVisualCutoffs(mwpds.getPosCutoff(), mwpds.getNegCutoff());
        changeNextButtonText("Begin analysis");
        setNextButtonEnabled(false);
        if (!filesLoaded){
            loadDefaultFileDirs();
            setEnabledContainer(paramInputPanel, false);
        }
    }
    
    /**
     * Loads file paths previously saved to a temp file in home directory
     */
    private void loadDefaultFileDirs(){
        JTextField[] fields = new JTextField[]{relationalFilePath, nameMapFilePath,
            complexFilePath, physicalInteractionFilePath};
        if (mwpds.inputInfoDirectory.exists()){
            try{
                BufferedReader br = new BufferedReader(new FileReader(mwpds.inputInfoDirectory));
                String fileDir = br.readLine();
                for (JTextField f : fields){
                    if (fileDir != null && (new File (fileDir)).exists()){
                            f.setText(fileDir);
                    }
                    fileDir = br.readLine();
                }
                br.close();
                //setNextButtonEnabled(allNecessaryFieldsValid());
                loadFiles.setEnabled(allNecessaryFieldsValid());
            }
            catch(IOException e){
                    System.out.println("Error reading saved filepaths.");
            }
        }
    }
    
    /**
     * Executed before main ICTools window is closed
     */
    @Override
    public void aboutToHidePanel() {
        
        if (!filesLoaded){
            loadFiles.setEnabled(false);
            clearFiles.setEnabled(false);
            mwpds.populatePanelOneData(
                    relationalFilePath.getText(), 
                    complexFilePath.getText(),
                    nameMapFilePath.getText(),
                    physicalInteractionFilePath.getText(),
                    relationFiltering.isSelected());
            filesLoaded = true;
            try{
                SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

                    @Override
                    protected Void doInBackground() throws Exception {
                        setNextButtonEnabled(false);
                        //mwpds.executeCutoffCalculations();	
                        mwpds.runTaskLoadFiles();
                        requestFocusInWindow();
                        return null;
                    }

                    @Override
                    protected void done(){
                        setVisualCutoffs(mwpds.getPosCutoff(), mwpds.getNegCutoff());
                        changeNextButtonText("Begin analysis");
                        setEnabledContainer(fileInput, false);
                        setEnabledContainer(paramInputPanel, true);
                        setNextButtonEnabled(true);
                        relationFiltering.setEnabled(false);
                        trialForEachComplex.setEnabled(false);
                        //clearFiles.setEnabled(false);
                        
                        //initial pvalue cutoffs
                        customPositiveCutoffField.setText(mwpds.getPosCutoff()+"");
                        customNegativeCutoffField.setText(mwpds.getNegCutoff()+"");
                        
                    }
                };
                task.execute();

            }
            catch(OutOfMemoryError o){
                mwpds.specialPrintln("ERROR " + o.toString());
                JOptionPane.showMessageDialog(null, "Insufficient Memory!\n"
                        + " please execute with vm arguments for more memory.",
                        "Fatality", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        }
        else{
            Double pos = Double.parseDouble(customPositiveCutoffField.getText());
            Double neg = Double.parseDouble(customNegativeCutoffField.getText());
            int trials = 0;
            boolean trialForEach = false;
            if (useSimulations.isSelected()){
                    trials = Integer.parseInt(numTrialsField.getText());
                    trialForEach = trialForEachComplex.isSelected();
            }
            //For no enrichment use fet with fdr = 1.0
//            if(noEnrichmentBtn.isSelected()){
//                noEnrichmentBtn.setSelected(false);
//                useFET.setSelected(true);
//                FDRField.setText("1.0");
//            }
            System.out.println("Input.java populating panel two data");
            
            mwpds.populatePanelTwoData(
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
        }
		
    }

    @Override
    public void displayingPanel() {
        setBackButtonEnabled(false);
        //setNextButtonEnabled(allNecessaryFieldsValid());
    }

    @Override
    public Object getBackPanelDescriptor() {
        return null;
    }
    
    /**
     * Updates cutoff box visual to user
     * @param pos Positive cutoff to update
     * @param neg Negative cutoff to update
     */
    private static void setVisualCutoffs (Double pos, Double neg){
        DecimalFormat format = new DecimalFormat("#.####");
        if (pos != null){
            mwpds.setCustomCutoffsCutoffs(pos, null);  
        }
        if (neg != null){
            mwpds.setCustomCutoffsCutoffs(null, neg); 
        }
    }
	
    // need to change this up, returns the current panel and performs cutoff calculations if only one thing loaded (and then ungrays other options)
    // otherwise it returns the next panel
    @Override
    public Object getNextPanelDescriptor() {
        if (filesLoaded)
            return "FINISH";
        else 
            return this.getPanelDescriptorIdentifier();
    }
	
    /**
     * Enables/disables all components inside a container including itself recursively
     * @param panel Panel to enable/disable
     * @param enable true to enable panel. false to disable
     */
    private static void setEnabledContainer(Container panel, boolean enable) {
        if (panel == null){
            return;
        }
        
        Component[] com = panel.getComponents();

        if (com == null){
            return;
        }
        
        for (int a = 0; a < com.length; a++){
            if(com[a] instanceof Container){
                setEnabledContainer((Container)com[a],enable);
            }
        }
        panel.setEnabled(enable);
        
        //Default disable
        customPosCutoffLabel.setEnabled(false);
        customNegCutoffLabel.setEnabled(false);
        customPositiveCutoffField.setEnabled(false);
        customNegativeCutoffField.setEnabled(false);
        leftTailPercentileLabel.setEnabled(false);
        leftTailPercentileField.setEnabled(false);
        rightTailPercentileLabel.setEnabled(false);
        rightTailPercentileField.setEnabled(false);
        numberOfTrialsLabel.setEnabled(false);
        numTrialsField.setEnabled(false);
        trialForEachComplex.setEnabled(false);

    }
	
    @Override
    public void finishingMove(){
        //disable everything
        setBackButtonEnabled(false);
        setNextButtonEnabled(false);
        setEnabledContainer((JPanel)this.getPanelComponent(), false);
        setEnabledContainer(consolePane, true);		
        try{
            SwingWorker<Void, Void> task = new SwingWorker<Void, Void>(){

                @Override
                protected Void doInBackground() throws Exception {
                    
                    //mwpds.executeComputations();	
                    mwpds.executeComputationsTask();
                    return null;
                }

                @Override
                protected void done(){
                    dispose();
                }
            };
            task.execute();
        }
        catch(OutOfMemoryError o){
            mwpds.specialPrintln("ERROR " + o.toString());
            JOptionPane.showMessageDialog(null, 
                    "Insufficient Memory!\n please execute with vm arguments for more memory.",
                    "Fatality", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }
	
    public static class InputActionListener implements ActionListener{
        private Input input;
        private String defaultDirectory = "";

        public InputActionListener(Input input){
                this.input = input;
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
                FDRField.setEditable(true);
                numberOfTrialsLabel.setEnabled(true);
                numTrialsField.setEnabled(true);
            }
            else if(actionCommand.equals("no_enrichment")){
                trialForEachComplex.setEnabled(false);
                numTrialsField.setEditable(false);
                numberOfTrialsLabel.setEnabled(false);
                numTrialsField.setEnabled(false);
            }
            else if(actionCommand.equals("choose_file_relational")){
                String relationalPath = showFileChooserDialog(defaultDirectory, "Select Genetic Relations File");
                if((new File(relationalPath)).exists()){
                        relationalFilePath.setText(relationalPath);
                        defaultDirectory = relationalPath;
                }
                loadFiles.setEnabled(allNecessaryFieldsValid());
            }
            else if(actionCommand.equals("choose_file_complex")){
                String complexPath = showFileChooserDialog(defaultDirectory, "Select Complex File");
                if((new File(complexPath)).exists()){
                        complexFilePath.setText(complexPath);
                        defaultDirectory = complexPath;
                }
                loadFiles.setEnabled(allNecessaryFieldsValid());
            }
            else if(actionCommand.equals("choose_file_name_map")){
                String nameMapPath = showFileChooserDialog(defaultDirectory, "Select Name Map File");
                if((new File(nameMapPath)).exists()){
                        nameMapFilePath.setText(nameMapPath);
                        defaultDirectory = nameMapPath;
                }	
                loadFiles.setEnabled(allNecessaryFieldsValid());
            }
            else if (actionCommand.equals("clear_name_file")){
                nameMapFilePath.setText("");
                loadFiles.setEnabled(allNecessaryFieldsValid());
            }
            else if (actionCommand.equals("clear_files")){
                relationalFilePath.setText("");
                complexFilePath.setText("");
                nameMapFilePath.setText("");
                physicalInteractionFilePath.setText("");
                loadFiles.setEnabled(allNecessaryFieldsValid());
            }
            else if(actionCommand.equals("choose_file_physical_interaction")){
                String physicalInteractionPath= showFileChooserDialog(defaultDirectory, "Select Physical Interactions File");
                if((new File(physicalInteractionPath)).exists()){
                        physicalInteractionFilePath.setText(physicalInteractionPath);
                        defaultDirectory = physicalInteractionPath;
                }	
                loadFiles.setEnabled(allNecessaryFieldsValid());
            }
            input.setNextButtonEnabled(input.allNecessaryFieldsValid());
            setNextButtonEnabled(allNecessaryFieldsValid());
        }
		
        private String showFileChooserDialog(String defaultDirectory, String title){
            JFileChooser jfc = new JFileChooser(defaultDirectory);
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int option = jfc.showDialog(input.getPanelComponent(), title);
            File file = jfc.getSelectedFile();

            if(file == null || option != JFileChooser.APPROVE_OPTION){
                    return "";
            }
            return file.getAbsolutePath();
        }
    }//end actionListener class

}
