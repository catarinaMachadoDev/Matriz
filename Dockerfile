FROM eclipse-temurin:21-jdk AS build
RUN apt-get update && apt-get install -y --no-install-recommends unzip && rm -rf /var/lib/apt/lists/*
WORKDIR /build
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package

FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system matriz && useradd --system --gid matriz matriz
WORKDIR /app
COPY --from=build /build/target/matriz-0.1.0-SNAPSHOT.jar /app/matriz.jar
USER matriz
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/matriz.jar"]
