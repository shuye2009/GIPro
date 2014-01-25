package plugin;
import guiICTools.MainGui;
import guiICTools.MyWizardPanelDataStorage;

import java.awt.event.ActionEvent;

import java.io.FileNotFoundException;
import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import java.io.File;
import java.util.List;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import cytoscape.Cytoscape;
import cytoscape.layout.CyLayoutAlgorithm;
import cytoscape.layout.CyLayouts;
import cytoscape.layout.Tunable;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.cytopanels.CytoPanelImp;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import javax.mail.internet.MimeUtility;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * ICTools plugin initialization class (the menu action wizard initiation)
 * 
 * @author YH
 */

public class Init extends CytoscapePlugin{
	
	private static RootNetwork rn;
        private static List<Double> fullListOfScores, filteredListOfScores;
        private static int leftTailPercentile, rightTailPercentile, numberOfTrials;
        // useFisher is true when using Fisher, and false if using simulations
        private static boolean usePvalCutoffs, usePercentileCutoffs, useFisher, 
                trialForEachComplex, useSimulations; 
        private static double pvalLeftTail, pvalRightTail;
        
        private static Map<String, Complex> complexes;
	private static Map<String, Gene> genes;
	private static Map<String, ComplexEdge> complexEdges;
	private static Map<String, String> geneToOrf;
        private static double withinPVal, betweenPVal, posCutoff, negCutoff, fdr;
        private static int totalNeg, totalPos, total;
        private static MyWizardPanelDataStorage mwpds;
        private static Map<String, Set<Complex>> networkComplexMap;
        private static List<Double> valuesWithin, valuesBetween;
        
        
	public static void setRootNetwork(RootNetwork rn){
            Init.rn = rn;
            complexes = rn.getComplexes();
            genes = rn.getGenes();
            complexEdges = rn.getComplexEdge();
            geneToOrf = rn.getGeneToOrf();
            withinPVal = rn.getWithinPValue();
            betweenPVal = rn.getBetweenPValue();
            totalPos = rn.getTotalPos();
            totalNeg = rn.getTotalNeg();
            total = rn.getTotal();
            mwpds = rn.getMyWizardPanelDataStorage();
            networkComplexMap = rn.getNetworkComplexMap();
            valuesWithin = rn.getValuesWithin();
            valuesBetween = rn.getValuesBetween();
            fullListOfScores = rn.getFullListOfScores();
            filteredListOfScores = rn.getFilteredListOfScores();
            
            //On the fly
            usePvalCutoffs = rn.getUsePvalCutoffs();
            pvalLeftTail = rn.getPvalLeftTail();
            pvalRightTail = rn.getPvalRightTail();
            
            usePercentileCutoffs = rn.getUsePercentileCutoffs();
            leftTailPercentile = rn.getLeftTailPercentile();
            rightTailPercentile = rn.getRightTailPercentile();
            
            useSimulations = rn.getUseSimulations();
            useFisher = rn.getUseFisher();
            numberOfTrials = rn.getNumberOfTrials();
            trialForEachComplex = rn.getTrailForEachComplex();
	}
        
