package market.futures.gateway;

import market.futures.config.CtpProperties;
import market.futures.utils.IdGenerate;
import ctp.thostapi.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TradeGateway extends CThostFtdcTraderSpi {

    private static int CONNECTION_STATUS_DISCONNECTED = 0;
    private static int CONNECTION_STATUS_CONNECTED = 1;
    private static int CONNECTION_STATUS_CONNECTING = 2;
    private static int CONNECTION_STATUS_DISCONNECTING = 3;
    private final static Logger logger = LoggerFactory.getLogger(TradeGateway.class);
    private CtpProperties config;

    private CThostFtdcTraderApi cThostFtdcTraderApi;
    private int sessionId;
    private int frontId;
    // 避免重复调用
    private int connectionStatus = CONNECTION_STATUS_DISCONNECTED;
    // 登陆状态
    private boolean loginStatus = false;
    private String tradingDay;

    private boolean instrumentQueried = false;
    private boolean investorNameQueried = false;
    // 是否已经使用错误的信息尝试登录过
    private boolean loginFailed = false;

    public TradeGateway(CtpProperties ctpProperties) {
        this.config = ctpProperties;
    }

    private void connectToCtp() {
        if(isConnected() || connectionStatus == CONNECTION_STATUS_CONNECTING) {
            logger.warn("交易接口已经连接或正在连接，不再重复连接");
            return;
        }

        if(connectionStatus == CONNECTION_STATUS_CONNECTED) {
            reqAuth();
            return;
        }
        connectionStatus = CONNECTION_STATUS_CONNECTING;
        loginStatus = false;
        instrumentQueried = false;
        investorNameQueried = false;
        try {

            if(cThostFtdcTraderApi != null) {
                try {
                    cThostFtdcTraderApi.RegisterSpi(null);
                    CThostFtdcTraderApi cThostFtdcTraderApiForRelease = cThostFtdcTraderApi;
                    cThostFtdcTraderApi = null;

                    new Thread(() -> {
                        Thread.currentThread().setName("GatewayID TD API Release Thread, Time "
                                + System.currentTimeMillis());

                        try {
                            logger.warn("交易接口异步释放启动！");
                            cThostFtdcTraderApiForRelease.Release();
                            logger.warn("交易接口异步释放完成！");
                        } catch (Throwable t) {
                            logger.error("交易接口异步释放发生异常！", t);
                        }
                    }).start();

                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.warn("交易接口连接前释放异常", e);
                }
            }
            logger.warn("交易接口实例初始化");
//            String tempFilePath = "trade" + File.separator + "xyz" + File.separator + "redtorch" + File.separator
//                    + "api" + File.separator + "jctp" + File.separator + "TEMP_CTP" + File.separator + "TD";
//            File tempFile = new File(tempFilePath);
//            if(!tempFile.exists() || !tempFile.getParentFile().exists()) {
//                try {
//                    boolean newFile = tempFile.createNewFile();
//                    if(!newFile) {
//                        logger.info("启动失败。。交易目录创建失败");
//                        return;
//                    }
//                    logger.info("交易接口创建临时文件夹 {}", tempFile.getParentFile().getAbsolutePath());
//                } catch (IOException e) {
//                    logger.error("交易接口创建临时文件夹失败{}", tempFile.getParentFile().getAbsolutePath(), e);
//                }
//            }
//            logger.info("交易接口使用临时文件夹{}", tempFile.getParentFile().getAbsolutePath());
            cThostFtdcTraderApi = CThostFtdcTraderApi.CreateFtdcTraderApi();
            cThostFtdcTraderApi.RegisterSpi(this);
            cThostFtdcTraderApi.RegisterFront(config.getAddress());
            cThostFtdcTraderApi.Init();
            new Thread(() -> {
                try {
                    Thread.sleep(3 * 1000);
                    if(!isConnected()) {
                        logger.error("交易接口连接超时,尝试断开");
                        disconnect();
                    }
                } catch (Throwable t) {
                    logger.error("交易接口处理连接超时线程异常", t);
                }
            }).start();
        } catch (Throwable t) {
            logger.error("交易接口连接异常", t);
        }

    }

    private void disconnect() {
        try {
            if(cThostFtdcTraderApi != null && connectionStatus != CONNECTION_STATUS_DISCONNECTING) {
                logger.warn("交易接口实例开始关闭并释放");
                loginStatus = false;
                instrumentQueried = false;
                investorNameQueried = false;
                connectionStatus = CONNECTION_STATUS_DISCONNECTING;
                try {
                    if(cThostFtdcTraderApi != null) {
                        cThostFtdcTraderApi.RegisterSpi(null);
                        CThostFtdcTraderApi traderApiForRelease = cThostFtdcTraderApi;
                        cThostFtdcTraderApi = null;
                        new Thread(() -> {
                            Thread.currentThread().setName("GatewayID  TD API Release Thread,Start Time " + System.currentTimeMillis());

                            try {
                                logger.warn("交易接口异步释放启动！");
                                traderApiForRelease.Release();
                                logger.warn("交易接口异步释放完成！");
                            } catch (Throwable t) {
                                logger.error("交易接口异步释放发生异常！", t);
                            }
                        }).start();

                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.error("交易接口实例关闭并释放异常", e);
                }

                connectionStatus = CONNECTION_STATUS_DISCONNECTED;
                logger.warn("交易接口实例关闭并异步释放");
            } else {
                logger.warn("交易接口实例不存在或正在关闭释放,无需操作");
            }
        } catch (Throwable t) {
            logger.error("交易接口实例关闭并释放异常", t);
        }

    }
    private boolean isConnected() {
        return connectionStatus == CONNECTION_STATUS_CONNECTED && loginStatus;
    }

    private void queryAccount() {
        if(cThostFtdcTraderApi == null) {
            logger.warn("交易接口尚未初始化,无法查询账户");
            return;
        }
        if(!loginStatus) {
            logger.warn("交易接口尚未登录,无法查询账户");
            return;
        }
        if(!instrumentQueried) {
            logger.warn("交易接口尚未获取到合约信息,无法查询持仓");
            return;
        }
        if(!investorNameQueried) {
            logger.warn("交易接口尚未获取到投资者姓名,无法查询持仓");
            return;
        }
        try {
            CThostFtdcQryTradingAccountField cThostFtdcQryTradingAccountField = new CThostFtdcQryTradingAccountField();
            cThostFtdcTraderApi.ReqQryTradingAccount(cThostFtdcQryTradingAccountField, IdGenerate.nextRequestId());
        } catch (Throwable t) {
            logger.error("交易接口查询账户异常", t);
        }

    }

    private void queryPosition() {
        if(cThostFtdcTraderApi == null) {
            logger.warn("交易接口尚未初始化,无法查询持仓");
            return;
        }
        if(!loginStatus) {
            logger.warn("交易接口尚未登录,无法查询持仓");
            return;
        }

        if(!instrumentQueried) {
            logger.warn("交易接口尚未获取到合约信息,无法查询持仓");
            return;
        }
        if(!investorNameQueried) {
            logger.warn("交易接口尚未获取到投资者姓名,无法查询持仓");
            return;
        }

        try {
            CThostFtdcQryInvestorPositionField cThostFtdcQryInvestorPositionField = new CThostFtdcQryInvestorPositionField();
            // log.info("查询持仓");
            cThostFtdcQryInvestorPositionField.setBrokerID(config.getBrokerId());
            cThostFtdcQryInvestorPositionField.setInvestorID(config.getUserId());
            cThostFtdcTraderApi.ReqQryInvestorPosition(cThostFtdcQryInvestorPositionField, IdGenerate.nextRequestId());
        } catch (Throwable t) {
            logger.error("交易接口查询持仓异常", t);
        }

    }

    private void reqAuth() {
        if(loginFailed) {
            logger.warn("交易接口登录曾发生错误,不再登录,以防被锁");
            return;
        }

        if(cThostFtdcTraderApi == null) {
            logger.warn("发起客户端验证请求错误,交易接口实例不存在");
            return;
        }

        if(StringUtils.isEmpty(config.getBrokerId())) {
            logger.error("BrokerID不允许为空");
            return;
        }

        if(StringUtils.isEmpty(config.getUserId())) {
            logger.error("UserId不允许为空");
            return;
        }

        if(StringUtils.isEmpty(config.getPassword())) {
            logger.error("Password不允许为空");
            return;
        }

        if(StringUtils.isEmpty(config.getAppId())) {
            logger.error("AppId不允许为空");
            return;
        }
        if(StringUtils.isEmpty(config.getAuthCode())) {
            logger.error("AuthCode不允许为空");
            return;
        }

        try {
            CThostFtdcReqAuthenticateField authenticateField = new CThostFtdcReqAuthenticateField();
            authenticateField.setAppID(config.getAppId());
            authenticateField.setAuthCode(config.getAuthCode());
            authenticateField.setBrokerID(config.getBrokerId());
            authenticateField.setUserID(config.getUserId());
            cThostFtdcTraderApi.ReqAuthenticate(authenticateField, IdGenerate.nextRequestId());
        } catch (Throwable t) {
            logger.error("发起客户端验证异常", t);
            disconnect();
        }

    }

    // 前置机联机回报
    @Override
    public void OnFrontConnected() {
        try {
            logger.info("交易接口前置机已连接");
            // 修改前置机连接状态
            connectionStatus = CONNECTION_STATUS_CONNECTED;
            reqAuth();
        } catch (Throwable t) {
            logger.error("OnFrontConnected Exception", t);
        }
    }



    public void OnFrontDisconnected(int nReason) {
        try {
            logger.warn("交易接口前置机已断开, 原因:{}", nReason);
            disconnect();
        } catch (Exception e) {
            logger.error("OnFrontDisconnected Exception", e);
            throw e;
        }
    }

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo,
                               int nRequestID, boolean bIsLast) {
        try {
            if(pRspInfo.getErrorID() == 0) {
                logger.warn("交易接口登录成功 TradingDay:{},SessionID:{},BrokerID:{},UserId:{}",
                        pRspUserLogin.getTradingDay(), pRspUserLogin.getSessionID(), pRspUserLogin.getBrokerID(),
                        pRspUserLogin.getUserID());
                sessionId = pRspUserLogin.getSessionID();
                frontId = pRspUserLogin.getFrontID();
                // 修改登录状态为true
                loginStatus = true;
                tradingDay = pRspUserLogin.getTradingDay();
                logger.warn("交易接口获取到的交易日为{}", tradingDay);

                // 确认结算单
                CThostFtdcSettlementInfoConfirmField settlementInfoConfirmField = new CThostFtdcSettlementInfoConfirmField();
                settlementInfoConfirmField.setBrokerID(config.getBrokerId());
                settlementInfoConfirmField.setInvestorID(config.getInvestorId());
                cThostFtdcTraderApi.ReqSettlementInfoConfirm(settlementInfoConfirmField, IdGenerate.nextRequestId());

            } else {
                logger.error("交易接口登录回报错误 错误ID:{},错误信息:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg());
                loginFailed = true;
            }
        } catch (Throwable t) {
            logger.error("交易接口处理登录回报异常", t);
            loginFailed = true;
        }

    }

    // 心跳警告
    @Override
    public void OnHeartBeatWarning(int nTimeLapse) {
        logger.warn("交易接口心跳警告, Time Lapse:{}", nTimeLapse);
    }

    // 登出回报
    @Override
    public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID,
                                boolean bIsLast) {
        try {
            if(pRspInfo.getErrorID() != 0) {
                logger.error("OnRspUserLogout!错误ID:{},错误信息:{}", pRspInfo.getErrorID(),
                        pRspInfo.getErrorMsg());
            } else {
                logger.info("OnRspUserLogout!BrokerID:{},UserId:{}", pUserLogout.getBrokerID(),
                        pUserLogout.getUserID());

            }
        } catch (Throwable t) {
            logger.error("交易接口处理登出回报错误", t);
        }

        loginStatus = false;
    }

    @Override
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        logger.error("交易接口错误回报!错误ID:{},错误信息:{},请求ID:{}", pRspInfo.getErrorID(), pRspInfo.getErrorMsg(),
                nRequestID);
    }

    // 验证客户端回报
    @Override
    public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo,
                                  int nRequestID, boolean bIsLast) {
        try {
            if(pRspInfo != null) {
                if(pRspInfo.getErrorID() == 0) {
                    logger.warn("交易接口客户端验证成功");
                    CThostFtdcReqUserLoginField reqUserLoginField = new CThostFtdcReqUserLoginField();
                    reqUserLoginField.setBrokerID(config.getBrokerId());
                    reqUserLoginField.setUserID(config.getUserId());
                    reqUserLoginField.setPassword(config.getPassword());
                    cThostFtdcTraderApi.ReqUserLogin(reqUserLoginField, IdGenerate.nextRequestId());
                } else {

                    logger.error("交易接口客户端验证失败 错误ID:{},错误信息:{}", pRspInfo.getErrorID(),
                            pRspInfo.getErrorMsg());
                    loginFailed = true;
                }
            } else {
                loginFailed = true;
                logger.error("处理交易接口客户端验证回报错误,回报信息为空");
            }
        } catch (Throwable t) {
            loginFailed = true;
            logger.error("处理交易接口客户端验证回报异常", t);
        }
    }

    @Override
    public void OnRspUserPasswordUpdate(CThostFtdcUserPasswordUpdateField pUserPasswordUpdate,
                                        CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
    }

    @Override
    public void OnRspTradingAccountPasswordUpdate(
            CThostFtdcTradingAccountPasswordUpdateField pTradingAccountPasswordUpdate, CThostFtdcRspInfoField pRspInfo,
            int nRequestID, boolean bIsLast) {
    }
}