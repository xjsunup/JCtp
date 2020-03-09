package market.futures.config;

import ctp.thostapi.CThostFtdcMdApi;
import market.futures.gateway.MarketGateway;
import market.futures.gateway.TradeGateway;
import market.futures.handler.LevelOneMarketHandler;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;

/**
 * @author xuejian.sun
 * @date 2019/4/12 13:24
 */
@Configuration
@EnableConfigurationProperties(value = {CtpProperties.class, FutureContractTable.class})
public class AppConfiguration {

    @Bean
    @ConditionalOnProperty(name = "server.model", havingValue = "market")
    public MarketGateway marketGateway(CtpProperties ctpProperties, AppEnv appEnv, LevelOneMarketHandler levelOneMarketHandler) {
        return new MarketGateway(cThostFtdcMdApi(), appEnv, ctpProperties, levelOneMarketHandler);
    }

    @Bean(initMethod = "connectToCtp")
    @ConditionalOnProperty(name = "server.model", havingValue = "trade")
    public TradeGateway tradeGateway(CtpProperties ctpProperties) {
        return new TradeGateway(ctpProperties);
    }

    @Bean
    public CThostFtdcMdApi cThostFtdcMdApi() {
        return CThostFtdcMdApi.CreateFtdcMdApi();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("futures.yml"));//class引入
        configurer.setProperties(yaml.getObject());
        return configurer;
    }
}