        /**
         * Default constructor called by Cytoscape
         */
	public Init() {
            CytoscapeAction pnda = new GIPro();
            Config config = new Config();
            JMenu menus = Cytoscape.getDesktop().getCyMenus().getOperationsMenu();
            menus.add(pnda);
            
	}
        @Override
	public void restoreSessionState(final List<File> pStateFileList) {
            
        //Thread starts
            Thread restoreThread = new Thread(null, new Runnable() {
            public void run() {
            
            

            try {
                File prop_file = null;
            
                if ((pStateFileList == null) || (pStateFileList.isEmpty())) {
                    //No previous state to restore
                    return;
                }

                System.out.println("pStateFileList:");
                for(File f: pStateFileList){
                    if(f.getName().endsWith("GIPro.props")) prop_file = f;
                    System.out.println("\t- "+f.getAbsolutePath());
                }
                System.out.println("exits?="+prop_file.exists());
                System.out.println("Getting bytes from file...");
                byte[] data = getBytesFromFile(prop_file);
                
                System.out.println("Creaing ByteArrayInputStream...");
                ByteArrayInputStream bis = new ByteArrayInputStream(data);
                ObjectInputStream in = new ObjectInputStream(MimeUtility.decode(bis,"base64"));
                
                //Data structures
                //complexes = (Map<String, Complex>) in.readObject();
                System.out.println("Restoring objects...");
                
                complexes = (Map<String, Complex>) in.readObject();
                for(String s: complexes.keySet())
                    System.out.println("#"+s+"\t"+complexes.get(s));
                
                genes = (Map<String, Gene>) in.readObject();
                complexEdges = (Map<String, ComplexEdge>) in.readObject();
                geneToOrf = (Map<String, String>)in.readObject();
                
                //Constants
                withinPVal = (Double) in.readObject();
                betweenPVal = (Double) in.readObject();
                totalPos = (Integer) in.readObject();
                totalNeg = (Integer) in.readObject();
                total = (Integer) in.readObject();
                posCutoff = (Double) in.readObject();
                negCutoff = (Double) in.readObject();
                fdr = (Double) in.readObject();
                
                //Network stuff
                networkComplexMap = (Map<String, Set<Complex>>)in.readObject();
                
                //For histogram stuff
                valuesWithin = (ArrayList<Double>) in.readObject();
                valuesBetween = (ArrayList<Double>) in.readObject();
                
                //Full list of relations
                fullListOfScores = (List<Double>) in.readObject();
                filteredListOfScores = (List<Double>) in.readObject();
                
                //for on the fly
                usePvalCutoffs = (Boolean) in.readObject();
                pvalLeftTail = (Double) in.readObject();
                pvalRightTail = (Double) in.readObject();
                
                usePercentileCutoffs = (Boolean)in.readObject();
                leftTailPercentile = (Integer)in.readObject();
                rightTailPercentile = (Integer)in.readObject();
                
                useFisher = (Boolean) in.readObject();
                useSimulations = (Boolean) in.readObject();
                numberOfTrials = (Integer) in.readObject();
                trialForEachComplex = (Boolean) in.readObject();
                
                
                rn = new RootNetwork(complexes, genes, complexEdges, geneToOrf, 
                        withinPVal, betweenPVal, totalPos, totalNeg, total, 
                        posCutoff, negCutoff, fdr, networkComplexMap, valuesWithin, 
                        valuesBetween, fullListOfScores, filteredListOfScores,
                        
                        usePvalCutoffs,pvalLeftTail, pvalRightTail,
                        usePercentileCutoffs, leftTailPercentile, rightTailPercentile,  
                        useFisher, useSimulations ,numberOfTrials, trialForEachComplex);  
                rn.restoreRootNetwork();
                System.out.println("Closing ObjectInputStream...");
                in.close();
            } catch (Exception ee) {
                System.out.println("EXEPTION RESTORING");
                ee.printStackTrace();
            }
            
            }
            }, "RestoreThread", 1 << 25);
            
            try{
                //Start thread with 2^25 bytes (32M)
                restoreThread.start();
                //Makes cytoscape wait for save thread to finish running before finishing its thread
                restoreThread.join();
                
            }catch(Exception e){
                e.printStackTrace();
            }
	}
        
