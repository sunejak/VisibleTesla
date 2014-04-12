/*
 * VampireLossResults.java - Copyright(c) 2014 Joe Pasqua
 * Provided under the MIT License. See the LICENSE file for details.
 * Created: Apr 05, 2014
 */
package org.noroomattheinn.visibletesla.dialogs;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import org.noroomattheinn.utils.Utils;
import org.noroomattheinn.visibletesla.VampireStats;

/**
 * VampireLossResults: Display statistics about vampire loss.
 * 
 * @author Joe Pasqua <joe at NoRoomAtTheInn dot org>
 */
public class VampireLossResults  implements DialogUtils.DialogController {
    private static final int TimeOffset = 0;
    
    private Stage myStage;
    private String units;
    
    @FXML private ResourceBundle resources;
    @FXML private URL location;

    @FXML private TextArea rawDataField;
    @FXML private LineChart<Number, Number> chart;
    @FXML private LineChart<Number, Number> sequenceChart;

    @FXML void initialize() { }

    @Override public void setStage(Stage stage) { this.myStage = stage; }
    @Override public void setProps(Map props) {
        List<VampireStats.Rest> restPeriods = Utils.cast(props.get("REST_PERIODS"));
        if (restPeriods == null || restPeriods.isEmpty()) return;
        units = (String)props.get("UNITS");
        
        // Hack to make tooltip styles works. No one knows why.
        URL url = getClass().getClassLoader().getResource("org/noroomattheinn/styles/tooltip.css");
        myStage.getScene().getStylesheets().add(url.toExternalForm());
        
        // ----- Set up time-based chart
        
        chart.setTitle("Vampire Loss Data - EXPERIMENTAL");
        chart.setLegendVisible(false);
        Node chartBackground = chart.lookup(".chart-plot-background");
        chartBackground.setStyle("-fx-background-color: white;");
        NumberAxis xAxis = (NumberAxis)chart.getXAxis();
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0.0);
        xAxis.setUpperBound(24.0);        
        xAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(xAxis) {
            @Override public String toString(Number hr) {
                int adjusted = (hr.intValue() + (24-TimeOffset)) % 24;
                return String.format("%2d", adjusted);
            }
        });
        
        for (VampireStats.Rest r : restPeriods) {
            XYChart.Series<Number,Number> series = new XYChart.Series<>();
            ObservableList<XYChart.Data<Number, Number>> data = series.getData();

            addPeriod(data, r);
            chart.getData().add(series);
            series.getNode().setStyle("-fx-opacity: 0.25; -fx-stroke-width: 3px;");
        }
        
        
        final double overallAverage = (Double)props.get("OVERALL_AVG");
        XYChart.Series<Number,Number> avg = new XYChart.Series<>();
        avg.setName("Average");
        
        final String tip = String.format("Average Loss: %3.2f %s/hr", overallAverage, units);
        final XYChart.Data<Number,Number> p1 = new XYChart.Data<Number,Number>(0, overallAverage);
        p1.setExtraValue(tip); avg.getData().add(p1); addTooltip(p1);
        
        final XYChart.Data<Number,Number> p2 = new XYChart.Data<Number,Number>(23.99, overallAverage);
        p2.setExtraValue(tip); avg.getData().add(p2); addTooltip(p2);
        
        chart.getData().add(avg);
        avg.getNode().setStyle("-fx-opacity: 0.5; -fx-stroke-width: 4px;");


        // ----- Set up the Scatter Chart
        
        sequenceChart.setTitle("Vampire Loss Data - EXPERIMENTAL");
        sequenceChart.setLegendVisible(false);
        chartBackground = sequenceChart.lookup(".chart-plot-background");
        chartBackground.setStyle("-fx-background-color: white;");
        xAxis = (NumberAxis)sequenceChart.getXAxis();
        xAxis.setAutoRanging(true);
        
        XYChart.Series<Number,Number> series = new XYChart.Series<>();
        series.setName("Vampire Loss");
        int index = 0;
        ObservableList<XYChart.Data<Number, Number>> data = series.getData();
        for (VampireStats.Rest r : restPeriods) {
            final XYChart.Data<Number,Number> dataPoint =
                    new XYChart.Data<Number,Number>(index++, r.avgLoss());
            addTooltip(dataPoint);
            dataPoint.setExtraValue(r);
            dataPoint.setNode(getMarker());
            data.add(dataPoint);
        }
        sequenceChart.getData().add(series);
        series.getNode().setStyle("-fx-opacity: 0.0; -fx-stroke-width: 0px;");

        avg = new XYChart.Series<>();
        avg.setName("Average");
        
        final XYChart.Data<Number,Number> a1 = new XYChart.Data<Number,Number>(0, overallAverage);
        a1.setExtraValue(tip); avg.getData().add(a1); addTooltip(a1);
        
        final XYChart.Data<Number,Number> a2 = new XYChart.Data<Number,Number>(index-1, overallAverage);
        a2.setExtraValue(tip); avg.getData().add(a2); addTooltip(a2);
        
        sequenceChart.getData().add(avg);
        avg.getNode().setStyle("-fx-opacity: 0.5; -fx-stroke-width: 4px;");
        
        // ----- Set up the raw data text area

        String rawResults = (String) props.get("RAW_RESULTS");
        if (rawResults == null) rawResults = "";
        rawDataField.setText(rawResults);
    }
    
    private Node getMarker() {
        Circle c = new Circle(4.0);
        c.setFill(Color.web("#0000ff", 0.5));
        c.setStroke(Color.web("#0000ff"));
        c.setStrokeWidth(1.0);
        return c;
    }
    
    private void addPeriod(ObservableList<XYChart.Data<Number, Number>> data, VampireStats.Rest r) {
        addPoint(data, r, r.startTime);
        addPoint(data, r, r.endTime);
    }
    
    private void addPoint(ObservableList<XYChart.Data<Number, Number>> data,
                          VampireStats.Rest r, long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        double time = c.get(Calendar.HOUR_OF_DAY) + ((double)c.get(Calendar.MINUTE))/60;
        time = (time + TimeOffset) % 24;
        
        final XYChart.Data<Number,Number> dataPoint =
                new XYChart.Data<Number,Number>(time, r.avgLoss());
        dataPoint.setExtraValue(r);
        data.add(dataPoint);
        addTooltip(dataPoint);
    }
    
    private void addTooltip(final XYChart.Data<Number,Number> dataPoint) {
        dataPoint.nodeProperty().addListener(new ChangeListener<Node>() {
            @Override public void changed(ObservableValue<? extends Node> observable,
                    Node oldValue, Node newValue) {
                if (newValue != null) {
                    String tip =  (dataPoint.getExtraValue() instanceof String) ?
                            (String)dataPoint.getExtraValue() : 
                            genTooltip((VampireStats.Rest)dataPoint.getExtraValue());
                    Tooltip.install(newValue, new Tooltip(tip));
                    dataPoint.nodeProperty().removeListener(this);
                }
            }
        });
    }
    
    private double hours(long millis) {return ((double)(millis))/(60 * 60 * 1000); }
    
    private String genTooltip(VampireStats.Rest rest) {
        double period = hours(rest.endTime - rest.startTime);
        double loss = rest.startRange - rest.endRange;
        String date = String.format("%1$tm/%1$td %1$tH:%1$tM", new Date(rest.startTime));
        return String.format(
                "Date: %s\n" + 
                "Elapsed (HH:MM): %02d:%02d\n" + 
                "Loss: %3.2f %s\n" +
                "Loss/hr: %3.2f",
                date, (int)period, (int)((period%1)*60), loss, units, loss/period  );
    }
}
