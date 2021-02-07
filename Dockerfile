FROM openjdk:8-alpine

COPY target/uberjar/sandbox.jar /sandbox/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/sandbox/app.jar"]
