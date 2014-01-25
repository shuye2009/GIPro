package plugin;

import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.LoadException;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.app.LinkedViewApp;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import wcluster.WCluster;

@SuppressWarnings("serial")
public class ComplexHeatMap implements Task{
    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    
    private Map<String, ComplexEdge> complexEdges;
    ArrayList<Complex> complexesSelected;
    ComplexEdge edge;
    public static final double NEG_SIGNIFICANCE = -1;
    public static final double POS_SIGNIFICANCE = 1;
    
    int nArray;
    int nQuery;
    boolean isPositive;
    boolean hasInsufficentData;
    
    List<List<Double>> scores;
    List<String> pclLines;
    TaskMonitor taskMonitor;
    TreeViewFrame tvFrame;
    
    /**
     * ComplexHeatMap constructor
     * @param complexesSelected List of selected complexes
     * @param complexEdges Map<String, ComplexEdge> of complex edges 
     */
    public ComplexHeatMap (ArrayList<Complex> complexesSelected, Map<String, ComplexEdge> complexEdges, boolean isPositive){
        
        pclLines = new ArrayList();
        this.isPositive = isPositive;
        this.hasInsufficentData = false;
        
        this.complexEdges = complexEdges;
        this.complexesSelected = removeNonInteractingComplexes(complexesSelected);
        nArray = this.complexesSelected.size();
        nQuery = this.complexesSelected.size();
        scores = new ArrayList<List<Double>>();
        
        // Configure JTask Dialog Pop-Up Box
        JTaskConfig jTaskConfig = new JTaskConfig();
        jTaskConfig.setOwner(Cytoscape.getDesktop());
        jTaskConfig.displayCloseButton(true);

        jTaskConfig.displayCancelButton(true);

        jTaskConfig.displayStatus(true);
        jTaskConfig.setAutoDispose(true);
        
        // Execute Task in New Thread; pops open JTask Dialog Box.
        TaskManager.executeTask(this, jTaskConfig);
    }
    
    public ViewFrame getViewFrame(){
        return tvFrame;
    }
    
    public boolean hasInsufficentData(){
        return hasInsufficentData;
    }
    
    public void initPCL(){
        StringBuilder line0 = new StringBuilder("UID" + "\t" + "NAME" + "\t" + "GWEIGHT" + "\t"); 
        StringBuilder line1 = new StringBuilder("EWEIGHT" + "\t"  + "\t"+ "\t");
//        System.out.println("complexes selected size:"+complexesSelected.size());
        for(Complex c: complexesSelected){
            line0.append(c.getName() + "\t");
            line1.append("1"+"\t");
        }
        //remove last tabs
        line0.replace(line0.length()-1, line0.length(), "");
        line1.replace(line1.length()-1, line1.length(), "");
        pclLines.add(line0.toString());
        pclLines.add(line1.toString());
//        
//        System.out.println("====init pcl:\n");
//        for(String s: pclLines)
//            System.out.println(s);
//        System.out.println("====");
        
    }
    
    /**
     * Removes all complexes that don't interact (should not be shown in the heat-map)
     * @param complexes List of input complexes 
     * @return List of complexes from input list that interact with one another
     */
    private ArrayList<Complex> removeNonInteractingComplexes(ArrayList<Complex> complexes){
        ArrayList<Complex> toReturn = new ArrayList<Complex>();
        for (Complex c : complexes){
            for (Complex c2 : complexes){
                
                ComplexEdge ce = null;
                if (complexEdges.containsKey(c.getName() + "//" + c2.getName())){
                    ce = complexEdges.get(c.getName() + "//" + c2.getName());
                }
                if(complexEdges.containsKey(c2.getName() + "//" + c.getName())){
                    ce = complexEdges.get(c2.getName() + "//" + c.getName());
                }
                
                if(ce != null){
                    if(isPositive){
                        if(ce.posRelations() != 0){
                            toReturn.add(c);
                            break;
                        }
                    }
                    if(!isPositive){
                        if(ce.negRelations() != 0){
                            toReturn.add(c);
                            break;
                        }
                    }
                }
            }
        }
        return toReturn;
    }
    
 
    /**
     * Fills the score array with scores representing the interaction between complexes
     */
    protected void fillScoreArray(){
       initPCL();
       for (int row = 0; row < nQuery; row++){
            
            Complex comp1 = complexesSelected.get(row);
            
            StringBuilder pclRow = new StringBuilder(comp1.getName() +"\t"+
                    comp1.getName() +"\t"+"1"+"\t");
            
            List<Double> scoreRow = new ArrayList<Double>();
            for (int col = 0; col < nArray; col++){
                Complex comp2 = complexesSelected.get(col);
                ComplexEdge ce;
                if (comp1.compareTo(comp2) < 0){
                    ce = complexEdges.get(comp1.getName() + "//" + comp2.getName());
                }
                else{
                    ce = complexEdges.get(comp2.getName() + "//" + comp1.getName());
                }

                if (ce == null){
                    scoreRow.add(null);
                    pclRow.append(" \t");
                }
                else{
                    String significance = ce.getSignificance();
                    if (significance == null){
                        scoreRow.add(null);
                        pclRow.append(" \t");
                    }
                    else{
                        if(significance.equals(Config.edgeBothSignificanceKey)
                        || significance.equals(Config.edgePosSignificanceKey)
                        || significance.equals(Config.edgeNegSignificanceKey)){
                            
                            scoreRow.add(ce.getAverageScore());
                            if(isPositive){
                                if(ce.posRelations() != 0)
                                    pclRow.append(ce.getPosAverageScore()+"\t");
                                else
                                    pclRow.append(" \t");
                            }
                            if(!isPositive){
                                if(ce.negRelations() != 0)
                                    pclRow.append(ce.getNegAverageScore()+"\t");
                                else
                                    pclRow.append(" \t");
                            }
                        }
                        else{
                            pclRow.append(" \t");
                        }
                    }
                }		
            }
            //Remove end of line tabs
            pclRow.replace(pclRow.length()-1, pclRow.length(), "");
            pclLines.add(pclRow.toString());
            scores.add(scoreRow);
            
//            for(String s: pclLines)
//                System.out.println(s);
        }
        
        hasInsufficentData = nQuery <= 1 || nArray <=1;
    }
    
    public void clusterAndView(){
        if(hasInsufficentData) return;
        //DO CLUSTERING
        taskMonitor.setPercentCompleted(20);
        taskMonitor.setStatus("Clustering...");
        
        WCluster wc = new WCluster(tmpDir, pclLines);
        wc.doCluster(taskMonitor);
        
        //START TREEVIEW 
        taskMonitor.setStatus("Launching TreeView");
        LinkedViewApp lva = new LinkedViewApp();
        
        File file = new File(tmpDir + File.separator+"WCLUSTER.cdt");
        FileSet fileSet = new FileSet(file.getName(), file.getParent()+File.separator);
        
        try {
            tvFrame = new TreeViewFrame(lva);
            tvFrame.loadNW(fileSet);
        } catch (LoadException ex) {
            ex.printStackTrace();
        }
    }
    
    
    //TASK STUFF
    @Override
    public void run() {
        taskMonitor.setStatus("Initializing...");
        taskMonitor.setPercentCompleted(-1);
        fillScoreArray();
        clusterAndView();
        taskMonitor.setStatus("Done...");
        taskMonitor.setPercentCompleted(100);
    }

    @Override
    public void halt() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
        this.taskMonitor = tm;
    }

    @Override
    public String getTitle() {
        return "Generating complex heatmaps";
    }
    
    
}
