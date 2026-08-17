FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
COPY build/libs/gradup-9f9793e5.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
