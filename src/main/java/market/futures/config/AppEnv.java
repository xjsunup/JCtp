package market.futures.config;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author xuejian.sun
 * @date 2019/9/18 10:57
 */
@Component
@Setter
public class AppEnv {

    @Value("${market.subscribe.symbols:#{null}}")
    private String subscribeSymbol;

    public List<String> subscribeSymbols() {
        return FutureContractTable.getSubSymbols();
    }

    public static void main(String[] args) {
        AppEnv appEnv = new AppEnv();
        appEnv.setSubscribeSymbol("ag1903");
        List<String> stringList = appEnv.subscribeSymbols();
        System.out.println(stringList.toString());
    }
}
