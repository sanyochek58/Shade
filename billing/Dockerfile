FROM gradle:8.14-jdk25 AS builder

WORKDIR /app

COPY build.gradle .
COPY settings.gradle .

RUN gradle dependencies --no-daemon

COPY src ./src
RUN gradle bootJar --no-daemon

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", \
                       "-XX:+UseContainerSupport", \
                       "-XX:MaxRAMPercentage=75.0", \
                       "-jar","app.jar"]