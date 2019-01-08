# Base Alpine Linux based image with OpenJDK JRE only
FROM openjdk:8-jre-alpine
# copy application WAR (with libraries inside)
COPY target/Citos-Server-1.0.jar /app.jar
copy server.conf /server.conf
copy users.csv /users.csv
# Expose Port
EXPOSE 12345
#specify default command
CMD ["/usr/bin/java", "-jar", "/app.jar"]