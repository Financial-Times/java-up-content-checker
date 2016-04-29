package com.ft.up.contentchecker;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.core.UriBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class ImageChecker
    implements Checker {
  
  private static final String IMAGE_SET_XPATH = "//ft-content[@type=\"http://www.ft.com/ontology/content/ImageSet\"]/@url";
  
  private Client client;
  private String documentStoreApi;
  
  public ImageChecker(Client client, String documentStoreApi) {
    this.client = client;
    this.documentStoreApi = documentStoreApi;
  }
  
  @Override
  public List<List<String>> check(final UUID uuid) {
//    System.out.println("check: " + uuid);
    
    Content content = fetchContent(uuid);
    if (content == null) {
      return Collections.singletonList(Arrays.asList(uuid.toString(), "Content not found"));
    }
    
    Set<UUID> imageSets = extractImageSets(content);
//    System.out.println("image sets: " + imageSets);
    if (imageSets == null) {
      return Collections.singletonList(Arrays.asList(uuid.toString(), "Unable to read ImageSets"));
    }
    
    List<List<String>> results = new ArrayList<>();
    imageSets.forEach(imageSetUuid -> results.addAll(checkImageSet(uuid, imageSetUuid)));
    
    return results.stream()
      .filter(list -> !list.isEmpty())
      .collect(Collectors.toList());
  }
  
  private Content fetchContent(UUID uuid) {
    URI contentUri = UriBuilder.fromUri(documentStoreApi)
        .path(uuid.toString())
        .build();
    
    try {
    return client.resource(contentUri).get(Content.class);
    }
    catch (Exception e) {
      return null;
    }
  }
  
  private Set<UUID> extractImageSets(Content content)
      /*throws Exception */{
    
    Set<UUID> imageSets = new LinkedHashSet<>();
    
    MainImage mainImage = content.mainImage;
    if (mainImage != null) {
      UUID mainImageUuid = ContentChecker.extractUuid(mainImage.id);
      if (mainImageUuid != null) {
        imageSets.add(mainImageUuid);
      }
    }
    
    String body = content.bodyXML;
    
    XPathFactory xpf = XPathFactory.newInstance();
    XPath xp = xpf.newXPath();
    
    NodeList images;
    try {
      images = (NodeList)xp.evaluate(IMAGE_SET_XPATH, new InputSource(new StringReader(body)), XPathConstants.NODESET);
    int len = images.getLength();
    for (int i = 0; i < len; i++) {
      String imageSetUrl = images.item(i).getNodeValue();
      UUID auxImageUuid = ContentChecker.extractUuid(imageSetUrl);
      if (auxImageUuid != null) {
        imageSets.add(auxImageUuid);
      }
    }
    } catch (XPathExpressionException e) {
      System.err.println("unable to fetch image sets for " + content.id);
      return null;
    }
    
    return imageSets;
  }
  
  private List<List<String>> checkImageSet(UUID articleUuid, UUID imageSetUuid) {
    Content imageSet = fetchContent(imageSetUuid);
    if (imageSet == null) {
//      System.out.println("Missing image set: " + articleUuid + "," + imageSetUuid);
      return Collections.singletonList(Arrays.asList(articleUuid.toString(), imageSetUuid.toString()));
    }
    
    return imageSet.members.stream()
        .map(m -> checkImageModel(articleUuid, imageSetUuid, ContentChecker.extractUuid(m.id)))
        .collect(Collectors.toList());
  }
  
  private List<String> checkImageModel(UUID articleUuid, UUID imageSetUuid, UUID imageModelUuid) {
    Content imageModel = fetchContent(imageModelUuid);
    if (imageModel == null) {
//      System.out.println("Missing image model: " + articleUuid + "," + imageSetUuid + "," + imageModelUuid);
      return Arrays.asList(articleUuid.toString(), imageSetUuid.toString(), imageModelUuid.toString());
    }
    
    URI binaryUri = UriBuilder.fromUri(imageModel.binaryUrl)
        .build();
    
    try {
      ClientResponse resp = client.resource(binaryUri).head();
      if (resp.getStatus() != 200) {
        System.out.println("Missing image binary[1]: " + articleUuid + " ... " + binaryUri + ": " + resp.getStatus());
        return Arrays.asList(articleUuid.toString(), imageSetUuid.toString(), imageModelUuid.toString(), binaryUri.toString());
      }
    }
    catch (Exception e) {
      System.out.println("Missing image binary[2]: " + articleUuid + " ... " + binaryUri + ": " + e.getMessage());
      return Arrays.asList(articleUuid.toString(), imageSetUuid.toString(), imageModelUuid.toString(), binaryUri.toString());
    }
    
    return Collections.emptyList();
  }
}
