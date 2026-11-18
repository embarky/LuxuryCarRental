# -----------------------------------------------------------------------------
# Stage 1: build all Maven modules (domain -> service -> webapp)
# -----------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Resolve dependencies first so they are cached between builds
COPY pom.xml .
COPY domain/pom.xml domain/
COPY service/pom.xml service/
COPY webapp/pom.xml webapp/
RUN mvn -B -q dependency:go-offline -DexcludeArtifactIds=luxurycarrental-domain || true

COPY domain domain
COPY service service
COPY webapp webapp
RUN mvn -B package

# -----------------------------------------------------------------------------
# Stage 2: Payara application server running both WARs
# -----------------------------------------------------------------------------
FROM payara/server-full:6.2025.8-jdk17

# MySQL JDBC driver for the server-managed connection pool
COPY --from=build /build/service/target/libs/mysql-connector-j-*.jar \
     /opt/payara/appserver/glassfish/lib/

# WARs are deployed explicitly by post-boot-commands.asadmin (with context roots)
COPY --from=build /build/service/target/luxurycarrental-service.war /opt/payara/apps/
COPY --from=build /build/webapp/target/luxurycarrental-webapp.war   /opt/payara/apps/

COPY docker/pre-boot-commands.asadmin  /opt/payara/config/pre-boot-commands.asadmin
COPY docker/post-boot-commands.asadmin /opt/payara/config/post-boot-commands.asadmin
