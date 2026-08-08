package com.clockshore.aooai.config;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class DataSourcePrint {

    private final ApplicationContext applicationContext;

    public DataSourcePrint(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void printBeans() throws Exception {

        String[] names =
                applicationContext.getBeanNamesForType(DataSource.class);

        for (String name : names) {

            DataSource ds =
                    applicationContext.getBean(name, DataSource.class);

            try (Connection conn = ds.getConnection()) {

                System.out.println(
                        name + " -> "
                                + conn.getMetaData().getURL()
                );
            }
        }
    }
}