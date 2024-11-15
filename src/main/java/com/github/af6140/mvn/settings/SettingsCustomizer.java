package com.github.af6140.mvn.settings;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.slf4j.Logger;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;


@Component(role = AbstractMavenLifecycleParticipant.class)
public class SettingsCustomizer extends AbstractMavenLifecycleParticipant {
  @Requirement
  private Logger logger;

  @Override
  public void afterSessionStart(MavenSession session) throws MavenExecutionException {
    logger.debug("After session started");
    Settings settings = session.getSettings();
    if (settings == null) {
      return;
    }
    String checkPattern = (String) System.getenv("MVN_MIRROR_CHECK_PATTERN");
    if(checkPattern==null || checkPattern.trim().isEmpty()) {
     checkPattern =  ".*artifactory.*";
    }
    logger.info("Start checking mirrors, total={}", settings.getMirrors().size());
    boolean removed = removeMirror(settings.getMirrors(), checkPattern);
    logger.info("Finished checking mirrors, total={}", settings.getMirrors().size());
    if(removed) {
      // add central
      List<ArtifactRepository> repos= session.getProjectBuildingRequest().getRemoteRepositories();
      final String badPattern = checkPattern;
      repos.removeIf(r -> {
        boolean matched = false;
        if( r.getUrl() != null && r.getLayout()!=null && r.getUrl().toLowerCase().matches(badPattern)) {
          matched = true;
        }
        return matched;
      });

      long count = repos.stream().filter(x -> x.getId().toLowerCase().equals("central")).count();
      if(count<=0) {
        logger.info("Adding central repo");
        repos.add(getCentralRepo());
      }
    }
  }

  private boolean removeMirror(List<Mirror> mirrors, String checkPattern) {
    if(mirrors ==null || mirrors.isEmpty()) {
      return false;
    }
    boolean removed= mirrors.removeIf(m -> ! validateUrl(m, checkPattern));
    return removed;
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

  /**
   * @return Maven official central
   */
  protected ArtifactRepository getCentralRepo() {
    MavenArtifactRepository repo = new MavenArtifactRepository();
    repo.setUrl("https://repo.maven.apache.org/maven2/");
    repo.setId("central");
    repo.setLayout(new DefaultRepositoryLayout());
    ArtifactRepositoryPolicy repoPolicy = new ArtifactRepositoryPolicy();
    repoPolicy.setUpdatePolicy(ArtifactRepositoryPolicy.UPDATE_POLICY_NEVER);
    repoPolicy.setEnabled(true);
    repo.setReleaseUpdatePolicy(repoPolicy);
    ArtifactRepositoryPolicy snapshotPolicy = new ArtifactRepositoryPolicy();
    snapshotPolicy.setEnabled(false);
    repo.setSnapshotUpdatePolicy(snapshotPolicy);
    return repo;
  }
}
