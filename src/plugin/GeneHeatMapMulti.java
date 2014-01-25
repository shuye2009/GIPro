package plugin;

import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;
import edu.stanford.genetics.treeview.FileSet;
import edu.stanford.genetics.treeview.TreeViewFrame;
import edu.stanford.genetics.treeview.ViewFrame;
import edu.stanford.genetics.treeview.app.LinkedViewApp;
import java.awt.Color;
import java.awt.Container;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("serial")
public class GeneHeatMapMulti implements Task{
    String tmpDir;
    
    List<Complex> complexes;
    Map<Complex, Set<Gene>> mapOfComplexGenes;
    List<Gene> allGenes = new ArrayList<Gene>();
    
    List<List<Double>> scores;
    
    int nArray;
    int nQuery;
    boolean hasInsufficentData;
    
    TreeViewFrame tvFrame;
    Container container;
    List<String> pclLines;
    
    Map<Complex, String> complexToColorMap;

    /**
     * GeneHeatMap constructor
     * @param comp1 Source complex
     * @param comp2 Target complex
     */
    public GeneHeatMapMulti(List<Complex> complexes){
        Random numGen = new Random();

        tmpDir = System.getProperty("java.io.tmpdir");
        this.complexes = complexes;
        mapOfComplexGenes = new HashMap();
        complexToColorMap = new HashMap();
        
        for(Complex c: complexes){
            allGenes.addAll(c.getGenes());
            mapOfComplexGenes.put(c, new HashSet(c.getGenes()));
            
            Color randColor = new Color(numGen.nextInt(220), numGen.nextInt(220), numGen.nextInt(220));
            String hex = "#"+Integer.toHexString( randColor.getRGB() & 0x00ffffff );
            complexToColorMap.put(c, hex);
        }
        
        hasInsufficentData = false;
        pclLines = new ArrayList();
        removeNonInteractingGenes();
        nArray = allGenes.size();
        nQuery = allGenes.size();
        scores = new ArrayList<List<Double>>();
        
        fillScoreArray();
        viewUnclusteredMatrix();
    }
    
    public ViewFrame getViewFrame(){
        return tvFrame;
    }
    
    public boolean hasInsufficentData(){
        return hasInsufficentData;
    }
	
    /**
     * This method orders the genes with complex1genes followed by complex2genes
     * and stores its index in a vector 
     * @return Vector of integers containing indexes of complex1 ordered genes
     * followed by complex2 ordered genes
     */
//    private List<Integer> orderGenes(){
//        List<Integer> order = new ArrayList<Integer>();
//        ArrayList<Gene> temp = new ArrayList<Gene>(complex1Genes);
//
//        Collections.sort(temp);
//        for (Gene g : temp){
//            order.add(allGenes.indexOf(g));
//        }
//        
//        temp.clear();
//        //temp.addAll(complex2Genes);
//        
//        Collections.sort(temp);
//        for (Gene g : temp){
//            order.add(allGenes.indexOf(g));
//        }
//        return order;
//    }
    
    public void initPCL(){
//        StringBuilder line0 = new StringBuilder("UID" + "\t" + "NAME" + "\t" + "FGCOLOR"+"\t"+"GWEIGHT" + "\t"); 
//        StringBuilder line1 = new StringBuilder("FGCOLOR"+"\t"+"\t"+"\t"+"\t");
        StringBuilder line0 = new StringBuilder("UID" + "\t" + "NAME" + "\t"+"GWEIGHT" + "\t"); 
        //StringBuilder line1 = new StringBuilder("FGCOLOR"+"\t"+"\t"+"\t"+"\t");
        StringBuilder line2 = new StringBuilder("EWEIGHT" + "\t"+ "\t" + "\t"+"\t");
        
        for(Gene g: allGenes){
            String gName = g.getGeneName();
            List<String> cpxs = new ArrayList();
            for(Complex c: mapOfComplexGenes.keySet()){
                if(mapOfComplexGenes.get(c).contains(g)){
                    cpxs.add(c.getName());
                }
            }
            
            line0.append(gName+fakeTab(gName, 10)+cpxs.toString() + "\t");
            
            line2.append("1"+"\t");
        }
        //remove last tabs
        line0.replace(line0.length()-1, line0.length(), "");
        //line1.replace(line1.length()-1, line1.length(), "");
        line2.replace(line2.length()-1, line2.length(), "");
        pclLines.add(line0.toString());
        //pclLines.add(line1.toString());
        pclLines.add(line2.toString());
    }

