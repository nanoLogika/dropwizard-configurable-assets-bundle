package de.nanologika.dropwizard.bundles.assets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.cache.CacheBuilderSpec;
import com.google.common.collect.ImmutableMap;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jetty.setup.ServletEnvironment;
import jakarta.servlet.ServletRegistration;

public class AssetsBundleTest {
  private final ServletEnvironment servletEnvironment = mock(ServletEnvironment.class);
  private final Environment environment = mock(Environment.class);

  private final AssetsBundleConfiguration defaultConfiguration = new AssetsBundleConfiguration() {
    @Override
    public AssetsConfiguration getAssetsConfiguration() {
      return AssetsConfiguration.builder().build();
    }
  };

  private String servletPath;
  private List<String> servletPaths;
  private AssetServlet servlet;

  @Before
  public void setUp() throws Exception {
    when(environment.servlets()).thenReturn(servletEnvironment);
  }


  @SuppressWarnings("static-method")
  @Test
  public void deserialzeAssetsConfiguration() throws IOException {
    ObjectMapper mapper = Jackson.newObjectMapper(new YAMLFactory());
    FileInputStream inputStream = new FileInputStream("src/test/resources/assets/config.yml");
    AssetsConfiguration assets = mapper.readValue(inputStream, AssetsConfiguration.class);
    assertEquals(assets.getCacheSpec(), "abc");
    assertEquals(assets.getResourcePathToUriMappings(), ImmutableMap.of("/assets/","/dashboard/"));
    assertEquals(assets.getMimeTypes(), ImmutableMap.of("woff", "application/font-woff"));
    assertEquals(assets.getOverrides(), ImmutableMap.of("/","../example-app/build/src"));
    assertNull(assets.getCacheControlHeader());
  }

  @Test
  public void hasADefaultPath() throws Exception {
    runBundle(new ConfiguredAssetsBundle());

    assertThat(servletPath)
            .isEqualTo("/assets/*");
  }

  @Test
  public void canHaveCustomPaths() throws Exception {
    runBundle(new ConfiguredAssetsBundle("/json"));

    assertThat(servletPath)
            .isEqualTo("/json/*");
  }

  @Test
  public void canHaveDifferentUriAndResourcePaths() throws Exception {
    runBundle(new ConfiguredAssetsBundle("/json", "/what"));

    assertThat(servletPath)
            .isEqualTo("/what/*");
  }

  @Test
  public void canSupportDifferentAssetsBundleName() throws Exception {
    runBundle(new ConfiguredAssetsBundle("/json", "/what/new", "index.txt", "customAsset1"),
            "customAsset1", defaultConfiguration);

    assertThat(servletPath)
            .isEqualTo("/what/new/*");

    runBundle(new ConfiguredAssetsBundle("/json", "/what/old", "index.txt", "customAsset2"),
            "customAsset2", defaultConfiguration);
    assertThat(servletPath)
            .isEqualTo("/what/old/*");
  }

  @Test
  public void canHaveDifferentUriAndResourcePathsAndIndexFilename() throws Exception {
    runBundle(new ConfiguredAssetsBundle("/json", "/what", "index.txt"));

    assertThat(servletPath)
            .isEqualTo("/what/*");
  }

  @Test
  public void canHaveMultipleMappings() throws Exception {
    runBundle(new ConfiguredAssetsBundle(ImmutableMap.<String, String>builder()
            .put("/risk", "/riskPath")
            .put("/catan", "/catanPath")
            .build()
    ));

    assertThat(servletPaths.size()).isEqualTo(2);
    assertThat(servletPaths).contains("/riskPath/*");
    assertThat(servletPaths).contains("/catanPath/*");
  }

  @Test
  public void usesDefaultCacheSpec() throws Exception {
    runBundle(new ConfiguredAssetsBundle());
    assertThat(servlet.getCacheSpec()).isEqualTo(ConfiguredAssetsBundle.DEFAULT_CACHE_SPEC);
  }

  @Test
  public void canOverrideCacheSpec() throws Exception {
    final String cacheSpec = "expireAfterAccess=20m";

    AssetsBundleConfiguration config = new AssetsBundleConfiguration() {
      @Override
      public AssetsConfiguration getAssetsConfiguration() {
        return AssetsConfiguration.builder().cacheSpec(cacheSpec).build();
      }
    };

    runBundle(new ConfiguredAssetsBundle(), "assets", config);
    assertThat(servlet.getCacheSpec()).isEqualTo(CacheBuilderSpec.parse(cacheSpec));
  }

  private void runBundle(ConfiguredAssetsBundle bundle) throws Exception {
    runBundle(bundle, "assets", defaultConfiguration);
  }

  private void runBundle(ConfiguredAssetsBundle bundle, String assetName,
                         AssetsBundleConfiguration config) throws Exception {
    final ServletRegistration.Dynamic registration = mock(ServletRegistration.Dynamic.class);
    when(servletEnvironment.addServlet(anyString(), any(AssetServlet.class)))
            .thenReturn(registration);

    bundle.run(config, environment);

    final ArgumentCaptor<AssetServlet> servletCaptor = ArgumentCaptor.forClass(AssetServlet.class);
    final ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);

    verify(servletEnvironment, atLeastOnce()).addServlet(eq(assetName), servletCaptor.capture());
    verify(registration, atLeastOnce()).addMapping(pathCaptor.capture());

    this.servletPath = pathCaptor.getValue();
    this.servletPaths = pathCaptor.getAllValues();

    // If more than one servlet was captured, let's verify they're the same instance.
    List<AssetServlet> capturedServlets = servletCaptor.getAllValues();
    if (capturedServlets.size() > 1) {
      for (AssetServlet servlet : capturedServlets) {
        assertThat(servlet == capturedServlets.get(0));
      }
    }

    this.servlet = capturedServlets.get(0);
  }
}
