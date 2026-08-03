# syntax=docker/dockerfile:1

# stage 1 — build from source and extract layers
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw clean package -DskipTests -Ddocker.skip=true -B
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# stage 2 — runtime
FROM eclipse-temurin:25-jre

# eclipse-temurin runs as root by default — drop to a dedicated non-root user
RUN groupadd --system spring && useradd --system --gid spring --no-create-home spring

WORKDIR /application

COPY --from=build --chown=spring:spring /workspace/extracted/dependencies/          ./
COPY --from=build --chown=spring:spring /workspace/extracted/spring-boot-loader/    ./
COPY --from=build --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=spring:spring /workspace/extracted/application/           ./

USER spring:spring

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