    /**
     * Removes genes that do not interact with opposing complex.
     * Non-interacting genes are removed from the list allGenes
     */
    private void removeNonInteractingGenes(){
        ArrayList<Gene> toReturn = new ArrayList<Gene>();
        for (Gene g : allGenes){
            boolean keep = false;
            for (Gene g2 : allGenes){
                if (!g.equals(g2) && g.geneticallyConnectedTo(g2)){
                    keep = true;
                    break;
                }
            }
            if (keep){
                toReturn.add(g);
            }
        }
        for(Complex c: mapOfComplexGenes.keySet()){
            mapOfComplexGenes.get(c).retainAll(toReturn);
        }
        allGenes = toReturn;
    }
    
    public String fakeTab(String s, Integer i){
        if(s.length() > i) return "     ";//5 spaces
        
        String ret = "";
        int numSpaces = i - s.length();
        for(int x=0; x<numSpaces; x++) ret = ret+" ";
        return ret;
    }
    
    /**
     * Find max/min values and store scores in a 2D array
     */
    protected void fillScoreArray(){
        initPCL();
        
        int count = 0;
        for (Gene gene1 : allGenes){
            String gID = gene1.getGeneIdentifier();
            String gName = gene1.getGeneName();
            
            //Task monitor gen pcl
            count++;
            
            StringBuilder line = new StringBuilder();
            
            List<String> cpxs = new ArrayList();
            for(Complex c: mapOfComplexGenes.keySet()){
                if(mapOfComplexGenes.get(c).contains(gene1)){
                    cpxs.add(c.getName());
                }
            }
            
            
            
            line.append(gID+fakeTab(gName, 10)+cpxs.toString() +"\t"+gName+fakeTab(gName, 10)+cpxs.toString()+"\t"+"1"+"\t");
            
            List<Double> scoreRow = new ArrayList<Double>();
            for (Gene gene2 : allGenes){
                SubEdge edge = gene1.getGeneticEdge(gene2);
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
        
        hasInsufficentData = scores.isEmpty() || allGenes.isEmpty();
    }
    
    public void viewUnclusteredMatrix(){
        
        if(hasInsufficentData) return;
        HelperMethods.writeListToFile(pclLines, tmpDir+File.separator+"UNCLUSTERED.pcl");
        
        File file = new File(tmpDir + File.separator+"UNCLUSTERED.pcl");
        FileSet fileSet = new FileSet(file.getName(), file.getParent()+File.separator);
        
        //TaskMonitor tm = new TaskMonitor();
        //FOR IF YOU WANTED TO CLUSTER THEM
//        WCluster wc = new WCluster(tmpDir, pclLines);
//        wc.doCluster(null);
//        
//        File file = new File(tmpDir + File.separator+"WCLUSTER.cdt");
//        FileSet fileSet = new FileSet(file.getName(), file.getParent()+File.separator);
        
        LinkedViewApp lva = new LinkedViewApp();
        try{
            tvFrame = new TreeViewFrame(lva);
            tvFrame.loadNW(fileSet);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    //Run task stuff
    @Override
    public void run() {
        fillScoreArray();
        viewUnclusteredMatrix();
        
    }

    @Override
    public void halt() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setTaskMonitor(TaskMonitor tm) throws IllegalThreadStateException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getTitle() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
