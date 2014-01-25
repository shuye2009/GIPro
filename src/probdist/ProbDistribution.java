package probdist;

/**
 * http://www.fortunecity.co.uk/meltingpot/back/340/product/java/cdfdemomain.html
 * ProbDistribution is an abstract superclass for probability graphs.
 *
 * The following files should be distributed with ProbDistribution.java:
 * 
 *   Discrete.java
 *       BinomialDist.java
 *       GeometricDist.java
 *       HyperGeomDist.java
 *       NegBinomialDist.java
 *       PoissonDist.java
 *   Continuous.java
 *       BetaDist.java
 *       CauchyDist.java
 *       ChiSquareDist.java
 *       ExponentialDist.java
 *       FTestDist.java
 *       GammaDist.java
 *       GumbelDist.java
 *       LogNormalDist.java
 *       NormalDist.java
 *       RayleighDist.java
 *       StudentDist.java
 *       WeibullDist.java
 *   DiscreteProbability.html
 * 
 *   To run, each applet requires the presence of ProbDistribution.class
 *   and either Discrete.class or Continuous.class, according to its type.
 * 
 * WHAT'S GOING ON HERE:
 * 
 * You can browse the DiscreteProbability.html file to get the feel of the
 * applet's capabilities.  It just basically displays a graph and a number
 * with seven digits of precision.  A lot of features such as calibration
 * ticks, buttons, and menus have been omitted because 1) we want the applet
 * to load fast, 2) the author is still a newcomer to java, 3) the program
 * is free for gosh sakes.
 * 
 * If you devise an enhancement to the interface or display, or (extra
 * credit!) if you add a new distribution. please EMail it to me.  You will
 * be credited with it in the next upgrade.
 *
 * NOTES ON THE NUMERICAL RECIPES:
 *
 * Where possible, discrete probabilities are calculated using Chebyshev 
 * series.  Thus the binomial cumulative density is an incomplete beta 
 * function, and the poisson, an incomplete gamma function calculation.
 * Certain series, such as geometric cumulative probability, can be 
 * calculated ( CDF = 1 - (1-p)^x ) without recourse to convergent series.
 *
 * The remaining discrete probabilities are calculated as the sum of pdfs.
 * The double floating point type is capable of representing a number as
 * large as 1E+30800 or as small as 1E-30800, and in most cases this is
 * adequate for discrete sums.
 *
 * In continuous distributions whose integral is not a simple algebraic 
 * expression (normal, chi-square, ..), an incomplete gamma or incomplete 
 * beta function is used.  In rare cases, a Taylor series formula is used 
 * if it is more accurate.  The test for convergence is generally 
 * ABS(nth-term) < 1E-13.
 *
 * The recipes below, beginning with betai(a,b,x), were transcribed from
 * "Numerical Recipes in C", Second Edition, by Press, Teukolsky, 
 * Vetterling, and Flannery, Cambridge University Press, 1992.
 *
 * HISTORY:
 *
 * ProbDistribution.java was translated from a DLL written in C++ for a
 * Visual Basic application Geewhiz.exe.  The DLL itself was translated
 * from a Dos program CDF.EXE written in C.  Both programs are available
 * through Newfangled Software.  
 *
 * (It turns out numerical methods in C can be pasted directly into java,
 * with only the addition of a class, i.e., 'Math.exp()' for 'exp()'.)
 *
 * The two programs had an eight-digit accuracy in all Cdf and inverse Cdf
 * results.  That was author's claim, and in benchmark tests against known
 * packages such as MatLab, Excel, and SPSS, the claim was never refuted.
 * I have no idea if this accuracy holds in Java, especially on diverse
 * platforms, although the Windows 95 implementation of the double precision
 * floating type seems quite reliable.  I welcome comments and suggestions.
 *
 * FREEWARE CONSIDERATIONS:
 *
 * Newfangled Software does not agree to, and does not profit from, retail
 * sales of these programs except through freeware registration.  You can
 * give this program away, but you can't sell it.  Register by mailing
 * $10.00 to Newfangled Software, 1015 Island Drive Court 102, Ann Arbor,
 * MI  48105.  You may also register by sending the author any constructive
 * suggestion or improvement.
 *
 * The zipped file is verified to be virus-free, but it is still a good
 * habit to scan for viruses before using this or any other new software.
 *
 * POLITICAL CORRECTNESS:
 *
 * No animals were harmed in the making of these applets.
 *
 * author: John Bohr
 */
