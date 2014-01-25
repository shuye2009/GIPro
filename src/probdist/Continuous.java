package probdist;

/*
 * http://www.fortunecity.co.uk/meltingpot/back/340/product/java/cdfdemomain.html
 * Copyright (c) 1998 Newfangled Software, Inc. All Rights Reversed.
 *
 */
import java.applet.Applet;
import java.awt.Graphics;
import java.awt.Color;

abstract class Continuous extends ProbDistribution {

    public static final int   X_INCREMENT  =  3;
    public static final int   X_INC_RIGHT  =  5;
    protected boolean         symmetric = false;
    protected boolean      finegraphics = false;


    public void SetDiscreteness() {
        IsDiscrete = false;
        PARAM_randomvar = "randomvar";
    }

    /* -------------------------------------------------------------
       StandInverse calculates the inverse of the standard normal 
       distribution via Newton-Raphson
    ------------------------------------------------------------- */
    protected double StandInverse(double Area) {
        int k=0;
        double CDF, f, p, z=0.1;

        CDF = .5 + Math.abs(Area - .5);
        do {
            p = 0.5 * gammp( 0.5, z * z / 2.0 );
            f = (z < 0) ? 0.5 - p : 0.5 + p;
            z -= Math.exp(.5 * z * z) * (f - CDF);
        } while ((Math.abs(f - CDF) > DELTA) && (ITMAX > k++));
        if (Area < .5) z = 0.0 - z;

        return (z);
    }

    public void paint_draw(Graphics g, double frescale) {
        int rightx, righty, lefty, i, j, y, dx, dy, xprev, yprev;
        double x, dtmp, xscale, z;

        xscale = (double)apwidth;
        xscale *= axisratio / (xhi - xlo);

        z = ( xhi < RndX ) ? xhi : RndX;

        rightx = axismargin + (int)((z - xlo) * xscale);
        if (rightx < 10) rightx = 10;

        righty = baseline + 1 - (int)(frescale * mass(z));
        if (righty < graphtop) righty = graphtop;

        xprev = axismargin;
        yprev = baseline + 1 - (int)(frescale * mass(xlo));
        if (yprev < graphtop) yprev = graphtop;
        lefty = yprev;

        g.setColor(Color.blue);
        g.drawLine(rightx, baseline + 1, axismargin, baseline + 1);
        g.drawLine(axismargin, baseline + 1, axismargin, yprev);
        dx = (finegraphics) ? 1 : X_INCREMENT;

        i = xprev;

        while (i<rightx) {    /* draw the left curve.  Plot and connect */
                              /* the ordinates in x-increments of 3     */
            i+=dx;
            if (i > rightx) {
                i = rightx;
                dx = rightx - xprev;
            }
            dtmp = i - axismargin;
            x = xlo + dtmp / xscale;
            y = baseline + 1 - (int)(frescale * mass(x));
            if (y < graphtop) y = graphtop;

            /* 'paint' the area of curve */
            if ((yprev < baseline + 1) || (y < baseline + 1)) {
                g.setColor(Color.cyan);
                dy = y - yprev;
                for (j=0; j<dx; j++) {
                    g.drawLine( xprev + j, baseline, xprev + j, yprev + 1 + j * dy / dx );
                }
                g.setColor(Color.blue);
            }
            g.drawLine(xprev,yprev,i,y);
            xprev = i;
            yprev = y;
        }
        g.drawLine(rightx, righty, rightx, baseline + 1);
        if (lefty < baseline + 1)
            g.drawLine(axismargin, lefty, axismargin, baseline + 1);

        /* draw the empty right tail */
        rightx = axismargin + (int)((xhi - xlo) * xscale);
        if ( i+X_INC_RIGHT <= rightx ) {
            g.setColor(Color.gray);
            g.drawLine( i, baseline + 1, rightx, baseline + 1);
            xprev = i;        
            while (i+X_INC_RIGHT < rightx) {
                i += X_INC_RIGHT;
                x = xlo + (i - axismargin) / xscale;
                y = (int)(baseline + 1 - frescale * mass(x));
                if (y < graphtop) y = graphtop;
                g.drawLine( xprev, yprev, i, y );
                xprev = i;
                yprev = y;
            }
        }
        righty = (int)(baseline + 1 - frescale * mass(xhi));
        if (righty < graphtop) righty = graphtop;
        g.drawLine( rightx, righty, rightx, baseline + 1);

        /*  draw the y-axis if symmetrical  */
        if (symmetric) {
            g.setColor(Color.lightGray);
            g.drawLine(apwidth / 2, graphtop - 4, apwidth / 2, baseline + 1);
        }
    }

}