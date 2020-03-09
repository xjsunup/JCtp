package market.futures.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * @author xuejian.sun
 * @date 2019/10/22 8:57
 */
public enum ExchangeMapper {
    CFE("中金所"),
    DCE("大商所"),
    ZCE("郑商所"),
    SHF("上期所"),
    ;
    @Getter
    private String cnName;

    ExchangeMapper(String cnName) {
        this.cnName = cnName;
    }

    public static ExchangeMapper findBy(String cnName) {
        return Arrays.stream(ExchangeMapper.values())
                .filter(exchange -> exchange.cnName.equalsIgnoreCase(cnName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(cnName + "not found !"));
    }
}
