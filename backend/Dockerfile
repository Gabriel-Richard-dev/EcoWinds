# Build stage
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

ARG MAVEN_VERSION=3.9.9
RUN apt-get update -q && apt-get install -y --no-install-recommends wget ca-certificates \
    && wget -q "https://downloads.apache.org/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    && tar -xzf "apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    && ln -s "/app/apache-maven-${MAVEN_VERSION}/bin/mvn" /usr/local/bin/mvn \
    && rm "apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# Runtime stage
FROM eclipse-temurin:25-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
