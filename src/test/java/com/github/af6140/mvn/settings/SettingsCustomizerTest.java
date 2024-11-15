package com.github.af6140.mvn.settings;

import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SettingsCustomizerTest {

  @Mock
  MavenSession session;

  @Mock
  Settings settings;

  @Mock
  ProjectBuildingRequest buildingRequest;

  @Mock
  Logger logger;

  Mirror mirror;

  @InjectMocks
  SettingsCustomizer settingsCustomizer;

  @BeforeEach
  public void setUp() {
    mirror = new Mirror();
    mirror.setMirrorOf("central");
    mirror.setUrl("https://artifactory.dummy.example.com/maven-central");
  }

  @Test
  public void getCentralRepo() {
    ArtifactRepository repo = settingsCustomizer.getCentralRepo();
    assertNotNull(repo);
    assertEquals("central", repo.getId());
    assertTrue(repo.getReleases().isEnabled());
    assertFalse(repo.getSnapshots().isEnabled());
  }

  @Test
  public void afterSessionStarted() throws MavenExecutionException {
    when(session.getSettings()).thenReturn(settings);
    when(session.getProjectBuildingRequest()).thenReturn(buildingRequest);
    List<Mirror> mirrors = new ArrayList<>();
    mirrors.add(mirror);
    when(settings.getMirrors()).thenReturn(mirrors);
    List<ArtifactRepository> repos = new ArrayList<>();
    ArtifactRepository repo1= new MavenArtifactRepository();
    repo1.setId("test-private");
    repo1.setUrl("https://artifactory.dummy.example.com/interal-release");
    repo1.setLayout(new DefaultRepositoryLayout());
    ArtifactRepository repo2= new MavenArtifactRepository();
    repo2.setId("test-private2");
    repo2.setUrl("https://nexus.dummy.example.com/interal-snapshto");
    repo2.setLayout(new DefaultRepositoryLayout());
    repos.add(repo1);
    repos.add(repo2);
    when(buildingRequest.getRemoteRepositories()).thenReturn(repos);
    settingsCustomizer.afterSessionStart(session);
    settings.getMirrors().forEach(x -> System.out.println(x.getUrl()));
    assertTrue(settings.getMirrors().isEmpty());
    assertEquals(0, repos.stream().filter(x -> x.getId().equals(repo1.getId())).count());
    assertEquals(1, repos.stream().filter(x -> x.getId().equals("central")).count());
    assertEquals(1, repos.stream().filter(x -> x.getId().equals(repo2.getId())).count());
  }
}
