package wcluster;

// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   WCluster.java

import cytoscape.task.TaskMonitor;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WCluster
{
    final String EUCLIDEAN_DISTANCE = "E";
    final String PEARSON_CORRELATION = "P";
    final String ABS_PEARSON_CORRELATION = "AP";
    
    List<String> pclLines;
    String SAVE_PATH;
    
    public WCluster(String SAVE_PATH, List<String> pclLines)
    {
        this.pclLines = pclLines;
        this.SAVE_PATH = SAVE_PATH;
    }

    public static void usage()
    {
        System.out.println("Usage for WCluster:");
        System.out.println("    java -jar [-Xmx512m] WCluster.jar <inputPCL> <inputWeights>");
        System.out.println("         <distanceMetric> <outputCDT> <outputGTR> [<outputATR>]\n");
        System.out.println("<inputPCL> - .pcl format input file");
        System.out.println("<inputWeights> - same format as inputPCL, but contains weights");
        System.out.println("<distanceMetric> - E, P, or PA - (E)clidean, (P)earson, (P)earson(A)bsolute");
        System.out.println("<outputCDT> - desired filename of the output .cdt");
        System.out.println("<outputGTR> - desired filename of the output .gtr");
        System.out.println("<outputATR> - (optional) if present, arrays are also clustered");
        System.out.println("              and this is the desired filename of output .atr\n");
        System.out.println("This program hierarchically clusters the data in in the input PCL file");
        System.out.println("and generates JavaTreeView-able CDT, GTR, (and ATR) files.");
    }

    public static void main(String args[])
    {
        if(args.length < 5)
        {
            usage();
            System.exit(0);
        }
        PCLData pcl = new PCLData();
        try
        {
            BufferedReader br = new BufferedReader(new FileReader(args[0]));
            //pcl.readDataFromPCL(br);
        }
        catch(FileNotFoundException e)
        {
            System.err.println("FileNotFound: " + args[0] + "\nStopping execution.");
            System.exit(0);
        }
        catch(IOException e)
        {
            System.err.println("Problem reading file: " + args[0] + "\nStopping execution.");
            System.exit(0);
        }
        try
        {
            pcl.readWeightsFromFile(args[1]);
        }
        catch(IOException e)
        {
            System.err.println("Problem reading file: " + args[1] + "\nStopping execution.");
            System.exit(0);
        }
        pcl.convertToGeneArrays();
        HierarchicalCluster hc = new HierarchicalCluster(pcl.dataValues, pcl.missingValues, pcl.weights, args[2]);
        TreeCluster tc = new TreeCluster(hc.treeResult, hc.distResult);
        hc.writeGeneTreeFile(args[4]);
        if(args.length > 5)
        {
            pcl.convertToConditionArrays();
            HierarchicalCluster ahc = new HierarchicalCluster(pcl.dataValues, pcl.missingValues, pcl.weights, args[2]);
            TreeCluster atc = new TreeCluster(ahc.treeResult, ahc.distResult);
            ahc.writeArrayTreeFile(args[5]);
            pcl.writeCDT(args[3], tc.orderedIdxs, atc.orderedIdxs);
        } else
        {
            pcl.writeCDT(args[3], tc.orderedIdxs);
        }
    }

    public void doCluster(TaskMonitor tm){
        PCLData pcl = new PCLData();
        try {
            if(tm != null) tm.setStatus("Clustering...(Loading PCL data)");
            pcl.readDataFromPCL(pclLines);
            pcl.readWeightsFromFile("uniform_weights");
            
            //GTR SAVE FILE EXTENSION
            if(tm != null) tm.setPercentCompleted(50);
            if(tm != null) tm.setStatus("Clustering...(Generating GTR file)");
            pcl.convertToGeneArrays();
            HierarchicalCluster hc = new HierarchicalCluster(pcl.dataValues, pcl.missingValues, pcl.weights, EUCLIDEAN_DISTANCE);
            TreeCluster tc = new TreeCluster(hc.treeResult, hc.distResult);
            hc.writeGeneTreeFile(SAVE_PATH+File.separator+"/WCLUSTER.gtr");
            
            //ATR SAVE FILE EXTENSION
            if(tm != null) tm.setPercentCompleted(70);
            if(tm != null) tm.setStatus("Clustering...(Generating ATR file)");
            pcl.convertToConditionArrays();
            HierarchicalCluster ahc = new HierarchicalCluster(pcl.dataValues, pcl.missingValues, pcl.weights, EUCLIDEAN_DISTANCE);
            TreeCluster atc = new TreeCluster(ahc.treeResult, ahc.distResult);
            ahc.writeArrayTreeFile(SAVE_PATH+File.separator+"/WCLUSTER.atr");
            
            //CDT SAVE FILE EXTENSION
            if(tm != null) tm.setPercentCompleted(90);
            if(tm != null) tm.setStatus("Clustering...(Generating CDT file)");
            pcl.writeCDT(SAVE_PATH+File.separator+"WCLUSTER.cdt", tc.orderedIdxs, atc.orderedIdxs);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}