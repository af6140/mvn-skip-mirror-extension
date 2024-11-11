package com.github.af6140.mvn.settings;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

@Named("DeadMirror")
@Singleton
public class SettingsCustomizer extends AbstractMavenLifecycleParticipant {

  @Requirement
//  @Inject
  private Logger logger;

  @Override
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
    logger.info("After session started");
    Settings settings = session.getSettings();
    String checkPattern = (String) System.getenv("MVN_MIRROR_CHECK_PATTERN");
    if(checkPattern==null || checkPattern.trim().isEmpty()) {
     checkPattern =  ".*artifactory.*";
    }
    if(settings != null) {
      logger.info("Start checking mirrors, total={}", settings.getMirrors().size());
      removeMirror(settings.getMirrors(), checkPattern);
      logger.info("Finished checking mirrors, total={}", settings.getMirrors().size());
    }
  }

  private void removeMirror(List<Mirror> mirrors, String checkPattern) {
    if(mirrors ==null || mirrors.isEmpty()) {
      return;
    }
    mirrors.removeIf(m -> ! validateUrl(m, checkPattern));
  }

  private boolean validateUrl(Mirror mirror, String checkPattern) {
    String url = mirror.getUrl();
    if(url ==null || !url.toLowerCase().matches(checkPattern)) {
      return true;
    }
    logger.info("Checking mirror " + url);
    try {
      HttpRequest request = HttpRequest.newBuilder(new URI(url))
          .GET()
          .timeout(Duration.ofSeconds(3))
          .version(HttpClient.Version.HTTP_1_1)
          .build();
      HttpClient client = HttpClient.newBuilder().
          connectTimeout(Duration.ofSeconds(3)).build();
      client.send(request, HttpResponse.BodyHandlers.ofString());
    } catch (URISyntaxException | InterruptedException | IOException e) {
      logger.warn("Removing invalid or unreachable mirror {}", url);
      return false;
    }
    return true;
  }
}
