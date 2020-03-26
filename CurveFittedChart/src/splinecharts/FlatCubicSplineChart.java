package splinecharts;

import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.shape.*;
import javafx.util.Pair;

/**
 * Extended version of AreaChart that smooths the data points to a curve
 * 
 * @author Jasper Potts
 */
public class FlatCubicSplineChart extends LineChart<Number, Number>
{

    public FlatCubicSplineChart(NumberAxis xAxis, NumberAxis yAxis)
    {
        super(xAxis, yAxis);
    }


    /** @inheritDoc */
    @Override
    protected void layoutPlotChildren()
    {
        super.layoutPlotChildren();
        for (int seriesIndex = 0; seriesIndex < getDataSize(); seriesIndex++)
        {
            final XYChart.Series<Number, Number> series = getData().get(seriesIndex);
            final Path seriesLine = (Path) series.getNode();
            smooth(seriesLine.getElements());
        }
    }


    private int getDataSize()
    {
        final ObservableList<XYChart.Series<Number, Number>> data = getData();
        return (data != null) ? data.size() : 0;
    }


    private static void smooth(ObservableList<PathElement> strokeElements)
    {
        // as we do not have direct access to the data, first recreate the list
        // of all the data points we have
        final Point2D[] dataPoints = new Point2D[strokeElements.size()];
        for (int i = 0; i < strokeElements.size(); i++)
        {
            final PathElement element = strokeElements.get(i);
            if (element instanceof MoveTo)
            {
                final MoveTo move = (MoveTo) element;
                dataPoints[i] = new Point2D(move.getX(), move.getY());
            }
            else if (element instanceof LineTo)
            {
                final LineTo line = (LineTo) element;
                final double x = line.getX(), y = line.getY();
                dataPoints[i] = new Point2D(x, y);
            }
        }

        // now clear and rebuild elements
        strokeElements.clear();
        Pair<Point2D[], Point2D[]> result = calcCurveControlPoints(dataPoints);
        Point2D[] firstControlPoints = result.getKey();
        Point2D[] secondControlPoints = result.getValue();
        // start both paths
        strokeElements.add(new MoveTo(dataPoints[0].getX(), dataPoints[0].getY()));
        // add curves
        for (int i = 1; i < dataPoints.length; i++)
        {
            final int ci = i - 1;
            strokeElements.add(new CubicCurveTo(
                    firstControlPoints[ci].getX(), firstControlPoints[ci].getY(),
                    secondControlPoints[ci].getX(), secondControlPoints[ci].getY(),
                    dataPoints[i].getX(), dataPoints[i].getY()));
        }
    }


    /**
     * Calculate open-ended Bezier Spline Control Points.
     * 
     * @param dataPoints
     *                       Input data Bezier spline points.
     */
    public static Pair<Point2D[], Point2D[]> calcCurveControlPoints(Point2D[] dataPoints)
    {
        Point2D[] rhsControlPoints = new Point2D[dataPoints.length];
        Point2D[] lhsControlPoints = new Point2D[dataPoints.length];
        
        int n = dataPoints.length - 1;
        for (int i = 0; i < n; i++)
        {
            double x = (dataPoints[i].getX() + dataPoints[i + 1].getX()) / 2;
            rhsControlPoints[i] = new Point2D(x, dataPoints[i].getY());
            lhsControlPoints[i] = new Point2D(x, dataPoints[i + 1].getY());
        }
        
        return new Pair<Point2D[], Point2D[]>(rhsControlPoints, lhsControlPoints);
    }

}