package com.github.freeacs.stun;

import com.github.freeacs.common.util.IPAddress;
import com.github.freeacs.dbi.Unit;
import com.github.freeacs.dbi.crypto.Crypto;
import com.github.freeacs.dbi.util.SystemParameters;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Random;

import lombok.AllArgsConstructor;
import lombok.Data;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

public class Kick {
  @Data
  @AllArgsConstructor
  public static class KickResponse {
    private final boolean kicked;
    private String message;
  }

  private static final Kick kickSingleton = new Kick();

  private final RestClient defaultRestClient = RestClient.builder()
          .requestFactory(new HttpComponentsClientHttpRequestFactory(
                  HttpClients.custom()
                          .setDefaultRequestConfig(RequestConfig.custom()
                                  .setConnectTimeout(Timeout.ofMilliseconds(20000))
                                  .setResponseTimeout(Timeout.ofMilliseconds(20000))
                                  .build())
                          .build()))
          .build();

  public static KickResponse kick(Unit unit, Properties properties)
      throws MalformedURLException, SQLException {
    return kickSingleton.kickInternal(unit, properties);
  }

  private static final Logger log = LoggerFactory.getLogger("KickSingle");
  private static final Random random = new Random();

  public KickResponse kickInternal(Unit unit, Properties properties)
      throws MalformedURLException {
    CPEParameters cpeParams = new CPEParameters(getKeyroot(unit));
    String udpCrUrl = unit.getParameterValue(cpeParams.UDP_CONNECTION_URL, false);
    String crUrl = unit.getParameterValue(cpeParams.CONNECTION_URL, false);
    String crUser = unit.getParameterValue(cpeParams.CONNECTION_USERNAME);
    String crPass = unit.getParameterValue(cpeParams.CONNECTION_PASSWORD);
    if (crPass == null) {
      crPass = crUser;
    }
    String publicIP = unit.getParameterValue(SystemParameters.IP_ADDRESS);
    String publicProtocol = unit.getParameterValue(SystemParameters.PROTOCOL);
    Integer publicPort = getPublicPort(unit);

    // default response
    KickResponse kr =
        new KickResponse(
            false,
            "Neither a public ConnectionRequestURL nor any UDPConnectionRequestAddress was found");

    // TCP-kick (HTTP)
    if (crUrl != null && !crUrl.trim().isEmpty() && checkIfPublicIP(crUrl, properties)) {
      log.debug(unit.getId() + ": will try TCP kick on " + crUrl);
      return kickUsingTCP(unit, crUrl, crPass, crUser);
    }

    // UDP-kick
    if (!kr.isKicked() && udpCrUrl != null && !udpCrUrl.trim().isEmpty()) {
      log.debug(unit.getId() + ": will try UDP kick on " + udpCrUrl);
      return kickUsingUDP(unit, udpCrUrl, crPass, crUser);
    }

    // TCP-kick with port forwarding
    if (properties.isExpectPortForwarding() && publicIP != null && crUrl != null) {
      String newCrUrl = crUrl.replace(new URL(crUrl).getHost(), publicIP);
      log.debug(
          unit.getId()
              + ": will try TCP kick by expecting port forwarding on "
              + crUrl
              + " -> "
              + newCrUrl);
      return kickUsingTCP(unit, newCrUrl, crPass, crUser);
    }

    // Dynamic TCP-kick
    if (crUrl == null && publicIP != null && publicProtocol != null && publicPort != null) {
      crUrl = String.format("%s://%s:%d", publicProtocol, publicIP, publicPort);
      log.debug(unit.getId() + ": will try dynamic TCP kick on " + crUrl);
      return kickUsingTCP(unit, crUrl, crPass, crUser);
    }

    return kr;
  }

