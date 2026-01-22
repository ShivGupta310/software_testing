#official mvn image as base image
FROM maven:3.9.9-amazoncorretto-21-debian AS build
#set working dir in container
WORKDIR /app
#Copy pom.xml and project files to container
COPY pom.xml .
COPY src ./src
#build maven application
RUN mvn clean package -DskipTests
#use official open jdk image as base
FROM eclipse-temurin:21-jre
#set workdir in container
WORKDIR /app
#copy the built JAR file from the previous stage to the container
COPY --from=build /app/target/ilp_submission_1*.jar app.jar

EXPOSE 8080

#set command ot run the app
CMD ["java", "-jar", "./app.jar"]