import java.applet.Applet;
import java.awt.Graphics;
import java.awt.Color;

abstract class ProbDistribution extends Applet {

    public static final int      ITMAX        =     300;
    public static final double   FPMIN        = 1.0E-50;
    public static final double   EPSILON      = 1.0E-20;
    public static final double   DELTA        = 1.0E-13;
    public static final double   LANCZ_CUTOFF =   700.0;
    public static final int      MAXPOINTS    =    8000;

    public static final double   DFLT_AXIS_RATIO = 0.93;
    public static final double   DFLT_TOP_RATIO =  0.05;
    public static final double   DFLT_BOT_RATIO =  0.12;
    public static final boolean  DFLT_SHOWARGS  =  true;
    public static final boolean  DFLT_SHOWRESULT=  true;
    public static final boolean  DFLT_SHOW_PDF  = false;

//    protected Color     app_bg_color;   // real soon now

    protected String    DistName;
    protected String    Param_name[];
    protected String    PARAM_arg[];
    protected double    Param_doubval[];
    protected double    RndX;
    protected double    ProbInverse=0.0;
    protected int       argscount;
    protected boolean   IsDiscrete=false;

    protected String    PARAM_showparms  = "showparms";
    protected String    PARAM_showresult = "showresult";
    protected String    PARAM_PdfType    = "density";
    protected String    PARAM_InvValue   = "inverseprob";
    protected String    PARAM_randomvar  = "randomvar";
    protected String    ParamName_X = "X";

    protected double    axisratio;
    protected double    plottop;
    protected double    plotbottom;
    protected double    plotheight;
    protected int       graphtop;
    protected double    functionmax;
    protected double    xlo=0.0;
    protected double    xhi;

    protected int       axismargin;
    protected int       baseline; 
    protected int       apheight; 
    protected int       apwidth;

    protected boolean   showresult;
    protected boolean   showparms;
    protected boolean   pdf_flavor;
    protected boolean   cdf_flavor;
    protected boolean   inv_flavor;
    protected boolean   legendright=false;

    private static boolean BadBetacf;

    // ===== methods =============
    //
    public abstract void SetDiscreteness();

    public abstract void SetIntrinsicValues();

    public abstract void GetArgValues();

    public abstract void paint_draw(Graphics g, double frescale);

    public abstract double GetPDFMax();

    protected void Finish_Init() {
        if (inv_flavor) RndX = InverseCDF(ProbInverse);
    }

    protected double CDFvalue(double fX) {
        return (0.0);
    }

    protected double mass(double fX) {
        return (0.0);
    }

    protected double InverseCDF(double p) {
        return (0.0);
    }

    protected void paint_calibrate(Graphics g) {
    }


    protected void GetDisplayArgs() {
        String param;
        double temp;
        Double D;

        showparms = GetBooleanParm(this, PARAM_showparms, DFLT_SHOWARGS );
        showresult = GetBooleanParm(this, PARAM_showresult, DFLT_SHOWRESULT );
        inv_flavor = pdf_flavor = cdf_flavor = false;
        /*
            A point probability (pdf) is not defined for continuous 
            distributions.  The mass(x) function is only used for
            drawing graphics lines.
        */
        if (IsDiscrete)
            pdf_flavor = GetBooleanParm(this, PARAM_PdfType, DFLT_SHOW_PDF);

        if ( !pdf_flavor ) {
            param = getParameter(PARAM_InvValue);
            if (param != null) {
                D = Double.valueOf(param);
                temp = D.doubleValue();
                if ((temp > 0.0) && (temp < 1.0)) {
                    ProbInverse = temp;
                    inv_flavor = true;
                }
            }
            cdf_flavor = !inv_flavor;
        }
    }


    protected void initplotting() {  
        axismargin = (int)((1.0 - axisratio) * apwidth / 2.0);
        plotheight = (1.0 - plottop - plotbottom) * (double)apheight;
        baseline = (int)((1.0 - plotbottom) * apheight);
        graphtop = (int)(plottop * apheight);
    }