  private Integer getPublicPort(Unit unit) {
    try {
      return Integer.parseInt(unit.getParameterValue(SystemParameters.PORT));
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  /**
   * Check if IP is public only if its configured. If its not configured, this method returns true
   * always.
   *
   * @param crUrl The ip to check
   * @param properties props
   * @return a boolean if configured to check if ip is public, otherwise always true
   * @throws MalformedURLException if the ip is malformed
   */
  boolean checkIfPublicIP(String crUrl, Properties properties) throws MalformedURLException {
    return !properties.isCheckPublicIp() || IPAddress.isPublic(new URL(crUrl).getHost());
  }

  protected KickResponse kickUsingTCP(Unit unit, String crUrl, String crPass, String crUser)
      throws MalformedURLException {

    RestClient restClient = null;
    HttpStatusCode statusCode;
    if (crUser != null && crPass != null) {
      try {
        restClient = createSecureClient(crUrl, crUser, crPass);
        log.debug(unit.getId() + ": kicking with authentication (digest/basic)");
      } catch (MalformedURLException e) {
        return new KickResponse(false, "Invalid URL: " + crUrl);
      }
    } else {
      // Usa o cliente padrão, sem overhead de credenciais
      restClient = defaultRestClient;
      log.debug(unit.getId() + ": kicking without authentication");
    }
    try {
      ResponseEntity<Void> response = restClient.get()
              .uri(crUrl)
              .retrieve()
              .toBodilessEntity();
      statusCode = response.getStatusCode();
    } catch (Exception ex) {
      log.warn(unit.getId() + " did not respond, an error has occured: ", ex);
      return new KickResponse(
          false,
          "TCP/HTTP-kick to "
              + crUrl
              + " failed because of an unexpected error: "
              + ex.getMessage());
    }
    if (statusCode.is2xxSuccessful()) {
      log.debug(
          unit.getId() + " responded with HTTP " + statusCode + ", indicating a successful kick");
      return new KickResponse(
          true,
          "TCP/HTTP-kick to "
              + crUrl
              + " got HTTP response code "
              + statusCode
              + ", indicating success");
    } else {
      log.warn(
          unit.getId() + " responded with HTTP " + statusCode + ", indicating a unsuccessful kick");
      if (statusCode.value() == HttpStatus.SC_FORBIDDEN || statusCode.value() == HttpStatus.SC_UNAUTHORIZED) {
        return new KickResponse(
            false,
            "TCP/HTTP-kick to "
                + crUrl
                + " (user:"
                + crUser
                + ",pass:"
                + crPass
                + ") failed, probably due to wrong user/pass since HTTP response code is "
                + statusCode);
      } else {
        return new KickResponse(
            false, "TCP/HTTP-kick to " + crUrl + " failed with HTTP response code " + statusCode.value());
      }
    }
  }

  private KickResponse kickUsingUDP(Unit unit, String udpCrUrl, String crPass, String crUser) {
    try {
      String id = String.valueOf(random.nextInt(100000));
      String cn = String.valueOf(random.nextLong());
      String ts = String.valueOf(System.currentTimeMillis());
      String text = ts + id + crUser + cn;
      String passFix = crPass == null ? "password" : crPass;
      String sig = Crypto.computeHmacAsHexUpperCase(passFix, text);
      String req =
          "GET http://"
              + udpCrUrl
              + "/?ts="
              + ts
              + "&id="
              + id
              + "&un="
              + crUser
              + "&cn="
              + cn
              + "&sig="
              + sig
              + " HTTP/1.1\r\n\r\n";
      byte[] buf = req.getBytes();
      if (!udpCrUrl.contains(":")) {
        udpCrUrl += ":80";
      }
      InetAddress address = InetAddress.getByName(udpCrUrl.split(":")[0]);
      int port = Integer.parseInt(udpCrUrl.split(":")[1]);
      DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
      for (int i = 0; i < 3; i++) {
        MessageStack.push(packet);
      }
      log.debug(unit.getId() + " has been kicked using UDP (TR-111) method to " + udpCrUrl);
      return new KickResponse(true, "UDP kick to " + udpCrUrl + " was initiated");
    } catch (Throwable t) {
      log.error(unit.getId() + " UDP kick to " + udpCrUrl + " failed", t);
      return new KickResponse(false, "UDP kick to " + udpCrUrl + " failed: " + t.getMessage());
    }
  }

  /** KICK RELATED METHODS. */
  private String getKeyroot(Unit u) {
    for (String paramName : u.getParameters().keySet()) {
      if (paramName.startsWith("Device.")) {
        return "Device.";
      }
      if (paramName.startsWith("InternetGatewayDevice.")) {
        return "InternetGatewayDevice.";
      }
    }
    throw new RuntimeException(
        "No keyroot found for unit " + u.getId() + ", probably because no parameters are defined");
  }

  private RestClient createSecureClient(String urlStr, String username, String password) throws MalformedURLException {
    URL url = new URL(urlStr);

    // Configuração de Timeout moderna
    RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(20000))
            .setResponseTimeout(Timeout.ofMilliseconds(20000))
            .build();

    // Configura o provedor de credenciais (Suporta Basic e Digest automaticamente no HC5)
    BasicCredentialsProvider credsProvider = new BasicCredentialsProvider();
    credsProvider.setCredentials(
            new AuthScope(url.getHost(), url.getPort() == -1 ? 80 : url.getPort()),
            new UsernamePasswordCredentials(username, password.toCharArray())
    );

    // Cria o cliente Apache 5
    CloseableHttpClient httpClient = HttpClients.custom()
            .setDefaultRequestConfig(config)
            .setDefaultCredentialsProvider(credsProvider)
            .build();

    // Retorna o RestClient do Spring configurado com este cliente
    return RestClient.builder()
            .requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
            .build();
  }


}
