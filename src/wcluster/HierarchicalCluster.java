package wcluster;

import java.io.*;

public class HierarchicalCluster
{

    public HierarchicalCluster(double data[][], boolean missing[][], double weight[][], String distMetric)
    {
        treeResult = null;
        distResult = null;
        metric = distMetric;
        int rows = data.length;
        int cols = 0;
        if(rows > 0)
            cols = data[0].length;
        if(rows < 2 || cols < 1)
            return;
        double distMatrix[][] = new double[rows][];
        for(int i = 0; i < rows; i++)
            distMatrix[i] = new double[i];

        double maxDist = (-1.0D / 0.0D);
        double minDist = (1.0D / 0.0D);
        for(int i = 0; i < rows; i++)
        {
            for(int j = 0; j < i; j++)
            {
                if(distMetric.equalsIgnoreCase("e"))
                    distMatrix[i][j] = euclidDist(data[i], missing[i], weight[i], data[j], missing[j], weight[j]);
                else
                if(distMetric.equalsIgnoreCase("pa"))
                    distMatrix[i][j] = pearsonDist(data[i], missing[i], weight[i], data[j], missing[j], weight[j], true);
                else
                    distMatrix[i][j] = pearsonDist(data[i], missing[i], weight[i], data[j], missing[j], weight[j], false);
                if(Double.isNaN(distMatrix[i][j]))
                    System.err.println("distMatrix[" + i + "][" + j + "]= " + distMatrix[i][j]);
            }

        }

        treeResult = new int[rows - 1][2];
        distResult = new double[rows - 1];
        int szCluster[] = new int[rows];
        int clusterID[] = new int[rows];
        for(int i = 0; i < rows; i++)
        {
            szCluster[i] = 1;
            clusterID[i] = i;
        }

        for(int g = rows; g > 1; g--)
        {
            int i_save = 1;
            int j_save = 0;
            double distance = distMatrix[1][0];
            for(int i = 0; i < g; i++)
            {
                for(int j = 0; j < i; j++)
                    if(distMatrix[i][j] < distance)
                    {
                        i_save = i;
                        j_save = j;
                        distance = distMatrix[i][j];
                        if(Double.isNaN(distance))
                            System.err.println("NaN found during clustering");
                    }

            }

            treeResult[rows - g][0] = clusterID[i_save];
            treeResult[rows - g][1] = clusterID[j_save];
            if(Double.isNaN(distance))
                System.err.println("NaN found during clustering");
            distResult[rows - g] = distance;
            int sum = szCluster[i_save] + szCluster[j_save];
            if((double)sum < 0.0001D)
                System.err.println("sum is very small: " + sum);
            for(int j = 0; j < j_save; j++)
            {
                distMatrix[j_save][j] = distMatrix[i_save][j] * (double)szCluster[i_save] + distMatrix[j_save][j] * (double)szCluster[j_save];
                distMatrix[j_save][j] /= sum;
                if(Double.isNaN(distMatrix[j_save][j]))
                    System.err.println("NaN found during distMatrix[j_save][j]");
            }

            for(int j = j_save + 1; j < i_save; j++)
            {
                distMatrix[j][j_save] = distMatrix[i_save][j] * (double)szCluster[i_save] + distMatrix[j][j_save] * (double)szCluster[j_save];
                distMatrix[j][j_save] /= sum;
                if(Double.isNaN(distMatrix[j][j_save]))
                    System.err.println("NaN found during distMatrix[j][j_save]");
            }

            for(int j = i_save + 1; j < g; j++)
            {
                distMatrix[j][j_save] = distMatrix[j][i_save] * (double)szCluster[i_save] + distMatrix[j][j_save] * (double)szCluster[j_save];
                distMatrix[j][j_save] /= sum;
                if(Double.isNaN(distMatrix[j][j_save]))
                    System.err.println("NaN found during distMatrix[j][j_save]");
            }

            for(int j = 0; j < i_save; j++)
            {
                distMatrix[i_save][j] = distMatrix[g - 1][j];
                if(Double.isNaN(distMatrix[i_save][j]))
                    System.err.println("NaN found during distMatrix[i_save][j]");
            }

            for(int j = i_save + 1; j < g - 1; j++)
            {
                distMatrix[j][i_save] = distMatrix[g - 1][j];
                if(Double.isNaN(distMatrix[j][i_save]))
                    System.err.println("NaN found during distMatrix[j][i_save]");
            }

            szCluster[j_save] = sum;
            szCluster[i_save] = szCluster[g - 1];
            clusterID[j_save] = g - rows - 1;
            clusterID[i_save] = clusterID[g - 1];
        }

    }