    protected void paint_start(Graphics g) {
       g.setColor(Color.lightGray);
       g.fill3DRect(1, 1, apwidth-1, apheight-1, true);
       g.setColor(Color.white);
       g.fillRect(3, 3, apwidth-5, apheight-5);
    }


    protected void paint_axis(Graphics g) {
       if (IsDiscrete) {
          g.setColor(Color.gray);
          g.drawLine(axismargin, baseline + 1, apwidth - axismargin - 1, baseline + 1);
       }
    }


    protected void paint_finish(Graphics g, boolean bRight) {
       int i, y, xpos;
       String cdfresult;
       float ftemp;

       g.setColor(Color.gray);
       g.drawLine(axismargin, baseline + 1, apwidth - axismargin - 1, baseline + 1);
       g.setColor(Color.black);
       if (showparms) {
           xpos = (bRight) ? apwidth - 85 : axismargin;
           y    = (bRight) ? 23 : 18;
           for (i=0; i<argscount; i++) {
               g.drawString( Param_name[i] + " = " + String.valueOf(Param_doubval[i]), xpos, y );
               y += 15;
           }
           if (IsDiscrete) {
               i = (int)RndX;
               g.drawString( ParamName_X + " = " + String.valueOf(i), xpos, y );
           }
           else {
               g.drawString( ParamName_X + " = " + String.valueOf(RndX), xpos, y );
           }
       }
       if ((showresult) && (apheight - baseline >= 12)) {
           if (inv_flavor) 
               ftemp = (float)ProbInverse;
           else if (pdf_flavor) 
               ftemp = (float)mass(RndX);
           else 
               ftemp = (float)CDFvalue(RndX);
           cdfresult = DistName + " probability = " + String.valueOf(ftemp);
           g.drawString( cdfresult, axismargin, apheight - 6 );
       }
    }


    public void init() {

        axisratio = DFLT_AXIS_RATIO;
        plottop = DFLT_TOP_RATIO;
        plotbottom = DFLT_BOT_RATIO;

        apheight = size().height;
        apwidth = size().width;

        SetDiscreteness();
        SetIntrinsicValues();
        GetArgValues();
        GetDisplayArgs();
        functionmax = GetPDFMax();
        initplotting();
        setBackground(Color.white);
        Finish_Init();
    }


    public void paint(Graphics g) {
       double plotscale;

       paint_start(g);
       plotscale = plotheight / functionmax;
       paint_draw(g, plotscale);
       paint_axis(g);
       paint_calibrate(g);
       paint_finish(g, legendright);
    }


    public static boolean GetBooleanParm(Applet applet, String s, boolean bDefault) {
        String param;
        int temp;

        param = applet.getParameter(s);
        if (param != null) {
            if (param.equalsIgnoreCase("true"))
                return (true);
            else if (param.equalsIgnoreCase("false"))
                return (false);
            else if (param.equalsIgnoreCase("yes"))
                return (true);
            else if (param.equalsIgnoreCase("no"))
                return (false);
            else { 
                temp = Integer.parseInt(param);
                return (temp != 0);
            }
        } 
        else {
            return (bDefault);
        }
    }


    /* --------------------------------------------------------------------
       LogBinCoef     returns the log of the binomial coefficient.
                      The method used depends on n.
    -------------------------------------------------------------------- */
    protected static double LogBinCoef(double n, double k) {
	double Lbc;

        if ( n >= LANCZ_CUTOFF )

            Lbc = 1.0 - 0.5 * Math.log(2.0 * Math.PI) + (1.5 + n) * Math.log(n + 1.0) -
	          (1.5 + k) * Math.log(k + 1.0) - (1.5 + n - k) * Math.log(n - k + 1.0);
	else 
	    Lbc = Lanczos(n + 1.0) - Lanczos(k + 1.0) - Lanczos(n - k + 1.0);

        return (Lbc);
    }

    /* --------------------------------------------------------------------
       LogStirling  =  LOG( sqr(2Pi)n^(n+.5)exp(-n) )	
    -------------------------------------------------------------------- */
    protected static double LogStirling(double p) {
        return ( 0.5 * Math.log(2.0 * Math.PI) + (0.5 + p) * Math.log(p) - p );
    }