        @Override
	public void saveSessionStateFiles(final List<File> pFileList) {
            
            CytoPanelImp ctrlPanel = (CytoPanelImp) 
                                        Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
            int index = ctrlPanel.indexOfComponent("GIPro");
            if(index != -1){                    
                Object[] options = {"Networks only","GIPro Session"};

//                int n = JOptionPane.showOptionDialog(null,"What would you like to save?", "Save",
//                JOptionPane.YES_NO_OPTION,
//                JOptionPane.QUESTION_MESSAGE,
//                null,
//                options,
//                options[1]);
                
                int x = JOptionPane.showOptionDialog(null, "What would you like to save?", "Save",
JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
null, options, options[1]);
                
                
                
                
                if(x == 0){
                    System.out.println("Saving networks only");
                    return;
                    //networks only option
                }
                
                
            }
            Thread saveThread;
            saveThread = new Thread(null, new Runnable() {
                public void run() {
                    
                    System.out.println("Saving session");
                    if(rn == null){
                        System.out.println("root network is null");
                        return;
                    }
                    // Create an empty file on system temp directory
                    String tmpDir = System.getProperty("java.io.tmpdir");
                    System.out.println("java.io.tmpdir: [" + tmpDir + "]");
                    
                    File prop_file = new File(tmpDir, "GIPro.props");
                    
                    try {
                        /*===================================*/
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        OutputStream mout = MimeUtility.encode(bos, "base64");
                        ObjectOutputStream out = new ObjectOutputStream(mout);
                        
                        
                        //Data structures
                        
                        out.writeObject(complexes);
                        out.writeObject(genes);
                        out.writeObject(complexEdges);
                        out.writeObject(geneToOrf);
                        
                        //Constant numbers
                        out.writeObject(withinPVal);
                        out.writeObject(betweenPVal);
                        out.writeObject(totalPos);
                        out.writeObject(totalNeg);
                        out.writeObject(total);
                        out.writeObject(rn.getPosCutoff());
                        out.writeObject(rn.getNegCutoff());
                        out.writeObject(rn.getFDR());
                        
                        //Network stuff
                        out.writeObject(networkComplexMap);
                        
                        //For histogram, scores of within and between
                        out.writeObject(rn.getValuesWithin());
                        out.writeObject(rn.getValuesBetween());
                        
                        //Full list of scores
                        out.writeObject(rn.getFullListOfScores());
                        out.writeObject(rn.getFilteredListOfScores());
                        
                        //For on the fly cutoff/enrichment updates.
                        out.writeObject(usePvalCutoffs);
                        out.writeObject(pvalLeftTail);
                        out.writeObject(pvalRightTail);
                        
                        out.writeObject(usePercentileCutoffs);
                        out.writeObject(leftTailPercentile);
                        out.writeObject(rightTailPercentile);
                        
                        out.writeObject(useFisher);
                        out.writeObject(useSimulations);
                        out.writeObject(numberOfTrials);
                        out.writeObject(trialForEachComplex);
                        
                        out.writeObject(new Integer(0));
                        /*===================================*/
                        
                        OutputStream fout = new FileOutputStream(prop_file);
                        bos.writeTo(fout);
                        fout.close();
                        
                        out.flush();
                        out.close();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    
                    pFileList.add(prop_file);
                    
                    
                }
            }, "SaveThread", 1 << 25);
          try {  
            //Start thread with 2^25 bytes (32M) of stack size
            saveThread.start();
            //Makes cytoscape wait for save thread to finish running before finishing its thread
            saveThread.join();
          }catch (InterruptedException ex) {
            ex.printStackTrace();
          }
            
            
	}
	
        @SuppressWarnings("serial")
	protected class GIPro extends CytoscapeAction {
            /**
             * ICTools constructor
             */
            public GIPro() {
                    super("GIPro");
                    setPreferredMenu("GIPro");
            }
		
            public void actionPerformed(ActionEvent e) {
        	SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        //Check stack size stuff 
                        for(CyLayoutAlgorithm m: CyLayouts.getAllLayouts()){
                            System.out.println(m.getName());
                        }
                        
                        CyLayoutAlgorithm lay = CyLayouts.getLayout("attributes-layout");
                        List<Tunable> map = lay.getSettings().getTunables();
                        for(Tunable t: map){
                            System.out.println(t.getName()+"\t"+t.getValue() +"\t"+t.getDescription()+"\t"+t.getType());
                            //if(t.get)
                        }
                        
                        
                        //Check if network already generated
                        if(rn !=null){
                            Object[] options = {"Start new session","Cancel"};
                            int ans = JOptionPane.showOptionDialog(null,
                                "<html>To start another instance of GIPro, you must be in a new session. <br>"
                                    + "To start a new session: navigate to File-> New -> Session or click the button below"
                                    + "</html>",
                                "Message",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.INFORMATION_MESSAGE,
                                null,
                                options,
                                options[1]);

                            if(ans == JOptionPane.OK_OPTION){
                                //Clear ROOTNETWORK/MWPDS
                                System.out.println("root network now null");
                                rn.setRootNetworksMWPDS(null);
                                rn = null;
                                System.gc();
                                CytoPanelImp ctrlPanel = (CytoPanelImp) 
                                        Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
                                int index = ctrlPanel.indexOfComponent("GIPro");
                                if(index != -1){
                                    ctrlPanel.setSelectedIndex(0);
                                    ctrlPanel.remove(ctrlPanel.getComponentAt(index));
                                }
                                Cytoscape.createNewSession();
                                   
//                                CytoPanelImp panel = (CytoPanelImp) 
//                                        Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
//                                if(panel.indexOfComponent("GIPro") != -1){
//                                    panel.remove(panel.indexOfComponent("GIPro"));
//                                }
//                                panel.setSelectedIndex(0);
//
//                                for(CyNetwork network: Cytoscape.getNetworkSet()){
//                                    network.unselectAllEdges();
//                                    network.unselectAllNodes();
//                                    Cytoscape.destroyNetworkView(network);
//                                    Cytoscape.destroyNetwork(network);
//                                }
//                                rn.clearAll();
////                                rn = null;
//                                //System.exit(0);
                                
                            }
                            //No option, dont do anything
                            else{
                                return;
                            }
                        }
                        MainGui.createAndShowGUI();
                        
                    }
    		});
            }//End actionPerformed
	}
        public void copy(File src, File dst) throws IOException {

            try
            {
                    //create FileInputStream object for source file
                    FileInputStream fin = new FileInputStream(src);

                    //create FileOutputStream object for destination file
                    FileOutputStream fout = new FileOutputStream(dst);

                    byte[] b = new byte[1024];
                    int noOfBytes = 0;

                    System.out.println("Copying file using streams");

                    //read bytes from source file and write to destination file
                    while( (noOfBytes = fin.read(b)) != -1 )
                    {
                            fout.write(b, 0, noOfBytes);
                    }

                    System.out.println("File copied!");

                    //close the streams
                    fin.close();
                    fout.close();                  

            }
            catch(FileNotFoundException fnf)
            {
                    System.out.println("Specified file not found :" + fnf);
            }
            catch(IOException ioe)
            {
                    System.out.println("Error while copying file :" + ioe);
            }
        }
        
        // Returns the contents of the file in a byte array.
        public static byte[] getBytesFromFile(File file) throws IOException {
            InputStream is = new FileInputStream(file);

            // Get the size of the file
            long length = file.length();

            // You cannot create an array using a long type.
            // It needs to be an int type.
            // Before converting to an int type, check
            // to ensure that file is not larger than Integer.MAX_VALUE.
            if (length > Integer.MAX_VALUE) {
                // File is too large
            }

            // Create the byte array to hold the data
            byte[] bytes = new byte[(int)length];

            // Read in the bytes
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                   && (numRead=is.read(bytes, offset, bytes.length-offset)) >= 0) {
                offset += numRead;
            }

            // Ensure all the bytes have been read in
            if (offset < bytes.length) {
                throw new IOException("Could not completely read file "+file.getName());
            }

            // Close the input stream and return bytes
            is.close();
            return bytes;
        }
         
}//end class
