package dn.orderservice.configuration;

import dn.orderservice.client.ProductClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Configuration
public class ClientConfig {

    @Value("${app.http.client.product-service}")
    private String productServiceBaseUrl;

    @Bean
    public RestClient productRestClient() {
        return RestClient.builder()
                .baseUrl(productServiceBaseUrl)
                .build();
    }

    @Bean
    public ProductClient productHttpClient(RestClient productRestClient ) {
        return HttpServiceProxyFactory.builder()
                .exchangeAdapter(RestClientAdapter.create(productRestClient))
                .build()
                .createClient(ProductClient.class);
    }

}
