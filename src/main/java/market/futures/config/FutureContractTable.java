package market.futures.config;

import lombok.Data;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import market.futures.enums.ExchangeMapper;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xuejian.sun
 * @date 2019/9/26 15:24
 */
@Slf4j
@Setter
@ConfigurationProperties(prefix = "futures")
public class FutureContractTable {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMM");

    private static List<ContractTable> table;

    private static List<ContractSubscribeTable> subscribeFuturesList;

    private static void setTables(List<ContractTable> table) {
        FutureContractTable.table = table;
        subscribeFuturesList = new LinkedList<>();
        for(ContractTable tab : table) {
            List<String> symbols = new LinkedList<>();
            ContractSubscribeTable subscribeTable = new ContractSubscribeTable();
            subscribeTable.setSymbols(symbols);
            subscribeTable.setFutureExchangeName(tab.getName());
            ExchangeMapper securityExchange = ExchangeMapper.findBy(tab.name);
            subscribeTable.setSecurityExchange(securityExchange);
            log.info("***************** {} ***************** \r", tab.getName());
            for(Map<String, String> symbolMap : tab.getSymbol()) {
                symbolMap.forEach((code, symbol) -> {
                    log.info("-----> {}\r", symbol);
                    int month = getStartContractMonth();
                    int year = getStartContractYear();
                    String futureContract = generateSymbol(year, month, code, securityExchange);
                    log.info("{}", futureContract);
                    symbols.add(futureContract);
                    for(int i = 1; i <= 11; i++) {
                        if(month == 12) {
                            year += 1;
                            month = 1;
                        } else {
                            month += 1;
                        }
                        futureContract = generateSymbol(year, month, code, securityExchange);
                        log.info("{}", futureContract);
                        symbols.add(futureContract);
                    }
                });
            }
            log.info("***************** {} ***************** \r\n", tab.getName());
            subscribeFuturesList.add(subscribeTable);
        }
    }

    public void setTable(List<ContractTable> table) {
        setTables(table);
    }

    public static ContractSubscribeTable findBy(String symbol) {
        if(symbol == null) {
            return null;
        }
        for(ContractSubscribeTable subscribeTable : subscribeFuturesList) {
            if(subscribeTable.getSymbols().stream().anyMatch(s -> s.equals(symbol))) {
                return subscribeTable;
            }
        }
        log.warn("unknown symbol -> {}", symbol);
        return null;
    }

    public static List<String> getSubSymbols() {
        return subscribeFuturesList.stream()
                .map(ContractSubscribeTable::getSymbols)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private static String generateSymbol(int year, int month, String code, ExchangeMapper securityExchange) {
        if(securityExchange == ExchangeMapper.ZCE) {
            String strYear = year + "";
            return code + (strYear.substring(strYear.length() - 1)) + (month < 10 ? "0" + month : month);
        } else {
            return code + year + (month < 10 ? "0" + month : month);
        }
    }

    private static int getStartContractMonth() {
        String date = formatter.format(LocalDate.now());
        return Integer.parseInt(date.substring(date.length() - 2));
    }

    private static int getStartContractYear() {
        String date = formatter.format(LocalDate.now());
        return Integer.parseInt(date.substring(2, 4));
    }

    @Data
    public static class ContractTable {
        private String name;

        private List<Map<String, String>> symbol;
    }

    @Data
    public static class ContractSubscribeTable {
        private String futureExchangeName;

        private ExchangeMapper securityExchange;

        private List<String> symbols;
    }
}
