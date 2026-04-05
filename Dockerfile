FROM maven:3.9-eclipse-temurin-17 AS backend-build
WORKDIR /app
COPY backend/pom.xml ./
RUN mvn dependency:go-offline -q
COPY backend/src ./src
RUN mvn clean package -DskipTests -q

# Add this line - creates the final runtime stage
FROM eclipse-temurin:17-jre-alpine

# Now copy from the completed backend-build stage
COPY --from=backend-build /app/target/buildmat-web-1.0.0.jar app.jar

# Railway sets PORT env var
ENV PORT=8080
EXPOSE ${PORT}

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-railway} -XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -jar app.jar"]