    /* --------------------------------------------------------------------
        BisectCDF() find the inverse of F() using bisection
    -------------------------------------------------------------------- */
    protected double BisectCDF(double x_lo, double x_hi, double prob) {
        int k=0, Iters;
        double hi, lo, mid, cdf;

        Iters = 60;  // assuring 18 digits of decimal precision
        hi = x_hi;
        lo = x_lo;
        mid = 0.5 * (hi + lo);

        // 1) determine whether the CDF is computable at all 
        if (CDFvalue(mid) < 0.0) 
            return (x_lo);

        // 2) extend the right bisection window endpoint if necessary 
        k = 0;
        do {
            cdf = CDFvalue(hi);
            if ((cdf >= prob) || (cdf < 0.0)) break;
            hi += 5.0;
        } while (++k < ITMAX);

        if (k > 0) 
            lo = x_hi;
        else {
        // 3) extend the left end of the bisection window if necessary 
            k = 0;
            do {
                cdf = CDFvalue(lo);
                if (cdf <= prob) break;
                lo -= 5.0;
            } while (++k < ITMAX);
            if (k > 0) hi = x_lo;
        }

        // 4) bisect the puppy 
        k = 0;
        do {
            mid = 0.5 * (hi + lo);
            if (IsDiscrete) mid = Math.floor(mid + 0.001);
            cdf = CDFvalue(mid);
            if (cdf < 0) {  // error
                mid = x_lo;
                break;
            }
            if (Math.abs(cdf - prob) < EPSILON) 
                break;
            if (cdf > prob) 
                hi = mid;
            else 
                lo = mid;
            k++;
        } while ((hi-lo > EPSILON) && (k < Iters));

        return (mid);
    }



    /* **********
    *
    *  The following recipes are transcribed from "Numerical Recipes in C", 
    *  Second Edition, by Press, Teukolsky, Vetterling, and Flannery, 
    *  Cambridge University Press, 1992.
    *
    * */

	/* --------------------------------------------------------------------
	     double betai(a, b, x)
	-------------------------------------------------------------------- */
	protected static double betai(double a, double b, double x) {
	     int blowup;
	     double retval, bt;

	     if (x < 0.0 || x > 1.0) retval = -1.0;
	     else if (x == 0.0 || x == 1.0) retval = 0;
	     else {
	          BadBetacf = false;
	          bt = Math.exp( Lanczos(a+b) - Lanczos(a) - Lanczos(b) +
	                            a*Math.log(x) + b*Math.log(1.0-x) );
	          if (x < (a + 1.0) / (a + b + 2.0))
	                retval = bt * betacf(a, b, x) / a;
	          else retval = 1.0 - bt * betacf(b, a, 1.0 - x) / b;
	          if (BadBetacf) retval = -1.0;
	     }
	     return (retval);
	}


	/* --------------------------------------------------------------------
	     double betacf(a,b,x,&ok)
	     continued fraction for incomplete beta function
	     by modified Lentz's method
	-------------------------------------------------------------------- */
	protected static double betacf(double a, double b, double x ) {
	     int m, m2;
	     double retval, aa, c, d, del, qab, qam, qap;

	     BadBetacf = false;
	     qab = a + b;
	     qap = a + 1.0;
	     qam = a - 1.0;
	     c = 1.0;
	     d = 1.0 - qab * x / qap;
	     if (Math.abs(d) < FPMIN) d = FPMIN;
	     d = 1.0 / d;
	     retval = d;
	     for (m=1; m<=ITMAX; m++) {
	          m2 = m + m;
	          aa = (b-m) * m * x / ( (qam+m2) * (a+m2) );
	          d = 1.0 + aa * d;
	          if (Math.abs(d) < FPMIN) d = FPMIN;
	          c = 1.0 + aa / c;
	          if (Math.abs(c) < FPMIN) c = FPMIN;
	          d = 1.0 / d;
	          retval *= d * c;
	          aa = 0.0 - (a+m) * (qab+m) * x / ( (a+m2) * (qap+m2) );
	          d = 1.0 + aa * d;
	          if (Math.abs(d) < FPMIN) d = FPMIN;
	          c = 1.0 + aa / c;
	          if (Math.abs(c) < FPMIN) c = FPMIN;
	          d = 1.0 / d;
	          del = d * c;
	          retval *= del;
	          if (Math.abs(del - 1.0) < EPSILON) break;
	     }
	     if (m > ITMAX) 
	          BadBetacf = true;

	     return (retval);
	}


