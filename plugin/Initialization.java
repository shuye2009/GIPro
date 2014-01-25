package plugin;
import cytoscape.CyNetwork;
import guiICTools.MainGui;
import guiICTools.MyWizardPanelDataStorage;

import java.awt.event.ActionEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenu;
import javax.swing.SwingUtilities;

import java.io.File;
import java.util.List;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import cytoscape.Cytoscape;
import cytoscape.plugin.CytoscapePlugin;
import cytoscape.util.CytoscapeAction;
import cytoscape.view.cytopanels.CytoPanelImp;
import guiICTools.Input;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import wGUI.Wizard;
import wGUI.WizardPanel;

/**
 * ICTools plugin initialization class (the menu action wizard initiation)
 * 
 * @author YH
 */

public class Initialization extends CytoscapePlugin{
	
	private static RootNetwork rn;
        
        private static Map<String, Complex> complexes;
	private static Map<String, Gene> genes;
	private static Map<String, ComplexEdge> complexEdges;
	private static Map<String, String> geneToOrf;
        private static double withinPVal, betweenPVal, posCutoff, negCutoff, fdr;
        private static int totalNeg, totalPos, total;
        private static MyWizardPanelDataStorage mwpds;
        private static Map<String, Set<Complex>> networkComplexMap;
        private static ArrayList<Double> valuesWithin, valuesBetween;
        
	public static void setRootNetwork(RootNetwork rn){
            Initialization.rn = rn;
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
	}
        
        /**
         * Default constructor called by Cytoscape
         */
	public Initialization() {
            CytoscapeAction pnda = new ICTools();
            Config config = new Config();
            JMenu menus = Cytoscape.getDesktop().getCyMenus().getOperationsMenu();
            menus.add(pnda);
            
	}
        @Override
	public void restoreSessionState(final List<File> pStateFileList) {
            Thread restoreThread = new Thread(null, new Runnable() {
            public void run() {
            System.out.println("Restored property file");
            for(File f: pStateFileList){
                System.out.println(f.getAbsolutePath());
            }
            if ((pStateFileList == null) || (pStateFileList.isEmpty())) {
                //No previous state to restore
                return;
            }

            try {
                File prop_file = new File(pStateFileList.get(0).
                        getAbsolutePath().replaceAll("Initialization_", ""));

                FileInputStream fis = new FileInputStream(prop_file);
                ObjectInputStream in = new ObjectInputStream(fis);

                //Data structures
                complexes = (Map<String, Complex>)in.readObject();
                genes = (Map<String, Gene>)in.readObject();
                complexEdges = (Map<String, ComplexEdge>)in.readObject();
                geneToOrf = (Map<String, String>)in.readObject();
                
                //Constants
                withinPVal = (Double)in.readObject();
                betweenPVal = (Double)in.readObject();
                totalPos = (Integer)in.readObject();
                totalNeg = (Integer)in.readObject();
                total = (Integer)in.readObject();
                posCutoff = (Double)in.readObject();
                negCutoff = (Double)in.readObject();
                fdr = (Double)in.readObject();
                
                //Network stuff
                networkComplexMap = (Map<String, Set<Complex>>)in.readObject();
                
                //For histogram stuff
                valuesWithin = (ArrayList<Double>) in.readObject();
                valuesBetween = (ArrayList<Double>) in.readObject();

                rn = new RootNetwork(complexes, genes, complexEdges, geneToOrf, 
                        withinPVal, betweenPVal, totalPos, totalNeg, total, 
                        posCutoff, negCutoff, fdr, networkComplexMap, valuesWithin, valuesBetween);  
                rn.restoreRootNetwork();

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
            
            Thread saveThread = new Thread(null, new Runnable() {
            public void run() {
                
                System.out.println("Saving session");
            if(rn == null){
                System.out.println("root network is null");
                return;
            }
            // Create an empty file on system temp directory
            String tmpDir = System.getProperty("java.io.tmpdir");
            System.out.println("java.io.tmpdir: [" + tmpDir + "]");

            File prop_file = new File(tmpDir, "ICTools.props");

            try {
                FileOutputStream fout = new FileOutputStream(prop_file);
                ObjectOutputStream out = new ObjectOutputStream(fout);
                
                /*===================================*/
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
                /*===================================*/
                
                //Done
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
              
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
            
            
	}
	
        @SuppressWarnings("serial")
	protected class ICTools extends CytoscapeAction {
            /**
             * ICTools constructor
             */
            public ICTools() {
                    super("ICTools");
                    setPreferredMenu("Plugins");
            }
		
            public void actionPerformed(ActionEvent e) {
        	SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        //Check stack size stuff 
                        //checkVmOptions();
                        
                        //Check if network already generated
                        if(rn !=null){
                            int ans = JOptionPane.showConfirmDialog(Cytoscape.getDesktop(), 
                                    "An ICTools network is already generated. Are you sure you want to start over?", "Message", JOptionPane.YES_NO_OPTION);
                            
                            if(ans == JOptionPane.YES_OPTION){
                                CytoPanelImp panel = (CytoPanelImp) 
                                        Cytoscape.getDesktop().getCytoPanel(SwingConstants.WEST);
                                if(panel.indexOfComponent("ICTools") != -1){
                                    panel.remove(panel.indexOfComponent("ICTools"));
                                }
                                panel.setSelectedIndex(0);

                                for(CyNetwork network: Cytoscape.getNetworkSet()){
                                    Cytoscape.destroyNetworkView(network);
                                    Cytoscape.destroyNetwork(network);
                                }
                                rn.clearAll();
                                
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
         
}//end class