    private double euclidDist(double d1[], double d2[])
    {
        double sum = 0.0D;
        for(int i = 0; i < d1.length; i++)
        {
            double term = d1[i] - d2[i];
            sum += term * term;
        }

        return Math.abs(Math.sqrt(sum));
    }

    private double euclidDist(double d1[], boolean m1[], double w1[], double d2[], boolean m2[], double w2[])
    {
        double sum = 0.0D;
        double weightTotal = 0.0D;
        for(int i = 0; i < d1.length; i++)
            if(!m1[i] && !m2[i])
            {
                double weight = w1[i] * w2[i];
                double term = d1[i] - d2[i];
                sum += weight * term * term;
                weightTotal += weight;
            }

        if(weightTotal < 1.0000000000000001E-05D)
        {
            return (1.0D / 0.0D);
        } else
        {
            sum /= weightTotal;
            sum *= d1.length;
            return Math.sqrt(sum);
        }
    }

    private double spearmanDist(double d1[], double d2[])
    {
        int r1[] = rankTransform(d1);
        int r2[] = rankTransform(d2);
        double sum = 0.0D;
        for(int i = 0; i < r1.length; i++)
        {
            int term = r1[i] - r2[i];
            sum += term * term;
        }

        return Math.abs((6D * sum) / (double)(r1.length * (r1.length * r1.length - 1)));
    }

    private int[] rankTransform(double d[])
    {
        int result[] = new int[d.length];
        for(int i = 0; i < d.length; i++)
        {
            int rank = 0;
            for(int j = 0; j < d.length; j++)
                if(i != j && d[j] < d[i])
                    rank++;

            result[i] = rank;
        }

        return result;
    }

    private double pearsonDist(double d1[], boolean m1[], double w1[], double d2[], boolean m2[], double w2[], boolean abs)
    {
        double EPSILON = 1E-10D;
        double mean1 = 0.0D;
        double wt1 = 0.0D;
        double mean2 = 0.0D;
        double wt2 = 0.0D;
        int commonCount = 0;
        for(int i = 0; i < d1.length; i++)
            if(!m1[i] && !m2[i])
            {
                mean1 += d1[i] * w1[i];
                wt1 += w1[i];
                mean2 += d2[i] * w2[i];
                wt2 += w2[i];
                commonCount++;
            }

        if(wt1 < 1E-10D || wt2 < 1E-10D || commonCount < 3)
            return 1.0D;
        mean1 /= wt1;
        mean2 /= wt2;
        double numerator = 0.0D;
        double nwt = 0.0D;
        double denom1 = 0.0D;
        double denom1wt = 0.0D;
        double denom2 = 0.0D;
        double denom2wt = 0.0D;
        for(int i = 0; i < d1.length; i++)
            if(!m1[i] && !m2[i])
            {
                double wt = w1[i] * w2[i];
                numerator += wt * (d1[i] - mean1) * (d2[i] - mean2);
                nwt += wt;
                denom1 += w1[i] * (d1[i] - mean1) * (d1[i] - mean1);
                denom1wt += w1[i];
                denom2 += w2[i] * (d2[i] - mean2) * (d2[i] - mean2);
                denom2wt += w2[i];
            }

        if(nwt < 1E-10D || denom1wt < 1E-10D || denom2wt < 1E-10D)
            return 1.0D;
        numerator /= nwt;
        denom1 /= denom1wt;
        denom2 /= denom2wt;
        double coeff = numerator / Math.sqrt(denom1 * denom2);
        if(Double.isNaN(coeff))
            return 1.0D;
        if(abs)
        {
            coeff = Math.abs(coeff);
            if(coeff > 1.0D)
                return 0.0D;
            else
                return 1.0D - coeff;
        }
        if(coeff > 1.0D)
            return 0.0D;
        if(coeff < -1D)
            return 1.0D;
        else
            return coeff * -0.5D + 0.5D;
    }

