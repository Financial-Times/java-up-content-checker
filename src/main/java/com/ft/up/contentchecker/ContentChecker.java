package com.ft.up.contentchecker;

import com.sun.jersey.api.client.Client;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ContentChecker {
  private static final Pattern NEXT_SINCE_DATE = Pattern.compile("https?://.*[?&]since=([^&]+).*");
  private static final Pattern API_URL_UUID = Pattern.compile(".*/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})$");
  private static final String DELETE_NOTIFICATION = "http://www.ft.com/thing/ThingChangeType/DELETE";
  
  public static void main(String[] args) {
    //OffsetDateTime since = OffsetDateTime.parse("2006-02-09T02:00:42.000Z");
//    OffsetDateTime since = OffsetDateTime.parse("2014-04-07T21:20:02Z");
    OffsetDateTime since = OffsetDateTime.parse("2015-01-15T13:31:17Z");
//    OffsetDateTime since = OffsetDateTime.parse("2016-01-01T00:00:00Z");
//    OffsetDateTime since = OffsetDateTime.parse("2016-03-31T12:00:00Z");
    
    (new ContentChecker()).checkSince(since);
  }
  
  private String notificationsApi = "http://ftapp15021-lviw-uk-p/content/notifications";
  private String documentStoreApi = "http://document-store-api-iw-uk-p.svc.ft.com/content-read";
  
  private Client client = Client.create();
  private List<Checker> checkers = new ArrayList<>();
  private int uuidCount;
  private int errorCount;
  private String output = "content-checker.csv";
  
  public void checkSince(OffsetDateTime since) {
    checkers.add(new ImageChecker(client, documentStoreApi));
    
    try (CSVPrinter out = new CSVPrinter(new FileWriter(output), CSVFormat.DEFAULT)) {
    do {
      NotificationsPage page = fetchPage(since);
      
      page.notifications.stream()
          .filter(n -> !DELETE_NOTIFICATION.equals(n.type))
          .map(n -> extractUuid(n.apiUrl))
          .forEach(uuid -> check(uuid, out));
      
      OffsetDateTime next = getNextSinceDate(page);
      if (since.equals(next)) {
        since = null;
      }
      else {
        since = next;
      }
      
    } while (since != null);
    } catch (IOException e) {
      System.err.println("Could not write output file: " + e);
    }
    
    System.out.println("Checked " + uuidCount + " UUIDs, there were " + errorCount + " errors");
  }
  
  private NotificationsPage fetchPage(OffsetDateTime since) {
    System.out.println(OffsetDateTime.now() + "Fetching notifications since: " + since);
    
    URI notificationsUri = UriBuilder.fromUri(notificationsApi)
        .queryParam("since", since.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
        .build();
    
    return client.resource(notificationsUri).get(NotificationsPage.class);
  }
  
  private OffsetDateTime getNextSinceDate(NotificationsPage page) {
    for (Link link : page.links) {
      if ("next".equals(link.rel)) {
        Matcher m = NEXT_SINCE_DATE.matcher(link.href);
        if (m.matches()) {
          return OffsetDateTime.parse(m.group(1));
        }
      }
    }
    
    return null;
  }
  
  public void check(UUID uuid, CSVPrinter out) {
    uuidCount++;
    for (Checker c : checkers) {
      List<List<String>> result = c.check(uuid);
      if (!result.isEmpty()) {
        errorCount++;
        try {
          out.printRecords(result);
          out.flush();
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }
  
  public static UUID extractUuid(String apiUrl) {
    Matcher m = API_URL_UUID.matcher(apiUrl);
    if (m.matches()) {
      return UUID.fromString(m.group(1));
    }
    
    return null;
  }
}
