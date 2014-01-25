package wcluster;

// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   PCLData.java

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PCLData
{

    public PCLData()
    {
        genes = new HashMap();
        exprNames = new ArrayList();
        idToGene = new HashMap();
        numExprs = 0;
    }

    public boolean readDataFromPCL(List<String> pclList)
        throws Exception
    {
        genes.clear();
        numExprs = 0;
        int geneID = 0;
        try
        {
            int row = 0;
            int gweightCol = -1;
            int colOffset = 2;
            
            for(String line: pclList)
            {
                if(line == null || line.equals("")) continue;
                
                boolean processRow = true;
                if(row == 0)
                {
                    String names[] = line.split("\t");
                    for(int i = 2; i < names.length; i++)
                        if(names[i].equalsIgnoreCase("GWEIGHT"))
                        {
                            gweightCol = i;
                            colOffset = 3;
                        } else
                        {
                            exprNames.add(names[i]);
                            numExprs++;
                        }

                    if(names.length < 4)
                        return false;
                    processRow = false;
                } else
                if(row == 1)
                {
                    String lineParts[] = line.split("\t");
                    if(lineParts[0].equalsIgnoreCase("EWEIGHT"))
                        processRow = false;
                }
                if(processRow)
                {
                    String lineParts[] = line.split("\t");
                    if(lineParts.length > 1)
                    {
                        Object obj = genes.get(lineParts[0]);
                        Gene gene;
                        if(obj != null)
                        {
                            gene = (Gene)obj;
                            gene.numRecords++;
                            System.err.println("Duplicate Entry: " + lineParts[0]);
                        } else
                        {
                            gene = new Gene(lineParts[0], lineParts[1]);
                            gene.id = geneID++;
                            gene.exprLvls = new double[numExprs];
                            gene.missingValues = new boolean[numExprs];
                            gene.weights = new double[numExprs];
                            gene.numRecords = 1;
                            genes.put(lineParts[0], gene);
                            idToGene.put(new Integer(gene.id), gene);
                        }
                        for(int i = 2; i < lineParts.length; i++)
                            if(i != gweightCol)
                                try
                                {
                                    if(lineParts[i].equalsIgnoreCase("NaN"))
                                        throw new NumberFormatException();
                                    gene.exprLvls[i - colOffset] = Double.parseDouble(lineParts[i]);
                                    gene.missingValues[i - colOffset] = false;
                                }
                                catch(NumberFormatException except)
                                {
                                    if(i - colOffset >= numExprs || i - colOffset < 0)
                                        return false;
                                    gene.exprLvls[i - colOffset] = 0.0D;
                                    gene.missingValues[i - colOffset] = true;
                                }

                        for(int i = lineParts.length - 2; i < numExprs; i++)
                        {
                            gene.exprLvls[i] = 0.0D;
                            gene.missingValues[i] = true;
                        }

                    }
                }
                row++;
            }

            numGenes = geneID;
        }
        catch(Exception except)
        {
            throw except;
        }
        return true;
    }

    public boolean readWeightsFromFile(String fileName)
        throws IOException
    {
        try
        {
            BufferedReader fin = new BufferedReader(new FileReader(fileName));
            int row = 0;
            int gweightCol = -1;
            int colOffset = 2;
            for(String line = fin.readLine(); line != null && !line.equals(""); line = fin.readLine())
            {
                boolean processRow = true;
                if(row == 0)
                {
                    String names[] = line.split("\t");
                    for(int i = 2; i < names.length; i++)
                        if(names[i].equalsIgnoreCase("GWEIGHT"))
                        {
                            gweightCol = i;
                            colOffset = 3;
                        }

                    if(names.length < 4)
                        return false;
                    processRow = false;
                } else
                if(row == 1)
                {
                    String lineParts[] = line.split("\t");
                    if(lineParts[0].equalsIgnoreCase("EWEIGHT"))
                        processRow = false;
                }
                if(processRow)
                {
                    String lineParts[] = line.split("\t");
                    if(lineParts.length > 1)
                    {
                        Object obj = genes.get(lineParts[0].toUpperCase());
                        if(obj != null)
                        {
                            Gene gene = (Gene)obj;
                            for(int i = 2; i < lineParts.length; i++)
                                if(i != gweightCol)
                                    try
                                    {
                                        if(lineParts[i].equalsIgnoreCase("NaN"))
                                            throw new NumberFormatException();
                                        gene.weights[i - colOffset] = Double.parseDouble(lineParts[i]);
                                    }
                                    catch(NumberFormatException except)
                                    {
                                        if(i - colOffset >= numExprs || i - colOffset < 0)
                                            return false;
                                        gene.weights[i - colOffset] = 1.0D;
                                    }

                            for(int i = lineParts.length - 2; i < numExprs; i++)
                                gene.weights[i] = 1.0D;

                        }
                    }
                }
                row++;
            }

        }
        catch(FileNotFoundException except)
        {
            System.err.println("Weighting File Not Found: " + fileName + "\tAssuming uniform weighting.");
            for(int i = 0; i < numGenes; i++)
            {
                Gene g = (Gene)idToGene.get(new Integer(i));
                g.weights = new double[numExprs];
                for(int j = 0; j < numExprs; j++)
                    g.weights[j] = 1.0D;

            }

        }
        return true;
    }

    public void convertToGeneArrays()
    {
        dataValues = new double[numGenes][];
        missingValues = new boolean[numGenes][];
        weights = new double[numGenes][];
        for(int i = 0; i < numGenes; i++)
        {
            Gene g = (Gene)idToGene.get(new Integer(i));
            dataValues[i] = g.exprLvls;
            missingValues[i] = g.missingValues;
            weights[i] = g.weights;
        }

    }

    public void convertToConditionArrays()
    {
        dataValues = new double[numExprs][numGenes];
        missingValues = new boolean[numExprs][numGenes];
        weights = new double[numExprs][numGenes];
        for(int i = 0; i < numGenes; i++)
        {
            Gene g = (Gene)idToGene.get(new Integer(i));
            for(int j = 0; j < numExprs; j++)
            {
                dataValues[j][i] = g.exprLvls[j];
                missingValues[j][i] = g.missingValues[j];
                weights[j][i] = g.weights[j];
            }

        }

    }

    public boolean writeCDT(String filename, int ordering[])
    {
        BufferedWriter bw;
        try
        {
            bw = new BufferedWriter(new FileWriter(filename));
        }
        catch(IOException e)
        {
            System.err.println("Unable to open " + filename + " for output.");
            return false;
        }
        try
        {
            bw.write("GID\tYORF\tNAME\tGWEIGHT");
            for(int i = 0; i < exprNames.size(); i++)
                bw.write("\t" + (String)exprNames.get(i));

            bw.newLine();
            bw.write("EWEIGHT\t\t1\t");
            for(int i = 0; i < exprNames.size(); i++)
                bw.write("\t1");

            bw.newLine();
            for(int z = 0; z < numGenes; z++)
            {
                int i = ordering[z];
                Gene g = (Gene)idToGene.get(new Integer(i));
                bw.write("GENE" + i + "\t" + g.spotID + "\t" + g.name + "\t1");
                for(int j = 0; j < g.exprLvls.length; j++)
                    if(g.missingValues[j])
                        bw.write("\t");
                    else
                        bw.write("\t" + g.exprLvls[j]);

                bw.newLine();
            }

            bw.flush();
            bw.close();
        }
        catch(IOException e)
        {
            System.err.println("Unable to write to " + filename + ".");
            return false;
        }
        return true;
    }

    public boolean writeCDT(String filename, int geneOrdering[], int exprOrdering[])
    {
        BufferedWriter bw;
        try
        {
            bw = new BufferedWriter(new FileWriter(filename));
        }
        catch(IOException e)
        {
            System.err.println("Unable to open " + filename + " for output.");
            return false;
        }
        try
        {
            bw.write("GID\tYORF\tNAME\tGWEIGHT");
            for(int i = 0; i < exprNames.size(); i++)
                bw.write("\t" + (String)exprNames.get(exprOrdering[i]));

            bw.newLine();
            bw.write("AID\t\t\t");
            for(int i = 0; i < exprNames.size(); i++)
                bw.write("\tARRAY" + exprOrdering[i]);

            bw.newLine();
            bw.write("EWEIGHT\t\t1\t");
            for(int i = 0; i < exprNames.size(); i++)
                bw.write("\t1");

            bw.newLine();
            for(int z = 0; z < numGenes; z++)
            {
                int i = geneOrdering[z];
                Gene g = (Gene)idToGene.get(new Integer(i));
                bw.write("GENE" + i);
                double test;
                try
                {
                    test = Double.parseDouble(g.spotID);
                }
                catch(NumberFormatException e)
                {
                    test = (0.0D / 0.0D);
                }
                if(Double.isNaN(test))
                    bw.write("\t" + g.spotID);
                else
                    bw.write("\tX" + g.spotID);
                try
                {
                    test = Double.parseDouble(g.name);
                }
                catch(NumberFormatException e)
                {
                    test = (0.0D / 0.0D);
                }
                if(Double.isNaN(test))
                    bw.write("\t" + g.name);
                else
                    bw.write("\t" + g.name);
                bw.write("\t1");
                for(int q = 0; q < g.exprLvls.length; q++)
                {
                    int j = exprOrdering[q];
                    if(g.missingValues[j])
                        bw.write("\t");
                    else
                        bw.write("\t" + g.exprLvls[j]);
                }

                bw.newLine();
            }

            bw.flush();
            bw.close();
        }
        catch(IOException e)
        {
            System.err.println("Unable to write to " + filename + ".");
            return false;
        }
        return true;
    }

    public HashMap genes;
    public HashMap idToGene;
    public int numGenes;
    public double dataValues[][];
    public boolean missingValues[][];
    public double weights[][];
    public ArrayList exprNames;
    public int numExprs;
}
