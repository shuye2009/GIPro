package probdist;


/**
 * http://www.fortunecity.co.uk/meltingpot/back/340/product/java/cdfdemomain.html
 */

public class StudentDist extends Continuous {

    int df;
    double ddf;
    double Tcoefficient;

    public void SetIntrinsicValues() {

        DistName = "Student t";
        argscount = 1;
        Param_name = new String[argscount];
        Param_doubval = new double[argscount];
        PARAM_arg = new String[argscount];

        Param_name[0] = "df";
        PARAM_arg[0] = "DegreesFreedom";

        df = 8;
        RndX = 0.5;     

        symmetric = true;
        legendright = false;
    }

    public void GetArgValues() {
        String param;
        Double D;
        double dtemp;

        param = getParameter(PARAM_arg[0]);
        if (param != null) {
            D = Double.valueOf(param);
            dtemp = D.doubleValue();
            if ((dtemp >= 1.0) && (dtemp <= (double)MAXPOINTS))
                df = (int)dtemp;
        }

        param = getParameter(PARAM_randomvar);
        if (param != null) {
            D = Double.valueOf(param);
            RndX = D.doubleValue();
        }

        ddf = (double)df;
        Param_doubval[0] = ddf;

        Tcoefficient = Math.exp( Lanczos(0.5*(ddf+1.0)) - Lanczos(0.5*ddf) ) / Math.sqrt(ddf * Math.PI);
        dtemp = 0.0003 / Tcoefficient;
        xhi = 0.0;
        do xhi += 5.0; while ( mass(xhi) > dtemp );
        xlo = 0 - xhi;
    }


    public double CDFvalue(double fX, double df) {
        ddf = df;
        double area, v, z;

        if (Math.abs(fX)<EPSILON) {
            area = 0.5;
        }
        else {
            z = ddf / (ddf + fX * fX);          /* betai arg */
            v = 0.5 * betai( 0.5 * ddf, 0.5, z);
            area = (fX <= 0.0) ? v : 1.0 - v;
            if (area > 1.0) area = -1.0;
        }
        return ( area );
    }


    public double GetPDFMax() {
        return ( Tcoefficient );
    }


    public double mass(double fX) {
        double Tlog;

        Tlog = Math.log( 1.0 + fX * fX / ddf );
        return ( Tcoefficient * Math.exp( Tlog * (0.0 - 0.5 * (ddf + 1.0)) ));
    }


    public double InverseCDF(double prob) {
        return ( BisectCDF(xlo, xhi, prob) );
    }
}
