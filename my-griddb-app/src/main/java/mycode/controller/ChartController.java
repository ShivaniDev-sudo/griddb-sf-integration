package mycode.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import mycode.service.ChartService;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ChartController {

  @Autowired
  ChartService chartService;

  @GetMapping("/charts")
  public String showCharts(Model model) {

    Map<String, Object> chartData = new HashMap<>();
    try {
      Map<String, Object> projectionData = chartService.queryData();
      chartData.put("values", projectionData.get("time"));
      chartData.put("labels", projectionData.get("dates"));
    } catch (Exception e) {
      e.printStackTrace();
    }
    model.addAttribute("chartData", chartData);
    // Returning the name of the Thymeleaf template (without .html extension)
    return "charts";
  }
}
