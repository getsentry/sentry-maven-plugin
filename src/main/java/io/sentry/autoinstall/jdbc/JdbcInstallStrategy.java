package io.sentry.autoinstall.jdbc;

import io.sentry.autoinstall.AbstractIntegrationInstaller;
import io.sentry.autoinstall.AutoInstallState;
import io.sentry.semver.Version;
import org.apache.maven.model.Dependency;

import java.util.ArrayList;
import java.util.List;

public class JdbcInstallStrategy extends AbstractIntegrationInstaller {

   private static final String SPRING_JDBC_GROUP = "org.springframework";
   private static final String SPRING_JDBC_ID = "spring-jdbc";

   private static final String HSQL_GROUP = "org.hsqldb";
   private static final String HSQL_ID = "hsqldb";

   private static final String MYSQL_GROUP = "mysql";
   private static final String MYSQL_ID = "mysql-connector-java";

   private static final String MARIADB_GROUP = "org.mariadb.jdbc";
   private static final String MARIADB_ID = "mariadb-java-client";

   private static final String POSTGRES_GROUP = "org.postgresql";
   private static final String POSTGRES_ID = "postgresql";

   private static final String ORACLE_GROUP = "com.oracle.jdbc";
   private static final String ORACLE_DATABASE_GROUP = "com.oracle.database.jdbc";
   private static final String ORACLE_OJDBC_ID_PREFIX = "ojdbc";

   public static final String SENTRY_JDBC_ID = "sentry-jdbc";

    protected Version minSupportedSentryVersion() {
        return Version.create(5, 3, 0);
    }

    @Override
    protected Dependency findThirdPartyDependency(List<Dependency> dependencyList) {
        List<String> oracleDependencyList = new ArrayList<>();

        for(int i = 5; i <= 15; i++) {
            oracleDependencyList.add(ORACLE_GROUP + ":" + ORACLE_OJDBC_ID_PREFIX + i);
            oracleDependencyList.add(ORACLE_DATABASE_GROUP + ":" + ORACLE_OJDBC_ID_PREFIX + i);
        }

        return dependencyList.stream().filter((dep) ->
            (dep.getGroupId().equals(SPRING_JDBC_GROUP) && dep.getArtifactId().equals(SPRING_JDBC_ID))
            || (dep.getGroupId().equals(HSQL_GROUP) && dep.getArtifactId().equals(HSQL_ID))
            || (dep.getGroupId().equals(MYSQL_GROUP) && dep.getArtifactId().equals(MYSQL_ID))
            || (dep.getGroupId().equals(MARIADB_GROUP) && dep.getArtifactId().equals(MARIADB_ID))
            || (dep.getGroupId().equals(SPRING_JDBC_GROUP) && dep.getArtifactId().equals(SPRING_JDBC_ID))
            || (dep.getGroupId().equals(POSTGRES_GROUP) && dep.getArtifactId().equals(POSTGRES_ID))
            || oracleDependencyList.contains(dep.getGroupId() + ":" + dep.getArtifactId())
        ).findFirst().orElse(null);
    }

    @Override
    protected boolean shouldInstallModule(AutoInstallState autoInstallState) {
        return autoInstallState.isInstallSpring();
    }

    @Override
    protected String sentryModuleId() {
        return SENTRY_JDBC_ID;
    }
}
