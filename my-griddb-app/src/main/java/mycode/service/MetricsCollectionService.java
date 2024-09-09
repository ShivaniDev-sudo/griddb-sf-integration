package mycode.service;

import java.util.ArrayList;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.toshiba.mwcloud.gs.*;
import java.util.Random;

import mycode.dto.ServiceTicket;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class MetricsCollectionService {
  @Autowired
  GridStore store;

  @Autowired
  RestTemplate restTemplate;

  @Scheduled(fixedRate = 600020) // Collect metrics every minute
  public void collectMetrics() throws GSException, JsonMappingException, JsonProcessingException, ParseException {
    // Get Salesforce OAuth token
    String accessToken = getSalesforceAccessToken();
    System.out.println("Salesforce Data: " + accessToken);
    ArrayList<ServiceTicket> salesforceData = fetchSalesforceData(accessToken);

    salesforceData.forEach(t -> {
      try {
        TimeSeries<ServiceTicket> ts = store.putTimeSeries("serviceTickets",
            ServiceTicket.class);
        ts.put(salesforceData);
      } catch (GSException e) {
        e.printStackTrace();
      }
    });
  }

  public ArrayList<ServiceTicket> fetchSalesforceData(String accessToken)
      throws JsonMappingException, JsonProcessingException, ParseException {
    String queryUrl = "https://<ENTER_YOUR_SF_INSTANCE>.develop.my.salesforce.com/services/data/v57.0/query";

    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(queryUrl)
        .queryParam("q",
            "SELECT+Id,+CaseNumber,+Subject,+Status,+CreatedDate,+ClosedDate,+Priority+FROM+Case");

    HttpEntity<String> request = new HttpEntity<>(headers);
    ResponseEntity<String> response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, request,
        String.class);

    if (response.getStatusCode() == HttpStatus.OK) {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(response.getBody());
      ArrayNode records = (ArrayNode) rootNode.path("records");
      System.out.println(response.getBody());
      ArrayList<ServiceTicket> serviceTickets = new ArrayList<>();
      for (JsonNode record : records) {
        ServiceTicket ticket = new ServiceTicket();
        String status = record.get("Status").asText();
        ticket.setStatus(status);
        if (status.equals("Closed")) {
          ticket.setCaseNumber(record.get("CaseNumber").asText());
          ticket.setCreatedDate(objectMapper.convertValue(record.get("CreatedDate"),
           Date.class));
          ticket.setClosedDate(objectMapper.convertValue(record.get("ClosedDate"), Date.class));
          ticket.setSubject(record.get("Subject").asText());
          ticket.setPriority(record.get("Priority").asText());
          ticket.setResolutionTime(
              calculateResolutionTimeInHours(
                  objectMapper.convertValue(record.get("CreatedDate"), Date.class),
                  objectMapper.convertValue(record.get("ClosedDate"), Date.class)));
          serviceTickets.add(ticket);
        }
      }
      return serviceTickets;

    } else {
      throw new RuntimeException("Failed to fetch data from Sales force");
    }
  }

  public static double calculateResolutionTimeInHours(Date createdDateStr, Date closedDateStr) {
    Date createdDate = createdDateStr;
    Date closedDate = closedDateStr;
    long timeDifferenceMillis = closedDate.getTime() - createdDate.getTime();
    return timeDifferenceMillis;

  }

  public String getSalesforceAccessToken() throws JsonMappingException, JsonProcessingException {
    // URL and endpoint

    String url = "https://login.salesforce.com/services/oauth2/token";

    // Request headers
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    // Request body parameters
    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
    body.add("grant_type", "password");
    body.add("client_id", "<ENTER_YOUR_VALUE_HERE>");
    body.add("client_secret", "<ENTER_YOUR_VALUE_HERE>");
    body.add("password", "<ENTER_YOUR_VALUE_HERE>");
    body.add("redirect_uri", "<ENTER_YOUR_VALUE_HERE>");
    body.add("username", "<ENTER_YOUR_VALUE_HERE>");

    // Create HttpEntity with headers and body
    HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(body, headers);
    try {
      // Send POST request
      ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

      if (response.getStatusCode() == HttpStatus.OK) {
        String responseBody = response.getBody();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        String accessToken = jsonNode.get("access_token").asText();
        return accessToken;
      } else {
        throw new RuntimeException("Failed to retrieve the token");
      }
    } catch (HttpClientErrorException e) {
      // Log request and response details
      System.out.println(("HTTP Error: " + e.getStatusCode()));
      System.out.println("Response Body: " + e.getResponseBodyAsString());
      throw e;
    }
  }
}
