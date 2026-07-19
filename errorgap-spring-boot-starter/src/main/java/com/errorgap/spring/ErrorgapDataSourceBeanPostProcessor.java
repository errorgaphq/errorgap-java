package com.errorgap.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.function.Supplier;

public class ErrorgapDataSourceBeanPostProcessor implements BeanPostProcessor {
    private final Supplier<QuerySpanCollector> collector;

    public ErrorgapDataSourceBeanPostProcessor(Supplier<QuerySpanCollector> collector) {
        this.collector = collector;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof DataSource dataSource)) {
            return bean;
        }
        if (Proxy.isProxyClass(bean.getClass())
            && Proxy.getInvocationHandler(bean) instanceof DataSourceHandler) {
            return bean;
        }
        return Proxy.newProxyInstance(
            dataSource.getClass().getClassLoader(),
            new Class<?>[]{DataSource.class},
            new DataSourceHandler(dataSource)
        );
    }

    private final class DataSourceHandler implements InvocationHandler {
        private final DataSource delegate;

        private DataSourceHandler(DataSource delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = invokeDelegate(delegate, method, args);
            if (result instanceof Connection connection) {
                return wrapConnection(connection);
            }
            return result;
        }
    }

    private Connection wrapConnection(Connection delegate) {
        return (Connection) Proxy.newProxyInstance(
            delegate.getClass().getClassLoader(),
            new Class<?>[]{Connection.class},
            (proxy, method, args) -> {
                Object result = invokeDelegate(delegate, method, args);
                String name = method.getName();
                if (result instanceof CallableStatement callable && args != null
                    && args.length > 0 && args[0] instanceof String sql) {
                    return wrapStatement(callable, sql, CallableStatement.class);
                }
                if (result instanceof PreparedStatement prepared && args != null
                    && args.length > 0 && args[0] instanceof String sql) {
                    return wrapStatement(prepared, sql, PreparedStatement.class);
                }
                if (result instanceof Statement statement && name.equals("createStatement")) {
                    return wrapStatement(statement, null, Statement.class);
                }
                return result;
            }
        );
    }

    private Object wrapStatement(Statement delegate, String preparedSql, Class<?> statementType) {
        return Proxy.newProxyInstance(
            delegate.getClass().getClassLoader(),
            new Class<?>[]{statementType},
            (proxy, method, args) -> {
                String name = method.getName();
                String sql = preparedSql;
                if (sql == null && name.startsWith("execute") && args != null
                    && args.length > 0 && args[0] instanceof String value) {
                    sql = value;
                }
                if (!name.startsWith("execute") || sql == null) {
                    return invokeDelegate(delegate, method, args);
                }

                long started = System.nanoTime();
                try {
                    return invokeDelegate(delegate, method, args);
                } finally {
                    collector.get().recordDatabase(sql, (System.nanoTime() - started) / 1_000_000.0);
                }
            }
        );
    }

    private static Object invokeDelegate(Object delegate, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        } catch (InvocationTargetException wrapped) {
            throw wrapped.getCause();
        }
    }
}
