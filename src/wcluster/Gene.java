package wcluster;

// Decompiled by Jad v1.5.8c. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.geocities.com/kpdus/jad.html
// Decompiler options: packimports(3) 
// Source File Name:   Gene.java


import java.io.PrintStream;
import java.util.ArrayList;

public class Gene
{

    public Gene(String _spotID, String _name)
    {
        spotID = _spotID;
        name = _name;
        ranks = null;
        utilArray = new ArrayList();
    }

    public void rankTransform()
    {
        ranks = new int[exprLvls.length];
        for(int i = 0; i < exprLvls.length; i++)
        {
            int rank = 0;
            for(int j = 0; j < exprLvls.length; j++)
                if(i != j && exprLvls[j] < exprLvls[i])
                    rank++;

            ranks[i] = rank;
        }

    }

    public double distance_L1(Gene g)
    {
        double d = 0.0D;
        for(int i = 0; i < exprLvls.length; i++)
            d += Math.abs(exprLvls[i] - g.exprLvls[i]);

        return d;
    }

    public double distance_L2(Gene g)
    {
        double d = 0.0D;
        for(int i = 0; i < exprLvls.length; i++)
            d += (exprLvls[i] - g.exprLvls[i]) * (exprLvls[i] - g.exprLvls[i]);

        return Math.sqrt(d);
    }

    public double distance_Spearman(Gene g)
    {
        if(ranks == null)
            rankTransform();
        if(g.ranks == null)
            g.rankTransform();
        double sum = 0.0D;
        for(int i = 0; i < ranks.length; i++)
        {
            int term = ranks[i] - g.ranks[i];
            sum += term * term;
        }

        return (6D * sum) / (double)(ranks.length * (ranks.length * ranks.length - 1));
    }

    public double distance_Pearson(Gene g)
    {
        double sumx = 0.0D;
        double sumy = 0.0D;
        double sumxx = 0.0D;
        double sumyy = 0.0D;
        double sumxy = 0.0D;
        for(int i = 0; i < exprLvls.length; i++)
        {
            sumx += exprLvls[i];
            sumy += g.exprLvls[i];
            sumxx += exprLvls[i] * exprLvls[i];
            sumyy += g.exprLvls[i] * g.exprLvls[i];
            sumxy += exprLvls[i] * g.exprLvls[i];
        }

        if(Math.abs((sumxy - (sumx * sumy) / (double)exprLvls.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)exprLvls.length) * (sumyy - (sumy * sumy) / (double)exprLvls.length))) > 1.0D)
            return 0.0D;
        else
            return 1.0D - (sumxy - (sumx * sumy) / (double)exprLvls.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)exprLvls.length) * (sumyy - (sumy * sumy) / (double)exprLvls.length));
    }

    public double distance_PearsonAbs(Gene g)
    {
        double sumx = 0.0D;
        double sumy = 0.0D;
        double sumxx = 0.0D;
        double sumyy = 0.0D;
        double sumxy = 0.0D;
        for(int i = 0; i < exprLvls.length; i++)
        {
            sumx += exprLvls[i];
            sumy += g.exprLvls[i];
            sumxx += exprLvls[i] * exprLvls[i];
            sumyy += g.exprLvls[i] * g.exprLvls[i];
            sumxy += exprLvls[i] * g.exprLvls[i];
        }

        if(Math.abs((sumxy - (sumx * sumy) / (double)exprLvls.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)exprLvls.length) * (sumyy - (sumy * sumy) / (double)exprLvls.length))) > 1.0D)
        {
            System.out.println("r=" + Math.abs((sumxy - (sumx * sumy) / (double)exprLvls.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)exprLvls.length) * (sumyy - (sumy * sumy) / (double)exprLvls.length))));
            return 0.0D;
        } else
        {
            return 1.0D - Math.abs((sumxy - (sumx * sumy) / (double)exprLvls.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)exprLvls.length) * (sumyy - (sumy * sumy) / (double)exprLvls.length)));
        }
    }

    public String spotID;
    public String name;
    public double exprLvls[];
    public boolean missingValues[];
    public double weights[];
    public ArrayList utilArray;
    public int ranks[];
    public int numRecords;
    public int id;
}