    private double pearsonDist(double x[], double y[])
    {
        double sumx = 0.0D;
        double sumy = 0.0D;
        double sumxx = 0.0D;
        double sumyy = 0.0D;
        double sumxy = 0.0D;
        for(int i = 0; i < x.length; i++)
        {
            sumx += x[i];
            sumy += y[i];
            sumxx += x[i] * x[i];
            sumyy += y[i] * y[i];
            sumxy += x[i] * y[i];
        }

        if(Math.abs((sumxy - (sumx * sumy) / (double)x.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)x.length) * (sumyy - (sumy * sumy) / (double)x.length))) > 1.0D)
            System.out.println("r=" + Math.abs((sumxy - (sumx * sumy) / (double)x.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)x.length) * (sumyy - (sumy * sumy) / (double)x.length))));
        return 1.0D - Math.abs((sumxy - (sumx * sumy) / (double)x.length) / Math.sqrt((sumxx - (sumx * sumx) / (double)x.length) * (sumyy - (sumy * sumy) / (double)x.length)));
    }

    public void normalizeDistances()
    {
        if(metric.equalsIgnoreCase("e"))
        {
            double maxDist = (-1.0D / 0.0D);
            double minDist = (1.0D / 0.0D);
            for(int i = 0; i < distResult.length; i++)
                if(distResult[i] < (1.0D / 0.0D) && distResult[i] > (-1.0D / 0.0D))
                    if(distResult[i] < minDist)
                        minDist = distResult[i];
                    else
                    if(distResult[i] > maxDist)
                        maxDist = distResult[i];

            double scale = maxDist - minDist;
            System.out.println("max=" + maxDist + "\tmin=" + minDist + "\tscale=" + scale);
            for(int i = 0; i < distResult.length; i++)
                if(distResult[i] == (1.0D / 0.0D) || distResult[i] == (-1.0D / 0.0D))
                {
                    distResult[i] = 0.0D;
                } else
                {
                    distResult[i] /= scale;
                    if(distResult[i] > 1.0D)
                        distResult[i] = 1.0D;
                    distResult[i] = 1.0D - distResult[i];
                }

        } else
        {
            for(int i = 0; i < distResult.length; i++)
                distResult[i] = 1.0D - distResult[i];

        }
    }

    public boolean writeGeneTreeFile(String filename)
    {
        normalizeDistances();
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
            bw.write("NODEID\tLEFT\tRIGHT\tCORRELATION");
            bw.newLine();
            for(int i = 0; i < treeResult.length; i++)
            {
                bw.write("NODE" + (-i - 1) + "\t");
                if(treeResult[i][0] < 0)
                    bw.write("NODE" + treeResult[i][0] + "\t");
                else
                    bw.write("GENE" + treeResult[i][0] + "\t");
                Double d = new Double(distResult[i]);
                if(d.isNaN())
                    System.err.println("NaN found");
                if(treeResult[i][1] < 0)
                    bw.write("NODE" + treeResult[i][1] + "\t" + distResult[i]);
                else
                    bw.write("GENE" + treeResult[i][1] + "\t" + distResult[i]);
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

    public boolean writeArrayTreeFile(String filename)
    {
        normalizeDistances();
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
            bw.write("NODEID\tLEFT\tRIGHT\tCORRELATION");
            bw.newLine();
            for(int i = 0; i < treeResult.length; i++)
            {
                bw.write("ARNODE" + (-i - 1) + "\t");
                if(treeResult[i][0] < 0)
                    bw.write("ARNODE" + treeResult[i][0] + "\t");
                else
                    bw.write("ARRAY" + treeResult[i][0] + "\t");
                if(treeResult[i][1] < 0)
                    bw.write("ARNODE" + treeResult[i][1] + "\t" + distResult[i]);
                else
                    bw.write("ARRAY" + treeResult[i][1] + "\t" + distResult[i]);
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

    public int treeResult[][];
    public double distResult[];
    public String metric;
}
