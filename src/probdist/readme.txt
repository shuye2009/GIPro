
  ProbDistribution.java is a superclass for 17 probability distributions.  

  Probdist.zip contains 20 java files and 3 illustrative html files.

  ProbDistribution.java       (abstract class)
     Discrete.java            (abstract class)
        BinomialDist.java
        GeometricDist.java
        HyperGeomDist.java
        NegBinomialDist.java
        PoissonDist.java
     Continuous.java          (abstract class)
        BetaDist.java
        CauchyDist.java
        ChiSquareDist.java
        ExponentialDist.java
        FTestDist.java
        GammaDist.java
        GumbelDist.java
        LogNormalDist.java
        NormalDist.java
        RayleighDist.java
        StudentDist.java
        WeibullDist.java

  ---------------------

  ARGUMENTS:

     randomvar ....... (double) value at which the probability should be
                       evaluated.  Example:

                       <param name=randomvar value="-1.0">

     showparms ....... (boolean) argument values should be printed
                       <param name=showparms value="yes">

     showresult ...... (boolean) the probability value should be printed
                       <param name=showresult value="false">

     density ......... (boolean) a point probability should be computed
                       rather than the default calculation, cumulative
                       probability.  Note that 'density' is not recognized
                       in continuous distributions.
                       <param name=density value="true">

     inverseprob ..... (double) the random variable should be computed from
                       this cumulative probability.  Useful in percentiles
                       and confidence intervals.
                       <param name=inverseprob value="0.95">

     <mu, sigma ..> .. each distribution has its own set of parameter names.
                       In the Normal distribution, you would use e.g.
                       <param name=mu    value="0.0">
                       <param name=sigma value="1">

  ---------------------

    Features such as buttons, menus, calibration ticks, have been omitted
    because 1) we want the applet to load fast, 2) the author is still a
    newcomer to java, 3) the program is free for gosh sakes.
  
    If you devise an enhancement to the interface or display, please EMail
    it.  You will be credited with it in the next upgrade.
 
  NOTES ON THE NUMERICAL RECIPES:
 
    Discrete probabilities: the calculations use a Chebyshev series where
    possible.  Thus the binomial cumulative density is an incomplete beta 
    function, and the poisson, an incomplete gamma function calculation.
    The geometric cumulative probability simplifies to F(x) = 1 - (1-p)^x.

    The remaining discrete probabilities are calculated as the sum of pdfs.
    The double floating point type represents a number as large as 1E+30800
    or as small as 1E-30800, which is generally adequate for discrete sums.
 
    Continuous distributions: when the integral is not a simple algebraic 
    expression (normal, chi-square, ..), an incomplete gamma or incomplete 
    beta function is used.  The test for convergence is generally 
    ABS(nth-term) < 1E-13.
 
    The inverse CDFs are found by bisection when there is not a simpler
    method.  Iterations terminate when ABS(high-low) < 1E-11.

    The main source for the numerical methods is "Numerical Recipes in C",
    2nd Edition, by Press, Teukolsky, Vetterling, and Flannery, Cambridge
    University Press, 1992.
 
  HISTORY:
 
    ProbDistribution.java was translated from a DLL written in C++ for a
    Visual Basic application Geewhiz.exe.  The DLL itself was translated
    from a Dos program, CDF.EXE, originally developed in C as a teaching
    tool for a statistics course at the University of Michigan.  Both
    programs are available from Newfangled Software.  
 
    (It turns out numerical methods in C can be pasted directly into java,
    with only the addition of a class, i.e., 'Math.exp()' for 'exp()'.)
 
    The two programs had an eight-digit accuracy in all Cdf and inverse Cdf
    results.  That was author's claim, and in benchmark tests against known
    packages such as MatLab, Excel, and SPSS, the claim was never refuted.
    It is not clear whether the accuracy holds in Java, especially on diverse
    platforms, although the Windows 95 implementation of the double precision
    floating type seems quite reliable.  I welcome comments and suggestions.
 
  FREEWARE CONSIDERATIONS:
 
    Newfangled Software does not agree to, and does not profit from, retail
    sales of these programs except through freeware registration.  You can
    give this program away, but you can't sell it.  Make as many copies as
    you like.  If you find it to be a useful tool, please register by mailing 
    $10.00 to Newfangled Software, 1996 Pauline #2B, Ann Arbor, MI 48103.
    You may also register by sending the author any constructive suggestion 
    or improvement at jnbohr@netscape.net
 
    The zipped file is verified to be virus-free, but it is still a good
    habit to scan for viruses before using this or any other new software.
 
  POLITICAL CORRECTNESS:
 
    No animals were harmed in the making of these programs.


author: John Bohr