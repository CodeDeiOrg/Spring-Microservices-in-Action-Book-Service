# stage 1 — extract layers
FROM eclipse-temurin:25-jdk AS build

ARG JAR_FILE
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract --destination extracted

# stage 2 — runtime
FROM eclipse-temurin:25-jre

VOLUME /tmp

COPY --from=build extracted/dependencies/          ./
COPY --from=build extracted/spring-boot-loader/    ./
COPY --from=build extracted/snapshot-dependencies/ ./
COPY --from=build extracted/application/           ./

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
