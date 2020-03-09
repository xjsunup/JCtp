package market.futures.utils;

import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * @author xuejian.sun
 * @date 2019-08-12 10:17
 */
@Component
public class SpringBeans implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public static <T> T getBean(Class<T> tClass) {
        return applicationContext.getBean(tClass);
    }

    public static Object getBean(String beanName) {
        return applicationContext.getBean(beanName);
    }

    public static <T> T getBean(String beanName, Class<T> tClass) {
        return applicationContext.getBean(beanName, tClass);
    }

    public static <T extends Annotation> Map<String, Object> getBeanWithAnnotation(Class<T> tClass) {
        return applicationContext.getBeansWithAnnotation(tClass);
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> tClass) {
        return applicationContext.getBeansOfType(tClass);
    }

    public static void stopApplication(int signal) {
        if(applicationContext != null) {
            System.exit(SpringApplication.exit(applicationContext, () -> signal));
        } else {
            System.exit(signal);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringBeans.applicationContext = applicationContext;
    }
}
