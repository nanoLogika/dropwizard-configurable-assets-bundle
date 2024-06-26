# Configurable Assets Bundle for Dropwizard

This GitHub repository is based on [configurable assets bundle](https://github.com/dropwizard-bundles/dropwizard-configurable-assets-bundle) by [Brandon Beck](https://github.com/bbeck), [Nate Bauernfeind](https://github.com/nbauernfeind/) and all other contributors that made the original project possible. This is merely an update to provide the same functionality to Dropwizard 4.x with Java 11+.

As the functionality remains the same, you may refer to the readme of the original project, though the content will be repeated here for ease of use.

## Maven Setup

```xml
<dependency>
  <groupId>de.nanologika.dropwizard</groupId>
  <artifactId>dropwizard-configurable-assets-bundle</artifactId>
  <version>1.0.0</version>
</dependency>
```

## Usage

Developers can use the `ConfiguredAssetsBundle` class anywhere they would use a `AssetsBundle` in their Dropwizard applications and take advantage of the ability
to specify redirects for URIs to that loads them from disk instead of the classpath. This allows developers to edit browser-interpreted files and reload them without needing to recompile source.

## Getting Started

Implement the AssetsBundleConfiguration:
```java
public class SampleConfiguration extends Configuration implements AssetsBundleConfiguration {
  @Valid
  @NotNull
  @JsonProperty
  private final AssetsConfiguration assets = AssetsConfiguration.builder().build();

  @Override
  public AssetsConfiguration getAssetsConfiguration() {
    return assets;
  }
}
```

Add the assets bundle:
```java
public class SampleService extends Application<SampleConfiguration> {
    public static void main(String[] args) throws Exception {
        new SampleService().run(args);
    }

    @Override
    public void initialize(Bootstrap<SampleConfiguration> bootstrap) {
        // Map requests to /dashboard/${1} to be found in the class path at /assets/${1}.
        bootstrap.addBundle(new ConfiguredAssetsBundle("/assets/", "/dashboard/"));
    }

    @Override
    public void run(SampleConfiguration configuration, Environment environment) {
        ...
    }
}
```

A sample local development config:
```yml
assets:
  overrides:
    # Override requests to /dashboard/${1} to instead look in 
    # ${working directory}/src/main/resources/assets/${1}
    /dashboard: src/main/resources/assets/
```

You can override multiple external folders with a single configuration in a following way:
```yml
assets:
  overrides:
    /dashboard/assets: /some/absolute/path/with/assets/
    /dashboard/images: /some/different/absolute/path/with/images
```

Instead of defining the resource path to uri path mappings in java code, they also can be specified in the configuration file.
```java
public class SampleService extends Application<SampleConfiguration> {
    ...

    @Override
    public void initialize(Bootstrap<SampleConfiguration> bootstrap) {
        bootstrap.addBundle(new ConfiguredAssetsBundle());
    }

    @Override
    public void run(SampleConfiguration configuration, Environment environment) {
        ...
    }
}
```

```yml
assets:
  mappings:
    /assets: /dashboard
  overrides:
    /dashboard/assets: /some/absolute/path/with/assets/
    /dashboard/images: /some/different/absolute/path/with/images
```

## Add Mime Types

Since 0.8, Dropwizard allows you to add new mimetypes directly to the application context.

```java
public class SampleService extends Application<SampleConfiguration> {
   ...

   @Override
   public void run(SampleConfiguration configuration, Environment environment) {
       environment
           .getApplicationContext()
           .getMimeTypes()
           .addMimeMapping("mp4", "video/mp4");
   }
}
```

However if you want to override a pre-existing mime type, or add them dynamically, you can do so
with your assets configuration.

```yml
assets:
  mimeTypes:
    woff: application/font-woff
```

## Multiple URI Mappings

You can map different folders to multiple top-level directories if you wish.

Either in java code
```java
public class SampleService extends Application<SampleConfiguration> {
    ...

    @Override
    public void initialize(Bootstrap<SampleConfiguration> bootstrap) {
        bootstrap.addBundle(new ConfiguredAssetsBundle(
            ImmutableMap.<String, String>builder()
                .put("/assets/", "/dashboard/")
                .put("/data/", "/static-data/")
                .build()
        ));
    }
}
```

or either in the configuration file
```yml
assets:
  mappings:
    /assets: /dashboard
    /data: /static-data
  overrides:
    ...
```
