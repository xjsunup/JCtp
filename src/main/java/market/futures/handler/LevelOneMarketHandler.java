package market.futures.handler;

import ctp.thostapi.CThostFtdcDepthMarketDataField;
import lombok.extern.slf4j.Slf4j;
import market.futures.model.FuturesSnapshotMarket;
import market.futures.utils.AsyncTaskCallback;
import org.springframework.stereotype.Component;

/**
 * @author xuejian.sun
 * @date 2019/4/15 13:14
 */
@Slf4j
@Component
public class LevelOneMarketHandler implements AsyncTaskCallback<CThostFtdcDepthMarketDataField> {

    @Override
    public void call(CThostFtdcDepthMarketDataField depthMarketDataField) {
        log.info("深度行情 -> {}", FuturesSnapshotMarket.parseFrom(depthMarketDataField));
    }
}
