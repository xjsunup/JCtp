package market.futures;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * @author xuejian.sun
 * @date 2019/4/12 10:56
 */
@Slf4j
@SpringBootApplication
public class JctpApplication implements ApplicationRunner {

    static {
        System.loadLibrary("thostmduserapi_se");
        System.loadLibrary("thostapi_wrap");
        System.loadLibrary("thosttraderapi_se");
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder()
                .web(WebApplicationType.NONE)
                .sources(JctpApplication.class)
                .main(JctpApplication.class)
                .run(args);
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread daemon = new Thread(() -> {
            while(true) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            }
        });
        daemon.start();
    }
}
