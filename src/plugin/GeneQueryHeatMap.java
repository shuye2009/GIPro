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
import java.awt.Container;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import wcluster.WCluster;

public class GeneQueryHeatMap implements Task {

    private static final long serialVersionUID = 5110577088090749629L;
    private static final String tmpDir = System.getProperty("java.io.tmpdir");
    
    ArrayList<Gene> queryGenes = new ArrayList<Gene>();
    ArrayList<Gene> allGenes = new ArrayList<Gene>();
    Map<String, List<Double>> pearsonInput = new HashMap();
    String type;
    
    List<List<Double>> scores;
    int nQuery;
    int nArray;
    boolean hasInsufficentData;
    
    List<String> pclLines;
    
    
    TaskMonitor taskMonitor;
    TreeViewFrame tvFrame;
    Container container;
    
    /**
     * GeneQueryHeatMap constructor
     * @param queryGenes
     * @param genes 
     */
    public GeneQueryHeatMap(ArrayList<Gene> queryGenes, Collection<Gene> genes, String type){
        this.type = type;
        this.queryGenes = queryGenes;
        this.allGenes.addAll(genes);
        pclLines = new ArrayList();
        hasInsufficentData = false;
        
        if(CustomPatterns.negCutoff == null) CustomPatterns.negCutoff = 0.0;
        if(CustomPatterns.posCutoff == null) CustomPatterns.posCutoff = 0.0;
        
        removeNonInteractingGenes();
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
    
    public String fakeTab(String s, Integer i){
        if(s.length() > i) return "     ";//5 spaces
        
        String ret = "";
        int numSpaces = i - s.length();
        for(int x=0; x<numSpaces; x++) ret = ret+" ";
        return ret;
    }
    
    public void initPCL(){
        StringBuilder line0 = new StringBuilder("UID" + "\t" + "NAME" + "\t" + "GWEIGHT" + "\t"); 
        StringBuilder line1 = new StringBuilder("EWEIGHT" + "\t"  + "\t"+ "\t");
        
        //+fakeTab(g.getGeneName(),10)+g.getDescription()
        for(Gene g: allGenes){
            line0.append(g.getGeneName() + "\t");
            line1.append("1"+"\t");
        }
        //remove last tabs
        line0.replace(line0.length()-1, line0.length(), "");
        line1.replace(line1.length()-1, line1.length(), "");
        pclLines.add(line0.toString());
        pclLines.add(line1.toString());
        
    }
    
    public ViewFrame getViewFrame(){
        return tvFrame;
    }
    
    public boolean hasInsufficentData(){
        return hasInsufficentData;
    }
    
    public int getNumQuery(){
        return nQuery;
    }
    
    public int getNumArray(){
        return nArray;
    }

    /**
     * Removes genes that do not interact with other genes (from those selected) 
     * from the heat-map
     */
    private void removeNonInteractingGenes(){
        ArrayList<Gene> toReturn = new ArrayList<Gene>();
        for (Gene g : allGenes){
            boolean keep = false;
            for (Gene qg : queryGenes){
                if (!g.equals(qg) && g.unfilteredGeneticallyConnectedTo(qg)){
                    keep = true;
                    break;
                }
            }
            if (keep){
                toReturn.add(g);
            }
        }
        allGenes.retainAll(toReturn);
        
        //Check for query genes that dont interact with anything and remove them
        //the query genes list
        List<Gene> filteredQueryGenes = new ArrayList();
        for(Gene g: queryGenes){
            if(!g.getUnfilteredGeneNeighboursString().isEmpty()){
                filteredQueryGenes.add(g);
                continue;
            }
            System.out.println("REMOVED: "+g.getGeneName());
        }
        queryGenes.retainAll(filteredQueryGenes);
        
        if(type.endsWith("-same") || type.endsWith("-alternating")){
            List<Gene> tmpAllGenes = new ArrayList();
            for(int col = 0; col<allGenes.size(); col++){
                List<Double> columnScore = new ArrayList();
                
                for(Gene g: queryGenes){
                    SubEdge edge = g.getUnfilteredGeneticEdge(allGenes.get(col));
                    if(edge == null){
                        columnScore.add(null);
                    }else{
                        columnScore.add(edge.getScore());
                    }
                }
                if(type.endsWith("-same")){
                    if(allPositive(columnScore) || allNegative(columnScore)){
                        tmpAllGenes.add(allGenes.get(col));
                    }
                }if(type.endsWith("-alternating")){
                    if(allAlternating(columnScore)){
                        tmpAllGenes.add(allGenes.get(col));
                    }
                }
            }
            allGenes.retainAll(tmpAllGenes);
        }
    }
    
    /**
     * Find max/min values and store scores in a 2D array
     */
    protected void fillScoreArray() {
        //Initialize pcl file 
        initPCL();
        
        int count = 0;
        for (Gene query_gene : queryGenes){
            String gName = query_gene.getGeneName();
            String desc = query_gene.getDescription();
            
            
            //+fakeTab(gName,10)+desc
            taskMonitor.setStatus("Generating PCL file... ("+count+"/"+queryGenes.size()+")");
            count++;
            
            StringBuilder line = new StringBuilder(gName
                    +"\t"+gName+"\t"+"1"+"\t");
            
            List<Double> scoreRow = new ArrayList<Double>();
            for (Gene array_gene : allGenes){
                SubEdge edge = query_gene.getUnfilteredGeneticEdge(array_gene);
                if (edge == null){
                    scoreRow.add(null);
                    line.append(" \t");
                }
                else{
                    double score = edge.getScore();
                    scoreRow.add(new Double(score));
                    line.append(score+" \t");
                }
            }
            //Remove end of line tabs
            line.replace(line.length()-1, line.length(), "");
            pclLines.add(line.toString());
            scores.add(scoreRow);
        }
        
        //Make sure we have sufficent data
        hasInsufficentData = allGenes.size() <=1 || queryGenes.isEmpty();
        if((type.endsWith("-same")||type.endsWith("-alternating")) && queryGenes.size() < 2)
            hasInsufficentData = true;
    }
    
    public void clusterAndView(){
        if(hasInsufficentData) return;
        
        if(queryGenes.size() == 1){
            HelperMethods.writeListToFile(pclLines, tmpDir+File.separator+"WCLUSTER.pcl");
            
            File file = new File(tmpDir + File.separator+"WCLUSTER.pcl");
            FileSet fileSet = new FileSet(file.getName(), file.getParent()+File.separator);
            
            taskMonitor.setStatus("Launching TreeView");
            LinkedViewApp lva = new LinkedViewApp();
            try{
                tvFrame = new TreeViewFrame(lva);
                tvFrame.loadNW(fileSet);
            }catch(Exception e){
                e.printStackTrace();
            }
            finally{
                return;
            }
            
        }
        
        //DO CLUSTERING IF WE HAVE ENOUGH QUERY GENES
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
            Logger.getLogger(GeneQueryHeatMap.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * @param list List of doubles
     * @return true if all are positive, false otherwise
     */
    public boolean allPositive(List<Double> list){
        for(Double d: list){
            if(d == null || d < 0.0)
                return false;
            
            if(CustomPatterns.posCutoff != null){
                if(d<CustomPatterns.posCutoff) return false;
            }
        }
        return true;
    }
    /**
     * @param list List of doubles
     * @return true if all are negative, false otherwise
     */
    public boolean allNegative(List<Double> list){
        for(Double d: list){
            if(d == null || d > 0.0)
                return false;
            if(CustomPatterns.negCutoff != null){
                if(Math.abs(d)<CustomPatterns.negCutoff) return false;
            }
        }
        return true;
    }
    /**
     * @param list List of doubles
     * @return true if at least one is alternating, false otherwise
     */
    public boolean allAlternating(List<Double> list){
        int pos = 0; int neg = 0;
        for(Double d: list){
            if(d == null){
                return false;
            }
            
            if(d>0 && d<CustomPatterns.posCutoff) return false;
            if(d<0 && Math.abs(d)<CustomPatterns.negCutoff){return false;}
            
            else{
                if(d>0) pos++;
                if(d<0) neg++;
            }
        }
        return (pos > 0) && (neg > 0);
    }

    
    //Task stuff...
    @Override
    public void run() {
        taskMonitor.setStatus("Initializing...");
        taskMonitor.setPercentCompleted(-1);
        fillScoreArray();
        clusterAndView();
        nQuery = queryGenes.size();
        nArray = allGenes.size();
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
        return "Generating heatmap...";
    }
}
