package com.hotpot.jdbc.core;

import com.hotpot.jdbc.annotation.Dao;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;


@Configuration
public class DaoProxyFactory {

    @Autowired
    private DefaultListableBeanFactory factory;

    public static final String FACTORY_METHOD_NAME = "proxy";

    public Object proxy(Class<?> clazz) {
        Dao dao = clazz.getAnnotation(Dao.class);
        NamedParameterJdbcTemplate template = createTemplate(dao.value());
        return Proxy.newProxyInstance(DaoProxyFactory.class.getClassLoader(), new Class[]{clazz}, new DaoInvocationHandler(template));
    }

    private NamedParameterJdbcTemplate createTemplate(String name) {
        if (StringUtils.isEmpty(name)) {
            NamedParameterJdbcTemplate npjt = getBean(NamedParameterJdbcTemplate.class);
            if (npjt != null) {
                return npjt;
            }
            JdbcTemplate jt = getBean(JdbcTemplate.class);
            if (jt != null) {
                name = JdbcTemplate.class.getSimpleName() + NamedParameterJdbcTemplate.class.getSimpleName();
                npjt = registerTemplate(name, new NamedParameterJdbcTemplate(jt));
                return npjt;
            }
            DataSource ds = getBean(DataSource.class);
            if (ds != null) {
                name = DataSource.class.getSimpleName() + NamedParameterJdbcTemplate.class.getSimpleName();
                npjt = registerTemplate(name, new NamedParameterJdbcTemplate(ds));
                return npjt;
            }
        } else {
            Object bean = getBean(name);
            if (bean instanceof NamedParameterJdbcTemplate) {
                return (NamedParameterJdbcTemplate) bean;
            } else if (bean instanceof JdbcTemplate) {
                name += NamedParameterJdbcTemplate.class.getSimpleName();
                Object template = getBean(name);
                if (template == null) {
                    return registerTemplate(name, new NamedParameterJdbcTemplate((JdbcTemplate) bean));
                }
                return (NamedParameterJdbcTemplate) template;
            } else if (bean instanceof DataSource) {
                name += NamedParameterJdbcTemplate.class.getSimpleName();
                Object template = getBean(name);
                if (template == null) {
                    return registerTemplate(name, new NamedParameterJdbcTemplate((DataSource) bean));
                }
                return (NamedParameterJdbcTemplate) template;
            }
        }
        return null;
    }

    public NamedParameterJdbcTemplate registerTemplate(String name, NamedParameterJdbcTemplate template) {
        try {
            factory.registerSingleton(name, template);
            return template;
        } catch (IllegalStateException e) {
            return (NamedParameterJdbcTemplate) factory.getSingleton(name);
        }
    }

    public <T> T getBean(Class<T> requiredType) {
        try {
            return factory.getBean(requiredType);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

    public Object getBean(String name) {
        try {
            return factory.getBean(name);
        } catch (NoSuchBeanDefinitionException e) {
            return null;
        }
    }

}
