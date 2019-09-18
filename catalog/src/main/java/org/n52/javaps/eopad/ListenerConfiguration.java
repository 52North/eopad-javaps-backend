package org.n52.javaps.eopad;

import okhttp3.OkHttpClient;
import org.n52.javaps.engine.Engine;
import org.n52.javaps.transactional.TransactionalAlgorithmRepository;
import org.n52.svalbard.encode.EncoderRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;

@Configuration
public class ListenerConfiguration {

    @Bean
    @Deimos
    public CatalogClient deimosCatalogClient(@Deimos CatalogConfiguration configuration,
                                             OkHttpClient client) {
        return new CatalogClientImpl(configuration, client);
    }

    @Bean
    @Deimos
    public CatalogEncoder deimosCatalogEncoder(@Deimos CatalogConfiguration configuration,
                                               Engine engine, EncoderRepository encoderRepository) {
        return new CatalogEncoderImpl(configuration, engine, encoderRepository);
    }

    @Bean
    public CatalogListener deimosCatalogListener(@Deimos CatalogConfiguration configuration,
                                                 @Deimos CatalogEncoder encoder,
                                                 @Deimos CatalogClient client) {
        return new CatalogListener(configuration, encoder, client);
    }

    @Bean
    @Deimos
    public CatalogConfiguration deimosCatalogConfiguration(@Deimos Catalog catalog,
                                                           Collection<TransactionalAlgorithmRepository> repositories) {
        return new CatalogConfigurationImpl(catalog, repositories);
    }

    @Bean
    @Deimos
    public Catalog deimosCatalog(@Value("http://servicecatalogue-ogctestbed15.deimos.pt/smi/") String url) {
        return new CatalogImpl(url);
    }

    @Bean
    @GMU
    public CatalogClient gmuCatalogClient(@GMU CatalogConfiguration configuration,
                                          OkHttpClient client) {
        return new CatalogClientImpl(configuration, client);
    }

    @Bean
    @GMU
    public CatalogEncoder gmuCatalogEncoder(@GMU CatalogConfiguration configuration,
                                            Engine engine, EncoderRepository encoderRepository) {
        return new CatalogEncoderImpl(configuration, engine, encoderRepository);
    }

    @Bean
    public CatalogListener gmuCatalogListener(@GMU CatalogConfiguration configuration,
                                              @GMU CatalogEncoder encoder,
                                              @GMU CatalogClient client) {
        return new CatalogListener(configuration, encoder, client);
    }

    @Bean
    @GMU
    public CatalogConfiguration gmuCatalogConfiguration(@GMU Catalog catalog,
                                                        Collection<TransactionalAlgorithmRepository> repositories) {
        return new CatalogConfigurationImpl(catalog, repositories);
    }

    @Bean
    @GMU
    public Catalog gmuCatalog(@Value("https://cloud.csiss.gmu.edu/ows15/geonet/rest3a/ogc/cat3a/") String url) {
        return new CatalogImpl(url);
    }

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface Deimos {}

    @Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    @interface GMU {}

}
