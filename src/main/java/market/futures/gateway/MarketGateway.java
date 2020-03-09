package market.futures.gateway;

import ctp.thostapi.*;
import lombok.extern.slf4j.Slf4j;
import market.futures.config.AppEnv;
import market.futures.config.CtpProperties;
import market.futures.handler.LevelOneMarketHandler;
import market.futures.utils.IdGenerate;
import market.futures.utils.LinkedAsyncTaskExecutor;
import market.futures.utils.NamedThreadFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xuejian.sun
 * @date 2019/4/12 11:11
 */
@Slf4j
public class MarketGateway extends CThostFtdcMdSpi implements ApplicationRunner, DisposableBean {

    private CThostFtdcMdApi cThostFtdcMdApi;

    private CtpProperties ctpProperties;

    private LinkedAsyncTaskExecutor<CThostFtdcDepthMarketDataField> taskQueue;

    private ThreadPoolExecutor threadPoolExecutor;

    private volatile boolean logon;

    private AppEnv appEnv;

    private List<String> subSymbol = new ArrayList<>();

    public MarketGateway(CThostFtdcMdApi cThostFtdcMdApi, AppEnv appEnv, CtpProperties ctpProperties, LevelOneMarketHandler levelOneMarketHandler) {
        this.cThostFtdcMdApi = cThostFtdcMdApi;
        this.ctpProperties = ctpProperties;
        this.appEnv = appEnv;
        this.taskQueue = new LinkedAsyncTaskExecutor<>("LevelOneMarketProcessThread", levelOneMarketHandler);
        this.threadPoolExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1), new NamedThreadFactory("CTPConnectThread", true));
    }

    @Override
    public void run(ApplicationArguments args) {
        threadPoolExecutor.execute(() -> {
            cThostFtdcMdApi.RegisterSpi(this);
            cThostFtdcMdApi.RegisterFront(ctpProperties.getAddress());
            cThostFtdcMdApi.Init();
            log.info("正在连接前端.... {}", ctpProperties.getAddress());
            log.info("broker -> {}", ctpProperties.getBrokerId());
        });
    }

    public void subMarketData(String symbol) {
        ArrayList<String> symbols = new ArrayList<>();
        symbols.add(symbol);
        subMarketData(symbols);
    }

    public void subMarketData(List<String> symbol) {
        if(!logon) {
            log.warn("请登录成功后再试...");
            return;
        }
        if(symbol.isEmpty()) {
            return;
        }
        String[] symbols = symbol.toArray(new String[]{});
        int subResp = cThostFtdcMdApi.SubscribeMarketData(symbols, symbols.length);
        responseCodeHandler(subResp, "sub " + symbol);
    }

    public void unsubMarketData(String symbol) {
        ArrayList<String> symbols = new ArrayList<>();
        symbols.add(symbol);
        unsubMarketData(symbols);
    }

    public void unsubMarketData(List<String> symbol) {
        if(!logon) {
            log.warn("请登录成功后再试...");
            return;
        }
        log.info("unsub market data -> {}", symbol);
        String[] symbols = symbol.toArray(new String[]{});
        int unsub = cThostFtdcMdApi.UnSubscribeMarketData(symbols, symbols.length);
        responseCodeHandler(unsub, "unsub " + symbol);
    }

    public void disConnect() {
        if(cThostFtdcMdApi != null) {
            new Thread(() -> {
                log.info("ctp ftd disconnect ...!");
                cThostFtdcMdApi.RegisterSpi(null);
                cThostFtdcMdApi.Release();
            }, "MdApiDisconnectThread").start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                log.error("", e);
            }
            log.info("ctp ftd release ...!");
        }
    }

    @Override
    public void OnFrontConnected() {
        log.info("前端已连接 ...");
        CThostFtdcReqUserLoginField logOn = new CThostFtdcReqUserLoginField();
        logOn.setBrokerID(ctpProperties.getBrokerId());
        logOn.setUserID(ctpProperties.getUserId());
        logOn.setPassword(ctpProperties.getPassword());
        int response = cThostFtdcMdApi.ReqUserLogin(logOn, IdGenerate.nextRequestId());
        responseCodeHandler(response, "登录请求");
    }

    @Override
    public void OnFrontDisconnected(int i) {
        log.info("前端已断开 {}...", i);
    }

    @Override
    public void OnHeartBeatWarning(int i) {
        log.warn("心跳异常 {} ...", i);
    }

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField loginRepInfo, CThostFtdcRspInfoField responseInfo, int i, boolean isLogon) {
        log.info("============== login response ==============\r\n");
        if(isLogon) {
            logon = true;
            log.info("login success!");
            //au1906
            subMarketData(appEnv.subscribeSymbols());
        } else {
            log.info("login failed -> {}", responseInfo.getErrorMsg());
        }
        log.info("userId[{}]", loginRepInfo.getUserID());
        log.info("brokerId [{}]", loginRepInfo.getBrokerID());
        log.info("============== login response ==============\r\n");
    }

    @Override
    public void OnRspUserLogout(CThostFtdcUserLogoutField logoutRepInfo, CThostFtdcRspInfoField responseInfo, int i, boolean logout) {
        log.info("============== logout response ==============\r\n");
        if(logout) {
            log.info("logout success!");
        } else {
            log.info("logout failed -> {}", responseInfo.getErrorMsg());
        }
        log.info("userId[{}]", logoutRepInfo.getUserID());
        log.info("brokerId [{}]", logoutRepInfo.getBrokerID());
        log.info("============== logout response ==============\r\n");
    }

    @Override
    public void OnRspError(CThostFtdcRspInfoField cThostFtdcRspInfoField, int i, boolean result) {
        log.info("============== response error ==============\r\n");
        log.info("{}", cThostFtdcRspInfoField.getErrorMsg());
        log.info("{}", cThostFtdcRspInfoField.getErrorID());
        log.info("============== response error ==============\r\n");
    }

    @Override
    public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField instrumentField, CThostFtdcRspInfoField responseInfo, int i, boolean result) {
        log.info("============== sub market response ==============\r\n");
        if(result) {
            subSymbol.add(instrumentField.getInstrumentID());
            log.info("sub success -> {}", instrumentField.getInstrumentID());
        } else {
            log.warn("sub failed -> {}", responseInfo.getErrorMsg());
        }
        log.info("============== sub market response ==============\r\n");
    }

    @Override
    public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField instrumentField, CThostFtdcRspInfoField responseInfo, int i, boolean result) {
        log.info("============== unsub market response ==============\r\n");
        if(result) {
            subSymbol.remove(instrumentField.getInstrumentID());
            log.info("unsubscribe market data success -> {}", instrumentField.getInstrumentID());
        } else {
            log.warn("unsubscribe market data failed -> {}", instrumentField.getInstrumentID());
        }
        log.info("============== unsub market response ==============\r\n");
    }

    @Override
    public void OnRspSubForQuoteRsp(CThostFtdcSpecificInstrumentField instrumentInfo, CThostFtdcRspInfoField responseInfo, int i, boolean b) {
    }

    @Override
    public void OnRspUnSubForQuoteRsp(CThostFtdcSpecificInstrumentField instrumentInfo, CThostFtdcRspInfoField responseInfo, int i, boolean b) {
        log.info("取消订阅询价应答 -> {}", responseInfo);
    }

    @Override
    public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField depthMarketInfo) {
        taskQueue.addTask(depthMarketInfo);
//        log.info("{},{},{},{}", f.getInstrument().getSymbol(), f.getTime(), f.getInternalTag().getClientTime(), f.getInternalTag().getClientTime() - f.getTime());
    }

    @Override
    public void OnRtnForQuoteRsp(CThostFtdcForQuoteRspField quoteResponseInfo) {
        log.info("询价通知 -> {}", quoteResponseInfo);
    }

    private void responseCodeHandler(int code, String operateInfo) {
        if(code == 0) {
            log.info("{} 操作成功", operateInfo);
        } else if(code == -1) {
            log.warn("网络连接失败");
        } else if(code == -2) {
            log.warn("未处理请求超过许可数");
        } else if(code == -3) {
            log.warn("每秒发送的请求超过许可数");
        }
    }

    @Override
    public void destroy() {
        subSymbol.forEach(this::unsubMarketData);
        CThostFtdcUserLogoutField logout = new CThostFtdcUserLogoutField();
        logout.setBrokerID(ctpProperties.getBrokerId());
        logout.setUserID(ctpProperties.getUserId());
        cThostFtdcMdApi.ReqUserLogout(logout, IdGenerate.nextRequestId());
        disConnect();
        threadPoolExecutor.shutdownNow();
        taskQueue.shutdown();
        log.info("MarketGateway is Terminated!");
    }
}
