FROM openjdk:8-alpine

COPY target/uberjar/oilfield-scada.jar /oilfield-scada/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/oilfield-scada/app.jar"]
