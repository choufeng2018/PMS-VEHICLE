package com.starnetsecurity.parkClientServer.init;

import com.starnetsecurity.common.util.Constant;
import com.starnetsecurity.common.util.TokenUtil;
import com.starnetsecurity.parkClientServer.allinpay.AllinQueryThread;
import com.starnetsecurity.parkClientServer.cloudTransport.CloudTransPackage;
import com.starnetsecurity.parkClientServer.cloudTransport.CloudTransThread;
import com.starnetsecurity.parkClientServer.quartz.UploadWhiteListThread;
import com.starnetsecurity.parkClientServer.service.AllinpayService;
import com.starnetsecurity.parkClientServer.service.BasicDataSyncService;
import com.starnetsecurity.parkClientServer.service.NewOrderParkService;
import com.starnetsecurity.parkClientServer.sockServer.*;
import com.starnetsecurity.parkClientServer.mq.ParkLotConsumerConfiguration;
import com.starnetsecurity.parkClientServer.service.OrderParkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.ServletContextAware;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by 宏炜 on 2017-07-18.
 */
@Component("AppInfo")
public class AppInfo implements ServletContextAware {
    private static Logger log = LoggerFactory.getLogger(AppInfo.class);

    public static final String parkLotServerError = "云服务器异常";

    public final static String dbSyncMq = "db_sync_mq";

    public final static String mainSyncMq = "main_sync_mq";

    public static boolean rabbitMqStatus = true;

    public static List<ParkLotConsumerConfiguration> rabbitQueueListenerThreads;

    public static String parkLotDomain;

    public static String qiniuAesKey;

    public static String qiniuSecretkey;

    public static String qiniuDomain;

    public static String qiniuBucketName;

    public static String appRootRealPath;
    public static String contextPath;
    public static String dllParh;

    public static String isConnectCloud;
    public static String cloudServerId;
    public static String cloudSecrectKey;
    public static String cloudIp;
    public static String cloudPort;
    public static String cloudCarparkCode;

    public static String isUseUparkPay;
    public static String bussCode;
    public static String bussKey;

    public static ConcurrentLinkedQueue<CloudTransPackage> cloudTransQueue;

    //是否使用通联支付，0-不适用 1-使用
    public static String isUseAllinpay;
    //通联支付专用，APPID
    public static String allinPayAppId;
    //通联支付专用，二维码编码
    public static String allinpayClientId;
    //通联支付专用，密钥
    public static String allinpayKey;
    //通联支付专用，商户号
    public static String allinpayBussinessId;
    //通联支付专用，Trxcode
    public static String allinpayTrxCode;

    public static String lastInCarno;

    public static String lastInCarparkId;

    public static String lastInTime;

    public static String lastOutCarno;

    public static String lastOutCarparkId;

    public static String lastOutTime;

    public static String isUseGuideScreen;

    public static Integer guideScreenPos;

    @Autowired
    BasicDataSyncService basicDataSyncService;

    @Autowired
    OrderParkService orderParkService;

    @Autowired
    NewOrderParkService newOrderParkService;

    @Autowired
    AllinpayService allinpayService;


    private ServletContext servletContext;



    @Override
    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @PostConstruct
    public void init() throws Exception {
        qiniuAesKey = Constant.getPropertie("qiniu.aeskey");
        qiniuSecretkey = Constant.getPropertie("qiniu.secretkey");
        qiniuDomain = Constant.getPropertie("qiniu.domain");
        qiniuBucketName = Constant.getPropertie("qiniu.bucket.name");

        contextPath = servletContext.getContextPath();

        appRootRealPath = servletContext.getRealPath("");
        dllParh = appRootRealPath + File.separator + "WEB-INF" + File.separator + "classes" + File.separator;

        isConnectCloud = Constant.getPropertie("isConnectCloud");
        cloudServerId = Constant.getPropertie("cloudServerId");
        cloudSecrectKey = Constant.getPropertie("cloudSecrectKey");
        cloudIp = Constant.getPropertie("cloudIp");
        cloudPort = Constant.getPropertie("cloudPort");
        cloudCarparkCode = TokenUtil.decrypt(cloudSecrectKey);
        isUseUparkPay = Constant.getPropertie("isUseUparkPay");
        bussCode = Constant.getPropertie("bussCode");
        bussKey = Constant.getPropertie("bussKey");

        if (Constant.getPropertie("isUseBasicPlatform").equals("1"))
            basicDataSyncService.regist();

        if ("on".equals(isConnectCloud)){
            cloudTransQueue = new ConcurrentLinkedQueue<>();
            CloudTransThread cloudTransThread = new CloudTransThread(newOrderParkService);
            cloudTransThread.start();
            log.info("启动白名单数据上传服务");
            UploadWhiteListThread uploadWhiteListThread = new UploadWhiteListThread();
            uploadWhiteListThread.start();
            log.info("启动白名单数据上传服务成功");
        }

        log.info("启动设备服务");
        DeviceSIPServer deviceSIPServer = new DeviceSIPServer();
        deviceSIPServer.start();
        log.info("设备服务启动成功");

        log.info("启动LED空闲状态播报服务");
        LedPlayFreeInfoServer ledPlayFreeInfoServer = new LedPlayFreeInfoServer();
        ledPlayFreeInfoServer.start();
        log.info("启动LED空闲状态播报服务成功");

        log.info("启动客户端数据业务发送线程");
        ClientSendThread clientSendThread = new ClientSendThread();
        clientSendThread.start();

        log.info("启动岗亭服务");
        ParkSocketServer parkSocketServer = new ParkSocketServer();
        parkSocketServer.start();
        log.info("岗亭服务启动成功");

        log.info("启动岗亭客户端心跳定时器");
        ClientHeartThread clientHeartThread = new ClientHeartThread();
        clientHeartThread.start();
        log.info("岗亭客户端心跳定时器启动成功");


        parkLotDomain = Constant.getPropertie("parkLotDomain");

        //通联支付所需参数
        isUseAllinpay = Constant.getPropertie("isUseAllinpay");
        if (isUseAllinpay.equals("1")){
            AllinQueryThread allinQueryThread = new AllinQueryThread(allinpayService);
            allinQueryThread.start();
            log.info("启动定时查询通联订单服务成功");
        }
        allinPayAppId = Constant.getPropertie("appId");
        allinpayClientId = Constant.getPropertie("clientId");
        allinpayKey = Constant.getPropertie("payKey");
        allinpayBussinessId = Constant.getPropertie("bussinessId");
        allinpayTrxCode = Constant.getPropertie("trxCode");

        isUseGuideScreen = Constant.getPropertie("isUseGuideScreen");
        guideScreenPos = Integer.valueOf(Constant.getPropertie("guideScreenPos"));
        if ("1".equals(isUseGuideScreen)){
            GuideScreenThread guideScreenThread = new GuideScreenThread();
            log.info("启动车位引导屏服务成功");
            guideScreenThread.start();
        }
    }

}
