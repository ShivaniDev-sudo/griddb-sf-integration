package mycode.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.toshiba.mwcloud.gs.Container;
import com.toshiba.mwcloud.gs.GridStore;
import com.toshiba.mwcloud.gs.Query;
import com.toshiba.mwcloud.gs.Row;
import com.toshiba.mwcloud.gs.RowSet;

@Service
public class ChartService {

  @Autowired
  GridStore store;

  public Map<String, Object> queryData() throws Exception {

    Container<?, Row> container = store.getContainer("serviceTickets");
    if (container == null) {
      throw new Exception("Container not found.");
    }
    Map<String, Object> resultMap = new HashMap<>();
    ArrayList<Double> resolutionTime = new ArrayList<>();
    ArrayList<Date> ticketDates = new ArrayList<>();

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    Date now = new Date();

    String nowString = dateFormat.format(now);
    String startTime = "1971-12-23T18:18:52.000Z";

    String queryString = "select * where CreatedDate >= TIMESTAMP('" + startTime
        + "') and CreatedDate <= TIMESTAMP('" + nowString + "')";
    Query<Row> query = container.query(queryString);
    RowSet<Row> rs = query.fetch();

    while (rs.hasNext()) {
      Row row = rs.next();
      resolutionTime.add(row.getDouble(6));
      ticketDates.add(row.getTimestamp(0));
      resultMap.putIfAbsent("time", resolutionTime);
      resultMap.putIfAbsent("dates", ticketDates);
    }
    return resultMap;
  }

}
