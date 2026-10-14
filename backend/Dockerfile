# syntax=docker/dockerfile:1
# ============================================================================
#  StayEase backend image: Java Servlets (Jakarta Servlet 6) on Apache Tomcat 10.1
#  Build context = ./backend
# ============================================================================

# ---- 1. build the WAR with Maven (unit tests run here; the MySQL integration
#         tests are skipped because TEST_DB_URL is not set) -------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package

# ---- 2. runtime ---------------------------------------------------------------
FROM tomcat:10.1-jdk21-temurin
# No default webapps (manager, docs, examples ...): nothing but our application is deployed.
RUN rm -rf /usr/local/tomcat/webapps/* /usr/local/tomcat/webapps.dist \
 # disable the shutdown port; never show Tomcat version / stack traces in error pages
 && sed -i 's|<Server port="8005"|<Server port="-1"|' /usr/local/tomcat/conf/server.xml \
 && sed -i 's|</Host>|  <Valve className="org.apache.catalina.valves.ErrorReportValve" showReport="false" showServerInfo="false"/>\n      </Host>|' /usr/local/tomcat/conf/server.xml \
 && groupadd --system tomcat && useradd --system --gid tomcat --no-create-home tomcat

COPY --from=build /build/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
RUN chown -R tomcat:tomcat /usr/local/tomcat/webapps /usr/local/tomcat/logs /usr/local/tomcat/temp /usr/local/tomcat/work /usr/local/tomcat/conf/Catalina 2>/dev/null || true \
 && mkdir -p /usr/local/tomcat/conf/Catalina && chown -R tomcat:tomcat /usr/local/tomcat/conf/Catalina

ENV CATALINA_OPTS="-XX:MaxRAMPercentage=70 -Djava.security.egd=file:/dev/urandom" \
    APP_TIMEZONE="Asia/Kolkata"
USER tomcat
EXPOSE 8080
# Database settings (DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD) are supplied at run time.
CMD ["catalina.sh", "run"]
