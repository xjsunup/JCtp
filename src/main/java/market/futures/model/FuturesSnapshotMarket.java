package market.futures.model;

import ctp.thostapi.CThostFtdcDepthMarketDataField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;
import market.futures.config.FutureContractTable;
import market.futures.enums.ExchangeMapper;

/**
 * @author xuejian.sun
 * @date 2019/4/12 14:39
 */
@Data
@ToString
@Accessors(chain = true)
public class FuturesSnapshotMarket {
    /**
     * 交易日
     */
    private String tradingDay;
    /**
     * 证券编号
     */
    private String instrumentId;
    /**
     * 交易所编号
     */
    private String exchangeId;

    private String exchangeInstId;
    /**
     * 最新价
     */
    private double lastPrice;
    /**
     * 昨结算价
     */
    private double preSettlementPrice;
    /**
     * 昨收盘价
     */
    private double preClosePrice;
    /**
     * 做持仓量
     */
    private double preOpenInterest;
    /**
     * 开盘价
     */
    private double openPrice;
    /**
     * 最高价
     */
    private double highestPrice;
    /**
     * 最低价
     */
    private double lowestPrice;
    /**
     * 成交量
     */
    private double volume;
    /**
     * 成交金额
     */
    private double turnover;
    /**
     * 持仓量
     */
    private double openInterest;
    /**
     * 收盘价
     */
    private double closePrice;
    /**
     * 结算价
     */
    private double settlementPrice;
    /**
     * 涨停价
     */
    private double upperLimitPrice;
    /**
     * 跌停价
     */
    private double lowerLimitPrice;
    /**
     * 昨虚实度
     */
    private double preDelta;
    /**
     * 今虚实度
     */
    private double currDelta;
    /**
     * 更新时间
     */
    private String updateTime;
    /**
     * 更新时间，毫秒位
     */
    private int updateMilliSec;
    /**
     * 买， 价量
     */
    private PriceInfo bid;
    /**
     * 卖，价量
     */
    private PriceInfo ask;
    /**
     * 均价
     */
    private double averagePrice;
    /**
     * 交易日
     */
    private String actionDay;

    private ExchangeMapper exchange;

    @Data
    @AllArgsConstructor
    public static class PriceInfo {
        private double price;

        private int volume;
    }

    public static FuturesSnapshotMarket parseFrom(CThostFtdcDepthMarketDataField field) {
        FuturesSnapshotMarket futuresSnapshot = new FuturesSnapshotMarket();
        futuresSnapshot.settlementPrice = 1.0;
        futuresSnapshot.tradingDay = field.getTradingDay();
        futuresSnapshot.instrumentId = field.getInstrumentID();
        futuresSnapshot.exchangeId = field.getExchangeID();
        futuresSnapshot.exchangeInstId = field.getExchangeInstID();
        futuresSnapshot.lastPrice = field.getLastPrice();
        futuresSnapshot.preSettlementPrice = field.getPreSettlementPrice();
        futuresSnapshot.preClosePrice = field.getPreClosePrice();
        futuresSnapshot.preOpenInterest = field.getPreOpenInterest();
        futuresSnapshot.openPrice = field.getOpenPrice();
        futuresSnapshot.highestPrice = field.getHighestPrice();
        futuresSnapshot.lowestPrice = field.getLowestPrice();
        futuresSnapshot.volume = field.getVolume();
        futuresSnapshot.turnover = field.getTurnover();
        futuresSnapshot.openInterest = field.getOpenInterest();
        futuresSnapshot.closePrice = field.getClosePrice();
        futuresSnapshot.settlementPrice = field.getSettlementPrice();
        futuresSnapshot.upperLimitPrice = field.getUpperLimitPrice();
        futuresSnapshot.lowerLimitPrice = field.getLowerLimitPrice();
        futuresSnapshot.preDelta = field.getPreDelta();
        futuresSnapshot.currDelta = field.getCurrDelta();
        futuresSnapshot.updateTime = field.getUpdateTime();
        futuresSnapshot.updateMilliSec = field.getUpdateMillisec();
        futuresSnapshot.bid = new PriceInfo(field.getBidPrice1(), field.getBidVolume1());
        futuresSnapshot.ask = new PriceInfo(field.getAskPrice1(), field.getAskVolume1());
        futuresSnapshot.averagePrice = field.getAveragePrice();
        futuresSnapshot.actionDay = field.getActionDay();
        FutureContractTable.ContractSubscribeTable table = FutureContractTable.findBy(field.getInstrumentID());
        if(table != null){
            futuresSnapshot.setExchange(table.getSecurityExchange());
        }
        return futuresSnapshot;
    }
}
