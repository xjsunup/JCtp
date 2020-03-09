package market.futures.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author xuejian.sun
 * @date 2019/4/12 13:37
 */
@Data
@ConfigurationProperties(prefix = "ctp")
public class CtpProperties {
    /**
     * ctp 行情地址
     */
    private String address;

    private String brokerId;
    /**
     * 用户编号
     */
    private String userId;
    /**
     * 密码
     */
    private String password;

    private String currencyId;
    /**
     * 账户
     */
    private String accountId;

    private String investorId;

    private String appId;

    private String authCode;
}
