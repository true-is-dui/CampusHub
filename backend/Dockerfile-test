# 第一阶段：构建阶段，用 Maven + JDK 编译源码
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests


# 第二阶段：运行阶段，只用 JRE 运行 jar
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /app/target/*.jar campushub.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "campushub.jar"]