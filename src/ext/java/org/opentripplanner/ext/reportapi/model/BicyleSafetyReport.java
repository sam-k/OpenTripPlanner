package org.opentripplanner.ext.reportapi.model;

import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;

public class BicyleSafetyReport {

  public static void main(String[] args) {
    System.out.println(makeCsv("norway"));
  }

  public static String makeCsv(String configName) {
    var wayPropertySet = new WayPropertySet();

    var source = WayPropertySetSource.fromConfig(configName);
    source.populateProperties(wayPropertySet);

    var buf = new CsvReportBuilder(",");

    buf.addHeader(
      "OSM tags for osmWayPropertySet " + configName,
      "mixin",
      "permissions",
      "safety penalty there",
      "safety penalty back"
    );

    wayPropertySet
      .getWayProperties()
      .forEach(p -> {
        buf.addText(p.getSpecifier().toString());
        buf.addBoolean(p.isSafetyMixin());
        buf.addText(p.getProperties().getPermission().toString());

        var safetyProps = p.getProperties().getBicycleSafetyFeatures();
        buf.addNumber(safetyProps.forward());
        buf.addNumber(safetyProps.back());
        buf.newLine();
      });

    return buf.toString();
  }
}
