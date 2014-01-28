package plugin;

//ContigencyTable.java
//
// (c) 1999-2001 PAL Development Core Team
//
// This package may be distributed under the
// terms of the Lesser GNU General Public License (LGPL)

/**
 * This does a Fisher Exact test.  The Fisher's Exact test procedure calculates an exact probability value
 * for the relationship between two dichotomous variables, as found in a two by two crosstable. The program
 * calculates the difference between the data observed and the data expected, considering the given marginal
 * and the assumptions of the model of independence. It works in exactly the same way as the Chi-square test
 * for independence; however, the Chi-square gives only an estimate of the true probability value, an estimate
 * which might not be very accurate if the marginal is very uneven or if there is a small value (less than five)
 * in one of the cells.
 *
 * It uses an array of factorials initialized at the beginning to provide speed.
 * There could be better ways to do this.
 *
 * @author Ed Buckler
 * @version $Id: FisherExact.java,v 1
 */

public class FisherExact {
    private double[] f;
    int maxSize;


    /**
     * constructor for FisherExact table
     *
     * @param maxSize is the maximum sum that will be encountered by the table (a+b+c+d)
     */
    public FisherExact(int maxSize) {
        this.maxSize = maxSize;
        f = new double[maxSize + 1];
        f[0] = 0.0;
        for (int i = 1; i <= this.maxSize; i++) {
            f[i] = f[i - 1] + Math.log(i);
        }
    }

    public int getMax(){
    	return maxSize;
    }
    /**
     * calculates the P-value for this specific state
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return the P-value
     */
    public final double getP(int a, int b, int c, int d) {
        if(a < 0) a = 0;
        if(b < 0) b = 0;
        if(c < 0) c = 0;
        if(d < 0) d = 0;
        
        
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p;
        try{
        p = (f[a + b] + f[c + d] + f[a + c] + f[b + d]) - (f[a] + f[b] + f[c] + f[d] + f[n]);
        }catch(Exception e){
            System.out.println("total="+maxSize+"\ta="+a+"\tb="+b+"\tc="+c+"\td="+d);
            e.printStackTrace();
            p=0;
        }
        return Math.exp(p);
    }

    /**
     * Calculates the one-tail P-value for the Fisher Exact test.  Determines whether to calculate the right- or left-
     * tail, thereby always returning the smallest p-value.
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return one-tailed P-value (right or left, whichever is smallest)
     */
    public final double getCumlativeP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;

        p += getP(a, b, c, d);
       
        if ((a * d) >= (b * c)) {
            min = (c < b) ? c : b;
            for (i = 0; i < min; i++) {
                p += getP(++a, --b, --c, ++d);
            }
        }
        if ((a * d) < (b * c)) {
            min = (a < d) ? a : d;
            for (i = 0; i < min; i++) {
                double pTemp = getP(--a, ++b, ++c, --d);
                p += pTemp;
            }
        }
        return p;
    }

    /**
     * Calculates the right-tail P-value for the Fisher Exact test.
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return one-tailed P-value (right-tail)
     */
    public final double getRightTailedP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;

        p += getP(a, b, c, d);
        min = (c < b) ? c : b;
        for (i = 0; i < min; i++) {
            p += getP(++a, --b, --c, ++d);

        }
        return p;
    }

       /**
     * Calculates the right-tail P-value for the Fisher Exact test.
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return one-tailed P-value (right-tail)
     */
    public final double getRightTailedPQuick(int a, int b, int c, int d, double maxP) {
        int min, i;
        double p = 0;

        p += getP(a, b, c, d);
        min = (c < b) ? c : b;
        for (i = 0; (i < min) && (p<maxP); i++) {
            p += getP(++a, --b, --c, ++d);

        }
        return p;
    }

    /**
     * Calculates the left-tail P-value for the Fisher Exact test.
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return one-tailed P-value (left-tail)
     */
    public final double getLeftTailedP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;

        p += getP(a, b, c, d);
        min = (a < d) ? a : d;
        for (i = 0; i < min; i++) {
            double pTemp = getP(--a, ++b, ++c, --d);
            p += pTemp;
        }
        return p;
    }


    /**
     *   Calculates the two-tailed P-value for the Fisher Exact test.
     *
     *   In order for a table under consideration to have its p-value included
     *   in the final result, it must have a p-value less than the original table's P-value, i.e.
     *   Fisher's exact test computes the probability, given the observed marginal
     *   frequencies, of obtaining exactly the frequencies observed and any configuration more extreme.
     *   By "more extreme," we mean any configuration (given observed marginals) with a smaller probability of
     *   occurrence in the same direction (one-tailed) or in both directions (two-tailed).
     *
     * @param a     a, b, c, d are the four cells in a 2x2 matrix
     * @param b
     * @param c
     * @param d
     * @return two-tailed P-value
     */
    public final double getTwoTailedP(int a, int b, int c, int d) {
        int min, i;
        int n = a + b + c + d;
        if (n > maxSize) {
            return Double.NaN;
        }
        double p = 0;
        double baseP = getP(a, b, c, d);

        int initialA = a, initialB = b, initialC = c, initialD = d;
        p += baseP;
        min = (c < b) ? c : b;
        for (i = 0; i < min; i++) {
            double tempP = getP(++a, --b, --c, ++d);
            if (tempP <= baseP) {
                p += tempP;
            }
        }

        // reset the values to their original so we can repeat this process for the other side
        a = initialA;
        b = initialB;
        c = initialC;
        d = initialD;

        min = (a < d) ? a : d;
        for (i = 0; i < min; i++) {
            double pTemp = getP(--a, ++b, ++c, --d);
            if (pTemp <= baseP) {
                p += pTemp;
            }
        }
        return p;
    }
}