	/* --------------------------------------------------------------
	    Lanczos(p)    Lanczos's neat approximation of log( �(p) )
	    --slightly modified for speed
	-------------------------------------------------------------- */
	protected static double Lanczos(double p) {
	   int j;
	   double x, tmp, ser;

	   x = p;
	   tmp = x + 5.5;
	   tmp = tmp - (x + .5) * Math.log(tmp);
	   ser = 1.000000000190015 + 76.18009172947146 / ( p + 1.0 );
	   ser -= 86.50532032941678 / ( p + 2.0 );
	   ser += 24.01409824083091 / ( p + 3.0 );
	   ser -= 1.231739572450155 / ( p + 4.0 );
	   ser += .001208650973866179 / ( p + 5.0 );
	   ser -= 5.395239384953E-06 / ( p + 6.0 );
	   return (Math.log(2.506628274631001 * ser / x) - tmp);
	}


	/* --------------------------------------------------------------------
		 FUNCTION gammp (a, x)
	-------------------------------------------------------------------- */
	protected static double gammp(double a, double x) {
	    double retval, g;

	    if (x < 0 || a <= 0) 
	        retval = -1.0;

	    else {
	        if (a >= LANCZ_CUTOFF) 
	            g = LogStirling(a);
	        else 
	            g = Lanczos(a);

		if (x < a + 1.0) 
	            retval = gser(a, x, g);
	        else if ((g = gcf(a, x, g)) < 0) 
	            retval = g;
	        else 
	            retval = 1.0 - g;
	    }
	    return (retval);
	}


	/* --------------------------------------------------------------------
		 double gammq(double a, double x)
	-------------------------------------------------------------------- */
	protected static double gammq(double a, double x) {
	     double retval, g;

	     if (x < 0 || a <= 0) 
	         retval = -1.0;

	     else {
	         if (a >= LANCZ_CUTOFF) 
	             g = LogStirling(a);
	         else 
	             g = Lanczos(a);

	         if (x >= a + 1.0) 
	             retval = gcf(a, x, g);
	         else if ((g = gser(a, x, g)) < 0) 
	             retval = g;
	         else 
	             retval = 1.0 - g;
	     }
	     return (retval);
	}


	/* --------------------------------------------------------------------
		 double gcf (a, x, g)
	   '
	   '  returns the incomplete gamma function Q(a,x)
	   '  g is Lanczos(a)
	   '
	-------------------------------------------------------------------- */
	protected static double gcf(double a, double x, double g) {
	    int i;
	    double retval, an, b, c, d, del, h;

	    b = x + 1.0 - a;
	    c = 1.0 / FPMIN;
	    d = 1.0 / b;
	    h = d;
	    for (i=1; i<=ITMAX; i++) {
	        an = i * (a - i);
	        b += 2.0;
	        d = an * d + b;
	        if (Math.abs(d) < FPMIN) d = FPMIN;
	        c = b + an / c;
	        if (Math.abs(c) < FPMIN) c = FPMIN;
	        d = 1.0 / d;
	        del = d * c;
	        h *= del;
	        if (Math.abs(del - 1.0) < EPSILON) break;
	    }

	    if (i > ITMAX) retval = -1.0;
	    else retval = Math.exp(a * Math.log(x) - x - g) * h;

	    return (retval);
	}


	/* --------------------------------------------------------------------
		 double gser (a, x, g)
		'
		'  returns the incomplete gamma function P(a,x)
		'  g is Lanczos(a)
		'
	-------------------------------------------------------------------- */
	protected static double gser(double a, double x, double g) {
	    int i;
	    double sum, del, ap, retval=-1.0;

	    if (x == 0.0) {
	         retval = 0.0;
	    }  
	    else if (x > 0) {
	        ap = a;
	        sum = 1.0 / a;
	        del = sum;
	        for (i=1; i<=ITMAX; i++) {
	            ap += 1.0;
	            del *= x / ap;
	            sum += del;
	            if (Math.abs(del) < Math.abs(sum) * EPSILON) {
	                retval = sum * Math.exp(a * Math.log(x) - x - g);
                        break;
	            }
	       }
	    }  
	    return (retval);
	}
}